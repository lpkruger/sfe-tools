/*
 * sillymem.h
 *
 *  Created on: Aug 25, 2009
 *      Author: louis
 */

//#if 0
#ifndef SILLYMEM_H_
#define SILLYMEM_H_

//#define DEBUG_WISEPTR 1

#if DEBUG_WISEPTR
#include <iostream>
#define D(x) std::cout << x
#else
#define D(x)
#endif

namespace silly {
namespace mem {

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
		D("dealloc " << p << " <- " << this << " " << refcnt << std::endl);
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
			D("linking " << cp->p << " <- " << cp << " <- " << this <<
					" " << cp->refcnt << std::endl);

		}
	}
public:
	explicit wise_ptr(T *p0 = NULL) {
		if (!p0) {
			cp = NULL;
		} else {
			cp = new wise_ptr_common<T>(p0);
			D("alloCat " << cp->p << " <- " << cp << " <- " << this <<
					" " << cp->refcnt << std::endl);
		}
	}
	wise_ptr(const Twptr &p0) {
		setWPtr(p0);
	}
	template<class U> wise_ptr(const wise_ptr<U> &p1) {
		T* pp = (U*) NULL;
		pp = static_cast<T*>(p1.get());
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
		free();
		if (p0) {
			cp = new wise_ptr_common<T>(p0);
			D("allo=at " << cp->p << " <- " << cp << " <- " << this <<
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
		pp = static_cast<T*>(p1.get());
		Twptr &p0 = (wise_ptr<T>&)(p1);
		free();
		setWPtr(p0);
		return *this;
	}


	T& operator*() const {
		return *get();
	}
	T* operator->() const {
		return get();
	}

	T* get() const {
		if (cp) {
			return cp->p;
		}
		return NULL;
	}

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
			D("unlinkd " << cp->p << " <- " << cp << " <- " << this <<
					" " << cp->refcnt << std::endl);
		}
		cp = NULL;

		return cptr;
	}

	void dump() const {}

	~wise_ptr() {
		free();
	}

	template<class U> static Twptr dyn_cast(wise_ptr<U> &p0) {
		T *pp = dynamic_cast<T*>(p0.get());
		if (pp) {
			Twptr &pref = (Twptr&) p0;
			return wise_ptr(pref);
		} else {
			return wise_ptr(NULL);
		}
	}


	template <class U> bool operator==(const wise_ptr<U> &p0) const {
		return get() == static_cast<T*>(p0.get());
	}
	template <class U> bool operator!=(const wise_ptr<U> &p0) const {
		return get() != static_cast<T*>(p0.get());
	}
	template <class U> bool operator<=(const wise_ptr<U> &p0) const {
		return get() <= static_cast<T*>(p0.get());
	}
	template <class U> bool operator>=(const wise_ptr<U> &p0) const {
		return get() >= static_cast<T*>(p0.get());
	}
	template <class U> bool operator<(const wise_ptr<U> &p0) const {
		return get() < static_cast<T*>(p0.get());
	}
	template <class U> bool operator>(const wise_ptr<U> &p0) const {
		return get() > static_cast<T*>(p0.get());
	}
};

template<class T, class U> static inline const wise_ptr<T>
			dynamic_pointer_cast(wise_ptr<U> &p0) {
	return wise_ptr<T>::dyn_cast(p0);
}

}
}

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

#undef D

#undef DEBUG_WISEPTR
#endif /* SILLYMEM_H_ */
