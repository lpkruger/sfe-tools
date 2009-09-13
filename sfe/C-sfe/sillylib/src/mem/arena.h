/*
 * sillymem3.h
 *
 *  Created on: Aug 27, 2009
 *      Author: louis
 */

#ifndef SILLYMEM_ARENA_H_
#define SILLYMEM_ARENA_H_


#include <string.h>
//#define DEBUG 1
#include <vector>
#include <stdexcept>
#include "bigint.h"
#include "sillydebug.h"

// this isn't really necessary, enable it if you want
#undef USE_PTR_MEMBER_FN

#ifdef USE_PTR_MEMBER_FN
class Unspecified;
typedef void (Unspecified::*voidMemberFunc)();

#else
typedef void Unspecified;
//#define Unspecified void
#endif

typedef void (*voidFunc)(Unspecified *);

#ifdef USE_PTR_MEMBER_FN
struct _voidFunc {
	static const int magic_num = 0xDEADBEEF;
	int magic;
	voidFunc func;

	void operator=(voidFunc f0) {
		if (!f0)
			magic = 0;
		else
			magic = magic_num;
		func = f0;
	}
	operator voidFunc() { return magic==magic_num ? func : NULL; }
};
#endif

union this_that_function {

#ifdef USE_PTR_MEMBER_FN
	voidMemberFunc memFunc;
	_voidFunc func;
#else
	voidFunc func;
#endif

	this_that_function() {
		zero();
	}
#ifdef USE_PTR_MEMBER_FN
	this_that_function(voidMemberFunc fn) {
		operator=(fn);
	}
	void operator=(voidMemberFunc fn) {
		zero();
		memFunc = fn;
	}
#endif
	this_that_function(voidFunc fn) {
		operator=(fn);
	}
	void operator=(voidFunc fn) {
		zero();
		func = fn;
	}

	void zero() {
		memset(this, 0, sizeof(this_that_function));
	}


	bool notNull() {
		char *p = reinterpret_cast<char*>(this);
		char *end = p+sizeof(this_that_function);
		while (p++<end) {
			if (p != 0)
				return true;
		}
		return false;
	}
	void call(Unspecified *that) {
		voidFunc fn = func;
		if (fn) {
			(*fn)(that);
			return;
		}
#ifdef USE_PTR_MEMBER_FN
		if (notNull()) {
			(that->*memFunc)();
		}
#endif
	}
};

#include <stdlib.h>

struct this_that_fn_object {
	Unspecified *obj;
	this_that_function fn;
	void set(Unspecified *obj0, this_that_function &fn0) {
		obj = obj0;
		fn = fn0;
	}
};

class arena {
	char *base;		// where the malloc allocation is
	char *bottom;   // the next location available
	this_that_fn_object *top;      // everything from here to ceiling is a pointer to an allocated object
	this_that_fn_object *ceiling;	// ceiling is an invalid pointer marking the end of the block

	arena (const arena &);            // No copying
	arena &operator= (const arena &); // No copying
public:
	arena(size_t size) {	// if constructed this way, the header is elsewhere
		char *bb = reinterpret_cast<char*>(::malloc(size));
		base = bottom = bb;
		top = ceiling = reinterpret_cast<this_that_fn_object*>(bb+size);
		DF("--- new arena %u ---", size);
		DD(dump());
	}

	static arena* makeArena(size_t size) {	// constructed this way, the header is at the beginning
		char *bb = reinterpret_cast<char*>(::malloc(size));
		arena *aa = reinterpret_cast<arena*>(bb);
		aa->base = bb;
		aa->bottom = bb+sizeof(arena);
		aa->top = aa->ceiling = reinterpret_cast<this_that_fn_object*>(bb+size);
		DF("--- makeArena %u ---", size);
		DD(aa->dump());
		return aa;
	}
//private:
//	void* operator new(size_t size, int nbytes) {
//		DF("new arena %d %d\n", size, nbytes);
//		return newArena(nbytes);
//	}
public:
	void operator delete(void* aa) {
		void **base_p = (void**) aa;
		void *base = *base_p;
		if (base == aa) {
			DF("delete self-hosted arena free(%08lx)", (ulong)aa);
			::free(aa);
		} else {
			DF("delete called on unhosted arena %08lx  base=%08lx", (ulong)aa, (ulong)base);
			::operator delete(aa);
		}
	}
	void dump(bool mem=false) {
		DF("\n---arena---");
		DF("ceil: %08lx", (ulong) ceiling);
		DF("top : %08lx", (ulong) top);
		DF("bott: %08lx", (ulong) bottom);
		DF("base: %08lx", (ulong) base);
		DF("this: %08lx\n--------", (ulong) this);
		if (!mem) return;
		for (char *p=base; p<bottom; ++p) {
			fprintf(stderr, "%02x ", (unsigned char)*p);
			if ((uint(p)%16)==15)
				fprintf(stderr, "\n");
			else if ((uint(p)%8)==7)
				fprintf(stderr, "  ");
		}
		fprintf(stderr, "\n");

	}

	bool isInside(void *ptr) {
		return ptr>=base && ptr<ceiling;
	}

	bool can_allocate(size_t size) {
		if (bottom + size + sizeof(this_that_fn_object) >= (char*) top) {
			return false;
		}
		return true;
	}

	void* allocate(size_t size) {
		DF("allocate @ %08lx  %d", (ulong) bottom, size);
		if (!can_allocate(size))
			return NULL;

		void* loc = bottom;
		bottom += size;
		return loc;
	}


	void* allocate(size_t size, this_that_function destructor_fn) {
		void *loc = allocate(size);
		if (!loc)
			return NULL;
		if (destructor_fn.notNull()) {
			registerDestructor(loc, destructor_fn);
		}
		return loc;
	}

	void registerDestructor(void* loc, this_that_function destructor_fn) {
		Unspecified *obj = reinterpret_cast<Unspecified*>(loc);
		(--top)->set(obj, destructor_fn);
	}

#ifdef USE_PTR_MEMBER_FN
	template<class T> void* allocateWithDestructor() {
		typedef void (T::*myMemberFunc)();
		myMemberFunc destructor = &(T::~T);
		this_that_function destructor_fn;
		destructor_fn = destructor;
		return allocate(sizeof(T), destructor_fn);
	}


	// this may not work?
	template<class T> T* newWithDestructor(voidMemberFunc destructor) {
		typedef void (T::*myMemberFunc)();
		//myMemberFunc destructor = &(T::~T);
		this_that_function destructor_fn = destructor;
		void *loc = allocate(sizeof(T), destructor_fn);
		T *o = new (loc) T();
	}
#endif


	template<class T> T* newWithDestructor(void (*destructor)(T*)) {
		this_that_function destructor_fn = reinterpret_cast<voidFunc>(destructor);
		void *loc = allocate(sizeof(T), destructor_fn);
		if (!loc)
			return NULL;
		T *o = new (loc) T();
		return o;
	}

	template<class T> static void callDestructor(T* a) {
		a->~T();
	}

	template<class T> struct destructoid {
		typedef void (*type)(T*);
	};
	template<class T> static inline typename destructoid<T>::type getDestructor() {
		return callDestructor;
	}
	template<class T> static inline typename destructoid<T>::type getArrayDestructor() {
		return destructArray;
	}

	template<class T> static void destructArray(T* ptr) {
#ifdef __GNUC__		// non-portable, gcc stores arrays in a particular way
		size_t *iptr = reinterpret_cast<size_t*>(ptr);
		int arraylen = *(--iptr);
		int allocsize = *(--iptr);
		allocsize -= sizeof(size_t);	// the requested amount
		int objsize = allocsize/arraylen;
		DF("destroy array @ %08lx objsize %d len %d", (ulong)ptr, objsize, arraylen);
		char *p0 = reinterpret_cast<char*>(ptr);
		char *p;
		for (p=p0+allocsize-objsize; p>=p0; p-=objsize) {
			reinterpret_cast<T*>(p) -> ~T();
		}
#endif
	}

//	template<class T> static T* callConstructor() {
//		return new T();
//	}

	template<class T> T* newWithDestructor() {
		void (*destructor)(T*) = arena::callDestructor;
		this_that_function destructor_fn = reinterpret_cast<voidFunc>(destructor);
		void *loc = allocate(sizeof(T), destructor_fn);
		T *o = new (loc) T();
		return o;
	}

	void destroyAll() {
		while (top < ceiling) {

#ifdef USE_PTR_MEMBER_FN
			DF("record %08lx call destructor %08lx %08lx for %08lx", (ulong) top,
					(ulong) top->fn.func.magic, (ulong) top->fn.func.func, (ulong) top->obj);
#else
			DF("record %08lx call destructor %08lx for %08lx", (ulong) top,
								(ulong) top->fn.func, (ulong) top->obj);
#endif
			top->fn.call(top->obj);
			++top;
		}
	}
	~arena() {
		destroyAll();
		if (this != reinterpret_cast<arena*>(base)) {
			DF("~arena unhosted this=%08lx  free(base=%08lx)", (ulong)this, (ulong)base);
			::free(base);
		} else {
			DF("~arena self-hosted this=%08lx", (ulong)this);
		}
	}
};

inline void* operator new (size_t bytes, arena *a) {
	DF("operator new arena %08lx  %d", (ulong)a, bytes);
	void* ptr = a->allocate (bytes);
	if (!ptr)
		throw std::bad_alloc();
	return ptr;
}
template<class T>
inline void* operator new (size_t bytes, arena *a, void (*destructor)(T*)) throw() {
	void* ptr = a->allocate(bytes, (voidFunc) destructor);
	if (!ptr)
		throw std::bad_alloc();
	return ptr;
}
template<class T>
inline void* operator new (size_t bytes, arena *a, T* dummy) throw() {
	void (*destructor)(T*) = arena::callDestructor;
	void* ptr = a->allocate(bytes, (voidFunc) destructor);
	if (!ptr)
		throw std::bad_alloc();
	return ptr;

}
inline void* operator new[] (size_t bytes, arena *a) {
#ifndef __GNUC__		// non-portable, gcc stores arrays in a particular way
	throw std::bad_alloc;
#else
	DF("operator new[] arena %08lx  %d", (ulong)a, bytes);
	void* ptr = a->allocate (bytes+sizeof(size_t));
	if (!ptr)
		throw std::bad_alloc();
	size_t *iptr = reinterpret_cast<size_t*>(ptr);
	*iptr = bytes;
	return iptr+1;
#endif
}
template<class T>
inline void* operator new[](size_t bytes, arena *a, void (*destructor)(T*)) {
	void* ptr = operator new[](bytes, a);
	if (!ptr)
		throw std::bad_alloc();
	char* p = (char*) ptr;
	a->registerDestructor(p+sizeof(size_t), (voidFunc) destructor);
	return ptr;
}


class growable_arena {
	//vector<arena*> arenas;
	int arenas_curr;
	arena *arenas[4096];

	//const static size_t chunksize = 16*1024*1024;
#define chunksize (16u*1024*1024)
public:
	growable_arena() : arenas_curr(-1) {
		grow();
	}
	arena* a() {
		//return arenas[arenas.size()-1];
		return arenas[arenas_curr];
	}
	void dump(bool mem=false) {
		a()->dump(mem);
	}
	arena* grow(size_t size=0) {
		printf("new arena %lu\n", (ulong) size);
		//arenas.push_back(arena::makeArena(std::max(chunksize, size+1024)));
		arenas[++arenas_curr] = arena::makeArena(std::max(chunksize, size+1024));
		return a();
	}
	void* allocate(size_t size) {
		//printf("allocate %lu\n", (ulong) size);
		void *ptr = a()->allocate(size);
		if (!ptr) {
			ptr = grow(size)->allocate(size);
		}
		return ptr;
	}
	void* allocate(size_t size, this_that_function fn) {
		printf("allocate-d %lu\n", (ulong) size);
		void *ptr = a()->allocate(size, fn);
		if (!ptr) {
			ptr = grow(size)->allocate(size, fn);
		}
		return ptr;
	}
	bool isInside(void *ptr) {
		//for (uint i=0; i<arenas.size(); ++i) {
		for (uint i=0; i<=arenas_curr; ++i) {
			if (arenas[i]->isInside(ptr))
				return true;
		}
		return false;
	}
	template<class T> T* newWithDestructor(void (*destructor)(T*)) {
		return  a()->newWithDestructor(destructor);
	}

	~growable_arena() {
		//for (uint i=0; i<arenas.size(); ++i) {
		for (uint i=0; i<=arenas_curr; ++i) {
			arenas[i]->destroyAll();
		}
	}
};

inline void* operator new (size_t bytes, growable_arena *a) throw() {
	DF("operator new arena %08lx  %d", (ulong)a, bytes);
	void* ptr = a->allocate (bytes);
	return ptr;
}
template<class T>
inline void* operator new (size_t bytes, growable_arena *a, void (*destructor)(T*)) throw() {
	return a->allocate(bytes, (voidFunc) destructor);
}
template<class T>
inline void* operator new (size_t bytes, growable_arena *a, T* dummy) throw() {
	void (*destructor)(T*) = arena::callDestructor;
	return a->allocate(bytes, (voidFunc) destructor);
}

//inline void* operator new[] (arena &a, size_t bytes) { // This function is bad news
//  return a.allocate (bytes, sizeof (double));
//}

class AA {
public:
	int n;
	int foo() {
		return 3;
	}
	AA() : n(0x0beefea7){
		DF("AA @ %08lx constructed", (ulong) this);
	}
	virtual ~AA() {
		DF("AA @ %08lx destructed", (ulong) this);
	}
};

static int _main_arenatest(int argc, char **argv) {
	arena *arena1 = arena::makeArena(16*1024*1024);
	// test arrays
	////
	arena arena_2(16*1024*1024);
	arena *arena2 = &arena_2;
	AA *aaa1 = new (arena2) AA();
	arena2->registerDestructor(aaa1, arena::getDestructor<AA>());
	AA *array1 = new (arena2) AA[4];
	arena2->registerDestructor(array1, arena::getArrayDestructor<AA>());
	AA *array2 = new (arena2, arena::getArrayDestructor<AA>()) AA[6];
	arena2->dump(true);
	return 0;

	//arena *a = new (16*1024*1024) arena;
	//AA *aa = a->newWithDestructor<AA>(&AA::~AA);

	AA *a1 = arena1->newWithDestructor<AA>(arena::callDestructor);
	AA *a2 = new (arena1, arena::getDestructor<AA>()) AA();
	AA *a3 = new (arena2) AA();
	arena1->registerDestructor(a3, arena::getDestructor<AA>());
	AA *a4 = new (arena1, (AA*)0) AA();

	AA *a5 = arena2->newWithDestructor<AA>();
	AA *aaa[4] = {a1,a2,a3,a4};
	arena1->dump();
	//arena1->destroyAll();
	delete arena1;
	return 0;
}


using silly::mem::basic_ptr;

static inline void test_basic_ptr() {
	int i=1;
	int *ip1 = &i;
	basic_ptr<int> ib1 = ip1;
	basic_ptr<int> ib2 = ib1;
	basic_ptr<basic_ptr<int> > ibb = &ib1;
	int *ip2 = ibb->get();
	ip2 = ib2;
	ip2 = ib2+1;
	int ii = *ib1;
	ib1 = ib2;
	bool b = (ib1 == ib2);

	int xx[10];
	for (ib1=xx; ib1<xx+10; ib1+=1) {

	}
}


namespace pmf {


using std::cout;
using std::endl;


template<class P> class delegate {
	// P is a ptmf
public:
	class unspecified;
	typedef void (unspecified::*pmf_t)(void* arg);
	pmf_t pmf;
	delegate(P pmf0) {
		pmf = pmf0;
	}
};

struct A {
	void foo(void* arg) {
		cout << "hi" << endl;
	}
	void bar(void* arg) {
		cout << "hello" << endl;
	}
	void null() {
		cout << "nada" << endl;
	}
};

#define DELEGATE(name, args)

class unspecified;
struct B {
	//DELEGATE(foo, void*)

	typedef void (unspecified::*foo_pmf_type)(void* arg);
	foo_pmf_type foo_pmf;
	unspecified *foo_obj;

	void foo(void* arg) {
		(foo_obj->*foo_pmf)(arg);
	}
	void set_foo(unspecified *foo_o, foo_pmf_type foo_mf) {
		foo_pmf = foo_mf;
		foo_obj = foo_obj;
	}
};


#include "bound_pmf.h"

struct C {
	bound_pmf<A, void, void*> foo;
	bound_pmf<A, void, void*> bar;
	C(A *a) : foo(a, &A::foo), bar(a, &A::bar) {}
};

struct D {
	void hell(void *where) {
		cout << "D sucks" << endl;
	}
};

static int _main_typetest(int argc, char **argv) {
	A a;
	//a.foo(&a);
	bound_pmf<A, void> a_null(&a, &A::null);
	bound_pmf<A, void, void*> a_foo(&a, &A::foo);
	bound_pmf<A, void, void*> a_bar(&a, &A::bar);
	a_null();
	a_foo(&a);
	a_foo = &A::bar;
	a_foo(&a);
	a_bar(&a);
	C c(&a);
	c.foo(&c);
	c.bar(&c);
	D d;
	a_foo.rebind(&d, &D::hell);
	a_foo(&a);
}

}

//#undef D
//#undef DF
//#undef DC
//#undef DD
#endif /* SILLYMEM_ARENA_H_ */
