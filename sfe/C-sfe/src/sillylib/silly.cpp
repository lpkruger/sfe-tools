/*
 * silly.cpp
 *
 *  Created on: Aug 17, 2009
 *      Author: louis
 */

#include "silly.h"
#include "sillyio.h"
#include "sillysocket.h"
#include "sillymem.h"
#include <netdb.h>
#include <string.h>

using std::vector;
using std::string;

void silly::mem::wise_void_ptr::set(wise_void_ptr &p0) {
	p = p0.p;
	if ((p0.next == NULL) && (p0.prev == NULL)) {
		p0.next = p0.prev = this;
		next = prev = &p0;
	} else {
		next = p0.next;
		prev = &p0;
		prev->next = this;
		next->prev = this;
	}
}
void* silly::mem::wise_void_ptr::unref() {
	// unhook object, return ptr if last copy
	if ((next==NULL) && (prev==NULL)) {
		void* ptr = p;
		p = NULL;
		return ptr;
	}
	if (next==prev) {
		next->next = NULL;
		next->prev = NULL;
		next = prev = NULL;
		return (p = NULL);
	}
	next->prev = prev;
	prev->next = next;
	next = prev = NULL;
	return (p = NULL);
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
		fprintf(stderr, "getaddrinfo: %s\n", gai_strerror(s));
		// TODO throw
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

	if (rp == NULL) {               /* No address succeeded */
		fprintf(stderr, "Could not connect\n");
		// TODO throw exit(EXIT_FAILURE);
	}

	freeaddrinfo(result);           /* No longer needed */
}
