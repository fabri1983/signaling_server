package org.fabri1983.signaling.core.distributed.serialization;

import org.nextrtc.signalingserver.api.dto.NextRTCConversation;
import org.nextrtc.signalingserver.cases.ExchangeSignalsBetweenMembers;
import org.nextrtc.signalingserver.cases.LeftConversation;
import org.nextrtc.signalingserver.domain.MessageSender;

public class NextRTCConversationWrapper {

	public static NextRTCConversationWrapper wrap(NextRTCConversation conversation) {
		// TODO Auto-generated method stub
		return new NextRTCConversationWrapper();
	}
	
	public static NextRTCConversation unwrapNow(NextRTCConversationWrapper nextRTCConversationWrapper, 
			LeftConversation leftConversation, MessageSender messageSender,
			ExchangeSignalsBetweenMembers exchange) {
		// TODO implement a selector in which if the instance doesn't match the selector condition then 
		// fallback to a dummy selector which throws exception
		return null;
	}

	public NextRTCConversation unwrap(LeftConversation leftConversation, MessageSender messageSender,
			ExchangeSignalsBetweenMembers exchange) {
		return NextRTCConversationWrapper.unwrapNow(this, leftConversation, messageSender, exchange);
	}

}
