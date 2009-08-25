/*
 * KO.h
 *
 *  Created on: Aug 6, 2009
 *      Author: louis
 */

#ifndef KO_H_
#define KO_H_

#include <openssl/bn.h>
#include <openssl/sha.h>
#include <openssl/rsa.h>
#include "sillytype.h"

#include <vector>
#include "DDB.h"

class KO {
	//BN_CTX *bn_ctx;
public:
	//KO() { bn_ctx = BN_CTX_new(); }
	//~KO() {	BN_CTX_free(bn_ctx); }

	//SecureRandom rand = new SecureRandom();
	const static int test_sizes[];
	const static int test_sizes_length;
	const static int L=8;	// security param

	DDB ddb;

	BigInt rr;
	int n;
	RSA* rsa;
	//// xfer variables
	BigInt X_;
	BigInt Y_;
	vector<vector<byte> > mhat;
	vector<BigInt> what;
	//// server variables

	static BigInt H(BNcPtr x) {
		byte md[SHA_DIGEST_LENGTH];
		byte buf[BN_num_bytes(x)];
		BN_bn2bin(x, buf);
		SHA1(buf, BN_num_bytes(x), md);
		BigInt hh;
		BN_bin2bn(md, SHA_DIGEST_LENGTH, hh.writePtr());
		return hh;
	}

	static vector<byte> Gxor(const vector<BNcPtr> &x, const vector<byte> &m);
	void clientxfer1(const BigInt &w);
	void clientxfer2(const BigInt &w);
	void servercommit(DDB & db);
	void serverxfer();

};

#endif /* KO_H_ */
