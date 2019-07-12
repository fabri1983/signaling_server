package org.fabri1983.signaling.http.response;

import org.fabri1983.signaling.http.exception.ApiException;
import org.springframework.http.HttpStatus;

import com.fasterxml.jackson.annotation.JsonIgnore;

public class ApiResponse {

	@JsonIgnore
	private HttpStatus httpStatus;
	private int code;
	private String message;
	
	public ApiResponse(HttpStatus httpStatus, String message, int code) {
		this.httpStatus = httpStatus;
		this.message = message;
		this.code = code;
	}

	public static ApiResponse from(ApiException apiException){
		return new ApiResponse(apiException.getHttpStatus(), apiException.getMessage(), apiException.getCode());
	}
	
	public HttpStatus getHttpStatus() {
		return httpStatus;
	}

	public int getCode() {
		return code;
	}

	public String getMessage() {
		return message;
	}

}
