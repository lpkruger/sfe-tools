/*
 * HKOT.h
 *
 *  Created on: Sep 20, 2009
 *      Author: louis
 */

/*
 * KO.h
 *
 *  Created on: Aug 6, 2009
 *      Author: louis
 */

#ifndef KO_H_
#define KO_H_

#if 1

#include "bigint.h"
#include "sillytype.h"
#include "sillyio.h"

#include <vector>
#include "../cipher/Paillier.h"

namespace crypto {
namespace ot {
namespace hkot {

using namespace silly::io;
using namespace silly::types;
using namespace silly::bigint;
using crypto::cipher::Paillier;
using crypto::cipher::PaillierEncKey;
using crypto::cipher::PaillierDecKey;

const static int lgB = 4;
const static int B = 1 << lgB;		// security param
//const static int M = 16;
const static int M = 32;			// security param
const static int Beta = 128/8;		// security param
const static int HKeySz = 1024;		// security param, size of Paillier key

class Sender {
	DataInput *in;
	DataOutput *out;

	BigInt_Mtrx smx;

public:
	void setStreams(DataInput *in0, DataOutput *out0) {
		in = in0;
		out = out0;
	}
	void precompute();
	BigInt get_value(const byte_buf &key);
	void online();


private:
	friend int test_ot(int argc, char **argv);
};

class Chooser {
	DataInput *in;
	DataOutput *out;

	PaillierDecKey decKey;
	BigInt_Mtrx cmx;
public:
	Chooser() : in(NULL), out(NULL), decKey(0,0,0,0) {}

	void setStreams(DataInput *in0, DataOutput *out0) {
		in = in0;
		out = out0;
	}
	void precompute(const byte_buf &key);
	BigInt online();

private:
	friend int test_ot(int argc, char **argv);
};

int test_ot(int argc, char **argv);

}
}
}


#endif
#endif /* HKOT_H_ */
