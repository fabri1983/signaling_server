package org.fabri1983.signaling.core;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.Claim;
import com.auth0.jwt.interfaces.DecodedJWT;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.InvalidKeySpecException;
import java.util.Optional;

import org.fabri1983.signaling.util.SignalingResourceLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

public class JwtVerifier implements IJwtVerifier {

	private static final Logger log = LoggerFactory.getLogger(JwtVerifier.class);
	
	private static final String FROM_UID = "fromUid";
	private static final String TO_UID = "toUid";
	private static final String ROOM_ID = "roomId";
	
	private String audience;
	private String issuer;
	private Algorithm algorithm;
	
	public JwtVerifier(String audience, String issuer, String publicKeyFileName, String privateKeyFileName) {
		this.audience = audience;
		this.issuer = issuer;
		try {
			initAlgorithm(publicKeyFileName, privateKeyFileName);
		} catch (NoSuchAlgorithmException | InvalidKeySpecException | IOException e) {
			log.error("Init Algorithm for JWT failed. Reason: {}", e.getMessage());
			throw new RuntimeException(e);
		}
	}
	
	private void initAlgorithm(String publicKeyFileName, String privateKeyFileName)
			throws NoSuchAlgorithmException, InvalidKeySpecException, IOException {

		// Define the location of the file in order of prevalence from low to high
		Resource[] resourceLocationPublicKey = new Resource[] {
				new ClassPathResource("/" + publicKeyFileName)
				//new UrlResource("file:" + "/conf/" + publicKeyFileName)
				};

		// Define the location of the file in order of prevalence from low to high
		Resource[] resourceLocationPrivateKey = new Resource[] {
				new ClassPathResource("/" + privateKeyFileName)
				//new UrlResource("file:" + "/conf/" + privateKeyFileName)
				};

		// get the keys
		RSAPublicKey pubKey = SignalingResourceLoader.getRSAPublicKeyEncoded(resourceLocationPublicKey);
		RSAPrivateKey privKey = SignalingResourceLoader.getRSAPrivateKeyEncoded(resourceLocationPrivateKey);

		algorithm = Algorithm.RSA256(pubKey, privKey);
	}
	
	@Override
	public boolean isTokenValid(String token) {
		Optional<DecodedJWT> jwtOpt = getVerifiedAndDecoded(token);
		return jwtOpt.isPresent() ;
	}

	@Override
	public boolean matchUserIdWithClaim(String token, String userId) {
		Optional<DecodedJWT> jwtOpt = getVerifiedAndDecoded(token);
		if (jwtOpt.isPresent()) {
			DecodedJWT jwt = jwtOpt.get();
		    Claim fromUidClaim = jwt.getClaim(FROM_UID);
		    Claim toUidClaim = jwt.getClaim(TO_UID);
		    if (!fromUidClaim.isNull() && !toUidClaim.isNull()) {
		    	return fromUidClaim.asString().equals(userId) || toUidClaim.asString().equals(userId);
		    }
		}
		return false;
	}
	
	@Override
	public boolean isRoomIdInClaim(String token) {
		Optional<DecodedJWT> jwtOpt = getVerifiedAndDecoded(token);
		if (jwtOpt.isPresent()) {
			DecodedJWT jwt = jwtOpt.get();
			Claim roomIdClaim = jwt.getClaim(ROOM_ID);
			if (!roomIdClaim.isNull()) {
				String roomId = roomIdClaim.asString();
		    	return !roomId.trim().isEmpty();
		    }
		}
		return false;
	}

	@Override
	public boolean matchRoomIdWithClaim(String token, String roomId) {
		Optional<DecodedJWT> jwtOpt = getVerifiedAndDecoded(token);
		if (jwtOpt.isPresent()) {
			DecodedJWT jwt = jwtOpt.get();
			Claim roomIdClaim = jwt.getClaim(ROOM_ID);
			if (!roomIdClaim.isNull()) {
		    	return roomIdClaim.asString().equals(roomId);
		    }
		}
		return false;
	}
	
	@Override
	public String getRoomIdFrom(String token) {
		Optional<DecodedJWT> jwtOpt = getVerifiedAndDecoded(token);
		if (jwtOpt.isPresent()) {
			DecodedJWT jwt = jwtOpt.get();
			Claim roomIdClaim = jwt.getClaim(ROOM_ID);
			if (!roomIdClaim.isNull()) {
		    	return roomIdClaim.asString();
		    }
		}
		return "";
	}
	
	@Override
	public String getUserToFrom(String token) {
		Optional<DecodedJWT> jwtOpt = getVerifiedAndDecoded(token);
		if (jwtOpt.isPresent()) {
			DecodedJWT jwt = jwtOpt.get();
			Claim roomIdClaim = jwt.getClaim(TO_UID);
			if (!roomIdClaim.isNull()) {
		    	return roomIdClaim.asString();
		    }
		}
		return "";
	}
	
	private Optional<DecodedJWT> getVerifiedAndDecoded(String token) {
		try {
			JWTVerifier verifier = JWT
					.require(algorithm)
					.withAudience(audience)
					.withIssuer(issuer)
					.build();
			return Optional.ofNullable(verifier.verify(token));
		} catch (JWTVerificationException exception){
			log.warn("Invalid signature when verifying jwt token. Reason: {}", exception.getMessage());
		}
		return Optional.empty();
	}
	
}
