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
#include "sillytype.h"
#include <openssl/sha.h>
#include <openssl/rand.h>

using namespace shdl;
using namespace silly::io;
using namespace silly::mem;

using std::vector;
class GarbledGate {
public:
	int id;
	int arity;
	vector<int> inputs;
	vector<byte_buf> truthtab;   // randomly permuted set of E(k)(Zn)
};

typedef wise_ptr<GarbledGate> GarbledGate_p;
typedef wise_ptr<byte_buf> byte_buf_p;

class SecretKey {
public:
	virtual byte_buf_p getEncoded() const = 0;
	virtual ~SecretKey() {}
	virtual bool equals(SecretKey* o) const = 0;

	string toHexString();
};

typedef wise_ptr<SecretKey> SecretKey_p;

class SFEKey : public SecretKey {
public:
	byte_buf buf;

	SFEKey() {}
	SFEKey(const byte_buf & buf0) : buf(buf0) {}

	static SecretKey_p bytesToKey(const byte_buf buf, string CIPHER) {
		return SecretKey_p(new SFEKey(buf));
	}

	virtual bool equals(SecretKey* o0) const {
		SFEKey *o = dynamic_cast<SFEKey*>(o0);
		if (!o)
			return false;
		return buf == o->buf;
	}

	void operator= (const byte_buf & buf0) {
		buf = buf0;
	}

	void operator= (byte_buf && buf0) {
		buf = buf0;
	}

	byte_buf_p getEncoded() const {
		return byte_buf_p(new byte_buf(buf));
	}

	static byte_buf_p xxor(const byte_buf_p &a, const byte_buf_p &b) {

		if (a->size() != b->size())
			throw bad_argument("a->size() != b->size()");
		byte_buf_p c = byte_buf_p(new byte_buf(a->size()));
		for (uint i=0; i<c->size(); ++i) {
			(*c)[i] = (*a)[i] ^ (*b)[i];
		}
		return byte_buf_p(c);
	}

	static SecretKey_p xxor(const SecretKey_p &aa, const SecretKey_p &bb, const string &cipher) {
		byte_buf_p a = aa->getEncoded();
		byte_buf_p b = bb->getEncoded();
		byte_buf_p c = xxor(a, b);
		return SecretKey_p(new SFEKey(*c));
	}
};

typedef wise_ptr<SecretKey> SFEKey_p;

class bad_padding : public exception {
	const char* msg;
public:
	bad_padding(const char *msg0) : msg(msg0) {}
	virtual const char *what() {
		return msg;
	}
};
class SFECipher {
public:
	const static int ENCRYPT_MODE=100;
	const static int DECRYPT_MODE=101;
	SHA_CTX ctx;
	SFEKey_p key;
	int mode;
	bool use_padding;

	// TODO
	void init(int mode0, SecretKey_p &key0) {
		SHA1_Init(&ctx);
		use_padding = true;
		mode = mode0;
		key = dynamic_pointer_cast<SFEKey>(key0);
		if (!key.get()) throw bad_argument("key is not SFEKey");
	}
	byte_buf doFinal(const byte_buf &data) {
		switch(mode) {
		case ENCRYPT_MODE:
			mode = -1;
			return use_padding ? encrypt(key, data) : deencrypt(key, data);
		case DECRYPT_MODE:
			mode = -1;
			return use_padding ? decrypt(key, data) : deencrypt(key, data);
		default:
			throw bad_argument("Invalid mode");
		}
	}


private:

	byte_buf md_xkey(const byte_buf &data) {
		byte_buf xkey(20);
		SHA1_Update(&ctx, &data[0], data.size());
		SHA1_Final(&xkey[0], &ctx);
		return xkey;
	}
	// padding-free mode
	byte_buf deencrypt(SFEKey_p &key, const byte_buf &data) {
		byte_buf xkey = md_xkey(*key->getEncoded());
		if (xkey.size() < data.size()) {
			throw bad_argument("data too long");
		}

		byte_buf ret(data.size());
		for (uint i=0; i<ret.size(); ++i) {
			ret[i] = (data[i] ^ xkey[i]);
		}
		return ret;
	}

	byte_buf encrypt(SFEKey_p &key, const byte_buf &data) {
		byte_buf xkey = md_xkey(*key->getEncoded());
		if (xkey.size() - 1 < data.size()) {
			throw bad_argument("data too long");
		}
		byte_buf ret(xkey.size());
		for (uint i=0; i<data.size(); ++i) {
			ret[i] = (data[i] ^ xkey[i]);
		}
		for (uint i=data.size(); i<ret.size() - 1; ++i) {
			ret[i] = xkey[i];
		}
		ret[ret.size() - 1] = (xkey[ret.size() - 1] ^ data.size());
		return ret;
	}

	byte_buf decrypt(SFEKey_p key, const byte_buf &data) { // throws BadPaddingException {
		byte_buf xkey = md_xkey(*key->getEncoded());
		if (xkey.size() != data.size()) {
			throw bad_padding("Invalid data length");
		}
		byte_buf dec(data.size());
		for (uint i=0; i<dec.size(); ++i) {
			dec[i] = (data[i] ^ xkey[i]);
		}
		uint len = dec[dec.size() - 1];
		if (len > dec.size() - 1 || len < 0)
			throw bad_padding("Invalid data range");
		for (uint i=len; i<dec.size()-1; ++i) {
			if (dec[i] != 0)
				throw bad_padding("Invalid data range");
		}
		dec.resize(len);
		//byte_buf ret = new byte[len];
		//System.arraycopy(dec, 0, ret, 0, len);
		//System.out.println("len is " + len + "  xkey is " + xkey.length);
		return dec;
	}

};

class SFEKeyGenerator {
public:
	const static uint default_length = 80;
	uint length;
	SFEKeyGenerator() : length(default_length) {}
	void init(int length0) {
		length = length0;
	}

	uint getLength() {
		return length;
	}

	SecretKey_p generateKey() {
		byte_buf buf(length/8);
		RAND_bytes(&buf[0], buf.size());
		return SecretKey_p(new SFEKey(buf));
	}
};

struct boolean_secrets {
	SFEKey_p s0;
	SFEKey_p s1;

	SFEKey_p operator[](int i) {
		if (i!=0 && i!=1) {
			throw bad_argument("secret must be 0 or 1");
		}
		if (i)
			return s1;
		else
			return s0;
	}
};

class GarbledCircuit {
public:
	boolean use_permute;
	int nInputs;
	vector<int> outputs;
	// TODO: unmake public
	vector<boolean_secrets> outputSecrets;
	vector<GarbledGate_p> allGates;

	GarbledCircuit();
	virtual ~GarbledCircuit();

	void hashCircuit(byte_buf &md);
	void writeCircuit(DataOutput *out);
	static GarbledCircuit readCircuit(DataInput *in);

};



#endif /* GARBLEDCIRCUIT_H_ */
