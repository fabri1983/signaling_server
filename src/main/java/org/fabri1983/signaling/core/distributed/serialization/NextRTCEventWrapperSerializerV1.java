package org.fabri1983.signaling.core.distributed.serialization;

import com.hazelcast.nio.ObjectDataInput;
import com.hazelcast.nio.ObjectDataOutput;
import com.hazelcast.nio.serialization.StreamSerializer;

import java.io.IOException;

public class NextRTCEventWrapperSerializerV1 implements StreamSerializer<NextRTCEventWrapper> {

	@Override
	public int getTypeId() {
		// ID must be unique and greater than or equal to 1. 
		// Uniqueness of the type ID enables Hazelcast to determine which serializer will be used during deserialization.
		return 1;
	}

	@Override
	public void destroy() {
	}

	@Override
	public void write(ObjectDataOutput out, NextRTCEventWrapper event) throws IOException {
		// TODO
		// Make sure the fields are written in the same order as they are read
	}

	@Override
	public NextRTCEventWrapper read(ObjectDataInput in) throws IOException {
		// TODO
		// Make sure the fields are read in the same order as they are written
		return null;
	}

}
