package org.fabri1983.signaling.core.handler.signal.pong;

import java.io.IOException;

import javax.websocket.CloseReason;
import javax.websocket.CloseReason.CloseCodes;

import org.fabri1983.signaling.core.handler.signal.drop.DropOperations;
import org.fabri1983.signaling.core.messagesender.ErrorMessageSender;
import org.fabri1983.signaling.core.task.ITask;
import org.fabri1983.signaling.core.task.ITaskManager;
import org.fabri1983.signaling.http.internalstatus.ApiStatus;
import org.fabri1983.signaling.util.UserIdMDCLogger;
import org.nextrtc.signalingserver.domain.Member;
import org.nextrtc.signalingserver.repository.MemberRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PongCloseAllTask implements ITask<String> {

	private static final Logger log = LoggerFactory.getLogger(PongCloseAllTask.class);
	
	private String id;
	private Member member;
	private MemberRepository members;
	private String userFrom;
	private ITaskManager<String> manager;
	private ErrorMessageSender errorSender;

	public PongCloseAllTask(Member member, String userFrom, ITaskManager<String> manager, ErrorMessageSender errorSender,
			MemberRepository members) {
		this.member = member;
		this.members = members;
		this.userFrom = userFrom;
		this.manager = manager;
		this.errorSender = errorSender;
		this.id = member.getId(); // it's the session id
	}

	@Override
	public String getId() {
		return id;
	}

	@Override
	public String getUserFrom() {
		return userFrom;
	}
	
	@Override
	public void run() {
		String userFromSafe = userFrom == null ? null : userFrom;
		UserIdMDCLogger.doWithLog(userFromSafe, closeTask());
	}

	private Runnable closeTask() {
		return () -> {
			
			manager.remove(this);
			
			if (!member.getSession().isOpen()) {
				return;
			}
			
			// send DROP signal to other participants
			DropOperations.processDirtyCallDrop(member.getSession(), members);
			
			// remove the member from the conversation
			member.getConversation().ifPresent( c -> c.left(member) );
			
			// let the user (which didn't send the pong at time) know that its websocket will be closed from server
			errorSender.sendErrorOverWebSocket(member.getSession(), ApiStatus.WEBSOCKET_CLOSED_PONG_TIMEOUT);
			
			try {
				// triggers onClose event from Websocket to ensure the member is unregistered from the system and so 
				// other messages are sent
				member.getSession()
					.close(new CloseReason(CloseCodes.NORMAL_CLOSURE, ApiStatus.WEBSOCKET_CLOSED_PONG_TIMEOUT.getMessage()));
				
				log.info("Closed websocket session {} for user {} due to Ping-Pong expiration time.", 
						member.getSession().getId(), userFrom);
				
			} catch (IOException e) {
				log.warn("Couldn't close websocket from member {}. Error: {}", member, e.getMessage());
			}
		};
	}
}
