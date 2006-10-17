package sfe.crypto;
import java.math.*;
import java.util.*;
import static java.math.BigInteger.ZERO;
import static java.math.BigInteger.ONE;

public final class DPE {
	static BigInteger TWO = ONE.add(ONE);
	static BigInteger THREE = TWO.add(ONE);
	
	static BigInteger modMul(BigInteger x, BigInteger y, BigInteger n) {
		return x.multiply(y).mod(n);
	}
	static BigInteger modDiv(BigInteger x, BigInteger y, BigInteger n) {
		return modMul(x, y.modInverse(n), n);
	}
	
	static boolean isResidue(BigInteger x, BigInteger exp, BigInteger p) {
		// p should be prime
		if (!p.mod(exp).equals(ONE))
			return true;
		if (x.modPow(p.subtract(ONE).divide(exp), p).equals(ONE))
			return true;
		return false;
	}
	
	static class DecryptFailureException extends RuntimeException {
		DecryptFailureException() {}
		DecryptFailureException(String m) {
			super(m);
		}
	}
	
	// public key
	public final static class EncKey implements HomomorphicCipher.EncKey, java.io.Serializable {
		BigInteger r;
		BigInteger y;
		BigInteger n;
		
		public String toString() {
			return "EncKey("+r+","+y+","+n+")";
		}
		
		BigInteger genU(Random rand) {
			BigInteger u;
			do {
				u = new BigInteger(n.bitLength()+16, rand);
			} while (!u.gcd(n).equals(ONE));
			return u.mod(n);
		}
		public BigInteger encrypt(BigInteger M) {
			BigInteger u = genU(new Random());
			return encrypt(M, u);
		}
		BigInteger encrypt(BigInteger M, BigInteger u) {
			return modMul(y.modPow(M, n), u.modPow(r, n), n);
		}
		EncKey(BigInteger r, BigInteger y, BigInteger n) {
			this.r = r;
			this.y = y;
			this.n = n;
		}
	}
	
	// private key, not serializable
	final public static class DecKey implements HomomorphicCipher.DecKey {
		BigInteger r;
		BigInteger p;
		BigInteger q;
		BigInteger y;
		BigInteger n;
		
		int k;			// r = 3^k
		
		DecKey(BigInteger r, BigInteger p, BigInteger q, BigInteger y) {
			this.r = r;
			this.p = p;
			this.q = q;
			this.y = y;
			this.n = p.multiply(q);
			
			int k = 0;
			
			BigInteger rok = r;
			if (r.compareTo(ONE) < 0) {
				throw new NumberFormatException("r is not positive");
			}
			do {
				if (!rok.mod(THREE).equals(ZERO))
					throw new NumberFormatException("r is not 3^k");
				rok = rok.divide(THREE);
				++k;
			} while (!rok.equals(ONE));
			
			this.k = k;
		}
		
		public EncKey encKey() {
			return new EncKey(r, y, n);
		}
		
		public BigInteger decrypt(BigInteger z) {
			// assume r = 3^k, or else this algorithm fails miserably.  would
			// need a different algorithm for other cases.
			
			BigInteger yinv = y.modInverse(n);
			BigInteger pm1qm1o3i = p.subtract(ONE).multiply(q.subtract(ONE));
			BigInteger result = ZERO;
			BigInteger place = ONE;
			
			for (int i=1; i <= this.k; ++i) {
				BigInteger tki3i = ZERO;
				int tki;
				pm1qm1o3i = pm1qm1o3i.divide(THREE);
				for (tki=0; tki<3; ++tki) {
					BigInteger exp = (tki3i == ZERO) ?
							result : result.add(tki3i);
					BigInteger v = modMul(yinv.modPow(exp, n), z, n);
					v = v.modPow(pm1qm1o3i, n);
					if (v.equals(ONE)) break;
					tki3i = tki3i.add(place);
				}
				if (tki == 3) throw new DecryptFailureException
				("r=" + r + "  p=" + p + "  q=" + q + "  y=" + y);
				
				//System.out.println("tk_" + i + " = " + tki);
				
				if (tki3i != ZERO) {
					result = result.add(tki3i);
				}
				place = place.add(place).add(place);
			}
			
			return result;
		}
		
		// make a new Y
		void makeNewY(Random rand) {
			BigInteger pm1qm1or = p.subtract(ONE).divide(r).
			multiply(q.subtract(ONE));
			do {
				//y = new BigInteger(n.bitLength()+16, rand).mod(n);
				y = y.add(ONE);
			} while (!y.gcd(n).equals(ONE) || 
					y.modPow(pm1qm1or, n).equals(ONE));
			//System.out.println("y^pm1qm1or = " + y.modPow(pm1qm1or, n));
			System.out.println("y = " + y);
		}
		
		// returns true if the key passes tests
		boolean validate(Random rand) {
			System.out.println("testing key...");
			DecKey dk = this;
			EncKey ek = dk.encKey();
			int i;
			
			BigInteger zi = ek.encrypt(ONE, ONE);
			
			System.out.println("y 3R p: " + isResidue(y, THREE, p) +
					"  y 3R q: " + isResidue(y, THREE, q));
			try {
				BigInteger MMi = dk.decrypt(zi);
				if (!MMi.equals(ONE)) {
					System.out.println("bad key");
					return false;
				}
				//System.out.println("pass " + i + " " + j);
			} catch (DecryptFailureException ex) {
				BigInteger pm1qm1or = p.subtract(ONE).divide(r);
				BigInteger ypq = y.modPow(pm1qm1or, n);
				System.out.println(ex);
				//System.out.println("(ypq,r)=" + ypq.gcd(r));
				//System.out.println("(ypq,n)=" + ypq.gcd(n));
				return false;
			}
			return true;
		}
	}
	
	public static DecKey genKey(BigInteger r, int nBits) {
		return genKey(r, nBits, true);
	}
	public static DecKey genKey(BigInteger r, int nBits, boolean validate) {
		// r should be odd, nBits should be big enough...
		// for decryption, r should be 3^k
		
		Random rand = new Random();
		
		BigInteger p = new BigInteger(nBits, rand);
		BigInteger q = new BigInteger(nBits, rand);
		
		// find p such that r | p-1, gcd(r,(p-1)/r)==1
		// i.e.  p = 1 (mod r) and gcd(r, p-1) = 1
		BigInteger pmr = p.mod(r);
		pmr = pmr.subtract(ONE);
		p = p.subtract(pmr);
		
		assert p.mod(r).equals(ONE);
		
		BigInteger pm1or = p.subtract(ONE).divide(r);
		//BigInteger r2 = r.add(r);
		//if (p.mod(TWO).equals(ZERO)) {
		//  p = p.add(r);
		//}
		
		while(!p.isProbablePrime(100) || !pm1or.gcd(r).equals(ONE)) {
			p = p.add(r);
			pm1or = pm1or.add(ONE);
		}
		
		// find q such that gcd(r,q-1)=1
		if (!q.isProbablePrime(100)) {
			q = q.nextProbablePrime();
		}
		BigInteger qm1 = q.subtract(ONE);
		while(!qm1.gcd(r).equals(ONE)) {
			q = q.nextProbablePrime();
			qm1 = q.subtract(ONE);
		}
		
		BigInteger n = p.multiply(q);
		BigInteger pm1qm1or = pm1or.multiply(qm1);
		
		BigInteger y;
		
		do {
			y = new BigInteger(n.bitLength()+16, rand).mod(n);
		} while (!y.gcd(n).equals(ONE) || 
				(isResidue(y, THREE, p) && isResidue(y, THREE, q)) ||
				y.modPow(pm1qm1or, n).equals(ONE));
		
		assert p.mod(r).equals(ONE);
		assert p.subtract(ONE).divide(r).gcd(r).equals(ONE);
		assert q.subtract(ONE).gcd(r).equals(ONE);
		assert p.isProbablePrime(100);
		assert q.isProbablePrime(100);
		assert y.gcd(n).equals(ONE);
		assert !y.modPow(p.subtract(ONE).
				multiply(q.subtract(ONE)).divide(r), n).equals(ONE);
		
		DecKey dk = new DecKey(r, p, q, y);
		//return dk;
		
		// test it...
		if (validate)
			while(!dk.validate(rand))
				dk.makeNewY(rand);
		
		return dk;
	}
	
	// test
	public static void main(String[] args) {
		Random rand = new Random();
		
		//BigInteger r = THREE.pow(16);
		//int nbits = r.bitLength() + 12;
		BigInteger min = TWO.pow(Integer.parseInt(args[0]));
		//BigInteger r = THREE.pow(Integer.parseInt(args[0]));
		BigInteger r = THREE;
		while (r.compareTo(min) < 0) {
			r = r.multiply(THREE);
		}
		int nbits = Integer.parseInt(args[1]);
		
		DecKey d = genKey(r, nbits);
		
		/*
		 int good = 0;
		 for (int i=0; i<1000; ++i) {
		 if (d.validate(rand)) {
		 good++;
		 }
		 d.makeNewY(rand);
		 }
		 System.out.println("good Ys: " + good + "  fraction: " + good/1000.0);
		 System.exit(0);
		 */
		
		System.out.println("DecKey:"); // rpqyn
		System.out.println("r = " + d.r);
		System.out.println("p = " + d.p);
		System.out.println("q = " + d.q);
		System.out.println("y = " + d.y);
		System.out.println("n = " + d.n);
		System.out.println();
		
		EncKey e = d.encKey();
		/*
		 System.out.println("r = " + e.r);
		 System.out.println("y = " + e.y);
		 System.out.println("n = " + e.n);
		 System.out.println();
		 */
		
		BigInteger M = new BigInteger(d.r.bitLength() + 16, rand).mod(r);
		System.out.println("M = " + M);
		BigInteger z1 = e.encrypt(M);
		System.out.println("z1 = " + z1);
		BigInteger z2 = e.encrypt(M);
		System.out.println("z2 = " + z2);
		BigInteger z3 = e.encrypt(M);
		System.out.println("z3 = " + z3);
		BigInteger M1 = d.decrypt(z1);
		System.out.println("M1 = " + M1);
		BigInteger M2 = d.decrypt(z2);
		System.out.println("M2 = " + M2);
		BigInteger M3 = d.decrypt(z3);
		System.out.println("M3 = " + M3);
	}
}

// sally - 838 0413 x122
