package org.fabri1983.signaling.http.exception;

import org.springframework.http.HttpStatus;

public abstract class ApiException extends RuntimeException {

	private static final long serialVersionUID = -3904286208554656500L;

	public ApiException(String message) {
		super(message);
	}

	public abstract HttpStatus getHttpStatus();
	
	public abstract int getCode();

	public String logInfo() {
		return "HttpStatus " + getHttpStatus() + ". Code: " + getCode() + ". Message: " + getMessage();
	}
	
}
