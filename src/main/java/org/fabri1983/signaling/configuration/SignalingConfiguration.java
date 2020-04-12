package org.fabri1983.signaling.configuration;

import java.util.Arrays;
import java.util.List;

import org.apache.catalina.Context;
import org.apache.tomcat.util.scan.StandardJarScanner;
import org.fabri1983.signaling.core.CustomSignal;
import org.fabri1983.signaling.core.IJwtVerifier;
import org.fabri1983.signaling.core.JwtVerifier;
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
import org.fabri1983.signaling.core.task.DummyTaskManager;
import org.fabri1983.signaling.core.task.ITaskManager;
import org.fabri1983.signaling.core.task.TaskManager;
import org.fabri1983.signaling.endpoint.SignalingEndpoint;
import org.fabri1983.signaling.endpoint.SignalingInsecureEndpoint;
import org.fabri1983.signaling.endpoint.configurator.ContextAwareEndpointConfigurator;
import org.fabri1983.signaling.http.controller.AppErrorController;
import org.fabri1983.signaling.http.filter.ApiExceptionResponseFilter;
import org.fabri1983.signaling.http.filter.PresentUserIdFilter;
import org.fabri1983.signaling.http.filter.SkipApiExceptionFilter;
import org.fabri1983.signaling.http.filter.VideochatTokenFilter;
import org.fabri1983.signaling.http.validator.PresentUserIdValidator;
import org.fabri1983.signaling.http.validator.VideoChatTokenValidator;
import org.nextrtc.signalingserver.api.ConfigurationBuilder;
import org.nextrtc.signalingserver.api.EndpointConfiguration;
import org.nextrtc.signalingserver.api.NextRTCEndpoint;
import org.nextrtc.signalingserver.domain.MessageSender;
import org.nextrtc.signalingserver.repository.ConversationRepository;
import org.nextrtc.signalingserver.repository.MemberRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.boot.web.servlet.error.ErrorAttributes;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.PropertySource;
import org.springframework.core.annotation.Order;
import org.springframework.web.socket.server.standard.ServerEndpointExporter;

@Configuration(proxyBeanMethods = false)
@Import(value = { SpringNextRTCConfiguration.class, LocalSignalingConfiguration.class, HazelcastSignalingConfiguration.class })
@PropertySource(value = { "classpath:application.properties", "classpath:nextrtc.properties", "classpath:environment.properties" })
public class SignalingConfiguration {

	private static final List<String> URL_PATTERNS_FOR_FILTERS = Arrays.asList("/v1/*", "/v2/*", "/v3/*");
	
	/**
	 * Disable Tomcat's scan jars feature.
	 */
	@Bean
	public TomcatServletWebServerFactory tomcatFactory() {
		return new TomcatServletWebServerFactory() {
			@Override
			protected void postProcessContext(Context context) {
				((StandardJarScanner) context.getJarScanner()).setScanManifest(false);
			}
		};
	}

	@Bean
	public ContextAwareEndpointConfigurator endpointConfigurator(IJwtVerifier jwtVerifier) {
		return new ContextAwareEndpointConfigurator(jwtVerifier);
	}

	@Bean
	public OnOpenHandler onOpenHandler() {
		return new OnOpenHandler();
	}

	@Bean
	public OnMessageHandler onMessageHandler() {
		return new OnMessageHandler();
	}

	@Bean
	public OnErrorHandler onErrorHandler() {
		return new OnErrorHandler();
	}

	@Bean
	public OnCloseHandler onCloseHandler() {
		return new OnCloseHandler();
	}

	@Bean
	public ConversationPopulation population(
			@Value("${nextrtc.max_participants_per_room}") int maxParticipantsPerRoom) {
		return new ConversationPopulation(maxParticipantsPerRoom);
	}

	@Bean
	public ErrorMessageSender errorMessageSender(MessageSender messageSender) {
		return new ErrorMessageSender(messageSender);
	}

	@Bean
	public IJwtVerifier jwtVerifier(
			@Value("${jwt.audience}") String audience, 
			@Value("${jwt.issuer}") String issuer,
			@Value("${jwt.public.key.file.name}") String publicKeyFilename, 
			@Value("${jwt.private.key.file.name}") String privateKeyFilename) {
		return new JwtVerifier(audience, issuer, publicKeyFilename, privateKeyFilename);
	}
	
	@Bean
	public ITaskManager<String> taskManager(
			@Value("${nextrtc.ping_period}") int pingDelaySeconds, 
			@Value("${nextrtc.pong_enabled:true}") boolean pongEnabled) {
		if (!pongEnabled) {
			return new DummyTaskManager<String>();
		} else {
			return new TaskManager<String>((int)Math.ceil((pingDelaySeconds*1000) * 1.5)); // wait 1.5 times the ping period time
		}
	}

	@Bean
	public ServerEndpointExporter serverEndpointExporter() {
		return new ServerEndpointExporter();
	}

	@Bean
	public SignalingEndpoint signalingEndpoint() {
		return new SignalingEndpoint();
	}

	@Bean
	@ConditionalOnProperty(prefix = "app", name = "allow.insecure.path", havingValue = "true")
	public SignalingInsecureEndpoint signalingInsecureEndpoint() {
		return new SignalingInsecureEndpoint();
	}

	@Bean
	public AppErrorController appErrorController(ErrorAttributes errorAttributes) {
		return new AppErrorController(errorAttributes);
	}

	@Bean
	public PresentUserIdValidator presentUserIdValidator() {
		return new PresentUserIdValidator();
	}

	@Bean
	public VideoChatTokenValidator videoChatTokenValidator(IJwtVerifier jwtVerifier) {
		return new VideoChatTokenValidator(jwtVerifier);
	}

	@Bean
	@Primary
	public NextRTCEndpoint nextRTCEndpoint(ITaskManager<String> pongTaskManager, MessageSender messageSender,
			MemberRepository members, ConversationRepository conversations, SpringNextRTCComponent nextRTCComponent,
			ConversationPopulation population, ErrorMessageSender errorSender) {
		
		return new NextRTCEndpoint() {
			
			@Override
			protected EndpointConfiguration manualConfiguration(final ConfigurationBuilder builder) {
				
				// NOTE: This method only called once, no matter how many instances you have from NextRTCEndpoint.
				
				// tell NextRTC that use our custom Spring NextRTC Components instead of those loaded with Dagger
		        EndpointConfiguration configuration = new EndpointConfiguration(nextRTCComponent);

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
		};	
	}
	
	@Bean
	@Order(1)
	public FilterRegistrationBean<ApiExceptionResponseFilter> apiExceptionResponseFilter() {
		FilterRegistrationBean<ApiExceptionResponseFilter> frb = new FilterRegistrationBean<ApiExceptionResponseFilter>();
		ApiExceptionResponseFilter apiExceptionResponseFilter = new ApiExceptionResponseFilter();
		frb.setFilter(apiExceptionResponseFilter);
		frb.setName("apiExceptionResponseFilter");
		frb.setUrlPatterns(URL_PATTERNS_FOR_FILTERS);
		return frb;
	}

	@Bean
	@Order(2)
	public FilterRegistrationBean<SkipApiExceptionFilter> presentUserIdFilter(PresentUserIdValidator presentUserIdValidator) {
		FilterRegistrationBean<SkipApiExceptionFilter> frb = new FilterRegistrationBean<SkipApiExceptionFilter>();
		SkipApiExceptionFilter presentUserIdFilter = SkipApiExceptionFilter.builder()
				.forPath(SignalingInsecureEndpoint.INSECURE_PATH)
				.theseExceptionCodes(presentUserIdValidator.getValidationCodes())
				.filter(new PresentUserIdFilter(presentUserIdValidator))
				.build();
		frb.setFilter(presentUserIdFilter);
		frb.setName("presentUserIdFilter");
		frb.setUrlPatterns(URL_PATTERNS_FOR_FILTERS);
		return frb;
	}
	
	@Bean
	@Order(3)
	public FilterRegistrationBean<SkipApiExceptionFilter> videoChatTokenFilter(VideoChatTokenValidator videoChatTokenValidator) {
		FilterRegistrationBean<SkipApiExceptionFilter> frb = new FilterRegistrationBean<SkipApiExceptionFilter>();
		SkipApiExceptionFilter videochatTokenFilter = SkipApiExceptionFilter.builder()
				.forPath(SignalingInsecureEndpoint.INSECURE_PATH)
				.theseExceptionCodes(videoChatTokenValidator.getValidationCodes())
				.filter(new VideochatTokenFilter(videoChatTokenValidator))
				.build();
		frb.setFilter(videochatTokenFilter);
		frb.setName("videochatTokenFilter");
		frb.setUrlPatterns(URL_PATTERNS_FOR_FILTERS);
		return frb;
	}

}
