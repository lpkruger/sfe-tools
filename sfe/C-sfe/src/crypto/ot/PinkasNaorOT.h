
/*
 * PinkasNaorOT.h
 *
 *  Created on: Aug 22, 2009
 *      Author: louis
 */

#ifndef PINKASNAOROT_H_
#define PINKASNAOROT_H_

#include "silly.h"
#include "sillyio.h"
#include "sillytype.h"
#include <openssl/bn.h>
#include "bigint.h"

using namespace silly::io;
using namespace silly::misc;
using namespace bigint;

namespace crypto {
namespace ot {
namespace pinkasnaor {

class OT {
public:
	BigInt QQQ;
	BigInt GGG;
public:
	OT();
	OT(CBigInt &q, CBigInt &g) : QQQ(q), GGG(g) {}
	virtual ~OT() {}

	static BigInt hash(const BigInt &m);
	static BigInt findGenerator(const BigInt &p);
};

class Sender {
	OT *ot;
	DataInput *in;
	DataOutput *out;

	BigInt_Mtrx M;

	BigInt_Vect C;
	BigInt_Mtrx PK;
	BigInt_Cube E;
	BigInt_Vect rr;

public:
	Sender(BigInt_Mtrx &M0, OT *ot0) : ot(ot0) {
		M.swap(M0);
		for (uint i=0; i<M.size(); ++i) {
			if (M[i].size() != 2) {
				throw bad_argument("Must have exactly 2 choices");
			}
		}
	}

	void setStreams(DataInput *in0, DataOutput *out0) {
		in = in0;
		out = out0;
	}

	void go();
	void precalc();
	void online();
};

class Chooser {
	OT *ot;
	DataInput *in;
	DataOutput *out;

	bit_vector s; // 0 or 1

	BigInt_Vect k;
	BigInt_Mtrx PK;
public:
	Chooser(bit_vector &s0, OT *ot0) : ot(ot0) {
		s.swap(s0);
	}

	void setStreams(DataInput *in0, DataOutput *out0) {
		in = in0;
		out = out0;
	}

	BigInt_Vect go();
	void precalc();
	BigInt_Vect online();
};

}
}
}

#endif /* PINKASNAOROT_H_ */

