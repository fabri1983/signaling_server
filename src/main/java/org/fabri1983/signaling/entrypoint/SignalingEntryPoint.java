package org.fabri1983.signaling.entrypoint;

import org.fabri1983.signaling.configuration.SignalingConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@SpringBootApplication
@Configuration
// without web.xml servlet container v3.1+ sometimes Spring doesn't scan classes, so we force it.
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
