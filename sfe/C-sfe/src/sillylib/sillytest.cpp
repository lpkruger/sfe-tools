#if 0 && !defined(__INTEL_COMPILER)	// not working

#include "silly.h"
#include "sillymem.h"
#include "mem/arena.h"
#include "sillythread.h"

#include <iostream>
#include <string.h>
#include <algorithm>
#include <errno.h>

using namespace std;
using namespace silly;

#define _main _main_numtest
#define _main _main_threadtest
#define _main pmf::_main_typetest
#define _main _main_arenatest

using namespace silly::mem;
using namespace silly::thread;

static int count_me;
static Mutex count_lock;
static void* increment_it(void* arg) {
	Lock lock(&count_lock);
	//SYNCHRONIZE(count_lock);
	//printf("adding 1\n");
	int* num = (int*)arg;
	int val = *num;
	usleep(20);
	*num = val+1;
	return (void*) 3;
}
static int _main_threadtest2(int argc, char **argv) {
	const int n_threads = 10000;
	Thread th[n_threads];
	for (int i=0; i<n_threads; ++i) {
		th[i].start(-1, 500, &increment_it, &count_me);
	}
	int retsum = 0;
	for (int j=0; j<n_threads; ++j) {
		retsum += (int) th[j].join();
	}
	printf("count_me is %d\n", count_me);
	printf("retsum is %d\n", retsum);
	return 0;
}
#if 1
static int _main_threadtest(int argc, char **argv) {
	const int n_threads = 10000;
	Thread th[n_threads];
	int i=0;
	while (i<n_threads) {
		for (; i<n_threads; ++i) {
			try {
				th[i].start(-1, 500, &increment_it, &count_me);
				//th[i].detach();
			} catch (ThreadException ex) {
				if (ex.getErrno() == EAGAIN) {
					cout << "try again" << endl;
					break;
				}
				throw;
			}
		}
		for (int j=0; j<i; ++j) {
			try {
				th[j].join();
			} catch (ThreadException ex) {
				if (ex.getErrno()==EINVAL || ex.getErrno()==ESRCH)
					continue;
				throw;
			}
		}
	}
	printf("count_me is %d\n", count_me);
}
#endif
//template<class T, class D> void wise_ptr<T,D>::dump() {
//	//T& obj = p==NULL ? "<null>" : *(T*)p;
//	std::cout << "@" << this << " : " << p << " " << prev << " " << next << "     " << endl;
//}

//typedef wise_ptr<const string, news_dealloc<const string> > string_ref_ptr;
typedef wise_ptr<const string> string_ref_ptr;

#ifndef USE_STD_SHARED_PTR
static void dump(string_ref_ptr &p1, string_ref_ptr &p2) {
	cout << "p1: " ; p1.dump();
	cout << "p2: " ; p2.dump();
}

struct A {
	A(int n0) : n(n0) {}
	int n;
	virtual void nn() {
		cout << "n is " << n << endl;
	}
};

struct B : public A {
	B(int n0, int o0) : A(n0), o(o0) {}
	int o;
	virtual void nn() {
		cout << "n is " << n << endl;
		cout << "o is " << o << endl;
	}
};

struct C : public B {
	C(int n0, int o0) : B(n0,o0) {}
	virtual void nn() {
		cout << "Im a C" << endl;
		B::nn();
	}
};

static void dumpmem(uint* mem, int n) {
	for (int i=0; i<n; ++i) {
		printf("%x ", mem[i]);
	}
	printf("\n");
}
static int _main_ptrtest(int argc, char **argv) {
	// compare
	A *aaa[4] = { new A(1), new A(2), new A(3), new A(4) };
	A* ap = aaa[0];
	wise_ptr<A> aw = wise_ptr<A>(aaa[1]);
	cout << "name: " << (typeid(&aw).name()) << endl;
	auto_ptr<A> aa = auto_ptr<A>(aaa[2]);
	shared_ptr<A> as = shared_ptr<A>(aaa[3]);
	dumpmem((uint*) aaa, 4);
	cout << "A pointer   " << sizeof(ap) << endl;
	dumpmem((uint*) &ap, sizeof(ap)/4);
	cout << "wise_ptr<A> " << sizeof(aw) << endl;
	dumpmem((uint*) &aw, sizeof(aw)/4);
	cout << "auto_ptr<A> " << sizeof(aa) << endl;
	dumpmem((uint*) &aa, sizeof(aa)/4);
	cout << "shrd_ptr<A> " << sizeof(as) << endl;
	dumpmem((uint*) &as, sizeof(as)/4);
	return 0;
	vector<wise_ptr<B> > bvec;
	const int N=4;
	for (int i=0; i<N; ++i) {
		bvec.push_back(new B(i,-i));
	}
	cout << (bvec[0] == bvec[1] ? "same" : "different") << endl;
	vector<wise_ptr<A> > avec(bvec.size());
	for (int i=0; i<N; ++i) {
		avec[i] = bvec[i];
		avec[i]->nn();
		avec[i].dump();
		bvec[i].dump();
	}
	reverse(bvec.begin(), bvec.end());
	for (int i=0; i<N; ++i) {
		bvec[i]->nn();
		avec[i].dump();
		bvec[i].dump();
	}

	return 0;
	wise_ptr<A> a1(new A(1));
	a1->nn();
	wise_ptr<B> b1(new B(2,3));
	b1->nn();
	wise_ptr<A> a2(b1);
	a2->nn();

	wise_ptr<C> c1(new C(4,5));
	c1->nn();
	b1 = c1;
	b1->nn();
	a1 = b1;
	a1->nn();

	//wise_ptr<B> b2(a1);
	//b2->nn();

	A* app = a1.get();
	//B* bp = static_cast<A*>(ap);

	return 0;
	// test wise_ptr
	const string *s1 = new string("foo");
	cout << "ptr " << *s1 << "  @ " << s1 << endl;
	string_ref_ptr p1(s1);
	string_ref_ptr p2;
	dump(p1,p2);
	cout << "-- p2 = p1" << endl;
	p2 = p1;
	dump(p1,p2);
	string_ref_ptr p3 = p2;
	dump(p1,p2);
	cout << "-- p2.unref() " << p2.unref() << endl;
	dump(p1,p2);
	cout << "-- p1.unref() " << p1.unref() << endl;
	cout << p1.unref() << endl;
	dump(p1,p2);
	//cout << "cnt " << *p1 << endl;
	cout << "p3: "; p3.dump();

	string *s2 = new string("bar");
	p3 = s2;
	// cout << "ptr " << *s1 << "  @ " << s1 << endl; // segfaults because s1 is invalid
	cout << "p3: "; p3.dump();
	p2 = p3;
	p1 = p2;
	dump(p1,p2);

	const char msg_str[] = "Message in a bottled array";
	//char *msg = new char[strlen(msg_str)+1];

//	wise<char>::array pmsg = newArray<char>(strlen(msg_str)+1);
//	strcpy(pmsg.ptr(), msg_str);
//	cout << pmsg.ptr() << endl;

	//wise_ptr<char, array_dealloc<char> > pmsg(msg);
	//delete[] msg;
	return 0;
}
#endif

static int _main_stringtest(int argc, char **argv) {
	vector<string> foo = misc::split("I once was lost", " ");
	cout << "Hello" << endl;
	vector<string>::iterator ii;
	for (ii=foo.begin(); ii!=foo.end(); ++ii) {
		cout << *ii << " ";
	}
	cout << endl;

	for (int i=0; i<foo.size(); ++i) {
		cout << foo[i] << " ";
	}
	cout << endl;
}



#include "bigint.h"
#include "sillyio.h"
using namespace bigint;
using namespace silly::misc;
using namespace silly::io;
static int _main_numtest(int argc, char **argv) {
	string sstr;
	if (argc>1)
		sstr = argv[1];
	int base=10;
	if (argc>2)
		base = atoi(argv[2]);

	typedef int i_qi __attribute__((__mode__(__QI__)));
	typedef int i_hi __attribute__((__mode__(__HI__)));
	typedef int i_si __attribute__((__mode__(__SI__)));
	typedef int i_di __attribute__((__mode__(__DI__)));
	//typedef int i_ti __attribute__((__mode__(__TI__)));

	//	printf("arg1: %s\n", sstr.c_str());
	BigInt n = BigInt::parseString(sstr, base);
	printf("i %u  l %u  ll %u  BN_ULONG %u\n", sizeof(int), sizeof(long), sizeof(long long), sizeof(BN_ULONG));
	printf("unsign: %lu %llu\n", n.toULong(), n.toULLong());
	printf("signed: %ld %lld\n", n.toLong(), n.toLLong());
	printf("in b32: %s\n", n.toString(32).c_str());
	printf("in hex: %s\n", n.toString(16).c_str());
	printf("in dec: %s\n", n.toString().c_str());
	printf("in oct: %s\n", n.toString(8).c_str());
	printf("in trn: %s\n", n.toString(3).c_str());
	printf("in bin: %s\n", n.toString(2).c_str());

	printf("in base64: %s\n", toBase64(BigInt::fromPosBigInt(n)).c_str());
	printf("pos: %s\n", toHexString(BigInt::fromPosBigInt(n)).c_str());
	printf("mpi: %s\n", toHexString(BigInt::MPIfromBigInt(n)).c_str());
	printf("2sc: %s\n", toHexString(BigInt::from2sCompBigInt(n)).c_str());
//	printf("pad: %s\n", toHexString(BigInt::fromPaddedBigInt(n)).c_str());
	return 0;

	byte_buf testbuf = BigInt::fromPosBigInt(n);
	long ttime = currentTimeMillis();
	for (int i=0; i<10000000; ++i) {
		toBase64(testbuf);
	}
	long ttime2 = currentTimeMillis();
	printf("reg %ld\n", ttime2 - ttime);
	ttime = ttime2;
//	for (int i=0; i<10000000; ++i) {
//		toBase64_goto_version(testbuf);
//	}
//	ttime2 = currentTimeMillis();
//	printf("krz %ld\n", ttime2 - ttime);
	return 0;
}


#include "sillymain.h"
MAIN("sillytest");


#include "sillythread.h"

#endif	// __INTEL_COMPILER
