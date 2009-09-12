/*
 * BlumBlumShub.h
 *
 *  Created on: Sep 10, 2009
 *      Author: louis
 */

#ifndef BLUMBLUMSHUB_H_
#define BLUMBLUMSHUB_H_

#include "bigint.h"
#include "SecureRandom.h"
#include <cmath>

namespace crypto {
using namespace bigint;
class BlumBlumShub : public Random {
	BigInt M;
	BigInt X;
	int loworderbits;
	ulong lowordermask;
	ulong bits;
	int valid_bits;

	byte next();
	void generate(int bits=1024, byte_buf key = byte_buf());

public:
	BlumBlumShub(int bits, const byte_buf &key);
	BlumBlumShub(const BigInt &M0, const BigInt &X0);

	virtual void getBytes(byte *out, uint len) {
		while (len>0) {
			*(out++) = next();
			--len;
		}
	}

	void getState(BigInt &Mout, BigInt &Xout) {
		Mout = M;
		Xout = X;
	}
};
}

#endif /* BLUMBLUMSHUB_H_ */
