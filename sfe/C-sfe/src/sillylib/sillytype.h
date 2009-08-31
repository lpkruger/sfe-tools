/*
 * sillytype.h
 *
 *  Created on: Aug 23, 2009
 *      Author: louis
 */

#ifndef SILLYTYPE_H_
#define SILLYTYPE_H_

#include "sillycommon.h"
#include <vector>
#include <map>
#include <iostream>

namespace silly {
namespace types {

using std::vector;
using std::map;

typedef unsigned int uint;
typedef unsigned char byte;
typedef unsigned char uchar;
typedef bool boolean;

typedef vector<byte> byte_buf;
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
