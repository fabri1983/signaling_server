package org.fabri1983.signaling.core.distributed.serialization;

import com.hazelcast.nio.ObjectDataInput;

import java.io.IOException;

public class SerializerWrapperHelper {

	public static String wrapString(String s) {
		if (s == null) {
			return "null_tag";
		}
		return s;
	}
	
	public static Object wrapObject(Object obj) {
		if (obj == null) {
			return new NullObject();
		}
		return obj;
	}
	
	public static String unwrapString(String s) {
		if ("null_tag".equals(s)) {
			return null;
		}
		return s;
	}
	
	@SuppressWarnings("unchecked")
	public static <T> T unwrapObject(Object obj) {
		if (obj instanceof NullObject) {
			return null;
		}
		return (T) obj;
	}
	
	@SuppressWarnings("unchecked")
	public static <T> Class<? extends T> extractTargetClass(ObjectDataInput in) {
		try {
			return (Class<? extends T>) Class.forName(in.readUTF());
		} catch (ClassNotFoundException | IOException ex) {
			throw new RuntimeException(ex);
		}
	}
	
}
