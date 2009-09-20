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

#define USE_OLD_BIGINT

#ifndef USE_OLD_BIGINT
// use new implementation
#define BI BigInt_BN_Base
#define BI2 BigInt
#include "BigIntShape.h"
#undef BI2
#undef BI
#else
//#define BigInt BigInt_BN
#include "BigInt_BN.h"
//#undef BigInt
#endif

namespace silly {
namespace bigint {

typedef const BigInt CBigInt;
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

