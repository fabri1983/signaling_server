package org.fabri1983.signaling.http.exception;

import org.fabri1983.signaling.http.internalstatus.ValidationStatus;
import org.springframework.http.HttpStatus;

public class ValidationException extends ApiException {

	private static final long serialVersionUID = 464250682463078374L;

	private int code;
	
	public ValidationException() {
		// default message and code
		this(HttpStatus.PRECONDITION_FAILED.getReasonPhrase(), 0);
	}

	public ValidationException(ValidationStatus validatorStatus) {
		this(validatorStatus.getMessage(), validatorStatus.getCode());
	}

	public ValidationException(String msg, int code) {
		super(msg);
		this.code = code;
	}

	@Override
	public HttpStatus getHttpStatus() {
		return HttpStatus.PRECONDITION_FAILED;
	}

	@Override
	public int getCode() {
		return code;
	}
	
}
