/*
 * sillymain.cpp
 *
 *  Created on: Aug 23, 2009
 *      Author: louis
 */


#include <map>
#include <iostream>
#include <string.h>
#include "sillymain.h"

using namespace std;

struct scmp {
	bool operator() (const char* lhs, const char* rhs) const
	{return strcmp(lhs,rhs)<0;}
};

typedef map<const char*, main_ptr, scmp>::iterator smap_it;
typedef map<const char*, main_ptr, scmp> smap;


smap &mainmap() {
	static smap themap;
	return themap;
}
void* add_main(const char* name, main_ptr main_f) {
	mainmap()[name] = main_f;
	return NULL;
}




static int main_go(main_ptr main_f, int argc, char **argv) {
	return main_f(argc, argv);
}

#ifdef MAINPROG
#define STRINGIFY(x) #x
#define STRING(x) STRINGIFY(x)
int main(int argc, char **argv) {
	main_go(mainmap().at(STRING(MAINPROG)), argc, argv);
}
#else

static int usage() {
	smap_it it;
	smap &map = mainmap();

	cout << "All programs:" << endl;
	for (it=map.begin(); it!=map.end(); ++it) {
		cout << it->first << endl;
	}
	return 1;
}


template<int ARG0>
static int mainmain(int argc, char **argv)
{
	smap_it it;
	smap &map = mainmap();
	char *prog;

	if (argc<2-ARG0) {
		return usage();
	}

	prog = rindex(argv[1-ARG0], '/');
	(prog++) || (prog=argv[1-ARG0]);

	it = map.find(prog);
	if (it == map.end()) {
		if (ARG0 == 1) {
			return mainmain<0>(argc, argv);
		}
		cout << "No such program " << prog << endl << endl;
		return usage();
	}

	return main_go((it->second), argc-1+ARG0, argv+1-ARG0);
}

int main(int argc, char **argv) {
	mainmain<1>(argc, argv);
}

#endif
