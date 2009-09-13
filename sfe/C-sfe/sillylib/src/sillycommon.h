/*
 * sillycommon.h
 *
 *  Created on: Aug 28, 2009
 *      Author: louis
 */

#ifndef SILLYCOMMON_H_
#define SILLYCOMMON_H_

//#include <errno.h>
#include <string.h>
#include <stdexcept>

#define NOCOPY(type) \
		type(const type &nocopy) {}		\
		void operator=(const type &nocopy) {}


#define RESOURCE __attribute__ ((unused))

namespace silly {

class ErrnoException : public virtual std::exception {
	int errnum;
public:
	ErrnoException(unsigned int err) : errnum(err) {}
	virtual const char* what() const throw() {
		if (errnum==-1)
			return "<unspecified error>";
		return strerror(errnum);
	}
	unsigned int getErrno() const {
		return errnum;
	}
};

class MsgBufferException : public virtual std::exception {
protected:
	static const int buflen = 1024;
	char msg[buflen];
public:
	MsgBufferException(const char* msg0) {
		strncpy(msg, msg0, buflen);
		msg[buflen-1] = '\0';
	}
	virtual const char* what() const throw() {
		return msg;
	}
};

struct NullPointerException : public MsgBufferException {
	NullPointerException(const char* msg0) : MsgBufferException(msg0) {}
};

}
using silly::MsgBufferException;
using silly::NullPointerException;


//// some handy optimization macros
#define _likely(x) __builtin_expect(!!(x), 1)
#define _unlikely(x) __builtin_expect(x, 0)
#define _CONST __attribute__ ((const))
#define _PURE __attribute__ ((pure))
#define _QUICK __attribute__ ((regparm(3)))


// some hand NOP template constrainst
template<class T, class B> struct Derived_from {
	static void constraints(T* p) { B* pb = p; }
	Derived_from() { void(*p)(T*) = constraints; }
};

template<class T1, class T2> struct Can_copy {
	static void constraints(T1 a, T2 b) { T2 c = a; b = a; }
	Can_copy() { void(*p)(T1,T2) = constraints; }
};


#endif /* SILLYCOMMON_H_ */
