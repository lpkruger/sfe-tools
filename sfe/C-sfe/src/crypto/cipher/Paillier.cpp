/*
 * Paillier.cpp
 *
 *  Created on: Aug 6, 2009
 *      Author: louis
 */

#include "Paillier.h"
#include "../cryptoio.h"

using namespace crypto::cipher;

PaillierDecKey Paillier::genKey(int nBits) {
	BigInt p = BigInt::genPrime(nBits);
	BigInt q;
	BigInt pm1, qm1;
	BigInt ONE(1);
	do {
		do {
			q = BigInt::genPrime(nBits);
		} while (q == p);

		//out("p=" + p + "  q=" + q);


		pm1 = p-1;
		qm1 = q-1;
	} while ((p*q).gcd(pm1*qm1) != ONE);
	BigInt gcd = pm1.gcd(qm1);
	// compute LCM(p-1, q-1)
	BigInt lambda = pm1/gcd*qm1;

	BigInt n = p*q;
	BigInt n2 = n*n;

	BigInt g;
	BigInt u;
	do {
		g = BigInt::random(n2.bitLength()).mod(n2);
		u = L(g.modPow(lambda, n2), n);
	} while (!g.gcd(n).equals(1) || !u.gcd(n).equals(1));
	u = u.modInverse(n);

	return PaillierDecKey(n, g, lambda, u);
}

#define out(x) std::cout << x << std::endl
#define nl() std::cout << std::endl

static int _main(int argc, char **argv) {
	using namespace crypto::cipher;
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

	BigInt M = 15;
	M = BigInt::random(d.n.bitLength()/2);

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
	out("   3 * M = " << (M*3).toString());
	BigInt z_3 = e.add(e.add(z1,z2),z3);
	out("z1+z2+z3 = " << z_3);
	out("D(z+z+z) = " << d.decrypt(z_3));
	z_3 = e.multByPlain(z1, BigInt(73));
	out("D(73* z) = " << d.decrypt(z_3));
	nl();
//	if (--count)
//		goto again;

	nl();
	BigInt ONE(1);
	BigInt E_ONE = e.encrypt(ONE);
	BigInt EM = e.multByPlain(E_ONE, M);
	out("D(1 * M) = " << d.decrypt(EM));
	return 0;
}

#include "sillymain.h"
MAIN("pailliertest")

