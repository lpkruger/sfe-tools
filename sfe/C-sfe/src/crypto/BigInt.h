/*
 * BigInt.h
 *
 *  Created on: Aug 12, 2009
 *      Author: louis
 */

#ifndef BIGINT_H_
#define BIGINT_H_


#include <vector>
#include <string>
#include <openssl/bn.h>
#include <openssl/rsa.h>
#include "sillytype.h"
//#include <boost/type_traits.hpp>
//#include <boost/numeric/conversion/converter.hpp>

using std::vector;
using std::string;

namespace bigint {

// see http://kaba.hilvi.org/Programming_C++/Texts/Null_Pointer.htm


template<class T> struct silly_ptr {
	T *p;
	silly_ptr<T>(T* p0) : p(p0) {};
	operator T* () const {
		return p;
	}
	T operator -> () const {
		return p;
	}
};

typedef silly_ptr<const BIGNUM> BNcPtr;


template<typename T> struct num {
	typedef T ctype;
};
template<> struct num<const BIGNUM*> {
	typedef BNcPtr ctype;
};
template<> struct num<int> {
	typedef long ctype;
};
template<> struct num<unsigned int> {
	typedef unsigned long ctype;
};
//template<> struct num<BNcPtr> {
//	typedef BNcPtr type;
//};


class BigInt {
private:
	BIGNUM n;
	BN_CTX *bn_ctx;

	void setDefaultBnCtx();

	operator BIGNUM* () { return &n; }
	operator const BIGNUM* () const { return &n; }
public:
	operator BNcPtr () const { return BNcPtr(&n); }

//	const static BigInt ZERO;
//	const static BigInt ONE;
//	const static BigInt TWO;

	BigInt(ulong nn=0, BN_CTX *ctx = NULL) : bn_ctx(ctx) {
		BN_init(&n);
		if (!bn_ctx)
			setDefaultBnCtx();
		operator=(nn);
	}

	template<typename T> BigInt(T nn, BN_CTX *ctx = NULL) : bn_ctx(ctx) {
		BN_init(&n);
		if (!bn_ctx)
			setDefaultBnCtx();
		operator= ((typename num<T>::ctype) nn);
	}


	//explicit BigInt(const stupid::Wrap<const BIGNUM*>& nnn, bool takeown=true, BN_CTX *ctx = NULL) : bn_ctx(ctx) {
	//  const BIGNUM *nn = nnn;	// workaround silly 0 -> ptr cast

	BigInt(BIGNUM *nn, BN_CTX *ctx = NULL) : bn_ctx(ctx) {
		BN_init(&n);
		if (!bn_ctx)
			setDefaultBnCtx();
		BN_copy(*this, nn);
	}

	//////// copy constructor: do not delete
	BigInt(const BigInt &b) {
		BN_init(&n);
		bn_ctx = b.bn_ctx;
		operator=(b);
	}
	////////

	BigInt(const vector<byte> &b, BN_CTX *ctx = NULL) : bn_ctx(ctx) {
		BN_init(&n);
		BN_bin2bn(&b[0], b.size(), *this);
		if (!bn_ctx)
			setDefaultBnCtx();
	}

#ifndef NO_RVALREF
	BigInt(BigInt &&b) {
		BN_init(&n);
		BN_swap(*this, b);
		bn_ctx = b.bn_ctx;
	}
	BigInt& operator= (BigInt &&b) {
		BN_zero(*this);
		BN_swap(*this, b);
		return *this;
	}
#endif
	//////// copy assignment: do not delete
	BigInt& operator= (const BigInt &b) {
		BN_copy(*this, b);
		return *this;
	}
	////////
	BigInt& operator= (BNcPtr b) {
		BN_copy(*this, b);
		return *this;
	}
	BigInt& operator= (ulong nn) {
		BN_set_word(*this, nn);
		return *this;
	}
	BigInt& operator= (long nn) {
		if (nn>=0) {
			BN_set_word(*this, nn);
		} else {
			BN_set_word(*this, -nn);
			BN_set_negative(*this, 1);
		}
		return *this;
	}
	template<typename T> BigInt& operator= (T nn) {
		return operator= ((typename num<T>::ctype) nn);
	}


//	BigInt mod(const BigInt &m) const {
//		BigInt r;
//		BN_mod(r, *this, m, bn_ctx);
//		return r;
//	}
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
		BN_rand_range(r, max);
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

	vector<byte> toPosByteArray() const {
		vector<byte> ret(BN_num_bytes(*this));
		if (!ret.size())
			ret.push_back(0);
		BN_bn2bin(*this, &ret[0]);
		return ret;
	}

	vector<byte> toMPIByteArray() const {
		vector<byte> ret(BN_bn2mpi(*this, NULL));
		BN_bn2mpi(*this, &ret[0]);
		//vector<byte>::iterator p = ret.begin();
		//ret.erase(p, p+4);
		return ret;
	}

	static BigInt fromMPIByteArray(const vector<byte> buf) {
		BigInt ret;
		BN_mpi2bn(&buf[0], buf.size(), ret);
		return ret;
	}

	BigInt xxor(const BigInt &b) const {
		vector<byte> aa = toPosByteArray();
		vector<byte> bb = b.toPosByteArray();

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
		// TODO: check n for NULL
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

	virtual ~BigInt() {
		BN_free(&n);
	}

	// multilating operations
	BigInt& swapWith(BigInt &b) {
		BN_swap(*this, b);
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


//#define CBI const BigInt&
//#define OP0m(op, fn)    BigInt& operator op ()          { return fn(); }
//#define OP0c(op, fn)    BigInt  operator op ()    const { return fn(); }
//#define OP1m(op, fn, T) BigInt& operator op (T b)       { return fn(b); }
//#define OP1c(op, fn, T) BigInt  operator op (T b) const { return fn(b); }
//#define OP1u(op, fn, T) T       operator op (T b)     { return (b); }
//template<class T> IN BigInt operator+ (const BigInt& a, T b) { return a.add(b); }

};

#define CBI const BigInt&
#define IN static inline
#define OP1m(op, fn, T) IN BigInt& operator op (BigInt& a, T b) { return a.fn(b); }
#define OP1c(op, fn, T) IN BigInt  operator op (CBI a, T b)     { return a.fn(b); }
#define OP1u(op, fn, T) IN T       operator op (BigInt& a, T b) { return a.fn(b); }

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

//#define OP0m(op, fn) IN BigInt& operator op (BigInt& a)        { return a.fn(); }
#define OP0c(op, fn) IN BigInt  operator op (CBI a)            { return a.fn(); }

OP0c(-, negate)
OP1c(+, add, CBI)
OP1c(-, subtract, CBI)
OP1c(*, multiply, CBI)
OP1c(%, mod, CBI)
OP1m(+=, addThis, CBI)
OP1m(-=, subtractThis, CBI)
OP1m(*=, multiplyThis, CBI)
OP1m(%=, modThis, CBI)
};

#endif /* BIGINT_H_ */
