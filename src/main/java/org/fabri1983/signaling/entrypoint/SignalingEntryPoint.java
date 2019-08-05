package org.fabri1983.signaling.entrypoint;

import org.fabri1983.signaling.configuration.SignalingConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@SpringBootApplication
@Configuration
// Might happen that a missing web.xml of Servlet Container v3.1+ causes sometimes Spring doesn't scan classes, 
// so here we explicit declare the import of configuration class.
@Import( value = { SignalingConfiguration.class })
public class SignalingEntryPoint extends SpringBootServletInitializer {

	@Override
    protected SpringApplicationBuilder configure(SpringApplicationBuilder builder) {
		return super.configure(builder
				.sources(SignalingEntryPoint.class)
				.banner(new SignalingServerBanner())
		);
    }
	
	public static void main(String[] args) throws Exception {
		new SpringApplicationBuilder()
			.sources(SignalingEntryPoint.class)
			.banner(new SignalingServerBanner())
			.run(args);
	}

}
