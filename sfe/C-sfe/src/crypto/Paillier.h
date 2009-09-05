/*
 * Paillier.h
 *
 *  Created on: Aug 6, 2009
 *      Author: louis
 */

#ifndef PAILLIER_H_
#define PAILLIER_H_

#include "cipher/Cipher.h"
#include "bigint.h"

namespace crypto {
namespace cipher {

using namespace silly::bigint;

// L function
static BigInt L(const BigInt &u, const BigInt &n) {
	return (u-1)/n;
}

struct PaillierEncKey {
	BigInt n;
	BigInt g;
	BigInt n2;
	//		public String toString() {
	//			return "EncKey("+g+","+n+")";
	//		}
	PaillierEncKey(const BigInt &n0, const BigInt &g0) : n(n0), g(g0) {
		n2 = n*n;
	}
	BigInt encrypt(BigInt &M) {
		BigInt r;
		do {
			r = BigInt::random(n2.bitLength()).mod(n2);
		} while (!r.gcd(n).equals(1));

		return g.modPow(M, n2).modMultiply(r.modPow(n, n2), n2);
	}
	BigInt add(const BigInt &x, const BigInt &y) {
		return x.modMultiply(y, n2);
	}
};

struct PaillierDecKey {
	BigInt n;
	BigInt g;
	BigInt lambda;
	BigInt u;
	BigInt n2;
	PaillierDecKey(BigInt n0, BigInt &g0, BigInt &lambda0, BigInt &u0) :
		n(n0), g(g0), lambda(lambda0), u(u0) {
		n2 = n*n;
	}

	PaillierEncKey encKey() {
		return PaillierEncKey(n, g);
	}

	BigInt decrypt(BigInt &z) {
		BigInt n2 = n.multiply(n);
		return L(z.modPow(lambda, n2), n).multiply(u).mod(n);
	}

};

class Paillier : public Cipher {
public:
	BigInt n;
	BigInt g;


	static PaillierDecKey genKey(int nBits) {
		BigInt p = BigInt::random(nBits).nextProbablePrime();
		BigInt q;
		do {
			q = BigInt::random(nBits).nextProbablePrime();
		} while (q == p);

		//out("p=" + p + "  q=" + q);

		// computer LCM(p-1, q-1)
		BigInt pm1 = p-1;
		BigInt qm1 = q-1;
		BigInt gcd = pm1.gcd(qm1);
		BigInt lambda = pm1/gcd*qm1;

		BigInt n = p.multiply(q);
		BigInt n2 = n.multiply(n);

		BigInt g;
		BigInt u;
		do {
			g = BigInt::random(n2.bitLength()).mod(n2);
			u = L(g.modPow(lambda, n2), n);
		} while (!g.gcd(n).equals(1) || !u.gcd(n).equals(1));
		u = u.modInverse(n);

		return PaillierDecKey(n, g, lambda, u);
	}
};

#define out(x) std::cout << x << std::endl
#define nl() std::cout << std::endl
	// test



}
}

using namespace crypto::cipher;

static std::ostream& operator<<(std::ostream &out, const BigInt& num) {
	out << num.toString();
	return out;
}

static int _main(int argc, char **argv) {
	vector<string> args(argc-1);
	for (int i=1; i<argc; ++i) {
		args[i-1] = argv[i];
	}

	int nbits = strtol(args.at(0).c_str(), NULL, 0);

	PaillierDecKey d = Paillier::genKey(nbits);

	out("DecKey:"); // rpqyn
	out("n = " << d.n);
	out("lambda = " << d.lambda);
	out("u = " << d.u);
	nl();

	PaillierEncKey e = d.encKey();
	out("g = " << e.g);
	out("n = " << e.n);
	nl();


	BigInt M = BigInt::random(d.n.bitLength()).mod(d.n);
	out("M = " << M);
	BigInt z1 = e.encrypt(M);
	out("z1 = " << z1);
	BigInt z2 = e.encrypt(M);
	out("z2 = " << z2);
	BigInt z3 = e.encrypt(M);
	out("z3 = " << z3);
	BigInt M1 = d.decrypt(z1);
	out("M1 = " << M1);
	BigInt M2 = d.decrypt(z2);
	out("M2 = " << M2);
	BigInt M3 = d.decrypt(z3);
	out("M3 = " << M3);
	nl();
	out("   3 * M = " << (M*3));
	BigInt z_3 = e.add(e.add(z1,z2),z3);
	out("z1+z2+z3 = " << z_3);
	out("D(3 * z) = " << d.decrypt(z_3));
	nl();
	return 0;
}

#include "sillymain.h"
MAIN("pailliertest")

#endif /* PAILLIER_H_ */
