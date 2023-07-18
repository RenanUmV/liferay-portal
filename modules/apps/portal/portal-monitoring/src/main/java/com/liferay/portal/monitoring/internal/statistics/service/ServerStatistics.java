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

package com.liferay.portal.monitoring.internal.statistics.service;

import com.liferay.portal.kernel.monitoring.DataSampleProcessor;
import com.liferay.portal.kernel.monitoring.MethodSignature;
import com.liferay.portal.monitoring.internal.statistics.service.util.ServerStatisticsUtil;

import org.osgi.service.component.annotations.Component;

/**
 * @author Michael C. Han
 */
@Component(
	enabled = false, property = "namespace=com.liferay.monitoring.Service",
	service = DataSampleProcessor.class
)
public class ServerStatistics
	implements DataSampleProcessor<ServiceRequestDataSample> {

	@Override
	public void processDataSample(
		ServiceRequestDataSample serviceRequestDataSample) {

		MethodSignature methodSignature =
			serviceRequestDataSample.getMethodSignature();

		String className = methodSignature.getClassName();

		ServiceStatistics serviceStatistics =
			ServerStatisticsUtil.getServiceStatistics(className);

		if (serviceStatistics == null) {
			serviceStatistics = new ServiceStatistics(className);

			ServerStatisticsUtil.setServiceStatistics(
				className, serviceStatistics);
		}

		serviceStatistics.processDataSample(serviceRequestDataSample);
	}

}