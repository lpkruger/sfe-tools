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
typedef unsigned int uint;
typedef unsigned char byte;
typedef unsigned char uchar;
typedef bool boolean;

typedef std::vector<byte> byte_buf;
typedef std::vector<bool> bit_vector;

template<class T, int dim> struct tensor {
	typedef std::vector<typename tensor<T, dim-1>::type > type;
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


}
}
using namespace silly::types;
using silly::types::uint;

#endif /* SILLYTYPE_H_ */
