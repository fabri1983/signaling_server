package org.fabri1983.signaling.util;

import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.EncodedResource;

public class SignalingResourceLoader {

	private final static Logger log = LoggerFactory.getLogger(SignalingResourceLoader.class);
	
	/**
	 * Create RSA Key from a Public X509 Encoded key.
	 * 
	 * @param locations
	 * @return
	 * @throws IOException
	 * @throws NoSuchAlgorithmException
	 * @throws InvalidKeySpecException
	 */
	public static RSAPublicKey getRSAPublicKeyEncoded(Resource... locations) throws IOException, NoSuchAlgorithmException, InvalidKeySpecException {
		InputStream isPublicKey = null;
		try {
			isPublicKey = getInputStreamFromEncodedResource(true, locations);
			final byte[] keyBytes = readInputStreamToBytes(isPublicKey);
			X509EncodedKeySpec spec = new X509EncodedKeySpec(keyBytes);
			KeyFactory kf = KeyFactory.getInstance("RSA");
			return (RSAPublicKey) kf.generatePublic(spec);
		}
		finally {
			if (isPublicKey != null) {
				isPublicKey.close();
			}
		}
	}
	
	/**
	 * Create RSA Key from a Private PKCS8 Encoded key.
	 * 
	 * @param locations
	 * @return
	 * @throws IOException
	 * @throws NoSuchAlgorithmException
	 * @throws InvalidKeySpecException
	 */
	public static RSAPrivateKey getRSAPrivateKeyEncoded(Resource... locations) throws IOException, NoSuchAlgorithmException, InvalidKeySpecException {
		InputStream isPrivateKey = null;
		try {
			isPrivateKey = getInputStreamFromEncodedResource(true, locations);
			final byte[] keyBytes = readInputStreamToBytes(isPrivateKey);
			PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(keyBytes);
			KeyFactory kf = KeyFactory.getInstance("RSA");
			return (RSAPrivateKey) kf.generatePrivate(spec);
		}
		finally {
			if (isPrivateKey != null) {
				isPrivateKey.close();
			}
		}
	}
	
	/**
     * Return {@link InputStream} of a resource given a list of possible locations.
     * Last location in the array has higher prevalence.
     * 
     * @param ignoreResourceNotFound
     * @param locations
     * @return
     * @throws IOException
     */
    public static InputStream getInputStreamFromResource(boolean ignoreResourceNotFound, Resource... locations) throws IOException {
    	
    	List<Resource> reversed = Arrays.asList(locations);
    	// invert the array so we tried to get the most prevalence location
    	Collections.reverse(reversed);
    	
    	for (Resource location : reversed) {
			log.info("Loading input stream from resource {}", location);
			try {
				return location.getInputStream();
			} catch (FileNotFoundException ex) {
				if (!ignoreResourceNotFound) {
					throw ex;
				}
			}
		}
    	throw new FileNotFoundException("No location were found.");
    }
    
	/**
     * Return {@link InputStream} of a binary resource given a list of possible locations.
     * Last location in the array has higher prevalence.
     * 
     * @param ignoreResourceNotFound
     * @param locations
     * @return
     * @throws IOException
     */
    public static InputStream getInputStreamFromEncodedResource(boolean ignoreResourceNotFound, Resource... locations) throws IOException {
    	
    	List<Resource> reversed = Arrays.asList(locations);
    	// invert the array so we tried to get the most prevalence location
    	Collections.reverse(reversed);
    	
    	for (Resource location : reversed) {
			log.info("Loading input stream from resource {}", location);
			try {
				return getInputStreamFromEncoder(new EncodedResource(location));
			} catch (FileNotFoundException ex) {
				if (!ignoreResourceNotFound) {
					throw ex;
				}
			}
		}
    	throw new FileNotFoundException("No location were found.");
    }
    
	/**
     * Reads binary data from an input stream and returns it as a byte array.
     *
     * @param is
     * input stream from which data is read.
     *
     * @return
     * byte array containing data read from the input stream.
     *
     * @throws IOException
     */
    public static byte[] readInputStreamToBytes(InputStream is) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        int b = -1;
        while ((b = is.read()) != -1) {
            baos.write(b);
        }
        return baos.toByteArray();
    }
    
    private static InputStream getInputStreamFromEncoder(EncodedResource resource) throws IOException {
		return resource.getInputStream();
	}
    
}
