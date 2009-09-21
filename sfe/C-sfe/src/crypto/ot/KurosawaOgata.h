/*
 * KO.h
 *
 *  Created on: Aug 6, 2009
 *      Author: louis
 */

#ifndef KUROSAWAOGATA_H_
#define KUROSAWAOGATA_H_

#include <openssl/bn.h>
#include <openssl/sha.h>
#include <openssl/rsa.h>
#include "sillytype.h"
#include "sillyio.h"
#include "bigint.h"
using namespace silly::bigint;

#include <vector>

namespace crypto {
namespace ot {
namespace ko {

using namespace silly::io;

BigInt H(BNcPtr x);
byte_buf Gxor(const vector<BNcPtr> &x, const byte_buf &m);

const static int L=8;	// security param

class Sender {
	DataInput *in;
	DataOutput *out;

	RSA* rsa;
	vector<byte_buf> mhat;
public:
	Sender() {}
	~Sender() {
		if (rsa != NULL) {
			RSA_free(rsa);
		}
	}

	void setStreams(DataInput *in0, DataOutput *out0) {
		in = in0;
		out = out0;
	}

	typedef map<BigInt, BigInt> bi_map_t;

	void precompute(const bi_map_t &ddb);
	void online();


private:
	void servercommit(const bi_map_t &);
	BigInt rsa_e() const { return BigInt(rsa->e, true); }
	BigInt rsa_d() const { return BigInt(rsa->d, true); }
	BigInt rsa_n() const { return BigInt(rsa->n, true); }
	BigInt serverxfer(CBigInt &Y);

	friend int test_ko(int argc, char **argv);
};

class Chooser {
	DataInput *in;
	DataOutput *out;

	BigInt rr;
	BigInt rsa_e;
	BigInt rsa_n;

public:
	void setStreams(DataInput *in0, DataOutput *out0) {
		in = in0;
		out = out0;
	}
	byte_buf online(CBigInt &w);

private:
	BigInt clientxfer1(CBigInt &w);
	byte_buf clientxfer2(CBigInt &w, CBigInt &X, const vector<byte_buf> &mhat);

	friend int test_ko(int argc, char **argv);
};

int test_ko(int argc, char **argv);

}
}
}

#endif /* KUROSAWAOGATA_H_ */

