package org.fabri1983.signaling.core.handler.signal.pong;

import java.util.Map;

import org.fabri1983.signaling.core.SignalingConstants;
import org.fabri1983.signaling.core.messagesender.ErrorMessageSender;
import org.fabri1983.signaling.core.task.ITaskManager;
import org.fabri1983.signaling.core.task.TaskActuator;
import org.nextrtc.signalingserver.cases.SignalHandler;
import org.nextrtc.signalingserver.domain.InternalMessage;
import org.nextrtc.signalingserver.domain.Member;
import org.nextrtc.signalingserver.repository.MemberRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PongSignalHandler {

	private static final Logger log = LoggerFactory.getLogger(PongSignalHandler.class);
	
	public static <U> SignalHandler createOrReschedule(ITaskManager<U> taskManager, ErrorMessageSender errorSender,
			MemberRepository members) {
		return (msg) -> {
			Member member = msg.getFrom();
			String userFrom = getUserFrom(msg, member);
			log.debug("PONG signal received from user {}", userFrom);
			TaskActuator.getTask( taskManager, member, castToGenericType(userFrom) )
				.ifExist( PongOperations.reschedule() )
				.elsse( PongOperations.createAndSchedule(errorSender, members) )
				.go();
		};
	}

	private static String getUserFrom(InternalMessage msg, Member member) {
		String userFrom = isNullOrEmpty(msg.getCustom()) ? null : msg.getCustom().get(SignalingConstants.USER_FROM);
		userFrom = isNullOrEmpty(userFrom.toString()) ? "s_"+member.getId() : userFrom;
		return userFrom;
	}

	@SuppressWarnings("unchecked")
	private static <U> U castToGenericType(String v) {
		return (U) v;
	}

	private static boolean isNullOrEmpty(String s) {
		return s == null || s.isEmpty();
	}
	
	private static boolean isNullOrEmpty(Map<String, String> map) {
		return map == null || map.isEmpty();
	}
	
}
