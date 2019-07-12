package org.fabri1983.signaling.http.internalstatus;

public enum ApiStatus implements InternalStatus {

	WEBSOCKET_CLOSED_PONG_TIMEOUT(8001, "Timeout (pong not received).");
	
	private int code;
	private String message;

	private ApiStatus(int code, String message) {
		this.code = code;
		this.message = message;
	}

	@Override
	public int getCode() {
		return this.code;
	}

	@Override
	public String getMessage() {
		return this.message;
	}
	
}
