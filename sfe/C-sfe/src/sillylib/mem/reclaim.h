/*
 * reclaim.h
 *
 *  Created on: Sep 2, 2009
 *      Author: louis
 */

#ifndef RECLAIM_H_
#define RECLAIM_H_

#include <vector>
// a context that is responsible for deleting an object

namespace silly {
namespace mem {

class Reclaimer {
	template<class T> static void callDelete(T* a) {
		delete a;
	}
	template<class T> struct functoid {
		typedef void (*type)(T*);
	};
	template<class T> static inline typename functoid<T>::type getDeleter() {
		return callDelete;
	}
	struct pair {
		void *ptr;
		functoid<void>::type fn;
	};
	std::vector<pair> garbage_bin;

public:

	template<class T> void add_garbage(T* ptr) {
		pair p = {
				reinterpret_cast<void*> (ptr),
				reinterpret_cast<functoid<void>::type> (getDeleter<T>())
		};
		garbage_bin.push_back(p);
	}

	void empty_garbage() {
		while (!garbage_bin.empty()) {
			pair p = garbage_bin.back();
			garbage_bin.pop_back();
			p.fn(p.ptr);
		}
	}

	virtual ~Reclaimer() {
		empty_garbage();
	}
};

}
}
#endif /* RECLAIM_H_ */
