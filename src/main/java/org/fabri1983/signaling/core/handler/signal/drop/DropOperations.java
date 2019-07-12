package org.fabri1983.signaling.core.handler.signal.drop;

import java.util.Optional;

import javax.websocket.Session;

import org.fabri1983.signaling.core.CustomSignal;
import org.nextrtc.signalingserver.domain.InternalMessage;
import org.nextrtc.signalingserver.domain.Member;
import org.nextrtc.signalingserver.repository.MemberRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DropOperations {

	private static final Logger log = LoggerFactory.getLogger(DropOperations.class);
	
	/**
	 * If participant of given session is still in the room then is considered a call drop (given the current context).
	 * Hence we must send a 'drop' signal to every other participant in the room.
	 * 
	 * @param session
	 * @param members
	 */
	public static void processDirtyCallDrop(Session session, MemberRepository members) {
		Optional<Member> memberFrom = members.findBy(session.getId());
		if (!memberFrom.isPresent() || !session.isOpen()) {
			return;
		}
		
		try {
			log.warn("Member from session {} abrupty drop call.", session.getId());
			
			memberFrom.get().getConversation().ifPresent( c -> { 
				c.broadcast(memberFrom.get(), InternalMessage.create()
					.from(memberFrom.get())
					.signal(CustomSignal.DROP)
					.content("Dirty drop call.")
					.build());
			});
		} catch (Exception e) {
			log.error("Couln't send drop message from session {}. Exception: {}", session.getId(), e.getMessage());
        }
	}
	
}
