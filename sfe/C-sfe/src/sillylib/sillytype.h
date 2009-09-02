/*
 * sillytype.h
 *
 *  Created on: Aug 23, 2009
 *      Author: louis
 */

#ifndef SILLYTYPE_H_
#define SILLYTYPE_H_

#include <gc/gc.h>
#include <gc/gc_allocator.h>
#include "sillycommon.h"
//#include "sillymem.h"		// for backtrace
#include <vector>
#include <map>
#include <iostream>

namespace std {
struct nullptr_t {
	template<class T>
	operator T*() const { return 0; }
	template<class T, class U>
	operator T U::*() const { return 0; }
	operator bool() const { return false; }
	void* const __dummy_zero_value;
};
}

const std::nullptr_t nullptr = {0};

namespace silly {
namespace types {

using std::vector;
using std::map;

typedef unsigned int uint;
typedef unsigned char byte;
typedef unsigned char uchar;
typedef bool boolean;

struct copy_counter {
	const char *name;
	int copy_count;
	int move_count;
	int eq_count;
	int refeq_count;

	copy_counter(const char *n) : name(n) {}
	~copy_counter() {
		fprintf(stderr, "%s:  con& %d   con&& %d\n=& %d   =&& %d\n",
				name, copy_count, move_count, eq_count, refeq_count);
	}
};

struct object_counter : public copy_counter {
	int con_count;
	int des_count;
	object_counter(const char *n) : copy_counter(n) {}
	~object_counter() {
		fprintf(stderr, "%s: con %d   des %d\n",
				name, con_count, des_count);
	}
};

/* print_backtrace(5, #type); */
#define COPY_COUNTER(type) \
		type(const type &copy) { ++counter.copy_count; } \
		type(type &&move) { ++counter.move_count; } \
		type& operator=(const type &copy) { \
			++counter.eq_count; \
			return *this; } \
		type& operator=(type &&copy) { \
			++counter.refeq_count; \
			return *this; }

#define COPY_COUNTER_DERIVED(type, super) \
		type(const type &copy, super) : super(copy) { ++counter.copy_count;  } \
		type(type &&move) : super(move) { ++counter.move_count; } \
		type& operator=(const type &copy) { \
			super::operator=(copy); \
			++counter.eq_count; \
			return *this; } \
		type& operator=(type &&copy) { \
			super::operator=(copy); \
			++counter.refeq_count; \
			return *this; }


//typedef vector<byte, gc_allocator<byte> > byte_buf;
class byte_buf : public vector<byte> {
	typedef vector<byte> super;
	static object_counter counter;
public:
	explicit byte_buf() : super() { ++counter.con_count; }
	explicit byte_buf(size_type size) : super(size) { ++counter.con_count; }
	explicit byte_buf(size_type size, byte value) : super(size, value) { ++counter.con_count; }
	explicit byte_buf(super::const_iterator first, super::const_iterator last)
			: super(first, last) { ++counter.con_count; }
	COPY_COUNTER_DERIVED(byte_buf, super)
	~byte_buf() { ++counter.des_count; }

};


typedef vector<bool> bit_vector;

template<class T, int dim> struct tensor {
	typedef vector<typename tensor<T, dim-1>::type > type;
};
template<class T> struct tensor<T, 0> {
	typedef T type;
};

template<class T, int dim=0> struct atype {
	typedef typename tensor<T,1>::type vector;
	typedef typename tensor<T,2>::type matrix;
	typedef typename tensor<T,3>::type cubic;
	typedef typename tensor<T,dim>::type tensor;
};

// use this for checking arguments passed to a function
struct bad_argument : public MsgBufferException {
	bad_argument(const char *msg0) : MsgBufferException(msg0) {}
};

#if 0
template<class T,class U> static inline T* map_get(std::map<U,T> &map, const U &key) {
	std::cout << "map_get: generic overload" << std::endl;
	typedef typename std::map<U,T>::iterator map_it;
	map_it it = map.find(key);
	if (it == map.end())
		return NULL;
	return &it->second;
}
template<class T,class U> static inline T* map_get(std::map<U,T*> &map, const U &key) {
	std::cout << "map get: ptr overload" << std::endl;
	typedef typename std::map<U,T*>::iterator map_it;
	map_it it = map.find(key);
	if (it == map.end())
		return NULL;
	return it->second;
}
#endif

// access a vector through a permutation
template<class T> class vector_perm {
	vector<T> &the;
	vector<int> perm;
public:
	vector_perm(vector<T> &v, vector<int> &p) :
		the(v), perm(p) {}

	T& at(int i) {
		return the.at(perm.at(i));
	}
	T& operator[] (int i) {
		return the.at(perm.at(i));
	}
	uint size() {
		return perm.size();
	}
	vector<int> getPerm() {
		return perm;
	}
	class iterator {
		vector_perm &v;
		int pos;
	public:
		iterator(vector_perm &v0, int p0) : v(v0), pos(p0) {}
		T& operator*() {
			return v[pos];
		}
		T* operator->() {
			return &v[pos];
		}
		int operator-(const iterator &o) const {
			return pos - o.pos;
		}
		bool operator==(const iterator &o) const {
			return pos==o.pos && &v == &o.v;
		}
		bool operator!=(const iterator &o) const {
			return ! operator==(o);
		}
		iterator& operator++() {
			++pos;
			return *this;
		}
		iterator operator++(int) {
			iterator ret(v,pos);
			++pos;
			return ret;
		}
	};

	iterator begin() {
		return iterator(*this, 0);
	}
	iterator end() {
		return iterator(*this, the.size());
	}
	vector_perm inverse_perm() {
		vector<int> invperm(the.size(), -1);
		for (uint i=0; i<perm.size(); ++i) {
			invperm.at(perm[i]) = i;
		}
		return vector_perm(the, invperm);
	}
	vector_perm negate_perm() {
		vector<bool> mark(the.size());
		for (uint i=0; i<perm.size(); ++i) {
			mark.at(perm[i]) = true;
		}
		vector<int> negperm;
		for (uint i=0; i<mark.size(); ++i) {
			if (!mark[i] )
				negperm.push_back(i);
		}
		return vector_perm(the, negperm);
	}
};


}
}
using namespace silly::types;
using silly::types::uint;

#endif /* SILLYTYPE_H_ */
