/*
 * BigInt.cpp
 *
 *  Created on: Aug 12, 2009
 *      Author: louis
 */

#include "BigInt.h"

namespace bigint {

//const BigInt BigInt::ZERO(0);
//const BigInt BigInt::ONE(1);
//const BigInt BigInt::TWO(2);

void BigInt::setDefaultBnCtx() {
	static BN_CTX *default_bn_ctx = BN_CTX_new();
	bn_ctx = default_bn_ctx;
}

static BigInt test() {
	BigInt a,b;
	a.add(b);
	b+=1;
	a+b;
	a-b;
	a*b;
	a+(short)3;
	return a*3u;
}

}
