/*
 * EVPCipher.cpp
 *
 *  Created on: Sep 3, 2009
 *      Author: louis
 */

#include "EVPCipher.h"

///////////////////////////////
// EVPCipher.cpp

#include <string.h>
#include <stdlib.h>
#include "silly.h"
#include <openssl/err.h>

using namespace crypto::cipher;
using silly::misc::string_printf;

void crypto::throwCipherError(const char* localerr) {
	static bool loaded_strings __attribute__ ((unused))
			= (ERR_load_crypto_strings(), true);
	int locallen = localerr == NULL ? 0 : strlen(localerr);
	char errstr[128+locallen];
	if (localerr) {
		memcpy(errstr, localerr, locallen);
		errstr[locallen] = ':';
	}
	ulong err = ERR_get_error();
	ulong err0 = err;
	while (err0) {
		err = err0;
		err0 = ERR_get_error();
	}
	ERR_error_string_n(err, errstr+locallen+1, 128);
	throw CipherException(errstr);
}
// Takes key, number of key gen rounds as the arguments
EVPCipher::EVPCipher(const EVP_CIPHER *c, int keylen) {
	EVP_CIPHER_CTX_init(&c_ctx);
	if (!EVP_EncryptInit_ex(&c_ctx,c, NULL, NULL, NULL))
		throwCipherError();
	if (keylen)
		if (!EVP_CIPHER_CTX_set_key_length(&c_ctx, keylen))
			throwCipherError();
}

EVPCipher::EVPCipher(const char* ciphername, int keylen) {
	const EVP_CIPHER *c = EVP_get_cipherbyname(ciphername);
	if (!c) {
		//throw CipherException(string_printf("Unknown cipher %s", ciphername).c_str());
		throwCipherError();
	}
	EVP_CIPHER_CTX_init(&c_ctx);
	if (!EVP_EncryptInit_ex(&c_ctx,c, NULL, NULL, NULL))
		throwCipherError();
	if (keylen)
		if (!EVP_CIPHER_CTX_set_key_length(&c_ctx, keylen))
			throwCipherError();
}

EVPCipher::~EVPCipher() {

	if (!EVP_CIPHER_CTX_cleanup(&c_ctx)) {
		//throw CipherException("error in EVP_CIPHER_CTX_cleanup");
		throwCipherError();
	}
}

void EVPCipher::init(modes mode, const SecretKey *sk, const AlgorithmParams *param0) {
	out_buffer.clear();
	//EVP_CIPHER_CTX_init(&c_ctx);
	if (!sk)
		throw CipherException("key is NULL");
	const CipherKey *key = dynamic_cast<const CipherKey*> (sk);

	byte_buf key_buf;
	byte_buf *pKey;
	if (key) {
		pKey = key->getRawBuffer();
	} else {
		key_buf = key->getEncoded();
		pKey = &key_buf;
	}

	EVPParams default_params;
	const EVPParams *params = dynamic_cast<const EVPParams*>(param0);
	if (!params) {
		params=&default_params;
	}

	byte_buf *pIV = params->IV;
	byte_buf iv_buf;

	if (params->rounds>0) {
		const EVP_MD *md = params->md;
		if (!md)
			md = EVP_md5();
		byte_buf keyiv;
		keyiv.insert(keyiv.end(), pKey->begin(), pKey->end());
		if (params->IV)
			keyiv.insert(keyiv.end(), pIV->begin(), pIV->end());
		key_buf.resize(c_ctx.cipher->key_len);
		iv_buf.resize(c_ctx.cipher->iv_len);
		uint ret = EVP_BytesToKey(c_ctx.cipher, md, params->salt,
				&keyiv[0], keyiv.size(), params->rounds, &key_buf[0], &iv_buf[0]);
		if (ret != key_buf.size())
			throw CipherException(string_printf("error in EVP_BytesToKey %d != %d", ret, key_buf.size()).c_str());
		pKey = &key_buf;
		pIV = &iv_buf;
	}
	int ret;
	switch(mode) {
	case ENCRYPT_MODE:
		ret = EVP_EncryptInit_ex(&c_ctx, c_ctx.cipher, NULL, &pKey->at(0), pIV ? &pIV->at(0) : NULL);
		if (!ret) {
			//throw throw CipherException("error in EVP_EncryptInit_ex");
			throwCipherError();
		}

		break;
	case DECRYPT_MODE:
		ret = EVP_DecryptInit_ex(&c_ctx, c_ctx.cipher, NULL, &pKey->at(0), pIV ? &pIV->at(0) : NULL);
		if (!ret) {
			//throw CipherException("error in EVP_DecryptInit_ex");
			throwCipherError();
		}
		break;
	default:
		throw CipherException(string_printf("Unknown mode %d", mode).c_str());
	}
	EVP_CIPHER_CTX_set_padding(&c_ctx, !params->nopadding);
}


void EVPCipher::update(const byte *in, int inl) {
	byte out[inl + c_ctx.cipher->block_size - 1];
	int outl = 0;
	int ret = EVP_CipherUpdate(&c_ctx, out, &outl, in, inl);
	if (!ret) {
		//throw CipherException("error in EVP_CipherUpdate");
		throwCipherError();
	}
	//printf("update %d %d\n", inl, outl);
	out_buffer.insert(out_buffer.end(), &out[0], &out[outl]);
	//printf("have bytes: %d\n", bytesAvailable());
}

byte_buf EVPCipher::doFinal(const byte *input, int inl) {
	byte out[c_ctx.cipher->block_size];
	int outl = 0;
	update(input, inl);
	int ret = EVP_CipherFinal_ex(&c_ctx, out, &outl);
	//printf("final %d\n", outl);
	if (!ret) {
		//throw CipherException("error in EVP_CipherFinal_ex");
		throwCipherError();
	}
	out_buffer.insert(out_buffer.end(), &out[0], &out[outl]);

	byte_buf ret_buffer;
	ret_buffer.swap(out_buffer);
	return silly_move(ret_buffer);
}




static void evp_add_name(const OBJ_NAME *obj, void *arg) {
	vector<string> *vv = (vector<string>*) arg;
	vv->push_back(string(obj->name));
}
vector<string> EVPCipher::enumerate_ciphers() {
	init_all_algorithms();
	vector<string> out;
	OBJ_NAME_do_all_sorted(OBJ_NAME_TYPE_CIPHER_METH, evp_add_name, &out);
	return silly_move(out);
}

vector<string> EVPCipher::enumerate_digests() {
	init_all_algorithms();
	vector<string> out;
	OBJ_NAME_do_all_sorted(OBJ_NAME_TYPE_MD_METH, evp_add_name, &out);
	return silly_move(out);
}

struct EVPCipher_Init {
	EVPCipher_Init() {
		OpenSSL_add_all_algorithms();
	}
	~EVPCipher_Init() {
		EVP_cleanup();
	}
};

void EVPCipher::init_all_algorithms() {
	static EVPCipher_Init init_object;
}



