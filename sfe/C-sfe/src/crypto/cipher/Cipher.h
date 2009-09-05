/*
 * Cipher.h
 *
 *  Created on: Sep 1, 2009
 *      Author: louis
 */

#ifndef CIPHER_H_
#define CIPHER_H_

#include <string>
#include <typeinfo>
#include "sillytype.h"

using std::string;

namespace crypto {
namespace cipher {

struct CipherException : public silly::MsgBufferException {
	CipherException(const char* msg0) : MsgBufferException(msg0) {}
};

struct bad_padding : public CipherException {
	bad_padding(const char* msg0) : CipherException(msg0) {}
};

struct SecretKey {
	static copy_counter counter;
	SecretKey() {}
	COPY_COUNTER(SecretKey)
	virtual byte_buf getEncoded() const = 0;
	//virtual getEncodedTo(byte_buf &out) const = 0;
	virtual ~SecretKey() {}
	virtual bool equals(SecretKey* o) const = 0;
	string toHexString();
};

struct AlgorithmParams {
	virtual ~AlgorithmParams() {}
};

class CipherKey : public SecretKey {
	typedef SecretKey super;
public:
	byte_buf *buf;
	bool tofree;
	CipherKey() : buf(NULL), tofree(false) {}
	CipherKey(byte_buf *buf0, bool free0=false) : buf(buf0), tofree(free0) {}
	CipherKey(const byte_buf &buf0, bool free0=false) {
		buf = new byte_buf(buf0);
		tofree = true;
	}
	~CipherKey() {
		if (tofree)
			delete buf;
	}
	CipherKey(const CipherKey& copy) : super(copy), buf(NULL), tofree(false) {
		operator=(copy);
	}
	CipherKey& operator=(const CipherKey& copy) {
		super::operator=(copy);
		if (buf && tofree)
			delete buf;
		if (copy.tofree) {
			buf = new byte_buf(*copy.buf);
			tofree = true;
		} else {
			buf = copy.buf;
			tofree = false;
		}
		return *this;
	}
#if USE_RVALREFS
	CipherKey(CipherKey && move) : super(move) {
		buf = move.buf;
		tofree = move.tofree;
		move.tofree = false;
	}
	CipherKey& operator= (CipherKey && move) {
		super::operator=(move);
		buf = move.buf;
		tofree = move.tofree;
		move.tofree = false;
		return *this;
	}
#endif

	virtual bool equals(SecretKey *o0) const {
		//printf("%s %s\n", typeid(*this).name(), typeid(*o0).name());
		if (typeid(*this) != typeid(*o0))
			return false;

//		CipherKey *o = dynamic_cast<CipherKey*>(o0);
//		if (!o)
//			return false;

		CipherKey *o = static_cast<CipherKey*>(o0);
		return *buf == *o->buf;
	}

	byte_buf getEncoded() const {
		return silly_move(byte_buf(*buf));
	}
	byte_buf *getRawBuffer() const {
		return buf;
	}
};





class Cipher {
protected:
	Cipher() {}
public:
	enum modes {DECRYPT_MODE,ENCRYPT_MODE,
		PRIVATE_KEY,PUBLIC_KEY,SECRET_KEY,
		UNWRAP_MODE, WRAP_MODE};

	static Cipher *getInstance(string cipher);

	virtual void init(modes mode, const SecretKey *sk, const AlgorithmParams *params=NULL) = 0;
	virtual void update(const byte *input, int len) = 0;
	virtual uint bytesAvailable() = 0;
	virtual byte_buf getOutput() = 0;
	virtual byte_buf doFinal(const byte *input, int len) = 0;

	virtual ~Cipher() {}
};

}
}

#endif /* CIPHER_H_ */
