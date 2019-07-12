package org.fabri1983.signaling.core.messagesender;

import javax.websocket.Session;

import org.fabri1983.signaling.http.internalstatus.InternalStatus;
import org.fabri1983.signaling.util.NoopScheduledFuture;
import org.nextrtc.signalingserver.domain.InternalMessage;
import org.nextrtc.signalingserver.domain.Member;
import org.nextrtc.signalingserver.domain.MessageSender;
import org.nextrtc.signalingserver.domain.Signal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ErrorMessageSender {

	private static final Logger log = LoggerFactory.getLogger(ErrorMessageSender.class);
	
	private MessageSender theSender;
	
	public ErrorMessageSender(MessageSender messageSender) {
		this.theSender = messageSender;
	}
	
	public void sendErrorOverWebSocket(Session session, InternalStatus internalStatus) {
		if (!session.isOpen()) {
			return;
		}
		
		try {
			String code = ""+internalStatus.getCode();
			String errorMsg = internalStatus.getMessage();
			
			theSender.send(InternalMessage.create()
					.to(new Member(session, NoopScheduledFuture.build()))
					.signal(Signal.ERROR)
					.content(code)
					.addCustom("code", code)
					.addCustom("message", errorMsg)
					.build());
			
		} catch (Exception e) {
			log.error("Couln't send error message on session {}. Exception: {}", session.getId(), e.getMessage());
        }
    }
	
}
