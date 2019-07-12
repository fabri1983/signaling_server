package org.fabri1983.signaling.core;

public interface IJwtVerifier {

	boolean isTokenValid(String token);

	boolean matchUserIdWithClaim(String token, String userId);

	boolean isRoomIdInClaim(String token);

	boolean matchRoomIdWithClaim(String token, String roomId);

	String getRoomIdFrom(String token);

	String getUserToFrom(String token);
	
}
