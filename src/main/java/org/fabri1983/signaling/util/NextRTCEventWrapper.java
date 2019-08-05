package org.fabri1983.signaling.util;

import java.util.Map;

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
	public static NextRTCEventWrapperBuilder wrapBuilder(Class<? extends NextRTCEvent> targetClass) {
		return new NextRTCEventWrapperBuilder(targetClass);
	}

	/**
	 * Directly wrap the event instance to later be serialized.
	 * 
	 * @param event
	 * @return
	 */
	public static NextRTCEventWrapper wrap(NextRTCEvent event) {
		return new NextRTCEventWrapperBuilder().wrapFrom(event);
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
			ExchangeSignalsBetweenMembers exchange) {
		
		// TODO implement a selector in which if the instance doesn't match the selector condition then 
		// fallback to a dummy selector which throws exception
		
		if (wrapper.getTargetClass().equals(EventContext.class)) {
			return EventContext.builder()
					.type(wrapper.getType())
					.from(wrapper.getFrom().unwrap(eventBus))
					.to(wrapper.getTo().unwrap(eventBus))
					.conversation(wrapper.getConversation().unwrap(leftConversation, messageSender, exchange))
					.exception(wrapper.getException())
					.custom(wrapper.getCustom())
					.content(wrapper.getContent())
					.reason(wrapper.getReason())
					.build();
		} else {
			throw new RuntimeException("Can't create instance of unkown target class.");
		}
	}
	
	public NextRTCEvent unwrap(NextRTCEventWrapper wrapper, NextRTCEventBus eventBus, 
			LeftConversation leftConversation, MessageSender messageSender, 
			ExchangeSignalsBetweenMembers exchange) {
		return NextRTCEventWrapper.unwrapNow(wrapper, eventBus, leftConversation, messageSender, exchange);
	}
	
	public Class<?> getTargetClass() {
		return targetClass;
	}

	public NextRTCEvents getType() {
		return type;
	}

	public NextRTCMemberWrapper getFrom() {
		return from;
	}

	public NextRTCMemberWrapper getTo() {
		return to;
	}

	public NextRTCConversationWrapper getConversation() {
		return conversation;
	}

	public SignalingException getException() {
		return exception;
	}

	public Map<String, String> getCustom() {
		return custom;
	}

	public String getContent() {
		return content;
	}

	public String getReason() {
		return reason;
	}

	public static class NextRTCEventWrapperBuilder {

		private NextRTCEventWrapper newObj;

		public NextRTCEventWrapperBuilder() {
			this(null);
		}
		
		public NextRTCEventWrapperBuilder(Class<? extends NextRTCEvent> targetClass) {
			newObj = new NextRTCEventWrapper();
			newObj.targetClass = targetClass;
		}

		public NextRTCEventWrapperBuilder targetClass(Class<? extends NextRTCEvent> targetClass) {
			newObj.targetClass = targetClass;
			return this;
		}
		
		public NextRTCEventWrapperBuilder type(NextRTCEvents type) {
			newObj.type = type;
			return this;
		}
		
		public NextRTCEventWrapperBuilder from(NextRTCMember from) {
			newObj.from = NextRTCMemberWrapper.wrap(from);
			return this;
		}
		
		public NextRTCEventWrapperBuilder to(NextRTCMember to) {
			newObj.to = NextRTCMemberWrapper.wrap(to);
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
		
		public NextRTCEventWrapper wrapFrom(NextRTCEvent event) {
			if (event instanceof EventContext) {
				newObj.targetClass = EventContext.class;
			}
			newObj.type = event.type();
			newObj.from = NextRTCMemberWrapper.wrap(event.from().orElse(null));
			newObj.to = NextRTCMemberWrapper.wrap(event.to().orElse(null));
			newObj.conversation = NextRTCConversationWrapper.wrap(event.conversation().orElse(null));
			newObj.exception = event.exception().orElse(null);
			newObj.custom = event.custom();
			newObj.content = event.content().orElse(null);
			newObj.reason = event.reason().orElse(null);
			return newObj;
		}
	}

}
