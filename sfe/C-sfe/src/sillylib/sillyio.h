/*
 * sillyio.h
 *
 *  Created on: Aug 22, 2009
 *      Author: louis
 */

#ifndef SILLYIO_H_
#define SILLYIO_H_

#include <vector>
#include <iostream>
#include <netinet/in.h>
#include <string.h>
#include "sillytype.h"
#include <errno.h>

namespace silly {
namespace io {

// IO stuff


class IOException : public std::exception {
	int errnum;
public:
	IOException(int err=-1) : errnum(err) {}

	virtual const char *what() const throw () {
		if (errnum==-1)
			return "<unspecified i/o error>";
		return strerror(errnum);
	}
};
struct EOFException : public IOException {
	virtual const char *what() const throw () {
		return "<end of file>";
	}
};
struct ProtocolException : public IOException {
	const char* msg;
	ProtocolException(const char* msg0) : msg(msg0) {}
	virtual const char *what() const throw () {
		return msg;
	}
};

class DataOutput {
protected:
	virtual int tryWrite(const byte* c, int len) = 0;
public:
	virtual void flush() {}
	virtual void close() {}

	void writeBoolean(bool b) {
		writeByte(b ? 0xFF : 0);
	}
	void writeByte(byte b) {
		write(&b, 1);
	}
	void writeInt(uint32_t i) {
		uint32_t j = htonl(i);
		write((const byte*) &j, 4);
	}
	void write(const byte* c, int len) {
		int cnt=0;
		while (cnt<len) {
//			if(cnt>0)
//				std::cout << "count is " << cnt << std::endl;
			int n = tryWrite(c+cnt, len-cnt);
			if (n==0) {
				throw EOFException();
			}
			if (n<0) {
				throw IOException(0);
			}
			cnt+=n;
		}
	}
	void write(const byte_buf &v, uint off=0, int len=-1) {
		if (uint(len)>v.size()-off)
			len = v.size()-off;
		return write(&v[off], len);
	}

};

class BytesDataOutput : public DataOutput {
public:
	byte_buf buf;

	void writeByte(byte b) {
		buf.push_back(b);
	}

protected:
	virtual int tryWrite(const byte* c, int len) {
		for (int i=0; i<len; ++i) {
			buf.push_back(c[i]);
		}
		return len;
	}
};

class ostreamDataOutput : public DataOutput {
	std::ostream &out;

protected:
	virtual int tryWrite(const byte* c, int len) {
		 out.write ((const char*) c, len);
		 return len;
	}

public:
	ostreamDataOutput(std::ostream &out0) : out(out0) {}
	std::ostream& stream() {	return out; }
};

class DataInput {
protected:
	virtual int tryRead(byte* c, int len) = 0;
public:
	int read(byte* c, int len) {
		return tryRead(c, len);
	}
	bool readBoolean() {
		return readByte() != 0;
	}
	byte readByte() {
		byte c;
		readFully(&c, 1);
		return c;
	}
	uint32_t readInt() {
		uint32_t j;
		readFully((byte*) &j, 4);
		return ntohl(j);
	}

	void readFully(byte* c, int len) {
		int cnt=0;
		while (cnt<len) {
			int n = read(c+cnt, len-cnt);
			if (n==0) {
				throw EOFException();
			}
			cnt+=n;
		}
	}
	void readFully(byte_buf &c, int off=0, int len=-1) {
		if (len<0)
			len = c.size();
		else if (uint(len)>c.size())
			c.resize(len);
		return readFully(&c[off], len);
	}
};

class FDDataOutput : public DataOutput {
	int fd;
public:
	FDDataOutput(int fd0) : fd(fd0) {}

protected:
	virtual int tryWrite(const byte* c, int len) {
		int n = ::write(fd, c, len);
		if (n<0) {
			throw IOException(errno);
		}
		return n;
	}
};
class FDDataInput : public DataInput {
	int fd;
public:
	FDDataInput(int fd0) : fd(fd0) {}

protected:
	virtual int tryRead(byte* c, int len) {
		int n = ::read(fd, c, len);
		if (n<0) {
			throw IOException();
		}
		return n;
	}
};

class istreamDataInput : public DataInput {
	std::istream &in;

public:
	istreamDataInput(std::istream &in0) : in(in0) {}

protected:
	virtual int tryRead(byte* c, int len) {
		 return in.readsome((char*) c, len);
	}

public:
	std::istream& stream() { return in; }
};

#define D(X)

template<class T> static inline void writeVector(DataOutput *out, std::vector<T> &vec) {
	D("write vector of BigInt");
	out->writeInt(vec.size());
	for (uint i=0; i<vec.size(); ++i) {
		writeObject(out, vec[i]);
	}
}
template<class T> void writeVector(DataOutput *out, std::vector<std::vector<T> > &vec) {
	D("write vector of vector");
	out->writeInt(vec.size());
	for (uint i=0; i<vec.size(); ++i) {
		writeVector(out, vec[i]);
	}
}
template<> void writeVector(DataOutput *out, byte_buf &vec) {
	out->writeInt(vec.size());
	out->write(&vec[0], vec.size());
}

template<class T> static inline void readVector(DataInput *in, std::vector<T> &vec) {
	D("read vector of BigInt");
	int len = in->readInt();
	vec.resize(len);
	for (uint i=0; i<vec.size(); ++i) {
		readObject(in, vec[i]);
	}
}
template<class T> void readVector(DataInput *in, std::vector<std::vector<T> > &vec) {
	D("read vector of vector");
	int len = in->readInt();
	vec.resize(len);
	for (uint i=0; i<vec.size(); ++i) {
		readVector(in, vec[i]);
	}
}
template<> void readVector(DataInput *in, byte_buf &vec) {
	int len = in->readInt();
	vec.resize(len);
	in->readFully(&vec[0], vec.size());
}


std::string toBase64(const byte_buf &buf);
byte_buf fromBase64(const std::string str);
std::string toHexString(const byte_buf &buf);

#undef D

}
}

#include "sillysocket.h"

#endif /* SILLYIO_H_ */
