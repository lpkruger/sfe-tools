/*
 * SFECipher.h
 *
 *  Created on: Sep 1, 2009
 *      Author: louis
 */

#ifndef SFECIPHER_H_
#define SFECIPHER_H_

//#include <gc_cpp.h>
#include <openssl/sha.h>
#include <openssl/rand.h>
#include "Cipher.h"
#include "silly.h"
#include "sillydebug.h"

namespace crypto {
namespace cipher {

typedef CipherKey SFEKey;

inline byte_buf xxor(const byte_buf &a, const byte_buf &b) {
	if (a.size() != b.size()) {
		//print_backtrace();
		throw bad_argument(silly::misc::string_printf("a.size(%d) != b.size(%d)", a.size(), b.size()).c_str());
	}
	byte_buf c(a.size());
	for (uint i=0; i<a.size(); ++i) {
		c[i] = a[i] ^ b[i];
	}
	return silly_move(c);
}

inline SFEKey* xxor(const SecretKey &aa, const SecretKey &bb, const string &cipher) {
	{
		const SFEKey *a = dynamic_cast<const SFEKey*>(&aa);
		const SFEKey *b = dynamic_cast<const SFEKey*>(&bb);
		if (a && b) {
			byte_buf *c = new byte_buf(xxor(*a->buf, *b->buf));
			return new SFEKey(c, true);
		}
	}
	byte_buf a = aa.getEncoded();
	byte_buf b = bb.getEncoded();
	byte_buf *c = new byte_buf(xxor(a, b));
	return new SFEKey(c, true);
}


class SFECipher : public Cipher {
	SHA_CTX ctx;
	int mode;
	bool use_padding;
	const SFEKey *key;
public:

	SFECipher() : use_padding(true) {}

	void setUsePadding(bool p) {
		//print_backtrace(5);
		use_padding = p;
		//printf("setUsePadding: %d\n", p);
	}

	// TODO
	virtual void init(modes mode0, const SecretKey *key0, const AlgorithmParams *params = NULL) {
		SHA1_Init(&ctx);
		mode = mode0;
		key = dynamic_cast<const SFEKey*>(key0);
		if (!key) throw bad_argument("key is not SFEKey");

	}
	virtual uint bytesAvailable() { return 0; }
	virtual byte_buf getOutput() { return byte_buf(); }
	virtual void update(const byte *input, int len) {
		throw CipherException("update not supported for SFECipher, use doFinal");
	}
	virtual byte_buf doFinal(const byte *input, int len) {
		byte_buf in(input, input+len);
		byte_buf out(doFinal(in));
		return silly_move(out);
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
	byte_buf md_xkey(const byte_buf &data);
	byte_buf deencrypt(const SFEKey *key, const byte_buf &data);
	byte_buf encrypt(const SFEKey *key, const byte_buf &data);
	byte_buf decrypt(const SFEKey *key, const byte_buf &data);

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

	SFEKey *generateKey() {
		byte_buf *buf = new byte_buf(length/8);
		RAND_bytes(&(*buf)[0], buf->size());
		return new SFEKey(buf, true);
	}
};
}
}

#endif /* SFECIPHER_H_ */
