#include "silly.h"
#include "sillymem.h"
#include "sillymem3.h"

#include <iostream>
#include <string.h>
#include <algorithm>

using namespace std;
using namespace silly;

#ifdef SILLYMEM_H_
using namespace silly::mem;

//template<class T, class D> void wise_ptr<T,D>::dump() {
//	//T& obj = p==NULL ? "<null>" : *(T*)p;
//	std::cout << "@" << this << " : " << p << " " << prev << " " << next << "     " << endl;
//}

//typedef wise_ptr<const string, news_dealloc<const string> > string_ref_ptr;
typedef wise_ptr<const string> string_ref_ptr;

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
static int _main(int argc, char **argv) {
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
#else

static int _main(int argc, char **argv) {
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
#endif
#include "sillymain.h"
MAIN("sillytest");


