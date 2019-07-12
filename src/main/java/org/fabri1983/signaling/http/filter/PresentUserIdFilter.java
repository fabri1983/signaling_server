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
import org.fabri1983.signaling.http.validator.PresentUserIdValidator;

public class PresentUserIdFilter implements Filter {

	private PresentUserIdValidator presentUserIdValidator;
	
	public PresentUserIdFilter(PresentUserIdValidator presentUserIdValidator) {
		this.presentUserIdValidator = presentUserIdValidator;
	}

	@Override
	public void init(FilterConfig filterConfig) throws ServletException {
	}

	@Override
	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
			throws IOException, ServletException {
		validateUserIdIsPresent(request);
		chain.doFilter(request, response);
	}

	private void validateUserIdIsPresent(ServletRequest request) {
		HttpServletRequest req = (HttpServletRequest) request;
		String uid = req.getHeader(SignalingHttpHeaderConstants.HEADER_VIDEOCHAT_USER);
		presentUserIdValidator.validate(uid);
	}

	@Override
	public void destroy() {
	}
	
}
