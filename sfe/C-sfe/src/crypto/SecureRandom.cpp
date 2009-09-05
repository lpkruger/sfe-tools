/*
 * Random.cpp
 *
 *  Created on: Sep 3, 2009
 *      Author: louis
 */

#include "SecureRandom.h"
#include "cipher/EVPCipher.h"
#include <openssl/rand.h>

void crypto::SecureRandom::getBytes(byte *out, uint len) {
	int err = RAND_bytes(out, len);
	if (err != 1)
		throwCipherError();
}

