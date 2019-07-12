package org.fabri1983.signaling.endpoint;

import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

import javax.annotation.PostConstruct;
import javax.websocket.CloseReason;
import javax.websocket.EndpointConfig;
import javax.websocket.OnClose;
import javax.websocket.OnError;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;

import static org.fabri1983.signaling.core.handler.signal.ConditionalSignalActuator.ifSignal;
import static org.fabri1983.signaling.core.handler.signal.SignalDisjunction.or;

import org.fabri1983.signaling.configuration.SpringNextRTCComponent;
import org.fabri1983.signaling.core.CustomSignal;
import org.fabri1983.signaling.core.IJwtVerifier;
import org.fabri1983.signaling.core.SignalingConstants;
import org.fabri1983.signaling.core.handler.signal.ConditionalSignalActuator;
import org.fabri1983.signaling.core.handler.signal.cancelcall.CancelCallSignalHandler;
import org.fabri1983.signaling.core.handler.signal.drop.DropOperations;
import org.fabri1983.signaling.core.handler.signal.drop.DropSimuSignalHandler;
import org.fabri1983.signaling.core.handler.signal.onhold.OnholdSignalHandler;
import org.fabri1983.signaling.core.handler.signal.pong.PongSignalHandler;
import org.fabri1983.signaling.core.handler.signal.reject.RejectSignalHandler;
import org.fabri1983.signaling.core.handler.signal.toggle.ToggleSignalHandler;
import org.fabri1983.signaling.core.messagesender.ErrorMessageSender;
import org.fabri1983.signaling.core.population.ConversationPopulation;
import org.fabri1983.signaling.core.task.ITask;
import org.fabri1983.signaling.core.task.ITaskManager;
import org.fabri1983.signaling.http.SignalingHttpHeaderConstants;
import org.fabri1983.signaling.http.internalstatus.ValidationStatus;
import org.fabri1983.signaling.util.NoopScheduledFuture;
import org.fabri1983.signaling.util.SignalHandlerHelper;
import org.fabri1983.signaling.util.UserIdMDCLogger;
import org.nextrtc.signalingserver.api.ConfigurationBuilder;
import org.nextrtc.signalingserver.api.EndpointConfiguration;
import org.nextrtc.signalingserver.api.NextRTCEndpoint;
import org.nextrtc.signalingserver.domain.InternalMessage;
import org.nextrtc.signalingserver.domain.Member;
import org.nextrtc.signalingserver.domain.Message;
import org.nextrtc.signalingserver.domain.MessageSender;
import org.nextrtc.signalingserver.domain.Signal;
import org.nextrtc.signalingserver.repository.ConversationRepository;
import org.nextrtc.signalingserver.repository.MemberRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

public abstract class SignalingAbstractEndpoint {
	
	private static final Logger log = LoggerFactory.getLogger(SignalingAbstractEndpoint.class);
	
	@Autowired
	protected ITaskManager<String> pongTaskManager;
	
	@Autowired
	protected IJwtVerifier jwtVerifier;
	
	@Autowired
	protected ConversationPopulation<String, String, String> population;
	
	@Autowired
	protected ErrorMessageSender errorSender;
	
	@Autowired
	protected MessageSender messageSender;
	
	@Autowired
	protected MemberRepository members;
	
	@Autowired
	protected ConversationRepository conversations;

	@Autowired
	protected SpringNextRTCComponent springNextRTCComponent;
	
	// using the NextRTCEndpoint as a composite element of this endpoint
	private NextRTCEndpoint nextRTCEndpoint;
	
	@PostConstruct
	private void postConstruct() {
		this.nextRTCEndpoint = new NextRTCEndpoint(){
			@Override
			protected EndpointConfiguration manualConfiguration(final ConfigurationBuilder builder) {
				return customConfiguration(builder);
			}
		};	
	}
	
	private EndpointConfiguration customConfiguration(final ConfigurationBuilder builder) {
		
		// NOTE: This method only called once, no matter how many instances you have from NextRTCEndpoint.
		
		// tell NextRTC that use our custom Spring NextRTC Components instead of those loaded with Dagger
        EndpointConfiguration configuration = new EndpointConfiguration(springNextRTCComponent);

		// IMPORTANT: only signals not equal to those from org.nextrtc.signalingserver.domain.Signals can be used
		
		// on 'pong' signal do some rescheduling logic to eventually close the Websocket if not used anymore
		configuration.signalResolver()
			.addCustomSignal(CustomSignal.PONG, PongSignalHandler.createOrReschedule(pongTaskManager, errorSender, 
					members));
		
		// on 'reject' signal send same signal to target user
		configuration.signalResolver()
			.addCustomSignal(CustomSignal.REJECT, RejectSignalHandler.reject(conversations));

		// on 'onhold' signal send same signal to target user
		configuration.signalResolver()
			.addCustomSignal(CustomSignal.ONHOLD, OnholdSignalHandler.onhold(conversations));
		
		// on 'drop_simu' signal send same signal to target user. In this case the websocket is not disconnected at the origin source
		configuration.signalResolver()
			.addCustomSignal(CustomSignal.DROP_SIMU, DropSimuSignalHandler.dropsimu(conversations));
		
		// on 'cancel_call' signal send same signal to target user. Find the target user's session using the population object
		configuration.signalResolver()
			.addCustomSignal(CustomSignal.CANCEL_CALL, CancelCallSignalHandler.cancelCall(messageSender, population));
		
		configuration.signalResolver()
			.addCustomSignal(CustomSignal.VIDEO_OFF, ToggleSignalHandler.toggle(conversations, population, 
					CustomSignal.VIDEO_OFF));
		configuration.signalResolver()
			.addCustomSignal(CustomSignal.VIDEO_ON, ToggleSignalHandler.toggle(conversations, population, 
					CustomSignal.VIDEO_ON));
		configuration.signalResolver()
			.addCustomSignal(CustomSignal.AUDIO_OFF, ToggleSignalHandler.toggle(conversations, population, 
					CustomSignal.AUDIO_OFF));
		configuration.signalResolver()
			.addCustomSignal(CustomSignal.AUDIO_ON, ToggleSignalHandler.toggle(conversations, population, 
					CustomSignal.AUDIO_ON));
		
		return configuration;
	}
	
	/**
	 * Checks room id from message's content and message's custom map key vctoken to be equal.
	 * 
	 * @param message
	 * @return
	 */
	public abstract Supplier<Boolean> hasMatchingRoomIds(Message message, Session session);

	@OnOpen
    public void onOpen(Session session, EndpointConfig config) {
		String userFrom = getVCUserFromEndpointConfigOrSendError(session, config);
		UserIdMDCLogger.doWithLog(userFrom, handleOnOpen(session, config));
    }
	
    @OnMessage
    public void onMessage(Message message, Session session) {
		String userFrom = !isNullOrEmpty(message.getCustom()) ? 
				message.getCustom().get(SignalingConstants.USER_FROM) : "s_"+session.getId();
		UserIdMDCLogger.doWithLog(userFrom, handleOnMessage(message, session));
    }
	
	@OnError
    public void onError(Session session, Throwable exception) {
		String userFrom = Optional.ofNullable(population.getUserIdBySessionId(session.getId()))
				.orElse("s_"+session.getId());
		UserIdMDCLogger.doWithLog(userFrom, handleOnError(session, exception));
	}
	
	@OnClose
    public void onClose(Session session, CloseReason reason) {
		String userFrom = Optional.ofNullable(population.getUserIdBySessionId(session.getId()))
				.orElse("s_"+session.getId());
		UserIdMDCLogger.doWithLog(userFrom, handleOnClose(session, reason));
    }

	private Runnable handleOnOpen(Session session, EndpointConfig endpointConfig) {
		return () -> {
			
			// process event on NextRTC framework
			nextRTCEndpoint.onOpen(session, endpointConfig);
			
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
	        
			String userFrom = getVCUserFromEndpointConfigOrSendError(session, endpointConfig);
			population.addSessionByUserId(session, session.getId(), userFrom);
			
			// force a PONG message to initialize the session kill scheduler
			Message pongMessage = Message.create()
		        	.from(session.getId())
		        	.custom( SignalHandlerHelper.customMapWithUserFrom(userFrom) )
		        	.signal(CustomSignal.PONG.ordinaryName())
		        	.build();
	        onMessage(pongMessage, session);
		};
	}

	private Runnable handleOnMessage(Message message, Session session) {
		return () -> {
			
			// on 'create' or 'join' signal check equality on both room ids from message body and message token
			Boolean hasRoomIdMatched = ConditionalSignalActuator.<Boolean>
				ifSignal( message.getSignal() )
				.is( or(Signal.CREATE, Signal.JOIN) )
				.then( hasMatchingRoomIds(message, session) )
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
			nextRTCEndpoint.onMessage(message, session);
		};
	}
	
	private Runnable handleOnError(Session session, Throwable exception) {
		return () -> {
			
			// dirty call drop: if participant is still in the room then send drop signal to all members of the room
			DropOperations.processDirtyCallDrop(session, members);
			
			// process event on NextRTC framework
			nextRTCEndpoint.onError(session, exception);
		};
	}
	
	private Runnable handleOnClose(Session session, CloseReason reason) {
		return () -> {
			
			// dirty call drop: if participant is still in the room then send drop signal to all members of the room
			DropOperations.processDirtyCallDrop(session, members);
			
			// remove the session id from the room that session is associated with
			removeCountableParticipantInRoom(session);
			
			// remove pong task of being executed by the scheduler
			removePongTask(session);
			
			// process event on NextRTC framework
			nextRTCEndpoint.onClose(session, reason);
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

	private void removeCountableParticipantInRoom(Session session) {
		population.removeConversationIdBySessionId(session.getId());
	}
	
	private void removePongTask(Session session) {
		ITask<String> task = pongTaskManager.get(session.getId());
		pongTaskManager.remove(task);
	}
	
	/**
	 * Only useful when a Web Socket is open on OnOpen message, since it uses the {@link EndpointConfig} argument.
	 * 
	 * @param session
	 * @param endpointConfig
	 * @return
	 */
	private String getVCUserFromEndpointConfigOrSendError(Session session, EndpointConfig endpointConfig) {
		return Optional.ofNullable( endpointConfig.getUserProperties().get(SignalingHttpHeaderConstants.HEADER_VIDEOCHAT_USER) )
				.orElseGet( () -> {
					errorSender.sendErrorOverWebSocket(session, ValidationStatus.MISSING_VIDEO_CHAT_USERID);
					return "";
				}).toString();
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
	
	private boolean isNullOrEmpty(Map<String, String> map) {
		return map == null || map.isEmpty();
	}

    private boolean isNullOrEmpty(String s) {
		return s == null || s.isEmpty();
	}
    
}
