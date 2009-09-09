/*
 * EVPCipher.h
 *
 *  Created on: Sep 3, 2009
 *      Author: louis
 */

#ifndef EVPCIPHER_H_
#define EVPCIPHER_H_

#include <openssl/evp.h>
#include <openssl/rand.h>
#include "cipher.h"
#include "sillytype.h"

namespace crypto {
void throwCipherError(const char* localerr = NULL);

namespace cipher {

struct EVPParams : public AlgorithmParams {
	byte_buf *IV;
	uint rounds;	// EVP_BytesToKey is used if rounds>0
	EVP_MD *md;
	uchar *salt;  // 8 bytes
	bool nopadding;
	EVPParams(byte_buf *I0=NULL, uint r0=0, EVP_MD *m0=NULL, uchar *s=NULL, bool nopad=false) :
		IV(I0), rounds(r0), md(m0), salt(s), nopadding(nopad) {}
	EVPParams(bool nopad, byte_buf *I0=NULL, uint r0=0, EVP_MD *m0=NULL, uchar *s=NULL) :
		IV(I0), rounds(r0), md(m0), salt(s), nopadding(nopad) {}
};
class EVPCipher : public Cipher {
	modes mode;
	//const EVP_CIPHER *cipher;
	EVP_CIPHER_CTX c_ctx;
	byte_buf out_buffer;

public:
	EVPCipher(const EVP_CIPHER *c, int keylen=0);
	EVPCipher(const char *name, int keylen=0);
	~EVPCipher();

	virtual void init(modes mode, const SecretKey *sk, const AlgorithmParams *params=NULL);
	virtual void update(const byte *input, int len);
	virtual byte_buf doFinal(const byte *input, int len);

	virtual uint bytesAvailable() {
		return out_buffer.size();
	}
	virtual byte_buf getOutput() {
		byte_buf ret_buffer;
		ret_buffer.swap(out_buffer);
		return silly_move(ret_buffer);
	}
	int getBlockSize() {
		return EVP_CIPHER_CTX_block_size(&c_ctx);
		//return EVP_CIPHER_block_size(cipher);
	}
	int getKeySize() {
		return EVP_CIPHER_CTX_key_length(&c_ctx);
		//return EVP_CIPHER_key_length(cipher);
	}
	int getIVSize() {
		return EVP_CIPHER_CTX_iv_length(&c_ctx);
		//return EVP_CIPHER_iv_length(cipher);
	}

	const char* getCipherName() {
		return EVP_CIPHER_name(c_ctx.cipher);
	}

	static void init_all_algorithms();
	static vector<string> enumerate_ciphers();
	static vector<string> enumerate_digests();
};

}
}

#endif /* EVPCIPHER_H_ */
