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
class EOFException : public IOException {
	virtual const char *what() const throw () {
		return "<end of file>";
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
			int n = tryWrite(c+cnt, len-cnt);
			if (n==0) {
				throw EOFException();
			}
			cnt+=n;
		}
	}
	void write(const std::vector<byte> &v) {
		return write(&v[0], v.size());
	}

};

class BytesDataOutput : public DataOutput {
public:
	std::vector<byte> buf;

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
	ostreamDataOutput(std::ostream &out0) : out(out0) {}

protected:
	virtual int tryWrite(const byte* c, int len) {
		 out.write ((const char*) c, len);
		 return len;
	}

public:
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
	void readFully(std::vector<byte> c, int off=0, int len=-1) {
		if (len==-1)
			len = c.size();
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
	istreamDataInput(std::istream &in0) : in(in0) {}

protected:
	virtual int tryRead(byte* c, int len) {
		 return in.readsome((char*) c, len);
	}

public:
	std::istream& stream() { return in; }
};

#define D(X)

template<class T> void writeVector(DataOutput *out, std::vector<T> &vec) {
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

template<class T> void readVector(DataInput *in, std::vector<T> &vec) {
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

#undef D

#include "sillysocket.h"


}
#endif /* SILLYIO_H_ */
