package org.fabri1983.signaling.configuration;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;

import javax.inject.Inject;

import org.apache.commons.lang3.tuple.Pair;
import org.nextrtc.signalingserver.Names;
import org.nextrtc.signalingserver.NextRTCComponent;
import org.nextrtc.signalingserver.api.NextRTCEndpoint;
import org.nextrtc.signalingserver.api.NextRTCEventBus;
import org.nextrtc.signalingserver.api.NextRTCEvents;
import org.nextrtc.signalingserver.api.annotation.NextRTCEventListener;
import org.nextrtc.signalingserver.cases.SignalHandler;
import org.nextrtc.signalingserver.domain.MessageSender;
import org.nextrtc.signalingserver.domain.Server;
import org.nextrtc.signalingserver.domain.Signal;
import org.nextrtc.signalingserver.domain.resolver.ManualSignalResolver;
import org.nextrtc.signalingserver.domain.resolver.SpringSignalResolver;
import org.nextrtc.signalingserver.eventbus.ManualEventDispatcher;
import org.nextrtc.signalingserver.eventbus.SpringEventDispatcher;
import org.nextrtc.signalingserver.property.ManualNextRTCProperties;
import org.nextrtc.signalingserver.property.NextRTCProperties;
import org.nextrtc.signalingserver.repository.ConversationRepository;
import org.nextrtc.signalingserver.repository.MemberRepository;
import org.springframework.aop.framework.Advised;
import org.springframework.aop.support.AopUtils;
import org.springframework.context.ApplicationContext;

/**
 * NextRTC components managed by Spring, since we use the Spring framework instead of Dagger 2 as the NextRTC api does.
 */
public class SpringNextRTCComponent implements NextRTCComponent {

	@Inject
    private NextRTCProperties nextRTCProperties;
	
	@Inject
    private NextRTCEventBus eventBus;
	
	@Inject
	private SpringSignalResolver springSignalResolver;
	
	@Inject
    private ApplicationContext context;
	
	@Inject
	private MessageSender messageSender;
	
	@Inject
	private MemberRepository memberRepository;
	
	@Inject
	private ConversationRepository conversationRepository;
	
	@Inject
	private Server server;
	
	@Override
	public ManualNextRTCProperties manualProperties() {
		ManualNextRTCProperties newObj = new ManualNextRTCProperties();
		newObj.setMaxConnectionSetupTime(nextRTCProperties.getMaxConnectionSetupTime());
		newObj.setJoinOnlyToExisting(nextRTCProperties.isJoinOnlyToExisting());
		newObj.setPingPeriod(nextRTCProperties.getPingPeriod());
		newObj.setSchedulerPoolSize(nextRTCProperties.getSchedulerPoolSize());
		return newObj;
	}

	@Override
	public ManualEventDispatcher manualEventDispatcher() {
		return new CustomManualEventDispatcher(eventBus); 
	}

	@Override
	public ManualSignalResolver manualSignalResolver() {
		return new CustomManualSignalResolver(springSignalResolver);
	}

	@Override
	public MessageSender messageSender() {
		return messageSender;
	}

	@Override
	public MemberRepository memberRepository() {
		return memberRepository;
	}

	@Override
	public ConversationRepository conversationRepository() {
		return conversationRepository;
	}

	@Override
	public void inject(NextRTCEndpoint endpoint) {
		endpoint.setServer(server);
	}

	/**
	 * Copy from {@link SpringEventDispatcher}. Here we force it to use our Spring beans.
	 */
	@NextRTCEventListener
	public class CustomManualEventDispatcher extends ManualEventDispatcher {

		public CustomManualEventDispatcher(NextRTCEventBus eventBus) {
			super(eventBus);
		}
		
		protected Collection<Object> getNextRTCEventListeners() {
			Map<String, Object> beans = context.getBeansWithAnnotation(NextRTCEventListener.class);
	        beans.remove(Names.EVENT_DISPATCHER);
	        return beans.values();
		}
		
		@Override
	    protected NextRTCEvents[] getSupportedEvents(Object listener) {
	        try {
	            if (AopUtils.isJdkDynamicProxy(listener)) {
	                listener = ((Advised) listener).getTargetSource().getTarget();
	            }
	        } catch (Exception e) {
	            return new NextRTCEvents[0];
	        }
	        return (NextRTCEvents[]) getValue(listener.getClass().getAnnotation(NextRTCEventListener.class));
	    }
	}
	
	public class CustomManualSignalResolver extends ManualSignalResolver {
		
		private SpringSignalResolver springSignalResolver;
		
	    public CustomManualSignalResolver(SpringSignalResolver springSignalResolver) {
	        super(Collections.emptyMap());
	        this.springSignalResolver = springSignalResolver;
	    }

		@Override
	    public Pair<Signal, SignalHandler> resolve(String string) {
			return springSignalResolver.resolve(string);
		}
		
		@Override
	    public Optional<Pair<Signal, SignalHandler>> addCustomSignal(Signal signal, SignalHandler handler) {
			return springSignalResolver.addCustomSignal(signal, handler);
		}
		
		@Override
		protected void initByDefault() {
			// avoid super class constructor exceptions by suppressing initialization behavior
		}
	}
}
