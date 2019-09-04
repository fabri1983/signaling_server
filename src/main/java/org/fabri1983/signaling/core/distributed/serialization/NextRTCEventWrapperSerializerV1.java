package org.fabri1983.signaling.core.distributed.serialization;

import com.hazelcast.nio.ObjectDataInput;
import com.hazelcast.nio.ObjectDataOutput;
import com.hazelcast.nio.serialization.StreamSerializer;

import java.io.IOException;

import static org.fabri1983.signaling.core.distributed.serialization.SerializerWrapperHelper.extractTargetClass;
import static org.fabri1983.signaling.core.distributed.serialization.SerializerWrapperHelper.unwrapObject;
import static org.fabri1983.signaling.core.distributed.serialization.SerializerWrapperHelper.unwrapString;
import static org.fabri1983.signaling.core.distributed.serialization.SerializerWrapperHelper.wrapObject;
import static org.fabri1983.signaling.core.distributed.serialization.SerializerWrapperHelper.wrapString;

import org.fabri1983.signaling.core.distributed.wrapper.NextRTCEventWrapper;

public class NextRTCEventWrapperSerializerV1 implements StreamSerializer<NextRTCEventWrapper> {

	private int typeId = 1;
	
	@Override
	public int getTypeId() {
		// ID must be unique and greater than or equal to 1. 
		// Uniqueness of the type ID enables Hazelcast to determine which serializer will be used during deserialization.
		return typeId;
	}

	@Override
	public void destroy() {
	}

	@Override
	public void write(ObjectDataOutput out, NextRTCEventWrapper event) throws IOException {
		// Make sure the fields are written in the same order as they are read
		
		out.writeUTF(event.getInstanceId());
		out.writeUTF(event.getTargetClass().getName());
		out.writeObject(event.getType());
		out.writeObject(wrapObject(event.getFrom()));
		out.writeObject(wrapObject(event.getTo()));
		out.writeObject(wrapObject(event.getConversation()));
		out.writeObject(wrapObject(event.getException()));
		out.writeObject(wrapObject(event.getCustom()));
		out.writeUTF(wrapString(event.getContent()));
		out.writeUTF(wrapString(event.getReason()));
	}

	@Override
	public NextRTCEventWrapper read(ObjectDataInput in) throws IOException {
		// Make sure the fields are read in the same order as they are written
		
		NextRTCEventWrapper event = new NextRTCEventWrapper();
		
		event.setInstanceId(in.readUTF());
		event.setTargetClass(extractTargetClass(in));
		event.setType(in.readObject());
		event.setFrom(unwrapObject(in.readObject()));
		event.setTo(unwrapObject(in.readObject()));
		event.setConversation(unwrapObject(in.readObject()));
		event.setException(unwrapObject(in.readObject()));
		event.setCustom(unwrapObject(in.readObject()));
		event.setContent(unwrapString(in.readUTF()));
		event.setReason(unwrapString(in.readUTF()));
		
		return event;
	}

}
