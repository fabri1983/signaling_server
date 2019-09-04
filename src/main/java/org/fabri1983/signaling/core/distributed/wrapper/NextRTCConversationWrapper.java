package org.fabri1983.signaling.core.distributed.wrapper;

import org.nextrtc.signalingserver.api.dto.NextRTCConversation;
import org.nextrtc.signalingserver.exception.Exceptions;
import org.nextrtc.signalingserver.exception.SignalingException;
import org.nextrtc.signalingserver.repository.ConversationRepository;

public class NextRTCConversationWrapper {

	private String id;
	
	public static NextRTCConversationWrapper wrap(NextRTCConversation conversation) {
		NextRTCConversationWrapper wrapper = new NextRTCConversationWrapper();
		wrapper.id = conversation.getId();
		return wrapper;
	}
	
	public static NextRTCConversation unwrapNow(NextRTCConversationWrapper wrapper, 
			ConversationRepository conversationRepository) {
		if (wrapper == null) {
			return null;
		}
		return conversationRepository.findBy(wrapper.id)
				.orElseThrow( () -> errorConversationNotFound(wrapper) );
	}

	public NextRTCConversation unwrap(ConversationRepository conversationRepository) {
		return NextRTCConversationWrapper.unwrapNow(this, conversationRepository);
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	private static SignalingException errorConversationNotFound(NextRTCConversationWrapper wrapper) {
		return Exceptions.CONVERSATION_NOT_FOUND.exception(
				String.format("Conversation id %s couldn't be found, so no unwrap operation will be performed.", wrapper.id));
	}

}
