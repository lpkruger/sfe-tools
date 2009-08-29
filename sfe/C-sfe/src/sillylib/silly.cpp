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

using std::vector;
using std::string;


long silly::misc::currentTimeMillis() {
	struct timeval tv;
	gettimeofday(&tv, NULL);
	long ret = tv.tv_sec*1000;
	ret += (tv.tv_usec/1000);
	return ret;
}

string silly::io::toBase64(const byte_buf &buf) {
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


std::string silly::io::toHexString(const byte_buf &buf) {
	string s(3*buf.size()+1, ' ');
	for (uint i=0; i<buf.size(); ++i) {
		sprintf(&s[3*i], "%02x ", buf[i]);
	}
	s.resize(3*buf.size()-1);
	return s;
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

		close(sfd);
	}

	freeaddrinfo(result);           /* No longer needed */

	if (rp == NULL) {               /* No address succeeded */
		throw ConnectException("Could not connect");
	}

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
