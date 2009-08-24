/*
 * sillysocket.h
 *
 *  Created on: Aug 23, 2009
 *      Author: louis
 */

#ifndef SILLYSOCKET_H_
#define SILLYSOCKET_H_

#include <sys/socket.h>

class Socket {
	int fd;
public:
	Socket(int fd0 = -1) : fd(fd0) {}
	virtual ~Socket() {
		if (fd>=0)
			::close(fd);
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

	DataOutput *getOutput() {
		if (fd < 0)
			return NULL;  // TODO throw;
		return new FDDataOutput(fd);
	}
	DataInput *getInput() {
		if (fd < 0)
			return NULL;  // TODO throw;
		return new FDDataInput(fd);
	}
};

#define LISTENQ 1024

class ServerSocket {
	int list_s;                /*  listening socket          */
public:
	ServerSocket(short port) {
		struct sockaddr_in servaddr;  /*  socket address structure  */

		/*  Create the listening socket  */

		if ( (list_s = socket(AF_INET, SOCK_STREAM, 0)) < 0 ) {
			fprintf(stderr, "ECHOSERV: Error creating listening socket.\n");
			// TODO throw
		}
		memset(&servaddr, 0, sizeof(servaddr));
		servaddr.sin_family      = AF_INET;
		servaddr.sin_addr.s_addr = htonl(INADDR_ANY);
		servaddr.sin_port        = htons(port);


		/*  Bind our socket addresss to the
					listening socket, and call listen()  */

		if ( ::bind(list_s, (struct sockaddr *) &servaddr, sizeof(servaddr)) < 0 ) {
			fprintf(stderr, "ECHOSERV: Error calling bind()\n");
			// TODO throw
		}
		if ( listen(list_s, LISTENQ) < 0 ) {
			fprintf(stderr, "ECHOSERV: Error calling listen()\n");
			// TODO throw
		}
	}
	Socket* accept() {
		int conn_s;                /*  connection socket         */
		if ( (conn_s = ::accept(list_s, NULL, NULL) ) < 0 ) {
			fprintf(stderr, "ECHOSERV: Error calling accept()\n");
			// TODO throw
		}
		return new Socket(conn_s);
	}

	void close() {
		::close(list_s);
		list_s = -1;
	}
};

#endif /* SILLYSOCKET_H_ */
