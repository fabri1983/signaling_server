package org.fabri1983.signaling.core.handler.signal.onhold;

import javax.websocket.Session;

import org.fabri1983.signaling.core.CustomSignal;
import org.nextrtc.signalingserver.cases.SignalHandler;
import org.nextrtc.signalingserver.domain.InternalMessage;
import org.nextrtc.signalingserver.repository.ConversationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OnHoldSignalHandler {

	private static final Logger log = LoggerFactory.getLogger(OnHoldSignalHandler.class);
	
	public static SignalHandler onhold(ConversationRepository conversations) {
		return (msg) -> {
			Session sessionFrom = msg.getFrom().getSession();
			String roomId = msg.getContent();
			
			if (!sessionFrom.isOpen()) {
				return;
			}
			
			try {
				conversations.findBy(roomId).ifPresent( c -> {
					c.broadcast(msg.getFrom(), InternalMessage.create()
							.from(msg.getFrom())
							.signal(CustomSignal.ONHOLD)
							.content(roomId)
							.build());
				});
			} catch (Exception e) {
				log.error("Couln't send {} message from session {}. Exception: {}", CustomSignal.ONHOLD.ordinaryName(), 
						sessionFrom.getId(), e.getMessage());
	        }
		};
	}
	
}
