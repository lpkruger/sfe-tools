package sfe.crypto;

import java.security.GeneralSecurityException;
import java.security.SecureRandom;

import javax.crypto.SecretKey;

public class SFEKeyGenerator {
	public static SFEKeyGenerator getInstance(String cipher, SecureRandom rand) {
		return new SFEKeyGenerator(rand);
	}
	
	private SFEKeyGenerator(SecureRandom rand) {
		this.random = rand;
		
	}
	SecureRandom random;
	
	int length = 80;
	
	public void init(int length) {
		this.length = length;
	}
	
	public int getLength() {
		return length;
	}
	
	public SecretKey generateKey() {
		byte[] bytes = new byte[length/8];
		random.nextBytes(bytes);
		return new SFEKey(bytes);
	}
}
