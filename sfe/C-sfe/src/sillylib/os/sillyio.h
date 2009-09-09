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
#include <fstream>
#include <netinet/in.h>
#include <string.h>
//#include "../sillytype.h"
#include <errno.h>

namespace silly {
namespace io {

// IO stuff


struct IOException : public ErrnoException, public MsgBufferException {
	IOException(int err0=-1, const char *msg0="") : ErrnoException(err0), MsgBufferException(msg0) {
		if (err0>-1) {
			int len = strlen(msg);
			if (len < buflen-16) {
				const char *msg2 = ErrnoException::what();
				msg[len] = ':';
				msg[len+1] = ' ';
				strncpy(msg+len+2, msg2, buflen-len-2);
				msg[buflen-1]='\0';
			}
		}
	}
	IOException(const char *msg0) : ErrnoException(-1), MsgBufferException(msg0) {}
	virtual const char *what() const throw() {
		return MsgBufferException::what();
	}
};

struct EOFException : public IOException {
	EOFException() : IOException() {}
	const char *what() const throw () {
		return "<end of file>";
	}
};
struct ProtocolException : public IOException {
	ProtocolException(const char* msg0) : IOException(msg0) {}
	virtual const char* what() const throw() {
		return IOException::what();
	}
};


class DataOutput {
protected:
	virtual int tryWrite(const byte* c, int len) = 0;
public:
	ulong total;
	DataOutput() : total(0) {}

	virtual ~DataOutput() {}
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
				throw IOException();
			}
			cnt+=n;
			total+=n;
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

	//BytesDataOutput() : buf() {}
	void writeByte(byte b) {
		buf.push_back(b);
	}

protected:
	virtual int tryWrite(const byte* c, int len) {
		buf.insert(buf.end(), c, c+len);
		return len;
	}
};

class BufferedDataOutput : public DataOutput {
	DataOutput *under;
	byte_buf buffer;
	int pos;
public:
	BufferedDataOutput(DataOutput *u0, int bufsize=32*1024) : under(u0), pos(0) {
		buffer.resize(bufsize);
	}
protected:
	virtual int tryWrite(const byte* c, int len) {
		int free = buffer.size() - pos;
		if (free == 0) {
			flush();
			free = buffer.size();
		}
		int l = std::min(len, free);
		memcpy(&buffer[pos], c, l);
		pos += l;
		return l;
	}
	virtual void flush() {
		if (pos>0)
			under->write(buffer, 0, pos);
		pos = 0;
	}
	virtual void close() {
		flush();
		byte_buf().swap(buffer);
		under->close();
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
	ulong total;

	DataInput() : total(0) {}
	virtual ~DataInput() {}
	virtual void close() {};

	virtual void skip(int len) {
		byte c[len];
		readFully(c, len);
	}

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
			total+=n;
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

class BytesDataInput : public DataInput {
public:
	byte_buf buf;
	uint pos;

protected:
	virtual int tryRead(byte* c, int len0) {
		uint len = len0;
		if (buf.size()-pos > len) {
			memcpy(c, &buf[pos], len);
			pos += len;
			if (buf.size()-pos < 1024) { // arbitrary
				buf.erase(buf.begin(), buf.begin()+pos);
				pos = 0;
			}
			return len;
		} else {
			len = buf.size();
			memcpy(c, &buf[pos], len);
			buf.clear();
			return len;
		}
	}
};

class BufferedDataInput : public DataInput {
	DataInput *under;
	byte_buf buffer;
	int off;
	int pos;
public:
	BufferedDataInput(DataInput *u0, int bufsize=32*1024) : under(u0), off(0), pos(0) {
		buffer.resize(bufsize);
	}
protected:
	virtual int tryRead(byte* c, int len) {
		int nread = std::min(len, pos-off);
		if (pos>off) {
			memcpy(c, &buffer[off], nread);
			len -= nread;
			c += nread;
			off += nread;
		}
		if (len > 0) {
			pos = off = 0;
			if (len >= (int)buffer.size()) {
				nread += under->read(c, len);
			}  else {
				pos += under->read(&buffer[0], buffer.size());
				if (pos > 0) {
					nread += tryRead(c, len);
				}
			}
		}
		return nread;
	}
	virtual void close() {
		byte_buf().swap(buffer);
		under->close();

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



//template<class I> inline void writeIt(DataOutput *out, I it, I last) {
//	out->writeInt(last - it);
//	while(it != last) {
//		writeObject(out, *it);
//		++it;
//	}
//}

template<class T, class A> inline void writeVector(DataOutput *out, std::vector<T,A> &vec) {
	D("write vector of objects");
	out->writeInt(vec.size());
	for (uint i=0; i<vec.size(); ++i) {
		writeObject(out, vec[i]);
	}
}
template<class T, class A> inline void writeVector(DataOutput *out, std::vector<std::vector<T,A> > &vec) {
	D("write vector of vector");
	out->writeInt(vec.size());
	for (uint i=0; i<vec.size(); ++i) {
		writeVector(out, vec[i]);
	}
}
inline void writeVector(DataOutput *out, byte_buf &vec) {
	out->writeInt(vec.size());
	out->write(&vec[0], vec.size());
}

template<class T, class A> inline void readVector(DataInput *in, std::vector<T,A> &vec) {
	D("read vector of BigInt");
	int len = in->readInt();
	vec.resize(len);
	for (uint i=0; i<vec.size(); ++i) {
		readObject(in, vec[i]);
	}
}
template<class T, class A> inline void readVector(DataInput *in, std::vector<std::vector<T,A> > &vec) {
	D("read vector of vector");
	int len = in->readInt();
	vec.resize(len);
	for (uint i=0; i<vec.size(); ++i) {
		readVector(in, vec[i]);
	}
}
inline void readVector(DataInput *in, byte_buf &vec) {
	int len = in->readInt();
	vec.resize(len);
	in->readFully(&vec[0], vec.size());
}

namespace std_obj_rw {
// putting these in a seperate namespace prevents overloading problems
inline void writeObject(DataOutput *out, const int num) {
	out->writeInt(num);
}
inline void readObject(DataInput *in, int &num) {
	num = in->readInt();
}
template<class T> inline void writeObject(DataOutput *out, std::vector<T> *v) {
	writeVector(out, *v);
}
template<class T> inline void readObject(DataInput *in, std::vector<T> *v) {
	readVector(in, *v);
}
inline void writeObject(DataOutput *out, byte_buf &vec) {
	writeVector(out, vec);
}

inline void readObject(DataInput *in, byte_buf &vec) {
	readVector(in, vec);
}
inline void writeObject(DataOutput *out, std::string &vec) {
	byte_buf buf(vec.begin(), vec.end());
	writeVector(out, buf);
}

inline void readObject(DataInput *in, std::string &vec) {
	byte_buf buf;
	readVector(in, buf);
	vec.assign(buf.begin(), buf.end());
}

}

#undef D

}
}

#include "sillysocket.h"

#endif /* SILLYIO_H_ */
