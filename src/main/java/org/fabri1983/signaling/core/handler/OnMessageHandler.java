package org.fabri1983.signaling.core.handler;

import java.util.function.Supplier;

import javax.inject.Inject;
import javax.websocket.Session;

import static org.fabri1983.signaling.core.handler.signal.ConditionalSignalActuator.ifSignal;
import static org.fabri1983.signaling.core.handler.signal.SignalDisjunction.or;

import org.fabri1983.signaling.core.handler.signal.ConditionalSignalActuator;
import org.fabri1983.signaling.core.messagesender.ErrorMessageSender;
import org.fabri1983.signaling.core.population.ConversationPopulation;
import org.fabri1983.signaling.endpoint.SignalingAbstractEndpoint;
import org.fabri1983.signaling.http.internalstatus.ValidationStatus;
import org.nextrtc.signalingserver.domain.Message;
import org.nextrtc.signalingserver.domain.Signal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OnMessageHandler {

	private static final Logger log = LoggerFactory.getLogger(OnMessageHandler.class);
	
	@Inject
	private ConversationPopulation population;
	
	@Inject
	private ErrorMessageSender errorSender;
	
	public Runnable handle(Message message, Session session, SignalingAbstractEndpoint endpoint) {
		return () -> {
			
			// on 'create' or 'join' signal check equality on both room ids from message body and message token
			Boolean hasRoomIdMatched = ConditionalSignalActuator.<Boolean>
				ifSignal( message.getSignal() )
				.is( or(Signal.CREATE, Signal.JOIN) )
				.then( endpoint.hasMatchingRoomIds(message, session) )
				.get();
			
			// short circuit condition
			if (Boolean.FALSE.equals(hasRoomIdMatched)) {
				return; // error signal was sent in previous step
			}
			
			// on 'create' or 'join' signal check number of participants in the target room id
			Boolean maxParticipantsExceeded = ConditionalSignalActuator.<Boolean>
				ifSignal( message.getSignal() )
				.is( or(Signal.CREATE, Signal.JOIN) )
				.then( exceedMaxNumberParticipants(message, session) )
				.get();
			
			// short circuit condition
			if (Boolean.TRUE.equals(maxParticipantsExceeded)) {
				return; // error signal was sent in previous step
			}
			
			// on 'create' or 'join' signals count the participant for the given the room
			ifSignal( message.getSignal() )
				.is( or(Signal.CREATE, Signal.JOIN) )
				.then( addCountableParticipantInRoom(message, session) )
				.go();
			
			// on 'left' signal discount the participant for the given the room
			ifSignal( message.getSignal() )
				.is( or(Signal.LEFT) )
				.then( removeCountableParticipantInRoom(message, session) )
				.go();
			
			// process event on NextRTC framework
			endpoint.getNextRTCEndpoint().onMessage(message, session);
		};
	}
	
	/**
	 * Checks number of participants on the target room. If max is reached then throw error on Websocket.
	 * 
	 * @param message
	 * @return
	 */
	private Supplier<Boolean> exceedMaxNumberParticipants(Message message, Session session) {
		return () -> {
			String roomId = message.getContent();
			if (population.at(roomId).hasReachedMax(session.getId())) {
				log.warn("Max particpants already reached for requested room {}", roomId);
				errorSender.sendErrorOverWebSocket(session, ValidationStatus.ROOM_MAX_PARTICIPANTS_REACHED);
				return Boolean.TRUE;
			}
			return Boolean.FALSE;
		};
	}
	
	private Runnable addCountableParticipantInRoom(Message message, Session session) {
		return () -> {
			String roomId = message.getContent();
			population.addSessionIdAtConversationId(roomId, session.getId());
		};
	}
	
	private Runnable removeCountableParticipantInRoom(Message message, Session session) {
		return () -> {
			String roomId = message.getContent();
			population.removeSessionIdByConversationId(roomId, session.getId());
		};
	}
	
}
