/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.document.library.preview.pdf.internal;

import com.liferay.document.library.kernel.document.conversion.DocumentConversionUtil;
import com.liferay.document.library.kernel.exception.NoSuchFileEntryException;
import com.liferay.document.library.kernel.util.DLUtil;
import com.liferay.document.library.preview.pdf.internal.background.task.PDFPreviewBackgroundTaskExecutor;
import com.liferay.document.library.preview.pdf.internal.configuration.admin.service.PDFPreviewManagedServiceFactory;
import com.liferay.document.library.preview.pdf.internal.util.ProcessConfigUtil;
import com.liferay.petra.process.ProcessCallable;
import com.liferay.petra.process.ProcessChannel;
import com.liferay.petra.process.ProcessException;
import com.liferay.petra.process.ProcessExecutor;
import com.liferay.petra.string.StringBundler;
import com.liferay.petra.string.StringPool;
import com.liferay.portal.configuration.metatype.annotations.ExtendedObjectClassDefinition;
import com.liferay.portal.kernel.backgroundtask.BackgroundTaskManager;
import com.liferay.portal.kernel.backgroundtask.constants.BackgroundTaskContextMapConstants;
import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.image.Ghostscript;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.model.CompanyConstants;
import com.liferay.portal.kernel.model.UserConstants;
import com.liferay.portal.kernel.repository.event.FileVersionPreviewEventListener;
import com.liferay.portal.kernel.repository.model.FileVersion;
import com.liferay.portal.kernel.service.CompanyLocalService;
import com.liferay.portal.kernel.service.ServiceContext;
import com.liferay.portal.kernel.util.FileUtil;
import com.liferay.portal.kernel.util.HashMapBuilder;
import com.liferay.portal.kernel.util.SystemEnv;
import com.liferay.portal.kernel.uuid.PortalUUID;
import com.liferay.portal.util.PropsUtil;
import com.liferay.portal.util.PropsValues;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDDocumentCatalog;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageTree;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.encryption.InvalidPasswordException;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;

/**
 * @author Alexander Chow
 * @author Mika Koivisto
 * @author Juan González
 * @author Sergio González
 * @author Ivica Cardic
 */
@Component(service = com.liferay.document.library.kernel.util.PDFProcessor.class)
public class PDFProcessor
	extends PDFDLProcessor
	implements com.liferay.document.library.kernel.util.PDFProcessor {

	@Override
	public void generateImages(
			FileVersion sourceFileVersion, FileVersion destinationFileVersion)
		throws Exception {

		int maxNumberOfPages =
			_pdfPreviewManagedServiceFactory.getMaxNumberOfPages(
				ExtendedObjectClassDefinition.Scope.GROUP.getValue(),
				destinationFileVersion.getGroupId());

		_generateImages(
			sourceFileVersion, destinationFileVersion, maxNumberOfPages);
	}

	@Override
	public void generatePreviews() {
		_companyLocalService.forEachCompanyId(
			companyId -> {
				try {
					String jobName = "generatePreviews-".concat(
						_portalUUID.generate());

					_backgroundTaskManager.addBackgroundTask(
						UserConstants.USER_ID_DEFAULT, CompanyConstants.SYSTEM,
						jobName,
						PDFPreviewBackgroundTaskExecutor.class.getName(),
						HashMapBuilder.<String, Serializable>put(
							BackgroundTaskContextMapConstants.DELETE_ON_SUCCESS,
							true
						).put(
							"companyId", companyId
						).build(),
						new ServiceContext());
				}
				catch (PortalException portalException) {
					if (_log.isWarnEnabled()) {
						_log.warn(portalException);
					}
				}
			});
	}

	@Override
	public InputStream getPreviewAsStream(FileVersion fileVersion, int index)
		throws Exception {

		return doGetPreviewAsStream(fileVersion, index, PREVIEW_TYPE);
	}

	@Override
	public int getPreviewFileCount(FileVersion fileVersion) {
		return getPreviewFileCount(fileVersion);
	}

	@Override
	public long getPreviewFileSize(FileVersion fileVersion, int index)
		throws Exception {

		return doGetPreviewFileSize(fileVersion, index);
	}

	@Override
	public InputStream getThumbnailAsStream(FileVersion fileVersion, int index)
		throws Exception {

		return doGetThumbnailAsStream(fileVersion, index);
	}

	@Override
	public long getThumbnailFileSize(FileVersion fileVersion, int index)
		throws Exception {

		return doGetThumbnailFileSize(fileVersion, index);
	}

	@Override
	public boolean hasImages(FileVersion fileVersion) {
		return hasImages(fileVersion);
	}

	@Override
	public boolean isDocumentSupported(FileVersion fileVersion) {
		return isSupported(fileVersion);
	}

	@Override
	public boolean isDocumentSupported(String mimeType) {
		return isSupported(mimeType);
	}

	@Override
	public void trigger(
		FileVersion sourceFileVersion, FileVersion destinationFileVersion) {

		super.trigger(sourceFileVersion, destinationFileVersion);

		queueGeneration(sourceFileVersion, destinationFileVersion);
	}

	@Activate
	protected void activate() {
		afterPropertiesSet();
	}

	@Deactivate
	protected void deactivate() {
		destroy();
	}

	private void _addDimensions(List<String> arguments, File file)
		throws Exception {

		Map<String, Integer> scaledDimensions = _getScaledDimensions(file);

		if ((PropsValues.DL_FILE_ENTRY_PREVIEW_DOCUMENT_MAX_WIDTH != 0) &&
			(PropsValues.DL_FILE_ENTRY_PREVIEW_DOCUMENT_MAX_HEIGHT != 0)) {

			arguments.add(
				"-dDEVICEWIDTH=" +
					PropsValues.DL_FILE_ENTRY_PREVIEW_DOCUMENT_MAX_WIDTH);

			arguments.add(
				"-dDEVICEHEIGHT=" +
					PropsValues.DL_FILE_ENTRY_PREVIEW_DOCUMENT_MAX_HEIGHT);
		}
		else if ((PropsValues.DL_FILE_ENTRY_PREVIEW_DOCUMENT_MAX_WIDTH != 0) &&
				 (PropsValues.DL_FILE_ENTRY_PREVIEW_DOCUMENT_MAX_HEIGHT == 0)) {

			arguments.add(
				"-dDEVICEWIDTH=" +
					PropsValues.DL_FILE_ENTRY_PREVIEW_DOCUMENT_MAX_WIDTH);

			arguments.add("-dDEVICEHEIGHT=" + scaledDimensions.get("height"));
		}
		else if ((PropsValues.DL_FILE_ENTRY_PREVIEW_DOCUMENT_MAX_WIDTH == 0) &&
				 (PropsValues.DL_FILE_ENTRY_PREVIEW_DOCUMENT_MAX_HEIGHT != 0)) {

			arguments.add("-dDEVICEWIDTH=" + scaledDimensions.get("width"));

			arguments.add(
				"-dDEVICEHEIGHT=" +
					PropsValues.DL_FILE_ENTRY_PREVIEW_DOCUMENT_MAX_HEIGHT);
		}
	}

	private void _generateImages(
			FileVersion fileVersion, File file, int maxNumberOfPages)
		throws Exception {

		if (_ghostscript.isEnabled()) {
			_generateImagesGS(fileVersion, file, maxNumberOfPages);
		}
		else {
			_generateImagesPB(fileVersion, file, maxNumberOfPages);
		}
	}

	private void _generateImages(
			FileVersion sourceFileVersion, FileVersion destinationFileVersion,
			int maxNumberOfPages)
		throws Exception {

		try {
			if (sourceFileVersion != null) {
				copy(sourceFileVersion, destinationFileVersion);

				return;
			}

			if (hasImage(destinationFileVersion)) {
				return;
			}

			String extension = destinationFileVersion.getExtension();

			if (extension.equals("pdf")) {
				try (InputStream inputStream =
						destinationFileVersion.getContentStream(false)) {

					_generateImages(
						destinationFileVersion, inputStream, maxNumberOfPages);
				}
			}
			else if (DocumentConversionUtil.isEnabled()) {
				try (InputStream inputStream =
						destinationFileVersion.getContentStream(false)) {

					String tempFileId = DLUtil.getTempFileId(
						destinationFileVersion.getFileEntryId(),
						destinationFileVersion.getVersion());

					if (Objects.equals(
							destinationFileVersion.getVersion(), "PWC") ||
						destinationFileVersion.isPending()) {

						File file = new File(
							DocumentConversionUtil.getFilePath(
								tempFileId, "pdf"));

						FileUtil.delete(file);
					}

					File file = DocumentConversionUtil.convert(
						tempFileId, inputStream, extension, "pdf");

					_generateImages(
						destinationFileVersion, file, maxNumberOfPages);
				}
			}
		}
		catch (NoSuchFileEntryException noSuchFileEntryException) {
			if (_log.isDebugEnabled()) {
				_log.debug(noSuchFileEntryException);
			}
		}
		finally {
			fileVersionIds.remove(destinationFileVersion.getFileVersionId());
		}
	}

	private void _generateImages(
			FileVersion fileVersion, InputStream inputStream,
			int maxNumberOfPages)
		throws Exception {

		if (_ghostscript.isEnabled()) {
			_generateImagesGS(fileVersion, inputStream, maxNumberOfPages);
		}
		else {
			_generateImagesPB(fileVersion, inputStream, maxNumberOfPages);
		}
	}

	private void _generateImagesGS(
			FileVersion fileVersion, File file, int maxNumberOfPages)
		throws Exception {

		if (_isGeneratePreview(fileVersion)) {
			long start = System.currentTimeMillis();

			_generateImagesGS(fileVersion, file, maxNumberOfPages, false);

			if (_log.isInfoEnabled()) {
				_log.info(
					StringBundler.concat(
						"Ghostscript generated ",
						getPreviewFileCount(fileVersion), " preview pages for ",
						fileVersion.getTitle(), " in ",
						System.currentTimeMillis() - start, " ms"));
			}
		}

		if (_isGenerateThumbnail(fileVersion)) {
			long start = System.currentTimeMillis();

			_generateImagesGS(fileVersion, file, maxNumberOfPages, true);

			if (_log.isInfoEnabled()) {
				_log.info(
					StringBundler.concat(
						"Ghostscript generated a thumbnail for ",
						fileVersion.getTitle(), " in ",
						System.currentTimeMillis() - start, " ms"));
			}
		}
	}

	private void _generateImagesGS(
			FileVersion fileVersion, File file, int maxNumberOfPages,
			boolean thumbnail)
		throws Exception {

		if (!_ghostscriptInitialized) {
			_ghostscript.reset();

			_ghostscriptInitialized = true;
		}

		// Generate images

		String tempFileId = DLUtil.getTempFileId(
			fileVersion.getFileEntryId(), fileVersion.getVersion());

		List<String> arguments = new ArrayList<>();

		arguments.add("-sDEVICE=png16m");

		if (thumbnail) {
			arguments.add(
				"-sOutputFile=" + getThumbnailTempFilePath(tempFileId));
			arguments.add("-dFirstPage=1");
			arguments.add("-dLastPage=1");
		}
		else {
			arguments.add(
				"-sOutputFile=" + getPreviewTempFilePath(tempFileId, -1));

			if (maxNumberOfPages != 0) {
				arguments.add("-dFirstPage=1");
				arguments.add("-dLastPage=" + maxNumberOfPages);
			}
		}

		arguments.add("-dTextAlphaBits=4");
		arguments.add("-dGraphicsAlphaBits=4");
		arguments.add("-r" + PropsValues.DL_FILE_ENTRY_PREVIEW_DOCUMENT_DPI);

		_addDimensions(arguments, file);

		arguments.add(file.getPath());

		Future<?> future = _ghostscript.execute(arguments);

		String processIdentity = String.valueOf(fileVersion.getFileVersionId());

		long ghostscriptTimeout =
			PropsValues.DL_FILE_ENTRY_PREVIEW_GENERATION_TIMEOUT_GHOSTSCRIPT;

		if (_log.isDebugEnabled()) {
			if (thumbnail) {
				_log.debug(
					StringBundler.concat(
						"Waiting for ", ghostscriptTimeout,
						" seconds to generate thumbnail for ", file.getPath()));
			}
			else {
				_log.debug(
					StringBundler.concat(
						"Waiting for ", ghostscriptTimeout,
						" seconds to generate preview for ", file.getPath()));
			}
		}

		try {
			future.get(ghostscriptTimeout, TimeUnit.SECONDS);

			futures.put(processIdentity, future);
		}
		catch (TimeoutException timeoutException) {
			String errorMessage =
				"Timeout when generating preview for " + file.getPath();

			if (thumbnail) {
				errorMessage =
					"Timeout when generating thumbanil for " + file.getPath();
			}

			if (future.cancel(true)) {
				errorMessage += " resulted in a canceled timeout for " + future;
			}

			_fileVersionPreviewEventListener.onFailure(fileVersion);

			_log.error(errorMessage);

			throw timeoutException;
		}
		catch (Exception exception) {
			_fileVersionPreviewEventListener.onFailure(fileVersion);

			_log.error(exception);

			throw exception;
		}

		// Store images

		if (thumbnail) {
			File thumbnailTempFile = getThumbnailTempFile(tempFileId);

			try {
				storeThumbnailImages(fileVersion, thumbnailTempFile);
			}
			finally {
				FileUtil.delete(thumbnailTempFile);
			}
		}
		else {
			int total = getPreviewTempFileCount(fileVersion);

			for (int i = 0; i < total; i++) {
				File previewTempFile = getPreviewTempFile(tempFileId, i + 2);

				try {
					addFileToStore(
						fileVersion.getCompanyId(), PREVIEW_PATH,
						getPreviewFilePath(fileVersion, i + 1),
						previewTempFile);

					_fileVersionPreviewEventListener.onSuccess(fileVersion);
				}
				finally {
					FileUtil.delete(previewTempFile);
				}
			}
		}
	}

	private void _generateImagesGS(
			FileVersion fileVersion, InputStream inputStream,
			int maxNumberOfPages)
		throws Exception {

		File file = null;

		try {
			file = FileUtil.createTempFile(inputStream);

			_generateImagesGS(fileVersion, file, maxNumberOfPages);
		}
		finally {
			FileUtil.delete(file);
		}
	}

	private void _generateImagesPB(
			FileVersion fileVersion, File file, int maxNumberOfPages)
		throws Exception {

		String tempFileId = DLUtil.getTempFileId(
			fileVersion.getFileEntryId(), fileVersion.getVersion());

		File[] previewFiles = null;
		File thumbnailFile = null;

		long start = 0;

		boolean generatePreview = _isGeneratePreview(fileVersion);
		boolean generateThumbnail = _isGenerateThumbnail(fileVersion);

		try (PDDocument pdDocument = _openPDDocument(file)) {
			int previewFilesCount = maxNumberOfPages;

			if ((previewFilesCount == 0) ||
				(previewFilesCount > pdDocument.getNumberOfPages())) {

				previewFilesCount = pdDocument.getNumberOfPages();
			}

			if (previewFilesCount == 0) {
				if (_log.isWarnEnabled()) {
					_log.warn(
						"Unable to decrypt PDF document for file version " +
							fileVersion.getFileVersionId());
				}

				_fileVersionPreviewEventListener.onFailure(fileVersion);

				return;
			}

			thumbnailFile = getThumbnailTempFile(tempFileId);

			previewFiles = new File[previewFilesCount];

			for (int i = 0; i < previewFilesCount; i++) {
				previewFiles[i] = getPreviewTempFile(tempFileId, i);
			}

			start = System.currentTimeMillis();

			try {
				if (PropsValues.DL_FILE_ENTRY_PREVIEW_FORK_PROCESS_ENABLED) {
					File decryptedFile = file;

					if (pdDocument.isEncrypted()) {
						decryptedFile = getDecryptedTempFile(tempFileId);

						pdDocument.setAllSecurityToBeRemoved(true);

						pdDocument.save(decryptedFile);
					}

					ProcessCallable<String> processCallable =
						new LiferayPDFBoxProcessCallable(
							decryptedFile, thumbnailFile, previewFiles,
							getThumbnailType(fileVersion),
							getPreviewType(fileVersion),
							PropsValues.DL_FILE_ENTRY_PREVIEW_DOCUMENT_DPI,
							PropsValues.
								DL_FILE_ENTRY_PREVIEW_DOCUMENT_MAX_HEIGHT,
							PropsValues.
								DL_FILE_ENTRY_PREVIEW_DOCUMENT_MAX_WIDTH,
							generatePreview, generateThumbnail);

					ProcessChannel<String> processChannel =
						_processExecutor.execute(
							ProcessConfigUtil.getProcessConfig(),
							processCallable);

					Future<String> future =
						processChannel.getProcessNoticeableFuture();

					String processIdentity = String.valueOf(
						fileVersion.getFileVersionId());

					long pdfBoxTimeout =
						PropsValues.
							DL_FILE_ENTRY_PREVIEW_GENERATION_TIMEOUT_PDFBOX;

					if (_log.isDebugEnabled()) {
						if (generateThumbnail && generatePreview) {
							_log.debug(
								StringBundler.concat(
									"Waiting for ", pdfBoxTimeout,
									" seconds to generate thumbnail and ",
									"preview for ", decryptedFile.getPath()));
						}
						else {
							if (generateThumbnail) {
								_log.debug(
									StringBundler.concat(
										"Waiting for ", pdfBoxTimeout,
										" seconds to generate thumbnail for ",
										decryptedFile.getPath()));
							}

							if (generatePreview) {
								_log.debug(
									StringBundler.concat(
										"Waiting for ", pdfBoxTimeout,
										" seconds to generate preview for ",
										decryptedFile.getPath()));
							}
						}
					}

					try {
						future.get(pdfBoxTimeout, TimeUnit.SECONDS);

						futures.put(processIdentity, future);
					}
					catch (TimeoutException timeoutException) {
						String message = null;

						if (generateThumbnail && generatePreview) {
							message = StringBundler.concat(
								"Timeout when generating thumbnail and ",
								"preview for ", decryptedFile.getPath());
						}
						else {
							if (generateThumbnail) {
								message =
									"Timeout when generating thumbnail for " +
										decryptedFile.getPath();
							}

							if (generatePreview) {
								message =
									"Timeout when generating preview for " +
										decryptedFile.getPath();
							}
						}

						if (future.cancel(true)) {
							message +=
								" resulted in a canceled timeout for " + future;
						}

						_fileVersionPreviewEventListener.onFailure(fileVersion);

						if (_log.isWarnEnabled()) {
							_log.warn(message);
						}

						throw timeoutException;
					}
					finally {
						if (pdDocument.isEncrypted()) {
							FileUtil.delete(decryptedFile);
						}
					}
				}
				else {
					LiferayPDFBoxUtil.generateImagesPB(
						pdDocument, thumbnailFile, previewFiles,
						getPreviewType(fileVersion),
						getThumbnailType(fileVersion),
						PropsValues.DL_FILE_ENTRY_PREVIEW_DOCUMENT_DPI,
						PropsValues.DL_FILE_ENTRY_PREVIEW_DOCUMENT_MAX_HEIGHT,
						PropsValues.DL_FILE_ENTRY_PREVIEW_DOCUMENT_MAX_WIDTH,
						generatePreview, generateThumbnail, previewFilesCount);
				}
			}
			catch (TimeoutException timeoutException) {
				throw timeoutException;
			}
			catch (Exception exception) {
				_fileVersionPreviewEventListener.onFailure(fileVersion);

				if (_log.isWarnEnabled()) {
					_log.warn(
						StringBundler.concat(
							"Unable to process ",
							fileVersion.getFileVersionId(), " ",
							fileVersion.getTitle()));
				}

				if (_log.isDebugEnabled()) {
					_log.debug(exception);
				}

				throw exception;
			}

			if (generateThumbnail) {
				storeThumbnailImages(fileVersion, thumbnailFile);
			}

			if (generatePreview) {
				int index = 0;

				for (File previewFile : previewFiles) {
					addFileToStore(
						fileVersion.getCompanyId(), PREVIEW_PATH,
						getPreviewFilePath(fileVersion, index + 1),
						previewFile);

					_fileVersionPreviewEventListener.onSuccess(fileVersion);

					index++;
				}
			}
		}
		finally {
			FileUtil.delete(thumbnailFile);

			for (File previewFile : previewFiles) {
				FileUtil.delete(previewFile);
			}
		}

		if (!_log.isInfoEnabled()) {
			return;
		}

		long fileVersionId = fileVersion.getFileVersionId();
		int previewFileCount = getPreviewFileCount(fileVersion);
		long time = System.currentTimeMillis() - start;

		if (generateThumbnail && generatePreview) {
			_log.info(
				StringBundler.concat(
					"PDFBox generated a thumbnail and ", previewFileCount,
					" preview pages for ", fileVersionId, " in ", time, " ms"));
		}
		else {
			if (generateThumbnail) {
				_log.info(
					StringBundler.concat(
						"PDFBox generated a thumbnail for ", fileVersionId,
						" in ", time, " ms"));
			}

			if (generatePreview) {
				_log.info(
					StringBundler.concat(
						"PDFBox generated ", previewFileCount,
						" preview pages for ", fileVersionId, " in ", time,
						" ms"));
			}
		}
	}

	private void _generateImagesPB(
			FileVersion fileVersion, InputStream inputStream,
			int maxNumberOfPages)
		throws Exception {

		File file = null;

		try {
			file = FileUtil.createTempFile(inputStream);

			_generateImagesPB(fileVersion, file, maxNumberOfPages);
		}
		finally {
			FileUtil.delete(file);
		}
	}

	private Map<String, Integer> _getScaledDimensions(File file)
		throws Exception {

		try (PDDocument pdDocument = PDDocument.load(file)) {
			PDDocumentCatalog pdDocumentCatalog =
				pdDocument.getDocumentCatalog();

			PDPageTree pages = pdDocumentCatalog.getPages();

			PDPage pdPage = pages.get(0);

			PDRectangle pdRectangle = pdPage.getMediaBox();

			float height = pdRectangle.getHeight();
			float width = pdRectangle.getWidth();

			return HashMapBuilder.put(
				"height",
				() -> {
					double widthFactor =
						(double)
							PropsValues.
								DL_FILE_ENTRY_PREVIEW_DOCUMENT_MAX_WIDTH /
									width;

					return (int)Math.round(widthFactor * height);
				}
			).put(
				"width",
				() -> {
					double heightFactor =
						(double)
							PropsValues.
								DL_FILE_ENTRY_PREVIEW_DOCUMENT_MAX_HEIGHT /
									height;

					return (int)Math.round(heightFactor * width);
				}
			).build();
		}
	}

	private boolean _isGeneratePreview(FileVersion fileVersion)
		throws Exception {

		if (PropsValues.DL_FILE_ENTRY_PREVIEW_ENABLED &&
			!hasPreview(fileVersion)) {

			return true;
		}

		return false;
	}

	private boolean _isGenerateThumbnail(FileVersion fileVersion) {
		if (PropsValues.DL_FILE_ENTRY_THUMBNAIL_ENABLED &&
			!hasThumbnail(fileVersion, THUMBNAIL_INDEX_DEFAULT)) {

			return true;
		}

		return false;
	}

	private PDDocument _openPDDocument(File file) throws IOException {
		for (String decryptPassword :
				PropsValues.
					DL_FILE_ENTRY_PREVIEW_GENERATION_DECRYPT_PASSWORDS_PDFBOX) {

			try {
				return PDDocument.load(file, decryptPassword);
			}
			catch (IOException ioException) {
				if (!(ioException instanceof InvalidPasswordException)) {
					_log.error(ioException);
				}
			}
		}

		return PDDocument.load(file);
	}

	private static final Log _log = LogFactoryUtil.getLog(
		PDFProcessor.class);

	@Reference
	private BackgroundTaskManager _backgroundTaskManager;

	@Reference
	private CompanyLocalService _companyLocalService;

	@Reference
	private FileVersionPreviewEventListener _fileVersionPreviewEventListener;

	@Reference
	private Ghostscript _ghostscript;

	private boolean _ghostscriptInitialized;

	@Reference
	private PDFPreviewManagedServiceFactory _pdfPreviewManagedServiceFactory;

	@Reference
	private PortalUUID _portalUUID;

	@Reference
	private ProcessExecutor _processExecutor;

	private static class LiferayPDFBoxProcessCallable
		implements ProcessCallable<String> {

		@Override
		public String call() throws ProcessException {
			Properties systemProperties = System.getProperties();

			SystemEnv.setProperties(systemProperties);

			Logger logger = Logger.getLogger("org.apache.pdfbox");

			logger.setLevel(Level.SEVERE);

			logger = Logger.getLogger(PropsUtil.class.getName());

			logger.setLevel(Level.WARNING);

			try (PDDocument pdDocument = PDDocument.load(_inputFile)) {
				LiferayPDFBoxUtil.generateImagesPB(
					pdDocument, _thumbnailFile, _previewFiles, _extension,
					_thumbnailExtension, _dpi, _height, _width,
					_generatePreview, _generateThumbnail, _previewFiles.length);
			}
			catch (Exception exception) {
				throw new ProcessException(exception);
			}

			return StringPool.BLANK;
		}

		private LiferayPDFBoxProcessCallable(
			File inputFile, File thumbnailFile, File[] previewFiles,
			String extension, String thumbnailExtension, int dpi, int height,
			int width, boolean generatePreview, boolean generateThumbnail) {

			_inputFile = inputFile;
			_thumbnailFile = thumbnailFile;
			_previewFiles = previewFiles;
			_extension = extension;
			_thumbnailExtension = thumbnailExtension;
			_dpi = dpi;
			_height = height;
			_width = width;
			_generatePreview = generatePreview;
			_generateThumbnail = generateThumbnail;
		}

		private static final long serialVersionUID = 1L;

		private final int _dpi;
		private final String _extension;
		private final boolean _generatePreview;
		private final boolean _generateThumbnail;
		private final int _height;
		private final File _inputFile;
		private final File[] _previewFiles;
		private final String _thumbnailExtension;
		private final File _thumbnailFile;
		private final int _width;

	}

}