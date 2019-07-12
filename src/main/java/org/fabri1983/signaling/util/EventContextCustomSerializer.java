package org.fabri1983.signaling.util;

import java.io.IOException;

import org.nextrtc.signalingserver.domain.EventContext;

import com.hazelcast.nio.ObjectDataInput;
import com.hazelcast.nio.ObjectDataOutput;
import com.hazelcast.nio.serialization.StreamSerializer;

public class EventContextCustomSerializer implements StreamSerializer<EventContext> {

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
	public void write(ObjectDataOutput out, EventContext eventContext) throws IOException {
		// Make sure the fields are written in the same order as they are read
	}

	@Override
	public EventContext read(ObjectDataInput in) throws IOException {
		// Make sure the fields are read in the same order as they are written
		return null;
	}

}
