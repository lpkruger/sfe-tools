/*
 * Random.h
 *
 *  Created on: Sep 3, 2009
 *      Author: louis
 */

#ifndef SECURERANDOM_H_
#define SECURERANDOM_H_

#include "sillytype.h"

namespace crypto {

struct Random {
	typedef unsigned char byte;
	typedef unsigned int uint;
	typedef unsigned long ulong;
	typedef unsigned long long ullong;

	virtual void getBytes(byte *out, uint len) = 0;

	// fill the buffer with random bytes
	void getBytes(byte_buf &buf, uint off=0, uint len=0) {
		if (len==0)
			len = buf.size()-off;
		if (off+len > buf.size())
			buf.resize(off+len);
		getBytes(&buf[off], len);
	}
	byte getByte() {
		byte b;
		getBytes(&b, sizeof(b));
		return b;
	}
	uint getInt(uint m=0) {
		uint n;
		getBytes((byte*)&n, sizeof(n));
		if (m)
			n %= m;
		return n;
	}
	ulong getLong(ulong m=0) {
		ulong n;
		getBytes((byte*)&n, sizeof(n));
		if (m)
			n %= m;
		return n;
	}
	ullong getLLong(ullong m=0) {
		ullong n;
		getBytes((byte*)&n, sizeof(n));
		if (m)
			n %= m;
		return n;
	}
	float getFloat() {
		return getLong() / float(ulong(-1L));
	}

	double getDouble() {
		return getLong() / double(ulong(-1L));
	}
	virtual ~Random() {}
};

struct SecureRandom : public Random {
	SecureRandom() {}
	SecureRandom(silly::types::byte_buf &key) {}

	// get len random bytes
	void getBytes(byte *out, uint len);
	void getBytes(byte_buf &buf, uint off=0, uint len=0) {
		Random::getBytes(buf, off, len);
	}


};

}

#endif /* SECURERANDOM_H_ */
