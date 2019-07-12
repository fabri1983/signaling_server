package org.fabri1983.signaling.util;

import java.util.function.Supplier;

import org.apache.log4j.MDC;

/**
 * Wraps a piece of code in order to put the user id coming from the message into the MDC map.
 */
public class UserIdMDCLogger {

	public static void doWithLog(String userId, Runnable runnable) {
		try {
			putUserId(userId);
			runnable.run();
		}
		finally {
			removeUserId();
		}
	}

	public static <T> T doWithLog(String userId, Supplier<T> supplier) {
		try {
			putUserId(userId);
			return supplier.get();
		}
		finally {
			removeUserId();
		}
	}

	private static void putUserId(String userId) {
		MDC.put("uid", isNullOrEmpty(userId) ? "" : "[uid:" + userId + "]");
	}

	private static void removeUserId() {
		MDC.remove("uid");
	}

	private static boolean isNullOrEmpty(String v) {
		return v == null || v.isEmpty();
	}
	
}
