package org.fabri1983.signaling.http.filter;

import java.io.IOException;
import java.util.stream.IntStream;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;

import org.fabri1983.signaling.http.exception.ApiException;

public class SkipApiExceptionFilter implements Filter {

	private String path;
	private Filter filter;
	private int[] codes;
	
	public static FilterExceptionSkipBuilder builder() {
		return new FilterExceptionSkipBuilder();
	}
	
	@Override
	public void init(FilterConfig filterConfig) throws ServletException {
	}

	@Override
	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
			throws IOException, ServletException {
		try {
			filter.doFilter(request, response, chain);
		} catch (ApiException e) {
			if (canBeSkipped(request, e.getCode())) {
				// continue with filter chain since the chain was broken on above filter due to an exception 
				// which at this point we know it must be skipped accordingly the settings of this filter
				chain.doFilter(request, response);
			} else {
				throw e;
			}
		}
	}

	@Override
	public void destroy() {
	}
	
	private boolean canBeSkipped(ServletRequest request, int code) {
		HttpServletRequest req = (HttpServletRequest) request;
		String url = req.getRequestURL().toString();
		String trimmedUrl = path.trim();
		
		if (path == null || "".equals(trimmedUrl)
				|| codes == null || codes.length == 0) {
			return false;
		}
		
		return url.contains(trimmedUrl) && IntStream.of(codes).anyMatch(x -> x == code);
	}
	
	/**
	 * Builder for {@link SkipApiExceptionFilter}. Caution: is not thread safe.
	 */
	public static class FilterExceptionSkipBuilder {

		private SkipApiExceptionFilter filterSkip;
		
		public FilterExceptionSkipBuilder() {
			filterSkip = new SkipApiExceptionFilter();
		}
		
		public FilterExceptionSkipBuilder forPath(String path) {
			filterSkip.path = path;
			return this;
		}

		public FilterExceptionSkipBuilder theseExceptionCodes(int[] codes) {
			filterSkip.codes = codes;
			return this;
		}
		
		public FilterExceptionSkipBuilder filter(Filter filter) {
			filterSkip.filter = filter;
			return this;
		}
		
		public SkipApiExceptionFilter build() {
			return filterSkip;
		}
	}
}
