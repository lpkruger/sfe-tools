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
#include "sillyio.h"

#include <vector>
#include "DDB.h"

namespace iarpa {
namespace ko {

using namespace silly;

BigInt H(BNcPtr x);
vector<byte> Gxor(const vector<BNcPtr> &x, const vector<byte> &m);

const static int L=8;	// security param

class Server {
	DataInput *in;
	DataOutput *out;

	RSA* rsa;
	vector<vector<byte> > mhat;
public:
	Server() {}
	~Server() {
		if (rsa != NULL) {
			RSA_free(rsa);
		}
	}

	void setStreams(DataInput *in0, DataOutput *out0) {
		in = in0;
		out = out0;
	}
	void precompute(DDB &ddb);
	void online();


private:
	void servercommit(DDB &);
	BNcPtr rsa_e() const { return rsa->e; }
	BNcPtr rsa_n() const { return rsa->n; }
	BigInt serverxfer(CBigInt &Y);

	friend int iarpa::ko::test_ko(int argc, char **argv);
};

class Client {
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
	void online(CBigInt &w);

private:
	BigInt clientxfer1(CBigInt &w);
	void clientxfer2(CBigInt &w, CBigInt &X, const vector<vector<byte> > &mhat);

	friend int iarpa::ko::test_ko(int argc, char **argv);
};

int test_ko(int argc, char **argv);

}
}

#endif /* KO_H_ */

