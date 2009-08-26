#include "silly.h"

#include <iostream>


using namespace std;
using namespace silly;

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

#include "sillymain.h"
MAIN("sillytest");


