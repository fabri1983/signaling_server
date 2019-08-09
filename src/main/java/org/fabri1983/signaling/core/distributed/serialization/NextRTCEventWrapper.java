package org.fabri1983.signaling.core.distributed.serialization;

import java.util.Map;

import org.fabri1983.signaling.core.population.ConversationPopulation;
import org.nextrtc.signalingserver.api.NextRTCEventBus;
import org.nextrtc.signalingserver.api.NextRTCEvents;
import org.nextrtc.signalingserver.api.dto.NextRTCConversation;
import org.nextrtc.signalingserver.api.dto.NextRTCEvent;
import org.nextrtc.signalingserver.api.dto.NextRTCMember;
import org.nextrtc.signalingserver.cases.ExchangeSignalsBetweenMembers;
import org.nextrtc.signalingserver.cases.LeftConversation;
import org.nextrtc.signalingserver.domain.EventContext;
import org.nextrtc.signalingserver.domain.MessageSender;
import org.nextrtc.signalingserver.exception.SignalingException;
import org.nextrtc.signalingserver.repository.MemberRepository;

public class NextRTCEventWrapper {

	private Class<? extends NextRTCEvent> targetClass;
	private NextRTCEvents type;
	private NextRTCMemberWrapper from;
	private NextRTCMemberWrapper to;
	private NextRTCConversationWrapper conversation;
	private SignalingException exception;
	private Map<String, String> custom;
	private String content;
	private String reason;

	/**
	 * Builder to wrap an instance of NextRTCEvent to later be serialized.
	 * 
	 * @param targetClass
	 * @return
	 */
	public static <T extends NextRTCEvent> NextRTCEventWrapperBuilder wrapBuilder(Class<T> targetClass, 
			ConversationPopulation<String, String, String> population) {
		return new NextRTCEventWrapperBuilder(targetClass, population);
	}

	/**
	 * Directly wrap the event instance to later be serialized.
	 * 
	 * @param event
	 * @return
	 */
	public static NextRTCEventWrapper wrap(NextRTCEvent event, ConversationPopulation<String, String, String> population) {
		return NextRTCEventWrapperBuilder.wrapFrom(event, event.getClass(), population);
	}
	
	/**
     * Instances of NextRTCEvent have references to managed objects, which are not serialized, so after
     * deserialization phase we have to populate with live correspondent instances.
     * 
     * @param eventWrapper
     * @param eventBus
     * @return
     */
	public static NextRTCEvent unwrapNow(NextRTCEventWrapper wrapper, NextRTCEventBus eventBus, 
			LeftConversation leftConversation, MessageSender messageSender,  
			ExchangeSignalsBetweenMembers exchange, ConversationPopulation<String, String, String> population, 
			MemberRepository members) {
		
		// TODO implement a selector in which if the instance doesn't match the selector condition then 
		// fallback to a dummy selector which throws exception
		
		if (wrapper.targetClass.equals(EventContext.class)) {
			return EventContext.builder()
					.type(wrapper.type)
					.from(wrapper.from.unwrap(eventBus, population, members))
					.to(wrapper.to.unwrap(eventBus, population, members))
					.conversation(wrapper.conversation.unwrap(leftConversation, 
							messageSender, exchange))
					.exception(wrapper.exception)
					.custom(wrapper.custom)
					.content(wrapper.content)
					.reason(wrapper.reason)
					.build();
		} else {
			throw new RuntimeException("Can't create instance of unkown target class.");
		}
	}
	
	public NextRTCEvent unwrap(NextRTCEventWrapper wrapper, NextRTCEventBus eventBus, 
			LeftConversation leftConversation, MessageSender messageSender, 
			ExchangeSignalsBetweenMembers exchange, ConversationPopulation<String, String, String> population, 
			MemberRepository members) {
		return NextRTCEventWrapper.unwrapNow(wrapper, eventBus, leftConversation, 
				messageSender, exchange, population, members);
	}
	
	public static class NextRTCEventWrapperBuilder {

		private NextRTCEventWrapper newObj;
		private ConversationPopulation<String, String, String> population;
		
		public <T extends NextRTCEvent> NextRTCEventWrapperBuilder(Class<T> targetClass, 
				ConversationPopulation<String, String, String> population) {
			newObj = new NextRTCEventWrapper();
			newObj.targetClass = targetClass;
			this.population = population;
		}

		public <T extends NextRTCEvent> NextRTCEventWrapperBuilder targetClass(Class<T> targetClass) {
			newObj.targetClass = targetClass;
			return this;
		}
		
		public NextRTCEventWrapperBuilder type(NextRTCEvents type) {
			newObj.type = type;
			return this;
		}
		
		public NextRTCEventWrapperBuilder from(NextRTCMember from) {
			newObj.from = NextRTCMemberWrapper.wrap(from, population.getUserIdBySessionId(from.getId()));
			return this;
		}
		
		public NextRTCEventWrapperBuilder to(NextRTCMember to) {
			newObj.to = NextRTCMemberWrapper.wrap(to, population.getUserIdBySessionId(to.getId()));
			return this;
		}
		
		public NextRTCEventWrapperBuilder conversation(NextRTCConversation conversation) {
			newObj.conversation = NextRTCConversationWrapper.wrap(conversation);
			return this;
		}
		
		public NextRTCEventWrapperBuilder exception(SignalingException exception) {
			newObj.exception = exception;
			return this;
		}
		
		public NextRTCEventWrapperBuilder custom(Map<String, String> custom) {
			newObj.custom = custom;
			return this;
		}
		
		public NextRTCEventWrapperBuilder content(String content) {
			newObj.content = content;
			return this;
		}
		
		public NextRTCEventWrapperBuilder reason(String reason) {
			newObj.reason = reason;
			return this;
		}
		
		public NextRTCEventWrapper build() {
			return newObj;
		}
		
		public static <T extends NextRTCEvent, S extends NextRTCEvent> NextRTCEventWrapper wrapFrom(T event, 
				Class<S> targetClass, ConversationPopulation<String, String, String> population) {
			NextRTCMember from = event.from().orElse(null);
			NextRTCMember to = event.to().orElse(null);
			NextRTCEventWrapper newObj = new NextRTCEventWrapper();
			newObj.targetClass = targetClass;
			newObj.type = event.type();
			newObj.from = NextRTCMemberWrapper.wrap(from, population.getUserIdBySessionId(from.getId()));
			newObj.to = NextRTCMemberWrapper.wrap(to, population.getUserIdBySessionId(to.getId()));
			newObj.conversation = NextRTCConversationWrapper.wrap(event.conversation().orElse(null));
			newObj.exception = event.exception().orElse(null);
			newObj.custom = event.custom();
			newObj.content = event.content().orElse(null);
			newObj.reason = event.reason().orElse(null);
			return newObj;
		}
	}

}
