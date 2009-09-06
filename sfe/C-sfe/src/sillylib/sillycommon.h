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
	unsigned int getErrno() {
		return errnum;
	}
};

class MsgBufferException : public virtual std::exception {
	char msg[512];
public:
	MsgBufferException(const char* msg0) {
		strncpy(msg, msg0, 512);
		msg[511] = '\0';
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

#endif /* SILLYCOMMON_H_ */
