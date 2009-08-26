/*
 * sillytype.h
 *
 *  Created on: Aug 23, 2009
 *      Author: louis
 */

#ifndef SILLYTYPE_H_
#define SILLYTYPE_H_

#include <vector>

namespace silly {
namespace types {
typedef unsigned int uint;
typedef unsigned char byte;
typedef unsigned char uchar;
typedef bool boolean;

template<class T> struct atype {
	typedef std::vector<T> vector;
	typedef vector array;
	typedef std::vector<array> matrix;
	typedef std::vector<matrix> cubic;
};

}
}
using namespace silly::types;
using silly::types::uint;

#endif /* SILLYTYPE_H_ */
