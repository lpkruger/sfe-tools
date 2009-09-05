/*
 * sillymem.h
 *
 *  Created on: Aug 25, 2009
 *      Author: louis
 */

//#if 0
#ifndef SILLYMEM_H_
#define SILLYMEM_H_

//#define USE_STD_SHARED_PTR 1

namespace silly {
namespace mem {

// semantics of a regular pointer in class form
template<class T> class basic_ptr {
	T *p;
public:
	basic_ptr() : p(NULL) {}
	basic_ptr(T* p0) : p(p0) {}
	// default copy constructor is fine

	basic_ptr& operator=(T* p0) { p = p0; return *this; }
	// default copy assignment is fine

	//T& operator*() { return *p; }
	T* operator->() const { return p; }
	T* get() const { return p; }
	T* ptr() const { return get(); }

	operator T*() const { return p; }		// OK since this is really interchangable
									// gives us almost everything else for free

	basic_ptr<T>& operator++() { ++p; return *this; }
	basic_ptr<T> operator++(int) { T* p0 = p++; return p0; }
	basic_ptr<T>& operator+=(int n) { p+=n; return *this; }
	basic_ptr<T>& operator--() { --p; return *this; }
	basic_ptr<T> operator--(int) { T* p0 = p--; return p0; }
	basic_ptr<T>& operator-=(int n) { p-=n; return *this; }
//	basic_ptr<T> operator+(int) { T* p0 = p--; return p0; }
//	basic_ptr<T> operator-(int) { T* p0 = p--; return p0; }
//	int operator-(T*) { T* p0 = p--; return p0; }
};

}
}

#if USE_STD_SHARED_PTR
#define wise_ptr shared_ptr
#define to_ptr() get()

#else
//#define DEBUG_WISEPTR 1
#define CHECK_NULL_PTR 0

#if DEBUG_WISEPTR
#include <iostream>
#define DCC(x) std::cout << x
#else
#define DCC(x)
#endif

#ifndef SILLYDEBUG_H_
void print_backtrace(int depth=10, const char *msg=NULL);
#endif

//#include "sillydebug.h"

namespace silly {
namespace mem {

struct null_pointer : public MsgBufferException {
	null_pointer(const char *msg) : MsgBufferException(msg) {}
};
// auto-freeing reference-counting pointers.  yay.
// wise_ptr struct uses 4 bytes vs 8 bytes for C++/Boost shared_ptr
// therefore mine is better

template<class T> class wise_ptr;

template<class T> class wise_ptr_common {
	T* p;
	int refcnt;
	// int weakrefcnt;
	wise_ptr_common(T* p0) : p(p0), refcnt(1) {}

	void dealloc() {	// TODO: overload
		DCC("dealloc " << p << " <- " << this << " " << refcnt << std::endl);
		delete p;
		delete this;	// TODO check for weak refs
	}

	friend class wise_ptr<T>;
};

template<class T> class wise_ptr {
	typedef wise_ptr<T> Twptr;
	typedef wise_ptr_common<T> Tcommon;
	Tcommon *cp;

	void setWPtr(const Twptr &p0) {
		if (!p0.cp || !p0.cp->p) {
			cp = NULL;
		} else {
			cp = p0.cp;
			++cp->refcnt;
			DCC("linking " << cp->p << " <- " << cp << " <- " << this <<
					" " << cp->refcnt << std::endl);

		}
	}
public:
	explicit wise_ptr() {
		cp = NULL;
	}
//	explicit wise_ptr(T *p0 = NULL)
	explicit wise_ptr(T *p0) {
		if (CHECK_NULL_PTR && !p0) {
			fprintf(stderr, "-- null pointer C --  ");
			print_backtrace(6);
			memset(p0, 0, 1);
		}
		if (!p0) {
			cp = NULL;
		} else {
			cp = new wise_ptr_common<T>(p0);
			DCC("alloCat " << cp->p << " <- " << cp << " <- " << this <<
					" " << cp->refcnt << std::endl);
		}
	}
	wise_ptr(const Twptr &p0) {
		setWPtr(p0);
	}
	template<class U> wise_ptr(const wise_ptr<U> &p1) {
		T* pp = (U*) NULL;
		pp = static_cast<T*>(p1.to_ptr());
		Twptr &p0 = (wise_ptr<T>&)(p1);
		setWPtr(p0);
	}

#if USE_RVALREFS
	wise_ptr(Twptr &&p0) {
		cp = p0.cp;
		p0.cp = NULL;
	}
	Twptr& operator=(Twptr &&p0) {
		free();
		cp = p0.cp;
		p0.cp = NULL;
		return *this;
	}
#endif

	Twptr& operator=(T* p0) {
		if (CHECK_NULL_PTR && !p0) {
			fprintf(stderr, "-- null pointer = --  ");
			fprintf(stderr, "\n");
		}
		free();
		if (p0) {
			cp = new wise_ptr_common<T>(p0);
			DCC("allo=at " << cp->p << " <- " << cp << " <- " << this <<
					" " << cp->refcnt << std::endl);
		}
		return *this;
	}

	Twptr& operator=(const Twptr &p0) {
		free();
		setWPtr(p0);
		return *this;
	}
	template<class U> Twptr& operator=(const wise_ptr<U> &p1) {
		T* pp = (U*) NULL;
		pp = static_cast<T*>(p1.to_ptr());
		Twptr &p0 = (wise_ptr<T>&)(p1);
		free();
		setWPtr(p0);
		return *this;
	}


	T& operator*() const {
		if (CHECK_NULL_PTR && !to_ptr()) {
			fprintf(stderr, "--- null pointer * ---  ");
			print_backtrace();
			//throw null_pointer();
		}
		return *to_ptr();
	}
	T* operator->() const {
		if (CHECK_NULL_PTR && !to_ptr()) {
			fprintf(stderr, "--- null pointer -> ---  ");
			print_backtrace();
			//throw null_pointer();
		}
		return to_ptr();
	}

//	T* get() const {
	T* to_ptr() const {
		if (cp) {
			return cp->p;
		}
		return NULL;
	}
//	T* ptr() const {
//		return get();
//	}

	int refcnt() const {
		if (cp)
			return cp->refcnt;
		return 0;
	}

	void free() {
		if (!cp)
			return;

		Tcommon *ptr = unref();
		if (ptr)
			ptr->dealloc();
	}

	Tcommon* unref() {
		Tcommon *cptr = cp;
		if (!cptr)
			return NULL;
		if (--cptr->refcnt != 0) {
			cptr = NULL;
			DCC("unlinkd " << cp->p << " <- " << cp << " <- " << this <<
					" " << cp->refcnt << std::endl);
		}
		cp = NULL;

		return cptr;
	}

	void dump() const {}

	~wise_ptr() {
		free();
	}

	template<class U> static Twptr dyn_cast(const wise_ptr<U> &p0) {
		T *pp = dynamic_cast<T*>(p0.to_ptr());
		if (pp) {
			Twptr &pref = (Twptr&) p0;
			return wise_ptr(pref);
		} else {
			return wise_ptr(NULL);
		}
	}


	template <class U> bool operator==(const wise_ptr<U> &p0) const {
		return to_ptr() == static_cast<T*>(p0.to_ptr());
	}
	template <class U> bool operator!=(const wise_ptr<U> &p0) const {
		return to_ptr() != static_cast<T*>(p0.to_ptr());
	}
	template <class U> bool operator<=(const wise_ptr<U> &p0) const {
		return to_ptr() <= static_cast<T*>(p0.to_ptr());
	}
	template <class U> bool operator>=(const wise_ptr<U> &p0) const {
		return to_ptr() >= static_cast<T*>(p0.to_ptr());
	}
	template <class U> bool operator<(const wise_ptr<U> &p0) const {
		return to_ptr() < static_cast<T*>(p0.to_ptr());
	}
	template <class U> bool operator>(const wise_ptr<U> &p0) const {
		return to_ptr() > static_cast<T*>(p0.to_ptr());
	}
};

// note: T is class, NOT wise_ptr<class> or class_p-- I always forget
template<class T, class U> static inline wise_ptr<T>
			dynamic_pointer_cast(const wise_ptr<U> &p0) {
	return wise_ptr<T>::dyn_cast(p0);
}

}
}
using silly::mem::basic_ptr;
using silly::mem::wise_ptr;

#include <map>
#define DEBUG_WISEPTR 1
#if DEBUG_WISEPTR
#include <iostream>
#endif
namespace silly {
namespace types {

//template<class T,class U> static inline wise_ptr<T> map_get(const std::map<U,wise_ptr<T> > &map, const U &key) {
//#if DEBUG_WISEPTR
//	std::cout << "map get: wise_ptr overload" << std::endl;
//#endif
//	typedef typename std::map<U,wise_ptr<T> >::const_iterator map_it;
//	map_it it = map.find(key);
//	if (it == map.end())
//		return wise_ptr<T>(NULL);
//	return it->second;
//}

}
}
#undef DCC
#undef DEBUG_WISEPTR

#endif

#endif /* SILLYMEM_H_ */
