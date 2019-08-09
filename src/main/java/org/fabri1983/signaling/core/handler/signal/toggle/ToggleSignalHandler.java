package org.fabri1983.signaling.core.handler.signal.toggle;

import javax.websocket.Session;

import org.fabri1983.signaling.core.handler.signal.SignalHandlerHelper;
import org.fabri1983.signaling.core.population.ConversationPopulation;
import org.nextrtc.signalingserver.cases.SignalHandler;
import org.nextrtc.signalingserver.domain.InternalMessage;
import org.nextrtc.signalingserver.domain.Signal;
import org.nextrtc.signalingserver.repository.ConversationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ToggleSignalHandler {

	private static final Logger log = LoggerFactory.getLogger(ToggleSignalHandler.class);
	
	public static SignalHandler toggle(ConversationRepository conversations, 
			ConversationPopulation population, Signal signal) {
		return (msg) -> {
			
			Session sessionFrom = msg.getFrom().getSession();
			String roomId = msg.getContent();
			
			if (!sessionFrom.isOpen()) {
				return;
			}
			
			try {
				String userFrom = population.getUserIdBySessionId(sessionFrom.getId());
				
				conversations.findBy(roomId).ifPresent( c -> {
					c.broadcast(msg.getFrom(), InternalMessage.create()
							.from(msg.getFrom())
							.signal(signal)
							.content(roomId)
							.custom(SignalHandlerHelper.customMapWithUserFrom(userFrom))
							.build());
				});
			} catch (Exception e) {
				log.error("Couln't send {} message from session {}. Exception: {}", signal.ordinaryName(), 
						sessionFrom.getId(), e.getMessage());
	        }
		};
	}
	
}
