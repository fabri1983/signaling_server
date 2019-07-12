package org.fabri1983.signaling.core;

public class DummyJwtVerifier implements IJwtVerifier {
	
	@Override
	public boolean isTokenValid(String token) {
		return true;
	}

	@Override
	public boolean matchUserIdWithClaim(String token, String userId) {
		return true;
	}
	
	@Override
	public boolean isRoomIdInClaim(String token) {
		return true;
	}

	@Override
	public boolean matchRoomIdWithClaim(String token, String roomId) {
		return true;
	}

	@Override
	public String getRoomIdFrom(String token) {
		return "";
	}

	@Override
	public String getUserToFrom(String token) {
		return "";
	}
	
}
