/*
 * Paillier.h
 *
 *  Created on: Aug 6, 2009
 *      Author: louis
 */

#ifndef PAILLIER_H_
#define PAILLIER_H_

#define _INLINE __attribute__((always_inline))
#include "cipher/Cipher.h"
#include "bigint.h"

namespace crypto {
namespace cipher {

using namespace silly::bigint;

// L function
static inline BigInt L(const BigInt &u, const BigInt &n) {
	return (u-1)/n;
}

struct PaillierEncKey {
	BigInt n;
	BigInt g;
	BigInt n2;
	const BigInt E_ZERO;
	//		public String toString() {
	//			return "EncKey("+g+","+n+")";
	//		}
	PaillierEncKey(CBigInt &n0, CBigInt &g0) : n(n0), g(g0) {
		n2 = n*n;
		*const_cast<BigInt*>(&E_ZERO) = encrypt(0);
	}
	BigInt encrypt(CBigInt &M) const _INLINE {
		BigInt r;
		do {
			r = BigInt::random(n2.bitLength()).mod(n2);
		} while (!r.gcd(n).equals(1));
		// while (! (r GCD n == 1));

		return g.modPow(M, n2).modMultiply(r.modPow(n, n2), n2);
		//return ((g^M) % n2)*((r^n) % n2) % n2;
	}
	// add in the encrypted domain
	BigInt add(CBigInt &x, CBigInt &y) const _INLINE {
		return x.modMultiply(y, n2);
		// return (x*y) % n2;
	}
	// multiply encrypted x by unencrypted y
	BigInt multByPlain(CBigInt &x, CBigInt &y) const _INLINE {
		BigInt z = E_ZERO;
		BigInt xx = x;
		int len = y.bitLength();
		for (int i=0; i<len; ++i) {
			if (y.testBit(i)) {
				z = add(z,xx);
			}
			xx = add(xx, xx);
		}
		return z;
	}
};

struct PaillierDecKey {
	BigInt n;
	BigInt g;
	BigInt lambda;
	BigInt u;
	BigInt n2;
	PaillierDecKey(CBigInt &n0, CBigInt &g0, CBigInt &lambda0, CBigInt &u0) :
		n(n0), g(g0), lambda(lambda0), u(u0), n2(n*n) {}

	PaillierEncKey encKey() const {
		return PaillierEncKey(n, g);
	}

	BigInt decrypt(CBigInt &z) const {
		BigInt n2 = n.multiply(n);
		return L(z.modPow(lambda, n2), n).multiply(u).mod(n);
	}

};

class Paillier : public Cipher {
public:
	BigInt n;
	BigInt g;


	static PaillierDecKey genKey(int nBits);
};

}
}


#endif /* PAILLIER_H_ */
