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

import com.liferay.portal.kernel.monitoring.MonitoringException;
import com.liferay.portal.monitoring.internal.statistics.RequestStatistics;
import com.liferay.portal.monitoring.internal.statistics.util.ServerStaticsHelper;

import java.util.Set;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

/**
 * @author Michael C. Han
 * @author Brian Wing Shun Chan
 */
@Component(enabled = false, service = RenderRequestSummaryStatistics.class)
public class RenderRequestSummaryStatistics
	implements PortletSummaryStatistics {

	@Override
	public long getAverageTime() {
		long averageTime = 0;

		long count = 0;

		for (PortletCompanyStatistics portletCompanyStatistics :
				_serverStaticsHelper.getPortletCompanyStatisticsSet()) {

			for (RequestStatistics requestStatistics :
					portletCompanyStatistics.getRenderRequestStatisticsSet()) {

				averageTime += requestStatistics.getAverageTime();

				count++;
			}
		}

		if (count > 0) {
			return averageTime / count;
		}

		return 0;
	}

	@Override
	public long getAverageTimeByCompany(long companyId)
		throws MonitoringException {

		return getAverageTimeByCompany(
			_serverStaticsHelper.getPortletCompanyStatistics(companyId));
	}

	@Override
	public long getAverageTimeByCompany(String webId)
		throws MonitoringException {

		return getAverageTimeByCompany(
			_serverStaticsHelper.getPortletCompanyStatistics(webId));
	}

	@Override
	public long getAverageTimeByPortlet(String portletId)
		throws MonitoringException {

		long averageTime = 0;

		Set<PortletCompanyStatistics> portletCompanyStatisticsSet =
			_serverStaticsHelper.getPortletCompanyStatisticsSet();

		for (PortletCompanyStatistics portletCompanyStatistics :
				portletCompanyStatisticsSet) {

			RequestStatistics requestStatistics =
				portletCompanyStatistics.getRenderRequestStatistics(portletId);

			averageTime += requestStatistics.getAverageTime();
		}

		if (!portletCompanyStatisticsSet.isEmpty()) {
			return averageTime / portletCompanyStatisticsSet.size();
		}

		return averageTime;
	}

	@Override
	public long getAverageTimeByPortlet(String portletId, long companyId)
		throws MonitoringException {

		PortletCompanyStatistics portletCompanyStatistics =
			_serverStaticsHelper.getPortletCompanyStatistics(companyId);

		RequestStatistics requestStatistics =
			portletCompanyStatistics.getRenderRequestStatistics(portletId);

		return requestStatistics.getAverageTime();
	}

	@Override
	public long getAverageTimeByPortlet(String portletId, String webId)
		throws MonitoringException {

		PortletCompanyStatistics portletCompanyStatistics =
			_serverStaticsHelper.getPortletCompanyStatistics(webId);

		RequestStatistics requestStatistics =
			portletCompanyStatistics.getRenderRequestStatistics(portletId);

		return requestStatistics.getAverageTime();
	}

	@Override
	public long getErrorCount() {
		long errorCount = 0;

		for (PortletCompanyStatistics portletCompanyStatistics :
				_serverStaticsHelper.getPortletCompanyStatisticsSet()) {

			errorCount += getErrorCountByCompany(portletCompanyStatistics);
		}

		return errorCount;
	}

	@Override
	public long getErrorCountByCompany(long companyId)
		throws MonitoringException {

		return getErrorCountByCompany(
			_serverStaticsHelper.getPortletCompanyStatistics(companyId));
	}

	@Override
	public long getErrorCountByCompany(String webId)
		throws MonitoringException {

		return getErrorCountByCompany(
			_serverStaticsHelper.getPortletCompanyStatistics(webId));
	}

	@Override
	public long getErrorCountByPortlet(String portletId)
		throws MonitoringException {

		long errorCount = 0;

		for (PortletCompanyStatistics portletCompanyStatistics :
				_serverStaticsHelper.getPortletCompanyStatisticsSet()) {

			errorCount += getErrorCountByPortlet(
				portletId, portletCompanyStatistics);
		}

		return errorCount;
	}

	@Override
	public long getErrorCountByPortlet(String portletId, long companyId)
		throws MonitoringException {

		return getErrorCountByPortlet(
			portletId,
			_serverStaticsHelper.getPortletCompanyStatistics(companyId));
	}

	@Override
	public long getErrorCountByPortlet(String portletId, String webId)
		throws MonitoringException {

		return getErrorCountByPortlet(
			portletId, _serverStaticsHelper.getPortletCompanyStatistics(webId));
	}

	@Override
	public long getMaxTime() {
		long maxTime = 0;

		for (PortletCompanyStatistics portletCompanyStatistics :
				_serverStaticsHelper.getPortletCompanyStatisticsSet()) {

			for (RequestStatistics requestStatistics :
					portletCompanyStatistics.getRenderRequestStatisticsSet()) {

				if (requestStatistics.getMaxTime() > maxTime) {
					maxTime = requestStatistics.getMaxTime();
				}
			}
		}

		return maxTime;
	}

	@Override
	public long getMaxTimeByCompany(long companyId) throws MonitoringException {
		PortletCompanyStatistics portletCompanyStatistics =
			_serverStaticsHelper.getPortletCompanyStatistics(companyId);

		return portletCompanyStatistics.getMaxTime();
	}

	@Override
	public long getMaxTimeByCompany(String webId) throws MonitoringException {
		PortletCompanyStatistics portletCompanyStatistics =
			_serverStaticsHelper.getPortletCompanyStatistics(webId);

		return portletCompanyStatistics.getMaxTime();
	}

	@Override
	public long getMaxTimeByPortlet(String portletId)
		throws MonitoringException {

		long maxTime = 0;

		for (PortletCompanyStatistics portletCompanyStatistics :
				_serverStaticsHelper.getPortletCompanyStatisticsSet()) {

			long curMaxTime = getMaxTimeByPortlet(
				portletId, portletCompanyStatistics);

			if (curMaxTime > maxTime) {
				maxTime = curMaxTime;
			}
		}

		return maxTime;
	}

	@Override
	public long getMaxTimeByPortlet(String portletId, long companyId)
		throws MonitoringException {

		return getMaxTimeByPortlet(
			portletId,
			_serverStaticsHelper.getPortletCompanyStatistics(companyId));
	}

	@Override
	public long getMaxTimeByPortlet(String portletId, String webId)
		throws MonitoringException {

		return getMaxTimeByPortlet(
			portletId, _serverStaticsHelper.getPortletCompanyStatistics(webId));
	}

	@Override
	public long getMinTime() {
		long minTime = 0;

		for (PortletCompanyStatistics portletCompanyStatistics :
				_serverStaticsHelper.getPortletCompanyStatisticsSet()) {

			for (RequestStatistics requestStatistics :
					portletCompanyStatistics.getRenderRequestStatisticsSet()) {

				if (requestStatistics.getMinTime() < minTime) {
					minTime = requestStatistics.getMinTime();
				}
			}
		}

		return minTime;
	}

	@Override
	public long getMinTimeByCompany(long companyId) throws MonitoringException {
		PortletCompanyStatistics portletCompanyStatistics =
			_serverStaticsHelper.getPortletCompanyStatistics(companyId);

		return portletCompanyStatistics.getMinTime();
	}

	@Override
	public long getMinTimeByCompany(String webId) throws MonitoringException {
		PortletCompanyStatistics portletCompanyStatistics =
			_serverStaticsHelper.getPortletCompanyStatistics(webId);

		return portletCompanyStatistics.getMinTime();
	}

	@Override
	public long getMinTimeByPortlet(String portletId)
		throws MonitoringException {

		long minTime = 0;

		for (PortletCompanyStatistics portletCompanyStatistics :
				_serverStaticsHelper.getPortletCompanyStatisticsSet()) {

			long curMinTime = getMinTimeByPortlet(
				portletId, portletCompanyStatistics);

			if (curMinTime < minTime) {
				minTime = curMinTime;
			}
		}

		return minTime;
	}

	@Override
	public long getMinTimeByPortlet(String portletId, long companyId)
		throws MonitoringException {

		return getMinTimeByPortlet(
			portletId,
			_serverStaticsHelper.getPortletCompanyStatistics(companyId));
	}

	@Override
	public long getMinTimeByPortlet(String portletId, String webId)
		throws MonitoringException {

		return getMinTimeByPortlet(
			portletId, _serverStaticsHelper.getPortletCompanyStatistics(webId));
	}

	@Override
	public long getRequestCount() {
		long requestCount = 0;

		for (PortletCompanyStatistics portletCompanyStatistics :
				_serverStaticsHelper.getPortletCompanyStatisticsSet()) {

			requestCount += getRequestCountByCompany(portletCompanyStatistics);
		}

		return requestCount;
	}

	@Override
	public long getRequestCountByCompany(long companyId)
		throws MonitoringException {

		return getRequestCountByCompany(
			_serverStaticsHelper.getPortletCompanyStatistics(companyId));
	}

	@Override
	public long getRequestCountByCompany(String webId)
		throws MonitoringException {

		return getRequestCountByCompany(
			_serverStaticsHelper.getPortletCompanyStatistics(webId));
	}

	@Override
	public long getRequestCountByPortlet(String portletId)
		throws MonitoringException {

		long requestCount = 0;

		for (PortletCompanyStatistics portletCompanyStatistics :
				_serverStaticsHelper.getPortletCompanyStatisticsSet()) {

			requestCount += getRequestCountByPortlet(
				portletId, portletCompanyStatistics);
		}

		return requestCount;
	}

	@Override
	public long getRequestCountByPortlet(String portletId, long companyId)
		throws MonitoringException {

		return getRequestCountByPortlet(
			portletId,
			_serverStaticsHelper.getPortletCompanyStatistics(companyId));
	}

	@Override
	public long getRequestCountByPortlet(String portletId, String webId)
		throws MonitoringException {

		return getRequestCountByPortlet(
			portletId, _serverStaticsHelper.getPortletCompanyStatistics(webId));
	}

	@Override
	public long getSuccessCount() {
		long successCount = 0;

		for (PortletCompanyStatistics portletCompanyStatistics :
				_serverStaticsHelper.getPortletCompanyStatisticsSet()) {

			successCount += getSuccessCountByCompany(portletCompanyStatistics);
		}

		return successCount;
	}

	@Override
	public long getSuccessCountByCompany(long companyId)
		throws MonitoringException {

		return getSuccessCountByCompany(
			_serverStaticsHelper.getPortletCompanyStatistics(companyId));
	}

	@Override
	public long getSuccessCountByCompany(String webId)
		throws MonitoringException {

		return getSuccessCountByCompany(
			_serverStaticsHelper.getPortletCompanyStatistics(webId));
	}

	@Override
	public long getSuccessCountByPortlet(String portletId)
		throws MonitoringException {

		long successCount = 0;

		for (PortletCompanyStatistics portletCompanyStatistics :
				_serverStaticsHelper.getPortletCompanyStatisticsSet()) {

			successCount += getSuccessCountByPortlet(
				portletId, portletCompanyStatistics);
		}

		return successCount;
	}

	@Override
	public long getSuccessCountByPortlet(String portletId, long companyId)
		throws MonitoringException {

		return getSuccessCountByPortlet(
			portletId,
			_serverStaticsHelper.getPortletCompanyStatistics(companyId));
	}

	@Override
	public long getSuccessCountByPortlet(String portletId, String webId)
		throws MonitoringException {

		return getSuccessCountByPortlet(
			portletId, _serverStaticsHelper.getPortletCompanyStatistics(webId));
	}

	@Override
	public long getTimeoutCount() {
		long timeoutCount = 0;

		for (PortletCompanyStatistics portletCompanyStatistics :
				_serverStaticsHelper.getPortletCompanyStatisticsSet()) {

			timeoutCount += getTimeoutCountByCompany(portletCompanyStatistics);
		}

		return timeoutCount;
	}

	@Override
	public long getTimeoutCountByCompany(long companyId)
		throws MonitoringException {

		return getTimeoutCountByCompany(
			_serverStaticsHelper.getPortletCompanyStatistics(companyId));
	}

	@Override
	public long getTimeoutCountByCompany(String webId)
		throws MonitoringException {

		return getTimeoutCountByCompany(
			_serverStaticsHelper.getPortletCompanyStatistics(webId));
	}

	@Override
	public long getTimeoutCountByPortlet(String portletId)
		throws MonitoringException {

		long timeoutCount = 0;

		for (PortletCompanyStatistics portletCompanyStatistics :
				_serverStaticsHelper.getPortletCompanyStatisticsSet()) {

			timeoutCount += getTimeoutCountByPortlet(
				portletId, portletCompanyStatistics);
		}

		return timeoutCount;
	}

	@Override
	public long getTimeoutCountByPortlet(String portletId, long companyId)
		throws MonitoringException {

		return getTimeoutCountByPortlet(
			portletId,
			_serverStaticsHelper.getPortletCompanyStatistics(companyId));
	}

	@Override
	public long getTimeoutCountByPortlet(String portletId, String webId)
		throws MonitoringException {

		return getTimeoutCountByPortlet(
			portletId, _serverStaticsHelper.getPortletCompanyStatistics(webId));
	}

	protected long getAverageTimeByCompany(
		PortletCompanyStatistics portletCompanyStatistics) {

		long averageTime = 0;

		Set<RequestStatistics> requestStatisticsSet =
			portletCompanyStatistics.getRenderRequestStatisticsSet();

		for (RequestStatistics requestStatistics : requestStatisticsSet) {
			averageTime += requestStatistics.getAverageTime();
		}

		if (!requestStatisticsSet.isEmpty()) {
			return averageTime / requestStatisticsSet.size();
		}

		return averageTime;
	}

	protected long getErrorCountByCompany(
		PortletCompanyStatistics portletCompanyStatistics) {

		long errorCount = 0;

		for (RequestStatistics requestStatistics :
				portletCompanyStatistics.getRenderRequestStatisticsSet()) {

			errorCount += requestStatistics.getErrorCount();
		}

		return errorCount;
	}

	protected long getErrorCountByPortlet(
			String portletId, PortletCompanyStatistics portletCompanyStatistics)
		throws MonitoringException {

		RequestStatistics requestStatistics =
			portletCompanyStatistics.getRenderRequestStatistics(portletId);

		return requestStatistics.getErrorCount();
	}

	protected long getMaxTimeByPortlet(
			String portletId, PortletCompanyStatistics portletCompanyStatistics)
		throws MonitoringException {

		long maxTime = 0;

		RequestStatistics requestStatistics =
			portletCompanyStatistics.getRenderRequestStatistics(portletId);

		if (requestStatistics.getMaxTime() > maxTime) {
			maxTime = requestStatistics.getMaxTime();
		}

		return maxTime;
	}

	protected long getMinTimeByPortlet(
			String portletId, PortletCompanyStatistics portletCompanyStatistics)
		throws MonitoringException {

		long minTime = 0;

		RequestStatistics requestStatistics =
			portletCompanyStatistics.getRenderRequestStatistics(portletId);

		if (requestStatistics.getMinTime() < minTime) {
			minTime = requestStatistics.getMinTime();
		}

		return minTime;
	}

	protected long getRequestCountByCompany(
		PortletCompanyStatistics portletCompanyStatistics) {

		long requestCount = 0;

		for (RequestStatistics requestStatistics :
				portletCompanyStatistics.getRenderRequestStatisticsSet()) {

			requestCount += requestStatistics.getRequestCount();
		}

		return requestCount;
	}

	protected long getRequestCountByPortlet(
			String portletId, PortletCompanyStatistics portletCompanyStatistics)
		throws MonitoringException {

		RequestStatistics requestStatistics =
			portletCompanyStatistics.getRenderRequestStatistics(portletId);

		return requestStatistics.getRequestCount();
	}

	protected long getSuccessCountByCompany(
		PortletCompanyStatistics portletCompanyStatistics) {

		long successCount = 0;

		for (RequestStatistics requestStatistics :
				portletCompanyStatistics.getRenderRequestStatisticsSet()) {

			successCount += requestStatistics.getSuccessCount();
		}

		return successCount;
	}

	protected long getSuccessCountByPortlet(
			String portletId, PortletCompanyStatistics portletCompanyStatistics)
		throws MonitoringException {

		RequestStatistics requestStatistics =
			portletCompanyStatistics.getRenderRequestStatistics(portletId);

		return requestStatistics.getSuccessCount();
	}

	protected long getTimeoutCountByCompany(
		PortletCompanyStatistics portletCompanyStatistics) {

		long timeoutCount = 0;

		for (RequestStatistics requestStatistics :
				portletCompanyStatistics.getRenderRequestStatisticsSet()) {

			timeoutCount += requestStatistics.getTimeoutCount();
		}

		return timeoutCount;
	}

	protected long getTimeoutCountByPortlet(
			String portletId, PortletCompanyStatistics portletCompanyStatistics)
		throws MonitoringException {

		RequestStatistics requestStatistics =
			portletCompanyStatistics.getRenderRequestStatistics(portletId);

		return requestStatistics.getTimeoutCount();
	}

	@Reference
	private ServerStaticsHelper _serverStaticsHelper;

}