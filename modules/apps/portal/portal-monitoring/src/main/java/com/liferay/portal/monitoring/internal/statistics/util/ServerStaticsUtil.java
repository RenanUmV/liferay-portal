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

package com.liferay.portal.monitoring.internal.statistics.util;

import com.liferay.portal.kernel.model.Company;
import com.liferay.portal.kernel.monitoring.MonitoringException;
import com.liferay.portal.kernel.service.CompanyLocalService;
import com.liferay.portal.kernel.util.ArrayUtil;
import com.liferay.portal.monitoring.internal.statistics.portlet.CompanyStatistics;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * @author Michael C. Han
 * @author Brian Wing Shun Chan
 */
public class ServerStaticsUtil {

	public static Set<Long> getCompanyIds() {
		return _companyStatisticsByCompanyId.keySet();
	}

	public static CompanyStatistics getCompanyStatistics(long companyId)
		throws MonitoringException {

		CompanyStatistics companyStatistics = _companyStatisticsByCompanyId.get(
			companyId);

		if (companyStatistics == null) {
			throw new MonitoringException(
				"No statistics found for company ID " + companyId);
		}

		return companyStatistics;
	}

	public static CompanyStatistics getCompanyStatistics(String webId)
		throws MonitoringException {

		CompanyStatistics companyStatistics = _companyStatisticsByWebId.get(
			webId);

		if (companyStatistics == null) {
			throw new MonitoringException(
				"No statistics found for web ID " + webId);
		}

		return companyStatistics;
	}

	public static Map<Long, CompanyStatistics>
		getCompanyStatisticsByCompanyId() {

		return _companyStatisticsByCompanyId;
	}

	public static Map<String, CompanyStatistics> getCompanyStatisticsByWebId() {
		return _companyStatisticsByWebId;
	}

	public static Set<CompanyStatistics> getCompanyStatisticsSet() {
		return new HashSet<>(_companyStatisticsByWebId.values());
	}

	public static Set<String> getPortletIds() {
		Set<String> portletIds = new HashSet<>();

		for (CompanyStatistics containerStatistics :
				_companyStatisticsByWebId.values()) {

			portletIds.addAll(containerStatistics.getPortletIds());
		}

		return portletIds;
	}

	public static Set<String> getWebIds() {
		return _companyStatisticsByWebId.keySet();
	}

	public static synchronized CompanyStatistics register(
		String webId, CompanyLocalService companyLocalService) {

		CompanyStatistics companyStatistics = new CompanyStatistics(
			companyLocalService, webId);

		setCompanyStatisticsByCompanyId(companyStatistics);
		setCompanyStatisticsByWebId(companyStatistics);

		return companyStatistics;
	}

	public static void reset(CompanyLocalService companyLocalService) {
		companyLocalService.forEachCompanyId(
			companyId -> reset(companyId),
			ArrayUtil.toLongArray(_companyStatisticsByCompanyId.keySet()));
	}

	public static void reset(long companyId) {
		CompanyStatistics companyStatistics = _companyStatisticsByCompanyId.get(
			companyId);

		if (companyStatistics == null) {
			return;
		}

		companyStatistics.reset();
	}

	public static void reset(String webId) {
		CompanyStatistics companyStatistics = _companyStatisticsByWebId.get(
			webId);

		if (companyStatistics == null) {
			return;
		}

		companyStatistics.reset();
	}

	public static CompanyStatistics setCompanyStatisticsByCompanyId(
		CompanyStatistics companyStatistics) {

		return _companyStatisticsByCompanyId.put(
			companyStatistics.getCompanyId(), companyStatistics);
	}

	public static CompanyStatistics setCompanyStatisticsByWebId(
		CompanyStatistics companyStatistics) {

		return _companyStatisticsByWebId.put(
			companyStatistics.getWebId(), companyStatistics);
	}

	public static synchronized void unregister(String webId) {
		CompanyStatistics companyStatistics = _companyStatisticsByWebId.remove(
			webId);

		if (companyStatistics != null) {
			_companyStatisticsByCompanyId.remove(
				companyStatistics.getCompanyId());
		}
	}

	private static final Map<Long, CompanyStatistics>
		_companyStatisticsByCompanyId = new TreeMap<>();
	private static final Map<String, CompanyStatistics>
		_companyStatisticsByWebId = new TreeMap<>();

}