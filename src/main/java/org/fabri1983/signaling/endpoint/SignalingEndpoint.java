package org.fabri1983.signaling.endpoint;

import java.util.function.Supplier;

import javax.inject.Inject;
import javax.websocket.Session;
import javax.websocket.server.ServerEndpoint;

import org.fabri1983.signaling.core.IJwtVerifier;
import org.fabri1983.signaling.core.messagesender.ErrorMessageSender;
import org.fabri1983.signaling.endpoint.configurator.ContextAwareEndpointConfigurator;
import org.fabri1983.signaling.http.SignalingHttpHeaderConstants;
import org.fabri1983.signaling.http.internalstatus.ValidationStatus;
import org.nextrtc.signalingserver.codec.MessageDecoder;
import org.nextrtc.signalingserver.codec.MessageEncoder;
import org.nextrtc.signalingserver.domain.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ServerEndpoint(value = "/v1/" + SignalingEndpoint.SECURE_PATH,
	decoders = MessageDecoder.class, encoders = MessageEncoder.class,
	configurator = ContextAwareEndpointConfigurator.class)
public class SignalingEndpoint extends SignalingAbstractEndpoint {
	
	private static final Logger log = LoggerFactory.getLogger(SignalingEndpoint.class);
	
	public static final String SECURE_PATH = "s";
	
	@Inject
	private IJwtVerifier jwtVerifier;
	
	@Inject
	private ErrorMessageSender errorSender;
	
	public SignalingEndpoint() {
		super();
	}
	
	@Override
	public Supplier<Boolean> hasMatchingRoomIds(Message message, Session session) {
		return () -> {
			try {
				String token = message.getCustom().get(SignalingHttpHeaderConstants.HEADER_VIDEOCHAT_TOKEN);
				String roomId = message.getContent();
				if (!jwtVerifier.matchRoomIdWithClaim(token, roomId)) {
					errorSender.sendErrorOverWebSocket(session, ValidationStatus.ROOM_ID_DOES_NOT_MATCH_CLAIM);
					return Boolean.FALSE;
				}
				return Boolean.TRUE;
			} catch (Exception e) {
				log.warn("Something went wrong when validating matching room ids. Exception: {}", e.getMessage());
			}
			return Boolean.FALSE;
		};
	}
	
}
