/*
 * Paillier.h
 *
 *  Created on: Aug 6, 2009
 *      Author: louis
 */

#ifndef PAILLIER_H_
#define PAILLIER_H_

#include <openssl/bn.h>

typedef BIGNUM* BigInteger;

class Paillier {
public:
	BigInteger ZERO;
	BigInteger ONE;

	BigInteger n;
	BigInteger g;

	BigInteger encrypt(BIGNUM M);
	BigInteger add(BigInteger x, BigInteger y);

	Paillier() {
		ZERO = BN_new();
		ONE = BN_new();
		BN_zero(ZERO);
		BN_one(ONE);

	}
	virtual ~Paillier();

/*
	// private key, not serializable
	final public static class DecKey implements HomomorphicCipher.DecKey {
		BigInteger n;
		BigInteger g;
		BigInteger lambda;
		BigInteger u;

		DecKey(BigInteger n, BigInteger g, BigInteger lambda, BigInteger u) {
			this.n = n;
			this.g = g;
			this.lambda = lambda;
			this.u = u;
		}

		public EncKey encKey() {
			return new EncKey(n, g);
		}

		public BigInteger decrypt(BigInteger z) {
			BigInteger n2 = n.multiply(n);
			return L(z.modPow(lambda, n2), n).multiply(u).mod(n);
		}


	}

	// L function
	static BigInteger L(BigInteger u, BigInteger n) {
		return u.subtract(ONE).divide(n);
	}

	public static DecKey genKey(int nBits) {
		Random rand = new Random();

		BigInteger p = new BigInteger(nBits, 100, rand);
		BigInteger q;
		do {
			q = new BigInteger(nBits, 100, rand);
		} while (q.equals(p));

		//System.out.println("p=" + p + "  q=" + q);

		// computer LCM(p-1, q-1)
		BigInteger pm1 = p.subtract(ONE);
		BigInteger qm1 = q.subtract(ONE);
		BigInteger gcd = pm1.gcd(qm1);
		BigInteger lambda = pm1.divide(gcd).multiply(qm1);

		BigInteger n = p.multiply(q);
		BigInteger n2 = n.multiply(n);

		BigInteger g;
		BigInteger u;
		do {
			g = new BigInteger(n2.bitLength()+16, rand).mod(n2);
			u = L(g.modPow(lambda, n2), n);
		} while (!g.gcd(n).equals(ONE) || !u.gcd(n).equals(ONE));
		u = u.modInverse(n);

		return new DecKey(n, g, lambda, u);
	}

	// test
	public static void main(String[] args) {
		Random rand = new Random();

		int nbits = Integer.parseInt(args[0]);

		DecKey d = genKey(nbits);

		System.out.println("DecKey:"); // rpqyn
		System.out.println("n = " + d.n);
		System.out.println("lambda = " + d.lambda);
		System.out.println("u = " + d.u);
		System.out.println();

		EncKey e = d.encKey();
		System.out.println("g = " + e.g);
		System.out.println("n = " + e.n);
		System.out.println();


		BigInteger M = new BigInteger(d.n.bitLength() + 16, rand).mod(d.n);
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

	 */
};

#endif /* PAILLIER_H_ */
