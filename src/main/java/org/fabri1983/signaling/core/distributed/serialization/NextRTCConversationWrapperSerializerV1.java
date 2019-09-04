package org.fabri1983.signaling.core.distributed.serialization;

import com.hazelcast.nio.ObjectDataInput;
import com.hazelcast.nio.ObjectDataOutput;
import com.hazelcast.nio.serialization.StreamSerializer;

import java.io.IOException;

import org.fabri1983.signaling.core.distributed.wrapper.NextRTCConversationWrapper;

public class NextRTCConversationWrapperSerializerV1 implements StreamSerializer<NextRTCConversationWrapper> {

	private int typeId = 3;
	
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
	public void write(ObjectDataOutput out, NextRTCConversationWrapper conversation) throws IOException {
		// Make sure the fields are written in the same order as they are read
		
		out.writeUTF(conversation.getId());
	}

	@Override
	public NextRTCConversationWrapper read(ObjectDataInput in) throws IOException {
		// Make sure the fields are read in the same order as they are written
		
		NextRTCConversationWrapper conversation = new NextRTCConversationWrapper();
		
		conversation.setId(in.readUTF());
		
		return conversation;
	}

}
