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

package com.liferay.redirect.internal.configuration;

import com.liferay.portal.configuration.metatype.bnd.util.ConfigurableUtil;
import com.liferay.portal.kernel.model.CompanyConstants;
import com.liferay.portal.kernel.redirect.RedirectURLSettings;
import com.liferay.portal.kernel.util.GetterUtil;
import com.liferay.portal.kernel.util.HashMapDictionaryBuilder;

import java.util.Dictionary;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedServiceFactory;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;

/**
 * @author Drew Brokke
 */
@Component(
	configurationPid = "com.liferay.redirect.internal.configuration.RedirectURLConfiguration",
	service = RedirectURLSettings.class
)
public class RedirectURLSettingsImpl implements RedirectURLSettings {

	@Override
	public String[] getAllowedDomains(long companyId) {
		RedirectURLConfiguration redirectURLConfiguration =
			getCompanyRedirectURLConfiguration(companyId);

		return redirectURLConfiguration.allowedDomains();
	}

	@Override
	public String[] getAllowedIPs(long companyId) {
		RedirectURLConfiguration redirectURLConfiguration =
			getCompanyRedirectURLConfiguration(companyId);

		return redirectURLConfiguration.allowedIPs();
	}

	public RedirectURLConfiguration getCompanyRedirectURLConfiguration(
		long companyId) {

		if (_companyConfigurationBeans.containsKey(companyId)) {
			return _companyConfigurationBeans.get(companyId);
		}

		return _systemRedirectURLConfiguration;
	}

	@Override
	public String getSecurityMode(long companyId) {
		RedirectURLConfiguration redirectURLConfiguration =
			getCompanyRedirectURLConfiguration(companyId);

		return redirectURLConfiguration.securityMode();
	}

	@Activate
	protected void activate(
		BundleContext bundleContext, Map<String, Object> properties) {

		modified(properties);

		_serviceRegistration = bundleContext.registerService(
			ManagedServiceFactory.class, new RedirectURLManagedServiceFactory(),
			HashMapDictionaryBuilder.put(
				Constants.SERVICE_PID,
				"com.liferay.redirect.internal.configuration." +
					"RedirectURLConfiguration.scoped"
			).build());
	}

	@Deactivate
	protected void deactivate() {
		_serviceRegistration.unregister();
	}

	@Modified
	protected void modified(Map<String, Object> properties) {
		_systemRedirectURLConfiguration = ConfigurableUtil.createConfigurable(
			RedirectURLConfiguration.class, properties);
	}

	private final Map<Long, RedirectURLConfiguration>
		_companyConfigurationBeans = new ConcurrentHashMap<>();
	private final Map<String, Long> _companyIds = new ConcurrentHashMap<>();
	private ServiceRegistration<ManagedServiceFactory> _serviceRegistration;
	private volatile RedirectURLConfiguration _systemRedirectURLConfiguration;

	private class RedirectURLManagedServiceFactory
		implements ManagedServiceFactory {

		@Override
		public void deleted(String pid) {
			_unmapPid(pid);
		}

		@Override
		public String getName() {
			return "com.liferay.redirect.internal.configuration." +
				"RedirectURLConfiguration.scoped";
		}

		@Override
		public void updated(String pid, Dictionary dictionary)
			throws ConfigurationException {

			_unmapPid(pid);

			long companyId = GetterUtil.getLong(
				dictionary.get("companyId"), CompanyConstants.SYSTEM);

			if (companyId != CompanyConstants.SYSTEM) {
				_companyConfigurationBeans.put(
					companyId,
					ConfigurableUtil.createConfigurable(
						RedirectURLConfiguration.class, dictionary));
				_companyIds.put(pid, companyId);
			}
		}

		private void _unmapPid(String pid) {
			if (_companyIds.containsKey(pid)) {
				long companyId = _companyIds.remove(pid);

				_companyConfigurationBeans.remove(companyId);
			}
		}

	}

}