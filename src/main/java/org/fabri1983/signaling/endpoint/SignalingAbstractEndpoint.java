package org.fabri1983.signaling.endpoint;

import java.util.Optional;
import java.util.function.Supplier;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.websocket.CloseReason;
import javax.websocket.EndpointConfig;
import javax.websocket.OnClose;
import javax.websocket.OnError;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;

import org.fabri1983.signaling.configuration.SpringNextRTCComponent;
import org.fabri1983.signaling.core.CustomSignal;
import org.fabri1983.signaling.core.SignalingConstants;
import org.fabri1983.signaling.core.handler.OnCloseHandler;
import org.fabri1983.signaling.core.handler.OnErrorHandler;
import org.fabri1983.signaling.core.handler.OnMessageHandler;
import org.fabri1983.signaling.core.handler.OnOpenHandler;
import org.fabri1983.signaling.core.handler.signal.cancelcall.CancelCallSignalHandler;
import org.fabri1983.signaling.core.handler.signal.drop.DropSimuSignalHandler;
import org.fabri1983.signaling.core.handler.signal.onhold.OnHoldSignalHandler;
import org.fabri1983.signaling.core.handler.signal.pong.PongSignalHandler;
import org.fabri1983.signaling.core.handler.signal.reject.RejectSignalHandler;
import org.fabri1983.signaling.core.handler.signal.toggle.ToggleSignalHandler;
import org.fabri1983.signaling.core.messagesender.ErrorMessageSender;
import org.fabri1983.signaling.core.population.ConversationPopulation;
import org.fabri1983.signaling.core.task.ITaskManager;
import org.fabri1983.signaling.http.SignalingHttpHeaderConstants;
import org.fabri1983.signaling.http.internalstatus.ValidationStatus;
import org.fabri1983.signaling.util.IFunctional;
import org.fabri1983.signaling.util.UserIdMDCLogger;
import org.nextrtc.signalingserver.api.ConfigurationBuilder;
import org.nextrtc.signalingserver.api.EndpointConfiguration;
import org.nextrtc.signalingserver.api.NextRTCEndpoint;
import org.nextrtc.signalingserver.domain.Message;
import org.nextrtc.signalingserver.domain.MessageSender;
import org.nextrtc.signalingserver.repository.ConversationRepository;
import org.nextrtc.signalingserver.repository.MemberRepository;

public abstract class SignalingAbstractEndpoint implements IFunctional {
	
	@Inject
	private ITaskManager<String> pongTaskManager;
	
	@Inject
	private ConversationPopulation population;
	
	@Inject
	private ErrorMessageSender errorSender;
	
	@Inject
	private MessageSender messageSender;
	
	@Inject
	private MemberRepository members;
	
	@Inject
	private ConversationRepository conversations;

	@Inject
	private SpringNextRTCComponent springNextRTCComponent;
	
	@Inject
	private OnOpenHandler onOpen;
	
	@Inject
	private OnMessageHandler onMessage;
	
	@Inject
	private OnErrorHandler onError;
	
	@Inject
	private OnCloseHandler onClose;
	
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
			.addCustomSignal(CustomSignal.ONHOLD, OnHoldSignalHandler.onhold(conversations));
		
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
		UserIdMDCLogger.doWithLog(userFrom, onOpen.handle(session, config, this));
    }
	
    @OnMessage
    public void onMessage(Message message, Session session) {
		String userFrom = !isNullOrEmpty(message.getCustom()) ? 
				message.getCustom().get(SignalingConstants.USER_FROM) : generateSessionIdForLog(session);
		UserIdMDCLogger.doWithLog(userFrom, onMessage.handle(message, session, this));
    }

	@OnError
    public void onError(Session session, Throwable exception) {
		String userFrom = Optional.ofNullable(population.getUserIdBySessionId(session.getId()))
				.orElse(generateSessionIdForLog(session));
		UserIdMDCLogger.doWithLog(userFrom, onError.handle(session, exception, this));
	}
	
	@OnClose
    public void onClose(Session session, CloseReason reason) {
		String userFrom = Optional.ofNullable(population.getUserIdBySessionId(session.getId()))
				.orElse(generateSessionIdForLog(session));
		UserIdMDCLogger.doWithLog(userFrom, onClose.handle(session, reason, this));
    }

	public NextRTCEndpoint getNextRTCEndpoint() {
		return nextRTCEndpoint;
	}
	
	/**
	 * Only useful when a Web Socket is open on OnOpen message, since it uses the {@link EndpointConfig} argument.
	 * 
	 * @param session
	 * @param endpointConfig
	 * @return
	 */
	public String getVCUserFromEndpointConfigOrSendError(Session session, EndpointConfig endpointConfig) {
		return Optional.ofNullable( endpointConfig.getUserProperties().get(SignalingHttpHeaderConstants.HEADER_VIDEOCHAT_USER) )
				.orElseGet( () -> {
					errorSender.sendErrorOverWebSocket(session, ValidationStatus.MISSING_VIDEO_CHAT_USERID);
					return "";
				}).toString();
	}

	private String generateSessionIdForLog(Session session) {
		return "s_" + session.getId();
	}
    
}
