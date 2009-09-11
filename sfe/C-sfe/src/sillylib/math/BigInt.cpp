/*
 * BigInt.cpp
 *
 *  Created on: Aug 12, 2009
 *      Author: louis
 */

#include "BigInt.h"

//const BigInt BigInt::ZERO(0);
//const BigInt BigInt::ONE(1);
//const BigInt BigInt::TWO(2);

__thread BN_CTX* bigint::BigNum_ctx::the_ctx = NULL;

#if 0
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

void *crash_test_dummy = (void*) &test;

#endif

