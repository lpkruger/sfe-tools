/*
 * BigInt.h
 *
 *  Created on: Aug 12, 2009
 *      Author: louis
 */


// don't include this file.
// Include bigint.h instead

#ifndef BIGINT_BN_BASE_IMPL_H_
#define BIGINT_BN_BASE_IMPL_H_

#include <vector>
#include <string>
#include <algorithm>
#include <openssl/bn.h>
#include <openssl/rsa.h>
#include "sillytype.h"

//#include "BigInt.h"

using std::vector;
using std::string;

namespace bigint {

typedef silly_ptr<const BIGNUM> BNcPtr;

#define BI_VIRTUAL //virtual
#define BI_BASE
// : public BigIntBase

//#define BN_PTR

class BigInt_BN_Base {
private:
#ifdef BN_PTR
	BIGNUM *n;
	void _init() {
		n = BN_new();
	}
protected:
	operator BIGNUM* () { return n; }
	operator const BIGNUM* () const { return n; }

	void _swap(BigInt_BN_Base &o) {
		std::swap(n, o.n);
	}
public:
	operator BNcPtr () const { return BNcPtr(n); }

	BI_VIRTUAL ~BigInt_BN_Base() {
		BN_free(n);
	}
#else
	BIGNUM n;
	void _init() {
		BN_init(&n);
	}
protected:
	operator BIGNUM* () { return &n; }
	operator const BIGNUM* () const { return &n; }

	void _swap(BigInt_BN_Base &o) {
		BN_swap(&n, &o.n);
	}
public:
	BI_VIRTUAL ~BigInt_BN_Base() {
		BN_free(&n);
	}

	operator BNcPtr () const { return BNcPtr(&n); }
#endif
private:
	void setDefaultBnCtx();

protected:
	BN_CTX *bn_ctx;
public:

//	const static BigInt ZERO;
//	const static BigInt ONE;
//	const static BigInt TWO;

	BigInt_BN_Base(ulong nn=0, BN_CTX *ctx = NULL) : bn_ctx(ctx) {
		_init();
		if (!bn_ctx)
			setDefaultBnCtx();
		operator=(nn);
	}
	BigInt_BN_Base(long nn, BN_CTX *ctx = NULL) : bn_ctx(ctx) {

		if (!bn_ctx)
			setDefaultBnCtx();
		operator=(nn);
	}
	BigInt_BN_Base(uint nn, BN_CTX *ctx = NULL) : bn_ctx(ctx) {
		_init();
		if (!bn_ctx)
			setDefaultBnCtx();
		operator=(nn);
	}
	BigInt_BN_Base(int nn, BN_CTX *ctx = NULL) : bn_ctx(ctx) {
		_init();
		if (!bn_ctx)
			setDefaultBnCtx();
		operator=(nn);
	}


	//explicit BigInt(const stupid::Wrap<const BIGNUM*>& nnn, bool takeown=true, BN_CTX *ctx = NULL) : bn_ctx(ctx) {}
	//  const BIGNUM *nn = nnn;	// workaround silly 0 -> ptr cast

	BigInt_BN_Base(const BIGNUM *nn, BN_CTX *ctx = NULL) : bn_ctx(ctx) {
		_init();
		if (!bn_ctx)
			setDefaultBnCtx();
		BN_copy(*this, nn);
	}

	//////// copy constructor: do not delete
	BigInt_BN_Base(const BigInt_BN_Base &b) {
		_init();
		bn_ctx = b.bn_ctx;
		operator=(b);
	}
	////////

#ifndef NO_RVALREF
	BigInt_BN_Base(BigInt_BN_Base &&b) {
		_init();
		_swap(b);
		bn_ctx = b.bn_ctx;
	}
	BigInt_BN_Base& operator= (BigInt_BN_Base &&b) {
		BN_zero(*this);
		_swap(b);
		return *this;
	}
#endif
	//////// copy assignment: do not delete
	BigInt_BN_Base& operator= (const BigInt_BN_Base &b) {
		BN_copy(*this, b);
		return *this;
	}
	////////

	BigInt_BN_Base& operator= (const BIGNUM* b) {
		BN_copy(*this, b);
		return *this;
	}
	//BigInt_BN_Base& operator= (BNcPtr b) { return operator=((const BIGNUM *) b); }

	BigInt_BN_Base& operator= (ulong nn) {
		BN_set_word(*this, nn);
		return *this;
	}
	BigInt_BN_Base& operator= (long nn) {
		if (nn>=0) {
			BN_set_word(*this, nn);
		} else {
			BN_set_word(*this, -nn);
			BN_set_negative(*this, 1);
		}
		return *this;
	}
	BigInt_BN_Base& operator= (uint nn) { return operator=((ulong) nn); }
	BigInt_BN_Base& operator= (int nn) { return operator=((long) nn); }
};

}

#endif /* BIGINT_H_ */
