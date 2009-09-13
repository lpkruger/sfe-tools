/*
 * Parser.h
 *
 *  Created on: Aug 17, 2009
 *      Author: louis
 */

#ifndef SILLYLIB_H_
#define SILLYLIB_H_

#include <vector>
//#include <set>
//#include <map>
#include <string>
#include <sstream>
#include <iosfwd>
#include <memory>
#include "sillytype.h"

using std::ostream;

namespace silly {
namespace misc {
using std::string;
using std::vector;

namespace stupid {
template<class T> static inline string toString(T t) {
	std::stringstream out;
	out << t;
	return out.str();
}

template<class T> static inline string operator + (string s, T t) {
  return s + toString(t);
}
}
static inline string operator +(string s, int x) {
	return s+stupid::toString(x);
}

class stringable {
public:
	virtual ~stringable() {}
	virtual string toString() = 0;
};

static inline string operator + (string &s, stringable &x) {
	return s + x.toString();
}
static inline ostream& operator << (ostream &s, stringable &x) {
	s << x.toString();
	return s;
}
static inline string operator + (string &s, stringable *x) {
	return s + x->toString();
}
static inline ostream& operator << (ostream &s, stringable *x) {
	s << x->toString();
	return s;
}

std::vector<string> split(string &s, string &d);
static inline std::vector<string> split(const char *s, const char *d) {
	string ss(s);
	string dd(d);
	return split(ss, dd);
}
static inline std::vector<string> split(string &s, const char *d) {
	string dd(d);
	return split(s, dd);
}

#if 0 // export templates not supported
export template<class T> void resize(vector<T> &v, int d1);
export template<class T> void resize(vector<vector<T> > &v, int d1, int d2);
export template<class T> void resize(vector<vector<vector<T> > > &v, int d1, int d2, int d3);
#else
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

#if 0
template<class T> class smart {
public:
	typedef wise_ptr<vector<T> > vec;
	typedef wise_ptr<T> ptr;
};
#endif

string string_printf(const char *fmt, ...)
	__attribute__ ((format (printf, 1, 2)));

#define cstr_printf(fmt, ...) \
	string_printf(fmt, __VA_ARGS__).c_str()

long currentTimeMillis();
int numCPUs();

string toBase64(const byte_buf &buf);
byte_buf fromBase64(const std::string str);
string toHexString(const byte_buf &buf);

}
}
#endif

