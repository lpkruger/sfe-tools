/*
 * GarbledCircuit.h
 *
 *  Created on: Aug 17, 2009
 *      Author: louis
 */

#ifndef GARBLEDCIRCUIT_H_
#define GARBLEDCIRCUIT_H_

#include <vector>

#include "shdl.h"
#include "../sillylib/silly.h"
#include "../sillylib/sillyio.h"
#include <openssl/sha.h>

using namespace shdl;
using namespace silly;

using std::vector;
class GarbledGate {
public:
	int id;
	int arity;
	vector<int> inputs;
	vector<vector<byte> > truthtab;   // randomly permuted set of E(k)(Zn)
};

class SecretKey {
};

class SFEKey : public SecretKey {
public:
	vector<byte> buf;

	SFEKey() : buf(20) {}
	SFEKey(const vector<byte> & buf0) : buf(buf0) {
		if (buf.size() < 20) buf.resize(20);
		// TODO assert(buf.size() == 20);
	}


#if 0
#if NO_RVALREF
	void operator= (const vector<byte> & buf0) {
		buf = buf0;
		if (buf.size() < 20) buf.resize(20);
	}
#else
	void operator= (vector<byte> && buf0) {
		buf = buf0;
		if (buf.size() < 20) buf.resize(20);
	}
#endif
#endif

	vector<byte> getEncoded() {
		return buf;
	}
};

class GarbledCircuit {
public:
	boolean use_permute;
	int nInputs;
	vector<int> outputs;
	// TODO: unmake public
	vector<vector<SFEKey> > outputSecrets;
	vector<GarbledGate> allGates;

	GarbledCircuit();
	virtual ~GarbledCircuit();

	void hashCircuit(vector<byte> &md);
	void writeCircuit(DataOutput &out);
	static GarbledCircuit readCircuit(DataInput &in);

};



#endif /* GARBLEDCIRCUIT_H_ */
