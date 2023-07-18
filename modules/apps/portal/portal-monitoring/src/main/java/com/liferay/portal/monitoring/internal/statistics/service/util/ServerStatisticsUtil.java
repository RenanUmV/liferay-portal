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

package com.liferay.portal.monitoring.internal.statistics.service.util;

import com.liferay.portal.monitoring.internal.statistics.service.ServiceStatistics;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author Renan Vasconcelos
 */
public class ServerStatisticsUtil {

	public static long getAverageTime(
		String className, String methodName, String[] parameterTypes) {

		ServiceStatistics serviceStatistics = _serviceStatistics.get(className);

		if (serviceStatistics != null) {
			return serviceStatistics.getAverageTime(methodName, parameterTypes);
		}

		return -1;
	}

	public static long getErrorCount(
		String className, String methodName, String[] parameterTypes) {

		ServiceStatistics serviceStatistics = _serviceStatistics.get(className);

		if (serviceStatistics != null) {
			return serviceStatistics.getErrorCount(methodName, parameterTypes);
		}

		return -1;
	}

	public static long getMaxTime(
		String className, String methodName, String[] parameterTypes) {

		ServiceStatistics serviceStatistics = _serviceStatistics.get(className);

		if (serviceStatistics != null) {
			return serviceStatistics.getMaxTime(methodName, parameterTypes);
		}

		return -1;
	}

	public static long getMinTime(
		String className, String methodName, String[] parameterTypes) {

		ServiceStatistics serviceStatistics = _serviceStatistics.get(className);

		if (serviceStatistics != null) {
			return serviceStatistics.getMinTime(methodName, parameterTypes);
		}

		return -1;
	}

	public static long getRequestCount(
		String className, String methodName, String[] parameterTypes) {

		ServiceStatistics serviceStatistics = _serviceStatistics.get(className);

		if (serviceStatistics != null) {
			return serviceStatistics.getRequestCount(
				methodName, parameterTypes);
		}

		return -1;
	}

	public static ServiceStatistics getServiceStatistics(String className) {
		return _serviceStatistics.get(className);
	}

	public static ServiceStatistics setServiceStatistics(
		String className, ServiceStatistics serviceStatistics) {

		return _serviceStatistics.put(className, serviceStatistics);
	}

	private static final Map<String, ServiceStatistics> _serviceStatistics =
		new ConcurrentHashMap<>();

}