// this file should not be included directly


///////////////////////////////
// cut and paste from old ver
#define RETURN_RET	return ret
///////////////////////////////

BigInt xxor(const BigInt &b) const _PURE {
	byte_buf aa = fromPosBigInt(me);
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

bool isNegative() const _PURE {
	return BN_is_negative(cptr(me));
}
int byteLength() const _PURE {
	return BN_num_bytes(me);
}
int bitLength() const _PURE {
	return BN_num_bits(me);
}
bool testBit(int n) const _PURE {
	return BN_is_bit_set(me, n);
}
BigInt setBit(int n) const _PURE {
	BigInt ret(me);
	BN_set_bit(ret, n);
	RETURN_RET;
}
BigInt& setBitThis(int n) {
	BN_set_bit(me, n);
	return me;
}
BigInt clearBit(int n) const _PURE {
	BigInt ret(me);
	if (ret.testBit(n))
		BN_clear_bit(ret, n);
	RETURN_RET;
}
BigInt& clearBitThis(int n) {
	if (testBit(n))
		BN_clear_bit(me, n);
	return me;
}
BigInt nextProbablePrime() const _PURE {
	BigInt ret(2);
	if (me < ret)
		RETURN_RET;
	ret = me;
	if (!ret.testBit(0))
		BN_sub_word(ret, 1);
	do {
		BN_add_word(ret, 2);
	} while (!BN_is_prime_fasttest(ret, BN_prime_checks, NULL, bn_ctx, NULL, 1));
	RETURN_RET;
}

static BigInt random(BNcPtr max) {
	BigInt ret;
	BN_rand_range(ret, max);
	RETURN_RET;
}
static BigInt random(int bits, int top=-1, bool oddnum=false) {
	// top: -1 for any number, 0 for top-bit 1, 1 for top-bits 11
	BigInt ret;
	BN_rand(ret, bits, top, oddnum);
	RETURN_RET;
}
static BigInt genPrime(int bits) {
	BigInt ret;
	BIGNUM *n =	BN_generate_prime(ret, bits, false, NULL, NULL, NULL, NULL);
	if (!n)
		throw math_exception("error generating prime number");
	RETURN_RET;
}

static byte_buf fromPosBigInt(BNcPtr num, int len=0) _PURE {
	int reallen = BN_num_bytes(num);
	if (len<reallen) len = reallen;
	byte_buf ret(1, 0);
	if (!len) RETURN_RET;
	ret.resize(len);
	int offset = len-reallen;
	BN_bn2bin(num, &ret[offset]);
	RETURN_RET;
}


static BigInt toPosBigInt(const byte_buf &buf) _PURE {
	BigInt ret;
	if (buf.empty())
		RETURN_RET;
	BN_bin2bn(&buf[0], buf.size(), ptr(ret));
	RETURN_RET;
}
static byte_buf from2sCompBigInt(BNcPtr num, int len=0) _PURE {
	int reallen = BN_num_bytes(num);
	if (!reallen)
		++reallen;
	if (BN_is_bit_set(num, reallen*8-1)) {
		if (!BN_is_negative(cptr(num)))
			++reallen;		// need an extra byte if high bit is set
		else {
			BigInt tmp(cptr(num), false);
			BN_clear_bit(tmp, reallen*8-1);
			if (!BN_is_zero(ptr(tmp)))
				++reallen;
		}
	}
	if (len<reallen)
		len = reallen;

	byte_buf ret;
	if (!BN_is_negative(cptr(num))) {
		ret = fromPosBigInt(num, len);
		RETURN_RET;
	}
	BigInt n2(1);
	n2.shiftLeftThis(len*8);
	n2.addThis(num);
	ret = fromPosBigInt(n2, len);
	RETURN_RET;
}

static BigInt to2sCompBigInt(const byte_buf &buf) _PURE {
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

static byte_buf MPIfromBigInt(BNcPtr num) _PURE {
	byte_buf ret(BN_bn2mpi(num, NULL));
	BN_bn2mpi(num, &ret[0]);
	RETURN_RET;
}

static BigInt MPItoBigInt(const byte_buf &buf) _PURE {
	BigInt ret;
	BN_mpi2bn(&buf[0], buf.size(), ret);
	RETURN_RET;
}

static BigInt toPaddedBigInt(byte_buf buf) _PURE {
	buf.insert(buf.begin(), 1);
	return toPosBigInt(buf);
}
static byte_buf fromPaddedBigInt(BNcPtr num) _PURE {
	byte_buf ret = fromPosBigInt(num);
	if (ret[0] != 1)
		throw math_exception("bignum not padded");
	ret.erase(ret.begin());
	RETURN_RET;
}


std::string toHexString() const _PURE {
	char *buf = BN_bn2hex(me);
	std::string ret(buf);
	OPENSSL_free(buf);
	RETURN_RET;
}

std::string toString() const _PURE {
	char *buf = BN_bn2dec(me);
	std::string ret(buf);
	OPENSSL_free(buf);
	RETURN_RET;
}

std::string toString(uint base) const _PURE {
	if (base<2 || base>36)
		throw math_exception("toString only supports bases from 2 to 36");
	std::string str;
	if (BN_is_zero(cptr(me))) {
		str = "0";
		return str;
	}
	BigInt copy(me);

	while (!BN_is_zero(ptr(copy))) {
		uint d = copy.mod(base);
		str.push_back(d<10 ? '0'+(d) : 'A'+(d-10));
		copy.divideThis(base);
	}

	if (isNegative())
		str.push_back('-');

	std::reverse(str.begin(), str.end());
	return str;
}

static BigInt parseString(const std::string &str, uint base=10) _PURE {
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

///////////////////////////////
