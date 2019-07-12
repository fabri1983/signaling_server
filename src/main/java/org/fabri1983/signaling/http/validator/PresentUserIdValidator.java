package org.fabri1983.signaling.http.validator;

import org.fabri1983.signaling.http.exception.ValidationException;
import org.fabri1983.signaling.http.internalstatus.ValidationStatus;

public class PresentUserIdValidator {

	private int[] validationCodes = new int[] { 
			ValidationStatus.MISSING_VIDEO_CHAT_USERID.getCode()
	};
	
	public void validate(String uid) {
		if (isNullOrEmpty(uid)) {
			throw new ValidationException(ValidationStatus.MISSING_VIDEO_CHAT_USERID);
		}
	}

	private boolean isNullOrEmpty(String value){
		return value == null || value.isEmpty();
	}
	
	public int[] getValidationCodes() {
		return validationCodes;
	}
	
}
