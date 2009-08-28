/*
 * sillytype.h
 *
 *  Created on: Aug 23, 2009
 *      Author: louis
 */

#ifndef SILLYTYPE_H_
#define SILLYTYPE_H_

#include <vector>
#include <map>
#include <iostream>

namespace silly {
namespace types {
//typedef unsigned int uint;
typedef unsigned char byte;
typedef unsigned char uchar;
typedef bool boolean;

typedef std::vector<byte> byte_buf;


template<class T> struct atype {
	typedef std::vector<T> vector;
	typedef vector array;
	typedef std::vector<array> matrix;
	typedef std::vector<matrix> cubic;
};

// use this for checking arguments passed to a function
class bad_argument : public std::exception {
	const char* msg;
public:
	bad_argument(const char *msg0) : msg(msg0) {}
	virtual const char *what() {
		return msg;
	}
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
//using silly::types::uint;

#endif /* SILLYTYPE_H_ */
