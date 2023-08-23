/**
 * SPDX-FileCopyrightText: (c) 2023 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.document.library.preview.pdf.internal;

import com.liferay.document.library.kernel.document.conversion.DocumentConversionUtil;
import com.liferay.document.library.kernel.model.DLProcessorConstants;
import com.liferay.document.library.kernel.store.Store;
import com.liferay.document.library.kernel.util.DLPreviewableProcessor;
import com.liferay.document.library.kernel.util.DLProcessor;
import com.liferay.document.library.kernel.util.DLProcessorRegistryUtil;
import com.liferay.document.library.kernel.util.PDFProcessor;
import com.liferay.exportimport.kernel.lar.PortletDataContext;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.messaging.DestinationNames;
import com.liferay.portal.kernel.repository.model.FileEntry;
import com.liferay.portal.kernel.repository.model.FileVersion;
import com.liferay.portal.kernel.util.ContentTypes;
import com.liferay.portal.kernel.util.FileUtil;
import com.liferay.portal.kernel.util.GetterUtil;
import com.liferay.portal.kernel.util.MimeTypesUtil;
import com.liferay.portal.kernel.util.Validator;
import com.liferay.portal.kernel.xml.Element;
import com.liferay.portal.util.PropsValues;

import java.io.InputStream;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.Vector;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

/**
 * @author Renan Vasconcelos
 */
@Component(
	property = "type=" + DLProcessorConstants.PDF_PROCESSOR,
	service = DLProcessor.class
)
public class PDFDLProcessor extends DLPreviewableProcessor {

	@Override
	public void afterPropertiesSet() {
		FileUtil.mkdirs(DECRYPT_TMP_PATH);
		FileUtil.mkdirs(PREVIEW_TMP_PATH);
		FileUtil.mkdirs(THUMBNAIL_TMP_PATH);
	}

	@Override
	public void destroy() {
		FileUtil.deltree(TMP_PATH);
	}

	public int getPreviewFileCount(FileVersion fileVersion) {
		try {
			return doGetPreviewFileCount(fileVersion);
		}
		catch (Exception exception) {
			_log.error(exception);
		}

		return 0;
	}

	@Override
	public String getType() {
		return DLProcessorConstants.PDF_PROCESSOR;
	}

	public boolean hasImages(FileVersion fileVersion) {
		boolean hasImages = false;

		try {
			hasImages = hasImage(fileVersion);

			if (!hasImages && isSupported(fileVersion)) {
				queueGeneration(null, fileVersion);
			}
		}
		catch (Exception exception) {
			_log.error(exception);
		}

		return hasImages;
	}

	@Override
	public boolean isSupported(FileVersion fileVersion) {
		if ((fileVersion == null) || (fileVersion.getSize() == 0) ||
			!DLProcessorRegistryUtil.isPreviewableSize(fileVersion)) {

			return false;
		}

		return isSupported(fileVersion.getMimeType());
	}

	@Override
	public boolean isSupported(String mimeType) {
		if (Validator.isNull(mimeType)) {
			return false;
		}

		if (mimeType.equals(ContentTypes.APPLICATION_PDF) ||
			mimeType.equals(ContentTypes.APPLICATION_X_PDF)) {

			return true;
		}

		if (DocumentConversionUtil.isEnabled()) {
			Set<String> extensions = MimeTypesUtil.getExtensions(mimeType);

			for (String extension : extensions) {
				extension = extension.substring(1);

				String[] targetExtensions =
					DocumentConversionUtil.getConversions(extension);

				if (Arrays.binarySearch(targetExtensions, "pdf") >= 0) {
					return true;
				}
			}
		}

		return false;
	}

	@Override
	protected void copyPreviews(
		FileVersion sourceFileVersion, FileVersion destinationFileVersion) {

		if (!PropsValues.DL_FILE_ENTRY_PREVIEW_ENABLED) {
			return;
		}

		try {
			if (hasPreview(sourceFileVersion) &&
				!hasPreview(destinationFileVersion)) {

				int count = getPreviewFileCount(sourceFileVersion);

				for (int i = 0; i < count; i++) {
					String previewFilePath = getPreviewFilePath(
						destinationFileVersion, i + 1);

					InputStream inputStream = doGetPreviewAsStream(
						sourceFileVersion, i + 1, PDFProcessor.PREVIEW_TYPE);

					addFileToStore(
						destinationFileVersion.getCompanyId(), PREVIEW_PATH,
						previewFilePath, inputStream);
				}
			}
		}
		catch (Exception exception) {
			_log.error(exception);
		}
	}

	@Override
	protected void doExportGeneratedFiles(
			PortletDataContext portletDataContext, FileEntry fileEntry,
			Element fileEntryElement)
		throws Exception {

		exportThumbnails(
			portletDataContext, fileEntry, fileEntryElement, "pdf");

		exportPreviews(portletDataContext, fileEntry, fileEntryElement);
	}

	@Override
	protected void doImportGeneratedFiles(
			PortletDataContext portletDataContext, FileEntry fileEntry,
			FileEntry importedFileEntry, Element fileEntryElement)
		throws Exception {

		importThumbnails(
			portletDataContext, fileEntry, importedFileEntry, fileEntryElement,
			"pdf");

		importPreviews(
			portletDataContext, fileEntry, importedFileEntry, fileEntryElement);
	}

	protected void exportPreviews(
			PortletDataContext portletDataContext, FileEntry fileEntry,
			Element fileEntryElement)
		throws Exception {

		FileVersion fileVersion = fileEntry.getFileVersion();

		if (!isSupported(fileVersion) || !hasImage(fileVersion)) {
			return;
		}

		if (!portletDataContext.isPerformDirectBinaryImport()) {
			int previewFileCount = getPreviewFileCount(fileVersion);

			fileEntryElement.addAttribute(
				"bin-path-pdf-preview-count", String.valueOf(previewFileCount));

			for (int i = 0; i < previewFileCount; i++) {
				exportPreview(
					portletDataContext, fileEntry, fileEntryElement, "pdf",
					PDFProcessor.PREVIEW_TYPE, i);
			}
		}
	}

	@Override
	protected List<Long> getFileVersionIds() {
		return fileVersionIds;
	}

	@Override
	protected String getPreviewType(FileVersion fileVersion) {
		return PDFProcessor.PREVIEW_TYPE;
	}

	@Override
	protected String getThumbnailType(FileVersion fileVersion) {
		return PDFProcessor.THUMBNAIL_TYPE;
	}

	protected boolean hasImage(FileVersion fileVersion) throws Exception {
		if (PropsValues.DL_FILE_ENTRY_PREVIEW_ENABLED &&
			!hasPreview(fileVersion)) {

			return false;
		}

		return hasThumbnails(fileVersion);
	}

	protected boolean hasPreview(FileVersion fileVersion) throws Exception {
		return hasPreview(fileVersion, null);
	}

	@Override
	protected boolean hasPreview(FileVersion fileVersion, String type)
		throws Exception {

		return store.hasFile(
			fileVersion.getCompanyId(), REPOSITORY_ID,
			getPreviewFilePath(fileVersion, 1), Store.VERSION_DEFAULT);
	}

	protected void importPreviews(
			PortletDataContext portletDataContext, FileEntry fileEntry,
			FileEntry importedFileEntry, Element fileEntryElement)
		throws Exception {

		int previewFileCount = GetterUtil.getInteger(
			fileEntryElement.attributeValue("bin-path-pdf-preview-count"));

		for (int i = 0; i < previewFileCount; i++) {
			importPreview(
				portletDataContext, fileEntry, importedFileEntry,
				fileEntryElement, "pdf", PDFProcessor.PREVIEW_TYPE, i);
		}
	}

	protected void queueGeneration(
		FileVersion sourceFileVersion, FileVersion destinationFileVersion) {

		if (fileVersionIds.contains(
				destinationFileVersion.getFileVersionId())) {

			return;
		}

		boolean generateImages = false;

		String extension = destinationFileVersion.getExtension();

		if (extension.equals("pdf")) {
			generateImages = true;
		}
		else if (DocumentConversionUtil.isEnabled()) {
			String[] conversions = DocumentConversionUtil.getConversions(
				extension);

			for (String conversion : conversions) {
				if (conversion.equals("pdf")) {
					generateImages = true;

					break;
				}
			}
		}

		if (generateImages) {
			fileVersionIds.add(destinationFileVersion.getFileVersionId());

			sendGenerationMessage(
				DestinationNames.DOCUMENT_LIBRARY_PDF_PROCESSOR,
				sourceFileVersion, destinationFileVersion);
		}
	}

	protected final List<Long> fileVersionIds = new Vector<>();

	@Reference(target = "(default=true)")
	protected Store store;

	private static final Log _log = LogFactoryUtil.getLog(
		PDFDLProcessor.class);

}