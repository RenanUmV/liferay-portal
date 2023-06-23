package com.liferay.staging.bar.web.internal.util;

import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.model.Layout;
import com.liferay.portal.kernel.theme.ThemeDisplay;
import com.liferay.portal.kernel.util.Constants;
import com.liferay.portal.kernel.util.ParamUtil;
import com.liferay.portal.kernel.util.WebKeys;
import com.liferay.staging.bar.web.internal.servlet.taglib.ui.StagingBarControlMenuJSPDynamicInclude;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class StagingBarControlMenuJSPDynamicIncludeUtil {

	public static boolean isShow(HttpServletRequest httpServletRequest)
		throws PortalException {

		Boolean show = (Boolean)httpServletRequest.getAttribute(_SHOW);

		if (show != null) {
			return show;
		}

		show = _isShow(httpServletRequest);

		httpServletRequest.setAttribute(_SHOW, show);

		return show;
	}

	private static boolean _isShow(HttpServletRequest httpServletRequest)
		throws PortalException {

		ThemeDisplay themeDisplay =
			(ThemeDisplay)httpServletRequest.getAttribute(
				WebKeys.THEME_DISPLAY);

		Layout layout = themeDisplay.getLayout();

		if (layout.isTypeControlPanel() || !themeDisplay.isShowStagingIcon()) {
			return false;
		}

		String layoutMode = ParamUtil.getString(
			httpServletRequest, "p_l_mode", Constants.VIEW);

		if (layoutMode.equals(Constants.EDIT)) {
			return false;
		}

		return true;
	}

	private static final Log _log = LogFactoryUtil.getLog(
		StagingBarControlMenuJSPDynamicInclude.class);

	private static final String _SHOW =
		StagingBarControlMenuJSPDynamicInclude.class + "#_SHOW";
	
}
