/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.document.library.internal.configuration.admin.service;

import com.liferay.document.library.internal.configuration.DLSizeLimitConfiguration;
import com.liferay.document.library.internal.configuration.helper.DLSizeLimitConfigurationHelper;
import com.liferay.portal.configuration.metatype.bnd.util.ConfigurableUtil;
import com.liferay.portal.kernel.model.CompanyConstants;
import com.liferay.portal.kernel.model.GroupConstants;
import com.liferay.portal.kernel.util.GetterUtil;

import java.util.Dictionary;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.osgi.framework.Constants;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedServiceFactory;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

/**
 * @author Adolfo Pérez
 */
@Component(
	property = Constants.SERVICE_PID + "=com.liferay.document.library.internal.configuration.DLSizeLimitConfiguration.scoped",
	service = ManagedServiceFactory.class
)
public class DLSizeLimitManagedServiceFactory implements ManagedServiceFactory {

	@Override
	public void deleted(String pid) {
		_unmapPid(pid);
	}

	@Override
	public String getName() {
		return "com.liferay.document.library.internal.configuration." +
			"DLSizeLimitConfiguration.scoped";
	}

	@Override
	public void updated(String pid, Dictionary<String, ?> dictionary)
		throws ConfigurationException {

		_unmapPid(pid);

		long companyId = GetterUtil.getLong(
			dictionary.get("companyId"), CompanyConstants.SYSTEM);

		if (companyId != CompanyConstants.SYSTEM) {
			_updateCompanyConfiguration(companyId, pid, dictionary);
		}

		long groupId = GetterUtil.getLong(
			dictionary.get("groupId"), GroupConstants.DEFAULT_PARENT_GROUP_ID);

		if (groupId != GroupConstants.DEFAULT_PARENT_GROUP_ID) {
			_updateGroupConfiguration(groupId, pid, dictionary);
		}
	}

	private void _unmapPid(String pid) {
		if (_companyIds.containsKey(pid)) {
			long companyId = _companyIds.remove(pid);

			_dlSizeLimitConfigurationHelper.removeCompanyConfigurationBeans(
				companyId);
			_dlSizeLimitConfigurationHelper.removeGroupMimeTypeSizeLimitsMap(
				companyId);

			_dlSizeLimitConfigurationHelper.clearGroupConfigurationBeans();
			_groupIds.clear();
			_dlSizeLimitConfigurationHelper.clearGroupMimeTypeSizeLimitsMap();
		}
		else if (_groupIds.containsKey(pid)) {
			long groupId = _groupIds.remove(pid);

			_dlSizeLimitConfigurationHelper.removeGroupConfigurationBeans(
				groupId);

			_dlSizeLimitConfigurationHelper.removeGroupMimeTypeSizeLimitsMap(
				groupId);
		}
	}

	private void _updateCompanyConfiguration(
		long companyId, String pid, Dictionary<String, ?> dictionary) {

		_dlSizeLimitConfigurationHelper.updateCompanyConfigurationBeans(
			companyId,
			ConfigurableUtil.createConfigurable(
				DLSizeLimitConfiguration.class, dictionary));

		_companyIds.put(pid, companyId);
		_dlSizeLimitConfigurationHelper.removeGroupMimeTypeSizeLimitsMap(
			companyId);
	}

	private void _updateGroupConfiguration(
		long groupId, String pid, Dictionary<String, ?> dictionary) {

		_dlSizeLimitConfigurationHelper.updateGroupConfigurationBeans(
			ConfigurableUtil.createConfigurable(
				DLSizeLimitConfiguration.class, dictionary),
			groupId);

		_groupIds.put(pid, groupId);
		_dlSizeLimitConfigurationHelper.removeGroupMimeTypeSizeLimitsMap(
			groupId);
	}

	private final Map<String, Long> _companyIds = new ConcurrentHashMap<>();

	@Reference
	private DLSizeLimitConfigurationHelper _dlSizeLimitConfigurationHelper;

	private final Map<String, Long> _groupIds = new ConcurrentHashMap<>();

}