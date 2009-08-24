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

using std::vector;
using std::string;

namespace bigint {
namespace stupid {
// see http://kaba.hilvi.org/Programming_C++/Texts/Null_Pointer.htm
template <typename T> class Wrap {
	T x;
public:
	Wrap(T x0) : x(x0) {}
	operator T() const { return x; };
};
}

class BigInt {
private:
	BIGNUM *n;
	BN_CTX *bn_ctx;

	void setDefaultBnCtx();

	operator BIGNUM* () { return n; }
	operator const BIGNUM* () const { return n; }
public:
	typedef BIGNUM* BIGNUM_ptr;
	const static BigInt ZERO;
	const static BigInt ONE;
	const static BigInt TWO;

	BigInt(long nn=0, BN_CTX *ctx = NULL) : bn_ctx(ctx) {

		n = BN_new();
		if (nn>=0) {
			BN_set_word(n, nn);
		} else {
			BN_set_word(n, -nn);
			BN_set_negative(n, 1);
		}
		if (!bn_ctx)
			setDefaultBnCtx();
	}

	//explicit BigInt(const stupid::Wrap<const BIGNUM*>& nnn, bool takeown=true, BN_CTX *ctx = NULL) : bn_ctx(ctx) {
	//  const BIGNUM *nn = nnn;	// workaround silly 0 -> ptr cast

	BigInt(const BIGNUM *nn, bool takeown = false, BN_CTX *ctx = NULL) : bn_ctx(ctx) {
		if (takeown) {
			n = (BIGNUM*) nn;
		} else {
			n = BN_dup(nn);
		}
		if (!bn_ctx)
			setDefaultBnCtx();
	}

	BigInt(const BigInt &b) {
		n = BN_dup((BigInt&) b);
		bn_ctx = b.bn_ctx;
	}

	BigInt(const vector<byte> &b, BN_CTX *ctx = NULL) : bn_ctx(ctx) {
		n = BN_new();
		BN_bin2bn(&b[0], b.size(), n);
		if (!bn_ctx)
			setDefaultBnCtx();
	}
#ifndef NO_RVALREF
	BigInt(BigInt &&b) {
		n = b.n;
		b.n = NULL;
		bn_ctx = b.bn_ctx;
	}
	BigInt& operator= (BigInt &&b) {
		if (n)
			BN_free(n);
		n = b.n;
		b.n = NULL;
		return *this;
	}
#endif

	BigInt& operator= (const BigInt &b) {
		BN_copy(n, b);
		return *this;
	}
	BigInt mod(const BigInt &m) const {
		BigInt r;
		BN_mod(r, *this, m, bn_ctx);
		return r;
	}
	BigInt modInverse(const BigInt &m) const {
		BigInt r;
		BN_mod_inverse(r, *this, m, bn_ctx);
		return r;
	}
	BigInt modMultiply(const BigInt &b, const BigInt &m) const {
		BigInt r;
		BN_mod_mul(r, *this, b, m, bn_ctx);
		return r;
	}
	BigInt modDivide(const BigInt &b, const BigInt &m) const {
		BigInt r;
		BN_mod_inverse(r, b, m, bn_ctx);
		// TODO check return for NULL
		BN_mod_mul(r, *this, r, m, bn_ctx);
		return r;
	}
	BigInt modPow(const BigInt &b, const BigInt &m) const {
		BigInt r;
		BN_mod_exp(r, *this, b, m, bn_ctx);
		return r;
	}

	BigInt add(const BigInt &b) const {
		BigInt r;
		BN_sub(r, *this, b);
		return r;
	}

	BigInt subtract(const BigInt &b) const {
		BigInt r;
		BN_sub(r, *this, b);
		return r;
	}

	BigInt multiply(const BigInt &b) const {
		BigInt r;
		BN_mul(r, *this, b, bn_ctx);
		return r;
	}
	BigInt pow(const BigInt &p) const {
		BigInt r;
		BN_exp(r, *this, p, bn_ctx);
		return r;
	}
	BigInt divide(const BigInt &b) const {
		BigInt r;
		BN_div(r, NULL, *this, b, bn_ctx);
		return r;
	}

	BigInt nextProbablePrime() const {
		BigInt r;
		BN_copy(r, n);
		do {
			BN_add_word(r, 1);
		} while (!BN_is_prime(r, 128, NULL, bn_ctx, NULL));
		return r;
	}

	static BigInt random(const BigInt &max) {
		BigInt r;
		BN_rand_range(r, max);
		return r;
	}

	bool isNegative() {
		return BN_is_negative(n);
	}

	vector<byte> toPosByteArray() const {
		vector<byte> ret(BN_num_bytes(n));
		if (!ret.size())
			ret.push_back(0);
		BN_bn2bin(n, &ret[0]);
		return ret;
	}

	vector<byte> toMPIByteArray() const {
		vector<byte> ret(BN_bn2mpi(n, NULL));
		BN_bn2mpi(n, &ret[0]);
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
		BN_set_negative(ret, BN_is_negative(n) ^ BN_is_negative(b.n));
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

	bool equals(const BigInt &o) const {
		if (this == &o)
			return true;
		return BN_cmp(*this, o) == 0;
	}

	// return the BIGNUM and release it from control
	BIGNUM* release() {
		BIGNUM* ptr = n;
		n = NULL;
		return ptr;
	}
	BIGNUM* getPtr() const {
		return n;
	}

	static BigInt genPrime(int bits) {
		BIGNUM *n =	BN_generate_prime(NULL, 129, false, NULL, NULL, NULL, NULL);
		return BigInt(n, true);
	}

	string toString() const {
		char *buf = BN_bn2dec(n);
		string str(buf);
		OPENSSL_free(buf);
		return str;
	}

	virtual ~BigInt() {
		if (n)
			BN_free(n);
		n = NULL;
	}
};

#if 0
static inline BigInt operator+ (const BigInt &a, const BigInt &b) {
	return a.add(b);
}
static inline BigInt operator- (const BigInt &a, const BigInt &b) {
	return a.subtract(b);
}
static inline BigInt operator* (const BigInt &a, const BigInt &b) {
	return a.multiply(b);
}
static inline BigInt operator/ (const BigInt &a, const BigInt &b) {
	return a.divide(b);
}
static inline BigInt operator% (const BigInt &a, const BigInt &b) {
	return a.mod(b);
}
#endif

};

#endif /* BIGINT_H_ */
