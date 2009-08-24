/*
 * Parser.cpp
 *
 *  Created on: Aug 17, 2009
 *      Author: louis
 */

#include "Parser.h"
#include "shdl.h"
#include <iostream>
#include <string>


namespace shdl {

Circuit parse(istream &in) {
	while (!in.eofbit) {
		string line;
		getline(in, line);
		istringstream lp(line);
		int id;
		lp >> id;
		string word;
		lp >> word;
	}
}
void shdltest() {
	Circuit cc;
	Input i0(0,0);
	Input i1(1,1);
	Gate g(2);
	Output o(3);

	cc.inputs.push_back(&i0);
	cc.inputs.push_back(&i1);
	cc.outputs.push_back(&o);

	g.arity = 2;
	g.truthtab = TT_AND();
	g.inputs.push_back(&i0);
	g.inputs.push_back(&i1);

	vector<bool> bits;
	bits.push_back(false);
	bits.push_back(true);
	EvalState state(cc, bits);
	g.eval(state);
}
}
