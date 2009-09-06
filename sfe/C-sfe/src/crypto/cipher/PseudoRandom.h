/*
 * PseudoRandom.h
 *
 *  Created on: Sep 3, 2009
 *      Author: louis
 */

#ifndef PSEUDORANDOM_H_
#define PSEUDORANDOM_H_

#include "sillytype.h"
#include "random.h"

namespace crypto {
namespace cipher {

class EVPCipher;

// this class just implements any standard cipher in ctr mode
class PseudoRandom : public Random {
	EVPCipher *cipher;
	bool tofree;
	uint64_t ctr;
	byte_buf buf;

public:
	PseudoRandom(byte_buf &key, EVPCipher *cipher0 = NULL, bool tofree0=false);
	void getBytes(byte *out, uint len);
	void getBytes(byte_buf &buf, uint off=0, uint len=0);
	~PseudoRandom();
};

}
}

#endif /* PSEUDORANDOM_H_ */
