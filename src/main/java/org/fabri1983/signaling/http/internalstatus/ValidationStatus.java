package org.fabri1983.signaling.http.internalstatus;

public enum ValidationStatus implements InternalStatus {

	MISSING_VIDEO_CHAT_USERID(9001, "Missing video chat user id."),
	MISSING_VIDEO_CHAT_TOKEN(9002, "Missing video chat token."),
	INVALID_VIDEO_CHAT_TOKEN(9003, "Invalid video chat token."),
	USER_ID_DOES_NOT_MATCH_CLAIM(9004, "User id does not match with the one in JWT claim."),
	ROOM_ID_NOT_IN_CLAIM(9005, "Room id not present in JWT claim."),
	ROOM_ID_DOES_NOT_MATCH_CLAIM(9006, "Room id does not match with the one in JWT claim."),
	ROOM_MAX_PARTICIPANTS_REACHED(9007, "Max number of participants reached on requested room.");
	
	private int code;
	private String message;

	private ValidationStatus(int code, String message) {
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
