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

using namespace silly::mem;
using namespace bigint;

__thread BN_CTX* BigNum_ctx::the_ctx = NULL;

copy_counter BigInt_BN_Base::counter("BigInt_BN");
count_printer<bigint::BigInt_BN_Base> BigInt_BN_count_printer;

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

