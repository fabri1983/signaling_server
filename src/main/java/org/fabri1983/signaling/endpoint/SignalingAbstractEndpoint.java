package org.fabri1983.signaling.endpoint;

import java.util.Optional;
import java.util.function.Supplier;

import javax.inject.Inject;
import javax.websocket.CloseReason;
import javax.websocket.EndpointConfig;
import javax.websocket.OnClose;
import javax.websocket.OnError;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;

import org.fabri1983.signaling.core.SignalingConstants;
import org.fabri1983.signaling.core.handler.OnCloseHandler;
import org.fabri1983.signaling.core.handler.OnErrorHandler;
import org.fabri1983.signaling.core.handler.OnMessageHandler;
import org.fabri1983.signaling.core.handler.OnOpenHandler;
import org.fabri1983.signaling.core.messagesender.ErrorMessageSender;
import org.fabri1983.signaling.core.population.ConversationPopulation;
import org.fabri1983.signaling.http.SignalingHttpHeaderConstants;
import org.fabri1983.signaling.http.internalstatus.ValidationStatus;
import org.fabri1983.signaling.util.IFunctional;
import org.fabri1983.signaling.util.UserIdMDCLogger;
import org.nextrtc.signalingserver.api.NextRTCEndpoint;
import org.nextrtc.signalingserver.domain.Message;

public abstract class SignalingAbstractEndpoint implements IFunctional {
	
	@Inject
	private ConversationPopulation population;
	
	@Inject
	private ErrorMessageSender errorSender;
	
	@Inject
	private OnOpenHandler onOpen;
	
	@Inject
	private OnMessageHandler onMessage;
	
	@Inject
	private OnErrorHandler onError;
	
	@Inject
	private OnCloseHandler onClose;
	
	@Inject
	private NextRTCEndpoint nextRTCEndpoint;
	
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
