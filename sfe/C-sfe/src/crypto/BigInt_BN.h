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

using std::vector;
using std::string;

#include "BigInt_BN_base.h"

namespace bigint {

//#define BI_VIRTUAL //virtual
//#define BI_BASE : public BigIntBase

class BigInt : public BigInt_BN_Base {
	typedef BigInt_BN_Base super;
public:

	////////////////////////////////////////////
	BigInt(ulong nn=0, BN_CTX *ctx = NULL) : super(nn,ctx) {}
	BigInt(long nn, BN_CTX *ctx = NULL) : super(nn,ctx) {}
	BigInt(uint nn, BN_CTX *ctx = NULL) : super(nn,ctx) {}
	BigInt(int nn, BN_CTX *ctx = NULL) : super(nn,ctx) {}
	BigInt(const BIGNUM *nn, BN_CTX *ctx = NULL) : super(nn,ctx) {}
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
		BigInt r;
		BN_mod(r, *this, m, bn_ctx);
		return r;
	}
	ulong mod(ulong m) const {
		return BN_mod_word(*this, m);
	}
	BigInt modInverse(BNcPtr m) const {
		BigInt r;
		BN_mod_inverse(r, *this, m, bn_ctx);
		return r;
	}
	BigInt modMultiply(BNcPtr b, BNcPtr m) const {
		BigInt r;
		BN_mod_mul(r, *this, b, m, bn_ctx);
		return r;
	}
	BigInt modDivide(BNcPtr b, BNcPtr m) const {
		BigInt r;
		BN_mod_inverse(r, b, m, bn_ctx);
		// TODO check return for NULL
		BN_mod_mul(r, *this, r, m, bn_ctx);
		return r;
	}
	BigInt modPow(BNcPtr b, BNcPtr m) const {
		BigInt r;
		BN_mod_exp(r, *this, b, m, bn_ctx);
		return r;
	}

	BigInt add(BNcPtr b) const {
		BigInt r;
		BN_add(r, *this, b);
		return r;
	}
	BigInt add(ulong b) const {
		BigInt r(*this);
		BN_add_word(r, b);
		return r;
	}
	BigInt subtract(BNcPtr b) const {
		BigInt r;
		BN_sub(r, *this, b);
		return r;
	}
	BigInt subtract(ulong b) const {
		BigInt r(*this);
		BN_sub_word(r, b);
		return r;
	}
	BigInt multiply(BNcPtr b) const {
		BigInt r;
		BN_mul(r, *this, b, bn_ctx);
		return r;
	}
	BigInt multiply(ulong b) const {
		BigInt r(*this);
		BN_mul_word(r, b);
		return r;
	}
	BigInt pow(BNcPtr b) const {
		BigInt r;
		BN_exp(r, *this, b, bn_ctx);
		return r;
	}
	BigInt pow(ulong b) const {
		return pow(BigInt(b));
	}

	BigInt divide(BNcPtr b) const {
		BigInt r;
		BN_div(r, NULL, *this, b, bn_ctx);
		return r;
	}
	BigInt divide(ulong b) const {
		BigInt r(*this);
		BN_div_word(r, b);
		return r;
	}

	BigInt nextProbablePrime() const {
		BigInt r(*this);
		do {
			BN_add_word(r, 1);
		} while (!BN_is_prime(r, 128, NULL, bn_ctx, NULL));
		return r;
	}

	static BigInt random(BNcPtr max) {
		BigInt r;
		BN_rand_range(r, (BIGNUM*) max.p);
		return r;
	}
	static BigInt random(int bits, int top=-1, bool oddnum=false) {
		// top: -1 for any number, 0 for top-bit 1, 1 for top-bits 11
		BigInt r;
		BN_rand(r, bits, top, oddnum);
		return r;
	}

	BigInt negate() const {
		BigInt r(*this);
		BN_set_negative(r, !r.isNegative());
		return r;
	}
	bool isNegative() const {
		return BN_is_negative((const BIGNUM*)*this);
	}
	int byteLength() const {
		return BN_num_bytes(*this);
	}
	int bitLength() const {
		return BN_num_bits(*this);
	}

	BigInt xxor(const BigInt &b) const {
		byte_buf aa = fromPosBigInt(*this);
		byte_buf bb = fromPosBigInt(b);

		int asize = aa.size();
		int bsize = bb.size();
		if (asize < bsize) {
			aa.swap(bb);
			bsize = asize;
			asize = aa.size();
		}
		for (int i=1; i<=bsize; ++i) {
			aa[asize-i] ^= bb[bsize-i];
		}
		BigInt ret;
		BN_bin2bn(&aa[0], aa.size(), ret);
		BN_set_negative(ret, isNegative() ^ b.isNegative());
		return ret;

	}

	BigInt shiftLeft(int n) const {
		BigInt ret;
		BN_lshift(ret, *this, n);
		return ret;
	}
	BigInt shiftRight(int n) const {
		BigInt ret;
		BN_rshift(ret, *this, n);
		return ret;
	}

	bool testBit(int n) const {
		return BN_is_bit_set(*this, n);
	}
	BigInt setBit(int n) const {
		BigInt ret(*this);
		BN_set_bit(ret, n);
		return ret;
	}
	BigInt clearBit(int n) const {
		BigInt ret(*this);
		if (ret.testBit(n)) {
			BN_clear_bit(ret, n);
		}
		return ret;
	}

	bool equals(BNcPtr o) const {
		if (ptr() == o.p)
			return true;
		return BN_cmp(*this, o) == 0;
	}

	BIGNUM* writePtr() {
		return *this;
	}
	const BIGNUM* ptr() const {
		return *this;
	}

	static BigInt genPrime(int bits) {
		BigInt ret;
		BIGNUM *n =	BN_generate_prime(ret, 129, false, NULL, NULL, NULL, NULL);
		if (!n)
			throw math_exception("error generating prime number");
		return ret;
	}
	string toHexString() const {
		char *buf = BN_bn2hex(*this);
		string str(buf);
		OPENSSL_free(buf);
		return str;
	}
	string toString() const {
		char *buf = BN_bn2dec(*this);
		string str(buf);
		OPENSSL_free(buf);
		return str;
	}

	// multilating operations
	BigInt& swapWith(BigInt &b) {
		_swap(b);
		return *this;
	}
	BigInt& negateThis() {
		BN_set_negative(*this, !isNegative());
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
		unsigned long long ret = 0;
		for (uint i=offset; i<buf.size(); ++i) {
			ret <<= 8;
			ret |= buf[i];
		}
		return ret;
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

	static byte_buf fromPosBigInt(BNcPtr num, int len=0) {
		int reallen = BN_num_bytes(num);
		if (len<reallen) len = reallen;
		if (!len) return byte_buf(1, 0);
		byte_buf ret(len);
		int offset = len-reallen;
		BN_bn2bin(num, &ret[offset]);
		return ret;
	}

	static BigInt toPosBigInt(const byte_buf &buf) {
		BigInt ret;
		BN_bin2bn(&buf[0], buf.size(), (BIGNUM*) ret.ptr());
		return ret;
	}
	static byte_buf from2sCompBigInt(BNcPtr num, int len=0) {
		int reallen = BN_num_bytes(num);
		if (!reallen)
			++reallen;
		if (BN_is_bit_set(num, reallen*8-1)) {
			if (!BN_is_negative(num))
				++reallen;		// need an extra byte if high bit is set
			else {
				BigInt tmp(num);
				BN_clear_bit(tmp, reallen*8-1);
				if (!BN_is_zero(tmp.ptr()))
					++reallen;
			}
		}
		if (len<reallen)
				len = reallen;
		if (!BN_is_negative(num)) {
			return fromPosBigInt(num, len);
		}
		BigInt n2(1);
		n2.shiftLeftThis(len*8);
		n2.addThis(num);
		byte_buf ret = fromPosBigInt(n2, len);
		return ret;
	}

	static BigInt to2sCompBigInt(const byte_buf &buf) {
		BigInt num = toPosBigInt(buf);
		int len = num.byteLength();
		if (!num.testBit(len*8-1))
			return num;

		// it's negative
		BigInt n2(1);
		n2.shiftLeftThis(len*8);
		n2.subtractThis(num);
		n2.negateThis();
		return n2;
	}

	static byte_buf MPIfromBigInt(BNcPtr num) {
		byte_buf ret(BN_bn2mpi(num, NULL));
		BN_bn2mpi(num, &ret[0]);
		return ret;
	}

	static BigInt MPItoBigInt(const byte_buf &buf) {
		BigInt ret;
		BN_mpi2bn(&buf[0], buf.size(), ret);
		return ret;
	}

	static BigInt toPaddedBigInt(byte_buf buf) {
		buf.insert(buf.begin(), 1);
		return toPosBigInt(buf);
	}
	static byte_buf fromPaddedBigInt(BNcPtr num) {
		byte_buf ret = fromPosBigInt(num);
		if (ret[0] != 1)
			throw math_exception("bignum not padded");
		ret.erase(ret.begin());
		return ret;
	}

	string toString(uint base=10) {
		if (base<2 || base>36)
			throw math_exception("toString only supports bases from 2 to 36");
		if (BN_is_zero(ptr()))
			return "0";
		BigInt copy(*this);

		string str;

		while (!BN_is_zero(copy.ptr())) {
			uint d = copy.mod(base);
			str.push_back(d<10 ? '0'+(d) : 'A'+(d-10));
			copy.divideThis(base);
		}

		if (isNegative())
			str.push_back('-');

		std::reverse(str.begin(), str.end());
		return str;
	}

	static BigInt parseString(const string &str, uint base=10) {
		if (str.size()==0)
			throw math_exception("can't parse an empty string");
		if (base<2 || base>36)
			throw math_exception("parseString only supports bases from 2 to 36");
		bool neg = false;
		uint i=0;
		if (str[i]=='+') {
			++i;
		} else if (str[i]=='-') {
			neg = true;
			++i;
		}
		BigInt num;
		for (; i<str.size(); ++i) {
			char c = str[i];
			int d=-1;
			if (c>='0' && c<='9')
				d = c-'0';
			else if (c>='A' && c<='Z')
				d = c-'A'+10;
			else if (c>='a' && c<='z')
				d = c-'a'+10;

			if (d<0 || uint(d)>=base)
				throw math_exception("unexpected character parsing string");

			num.multiplyThis((ulong)base).addThis((ulong)d);
		}
		if (neg)
			num.negateThis();

		return num;
	}

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

}

#endif /* BIGINT_H_ */
