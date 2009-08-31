/*
 * BigInt.h
 *
 *  Created on: Aug 12, 2009
 *      Author: louis
 */

#ifndef BIGINT_H_
#define BIGINT_H_

#include "sillytype.h"

namespace bigint {

class math_exception : public silly::MsgBufferException {
	char msg[512];
public:
	math_exception(const char* msg0) : MsgBufferException(msg0) {}
	virtual const char* what() const throw() {
		return MsgBufferException::what();
	}

};


// see http://kaba.hilvi.org/Programming_C++/Texts/Null_Pointer.htm
template<class T> struct silly_ptr {
	T *p;
	silly_ptr<T>(T* p0) : p(p0) {};
	operator T* () const {
		return p;
	}
	T* operator -> () const {
		return p;
	}
	T* get() {
		return p;
	}
};

}

//#define BigInt BigInt_BN
#include "BigInt_BN.h"
//#undef BigInt

namespace bigint {

//typedef BigInt_BN BigInt;

typedef vector<BigInt> BigInt_Vect;
typedef vector<BigInt_Vect> BigInt_Mtrx;
typedef vector<BigInt_Mtrx> BigInt_Cube;

}

#endif /* BIGINT_H_ */

