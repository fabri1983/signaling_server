package org.fabri1983.signaling.configuration;

import java.util.concurrent.ScheduledExecutorService;

import org.nextrtc.signalingserver.Names;
import org.nextrtc.signalingserver.property.NextRTCProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.core.io.ClassPathResource;
import org.springframework.scheduling.concurrent.ScheduledExecutorFactoryBean;

/**
 * Beans needed by instances created on org.nextrtc.signalingserver package. 
 * We have excluded its configuration class: NextRTCConfig.class.
 */
@Configuration(proxyBeanMethods = false)
@ComponentScan(
		basePackages = {
				"org.nextrtc.signalingserver.cases",    "org.nextrtc.signalingserver.domain", 
				"org.nextrtc.signalingserver.eventbus", "org.nextrtc.signalingserver.factory",
				"org.nextrtc.signalingserver.modules",  "org.nextrtc.signalingserver.property",
				"org.nextrtc.signalingserver.repository" }
)
public class SpringNextRTCConfiguration {

	@Bean
	public SpringNextRTCComponent nextRTCComponent() {
		return new SpringNextRTCComponent();
	}
	
	/**
	 * This forces any properties bean initialization to use a custom location. Useful to override properties inside NextRTC jar dependency.
	 * @return
	 */
	@Bean
	public PropertySourcesPlaceholderConfigurer propertySourcesPlaceholderConfigurer() {
		PropertySourcesPlaceholderConfigurer propertyPlaceholderConfigurer = new PropertySourcesPlaceholderConfigurer();
		propertyPlaceholderConfigurer.setLocation(new ClassPathResource("nextrtc.properties"));
		return propertyPlaceholderConfigurer;
	}

	/**
	 * Ping Scheduler.
	 */
	@Bean(name = Names.SCHEDULER_NAME)
	@Primary
	public ScheduledExecutorService scheduler(NextRTCProperties nextRTCProperties) {
		ScheduledExecutorFactoryBean factoryBean = new ScheduledExecutorFactoryBean();
		factoryBean.setThreadNamePrefix(Names.SCHEDULER_NAME);
		factoryBean.setPoolSize(nextRTCProperties.getSchedulerPoolSize());
		factoryBean.afterPropertiesSet();
		return factoryBean.getObject();
	}

}
