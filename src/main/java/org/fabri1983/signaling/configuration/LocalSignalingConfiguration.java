package org.fabri1983.signaling.configuration;

import org.nextrtc.signalingserver.Names;
import org.nextrtc.signalingserver.api.NextRTCEventBus;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;

@Configuration(proxyBeanMethods = false)
@Profile( {"eventbus-local"} )
public class LocalSignalingConfiguration {

	/*######################################################################################################
	 * Next are beans needed by instances created on org.nextrtc.signalingserver package.
	 * We manually created them here because we have excluded its configuration class NextRTCConfig.class.
	 *######################################################################################################*/
	
	@Bean(name = Names.EVENT_BUS)
	@Primary
	public NextRTCEventBus eventBus() {
		return new NextRTCEventBus();
	}
    
}
