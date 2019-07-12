package org.fabri1983.signaling.util;

import java.util.HashMap;
import java.util.Map;

import org.fabri1983.signaling.core.SignalingConstants;

public final class SignalHandlerHelper {

	public final static <U> Map<String, String> customMapWithUserFrom(U userFrom) {
		String strUserFrom = userFrom == null ? "" : userFrom.toString();
		Map<String, String> customMap = new HashMap<String, String>(2);
		customMap.put(SignalingConstants.USER_FROM, strUserFrom);
		return customMap;
	}
	
}
