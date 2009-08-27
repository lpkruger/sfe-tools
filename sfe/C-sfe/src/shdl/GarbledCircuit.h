/*
 * GarbledCircuit.h
 *
 *  Created on: Aug 17, 2009
 *      Author: louis
 */

#ifndef GARBLEDCIRCUIT_H_
#define GARBLEDCIRCUIT_H_

#include <vector>
#include <stdexcept>
#include <typeinfo>
#include "shdl.h"
#include "silly.h"
#include "sillyio.h"
#include "sillymem.h"
#include <openssl/sha.h>

using namespace shdl;
using namespace silly::io;
using namespace silly::mem;

using std::vector;
class GarbledGate {
public:
	int id;
	int arity;
	vector<int> inputs;
	vector<vector<byte> > truthtab;   // randomly permuted set of E(k)(Zn)
};

typedef wise_ptr<GarbledGate> GarbledGate_p;

class SecretKey {
public:
	virtual vector<byte> getEncoded() = 0;
	virtual ~SecretKey() {}
};

typedef wise_ptr<SecretKey> SecretKey_p;

class SFEKey : public SecretKey {
public:
	vector<byte> buf;

	SFEKey() : buf(20) {}
	SFEKey(const vector<byte> & buf0) : buf(buf0) {
		if (buf.size() < 20) buf.resize(20);
		// TODO assert(buf.size() == 20);
	}

#if 1
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


	vector<byte> getEncoded() {
		return buf;
	}

	static SecretKey_p xxor(const SecretKey_p &aa, const SecretKey_p &bb, const string &cipher) {
		SFEKey *a = dynamic_cast<SFEKey*>(aa.get());
		SFEKey *b = dynamic_cast<SFEKey*>(bb.get());
		if (!a) throw std::logic_error("a is not SFEKey");
		if (!b) throw std::logic_error("b is not SFEKey");
		if (a->buf.size() != b->buf.size())
			throw std::logic_error("a->buf.size() != b->buf.size()");
		SFEKey *c = new SFEKey();
		c->buf.resize(a->buf.size());
		for (int i=0; i<c->buf.size(); ++i) {
			c->buf[i] = a->buf[i] ^ b->buf[i];
		}
		return SecretKey_p(c);
	}
};

typedef wise_ptr<SecretKey> SFEKey_p;

class SFECipher {
public:
	const static int ENCRYPT_MODE=100;
	const static int DECRYPT_MODE=101;
	SHA_CTX ctx;
	SFEKey_p key;
	int mode;
	// TODO
	void init(int mode0, SecretKey_p &key0) {
		SHA1_Init(&ctx);
		mode = mode0;
		key = dynamic_pointer_cast<SFEKey>(key0);
		if (!key.get()) throw std::logic_error("key is not SFEKey");

	}
	vector<byte> doFinal(const vector<byte> &data) {
		switch(mode) {
		case ENCRYPT_MODE:
			mode = -1;
			return deencrypt(key, data);
		case DECRYPT_MODE:
			mode = -1;
			return deencrypt(key, data);
//		default:
//			throw new IllegalStateException("Invalid mode: " + mode);
		}
	}

	// padding-free mode
	vector<byte> deencrypt(SFEKey_p &key, const vector<byte> &data) {
		vector<byte> xkey(20);
		SHA1_Update(&ctx, &data[0], data.size());
		SHA1_Final(&xkey[0], &ctx);
//		byte[] xkey = md.digest(key.getEncoded());
//		if (xkey.length < data.length) {
//			throw new IllegalStateException("data too long");
//		}
		vector<byte> ret(data.size());
		for (uint i=0; i<ret.size(); ++i) {
			ret[i] = (byte) (data[i] ^ xkey[i]);
		}
		return ret;
	}

};
class GarbledCircuit {
public:
	boolean use_permute;
	int nInputs;
	vector<int> outputs;
	// TODO: unmake public
	vector<vector<SFEKey_p> > outputSecrets;
	vector<GarbledGate_p> allGates;

	GarbledCircuit();
	virtual ~GarbledCircuit();

	void hashCircuit(vector<byte> &md);
	void writeCircuit(DataOutput &out);
	static GarbledCircuit readCircuit(DataInput &in);

};



#endif /* GARBLEDCIRCUIT_H_ */
