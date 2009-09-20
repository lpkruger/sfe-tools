/*
 * HKOT.h
 *
 *  Created on: Sep 20, 2009
 *      Author: louis
 */

/*
 * KO.h
 *
 *  Created on: Aug 6, 2009
 *      Author: louis
 */

#ifndef KO_H_
#define KO_H_

#if 0

#include "bigint.h"
#include "sillytype.h"
#include "sillyio.h"

#include <vector>

namespace iarpa {
namespace hkot {

using namespace silly::io;
using namespace silly::bigint;

static inline BigInt H(const BigInt &x) {
	return bigint::
}
byte_buf Gxor(const vector<BNcPtr> &x, const byte_buf &m);

const static int L=8;	// security param

class Server {
	DataInput *in;
	DataOutput *out;

	RSA* rsa;
	vector<byte_buf> mhat;
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
	BigInt rsa_e() const { return BigInt(rsa->e, true); }
	BigInt rsa_d() const { return BigInt(rsa->d, true); }
	BigInt rsa_n() const { return BigInt(rsa->n, true); }
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
	byte_buf online(CBigInt &w);

private:
	BigInt clientxfer1(CBigInt &w);
	byte_buf clientxfer2(CBigInt &w, CBigInt &X, const vector<byte_buf> &mhat);

	friend int iarpa::ko::test_ko(int argc, char **argv);
};

int test_ko(int argc, char **argv);

}
}


#endif
#endif /* HKOT_H_ */
