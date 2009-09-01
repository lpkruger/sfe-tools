/*
 * bound_pmf.h
 *
 *  Created on: Aug 31, 2009
 *      Author: louis
 */

#ifndef BOUND_PMF_H_
#define BOUND_PMF_H_

#define PP_NARG(...) PP_NARG_(__VA_ARGS__,PP_RSEQ_N())
#define PP_NARG_(...) PP_ARG_N(__VA_ARGS__)
#define PP_ARG_N(_1,_2,_3,_4,_5,_6,_7,_8,N,...) N
#define PP_RSEQ_N() 8,7,6,5,4,3,2,1,0

#define ARG1(A1,...) A1
#define ARG2(A1,A2,...) A2
#define ARG3(A1,A2,A3,...) A3
#define ARG4(A1,A2,A3,A4,...) A4
#define ARG5(A1,A2,A3,A4,A5,...) A5
#define ARG6(A1,A2,A3,A4,A5,A6,...) A6
#define ARG7(A1,A2,A3,A4,A5,A6,A7,...) A7
#define ARG8(A1,A2,A3,A4,A5,A6,A7,A8,...) A8

//#define DO_PRAGMA(X) _Pragma(#X)
//#define WARN DO_PRAGMA(message ARG2(AA,BB,CC) )
//WARN

#define BOUND_PMF_BOILERPLATE \
	C *obj;	 PMF pmf;												\
	R operator=(PMF f) { pmf = f; }									\
	void bind(C *o) { obj = o; }									\
	template<class U, class UMF> 									\
	void rebind(U *u, UMF mf) { obj = (C*) u;  pmf = (PMF) mf; }	\
	bound_pmf() : obj(0), pmf(0) {}									\
	bound_pmf(C *o0, PMF f0) : obj(o0), pmf(f0) {}

template<class C, class R, class A1=void, class A2=void, class A3=void,
	class A4=void, class A5=void, class A6=void, class A7=void, class A8=void>
class bound_pmf {
public:
	typedef R (C::*PMF)(A1,A2,A3,A4,A5,A6,A7,A8);
	BOUND_PMF_BOILERPLATE
	R operator()(A1 a1,A2 a2, A3 a3, A4 a4, A5 a5, A6 a6, A7 a7, A8 a8) {
		return (obj->*pmf)(a1,a2,a3,a4,a5,a6,a7,a8);
	}
};

template<class C, class R, class A1, class A2, class A3>
class bound_pmf<C,R,A1,A2,A3,void> {
public:
	typedef R (C::*PMF)(A1,A2,A3);
	BOUND_PMF_BOILERPLATE
	R operator()(A1 a1,A2 a2,A3 a3) {
		return (obj->*pmf)(a1,a2,a3);
	}
};

template<class C, class R, class A1, class A2> class bound_pmf<C,R,A1,A2,void> {
public:
	typedef R (C::*PMF)(A1,A2);
	BOUND_PMF_BOILERPLATE
	R operator()(A1 a1,A2 a2) {
		return (obj->*pmf)(a1,a2);
	}
};
template<class C, class R, class A1> class bound_pmf<C,R,A1,void> {
public:

	typedef R (C::*PMF)(A1);
	BOUND_PMF_BOILERPLATE
	R operator()(A1 a1) {
		return (obj->*pmf)(a1);
	}
};
template<class C, class R> class bound_pmf<C,R,void> {
public:
	typedef R (C::*PMF)();
	BOUND_PMF_BOILERPLATE
	R operator()() {
		return (obj->*pmf)();
	}
};

#if 0
template<class C, class R, class A1, class A2, class A3,
		class A4, class A5, class A6, class A7, class A8>
bound_pmf<C,R,A1,A2,A3,A4,A5,A6,A7,A8>
bind_create(C *o, typename bound_pmf<C,R,A1,A2,A3,A4,A5,A6,A7,A8>::PMF f) {
	return bound_pmf<C,R,A1,A2,A3,A4,A5,A6,A7,A8>(o, f);
}
#elif 0
template<class C, class R, class A1=void, class A2=void, class A3=void,
		class A4=void, class A5=void, class A6=void, class A7=void, class A8=void>
bound_pmf<C,R,A1,A2,A3,A4,A5,A6,A7,A8>
bind_create(C *o, typename bound_pmf<C,R,A1,A2,A3,A4,A5,A6,A7,A8>::PMF f) {
	return bound_pmf<C,R,A1,A2,A3,A4,A5,A6,A7,A8>(o, f);
}
#endif

#endif /* BOUND_PMF_H_ */
