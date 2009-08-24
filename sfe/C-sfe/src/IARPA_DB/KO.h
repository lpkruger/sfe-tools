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

typedef BIGNUM* BigInteger;
typedef unsigned char byte;

//class DDB;
#include <vector>
#include "DDB.h"

class KO {
public:
	KO();
	~KO();

	BN_CTX *bn_ctx;

	//SecureRandom rand = new SecureRandom();
	const static int test_sizes[]; //= { 1,2,3, 10, 20, 50, 100, 200, 500, 1000, 2000, 5000, 10000 };
	const static int test_sizes_length;
	const static int L=8;	// security param

	DDB ddb;

	BigInteger rr;
	int n;
	RSA* rsa;
	//// xfer variables
	BigInteger X_;
	BigInteger Y_;
	vector<vector<byte> > mhat;
	vector<BigInteger> what;
	//// server variables

	static BigInteger H(BigInteger x) {
		byte md[SHA_DIGEST_LENGTH];
		byte buf[BN_num_bytes(x)];
		BN_bn2bin(x, buf);
		SHA1(buf, BN_num_bytes(x), md);
		BigInteger hh = BN_new();
		BN_bin2bn(md, SHA_DIGEST_LENGTH, hh);
		return hh;
	}

	static vector<byte> Gxor(vector<BigInteger> &x, vector<byte> &m);
	void clientxfer1(BigInteger w);
	void clientxfer2(BigInteger w);
	void servercommit(DDB & db);
	void serverxfer();

};

#endif /* KO_H_ */
