package count;

import java.math.BigInteger;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

public class Test {

	static void printBits(byte[] h) {
		for (int i=0; i<h.length; ++i) {
			String b = Integer.toBinaryString(((int)h[i])&0xff);
			while (b.length()<8) {
				b = "0"+b;
			}
			System.out.print(b);
		}
		System.out.println();
	}
	public static void main(String[] args) {
		for (int num=0; num<70; ++num) {
			//VirtualBitmap bitmap = new VirtualBitmap();
			SegmentBitmap bitmap = new SegmentBitmap();
			bitmap.init();
			for (int i=0; i<num; ++i) {
				byte[] h = hash(i);
				//printBits(h);
				bitmap.set(h);
			}
			System.out.println(num+" :  " + bitmap.count());
		}
	}
	
	static byte[] hash(int n) {
		return hmac(n);
	}
	
	static byte[] sha(int n) {
		MessageDigest md = null;
		try {
			md = MessageDigest.getInstance("SHA-1");
		} catch (NoSuchAlgorithmException e) {
			
			e.printStackTrace();
		}
		//md.reset();
		byte[] dig = md.digest(BigInteger.valueOf(n).toByteArray());
		return dig;
	}
	
	static {
		int rand = (int) (Math.random()*10000);
		System.out.println("random seed = " + rand);
		byte[] randb = BigInteger.valueOf(rand).toByteArray();
		key = new SecretKeySpec(randb, "HmacSHA1");
	}
	static SecretKeySpec key;
	
	static byte[] hmac(int n) {
		Mac mac = null;
		try {
			mac = Mac.getInstance("HmacSHA1");
			mac.init(key);
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		} catch (InvalidKeyException e) {
			e.printStackTrace();
		}
		
		byte[] dig = mac.doFinal(BigInteger.valueOf(n).toByteArray());
		return dig;
		
	}
	

}
