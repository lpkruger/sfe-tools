/*
 * PseudoRandom.h
 *
 *  Created on: Sep 3, 2009
 *      Author: louis
 */

#ifndef PSEUDORANDOM_H_
#define PSEUDORANDOM_H_

#include "EVPCipher.h"
namespace crypto {
namespace cipher {

// this class just implements any standard cipher in ctr mode
class PseudoRandom {
	EVPCipher *cipher;
	bool tofree;
	uint64_t ctr;
	byte_buf buf;

public:
	PseudoRandom(byte_buf &key, EVPCipher *cipher0 = NULL, bool tofree0=false) :
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

	void getBytes(byte *out, uint len) {
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

	void getBytes(byte_buf &buf, uint off=0, uint len=0) {
		if (len==0)
			len = buf.size()-off;
		if (off+len > buf.size())
			buf.resize(off+len);
		getBytes(&buf[off], len);
	}

	~PseudoRandom() {
		if (tofree)
			delete cipher;
	}
};

}
}

#endif /* PSEUDORANDOM_H_ */
