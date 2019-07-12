package org.fabri1983.signaling.configuration;

import org.fabri1983.signaling.core.NextRTCDistributedEventBus;
import org.fabri1983.signaling.util.EventContextCustomSerializer;
import org.nextrtc.signalingserver.Names;
import org.nextrtc.signalingserver.api.NextRTCEventBus;
import org.nextrtc.signalingserver.api.dto.NextRTCEvent;
import org.nextrtc.signalingserver.domain.EventContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;

import com.hazelcast.config.Config;
import com.hazelcast.config.ReliableTopicConfig;
import com.hazelcast.config.SerializerConfig;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.ITopic;

@Configuration
@Profile( {"eventbus-dist"} )
public class DistributedSignalingConfiguration {

	/**
	 * Override bean definition from @{link org.nextrtc.signalingserver.api.NextRTCEventBus} so we can customize the event bus.
	 * @return
	 */
    @Bean(name = Names.EVENT_BUS)
    @Primary
    public NextRTCEventBus eventBus(ITopic<NextRTCEvent> hzcTopic) {
        return new NextRTCDistributedEventBus(hzcTopic);
    }
    
    @Bean
    public ReliableTopicConfig hazelCastTopicConfig() {
    	ReliableTopicConfig topicConfig = new ReliableTopicConfig("signaling-topic");
    	return topicConfig;
    }
    
    @Bean
    public HazelcastInstance hazelCastInstance(ReliableTopicConfig hzcTopicConfig) {
    	Config config = new Config("signaling-hzc");
    	
    	config.addReliableTopicConfig(hzcTopicConfig);
    	
    	SerializerConfig sc = new SerializerConfig()
    		    .setImplementation(new EventContextCustomSerializer())
    		    .setTypeClass(EventContext.class);
    	config.getSerializationConfig().addSerializerConfig(sc);
    	
        HazelcastInstance hzcInstance = Hazelcast.getOrCreateHazelcastInstance(config);
        return hzcInstance;
    }
    
    @Bean
    public ITopic<NextRTCEvent> hazelCastTopic(HazelcastInstance hzcInstance) {
    	return hzcInstance.getReliableTopic("signaling-topic");
    }
    
}
