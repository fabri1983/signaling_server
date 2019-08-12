package org.fabri1983.signaling.core.distributed.serialization;

import javax.websocket.Session;

import org.fabri1983.signaling.core.population.ConversationPopulation;
import org.nextrtc.signalingserver.api.NextRTCEventBus;
import org.nextrtc.signalingserver.api.dto.NextRTCMember;
import org.nextrtc.signalingserver.domain.Member;
import org.nextrtc.signalingserver.exception.Exceptions;
import org.nextrtc.signalingserver.repository.MemberRepository;

public class NextRTCMemberWrapper {

	private Class<? extends NextRTCMember> targetClass;
	private String userId;
	
	public static <T extends NextRTCMember> NextRTCMemberWrapper wrap(T member, String userId) {
		NextRTCMemberWrapper wrapper = new NextRTCMemberWrapper();
		wrapper.targetClass = member.getClass();
		wrapper.userId = userId;
		return wrapper;
	}
	
	public static NextRTCMember unwrapNow(NextRTCMemberWrapper wrapper, NextRTCEventBus eventBus, 
			ConversationPopulation population, MemberRepository members) {
		
		// TODO implement a selector in which if the instance doesn't match the selector condition then 
		// fallback to a dummy selector which throws exception
		
		if (wrapper.targetClass.equals(Member.class)) {
			Session session = population.getSessionByUserId(wrapper.userId);
			Member member = members.findBy(session.getId()).orElse(null);
			member.setEventBus(eventBus);
			return member;
		} else {
			throw Exceptions.UNKNOWN_ERROR.exception("Can't create instance of unkown target class.");
		}
	}

	public NextRTCMember unwrap(NextRTCEventBus eventBus, ConversationPopulation population, 
			MemberRepository members) {
		return NextRTCMemberWrapper.unwrapNow(this, eventBus, population, members);
	}

}
