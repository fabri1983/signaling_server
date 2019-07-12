package org.fabri1983.signaling.http.validator;

import org.fabri1983.signaling.core.IJwtVerifier;
import org.fabri1983.signaling.http.exception.UnauthorizedException;
import org.fabri1983.signaling.http.internalstatus.ValidationStatus;
import org.fabri1983.signaling.util.UserIdMDCLogger;

public class VideoChatTokenValidator {

	private IJwtVerifier jwtVerifier;
	
	private int[] validationCodes = new int[] { 
			ValidationStatus.MISSING_VIDEO_CHAT_TOKEN.getCode(),
			ValidationStatus.INVALID_VIDEO_CHAT_TOKEN.getCode(),
			ValidationStatus.USER_ID_DOES_NOT_MATCH_CLAIM.getCode(),
			ValidationStatus.ROOM_ID_NOT_IN_CLAIM.getCode()
	};
	
	public VideoChatTokenValidator(IJwtVerifier jwtVerifier) {
		this.jwtVerifier = jwtVerifier;
	}

	public void validate(String token, String userId) {
		UserIdMDCLogger.doWithLog(userId, validateLambda(token, userId) );
		
	}

	private Runnable validateLambda(String token, String userId) {
		return () -> {
			// no token?
			if (isNullOrEmpty(token)) {
				throw new UnauthorizedException(ValidationStatus.MISSING_VIDEO_CHAT_TOKEN);
			}
			// invalid token?
			if (!jwtVerifier.isTokenValid(token)) {
				throw new UnauthorizedException(ValidationStatus.INVALID_VIDEO_CHAT_TOKEN);
			}
			// userId is in any claim?
			if (!jwtVerifier.matchUserIdWithClaim(token, userId)) {
				throw new UnauthorizedException(ValidationStatus.USER_ID_DOES_NOT_MATCH_CLAIM);
			}
			// has roomId in claim?
			if (!jwtVerifier.isRoomIdInClaim(token)) {
				throw new UnauthorizedException(ValidationStatus.ROOM_ID_NOT_IN_CLAIM);
			}
		};
	}

	private boolean isNullOrEmpty(String value){
		return value == null || value.isEmpty();
	}
	
	public int[] getValidationCodes() {
		return validationCodes;
	}
	
}
