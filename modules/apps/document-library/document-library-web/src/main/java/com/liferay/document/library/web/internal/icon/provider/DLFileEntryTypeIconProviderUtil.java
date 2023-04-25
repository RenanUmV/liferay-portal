/**
 * Copyright (c) 2000-present Liferay, Inc. All rights reserved.
 *
 * This library is free software; you can redistribute it and/or modify it under
 * the terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation; either version 2.1 of the License, or (at your option)
 * any later version.
 *
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 * details.
 */

package com.liferay.document.library.web.internal.icon.provider;

import com.liferay.document.library.icon.provider.DLFileEntryTypeIconProvider;
import com.liferay.document.library.kernel.model.DLFileEntryType;
import com.liferay.osgi.service.tracker.collections.map.ServiceTrackerMap;
import com.liferay.osgi.service.tracker.collections.map.ServiceTrackerMapFactory;

import org.osgi.framework.Bundle;
import org.osgi.framework.FrameworkUtil;

/**
 * @author Alejandro Tardín
 */
public class DLFileEntryTypeIconProviderUtil {

	public static String getIcon(DLFileEntryType fileEntryType) {
		DLFileEntryTypeIconProvider dlFileEntryTypeIconProvider =
			_serviceTrackerMap.getService(fileEntryType.getFileEntryTypeKey());

		if (dlFileEntryTypeIconProvider != null) {
			return dlFileEntryTypeIconProvider.getIcon();
		}

		return "file-template";
	}

	private static final ServiceTrackerMap<String, DLFileEntryTypeIconProvider>
		_serviceTrackerMap;

	static {
		Bundle bundle = FrameworkUtil.getBundle(
			DLFileEntryTypeIconProviderUtil.class);

		_serviceTrackerMap = ServiceTrackerMapFactory.openSingleValueMap(
			bundle.getBundleContext(), DLFileEntryTypeIconProvider.class,
			"file.entry.type.key");
	}

}