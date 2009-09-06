/*
 * PseudoRandom.cpp
 *
 *  Created on: Sep 3, 2009
 *      Author: louis
 */

#include "PseudoRandom.h"
#include "EVPCipher.h"

namespace crypto {
namespace cipher {

PseudoRandom::PseudoRandom(const byte_buf &key, EVPCipher *cipher0, bool tofree0) :
	cipher(cipher0), tofree(tofree0) {
	if (!cipher) {
		cipher = new EVPCipher(EVP_rc4());
		tofree = true;
	}
	CipherKey sk(key);
	cipher->init(Cipher::ENCRYPT_MODE, &sk);
	ctr = 0;
	buf.clear();
}

void PseudoRandom::getBytes(byte *out, uint len) {
	buf.reserve(len);
	byte_buf newbytes;
	while (buf.size() < len) {
		cipher->update(reinterpret_cast<byte*>(&ctr), sizeof(ctr));
		++ctr;
		newbytes = cipher->getOutput();
		if (newbytes.size() > 0) {
			buf.insert(buf.end(), newbytes.begin(), newbytes.end());
		}
	}
	memcpy(out, &buf[0], len);
	buf.erase(buf.begin(), buf.begin()+len);
}

void PseudoRandom::getBytes(byte_buf &buf, uint off, uint len) {
	if (len==0)
		len = buf.size()-off;
	if (off+len > buf.size())
		buf.resize(off+len);
	getBytes(&buf[off], len);
}

PseudoRandom::~PseudoRandom() {
	if (tofree)
		delete cipher;
}


}
}
