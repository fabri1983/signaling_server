package org.fabri1983.signaling.core.handler;

import javax.inject.Inject;
import javax.websocket.CloseReason;
import javax.websocket.Session;

import org.fabri1983.signaling.core.handler.signal.drop.DropOperations;
import org.fabri1983.signaling.core.population.ConversationPopulation;
import org.fabri1983.signaling.core.task.ITask;
import org.fabri1983.signaling.core.task.ITaskManager;
import org.fabri1983.signaling.endpoint.SignalingAbstractEndpoint;
import org.nextrtc.signalingserver.repository.MemberRepository;

public class OnCloseHandler {

	@Inject
	private ITaskManager<String> pongTaskManager;
	
	@Inject
	private ConversationPopulation population;
	
	@Inject
	private MemberRepository members;
	
	public Runnable handle(Session session, CloseReason reason, SignalingAbstractEndpoint endpoint) {
		return () -> {
			
			// dirty call drop: if participant is still in the room then send drop signal to all members of the room
			DropOperations.processDirtyCallDrop(session, members);
			
			// remove the session id from the room that session is associated with
			removeCountableParticipantInRoom(session);
			
			// remove pong task of being executed by the scheduler
			removePongTask(session);
			
			// process event on NextRTC framework
			endpoint.getNextRTCEndpoint().onClose(session, reason);
		};
	}
	
	private void removeCountableParticipantInRoom(Session session) {
		population.removeConversationIdBySessionId(session.getId());
	}
	
	private void removePongTask(Session session) {
		ITask<String> task = pongTaskManager.get(session.getId());
		pongTaskManager.remove(task);
	}
	
}
