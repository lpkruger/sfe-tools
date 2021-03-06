/*
 * GarbledCircuit.h
 *
 *  Created on: Aug 17, 2009
 *      Author: louis
 */

#ifndef GARBLEDCIRCUIT_H_
#define GARBLEDCIRCUIT_H_

#include <vector>
#include <string>
#include <iostream>
#include <stdexcept>
#include <typeinfo>
#include "shdl.h"
#include "silly.h"
#include "sillyio.h"
#include "sillymem.h"
#include "sillytype.h"

#include "../crypto/cipher/SFECipher.h"

using namespace shdl;
using namespace silly::io;
using namespace silly::mem;

using std::vector;
class GarbledGate : public stringable {
public:
	int id;
	int arity;
	vector<int> inputs;
	vector<byte_buf> truthtab;   // randomly permuted set of E(k)(Zn)

	string toString() {
		ostringstream ostr;
//		ostreamDataOutput out(ostr);
		ostr << "eGate id=" << id << "  arity=" << arity << endl;
		ostr << "  inp: [";
		for (uint i=0; i<inputs.size(); ++i) {
			ostr << " " << inputs[i];
		}
		ostr << " ]" << endl;
		for (uint i=0; i<truthtab.size(); ++i) {
			ostr << "  tt" << i <<": " << toHexString(truthtab[i]) << endl;
		}
		ostr << endl;
		return ostr.str();
	}
};

typedef GarbledGate* GarbledGate_p;
//typedef byte_buf* byte_buf_p;


using crypto::cipher::SecretKey;
using crypto::cipher::SFEKey;
using crypto::cipher::SFECipher;
using crypto::cipher::SFEKeyGenerator;
using crypto::cipher::bad_padding;

typedef wise_ptr<SecretKey> SecretKey_p;
typedef wise_ptr<SFEKey> SFEKey_p;


struct boolean_secrets {
	SFEKey_p s0;
	SFEKey_p s1;

	bool operator==(const boolean_secrets &b) const {
		return s0->equals(b.s0.to_ptr()) &&
		       s1->equals(b.s1.to_ptr());
	}
	bool operator!=(const boolean_secrets &b) const {
		return !operator==(b);
	}
	SFEKey_p& operator[](int i) {
		if (_unlikely(i!=0 && i!=1)) {
			throw bad_argument("secret must be 0 or 1");
		}
		if (i)
			return s1;
		else
			return s0;
	}


	vector<BigInt> toBigIntVector() const {
		vector<BigInt> ret(2);
		ret[0] = BigInt::toPaddedBigInt(s0->getEncoded());
		ret[1] = BigInt::toPaddedBigInt(s1->getEncoded());
		return ret;
	}
#if 0
	void fromBigIntVector(vector<BigInt> &vec) {
		s0 = SFEKey_p(new SFEKey(BigInt::fromPaddedBigInt(vec.at(0))));
		s1 = SFEKey_p(new SFEKey(BigInt::fromPaddedBigInt(vec.at(1))));
	}
#endif
};


inline void writeObject(DataOutput *out, SFEKey_p &key) {
	byte_buf *enc = key->getRawBuffer();
	out->writeByte(enc->size());
	out->write(*enc);
}
inline void readObject(DataInput *in, SFEKey_p &key) {
	int len = in->readByte();
	byte_buf *enc = new byte_buf(len);
	in->readFully(*enc);
	key = SFEKey_p(new SFEKey(enc, true));
}

inline void writeObject(DataOutput *out, boolean_secrets &secr) {
	writeObject(out, secr.s0);
	writeObject(out, secr.s1);
}
inline void readObject(DataInput *in, boolean_secrets &secr) {
	readObject(in, secr.s0);
	readObject(in, secr.s1);
}

struct GarbledCircuit;
typedef wise_ptr<GarbledCircuit> GarbledCircuit_p;

struct GarbledCircuit : public Reclaimer<GarbledGate>{

	boolean use_permute;
	int nInputs;
	vector<int> outputs;
	// TODO: unmake public
	vector<boolean_secrets> outputSecrets;
	vector<GarbledGate_p> allGates;

	GarbledCircuit() { reset(); }

	void reset() {
		use_permute = false;
		nInputs = 0;
		nInputs = 0;
		outputs.clear();
		outputSecrets.clear();
		allGates.clear();
	}

	byte_buf hashCircuit() const;
	void writeCircuit(DataOutput *out) const;
	static GarbledCircuit_p readCircuit(DataInput *in);

};


// some utility functions
string getDefaultDir(const string &f);

void get_circuits(const char* prefix, vector<GarbledCircuit_p> &gcc,
		vector<vector<boolean_secrets> > &inp_secs,	vector<byte_buf> &seeds, uint num, uint max);



#endif /* GARBLEDCIRCUIT_H_ */
