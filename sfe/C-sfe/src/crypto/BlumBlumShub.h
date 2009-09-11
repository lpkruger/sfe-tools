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

	byte next() {
		do {
//			printf("%s\n", X.toString(16).c_str());
//			printf("b:%lu v:%d\n", bits, valid_bits);
			bits <<= loworderbits;
			//bits |= (X.toULong() & lowordermask);
			for (int i=0; i<loworderbits; ++i) {
				bits |= X.testBit(i) ? 1<<i : 0;
			}
			X.modMultiplyThis(X,M);
			valid_bits += loworderbits;
		} while (valid_bits < 8);
		byte ret = bits;
		bits >>= 8;
		valid_bits -= 8;
		return ret;
	}

	void generate(int bits=1024, byte_buf key = byte_buf()) {
		SecureRandom rand;
		BigInt p;
		BigInt q;
		BigInt r = BigInt::toPosBigInt(key);
//		printf("%u %s\n", key.size(), r.toString().c_str());
		key = BigInt::fromPosBigInt(r);
		if (key.size() == 0) {
			do {
				p = BigInt::genPrime(bits);
			} while (p.mod(4) != 3);
			do {
				q = BigInt::genPrime(bits);
			} while (q.mod(4) != 3);
		} else {
			for (uint i=0; i<key.size()*8; ++i) {
				if (r.testBit(i)) {
					p = p.setBit(bits-i);
					q = q.setBit(bits-i-1);
				}
			}
			do {
				p = p.nextProbablePrime();
			} while (p.mod(4) != 3);
			do {
				q = q.nextProbablePrime();
			} while (q.mod(4) != 3);
		}
		M = p*q;
//		fprintf(stderr, "P = %s\n", p.toString().c_str());
//		fprintf(stderr, "Q = %s\n", q.toString().c_str());
//		fprintf(stderr, "M = %s\n", M.toString().c_str());
		X = 2;
		BigInt Y(2);
		Y.powThis(bits);
		X.modPowThis(Y,M);
		bits = M.bitLength();
		loworderbits = 2.0*log(log(bits));
		lowordermask = (1u << loworderbits) - 1;
		bits = valid_bits = 0;

	}
public:
	BlumBlumShub(int bits, const byte_buf &key) {
		generate(bits, key);
	}
	BlumBlumShub(const BigInt &M0, const BigInt &X0) {
		M = M0;
		X = X0;
		bits = M.bitLength();
		loworderbits = 2.0*log(log(bits));
		lowordermask = (1u << loworderbits) - 1;
		bits = valid_bits = 0;
	}
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
