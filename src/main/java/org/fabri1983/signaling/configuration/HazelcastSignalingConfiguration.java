package org.fabri1983.signaling.configuration;

import com.hazelcast.config.Config;
import com.hazelcast.config.ReliableTopicConfig;
import com.hazelcast.config.SerializerConfig;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.ITopic;

import javax.annotation.PostConstruct;

import org.fabri1983.signaling.core.distributed.NextRTCDistributedEventBus;
import org.fabri1983.signaling.core.distributed.serialization.NextRTCEventWrapper;
import org.fabri1983.signaling.core.distributed.serialization.NextRTCEventWrapperSerializerV1;
import org.fabri1983.signaling.core.population.ConversationPopulation;
import org.nextrtc.signalingserver.Names;
import org.nextrtc.signalingserver.api.NextRTCEventBus;
import org.nextrtc.signalingserver.repository.ConversationRepository;
import org.nextrtc.signalingserver.repository.MemberRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;

@Configuration
@Profile( {"eventbus-hazelcast"} )
public class HazelcastSignalingConfiguration {

	private final String TOPIC_CONFIG_NAME = "signaling-hzc";
	private final String RELIABLE_TOPIC_CONFIG_NAME = "signaling-topic";
	private final boolean registerCustomSerializers = false;
	
    @Bean
    public ReliableTopicConfig hazelCastTopicConfig() {
    	ReliableTopicConfig topicConfig = new ReliableTopicConfig(RELIABLE_TOPIC_CONFIG_NAME);
    	return topicConfig;
    }

    @Bean
    public HazelcastInstance hazelCastInstance(ReliableTopicConfig hzcTopicConfig) {
    	Config config = new Config(TOPIC_CONFIG_NAME);
    	config.addReliableTopicConfig(hzcTopicConfig);
    	registerSerializers(config);
        HazelcastInstance hzcInstance = Hazelcast.getOrCreateHazelcastInstance(config);
        return hzcInstance;
    }

	@Bean
    public ITopic<NextRTCEventWrapper> hazelCastTopic(HazelcastInstance hzcInstance) {
    	return hzcInstance.getReliableTopic(RELIABLE_TOPIC_CONFIG_NAME);
    }

	/**
	 * Override bean definition from @{link org.nextrtc.signalingserver.api.NextRTCEventBus} so we can customize the event bus.
	 * @return
	 */
    @Bean(name = Names.EVENT_BUS)
    @Primary
    public NextRTCEventBus eventBus(ITopic<NextRTCEventWrapper> hzcTopic, ConversationPopulation population, 
    		ConversationRepository conversationRepository, MemberRepository members) {
		return new NextRTCDistributedEventBus(hzcTopic, population, conversationRepository, members);
	}

	private void registerSerializers(Config config) {
		if (!registerCustomSerializers) {
			return;
		}
		
		SerializerConfig eventContextSc = new SerializerConfig()
			    .setImplementation(new NextRTCEventWrapperSerializerV1())
			    .setTypeClass(NextRTCEventWrapper.class);
		config.getSerializationConfig().addSerializerConfig(eventContextSc);
		
		// TODO here we can add more custom serializers for the many different types defined in NextRTCEventWrapper
	}

	/**
	 * This class serves the solely purpose of resolve circular bean reference creation, which happens 
	 * when two or more beans depend on each other.
	 */
    @Configuration
    public static class CircularBeanRefResolver {
    
    	// Inject beans here
    	
    	@PostConstruct
	    public void circularBeanRefResolver() {
	    	// set missing dependencies
	    }
    }
}
