package org.fabri1983.signaling.configuration;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ScheduledExecutorService;

import javax.inject.Inject;

import org.apache.catalina.Context;
import org.apache.tomcat.util.scan.StandardJarScanner;
import org.fabri1983.signaling.core.IJwtVerifier;
import org.fabri1983.signaling.core.JwtVerifier;
import org.fabri1983.signaling.core.handler.OnCloseHandler;
import org.fabri1983.signaling.core.handler.OnErrorHandler;
import org.fabri1983.signaling.core.handler.OnMessageHandler;
import org.fabri1983.signaling.core.handler.OnOpenHandler;
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
import org.nextrtc.signalingserver.Names;
import org.nextrtc.signalingserver.NextRTCComponent;
import org.nextrtc.signalingserver.domain.MessageSender;
import org.nextrtc.signalingserver.property.NextRTCProperties;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.boot.web.servlet.error.ErrorAttributes;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.PropertySource;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.ClassPathResource;
import org.springframework.scheduling.concurrent.ScheduledExecutorFactoryBean;
import org.springframework.web.socket.server.standard.ServerEndpointExporter;

@Configuration
@Import(value = { LocalSignalingConfiguration.class, HazelcastSignalingConfiguration.class })
// The next @ComponentScan annotation is used only to let Spring creates all the necessary beans except for the
// NextRTCEndpoint bean which we don't want to instantiate due to its static instance creation logic which disables
// completely the use of custom SignalResolver instances.
@ComponentScan(
		basePackages = {
				"org.nextrtc.signalingserver.cases",    "org.nextrtc.signalingserver.domain", 
				"org.nextrtc.signalingserver.eventbus", "org.nextrtc.signalingserver.factory",
				"org.nextrtc.signalingserver.modules",  "org.nextrtc.signalingserver.property",
				"org.nextrtc.signalingserver.repository"}
)
@PropertySource(value={"classpath:application.properties", "classpath:nextrtc.properties"})
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

	@Inject
	private ErrorAttributes errorAttributes;
	
	/*######################################################################################################
	 * Next section are beans needed by instances created on org.nextrtc.signalingserver package.
	 * We manually created them here because we have excluded its configuration class NextRTCConfig.class.
	 *######################################################################################################*/
    
    @Bean
    public static PropertySourcesPlaceholderConfigurer propertySourcesPlaceholderConfigurer() {
        PropertySourcesPlaceholderConfigurer propertyPlaceholderConfigurer = new PropertySourcesPlaceholderConfigurer();
        propertyPlaceholderConfigurer.setLocation(new ClassPathResource("nextrtc.properties"));
        return propertyPlaceholderConfigurer;
    }

    /**
     * Ping Scheduler.
     * 
     * @param nextRTCProperties
     * @return
     */
    @Bean(name = Names.SCHEDULER_NAME)
    public ScheduledExecutorService scheduler(NextRTCProperties nextRTCProperties) {
        ScheduledExecutorFactoryBean factoryBean = new ScheduledExecutorFactoryBean();
        factoryBean.setThreadNamePrefix(Names.SCHEDULER_NAME);
        factoryBean.setPoolSize(nextRTCProperties.getSchedulerPoolSize());
        factoryBean.afterPropertiesSet();
        return factoryBean.getObject();
    }
	
    @Bean
    public NextRTCComponent nextRTCComponent() {
    	return new SpringNextRTCComponent();
    }
    
    /*######################################################################################################*/
    
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
	@ConditionalOnProperty(prefix="app", name="allow.insecure.path", havingValue="true")
	public SignalingInsecureEndpoint signalingInsecureEndpoint() {
		return new SignalingInsecureEndpoint();
	}
	
	@Bean
	public AppErrorController appErrorController() {
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
