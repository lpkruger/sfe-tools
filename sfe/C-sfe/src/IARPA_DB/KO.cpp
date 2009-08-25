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

#define D(X)

void KO::clientxfer1(const BigInt &w) {
	BN_rand(rr.writePtr(), BN_num_bits(rsa->n), 0, 0);
	BigInt Y = rr.modPow(rsa->e, rsa->n);
	Y.modMultiplyThis(H(w), rsa->n);
	Y_. swapWith(Y);
}

void KO::clientxfer2(const BigInt &w) {
	const BigInt &X = X_;

	BigInt wwhat = X.modDivide(rr, rsa->n);
	D(printf("C: %s %s %s\n", wwhat.toHexString().c_str(), rr.toHexString().c_str(), X.toHexString().c_str());)

	BigInt ii;
	for (int i=0; i<n; ++i) {
		ii = i;
		BNcPtr vals[3] = { w, wwhat, ii };
		vector<BNcPtr> input(vals, vals+3);

		D(printf("C: %s %s %02x\n", w.toHexString().c_str(), wwhat.toHexString().c_str(), i);)
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
	printf("All done\n");
}

void KO::servercommit(DDB & ddb) {
	rsa = RSA_generate_key(1024, 17, NULL, NULL);
	int dbsize = ddb.thedb.size();
	what.resize(dbsize);
	mhat.resize(dbsize);
	int i=0;
	BigInt ii;
	map<BigInt,BigInt>::iterator it;
	for (it = ddb.thedb.begin(); it != ddb.thedb.end(); ++it) {
		const BigInt &w = it->first;
		const BigInt &m = it->second;
		//printf("A: %s %s\n",BN_bn2dec(w),BN_bn2dec(m));

		what[i] = H(w).modPow(rsa->d, rsa->n);
		int mlen = BN_num_bytes(m.ptr());
		//byte* mm = new byte[mlen+L];
		vector<byte> mm(mlen+L, 0);
		BN_bn2bin(m.ptr(), &mm[L]);
		//memset(mm, 0, L);

		ii = i;
		BNcPtr vals[3] = {w, what[i], ii};
		vector<BNcPtr> input(vals, vals+3);
		D(printf("S: %s %s %02x\n", w.toHexString().c_str(), what[i].toHexString().c_str(), i);)
		mhat[i] = Gxor(input, mm);
		++i;
	}
}

void KO::serverxfer() {
	BigInt &Y = Y_;
	BigInt X = Y.modPow(rsa->d, rsa->n);
	X_. swapWith(X);
}

vector<byte> KO::Gxor(const vector<BNcPtr> &x, const vector<byte> &m) {
	vector<byte> zz;

	int totalsize=0;
	int zlen;
	for (uint i=0; i<x.size(); ++i) {
		zlen = BN_num_bytes(x[i].p);
		if(!zlen) ++zlen;
		totalsize+= zlen;
		//printf("x[%d]/%d total = %d\n", i, zlen, totalsize);
	}

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

	// use RC4 as PRNG
	vector<byte> out(m.size());
	RC4_KEY key;
	RC4_set_key(&key, zz.size(), &zz[0]);
	RC4(&key, m.size(), &m[0], &out[0]);
	return out;
}

