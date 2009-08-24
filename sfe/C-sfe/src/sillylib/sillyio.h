/*
 * sillyio.h
 *
 *  Created on: Aug 22, 2009
 *      Author: louis
 */

#ifndef SILLYIO_H_
#define SILLYIO_H_

#include <netinet/in.h>
#include <string.h>
#include <sys/socket.h>

namespace silly {


// IO stuff
class DataOutput {
public:
	virtual void writeBoolean(bool b) {
		writeByte(b ? 0xFF : 0);
	}
	virtual void writeByte(byte b) {
		writeFully(&b, 1);
	}
	virtual void writeInt(uint32_t i) {
		uint32_t j = htonl(i);
		writeFully((const byte*) &j, 4);
	}

	virtual int write(const byte* c, int len) = 0;

	virtual void writeFully(const byte* c, int len) {
		int cnt=0;
		while (cnt<len) {
			int n = write(c+cnt, len-cnt);
			if (n<0) {
				// TODO: throw
			}
			if (n==0) {
				// TODO: throw EOF
			}
			cnt+=n;
		}
	}
	void writeFully(const vector<byte> &v) {
		return writeFully(&v[0], v.size());
	}
	virtual void flush() {}
	virtual void close() {}
};

class BytesDataOutput : public DataOutput {
public:
	vector<byte> buf;

	virtual void writeByte(byte b) {
		buf.push_back(b);
	}

	virtual int write(const byte* c, int len) {
		for (int i=0; i<len; ++i) {
			buf.push_back(c[i]);
		}
		return len;
	}
};

class DataInput {
public:
	virtual bool readBoolean() {
		return readByte() != 0;
	}
	virtual byte readByte() {
		byte c;
		readFully(&c, 1);
		return c;
	}
	virtual uint32_t readInt() {
		uint32_t j;
		readFully((byte*) &j, 4);
		return ntohl(j);
	}
	virtual int read(byte* c, int len) = 0;
	virtual void readFully(byte* c, int len) {
		int cnt=0;
		while (cnt<len) {
			int n = read(c+cnt, len-cnt);
			if (n<0) {
				// TODO: throw
			}
			if (n==0) {
				// TODO: throw EOF
			}
			cnt+=n;
		}
	}
	virtual void readFully(vector<byte> c) {
		return readFully(&c[0], c.size());
	}
};

class FDDataOutput : public DataOutput {
	int fd;
public:
	FDDataOutput(int fd0) : fd(fd0) {}

	virtual int write(const byte* c, int len) {
		int n = ::write(fd, c, len);
		if (n<0) {
			// throw an exception
		}
		return n;
	}
};
class FDDataInput : public DataInput {
	int fd;
public:
	FDDataInput(int fd0) : fd(fd0) {}

	virtual int read(byte* c, int len) {
		int n = ::read(fd, c, len);
		if (n<0) {
			// throw an exception
		}
		return n;
	}
};

#define D(X)

template<class T> void writeVector(DataOutput *out, vector<T> &vec) {
	D("write vector of BigInt");
	out->writeInt(vec.size());
	for (uint i=0; i<vec.size(); ++i) {
		writeObject(out, vec[i]);
	}
}
template<class T> void writeVector(DataOutput *out, vector<vector<T> > &vec) {
	D("write vector of vector");
	out->writeInt(vec.size());
	for (uint i=0; i<vec.size(); ++i) {
		writeVector(out, vec[i]);
	}
}

template<class T> void readVector(DataInput *in, vector<T> &vec) {
	D("read vector of BigInt");
	int len = in->readInt();
	vec.resize(len);
	for (uint i=0; i<vec.size(); ++i) {
		readObject(in, vec[i]);
	}
}
template<class T> void readVector(DataInput *in, vector<vector<T> > &vec) {
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
