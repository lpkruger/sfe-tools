/*
 * sillymem.h
 *
 *  Created on: Aug 25, 2009
 *      Author: louis
 */

#ifndef SILLYMEM_H_
#define SILLYMEM_H_

#include <iostream>
namespace silly {
namespace mem {

// auto-freeing reference-counting pointers.  yay.

class wise_void_ptr {
protected:
	void* p;
	wise_void_ptr *next;
	wise_void_ptr *prev;

	wise_void_ptr() : p(NULL), next(NULL), prev(NULL) {}
	void* unref();
	void  set(wise_void_ptr &p0);
};

template<class T> struct new_dealloc {
	static void free(T *p) { delete p; }
};
template<class T> struct array_dealloc {
	static void free(T *p) { delete[] p; }
};
template<class T> struct malloc_dealloc {
	static void free(T *p) { free(p); }
};
template<class T> struct destruct_dealloc {
	static void free(T *p) { p->~T(); }
};
struct null_dealloc {
	static void free(const void *p) {}
};

template<class T, class dealloc = new_dealloc<T> > class wise_ptr : protected wise_void_ptr {
	typedef wise_void_ptr super;
	typedef wise_ptr<T, dealloc> Tcnt;

	//explicit wise_ptr(const Tcnt &p0) {} // not allowed
	//Tcnt& operator=(const Tcnt &p0) {return *this;} // not allowed)

public:
	void dump();

	wise_ptr() : super() {}
	wise_ptr(T *p0) : super() {
		operator=(p0);
	}
//	wise_ptr(const Tcnt &p0) : super() {
//		operator=(p0);
//	}
	wise_ptr(const Tcnt &p0) : super() {
		operator=(p0);
	}

	template<class U, class D> wise_ptr(const wise_ptr<U, D> &p0) : super() {
		T* pp = (U*) NULL;
		pp = static_cast<T*>(p0.ptr());
		operator=(p0);
	}
#if 1
	template<class U, class D> explicit wise_ptr(wise_ptr<U, D> &&p0) : super() {
		T* pp = (U*) NULL;
		p = static_cast<T*>(p0.p);
		if (p) {
			next = p0.next;
			prev = p0.prev;
			if (next) {
				((Tcnt*)next)->prev = this;
				((Tcnt*)prev)->next = this;
			}
			p0.p = NULL;
			p0.next = NULL;
			p0.prev = NULL;
		}
	}
#endif
	T& operator*() const {
		return *reinterpret_cast<T*>(p);
	}
	T* operator->() const {
		return reinterpret_cast<T*>(p);
	}
	//operator T* () const {
	//	return reinterpret_cast<T*>(p);
	//}

	T* ptr() const {
		return reinterpret_cast<T*>(p);
	}

	template<class U, class D> bool dyncast_to(wise_ptr<U,D> &ref) {
		U *pp = dynamic_cast<U*>(reinterpret_cast<T*>(p));
		ref = wise_ptr<U,D>(pp ? *this : NULL);
		return pp!=NULL;
	}

	template<class U, class D> static Tcnt dyncast_from(wise_ptr<U,D> &p0) {
		T *pp = dynamic_cast<T*>(p0.ptr());
		if (pp) {
			Tcnt &pref = (Tcnt&) p0;
			return wise_ptr(pref);
		} else {
			return wise_ptr(NULL);
		}
	}

#if 0
	Tcnt& swap(Tcnt &p0) {
		std::swap(p, p0.p);
		std::swap(next, p0.next);
		std::swap(prev, p0.prev);
	}
#endif
	template<class U> Tcnt& operator=(U *p1) {
		T* p0 = (U*) NULL;
		p0 = static_cast<T*>(p1);
		if (p != p0) {
			free();
			p = (void*)(p0);
			next = prev = NULL;
		}
		return *this;
	}
	Tcnt& operator=(const Tcnt &p0) {
		if (this != &p0) {
			free();
			if (p0.p)
				super::set(const_cast<Tcnt &>(p0));
		}
		return *this;
	}
	template<class U, class D> Tcnt& operator=(const wise_ptr<U, D> &p1) {
		T* pp = (U*) NULL;
		pp = static_cast<T*>(p1.ptr());
		wise_ptr<T, dealloc> &p0 = (wise_ptr<T, dealloc>&)(p1);
		if (this != &p0) {
			free();
			if (p0.p)
				super::set(p0);
		}
		return *this;
	}
#if 1
	template<class U, class D> Tcnt& operator=(wise_ptr<U, D> &&p0) {
		T* pp = (U*) NULL;
		if (this != &p0) {
			free();
			if (p0.p) {
				p = static_cast<T*>(p0.p);
				next = p0.next;
				prev = p0.prev;
				if  (next) {
					((Tcnt*)next)->prev = this;
					((Tcnt*)prev)->next = this;
				}
				p0.p = NULL;
				p0.next = NULL;
				p0.prev = NULL;
			}
		}
		return *this;
	}
#endif

	void free() {
		if (!p)
			return;
		//dump();
		T *ptr = unref();
		if (ptr) {
			//std::cout << "freeing " << ptr << std::endl;
			dealloc::free(ptr);
		}
	}

	T* unref() {
		return reinterpret_cast<T*>(super::unref());
	}

	~wise_ptr() {
		free();
	}

	template <class U,class D> bool operator==(const wise_ptr<U,D> &p0) const {
		return p == static_cast<T*>(p0.ptr());
	}
	template <class U,class D> bool operator!=(const wise_ptr<U,D> &p0) const {
		return p != static_cast<T*>(p0.ptr());
	}
	template <class U,class D> bool operator<=(const wise_ptr<U,D> &p0) const {
		return p <= static_cast<T*>(p0.ptr());
	}
	template <class U,class D> bool operator>=(const wise_ptr<U,D> &p0) const {
		return p >= static_cast<T*>(p0.ptr());
	}
	template <class U,class D> bool operator<(const wise_ptr<U,D> &p0) const {
		return p < static_cast<T*>(p0.ptr());
	}
	template <class U,class D> bool operator>(const wise_ptr<U,D> &p0) const {
		return p > static_cast<T*>(p0.ptr());
	}
};

template<class T> struct wise {
	typedef wise_ptr<T> ptr;
	typedef wise_ptr<T,new_dealloc<T> > objptr;
	typedef wise_ptr<T,array_dealloc<T> > array;
	typedef wise_ptr<T,malloc_dealloc<T> > memptr;
	typedef wise_ptr<T,destruct_dealloc<T> > placeptr;
	typedef wise_ptr<T,null_dealloc> genptr;
};


template<class T> static inline wise_ptr<T> newObject() {
	return wise_ptr<T>(new T());
}
template<class T, class A> static inline wise_ptr<T> newObject(A a) {
	return wise_ptr<T>(new T(a));
}
template<class T, class A, class B>  static inline wise_ptr<T> newObject(A a, B b) {
	return wise_ptr<T>(new T(a,b));
}
template<class T, class A, class B, class C> static inline wise_ptr<T> newObject(A a, B b, C c) {
	return wise_ptr<T>(new T(a,b,c));
}
/// etc...
template<class T> static inline wise_ptr<T, array_dealloc<T>>  newArray(int sz) {
	//return wise_ptr<T, array_dealloc<T> > (new T[sz]);
	return wise_ptr<T, array_dealloc<T>> (new T[sz]);
}


#if 0
template<class T> class wise_ptr : public wise_ptr_impl<T, new_dealloc<T> > {
	typedef new_dealloc<T> D;
	typedef wise_ptr_impl<T,D> super;
	typedef wise_ptr<T> Tcnt;
public:
	wise_ptr() : super() {}
	wise_ptr(T *p0) : super(p0) {}
	wise_ptr(Tcnt &p0) : super(p0) {}
	Tcnt& operator=(T *p0) { return (Tcnt&) super::operator=(p0); }
	Tcnt& operator=(Tcnt &p0) { return (Tcnt&) super::operator=(p0); }
};
#endif

}
}
//
//template<class T, class D> static inline void std::swap
//		(silly::mem::wise_ptr<T,D> &a, silly::mem::wise_ptr<T,D> &b) {
//	a.swap(b);
//}
#endif /* SILLYMEM_H_ */
