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

package com.liferay.message.boards.web.internal.util;

import com.liferay.message.boards.web.internal.display.context.MBDisplayContextProvider;
import com.liferay.osgi.util.service.Snapshot;

/**
 * @author Iván Zaera
 */
public class MBWebComponentProvider {

	public static MBWebComponentProvider getMBWebComponentProvider() {
		return _mbWebComponentProvider;
	}

	public MBDisplayContextProvider getMBDisplayContextProvider() {
		return _mbDisplayContextProviderSnapshot.get();
	}

	private static final Snapshot<MBDisplayContextProvider>
		_mbDisplayContextProviderSnapshot = new Snapshot<>(
			MBWebComponentProvider.class, MBDisplayContextProvider.class);
	private static MBWebComponentProvider _mbWebComponentProvider;

}