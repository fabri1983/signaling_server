package org.fabri1983.signaling.core.distributed.serialization;

import org.nextrtc.signalingserver.api.NextRTCEventBus;
import org.nextrtc.signalingserver.api.dto.NextRTCMember;

public class NextRTCMemberWrapper {

	public static NextRTCMemberWrapper wrap(NextRTCMember member) {
		// TODO Auto-generated method stub
		return new NextRTCMemberWrapper();
	}
	
	public static NextRTCMember unwrapNow(NextRTCMemberWrapper nextRTCMemberWrapper, 
			NextRTCEventBus eventBus) {
		// TODO implement a selector in which if the instance doesn't match the selector condition then 
		// fallback to a dummy selector which throws exception
		return null;
	}

	public NextRTCMember unwrap(NextRTCEventBus eventBus) {
		return NextRTCMemberWrapper.unwrapNow(this, eventBus);
	}

}
