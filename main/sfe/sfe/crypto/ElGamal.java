package sfe.crypto;

import java.math.*;
import java.security.SecureRandom;
import java.util.Random;

public class ElGamal {
	
	static Random rand = new SecureRandom();
	
	@SuppressWarnings("serial")
	public static class Ciphertext implements java.io.Serializable {
		public Ciphertext(BigInteger a, BigInteger b) {
			this.a = a;
			this.b = b;
		}
		
		public BigInteger a;
		public BigInteger b;
	}
	public static class EncKey implements java.io.Serializable {
		BigInteger g; // generator;
		BigInteger P;
		BigInteger h;
		
		// TODO: use q=p-1/2
		public Ciphertext encrypt(BigInteger M) {
			BigInteger r = new BigInteger(16*P.bitLength(), rand);
			r = r.mod(P);
			return new Ciphertext(
					g.modPow(M, r),
					M.multiply(h.modPow(r, P)).mod(P)
			);
		}

		public Ciphertext mult(Ciphertext x1, Ciphertext x2) {
			return new Ciphertext(
					x1.a.multiply(x2.a).mod(P),
					x1.b.multiply(x2.b).mod(P)
			);
		}
		
		public Ciphertext random() {
			return new Ciphertext(
					new BigInteger(16*P.bitLength(), rand).mod(P),
					new BigInteger(16*P.bitLength(), rand).mod(P)
			);
		}
	}
	
	public static class DecKey {
		BigInteger g; // generator
		BigInteger alpha; // private exponent
		BigInteger P;
		public BigInteger decrypt(Ciphertext z) {
			return z.b.multiply(z.a.modPow(alpha, P)).mod(P);
		}

		public EncKey encKey() {
			EncKey e = new EncKey();
			e.g = g;
			e.P = P;
			e.h = g.modPow(alpha.negate(), P);
			return e;
		}
		
	}
	
	public static DecKey genKey(int nbits) {
		BigInteger q = new BigInteger(nbits-2, rand);
		BigInteger p;
		
		// find p=2q+1 for p,q prime
		do {
			q = q.nextProbablePrime();
			p = q.add(q).add(BigInteger.ONE);
		} while (!p.isProbablePrime(100));
		
		// find generator g
		BigInteger g, x, y;
		//BigInteger qm1 = q.subtract(BigInteger.ONE);
		do {
			g = new BigInteger(16*p.bitLength(), rand).mod(p);
			x = g.modPow(q, p);
			y = x.multiply(x).mod(p);
			System.out.println(y);
		} while(x.equals(BigInteger.ONE));
		System.out.println("found g");
		
		DecKey key = new DecKey();
		key.P = p;
		key.g = g;
		key.alpha = new BigInteger(16*p.bitLength(), rand).mod(p);
		key.alpha = key.alpha.multiply(key.alpha).mod(p);
		
		return key;
		
	}
	
	public static void main(String[] args) {
		// TODO: test suite
	}
}
