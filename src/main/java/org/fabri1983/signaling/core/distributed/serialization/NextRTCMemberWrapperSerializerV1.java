package org.fabri1983.signaling.core.distributed.serialization;

import com.hazelcast.nio.ObjectDataInput;
import com.hazelcast.nio.ObjectDataOutput;
import com.hazelcast.nio.serialization.StreamSerializer;

import java.io.IOException;

import static org.fabri1983.signaling.core.distributed.serialization.SerializerWrapperHelper.extractTargetClass;

import org.fabri1983.signaling.core.distributed.wrapper.NextRTCMemberWrapper;

public class NextRTCMemberWrapperSerializerV1 implements StreamSerializer<NextRTCMemberWrapper> {

	private int typeId = 2;
	
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
	public void write(ObjectDataOutput out, NextRTCMemberWrapper member) throws IOException {
		// Make sure the fields are written in the same order as they are read
		
		out.writeUTF(member.getUserId());
		out.writeUTF(member.getTargetClass().getName());
	}

	@Override
	public NextRTCMemberWrapper read(ObjectDataInput in) throws IOException {
		// Make sure the fields are read in the same order as they are written
		
		NextRTCMemberWrapper member = new NextRTCMemberWrapper();
		
		member.setUserId(in.readUTF());
		member.setTargetClass(extractTargetClass(in));
		
		return member;
	}

}
