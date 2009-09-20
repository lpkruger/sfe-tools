/*
 * BigInt.h
 *
 *  Created on: Aug 12, 2009
 *      Author: louis
 */


// don't include this file.
// Include bigint.h instead

#ifndef BIGINT_BN_IMPL_H_
#define BIGINT_BN_IMPL_H_

#include <vector>
#include <string>
#include <algorithm>
#include <openssl/bn.h>
#include <openssl/rsa.h>
#include "sillytype.h"

//#include "BigInt.h"

//using std::vector;
//using std::string;

#include "BigInt_BN_base.h"

namespace silly {
namespace bigint {

//#define BI_VIRTUAL //virtual
//#define BI_BASE : public BigIntBase

//#define RETURN_RET	return silly_move(ret)
#define RETURN_RET	return ret

class BigInt : public BigInt_BN_Base {
	typedef BigInt_BN_Base super;
public:

	////////////////////////////////////////////
	BigInt() : super() {}
	BigInt(ulong nn) : super(nn) {}
	BigInt(long nn) : super(nn) {}
	BigInt(uint nn) : super(nn) {}
	BigInt(int nn) : super(nn) {}
	BigInt(const BIGNUM *nn, int) : super(nn, false) {}
	BigInt(const BIGNUM *nn, bool) : super(nn, false) {}
	//////// copy constructor: do not delete
	BigInt(const BigInt_BN_Base &b) : super(b) {}
	////////
#if USE_RVALREFS
	BigInt(BigInt_BN_Base &&b) : super(b) {}
	BigInt& operator= (BigInt_BN_Base &&b) {
		super::operator=(b); return *this;
	}
#endif
	//////// copy assignment: do not delete
	BigInt& operator= (const BigInt_BN_Base &b) {
		super::operator=(b); return *this;
	}
	////////
	BigInt& operator= (const BIGNUM* b) {
		super::operator=(b); return *this;
	}
	BigInt& operator= (ulong nn) {
		super::operator=(nn); return *this;
	}
	BigInt& operator= (long nn) {
		super::operator=(nn); return *this;
	}
	BigInt& operator= (uint nn) {
		super::operator=(nn); return *this;
	}
	BigInt& operator= (int nn) {
		super::operator=(nn); return *this;
	}
	////////////////////////////////////////////

	BigInt mod(BNcPtr m) const {
		BigInt ret;
		BN_mod(ret, *this, m, bn_ctx);
		RETURN_RET;
	}
	ulong mod(ulong m) const {
		return BN_mod_word(*this, m);
	}
	BigInt modInverse(BNcPtr m) const {
		BigInt ret;
		BN_mod_inverse(ret, *this, m, bn_ctx);
		RETURN_RET;
	}
	BigInt modMultiply(BNcPtr b, BNcPtr m) const {
		BigInt ret;
		BN_mod_mul(ret, *this, b, m, bn_ctx);
		RETURN_RET;
	}
	BigInt modDivide(BNcPtr b, BNcPtr m) const {
		BigInt ret;
		BN_mod_inverse(ret, b, m, bn_ctx);
		// TODO check return for NULL
		BN_mod_mul(ret, *this, ret, m, bn_ctx);
		RETURN_RET;
	}
	BigInt modPow(BNcPtr b, BNcPtr m) const {
		BigInt ret;
		BN_mod_exp(ret, *this, b, m, bn_ctx);
		RETURN_RET;
	}

	BigInt add(BNcPtr b) const {
		BigInt ret;
		BN_add(ret, *this, b);
		RETURN_RET;
	}
	BigInt add(ulong b) const {
		BigInt ret(*this);
		BN_add_word(ret, b);
		RETURN_RET;
	}
	BigInt subtract(BNcPtr b) const {
		BigInt ret;
		BN_sub(ret, *this, b);
		RETURN_RET;
	}
	BigInt subtract(ulong b) const {
		BigInt ret(*this);
		BN_sub_word(ret, b);
		RETURN_RET;
	}
	BigInt multiply(BNcPtr b) const {
		BigInt ret;
		BN_mul(ret, *this, b, bn_ctx);
		RETURN_RET;
	}
	BigInt multiply(ulong b) const {
		BigInt ret(*this);
		BN_mul_word(ret, b);
		RETURN_RET;
	}
	BigInt pow(BNcPtr b) const {
		BigInt ret;
		BN_exp(ret, *this, b, bn_ctx);
		RETURN_RET;
	}
	BigInt pow(ulong b) const {
		return pow(BigInt(b));
	}

	BigInt divide(BNcPtr b) const {
		BigInt ret;
		BN_div(ret, NULL, *this, b, bn_ctx);
		RETURN_RET;
	}
	BigInt divide(ulong b) const {
		BigInt ret(*this);
		BN_div_word(ret, b);
		RETURN_RET;
	}


	BigInt gcd(BNcPtr b) const {
		BigInt ret;
		BN_gcd(ret, *this, b, bn_ctx);
		RETURN_RET;
	}

	BigInt negate() const {
		BigInt ret(*this);
		BN_set_negative(ret, !ret.isNegative());
		RETURN_RET;
	}

	BigInt shiftLeft(int n) const {
		BigInt ret;
		BN_lshift(ret, *this, n);
		RETURN_RET;
	}
	BigInt shiftRight(int n) const {
		BigInt ret;
		BN_rshift(ret, *this, n);
		RETURN_RET;
	}


	bool equals(BNcPtr o) const {
		if (to_ptr() == o)
			return true;
		return BN_cmp(*this, o) == 0;
	}
	bool equals(ulong n) const {
		if (n == ulong(-1L)) {
			return equals(BigInt(n));
		}
		if (BN_get_word(*this) == n)
			return true;
		return false;
	}

	BIGNUM* to_writePtr() {
		return *this;
	}
	const BIGNUM* to_ptr() const {
		return *this;
	}


	// multilating operations
	BigInt& swapWith(BigInt &b) {
		_swap(b);
		return *this;
	}
	BigInt& addThis(BNcPtr b) {
		BN_add(*this, *this, b);
		return *this;
	}
	BigInt& addThis(ulong n) {
		BN_add_word(*this, n);
		return *this;
	}
	BigInt& addThis(long n) {
		if (n>=0) {
			BN_add_word(*this, (ulong) n);
		} else {
			BN_sub_word(*this, (ulong) -n);
		}
		return *this;
	}
	BigInt& subtractThis(BNcPtr b) {
		BN_sub(*this, *this, b);
		return *this;
	}
	BigInt& subtractThis(ulong n) {
		BN_sub_word(*this, n);
		return *this;
	}
	BigInt& subtractThis(long n) {
		if (n>=0) {
			BN_sub_word(*this, (ulong) n);
		} else {
			BN_add_word(*this, (ulong) -n);
		}
		return *this;
	}

	BigInt& multiplyThis(BNcPtr b) {
		BN_mul(*this, *this, b, bn_ctx);
		return *this;
	}
	BigInt& multiplyThis(ulong nn) {
		BN_mul_word(*this, nn);
		return *this;
	}
	BigInt& multiplyThis(long nn) {
		if (nn>=0) {
			BN_mul_word(*this, (ulong) nn);
		} else {
			BN_mul_word(*this, (ulong) -nn);
			negateThis();
		}
		return *this;
	}
	BigInt& divideThis(BNcPtr b) {
		BN_div(*this, NULL, *this, b, bn_ctx);
		return *this;
	}
	BigInt& divideThis(ulong nn) {
		BN_div_word(*this, nn);
		return *this;
	}
	BigInt& modThis(BNcPtr m) {
		BN_mod(*this, *this, m, bn_ctx);
		return *this;
	}
	ulong modThis(ulong m) {
		ulong rem = BN_mod_word(*this, m);
		*this = rem;
		return rem;
	}
	BigInt& negateThis() {
		BN_set_negative(*this, !BN_is_negative(to_ptr()));
		return *this;
	}
	BigInt& powThis(BNcPtr b) {
		BN_exp(*this, *this, b, bn_ctx);
		return *this;
	}
	BigInt& powThis(ulong b) {
		return powThis(BigInt(b));
	}
	BigInt& modMultiplyThis(BNcPtr b, BNcPtr m) {
		BN_mod_mul(*this, *this, b, m, bn_ctx);
		return *this;
	}
	BigInt& modInverseThis(BNcPtr m) {
		BN_mod_inverse(*this, *this, m, bn_ctx);
		return *this;
	}

	BigInt& modDivideThis(BNcPtr b, BNcPtr m) {
		BigInt r;
		BN_mod_inverse(r, b, m, bn_ctx);
		// TODO check return for NULL
		BN_mod_mul(*this, *this, r, m, bn_ctx);
		return *this;
	}

	BigInt& modPowThis(BNcPtr b, BNcPtr m) {
		BN_mod_exp(*this, *this, b, m, bn_ctx);
		return *this;
	}
	BigInt& shiftLeftThis(int n) {
		BN_lshift(*this, *this, n);
		return *this;
	}
	BigInt& shiftRightThis(int n) {
		BigInt ret;
		BN_rshift(*this, *this, n);
		return *this;
	}

	ulong toULong() {
		return BN_get_word(*this);
	}
	unsigned long long toULLong() {
		const int len = sizeof(unsigned long long);
		if (sizeof(ulong) == len)
			return toULong();
		if (byteLength() > len)
			return (unsigned long long)(long long) -1;
		byte_buf buf = fromPosBigInt(*this, len);
		uint offset = buf.size() - len;
		unsigned long long r = 0;
		for (uint i=offset; i<buf.size(); ++i) {
			r <<= 8;
			r |= buf[i];
		}
		return r;
	}
	long toLong() {
		const long minlong = long((ulong(-1L)>>1)+1);
		const long maxlong = long(ulong(-1L)>>1);
		ulong n = toULong();
		long r = long(n);
		if (r<0)
			return isNegative() ? minlong : maxlong;
		else
			return isNegative() ? -r : r;
	}
	long long toLLong() {
		const long long minllong = (long long)(((unsigned long long)(-1LL)>>1)+1);
		const long long maxllong = (long long)(((unsigned long long)(-1LL))>>1);
		const int len = sizeof(long long);
		if (sizeof(long) == len)
			return toLong();
		unsigned long long n = toULLong();
		long long r = (long long)(n);
		if (r<0)
			return isNegative() ? minllong : maxllong;
		else
			return isNegative() ? -r : r;
	}



	bool operator< (BNcPtr b) const {
		return BN_cmp(*this, b)<0;
	}
	bool operator<= (BNcPtr b) const {
		return BN_cmp(*this, b)<=0;
	}
	bool operator> (BNcPtr b) const {
		return BN_cmp(*this, b)>0;
	}
	bool operator>= (BNcPtr b) const {
		return BN_cmp(*this, b)>=0;
	}
	bool operator== (BNcPtr b) const {
		return BN_cmp(*this, b)==0;
	}
	bool operator!= (BNcPtr b) const {
		return BN_cmp(*this, b)!=0;
	}

#define me (*this)
private:
	static BIGNUM* ptr(BigInt &n) {
		return n.to_writePtr();
	}
	static const BIGNUM* cptr(const BigInt &n) {
		return n.to_ptr();
	}
	static const BIGNUM* cptr(BNcPtr &n) {
		return n.ptr();
	}
public:
#include "BigInt_BN_utils.h"
#undef cptr
#undef ptr
#undef me

//#define CBI const BigInt&
//#define OP0m(op, fn)    BigInt& operator op ()          { return fn(); }
//#define OP0c(op, fn)    BigInt  operator op ()    const { return fn(); }
//#define OP1m(op, fn, T) BigInt& operator op (T b)       { return fn(b); }
//#define OP1c(op, fn, T) BigInt  operator op (T b) const { return fn(b); }
//#define OP1u(op, fn, T) T       operator op (T b)     { return (b); }
//template<class T> IN BigInt operator+ (const BigInt& a, T b) { return a.add(b); }

};


typedef const BigInt CBigInt;


#define CBI CBigInt&
#define IN static inline
#define OP1m(op, fn, T) IN BigInt& operator op (BigInt& a, T b) { return a.fn(b); }
#define OP1c(op, fn, T) IN BigInt  operator op (CBI a, T b)     { return a.fn(b); }
#define OP1u(op, fn, T) IN T       operator op (BigInt& a, T b) { return a.fn(b); }
//#define OP0m(op, fn) IN BigInt& operator op (BigInt& a)        { return a.fn(); }
#define OP0c(op, fn) IN BigInt  operator op (CBI a)            { return a.fn(); }

OP1c(+, add, ulong)
OP1c(+, add, long)
OP1c(+, add, uint)
OP1c(+, add, int)
OP1c(-, subtract, ulong)
OP1c(-, subtract, long)
OP1c(-, subtract, uint)
OP1c(-, subtract, int)
OP1c(*, multiply, ulong)
OP1c(*, multiply, long)
OP1c(*, multiply, uint)
OP1c(*, multiply, int)
OP1c(/, divide, ulong)
OP1u(%, mod, ulong)
OP1c(<<, shiftLeft, int)
OP1c(>>, shiftRight, int)

OP1m(+=, addThis, ulong)
OP1m(-=, subtractThis, ulong)
OP1m(*=, multiplyThis, ulong)
OP1m(/=, divideThis, ulong)
OP1u(%=, modThis, ulong)
OP1m(<<=, shiftLeftThis, int)
OP1m(>>=, shiftRightThis, int)

OP0c(-, negate)
OP1c(+, add, CBI)
OP1c(-, subtract, CBI)
OP1c(*, multiply, CBI)
OP1c(/, divide, CBI)
OP1c(%, mod, CBI)
OP1m(+=, addThis, CBI)
OP1m(-=, subtractThis, CBI)
OP1m(*=, multiplyThis, CBI)
OP1m(%=, modThis, CBI)

#undef CBI
#undef IN
#undef OP1m
#undef OP1c
#undef OP1u
#undef OP0c

#undef RETURN_RET

}
}

#endif /* BIGINT_H_ */
