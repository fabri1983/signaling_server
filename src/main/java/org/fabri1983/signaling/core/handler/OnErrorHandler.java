package org.fabri1983.signaling.core.handler;

import javax.inject.Inject;
import javax.websocket.Session;

import org.fabri1983.signaling.core.handler.signal.drop.DropOperations;
import org.fabri1983.signaling.endpoint.SignalingAbstractEndpoint;
import org.nextrtc.signalingserver.repository.MemberRepository;

public class OnErrorHandler {
	
	@Inject
	private MemberRepository members;
	
	public Runnable handle(Session session, Throwable exception, SignalingAbstractEndpoint endpoint) {
		return () -> {
			
			// dirty call drop: if participant is still in the room then send drop signal to all members of the room
			DropOperations.processDirtyCallDrop(session, members);
			
			// process event on NextRTC framework
			endpoint.getNextRTCEndpoint().onError(session, exception);
		};
	}
	
}
