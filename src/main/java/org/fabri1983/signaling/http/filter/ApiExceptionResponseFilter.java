package org.fabri1983.signaling.http.filter;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletResponse;

import org.fabri1983.signaling.http.exception.ApiException;
import org.fabri1983.signaling.http.response.ApiResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class ApiExceptionResponseFilter implements Filter {

	private final static Logger log = LoggerFactory.getLogger(ApiExceptionResponseFilter.class);
	
	@Override
	public void init(FilterConfig filterConfig) throws ServletException {
	}

	@Override
	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
			throws IOException, ServletException {

		try {
			chain.doFilter(request, response);
		} catch (ApiException ex) {
			log.warn(ex.logInfo());
			buildJsonErrorResponse(response, ex);
		}
	}

	private void buildJsonErrorResponse(ServletResponse response, ApiException apiException)
			throws IOException, JsonProcessingException {
		
		HttpServletResponse res = (HttpServletResponse) response;
		ApiResponse apiResponse = ApiResponse.from(apiException);
		
		res.setStatus(apiResponse.getHttpStatus().value());
		res.setContentType("application/json");
		
		PrintWriter out = res.getWriter();
		ObjectMapper mapper = new ObjectMapper();
		out.print(mapper.writeValueAsString(apiResponse));
		out.flush();
		out.close();
	}

	@Override
	public void destroy() {
	}
    
}
