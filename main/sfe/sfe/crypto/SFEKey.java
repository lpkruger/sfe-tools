package sfe.crypto;

import java.math.BigInteger;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

public class SFEKey implements SecretKey {
	private static final long serialVersionUID = 1;
	
	byte[] bytes;
	
	public SFEKey(byte[] bytes) {
		this.bytes = bytes;
	}
	public String getFormat() {
		return "RAW";
	}
	public byte[] getEncoded() {
		return bytes;
	}
	public String getAlgorithm() {
		return "SFE";
	}
	
	public boolean equals(Object other) {
		if (!(other instanceof SecretKey))
			return false;
		byte[] obytes = ((SecretKey) other).getEncoded();
		if (obytes.length != bytes.length)
			return false;
		for (int i=0; i<bytes.length; ++i) {
			if (obytes[i] != bytes[i])
				return false;
		}
		return true;
	}
	
	public int hashCode() {
		int sum=0;
		for (int i=0; i<bytes.length; ++i)
			sum = (sum*13) + bytes[i];
		return sum;
	}
	
	// utility functions
	// helper conversion function
	public static byte[] keyToBytes(SecretKey k) {
		return k.getEncoded();
	}
	// helper conversion function	
	public static SecretKey bytesToKey(byte[] k, String CIPHER) {
		return new SecretKeySpec(k, CIPHER);
	}
	// xor keys represented as byte arrays
	public static SecretKey xor(SecretKey a, SecretKey b, String CIPHER) {
		return new SecretKeySpec(xor(a.getEncoded(),
				b.getEncoded()), CIPHER);
	}
	// xor byte arrays
	public static byte[] xor(byte[] a, byte[] b) {
		if (a.length != b.length)
			throw new IllegalArgumentException(a.length + " != " + b.length);
		byte[] c = new byte[a.length];
		for (int i=0; i<c.length; ++i) {
			c[i] = (byte) (a[i] ^ b[i]);
		}
		return c;
	}
	// helper conversion function
	public static SecretKey bigIntToKey(BigInteger big, int size, String C) {
		byte[] bytes = big.toByteArray();
		if (bytes.length > size) {
			for (int i=0; i<bytes.length - size; ++i) {
				if (bytes[i] != 0)
					throw new RuntimeException("oops  " + bytes.length + " > " + size);
			}
			byte[] newbytes = new byte[size];
			System.arraycopy(bytes, bytes.length - size, newbytes, 
					0, size);
			bytes = newbytes;
		}
		if (bytes.length < size) {
			byte[] newbytes = new byte[size];
			System.arraycopy(bytes, 0, newbytes, 
					size - bytes.length, bytes.length);
			bytes = newbytes;
		}
		return new SecretKeySpec(bytes, C);
	}
	// helper conversion function
	public static BigInteger keyToBigInt(SecretKey sk) {
		return new BigInteger(1, sk.getEncoded());
	}
}
