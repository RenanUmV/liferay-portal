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

import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.model.Company;
import com.liferay.portal.kernel.monitoring.MonitoringException;
import com.liferay.portal.kernel.service.CompanyLocalService;
import com.liferay.portal.kernel.util.ArrayUtil;
import com.liferay.portal.monitoring.internal.statistics.portal.PortalCompanyStatistics;
import com.liferay.portal.monitoring.internal.statistics.portlet.PortletCompanyStatistics;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

/**
 * @author Renan Vasconcelos
 */
@Component(service = ServerStaticsHelper.class)
public class ServerStaticsHelper {

	public Company getCompanyByCompanyId(long companyId)
		throws PortalException {

		return _companyLocalService.getCompany(companyId);
	}

	public Set<Long> getPortalCompanyIds() {
		return _portalCompanyStatisticsByCompanyId.keySet();
	}

	public PortalCompanyStatistics getPortalCompanyStatistics(long companyId)
		throws MonitoringException {

		PortalCompanyStatistics portalCompanyStatistics =
			_portalCompanyStatisticsByCompanyId.get(companyId);

		if (portalCompanyStatistics == null) {
			throw new MonitoringException(
				"No statistics found for company ID " + companyId);
		}

		return portalCompanyStatistics;
	}

	public PortalCompanyStatistics getPortalCompanyStatisticsByCompanyId(long companyId) {

		return _portalCompanyStatisticsByCompanyId.get(companyId);
	}

	public PortalCompanyStatistics getPortalCompanyStatistics(String webId)
		throws MonitoringException {

		PortalCompanyStatistics portalCompanyStatistics =
			_portalCompanyStatisticsByWebId.get(webId);

		if (portalCompanyStatistics == null) {
			throw new MonitoringException(
				"No statistics found for web ID " + webId);
		}

		return portalCompanyStatistics;
	}

	public Set<PortalCompanyStatistics> getPortalCompanyStatisticsSet() {
		return new HashSet<>(_portalCompanyStatisticsByWebId.values());
	}

	public Set<String> getPortalWebIds() {
		return _portalCompanyStatisticsByWebId.keySet();
	}

	public Set<Long> getPortletCompanyIds() {
		return _portletCompanyStatisticsByCompanyId.keySet();
	}

	public PortletCompanyStatistics getPortletCompanyStatistics(long companyId)
		throws MonitoringException {

		PortletCompanyStatistics portletCompanyStatistics =
			_portletCompanyStatisticsByCompanyId.get(companyId);

		if (portletCompanyStatistics == null) {
			throw new MonitoringException(
				"No statistics found for company ID " + companyId);
		}

		return portletCompanyStatistics;
	}

	public PortletCompanyStatistics getPortletCompanyStatistics(String webId)
		throws MonitoringException {

		PortletCompanyStatistics portletCompanyStatistics =
			_portletCompanyStatisticsByWebId.get(webId);

		if (portletCompanyStatistics == null) {
			throw new MonitoringException(
				"No statistics found for web ID " + webId);
		}

		return portletCompanyStatistics;
	}

	public Map<Long, PortletCompanyStatistics>
		getPortletCompanyStatisticsByCompanyId() {

		return _portletCompanyStatisticsByCompanyId;
	}

	public Map<String, PortletCompanyStatistics>
		getPortletCompanyStatisticsByWebId() {

		return _portletCompanyStatisticsByWebId;
	}

	public Set<PortletCompanyStatistics> getPortletCompanyStatisticsSet() {
		return new HashSet<>(_portletCompanyStatisticsByWebId.values());
	}

	public Set<String> getPortletIds() {
		Set<String> portletIds = new HashSet<>();

		for (PortletCompanyStatistics containerStatistics :
				_portletCompanyStatisticsByWebId.values()) {

			portletIds.addAll(containerStatistics.getPortletIds());
		}

		return portletIds;
	}

	public Set<String> getPortletWebIds() {
		return _portletCompanyStatisticsByWebId.keySet();
	}

	public synchronized PortalCompanyStatistics registerPortalCompanyStatistics(
		String webId) {

		PortalCompanyStatistics portalCompanyStatistics =
			new PortalCompanyStatistics(_companyLocalService, webId);

		_portalCompanyStatisticsByCompanyId.put(
			portalCompanyStatistics.getCompanyId(), portalCompanyStatistics);
		_portalCompanyStatisticsByWebId.put(webId, portalCompanyStatistics);

		return portalCompanyStatistics;
	}

	public synchronized PortletCompanyStatistics
		registerPortletCompanyStatistics(String webId) {

		PortletCompanyStatistics portletCompanyStatistics =
			new PortletCompanyStatistics(_companyLocalService, webId);

		_portletCompanyStatisticsByCompanyId.put(
			portletCompanyStatistics.getCompanyId(), portletCompanyStatistics);

		_portletCompanyStatisticsByWebId.put(
			portletCompanyStatistics.getWebId(), portletCompanyStatistics);

		return portletCompanyStatistics;
	}

	public void resetPortalCompanyStatistics() {
		_companyLocalService.forEachCompanyId(
			companyId -> resetPortalCompanyStatistics(companyId),
			ArrayUtil.toLongArray(
				_portalCompanyStatisticsByCompanyId.keySet()));
	}

	public void resetPortalCompanyStatistics(long companyId) {
		PortalCompanyStatistics portalCompanyStatistics =
			_portalCompanyStatisticsByCompanyId.get(companyId);

		if (portalCompanyStatistics == null) {
			return;
		}

		portalCompanyStatistics.reset();
	}

	public void resetPortalCompanyStatistics(String webId) {
		PortalCompanyStatistics portalCompanyStatistics =
			_portalCompanyStatisticsByWebId.get(webId);

		if (portalCompanyStatistics == null) {
			return;
		}

		portalCompanyStatistics.reset();
	}

	public void resetPortletCompanyStatistics() {
		_companyLocalService.forEachCompanyId(
			companyId -> resetPortletCompanyStatistics(companyId),
			ArrayUtil.toLongArray(
				_portletCompanyStatisticsByCompanyId.keySet()));
	}

	public void resetPortletCompanyStatistics(long companyId) {
		PortletCompanyStatistics portletCompanyStatistics =
			_portletCompanyStatisticsByCompanyId.get(companyId);

		if (portletCompanyStatistics == null) {
			return;
		}

		portletCompanyStatistics.reset();
	}

	public void resetPortletCompanyStatistics(String webId) {
		PortletCompanyStatistics portletCompanyStatistics =
			_portletCompanyStatisticsByCompanyId.get(webId);

		if (portletCompanyStatistics == null) {
			return;
		}

		portletCompanyStatistics.reset();
	}

	public synchronized void unregisterPortalCompanyStatistics(String webId) {
		PortalCompanyStatistics portalCompanyStatistics =
			_portalCompanyStatisticsByWebId.remove(webId);

		if (portalCompanyStatistics != null) {
			_portalCompanyStatisticsByCompanyId.remove(
				portalCompanyStatistics.getCompanyId());
		}
	}

	public synchronized void unregisterPortletCompanyStatistics(String webId) {
		PortletCompanyStatistics portletCompanyStatistics =
			_portletCompanyStatisticsByCompanyId.remove(webId);

		if (portletCompanyStatistics != null) {
			_portletCompanyStatisticsByWebId.remove(
				portletCompanyStatistics.getCompanyId());
		}
	}

	@Activate
	protected void activate() {
		PortalCompanyStatistics portalCompanyStatistics =
			new PortalCompanyStatistics();

		PortletCompanyStatistics portletCompanyStatistics =
			new PortletCompanyStatistics();

		_portalCompanyStatisticsByCompanyId.put(
			portalCompanyStatistics.getCompanyId(), portalCompanyStatistics);
		_portalCompanyStatisticsByWebId.put(
			portalCompanyStatistics.getWebId(), portalCompanyStatistics);

		_portletCompanyStatisticsByCompanyId.put(
			portletCompanyStatistics.getCompanyId(), portletCompanyStatistics);
		_portletCompanyStatisticsByWebId.put(
			portletCompanyStatistics.getWebId(), portletCompanyStatistics);
	}

	@Reference
	private CompanyLocalService _companyLocalService;

	private final Map<Long, PortalCompanyStatistics>
		_portalCompanyStatisticsByCompanyId = new TreeMap<>();
	private final Map<String, PortalCompanyStatistics>
		_portalCompanyStatisticsByWebId = new TreeMap<>();
	private final Map<Long, PortletCompanyStatistics>
		_portletCompanyStatisticsByCompanyId = new TreeMap<>();
	private final Map<String, PortletCompanyStatistics>
		_portletCompanyStatisticsByWebId = new TreeMap<>();

}