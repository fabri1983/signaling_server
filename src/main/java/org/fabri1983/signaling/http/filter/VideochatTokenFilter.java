package org.fabri1983.signaling.http.filter;

import java.io.IOException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;

import org.fabri1983.signaling.http.SignalingHttpHeaderConstants;
import org.fabri1983.signaling.http.validator.VideoChatTokenValidator;

public class VideochatTokenFilter implements Filter {

	private VideoChatTokenValidator videoChatTokenValidator;
	
	public VideochatTokenFilter(VideoChatTokenValidator videoChatTokenValidator) {
		this.videoChatTokenValidator = videoChatTokenValidator;
	}

	@Override
	public void init(FilterConfig filterConfig) throws ServletException {
	}

	@Override
	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
			throws IOException, ServletException {
		validateToken(request);
		chain.doFilter(request, response);
	}

	private void validateToken(ServletRequest request) {
		HttpServletRequest req = (HttpServletRequest) request;
		String videoChatToken = req.getHeader(SignalingHttpHeaderConstants.HEADER_VIDEOCHAT_TOKEN);
		String uid = req.getHeader(SignalingHttpHeaderConstants.HEADER_VIDEOCHAT_USER);
		videoChatTokenValidator.validate(videoChatToken, uid);
	}

	@Override
	public void destroy() {
	}
    
}
