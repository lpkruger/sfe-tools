/*
 * SFECipher.cpp
 *
 *  Created on: Sep 1, 2009
 *      Author: louis
 */

#include "SFECipher.h"

namespace crypto {
namespace cipher {


byte_buf SFECipher::md_xkey(const byte_buf &data) {
	byte_buf xkey(20);
	SHA1_Update(&ctx, &data[0], data.size());
	SHA1_Final(&xkey[0], &ctx);
	return silly_move(xkey);
}
// TODO: why do these take a key* ?
// padding-free mode
byte_buf SFECipher::deencrypt(const SFEKey *key, const byte_buf &data) {
	byte_buf xkey = md_xkey(*key->buf);
	if (xkey.size() < data.size()) {
		throw bad_argument("data too long");
	}

	byte_buf ret(data.size());
	for (uint i=0; i<ret.size(); ++i) {
		ret[i] = (data[i] ^ xkey[i]);
	}
	//std::cout << "de len is " << data.size() << "  xkey is " << xkey.size() << std::endl;
	return silly_move(ret);
}

byte_buf SFECipher::encrypt(const SFEKey *key, const byte_buf &data) {
	byte_buf xkey = md_xkey(*key->buf);
	if (xkey.size() - 1 < data.size()) {
		throw bad_argument("data too long");
	}
	byte_buf ret(xkey.size());
	for (uint i=0; i<data.size(); ++i) {
		ret[i] = (data[i] ^ xkey[i]);
	}
	for (uint i=data.size(); i<ret.size() - 1; ++i) {
		ret[i] = xkey[i];
	}
	ret[ret.size() - 1] = (xkey[ret.size() - 1] ^ data.size());
	return silly_move(ret);
}

byte_buf SFECipher::decrypt(const SFEKey *key, const byte_buf &data) {
	byte_buf xkey = md_xkey(*key->buf);
	if (xkey.size() != data.size()) {
		throw bad_padding("Invalid data length");
	}
	byte_buf dec(data.size());
	for (uint i=0; i<dec.size(); ++i) {
		dec[i] = (data[i] ^ xkey[i]);
	}
	uint len = dec[dec.size() - 1];
	if (len > dec.size() - 1)
		throw bad_padding("Invalid data range");
	for (uint i=len; i<dec.size()-1; ++i) {
		if (dec[i] != 0)
			throw bad_padding("Invalid data range");
	}
	dec.resize(len);
	//byte_buf ret = new byte[len];
	//System.arraycopy(dec, 0, ret, 0, len);
	//std::cout << "len is " << len << "  xkey is " << xkey.size() << std::endl;
	return silly_move(dec);
}



}
}

