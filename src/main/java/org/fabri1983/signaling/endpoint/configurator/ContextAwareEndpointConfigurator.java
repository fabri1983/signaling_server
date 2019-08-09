package org.fabri1983.signaling.endpoint.configurator;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.websocket.HandshakeResponse;
import javax.websocket.server.HandshakeRequest;
import javax.websocket.server.ServerEndpointConfig;

import org.fabri1983.signaling.core.IJwtVerifier;
import org.fabri1983.signaling.core.SignalingConstants;
import org.fabri1983.signaling.endpoint.SignalingInsecureEndpoint;
import org.fabri1983.signaling.http.SignalingHttpHeaderConstants;
import org.fabri1983.signaling.http.exception.ValidationException;
import org.fabri1983.signaling.http.internalstatus.ValidationStatus;
import org.fabri1983.signaling.util.IFunctional;
import org.fabri1983.signaling.util.UserIdMDCLogger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.web.socket.server.standard.SpringConfigurator;

/**
 * With this custom class we solved the issue in which SpringConfigurator can not find a WebApplicationContext (probably 
 * because this SpringBoot app extends from a Servlet Aware Context instead of other similar to Web App Servlet Aware Context).
 * Use this class as a Configurator to your Websocket Endpoints.
 */
public class ContextAwareEndpointConfigurator extends SpringConfigurator implements ApplicationContextAware, IFunctional {

	private static final Logger log = LoggerFactory.getLogger(ContextAwareEndpointConfigurator.class);
	
	private static volatile ApplicationContext context;
	
	private static IJwtVerifier jwtVerifier;
	
	/**
	 * Constructor only called by Tomcat's Websocket Server Initializer before Spring enters into action.
	 */
	public ContextAwareEndpointConfigurator() {
		super();
	}
	
	/**
	 * Constructor only called by Spring.
	 * 
	 * @param jwtVerifier
	 */
	public ContextAwareEndpointConfigurator(IJwtVerifier jwtVerifier) {
		super();
		ContextAwareEndpointConfigurator.jwtVerifier = jwtVerifier;
	}

	@Override
	public <T> T getEndpointInstance(Class<T> clazz) throws InstantiationException {
		return context.getBean(clazz);
	}

	@Override
	public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
		context = applicationContext;
	}

    @Override
    public void modifyHandshake(ServerEndpointConfig config, HandshakeRequest request, HandshakeResponse response) {
    	logIfInsecurePath(request);
        String vcuser = getVcuserOrThrow(request);
        String vctoken = getVctokenOrThrow(request);
        config.getUserProperties().put(SignalingHttpHeaderConstants.HEADER_VIDEOCHAT_USER, vcuser);
        config.getUserProperties().put(SignalingConstants.ENDPOINT_CONFIG_USER_TO, jwtVerifier.getUserToFrom(vctoken));
        config.getUserProperties().put(SignalingConstants.ENDPOINT_CONFIG_ROOM_ID, jwtVerifier.getRoomIdFrom(vctoken));
        UserIdMDCLogger.doWithLog(vcuser, () -> log.info("Upgrading to websocket and processing handshake."));
    }

	private String getVcuserOrThrow(HandshakeRequest request) {
		Map<String, List<String>> headers = request.getHeaders();
	    List<String> values = headers != null ? headers.get(SignalingHttpHeaderConstants.HEADER_VIDEOCHAT_USER) : Collections.emptyList();
	    if (isNullOrEmpty(values) || isNullOrEmpty(values.get(0))) {
	    	if (isInsecuredPath(request)) {
	    		return "";
	    	} else {
	    		throw new ValidationException(ValidationStatus.MISSING_VIDEO_CHAT_USERID);
	    	}
	    }
	    return values.get(0);
	}

	private String getVctokenOrThrow(HandshakeRequest request) {
		Map<String, List<String>> headers = request.getHeaders();
	    List<String> values = headers != null ? headers.get(SignalingHttpHeaderConstants.HEADER_VIDEOCHAT_TOKEN) : Collections.emptyList();
	    if (isNullOrEmpty(values) || isNullOrEmpty(values.get(0))) {
	    	if (isInsecuredPath(request)) {
	    		return "";
	    	} else {
	    		throw new ValidationException(ValidationStatus.MISSING_VIDEO_CHAT_TOKEN);
	    	}
	    }
	    return values.get(0);
	}

	private boolean isInsecuredPath(HandshakeRequest request) {
		return request.getRequestURI().toString().contains(SignalingInsecureEndpoint.INSECURE_PATH);
	}
	
	private void logIfInsecurePath(HandshakeRequest request) {
		if (isInsecuredPath(request)) {
			log.warn("GOING THROUGH INSECURE PATH!");
		}
	}
	
}
