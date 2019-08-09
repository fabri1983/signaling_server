package org.fabri1983.signaling.util;

import java.util.List;
import java.util.Map;

public interface IFunctional {

	default public boolean isNullOrEmpty(Map<String, String> map) {
		return map == null || map.isEmpty();
	}

	default public boolean isNullOrEmpty(List<String> map) {
		return map == null || map.isEmpty();
	}
	
    default public boolean isNullOrEmpty(String s) {
		return s == null || s.isEmpty();
	}
    
}
