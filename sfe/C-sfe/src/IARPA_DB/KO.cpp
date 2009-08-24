/*
 * KO.cpp
 *
 *  Created on: Aug 6, 2009
 *      Author: louis
 */

//#undef _GLIBCXX_DEBUG
#include <iostream>

#include "KO.h"
#include <string.h>
#include <openssl/rc4.h>

using namespace std;

const int KO::test_sizes[] = { 1,2,3, 10, 20, 50, 100, 200, 500, 1000, 2000, 5000, 10000 };
const int KO::test_sizes_length = sizeof(test_sizes)/sizeof(int);


void KO::clientxfer1(BigInteger w) {
	rr = BN_new();
	BN_rand(rr, BN_num_bits(rsa->n), 0, 0);
	//BigInteger Y = r.modPow(e, N).multiply(H(w)).mod(N);
	BigInteger Y = BN_new();
	BN_mod_exp(Y, rr, rsa->e, rsa->n, bn_ctx);
	BN_mod_mul(Y, Y, H(w), rsa->n, bn_ctx);
	Y_ = Y;
}

void KO::clientxfer2(BigInteger w) {
	BigInteger X = X_;
	BigInteger N = rsa->n;
	BigInteger wwhat = BN_new();
	BigInteger rrinv = BN_new();
	BN_mod_inverse(rrinv, rr, N, bn_ctx);
	BN_mod_mul(wwhat, X, rrinv, N, bn_ctx);
//	printf("C: %s %s %s %s\n", BN_bn2hex(wwhat), BN_bn2hex(rr),
//			BN_bn2hex(rrinv), BN_bn2hex(X));

	BigInteger ii = BN_new();
	for (int i=0; i<n; ++i) {
		BN_set_word(ii, i);
		vector<BigInteger> input(3);
		input[0]=w; input[1]=wwhat; input[2]=ii;
		//printf("C: %s %s %s\n", BN_bn2hex(w), BN_bn2hex(wwhat), BN_bn2hex(ii));
		vector<byte> mi = Gxor(input, mhat[i]);

		for (int j=0; j<L; ++j) {
			// test for leading 0s
			if (mi[j]!=0)
				goto search;
		}

		for (uint j=0; j<mi.size(); ++j) {
			printf("%02x ", mi[j]);
		}
		printf("\n");

		search:
		do {} while(0);
	}
	BN_free(ii);
	printf("All done\n");
}

void KO::servercommit(DDB & ddb) {
	rsa = RSA_generate_key(1024, 17, NULL, NULL);
	int dbsize = ddb.thedb.size();
	//Set<BigInteger> keys = db.thedb.keySet();
	what.resize(dbsize);
	mhat.resize(dbsize);
	int i=0;
	map<BigInteger,BigInteger,bncmp>::iterator it;
	for (it = ddb.thedb.begin(); it != ddb.thedb.end(); ++it) {
		BigInteger w = (*it).first;
		BigInteger m = (*it).second;
		//printf("A: %s %s\n",BN_bn2dec(w),BN_bn2dec(m));

		what[i] = BN_new();
		BN_mod_exp(what[i],H(w),rsa->d,rsa->n, bn_ctx);

		int mlen = BN_num_bytes(m);
		//byte* mm = new byte[mlen+L];
		vector<byte> mm(mlen+L, 0);
		BN_bn2bin(m, &mm[L]);
		//memset(mm, 0, L);
		BigInteger ii = BN_new();
		BN_set_word(ii, i);
		vector<BigInteger> input(3);
		input[0]=w; input[1]=what[i]; input[2]=ii;
		//printf("S: %s %s %s\n", BN_bn2hex(w), BN_bn2hex(what[i]), BN_bn2hex(ii));
		mhat[i] = Gxor(input, mm);
		BN_free(ii);
		++i;
	}
}

void KO::serverxfer() {
	BigInteger Y = Y_;
	BigInteger X = BN_new();
	//= Y.modPow(rsa.privateKey, N);
	BN_mod_exp(X, Y, rsa->d, rsa->n, bn_ctx);
	X_ = X;
}

//byte* KO::Gxor(BigInteger* x, byte* m, int len, int *outlen) {
vector<byte> KO::Gxor(vector<BigInteger> &x, vector<byte> &m) {
	vector<byte> zz;

	int totalsize=0;
	int zlen;
	for (uint i=0; i<x.size(); ++i) {
		zlen = BN_num_bytes(x[i]);
		if(!zlen) ++zlen;
		totalsize+= zlen;
		//printf("x[%d]/%d total = %d\n", i, zlen, totalsize);
	}
#if 1 // better impl
	zz.resize(totalsize, 0);
	int count=0;
	for (uint i=0; i<x.size(); ++i) {
		zlen = BN_num_bytes(x[i]);
		if(!zlen) ++zlen;
		//zz.at(count);
		//printf("x[%d] = %d @ %d\n", i, zlen, count);
		BN_bn2bin(x[i], &zz[count]);
		count += zlen;
	}
#else
	zz.reserve(totalsize);
	byte* z;
	for (uint i=0; i<x.size(); ++i) {
		zlen = BN_num_bytes(x[i]);
		if(!zlen) ++zlen;
		z = new byte[zlen];
		BN_bn2bin(x[i], z);
		for (int j=0; j<zlen; ++j) {
			zz.push_back(z[j]);
		}
		delete[] z;
	}
#endif
	// use RC4 as PRNG
	vector<byte> out(m.size());
	RC4_KEY key;
	RC4_set_key(&key, zz.size(), &zz[0]);
	RC4(&key, m.size(), &m[0], &out[0]);
	return out;

	// PRNG implementation?
//	SecureRandom prng = DeterministicRandom.getRandom(xx.toByteArray());
//	byte[] r = new byte[m.length];
//	prng.nextBytes(r);
//	for (int i=0; i<r.length; ++i) {
//		r[i] ^= m[i];
//	}
//	return r;

}

