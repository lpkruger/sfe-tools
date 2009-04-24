package sfe.crypto;

import java.security.*;
import java.lang.reflect.*;

/* big giant reflection hack to stuff state into the Sun PRNG */
public class DeterministicRandom {
	public static SecureRandom getRandom(byte[] seed) {
		try {		
			SecureRandom rand = SecureRandom.getInstance("SHA1PRNG");
			Field spi_f = rand.getClass().getDeclaredField("secureRandomSpi");
			spi_f.setAccessible(true);
			SecureRandomSpi spi = (SecureRandomSpi) spi_f.get(rand);
			
			Field state_f = spi.getClass().getDeclaredField("state");
			state_f.setAccessible(true);
			MessageDigest md = MessageDigest.getInstance("SHA");
			byte[] state = md.digest(seed);
			
			state_f.set(spi, state);
			return rand;		
			
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}
	
	public static void main(String[] args) {
		// test
		SecureRandom rand = getRandom(new byte[] { 0 });
		System.out.println(rand.nextInt());
		rand = getRandom(new byte[] { 1 });
		System.out.println(rand.nextInt());
		rand = getRandom(new byte[] { 2 });
		System.out.println(rand.nextInt());
	}
}
