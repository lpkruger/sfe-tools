/*
 * sillydebug.h
 *
 *  Created on: Aug 28, 2009
 *      Author: louis
 */

#ifndef SILLYDEBUG_H_
#define SILLYDEBUG_H_


#ifndef DEBUG
template<class T> static inline void D(T x) {}
#define DD(x)
#define DC(x)
#define DF(x)
#else
#include <iostream>
#include <stdio.h>
#define DD(x) x
#define DC(x) std::cout << x << std::endl;
#define DF printf

static void D(const char* s) {
	fprintf(stderr, "%s\n", s);
}
static void D(const BigInt &n) {
	D(n.toString().c_str());
}

template<class T> static void D(vector<T> &vec) {
	fprintf(stderr, "[%u] ", vec.size());
	for (uint i=0; i<vec.size(); ++i) {
		fprintf(stderr, "%u: ", i);
		D(vec[i]);
	}
}
template<> void D(byte_buf &vec) {
	fprintf(stderr, "[%u: ", vec.size());
	for (uint i=0; i<vec.size(); ++i) {
		fprintf(stderr, "%02x ", (int)vec[i]);
	}
	fprintf(stderr, "]\n");
}
#endif

#endif /* SILLYDEBUG_H_ */
