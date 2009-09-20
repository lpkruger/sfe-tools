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
using namespace silly::bigint;

__thread BN_CTX* BigNum_ctx::the_ctx = NULL;

#ifndef USE_OLD_BIGINT
BigNum_ctx silly::bigint::ctx;
#endif

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


#if !defined(USE_OLD_BIGINT)

typedef BigInt (BigInt::*BI_op1)() const;
typedef BigInt (BigInt::*BI_op2)(const BigInt_BN_Base&) const;
typedef BigInt (BigInt::*BI_op2i)(int) const;
typedef BigInt (BigInt::*BI_op2u)(ulong) const;
typedef BigInt (BigInt::*BI_op2l)(long) const;
typedef ulong (BigInt::*BI_uop2u)(ulong) const;
typedef BigInt (BigInt::*BI_op3)
		(const BigInt_BN_Base&,const BigInt_BN_Base&) const;

BI_op2 p_add1 = &BigInt::add;
BI_op2u p_add2 = &BigInt::add;
BI_op2 p_sub1 = &BigInt::subtract;
BI_op2u p_sub2 = &BigInt::subtract;
BI_op2 p_mul1 = &BigInt::multiply;
BI_op2u p_mul2 = &BigInt::multiply;
BI_op2 p_div1 = &BigInt::divide;
BI_op2u p_div2 = &BigInt::divide;
BI_op2 p_exp1 = &BigInt::pow;
BI_op2l p_exp2 = &BigInt::pow;
BI_op2 p_mod1 = &BigInt::mod;
BI_uop2u p_mod2 = &BigInt::mod;
BI_op2 p_modinv = &BigInt::modInverse;

BI_op2i p_shl = &BigInt::shiftLeft;
BI_op2i p_shr = &BigInt::shiftRight;
BI_op2 p_gcd = &BigInt::gcd;
BI_op1 p_neg = &BigInt::negate;

BI_op3 p_modmul = &BigInt::modMultiply;
BI_op3 p_moddiv = &BigInt::modDivide;
BI_op3 p_modexp = &BigInt::modPow;

typedef BigInt& (BigInt::*BI_op1_t)();
typedef BigInt& (BigInt::*BI_op2_t)(const BigInt_BN_Base&);
typedef BigInt& (BigInt::*BI_op2w_t)(BigInt_BN_Base&);
typedef BigInt& (BigInt::*BI_op2i_t)(int);
typedef BigInt& (BigInt::*BI_op2u_t)(ulong);
typedef BigInt& (BigInt::*BI_op2l_t)(long);
typedef BigInt& (BigInt::*BI_op3_t)
		(const BigInt_BN_Base&,const BigInt_BN_Base&);


BI_op2_t p_add1_t = &BigInt::addThis;
BI_op2u_t p_add2_t = &BigInt::addThis;
BI_op2_t p_sub1_t = &BigInt::subtractThis;
BI_op2u_t p_sub2_t = &BigInt::subtractThis;
BI_op2_t p_mul1_t = &BigInt::multiplyThis;
BI_op2u_t p_mul2_t = &BigInt::multiplyThis;
BI_op2_t p_div1_t = &BigInt::divideThis;
BI_op2u_t p_div2_t = &BigInt::divideThis;
BI_op2_t p_exp1_t = &BigInt::powThis;
BI_op2l_t p_exp2_t = &BigInt::powThis;
BI_op2_t p_mod1_t = &BigInt::modThis;
BI_op2_t p_modinv_t = &BigInt::modInverseThis;

BI_op2i_t p_shl_t = &BigInt::shiftLeftThis;
BI_op2i_t p_shr_t = &BigInt::shiftRightThis;
BI_op1_t p_neg_t = &BigInt::negateThis;

BI_op3_t p_modmul_t = &BigInt::modMultiplyThis;
BI_op3_t p_moddiv_t = &BigInt::modDivideThis;
BI_op3_t p_modexp_t = &BigInt::modPowThis;

BI_op2w_t p_swap = &BigInt::swapWith;

#endif
