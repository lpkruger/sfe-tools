
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
#include "BigInt.h"

using namespace silly::io;
using namespace silly::misc;
using namespace bigint;

class PinkasNaorOT {
public:
	BigInt QQQ;
	BigInt GGG;
public:
	PinkasNaorOT();
	PinkasNaorOT(BigInt q, BigInt g) : QQQ(q), GGG(g) {}
	virtual ~PinkasNaorOT();

	static BigInt hash(const BigInt &m);
	static BigInt findGenerator(const BigInt &p);
};


typedef vector<BigInt> BigInt_Vect;
typedef vector<BigInt_Vect> BigInt_Mtrx;
typedef vector<BigInt_Mtrx> BigInt_Cube;

class OTSender {
	PinkasNaorOT *ot;
	DataInput *in;
	DataOutput *out;

	BigInt_Mtrx M;

	BigInt_Vect C;
	BigInt_Mtrx PK;
	BigInt_Cube E;
	BigInt_Vect rr;

public:
	OTSender(BigInt_Mtrx &M0, PinkasNaorOT *ot0) : ot(ot0) {
		M.swap(M0);
		for (uint i=0; i<M.size(); ++i) {
			if (M[i].size() != 2) {

				// TODO: throw new RuntimeException("Must have exactly 2 choices");
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

class OTChooser {
	PinkasNaorOT *ot;
	DataInput *in;
	DataOutput *out;

	vector<bool> s; // 0 or 1

	BigInt_Vect k;
	BigInt_Mtrx PK;
public:
	OTChooser(vector<bool> &s0, PinkasNaorOT *ot0) : ot(ot0) {
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

#endif /* PINKASNAOROT_H_ */

