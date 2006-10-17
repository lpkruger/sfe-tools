package sfe.crypto;

import java.security.GeneralSecurityException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.spec.AlgorithmParameterSpec;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;

// based on the javax.crypto.Cipher API, but does not
// completely implement it (currently)

/**
 * Secure Hash/XOR based encryption
 */
public class SFECipher {
	public final static int ENCRYPT_MODE = Cipher.ENCRYPT_MODE;
	public final static int DECRYPT_MODE = Cipher.DECRYPT_MODE;
	
	public static SFECipher getInstance(String cipher) 
	throws GeneralSecurityException {
		String[] cmp = cipher.split("/");
		MessageDigest md = MessageDigest.getInstance(cmp[0]);
		if (cmp.length == 1 || cmp.length == 2) {
			return new SFECipher(md, true);
		} else if (cmp.length == 3) {
			boolean use_padding = !"NoPadding".equals(cmp[2]);
			return new SFECipher(md, use_padding);
		} else {
			throw new GeneralSecurityException("Bad cipher: " + cipher);
		}
	}
	
	private SFECipher(MessageDigest md, boolean use_padding) {
		this.md = md;
		this.use_padding = use_padding;
	}
	
	MessageDigest md;
	int mode = -1;
	SecretKey sk;
	boolean use_padding;

	public void init(int mode, SecretKey sk) throws InvalidKeyException {
		this.mode = mode;
		this.sk = sk;
		this.md.reset();
	}
	
	void  init(int mode, SecretKey sk, AlgorithmParameterSpec params) 
	throws InvalidKeyException, InvalidAlgorithmParameterException {
		if (!(params instanceof IvParameterSpec))
			throw new InvalidAlgorithmParameterException();
		init(mode, sk);
		md.update(((IvParameterSpec) params).getIV());	
	}
	
	public void setIV(byte[] iv) {
		md.reset();
		md.update(iv);
	}
	
	public byte[] doFinal(byte[] data) 
	throws IllegalBlockSizeException, BadPaddingException {
		SecretKey key = sk;
		sk = null;
		switch(mode) {
		case ENCRYPT_MODE:
			mode = -1;
			return use_padding ? encrypt(key, data) : deencrypt(key, data);
		case DECRYPT_MODE:
			mode = -1;
			return use_padding ? decrypt(key, data) : deencrypt(key, data);
		default:
			throw new IllegalStateException("Invalid mode: " + mode);
		}
	}
	
	// padding-free mode
	private byte[] deencrypt(SecretKey key, byte[] data) {
		byte[] xkey = md.digest(key.getEncoded());
		if (xkey.length < data.length) {
			throw new IllegalStateException("data too long");
		}
		byte[] ret = new byte[data.length];
		for (int i=0; i<ret.length; ++i) {
			ret[i] = (byte) (data[i] ^ xkey[i]);
		}
		return ret;
	}
	
	private byte[] encrypt(SecretKey key, byte[] data) {
		byte[] xkey = md.digest(key.getEncoded());
		if (xkey.length - 1 < data.length) {
			throw new IllegalStateException("data too long");
		}
		byte[] ret = new byte[xkey.length];
		for (int i=0; i<data.length; ++i) {
			ret[i] = (byte) (data[i] ^ xkey[i]);
		}
		for (int i=data.length; i<ret.length - 1; ++i) {
			ret[i] = xkey[i];
		}
		ret[ret.length - 1] = (byte) (xkey[ret.length - 1] ^ data.length); 
		return ret;
	}
	
	private byte[] decrypt(SecretKey key, byte[] data) throws BadPaddingException {
		byte[] xkey = md.digest(key.getEncoded());
		if (xkey.length != data.length) {
			throw new BadPaddingException("Invalid data length");
		}
		byte[] dec = new byte[data.length];
		for (int i=0; i<dec.length; ++i) {
			dec[i] = (byte) (data[i] ^ xkey[i]);
		}
		int len = dec[dec.length - 1];
		if (len > dec.length - 1 || len < 0)
			throw new BadPaddingException("Invalid data range");
		for (int i=len; i<dec.length-1; ++i) {
			if (dec[i] != 0)
				throw new BadPaddingException("Invalid data range");
		}
		byte[] ret = new byte[len];
		System.arraycopy(dec, 0, ret, 0, len);
		//System.out.println("len is " + len + "  xkey is " + xkey.length);
		return ret;
	}
}
