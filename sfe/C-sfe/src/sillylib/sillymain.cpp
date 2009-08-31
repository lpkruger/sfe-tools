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


//// backtrace function ////
#include <stdio.h>
#include <stdlib.h>
#include <execinfo.h>

void print_backtrace(int depth=10){
	void *addresses[depth];
	char **strings;

	int size = backtrace(addresses, depth);
	strings = backtrace_symbols(addresses, size);
	fprintf(stderr, "Stack frames: %d\n", size);
	for(int i = 0; i < size; i++)
	{
		fprintf(stderr, "%d: %08X\t", i, (int)addresses[i]);
		fprintf(stderr, "%s\n", strings[i]);
	}
	free(strings);
}
///////////////////////////

int main_go(main_ptr main_f, int argc, char **argv) {
	return main_f(argc, argv);
}

#define ARG0 1
int main(int argc, char **argv) {
	smap_it it;
	smap &map = mainmap();
	char *prog;

	if (argc<2-ARG0) {
		goto usage;
	}

	prog = rindex(argv[1-ARG0], '/');
	(prog++) || (prog=argv[1-ARG0]);

	it = map.find(prog);
	if (it == map.end()) {
		cout << "No such program " << prog << endl << endl;
		goto usage;
	}

	return main_go((it->second), argc-1+ARG0, argv+1-ARG0);

	usage:
	cout << "All programs:" << endl;
	for (it=map.begin(); it!=map.end(); ++it) {
		cout << it->first << endl;
	}
	return 0;
}
