/*
 * sillysocket.h
 *
 *  Created on: Aug 23, 2009
 *      Author: louis
 */

#ifndef SILLYSOCKET_H_
#define SILLYSOCKET_H_

#include <sys/socket.h>
#include "sillyio.h"

namespace silly {
namespace net {


struct SocketException : public io::IOException {
	SocketException(const char* msg0) : IOException(errno, msg0) {}
};
struct ConnectException : public SocketException {
	ConnectException(const char* msg0) : SocketException(msg0) {}
};
struct BindException : public SocketException {
	BindException(const char* msg0) : SocketException(msg0) {}
};
struct UnknownHostException : public SocketException {
	UnknownHostException(const char* msg0) : SocketException(msg0) {}
};

using namespace silly::io;

class Socket {
	int fd;

public:
	Socket(int fd0 = -1) : fd(fd0) {}
	virtual ~Socket() {
		close();
	}
	Socket(const char* host, const char* port) {
		connect(host, port);
	}
	Socket(const char* host, int port) {
		connect(host, port);
	}

	void connect(const char* host, const char* port);

	void connect(const char* host, int port) {
		char buf[16];
		sprintf(buf, "%d", port);
		connect(host, buf);
	}

	void close() {
		if (fd>=0)
			::close(fd);
	}
	DataOutput *getOutput() {
		if (fd < 0)
			throw SocketException("getOutput: not connected");
		return new FDDataOutput(fd);
	}
	DataInput *getInput() {
		if (fd < 0)
			throw SocketException("getInput: not connected");
		return new FDDataInput(fd);
	}
};

#define LISTENQ 1024

class ServerSocket {
	int sfd;                /*  listening socket          */
public:

	ServerSocket(const char* port) {
		bind(port);
	}
	ServerSocket(int port) {
		bind(port);
	}
	void bind(const char* port);

	void bind(int port) {
		char buf[16];
		sprintf(buf, "%d", port);
		bind(buf);
	}
	Socket* accept();

	void close() {
		if (sfd>=0) {
			::close(sfd);
			sfd = -1;
		}
	}

	virtual ~ServerSocket() {
		close();
	}


};

}
}
#endif /* SILLYSOCKET_H_ */
