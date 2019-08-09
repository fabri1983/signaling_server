package org.fabri1983.signaling.core.handler;

import java.util.Optional;

import javax.inject.Inject;
import javax.websocket.EndpointConfig;
import javax.websocket.Session;

import org.fabri1983.signaling.core.CustomSignal;
import org.fabri1983.signaling.core.SignalingConstants;
import org.fabri1983.signaling.core.handler.signal.SignalHandlerHelper;
import org.fabri1983.signaling.core.population.ConversationPopulation;
import org.fabri1983.signaling.endpoint.SignalingAbstractEndpoint;
import org.fabri1983.signaling.util.IFunctional;
import org.fabri1983.signaling.util.NoopScheduledFuture;
import org.nextrtc.signalingserver.domain.InternalMessage;
import org.nextrtc.signalingserver.domain.Member;
import org.nextrtc.signalingserver.domain.Message;
import org.nextrtc.signalingserver.domain.MessageSender;
import org.nextrtc.signalingserver.repository.ConversationRepository;

public class OnOpenHandler implements IFunctional {

	@Inject
	private ConversationPopulation population;
	
	@Inject
	private MessageSender messageSender;
	
	@Inject
	private ConversationRepository conversations;

	public Runnable handle(Session session, EndpointConfig endpointConfig, SignalingAbstractEndpoint endpoint) {
		return () -> {
			
			// process event on NextRTC framework
			endpoint.getNextRTCEndpoint().onOpen(session, endpointConfig);
			
			// let the client knows its own session id
	        messageSender.send(InternalMessage.create()
					.to(new Member(session, NoopScheduledFuture.build()))
					.signal(CustomSignal.OPEN)
					.build());
	        
	        // let the client knows if the callee is already in another conversation or in the current one
	        String userTo = getUserToFromEndpointConfig(endpointConfig);
	        String roomId = getRoomIdFromEndpointConfig(endpointConfig);
	        if (isInAnotherRoomAlready(userTo, roomId)) {
		        messageSender.send(InternalMessage.create()
						.to(new Member(session, null))
						.signal(CustomSignal.OTHER_IN_CALL)
						.build());
	        } else if (isInThisRoomAlready(userTo, roomId)) {
		        messageSender.send(InternalMessage.create()
						.to(new Member(session, null))
						.signal(CustomSignal.OTHER_IN_ROOM)
						.build());
	        }
	        
			String userFrom = endpoint.getVCUserFromEndpointConfigOrSendError(session, endpointConfig);
			population.addSessionByUserId(session, session.getId(), userFrom);
			
			// force a PONG message to initialize the session kill scheduler
			Message pongMessage = Message.create()
		        	.from(session.getId())
		        	.custom( SignalHandlerHelper.customMapWithUserFrom(userFrom) )
		        	.signal(CustomSignal.PONG.ordinaryName())
		        	.build();
			endpoint.onMessage(pongMessage, session);
		};
	}
	
	private boolean isInAnotherRoomAlready(String userId, String roomId) {
		if (isNullOrEmpty(userId) || isNullOrEmpty(roomId) || population.getSessionByUserId(userId) == null) {
			return false;
		}
		Member member = new Member(population.getSessionByUserId(userId), NoopScheduledFuture.build());
		Boolean isInAnotherRoom = conversations.findBy(member)
			.map( c -> !c.getId().equals(roomId) ).orElse(Boolean.FALSE);
		return isInAnotherRoom.booleanValue();
	}

	private boolean isInThisRoomAlready(String userId, String roomId) {
		if (isNullOrEmpty(userId) || isNullOrEmpty(roomId) || population.getSessionByUserId(userId) == null) {
			return false;
		}
		Member member = new Member(population.getSessionByUserId(userId), NoopScheduledFuture.build());
		Boolean isInThisRoom = conversations.findBy(member)
			.map( c -> c.getId().equals(roomId) ).orElse(Boolean.FALSE);
		return isInThisRoom.booleanValue();
	}
	
	/**
	 * Only useful when a Web Socket is open on OnOpen message, since it uses the {@link EndpointConfig} argument.
	 * 
	 * @param endpointConfig
	 * @return
	 */
	private String getUserToFromEndpointConfig(EndpointConfig endpointConfig) {
		return Optional.ofNullable( endpointConfig.getUserProperties().get(SignalingConstants.ENDPOINT_CONFIG_USER_TO) )
				.orElse("").toString();
	}
	
	/**
	 * Only useful when a Web Socket is open on OnOpen message, since it uses the {@link EndpointConfig} argument.
	 * 
	 * @param endpointConfig
	 * @return
	 */
	private String getRoomIdFromEndpointConfig(EndpointConfig endpointConfig) {
		return Optional.ofNullable( endpointConfig.getUserProperties().get(SignalingConstants.ENDPOINT_CONFIG_ROOM_ID) )
				.orElse("").toString();
	}
	
}
