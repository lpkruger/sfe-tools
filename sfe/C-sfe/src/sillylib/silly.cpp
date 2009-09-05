/*
 * silly.cpp
 *
 *  Created on: Aug 17, 2009
 *      Author: louis
 */

#include "silly.h"
#include "sillyio.h"
#include "sillysocket.h"
#include <netdb.h>
#include <string.h>
#include <sys/time.h>
#include <stdlib.h>

using std::vector;
using std::string;

object_counter byte_buf::counter("byte_buf");

long silly::misc::currentTimeMillis() {
	struct timeval tv;
	gettimeofday(&tv, NULL);
	long ret = tv.tv_sec*1000;
	ret += (tv.tv_usec/1000);
	return ret;
}

int silly::misc::numCPUs() {
	int cpu_count=1;
	std::ifstream cpuinfo("/proc/cpuinfo");
	string line;
	while (!cpuinfo.eof()) {
		std::getline(cpuinfo, line);
		const char* processor = "processor";
		if (line.substr(0, strlen(processor)) == processor) {
			//std::cout << line << std::endl;
			uint space = line.rfind(' ');
			if (space != line.npos) {
				int num = strtol(line.substr(space+1).c_str(), NULL, 10);
				++num;
				if (num > cpu_count)
					cpu_count = num;
			}
		}
	}
	return cpu_count;
}

string silly::misc::toBase64(const byte_buf &buf) {
	const char *table = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/";

	int remainder = buf.size()%3;
	uint end = buf.size()-remainder;
	char outbuf[(buf.size()+2)/3*4+1];
	char *outp = outbuf;

	uint yy;
	uint i;
	for (i=0; i<end; i+=3) {
		yy = (buf[i]<<16) | (buf[i+1]<<8) | (buf[i+2]);
		for (int j=0; j<4; ++j) {
			*outp++ = (table[(yy>>18) & 0x3F]);
			yy <<= 6;
		}
	}
	switch(remainder) {
	case 2:
		yy = (buf[i]<<16) | (buf[i+1]<<8);
		for (int j=0; j<3; ++j) {
			*outp++ = (table[(yy>>18) & 0x3F]);
			yy <<= 6;
		}
		*outp++ = '=';
		break;
	case 1:
		yy |= buf[i]<<16;
		for (int j=0; j<2; ++j) {
			*outp++ = (table[(yy>>18) & 0x3F]);
			yy <<= 6;
		}
		*outp++ = '=';
		*outp++ = '=';
		break;
	default:
		break;
	}
	*outp = '\0';
	return string(outbuf);
}


std::string silly::misc::toHexString(const byte_buf &buf) {
	if (!buf.size())
		return "<empty>";
#if 1
	string s(3*buf.size()+1, ' ');
	for (uint i=0; i<buf.size(); ++i) {
		sprintf(&s[3*i], "%02x ", buf[i]);
	}
	s.resize(3*buf.size()-1);
	return s;
#else
	string s;
	char sss[1024];
	for (uint i=0; i<buf.size(); ++i) {
		snprintf(sss, 1022, "%02x ", buf[i]);
		s.append(sss);
	}
	return s;
#endif
}


vector<string> silly::misc::split(string &s, string &d) {
  vector<string> ret;
  size_t i=0;
  while(i != string::npos) {
    size_t j = s.find(d, i);
    ret.push_back(s.substr(i, j-i));
    i = (j == string::npos ? j : j+1);
  }
  return ret;
}

#if 0 // export templates not supported
template<class T> void resize(vector<T> &v, int d1) {
	v.resize(d1);
}
template<class T> void resize(vector<vector<T> > &v, int d1, int d2) {
	v.resize(d1);
	for (int i=0; i<d1; ++i) {
		v[i].resize(d2);
	}
}
template<class T> void resize(vector<vector<vector<T> > > &v, int d1, int d2, int d3) {
	v.resize(d1);
	for (int i=0; i<d1; ++i) {
		v[i].resize(d2);
		for (int j=0; j<d2; ++j) {
			v[i][j].resize(d3);
		}
	}
}
#endif


void silly::net::Socket::connect(const char* host, const char* port) {
	struct addrinfo hints;
	struct addrinfo *result, *rp;
	int sfd, s;

	/* Obtain address(es) matching host/port */
	memset(&hints, 0, sizeof(struct addrinfo));
	hints.ai_family = AF_UNSPEC;    /* Allow IPv4 or IPv6 */
	hints.ai_socktype = SOCK_STREAM; /* TCP socket */
	hints.ai_flags = 0;
	hints.ai_protocol = 0;          /* Any protocol */

	s = getaddrinfo(host, port, &hints, &result);
	if (s != 0) {
		throw UnknownHostException(misc::string_printf("getaddrinfo: %s\n", gai_strerror(s)).c_str());
	}

	/* getaddrinfo() returns a list of address structures.
          Try each address until we successfully connect(2).
          If socket(2) (or connect(2)) fails, we (close the socket
          and) try the next address. */

	for (rp = result; rp != NULL; rp = rp->ai_next) {
		sfd = ::socket(rp->ai_family, rp->ai_socktype,
				rp->ai_protocol);
		if (sfd == -1)
			continue;

		if (::connect(sfd, rp->ai_addr, rp->ai_addrlen) != -1) {
			this->fd = sfd;
			break;
		}

		::close(sfd);
	}

	freeaddrinfo(result);           /* No longer needed */

	if (rp == NULL) {               /* No address succeeded */
		throw ConnectException("Could not connect");
	}

}

silly::net::ServerSocket::ServerSocket(short port) {
	struct sockaddr_in servaddr;  /*  socket address structure  */

	/*  Create the listening socket  */

	if ( (list_s = socket(AF_INET, SOCK_STREAM, 0)) < 0 ) {
		fprintf(stderr, "ECHOSERV: Error creating listening socket.\n");
		throw SocketException("can't create server socket");
	}

	int on = 1;
	//printf("setsockopt(SO_REUSEADDR)\n");
	setsockopt(list_s, SOL_SOCKET, SO_REUSEADDR, &on, sizeof(on));


	memset(&servaddr, 0, sizeof(servaddr));
	servaddr.sin_family      = AF_INET;
	servaddr.sin_addr.s_addr = htonl(INADDR_ANY);
	servaddr.sin_port        = htons(port);


	/*  Bind our socket addresss to the
					listening socket, and call listen()  */

	if ( ::bind(list_s, (struct sockaddr *) &servaddr, sizeof(servaddr)) < 0 ) {
		throw BindException("can't bind server socket");
	}
	if ( ::listen(list_s, LISTENQ) < 0 ) {
		throw BindException("can't listen server socket");
	}
}

silly::net::Socket* silly::net::ServerSocket::accept() {
	int conn_s;                /*  connection socket         */
	if ( (conn_s = ::accept(list_s, NULL, NULL) ) < 0 ) {
		throw SocketException("can't accept connection");
	}
	return new Socket(conn_s);
}



//#include <stdio.h>
//#include <stdlib.h>
#include <stdarg.h>

// copied from man vsnprintf
string silly::misc::string_printf(const char *fmt, ...) {
	byte_buf buf(256);
	va_list ap;
	int n;
	while (1) {
		/* Try to print in the allocated space. */
		va_start(ap, fmt);
		n = vsnprintf((char*)&buf[0], buf.size(), fmt, ap);
		va_end(ap);
		/* If that worked, return the string. */
		if (n > -1 && uint(n) < buf.size())
			return string((char*)&buf[0]);
		/* Else try again with more space. */
		if (n > -1)    /* glibc 2.1 */
			buf.resize(n+1); /* precisely what is needed */
		else           /* glibc 2.0 */
			buf.resize(buf.size()*2);  /* twice the old size */

	}
}

//extern uint ::pthread_self();

void debug_printf(const char *fmt, ...) {
	fprintf(stderr, "tid %08x  ", (uint) pthread_self());
	va_list ap;
	int n;
	va_start(ap, fmt);
	n = vfprintf(stderr, fmt, ap);
	va_end(ap);
	fprintf(stderr, "\n");
}

//// backtrace function ////
#include <stdio.h>
#include <stdlib.h>
#include <execinfo.h>

void print_backtrace(int depth, const char *msg0){
	void *addresses[depth+1];
	char **strings;

	int size = backtrace(addresses, depth+1);
	strings = backtrace_symbols(addresses, size);
	const char *msg = msg0 ? msg0 : "";
	fprintf(stderr, "%s  stack frames: %d\n", msg, size-1);
	for(int i = 1; i < size; i++)
	{
		fprintf(stderr, "%d: %08X\t", i, (int)addresses[i]);
		fprintf(stderr, "%s\n", strings[i]);
	}
	free(strings);
}
///////////////////////////
