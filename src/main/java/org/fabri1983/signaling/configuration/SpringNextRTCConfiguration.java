package org.fabri1983.signaling.configuration;

import java.util.Map;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;

import javax.websocket.Session;

import org.nextrtc.signalingserver.Names;
import org.nextrtc.signalingserver.NextRTCComponent;
import org.nextrtc.signalingserver.api.NextRTCEventBus;
import org.nextrtc.signalingserver.cases.RegisterMember;
import org.nextrtc.signalingserver.cases.SignalHandler;
import org.nextrtc.signalingserver.domain.DefaultMessageSender;
import org.nextrtc.signalingserver.domain.Member;
import org.nextrtc.signalingserver.domain.MessageSender;
import org.nextrtc.signalingserver.domain.Server;
import org.nextrtc.signalingserver.domain.SignalResolver;
import org.nextrtc.signalingserver.domain.resolver.SpringSignalResolver;
import org.nextrtc.signalingserver.factory.MemberFactory;
import org.nextrtc.signalingserver.factory.SpringMemberFactory;
import org.nextrtc.signalingserver.property.NextRTCProperties;
import org.nextrtc.signalingserver.property.SpringNextRTCProperties;
import org.nextrtc.signalingserver.repository.ConversationRepository;
import org.nextrtc.signalingserver.repository.Conversations;
import org.nextrtc.signalingserver.repository.MemberRepository;
import org.nextrtc.signalingserver.repository.Members;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.core.io.ClassPathResource;
import org.springframework.scheduling.concurrent.ScheduledExecutorFactoryBean;

/**
 * Beans needed by instances created on org.nextrtc.signalingserver package. 
 * We manually created them here because we have excluded its configuration class: NextRTCConfig.class.
 */
@Configuration(proxyBeanMethods = false)
public class SpringNextRTCConfiguration {

	@Bean
	public PropertySourcesPlaceholderConfigurer propertySourcesPlaceholderConfigurer() {
		PropertySourcesPlaceholderConfigurer propertyPlaceholderConfigurer = new PropertySourcesPlaceholderConfigurer();
		propertyPlaceholderConfigurer.setLocation(new ClassPathResource("nextrtc.properties"));
		return propertyPlaceholderConfigurer;
	}

	@Bean
	public NextRTCProperties nextRTCProperties() {
		return new SpringNextRTCProperties();
	}

	@Bean
	public SignalResolver springSignalResolver(Map<String, SignalHandler> handlers) {
		return new SpringSignalResolver(handlers);
	}
	
	@Bean
	public MessageSender messageSender(MemberRepository members) {
		return new DefaultMessageSender(members);
	}
	
	@Bean
	public MemberRepository members() {
		return new Members();
	}
	
	@Bean
	public MemberFactory memberFactory() {
		return new SpringMemberFactory();
	}
	
	@Bean
	public ConversationRepository conversationRepository() {
		return new Conversations();
	}
	
	/**
	 * Ping Scheduler.
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
	public RegisterMember register(NextRTCEventBus eventBus, NextRTCProperties properties, MemberRepository members,
			ScheduledExecutorService scheduler, MemberFactory factory, MessageSender sender) {
		return new RegisterMember(eventBus, properties, members, scheduler, factory, sender);
	}
	
	@Bean
	public Server server(NextRTCEventBus eventBus, MemberRepository members, SignalResolver resolver,
			RegisterMember register, MessageSender sender) {
		return new Server(eventBus, members, resolver, register, sender);
	}
	
	@Bean
	@Scope("prototype")
	public Member member(Session session, ScheduledFuture<?> ping) {
		return new Member(session, ping);
	}
	
	@Bean
	public NextRTCComponent nextRTCComponent() {
		return new SpringNextRTCComponent();
	}

}
