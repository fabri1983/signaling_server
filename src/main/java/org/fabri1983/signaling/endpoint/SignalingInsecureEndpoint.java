package org.fabri1983.signaling.endpoint;

import java.util.function.Supplier;

import javax.websocket.Session;
import javax.websocket.server.ServerEndpoint;

import org.fabri1983.signaling.endpoint.configurator.ContextAwareEndpointConfigurator;
import org.nextrtc.signalingserver.codec.MessageDecoder;
import org.nextrtc.signalingserver.codec.MessageEncoder;
import org.nextrtc.signalingserver.domain.Message;

/**
 * This end-point only aimed to be used on development phase. The path 's-insecure' is detected on each security filter 
 * to disable any security check.
 */
@ServerEndpoint(value = "/v1/" + SignalingInsecureEndpoint.INSECURE_PATH,
	decoders = MessageDecoder.class, encoders = MessageEncoder.class,
	configurator = ContextAwareEndpointConfigurator.class)
public class SignalingInsecureEndpoint extends SignalingAbstractEndpoint {
	
	public static final String INSECURE_PATH = "s-insecure";
	
	public SignalingInsecureEndpoint() {
		super();
	}
	
	@Override
	public Supplier<Boolean> hasMatchingRoomIds(Message message, Session session) {
		return () -> { return Boolean.TRUE; };
	}
	
}
