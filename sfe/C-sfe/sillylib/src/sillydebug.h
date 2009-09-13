/*
 * sillydebug.h
 *
 *  Created on: Aug 28, 2009
 *      Author: louis
 */

#ifndef SILLYDEBUG_H_
#define SILLYDEBUG_H_

#ifndef SILLYMEM_H_			// also declares this
void print_backtrace(int depth=10, const char *msg=0);
#endif

#if !defined(DEBUG) && !defined(DEBUG2)
#define D(...) do {} while(0)
#define NDEBUG 1
//#define D D_OFF
//template<class T> static inline void D_OFF(T x, int lev=0) {}
#define DD(x) do {} while(0)
#define DC(x) do {} while(0)
//static inline void sillydebug_dummy(...) {}
//#define DF sillydebug_dummy
#define DF(...) do {} while(0)
#else
#include <iostream>
#include <stdio.h>
#include <stdarg.h>
#include "sillytype.h"
#define D D_ON
#define DD(x) x
#define DC(x) std::cerr << x << std::endl;
#define DF debug_printf

#ifndef SILLYTHREAD_H_		// also declares this
void debug_printf(const char *fmt, ...)
	__attribute__ ((format (printf, 1, 2)));
#endif

static inline void Dlevel(int lev) {
	char out[lev+1];
	memset(out, ' ', lev);
	out[lev] = '\0';
	fprintf(stderr, "%s", out);
}

static inline void D(const char* s, int lev=0) {
	Dlevel(lev);
	fprintf(stderr, "%s\n", s);
}

#ifdef BIGINT_H_
static inline void D(const bigint::BigInt &n, int lev=0) {
	Dlevel(lev);
	D(n.toString().c_str());
}
#endif

namespace debug_vector {
template<class T,class A=std::allocator<T> > static void D(const vector<T,A> &vec, int lev=0) {
	Dlevel(lev);
	fprintf(stderr, "[%u]:\n ", vec.size());
	for (uint i=0; i<vec.size(); ++i) {
		Dlevel(lev+2);
		fprintf(stderr, "-- %u --\n", i);
		D(vec[i], lev+4);
	}
}
template<> void D(const vector<int> &vec, int lev) {
	Dlevel(lev);
	fprintf(stderr, "[%u: ", vec.size());
	for (uint i=0; i<vec.size(); ++i) {
		fprintf(stderr, "%d ", vec[i]);
	}
	fprintf(stderr, "]\n");
}
}

using debug_vector::D;

static inline void D(const byte_buf &vec, int lev) {
	Dlevel(lev);
	fprintf(stderr, "[%u: ", vec.size());
	for (uint i=0; i<vec.size(); ++i) {
		fprintf(stderr, "%02x ", (int)vec[i]);
	}
	fprintf(stderr, "]\n");
}
#endif

#endif /* SILLYDEBUG_H_ */
