package org.fabri1983.signaling.core.handler.signal;

import java.util.HashMap;
import java.util.Map;

import org.fabri1983.signaling.core.SignalingConstants;

public final class SignalHandlerHelper {

	public final static Map<String, String> customMapWithUserFrom(String userFrom) {
		String strUserFrom = userFrom == null ? "" : userFrom;
		Map<String, String> customMap = new HashMap<String, String>(3);
		customMap.put(SignalingConstants.USER_FROM, strUserFrom);
		return customMap;
	}
	
}
