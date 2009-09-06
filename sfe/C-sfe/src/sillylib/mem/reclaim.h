/*
 * reclaim.h
 *
 *  Created on: Sep 2, 2009
 *      Author: louis
 */

#ifndef RECLAIM_H_
#define RECLAIM_H_

#include <vector>
	// a context that is responsible for deleting objects

namespace silly {
namespace mem {

template<class T = void> class Reclaimer {
	std::vector<T*> garbage_bin;
	NOCOPY(Reclaimer)
public:
	Reclaimer() {}
	void add_garbage(T* ptr) {
		garbage_bin.push_back(ptr);
	}

	void empty_garbage() {
		while (!garbage_bin.empty()) {
			T *p = garbage_bin.back();
			garbage_bin.pop_back();
			delete p;
		}
	}

	virtual ~Reclaimer() {
		empty_garbage();
	}
};

template<> class Reclaimer<void> {
	template<class U> struct functoid {
		typedef void (*type)(U*);
	};
	struct pair {
		void *ptr;
		functoid<void>::type fn;
	};

	std::vector<pair> garbage_bin;

	template<class U> static void callDelete(U* a) {
		delete a;
	}
	template<class U> static inline typename functoid<U>::type getDeleter() {
		return callDelete;
	}

	NOCOPY(Reclaimer)
public:

	Reclaimer() {}

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
