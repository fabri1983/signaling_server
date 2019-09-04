package org.fabri1983.signaling.core.distributed.wrapper;

import java.util.Map;

import org.fabri1983.signaling.core.population.ConversationPopulation;
import org.nextrtc.signalingserver.api.NextRTCEventBus;
import org.nextrtc.signalingserver.api.NextRTCEvents;
import org.nextrtc.signalingserver.api.dto.NextRTCConversation;
import org.nextrtc.signalingserver.api.dto.NextRTCEvent;
import org.nextrtc.signalingserver.api.dto.NextRTCMember;
import org.nextrtc.signalingserver.domain.EventContext;
import org.nextrtc.signalingserver.exception.Exceptions;
import org.nextrtc.signalingserver.exception.SignalingException;
import org.nextrtc.signalingserver.repository.ConversationRepository;
import org.nextrtc.signalingserver.repository.MemberRepository;

public class NextRTCEventWrapper {

	private String instanceId;
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
			String instanceId, ConversationPopulation population) {
		return new NextRTCEventWrapperBuilder(targetClass, instanceId, population);
	}

	/**
	 * Directly wrap the event instance to later be serialized.
	 * 
	 * @param event
	 * @return
	 */
	public static NextRTCEventWrapper wrap(NextRTCEvent event, String instancId, ConversationPopulation population) {
		return NextRTCEventWrapperBuilder.wrapFrom(event, instancId, event.getClass(), population);
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
			ConversationPopulation population, ConversationRepository conversationRepository, 
			MemberRepository members) {
		
		// TODO implement a selector in which if the instance doesn't match the selector condition then 
		// fallback to a dummy selector which throws exception
		
		if (wrapper.targetClass.equals(EventContext.class)) {
			return EventContext.builder()
					.type(wrapper.type)
					.from(NextRTCMemberWrapper.unwrapNow(wrapper.from, eventBus, population, members))
					.to(NextRTCMemberWrapper.unwrapNow(wrapper.to, eventBus, population, members))
					.conversation(NextRTCConversationWrapper.unwrapNow(wrapper.conversation, conversationRepository))
					.exception(wrapper.exception)
					.custom(wrapper.custom)
					.content(wrapper.content)
					.reason(wrapper.reason)
					.build();
		} else {
			throw Exceptions.UNKNOWN_ERROR.exception("Can't create instance of unkown target class.");
		}
	}
	
	public NextRTCEvent unwrap(NextRTCEventWrapper wrapper, NextRTCEventBus eventBus, 
			ConversationPopulation population, ConversationRepository conversationRepository, 
			MemberRepository members) {
		return NextRTCEventWrapper.unwrapNow(wrapper, eventBus, population, conversationRepository, members);
	}
	
	public String getInstanceId() {
		return instanceId;
	}

	public void setInstanceId(String instanceId) {
		this.instanceId = instanceId;
	}

	public Class<? extends NextRTCEvent> getTargetClass() {
		return targetClass;
	}

	public void setTargetClass(Class<? extends NextRTCEvent> targetClass) {
		this.targetClass = targetClass;
	}

	public NextRTCEvents getType() {
		return type;
	}

	public void setType(NextRTCEvents type) {
		this.type = type;
	}

	public NextRTCMemberWrapper getFrom() {
		return from;
	}

	public void setFrom(NextRTCMemberWrapper from) {
		this.from = from;
	}

	public NextRTCMemberWrapper getTo() {
		return to;
	}

	public void setTo(NextRTCMemberWrapper to) {
		this.to = to;
	}

	public NextRTCConversationWrapper getConversation() {
		return conversation;
	}

	public void setConversation(NextRTCConversationWrapper conversation) {
		this.conversation = conversation;
	}

	public SignalingException getException() {
		return exception;
	}

	public void setException(SignalingException exception) {
		this.exception = exception;
	}

	public Map<String, String> getCustom() {
		return custom;
	}

	public void setCustom(Map<String, String> custom) {
		this.custom = custom;
	}

	public String getContent() {
		return content;
	}

	public void setContent(String content) {
		this.content = content;
	}

	public String getReason() {
		return reason;
	}

	public void setReason(String reason) {
		this.reason = reason;
	}

	public static class NextRTCEventWrapperBuilder {

		private NextRTCEventWrapper newObj;
		private ConversationPopulation population;
		
		public <T extends NextRTCEvent> NextRTCEventWrapperBuilder(Class<T> targetClass, 
				String instanceId, ConversationPopulation population) {
			newObj = new NextRTCEventWrapper();
			newObj.instanceId = instanceId;
			newObj.targetClass = targetClass;
			this.population = population;
		}

		public <T extends NextRTCEvent> NextRTCEventWrapperBuilder instanceId(String instanceId) {
			newObj.instanceId = instanceId;
			return this;
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
				String instanceId, Class<S> targetClass, ConversationPopulation population) {
			NextRTCEventWrapper newObj = new NextRTCEventWrapper();
			newObj.instanceId = instanceId;
			newObj.targetClass = targetClass;
			newObj.type = event.type();
			event.from().ifPresent( from -> {
				newObj.from = NextRTCMemberWrapper.wrap(from, population.getUserIdBySessionId(from.getId()));
			});
			event.to().ifPresent( to -> {
				newObj.to = NextRTCMemberWrapper.wrap(to, population.getUserIdBySessionId(to.getId()));
			});
			event.conversation().ifPresent( conversation -> {
				newObj.conversation = NextRTCConversationWrapper.wrap(conversation);
			});
			newObj.exception = event.exception().orElse(null);
			newObj.custom = event.custom();
			newObj.content = event.content().orElse(null);
			newObj.reason = event.reason().orElse(null);
			return newObj;
		}
	}

}
