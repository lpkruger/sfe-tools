/*
 * BigInt.h
 *
 *  Created on: Aug 12, 2009
 *      Author: louis
 */

#ifndef BIGINT_H_
#define BIGINT_H_

#include "sillytype.h"
#include "sillymem.h"

namespace silly {
namespace bigint {

class math_exception : public silly::MsgBufferException {
	char msg[512];
public:
	math_exception(const char* msg0) : MsgBufferException(msg0) {}
	virtual const char* what() const throw() {
		return MsgBufferException::what();
	}

};

}
}

//#define BigInt BigInt_BN
#include "BigInt_BN.h"
//#undef BigInt

namespace silly {
namespace bigint {

//typedef BigInt_BN BigInt;

typedef vector<BigInt> BigInt_Vect;
typedef vector<BigInt_Vect> BigInt_Mtrx;
typedef vector<BigInt_Mtrx> BigInt_Cube;

}
}

// for compatibility
namespace bigint {
using namespace silly::bigint;
}

#endif /* BIGINT_H_ */

