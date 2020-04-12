package org.fabri1983.signaling.configuration;

import com.hazelcast.config.Config;
import com.hazelcast.config.ReliableTopicConfig;
import com.hazelcast.config.SerializerConfig;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.ITopic;

import org.fabri1983.signaling.core.distributed.NextRTCDistributedEventBus;
import org.fabri1983.signaling.core.distributed.serialization.NextRTCConversationWrapperSerializerV1;
import org.fabri1983.signaling.core.distributed.serialization.NextRTCEventWrapperSerializerV1;
import org.fabri1983.signaling.core.distributed.serialization.NextRTCMemberWrapperSerializerV1;
import org.fabri1983.signaling.core.distributed.wrapper.NextRTCConversationWrapper;
import org.fabri1983.signaling.core.distributed.wrapper.NextRTCEventWrapper;
import org.fabri1983.signaling.core.distributed.wrapper.NextRTCMemberWrapper;
import org.fabri1983.signaling.core.population.ConversationPopulation;
import org.nextrtc.signalingserver.Names;
import org.nextrtc.signalingserver.api.NextRTCEventBus;
import org.nextrtc.signalingserver.repository.ConversationRepository;
import org.nextrtc.signalingserver.repository.MemberRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;

@Configuration(proxyBeanMethods = false)
@Profile( {"eventbus-hazelcast"} )
public class HazelcastSignalingConfiguration {

	private final String HZ_INSTANCE_NAME = "signaling-hzc";
	private final String TOPIC_CONFIG_NAME = "signaling-hzc-topic";
	
    @Bean
    public ReliableTopicConfig hazelCastTopicConfig() {
    	// In the reliable topic, global order is always maintained
    	ReliableTopicConfig topicConfig = new ReliableTopicConfig(TOPIC_CONFIG_NAME);
    	return topicConfig;
    }

    @Bean(destroyMethod = "shutdown")
    public HazelcastInstance hazelCastInstance(ReliableTopicConfig hzcTopicConfig) {
    	Config config = new Config(HZ_INSTANCE_NAME);
    	config.addReliableTopicConfig(hzcTopicConfig);
    	config.getNetworkConfig().getJoin().getMulticastConfig().setEnabled(true);
    	registerSerializers(config);
        HazelcastInstance hzcInstance = Hazelcast.getOrCreateHazelcastInstance(config);
        return hzcInstance;
    }

	@Bean
    public ITopic<NextRTCEventWrapper> hazelCastTopic(HazelcastInstance hzcInstance) {
    	return hzcInstance.getReliableTopic(TOPIC_CONFIG_NAME);
    }

	/**
	 * Override bean definition from @{link org.nextrtc.signalingserver.api.NextRTCEventBus} so we can customize the event bus.
	 * @return
	 */
    @Bean(name = Names.EVENT_BUS)
    @Primary
    public NextRTCEventBus eventBus(HazelcastInstance hazelcastInstance, ITopic<NextRTCEventWrapper> hzcTopic, 
    		ConversationPopulation population, ConversationRepository conversationRepository, 
    		MemberRepository members) {
		return new NextRTCDistributedEventBus(hazelcastInstance, hzcTopic, population, 
				conversationRepository, members);
	}

	private void registerSerializers(Config config) {
		SerializerConfig eventSc = new SerializerConfig()
			    .setImplementation(new NextRTCEventWrapperSerializerV1())
			    .setTypeClass(NextRTCEventWrapper.class);
		
		SerializerConfig memberSc = new SerializerConfig()
			    .setImplementation(new NextRTCMemberWrapperSerializerV1())
			    .setTypeClass(NextRTCMemberWrapper.class);
		
		SerializerConfig conversationSc = new SerializerConfig()
			    .setImplementation(new NextRTCConversationWrapperSerializerV1())
			    .setTypeClass(NextRTCConversationWrapper.class);
		
		config.getSerializationConfig().addSerializerConfig(eventSc);
		config.getSerializationConfig().addSerializerConfig(memberSc);
		config.getSerializationConfig().addSerializerConfig(conversationSc);
	}

	/**
	 * This class serves the solely purpose of resolve circular bean reference creation, which happens 
	 * when two or more beans depend on each other.
	 */
//    @Configuration
//    public static class CircularBeanRefResolver {
//    
//    	// Inject beans here
//    	
//    	@PostConstruct
//	    public void circularBeanRefResolver() {
//	    	// set missing dependencies
//	    }
//    }
}
