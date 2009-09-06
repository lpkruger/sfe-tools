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
	virtual void getBytes(byte *out, uint len) = 0;
	virtual void getBytes(byte_buf &buf, uint off=0, uint len=0) = 0;
	virtual ~Random() {}
};

struct SecureRandom : public Random {
	SecureRandom() {}
	SecureRandom(silly::types::byte_buf &key) {}

	// get len random bytes
	void getBytes(byte *out, uint len);

	// fill the buffer with random bytes
	void getBytes(byte_buf &buf, uint off=0, uint len=0) {
		if (len==0)
			len = buf.size()-off;
		if (off+len > buf.size())
			buf.resize(off+len);
		getBytes(&buf[off], len);
	}
};

}

#endif /* SECURERANDOM_H_ */
