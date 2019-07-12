package org.fabri1983.signaling.http.exception;

import org.fabri1983.signaling.http.internalstatus.ValidationStatus;
import org.springframework.http.HttpStatus;

public class UnauthorizedException extends ApiException {

	private static final long serialVersionUID = 464250682463078374L;

	private int code;
	
	public UnauthorizedException() {
		// default message and code
		this(HttpStatus.UNAUTHORIZED.getReasonPhrase(), 0);
	}

	public UnauthorizedException(ValidationStatus validatorStatus) {
		this(validatorStatus.getMessage(), validatorStatus.getCode());
	}

	public UnauthorizedException(String msg, int code) {
		super(msg);
		this.code = code;
	}

	@Override
	public HttpStatus getHttpStatus() {
		return HttpStatus.UNAUTHORIZED;
	}

	@Override
	public int getCode() {
		return code;
	}
	
}
