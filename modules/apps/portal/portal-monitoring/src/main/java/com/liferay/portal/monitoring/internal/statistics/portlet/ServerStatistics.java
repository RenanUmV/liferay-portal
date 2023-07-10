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

package com.liferay.portal.monitoring.internal.statistics.portlet;

import com.liferay.portal.kernel.model.Company;
import com.liferay.portal.kernel.monitoring.DataSampleProcessor;
import com.liferay.portal.kernel.monitoring.MonitoringException;
import com.liferay.portal.kernel.service.CompanyLocalService;
import com.liferay.portal.monitoring.internal.statistics.util.ServerStaticsUtil;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

/**
 * @author Michael C. Han
 * @author Brian Wing Shun Chan
 */
@Component(
	enabled = false, property = "namespace=com.liferay.monitoring.Portlet",
	service = DataSampleProcessor.class
)
public class ServerStatistics
	implements DataSampleProcessor<PortletRequestDataSample> {

	@Override
	public void processDataSample(
			PortletRequestDataSample portletRequestDataSample)
		throws MonitoringException {

		long companyId = portletRequestDataSample.getCompanyId();

		CompanyStatistics companyStatistics =
			ServerStaticsUtil.getCompanyStatisticsByCompanyId(
			).get(
				companyId
			);

		if (companyStatistics == null) {
			try {
				Company company = _companyLocalService.getCompany(companyId);

				companyStatistics = ServerStaticsUtil.register(
					company.getWebId(), _companyLocalService);
			}
			catch (Exception exception) {
				throw new IllegalStateException(
					"Unable to get company with company ID " + companyId,
					exception);
			}
		}

		companyStatistics.processDataSample(portletRequestDataSample);
	}

	@Activate
	protected void activate() {
		CompanyStatistics companyStatistics = new CompanyStatistics();

		ServerStaticsUtil.setCompanyStatisticsByCompanyId(companyStatistics);

		ServerStaticsUtil.setCompanyStatisticsByWebId(companyStatistics);
	}

	@Reference
	private CompanyLocalService _companyLocalService;

}