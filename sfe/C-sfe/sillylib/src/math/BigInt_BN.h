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
	BigInt(const BIGNUM *nn) : super(nn) {}
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

	BigInt nextProbablePrime() const {
		BigInt ret(2);
		if (*this < ret)
			RETURN_RET;
		ret = *this;
		if (!ret.testBit(0))
			BN_sub_word(ret, 1);
		do {
			BN_add_word(ret, 2);
		} while (!BN_is_prime_fasttest(ret, BN_prime_checks, NULL, bn_ctx, NULL, 1));
		RETURN_RET;
	}

	static BigInt random(BNcPtr max) {
		BigInt ret;
		BN_rand_range(ret, (BIGNUM*) max.ptr());
		RETURN_RET;
	}
	static BigInt random(int bits, int top=-1, bool oddnum=false) {
		// top: -1 for any number, 0 for top-bit 1, 1 for top-bits 11
		BigInt ret;
		BN_rand(ret, bits, top, oddnum);
		RETURN_RET;
	}

	BigInt gcd(BNcPtr b) {
		BigInt ret;
		BN_gcd(ret, *this, b, bn_ctx);
		RETURN_RET;
	}

	BigInt negate() const {
		BigInt ret(*this);
		BN_set_negative(ret, !ret.isNegative());
		RETURN_RET;
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

	bool testBit(int n) const {
		return BN_is_bit_set(*this, n);
	}
	BigInt setBit(int n) const {
		BigInt ret(*this);
		BN_set_bit(ret, n);
		RETURN_RET;
	}
	BigInt& setBitThis(int n) {
		BN_set_bit(*this, n);
		return *this;
	}
	BigInt clearBit(int n) const {
		BigInt ret(*this);
		if (ret.testBit(n))
			BN_clear_bit(ret, n);
		RETURN_RET;
	}
	BigInt& clearBitThis(int n) {
		if (testBit(n))
			BN_clear_bit(*this, n);
		return *this;
	}

	bool equals(BNcPtr o) const {
		if (ptr() == o)
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

	BIGNUM* writePtr() {
		return *this;
	}
	const BIGNUM* ptr() const {
		return *this;
	}

	static BigInt genPrime(int bits) {
		BigInt ret;
		BIGNUM *n =	BN_generate_prime(ret, bits, false, NULL, NULL, NULL, NULL);
		if (!n)
			throw math_exception("error generating prime number");
		RETURN_RET;
	}
	string toHexString() const {
		char *buf = BN_bn2hex(*this);
		string ret(buf);
		OPENSSL_free(buf);
		RETURN_RET;
	}
	string toString() const {
		char *buf = BN_bn2dec(*this);
		string ret(buf);
		OPENSSL_free(buf);
		RETURN_RET;
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

	static byte_buf fromPosBigInt(BNcPtr num, int len=0) {
		int reallen = BN_num_bytes(num);
		if (len<reallen) len = reallen;
		byte_buf ret(1, 0);
		if (!len) RETURN_RET;
		ret.resize(len);
		int offset = len-reallen;
		BN_bn2bin(num, &ret[offset]);
		RETURN_RET;
	}

	static BigInt toPosBigInt(const byte_buf &buf) {
		BigInt ret;
		if (buf.empty())
			RETURN_RET;
		BN_bin2bn(&buf[0], buf.size(), (BIGNUM*) ret.ptr());
		RETURN_RET;
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

		byte_buf ret;
		if (!BN_is_negative(num)) {
			ret = fromPosBigInt(num, len);
			RETURN_RET;
		}
		BigInt n2(1);
		n2.shiftLeftThis(len*8);
		n2.addThis(num);
		ret = fromPosBigInt(n2, len);
		RETURN_RET;
	}

	static BigInt to2sCompBigInt(const byte_buf &buf) {
		BigInt ret = toPosBigInt(buf);
		int len = ret.byteLength();
		if (!ret.testBit(len*8-1))
			RETURN_RET;

		// it's negative
		BigInt n2(1);
		n2.shiftLeftThis(len*8);
		n2.subtractThis(ret);
		n2.negateThis();
		ret.swapWith(n2);
		RETURN_RET;
	}

	static byte_buf MPIfromBigInt(BNcPtr num) {
		byte_buf ret(BN_bn2mpi(num, NULL));
		BN_bn2mpi(num, &ret[0]);
		RETURN_RET;
	}

	static BigInt MPItoBigInt(const byte_buf &buf) {
		BigInt ret;
		BN_mpi2bn(&buf[0], buf.size(), ret);
		RETURN_RET;
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
		RETURN_RET;
	}

	string toString(uint base=10) {
		if (base<2 || base>36)
			throw math_exception("toString only supports bases from 2 to 36");
		string str;
		if (BN_is_zero(ptr())) {
			str = "0";
			return str;
		}
		BigInt copy(*this);

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
