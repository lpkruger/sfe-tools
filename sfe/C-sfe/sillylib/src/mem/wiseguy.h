/*
 * sillymem.h
 *
 *  Created on: Aug 25, 2009
 *      Author: louis
 */

//#if 0
#ifndef SILLYMEM2_H_
#define SILLYMEM2_H_

#include <iostream>
namespace silly {
namespace mem2 {

// auto-freeing reference-counting pointers.  yay.
// my original implementation used 12 bytes per wise_ptr.
// too many.  boo.

class wiseguy_void_ptr {
protected:
	void* p;
	wiseguy_void_ptr *next;
	wiseguy_void_ptr *prev;

	wiseguy_void_ptr() : p(NULL), next(NULL), prev(NULL) {}
	void* unref();
	void  set(wiseguy_void_ptr &p0);
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

template<class T, class dealloc = new_dealloc<T> > class wiseguy_ptr : protected wiseguy_void_ptr {
	typedef wiseguy_void_ptr super;
	typedef wiseguy_ptr<T, dealloc> Tcnt;

	//explicit wiseguy_ptr(const Tcnt &p0) {} // not allowed
	//Tcnt& operator=(const Tcnt &p0) {return *this;} // not allowed)

public:
	void dump const();

	explicit wiseguy_ptr() : super() {}
	explicit wiseguy_ptr(T *p0) : super() {
		operator=(p0);
	}
//	wiseguy_ptr(const Tcnt &p0) : super() {
//		operator=(p0);
//	}
	wiseguy_ptr(const Tcnt &p0) : super() {
		operator=(p0);
	}

	template<class U, class D> wiseguy_ptr(const wiseguy_ptr<U, D> &p0) : super() {
		T* pp = (U*) NULL;
		pp = static_cast<T*>(p0.get());
		operator=(p0);
	}
#if 1
	template<class U, class D> explicit wiseguy_ptr(wiseguy_ptr<U, D> &&p0) : super() {
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

	T* get() const {
		return reinterpret_cast<T*>(p);
	}

	template<class U, class D> bool dyncast_to(wiseguy_ptr<U,D> &ref) {
		U *pp = dynamic_cast<U*>(reinterpret_cast<T*>(p));
		ref = wiseguy_ptr<U,D>(pp ? *this : NULL);
		return pp!=NULL;
	}

	template<class U, class D> static Tcnt dyncast_from(wiseguy_ptr<U,D> &p0) {
		T *pp = dynamic_cast<T*>(p0.get());
		if (pp) {
			Tcnt &pref = (Tcnt&) p0;
			return wiseguy_ptr(pref);
		} else {
			return wiseguy_ptr(NULL);
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
	template<class U, class D> Tcnt& operator=(const wiseguy_ptr<U, D> &p1) {
		T* pp = (U*) NULL;
		pp = static_cast<T*>(p1.get());
		wiseguy_ptr<T, dealloc> &p0 = (wiseguy_ptr<T, dealloc>&)(p1);
		if (this != &p0) {
			free();
			if (p0.p)
				super::set(p0);
		}
		return *this;
	}
#if 1
	template<class U, class D> Tcnt& operator=(wiseguy_ptr<U, D> &&p0) {
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

	~wiseguy_ptr() {
		free();
	}

	template <class U,class D> bool operator==(const wiseguy_ptr<U,D> &p0) const {
		return p == static_cast<T*>(p0.get());
	}
	template <class U,class D> bool operator!=(const wiseguy_ptr<U,D> &p0) const {
		return p != static_cast<T*>(p0.get());
	}
	template <class U,class D> bool operator<=(const wiseguy_ptr<U,D> &p0) const {
		return p <= static_cast<T*>(p0.get());
	}
	template <class U,class D> bool operator>=(const wiseguy_ptr<U,D> &p0) const {
		return p >= static_cast<T*>(p0.get());
	}
	template <class U,class D> bool operator<(const wiseguy_ptr<U,D> &p0) const {
		return p < static_cast<T*>(p0.get());
	}
	template <class U,class D> bool operator>(const wiseguy_ptr<U,D> &p0) const {
		return p > static_cast<T*>(p0.get());
	}
};

template<class T> struct wiseguy {
	typedef wiseguy_ptr<T> ptr;
	typedef wiseguy_ptr<T,new_dealloc<T> > objptr;
	typedef wiseguy_ptr<T,array_dealloc<T> > array;
	typedef wiseguy_ptr<T,malloc_dealloc<T> > memptr;
	typedef wiseguy_ptr<T,destruct_dealloc<T> > placeptr;
	typedef wiseguy_ptr<T,null_dealloc> genptr;
};


template<class T> static inline wiseguy_ptr<T> newObject() {
	return wiseguy_ptr<T>(new T());
}
template<class T, class A> static inline wiseguy_ptr<T> newObject(A a) {
	return wiseguy_ptr<T>(new T(a));
}
template<class T, class A, class B>  static inline wiseguy_ptr<T> newObject(A a, B b) {
	return wiseguy_ptr<T>(new T(a,b));
}
template<class T, class A, class B, class C> static inline wiseguy_ptr<T> newObject(A a, B b, C c) {
	return wiseguy_ptr<T>(new T(a,b,c));
}
/// etc...
template<class T> static inline wiseguy_ptr<T, array_dealloc<T>>  newArray(int sz) {
	//return wiseguy_ptr<T, array_dealloc<T> > (new T[sz]);
	return wiseguy_ptr<T, array_dealloc<T>> (new T[sz]);
}


#if 0
template<class T> class wiseguy_ptr : public wiseguy_ptr_impl<T, new_dealloc<T> > {
	typedef new_dealloc<T> D;
	typedef wiseguy_ptr_impl<T,D> super;
	typedef wiseguy_ptr<T> Tcnt;
public:
	wiseguy_ptr() : super() {}
	wiseguy_ptr(T *p0) : super(p0) {}
	wiseguy_ptr(Tcnt &p0) : super(p0) {}
	Tcnt& operator=(T *p0) { return (Tcnt&) super::operator=(p0); }
	Tcnt& operator=(Tcnt &p0) { return (Tcnt&) super::operator=(p0); }
};
#endif

}
}



#ifdef SILLYMEM2_H_
void silly::mem::wiseguy_void_ptr::set(wiseguy_void_ptr &p0) {
	p = p0.p;
	if ((p0.next == NULL) && (p0.prev == NULL)) {
		p0.next = p0.prev = this;
		next = prev = &p0;
	} else {
		next = p0.next;
		prev = &p0;
		prev->next = this;
		next->prev = this;
	}
}
void* silly::mem::wiseguy_void_ptr::unref() {
	// unhook object, return ptr if last copy
	if ((next==NULL) && (prev==NULL)) {
		void* ptr = p;
		p = NULL;
		return ptr;
	}
	if (next==prev) {
		next->next = NULL;
		next->prev = NULL;
		next = prev = NULL;
		return (p = NULL);
	}
	next->prev = prev;
	prev->next = next;
	next = prev = NULL;
	return (p = NULL);
}
#endif


//
//template<class T, class D> static inline void std::swap
//		(silly::mem::wiseguy_ptr<T,D> &a, silly::mem::wiseguy_ptr<T,D> &b) {
//	a.swap(b);
//}
#endif /* SILLYMEM_H_ */
