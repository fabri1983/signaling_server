package org.fabri1983.signaling.core.handler.signal.cancelcall;

import java.util.Map;

import javax.websocket.Session;

import org.fabri1983.signaling.core.CustomSignal;
import org.fabri1983.signaling.core.SignalingConstants;
import org.fabri1983.signaling.core.handler.signal.SignalHandlerHelper;
import org.fabri1983.signaling.core.population.ConversationPopulation;
import org.fabri1983.signaling.util.NoopScheduledFuture;
import org.nextrtc.signalingserver.cases.SignalHandler;
import org.nextrtc.signalingserver.domain.InternalMessage;
import org.nextrtc.signalingserver.domain.Member;
import org.nextrtc.signalingserver.domain.MessageSender;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CancelCallSignalHandler {

	private static final Logger log = LoggerFactory.getLogger(CancelCallSignalHandler.class);
	
	/**
	 * When the caller decides to cancel the current call just before the callee drops it.
	 * 
	 * @param messageSender
	 * @param population
	 * @return
	 */
	public static SignalHandler cancelCall(MessageSender messageSender, ConversationPopulation population) {
		return (msg) -> {
			
			if (!isValidUserTo(msg)) {
				return;
			}
			
			Session sessionFrom = msg.getFrom().getSession();
			if (!sessionFrom.isOpen()) {
				return;
			}
			
			String userTo =  msg.getCustom().get(SignalingConstants.USER_TO);
			
			try {
				Session sessionTo = population.getSessionByUserId(userTo);
				if (sessionTo == null) {
					return;
				}
				
				String userFrom = population.getUserIdBySessionId(sessionFrom.getId());
				
				messageSender.send(InternalMessage.create()
						.from(msg.getFrom())
						.to(new Member(sessionTo, NoopScheduledFuture.build()))
						.signal(CustomSignal.CANCEL_CALL)
						.custom(SignalHandlerHelper.customMapWithUserFrom(userFrom))
						.build());
				
			} catch (Exception e) {
				log.error("Couln't send {} message from session {} to userTo {}. Exception: {}", 
						CustomSignal.CANCEL_CALL.ordinaryName(), sessionFrom.getId(), userTo, e.getMessage());
	        }
		};
	}
	
	private static boolean isValidUserTo(InternalMessage msg) {
		if (isNullOrEmpty(msg.getCustom())) {
			log.warn("Missing custom map.");
			return false;
		}
		String userTo = msg.getCustom().get(SignalingConstants.USER_TO);
		if (isNullOrEmpty(userTo)) {
			log.warn("Missing {} key in custom map.", SignalingConstants.USER_TO);
			return false;
		}
		String userFrom = msg.getCustom().get(SignalingConstants.USER_FROM);
		if (userTo.equals(userFrom)) {
			log.warn("Keys {} and {} in custom map have same value.", SignalingConstants.USER_FROM, SignalingConstants.USER_TO);
			return false;
		}
		return true;
	}

	private static boolean isNullOrEmpty(String s) {
		return s == null || s.isEmpty();
	}
	
	private static boolean isNullOrEmpty(Map<String, String> map) {
		return map == null || map.isEmpty();
	}
	
}
