/*
 * Parser.h
 *
 *  Created on: Aug 17, 2009
 *      Author: louis
 */

#ifndef SHDL_H_
#define SHDL_H_

#include <vector>
#include <set>
#include <map>
#include <string>
#include <sstream>

using namespace std;

#include "../sillylib/silly.h"
#include "../sillylib/sillyio.h"
namespace shdl {

using namespace silly;

class Circuit;
class Gate;
class Input;
class Output;


class EvalState {
public:
	map<int,bool> vals;
	EvalState(Circuit &cc, vector<bool> &in);
};

class GateBase {

public:
	int id;
	set<Gate*> deps;
	GateBase(int id0) : id(id0) {
		//this.id = id0;
	}
	virtual bool eval(EvalState &state) = 0;
	//virtual void write(PrintStream out);

	string comment;
	void setComment(string s) {
		comment = s;
	}
	string getComment() {
		return comment;
	}

	virtual string toString() = 0;
	//virtual GateBase copy_rec(Map<GateBase,GateBase> map);
};

class Circuit {
public:
	vector<Input*> inputs;
	vector<Output*> outputs;
};


class Gate : public GateBase {
public:
	int arity;
	vector<bool> truthtab;  // truth table
	vector<GateBase*> inputs;

	Gate(int id0) : GateBase(id0) {}

	virtual string toString() {
		return string("[Gate ") + id + " arity " + arity + "]";
	}
	//virtual bool eval(EvalState &state) {return false;}
	bool eval(EvalState &state) {
		map<int,bool>::iterator out = state.vals.find(id);
		if (out != state.vals.end())
			return out->second;

		bool x=false,y=false,z=false;

		map<int,bool>::iterator b = state.vals.find(id);
		if (b != state.vals.end())
			return b->second;

		int n = 0;
		switch(arity) {
		case 3:
			z = inputs[2]->eval(state);
			n = (z?1:0);
		case 2:
			y = inputs[1]->eval(state);
			n += (y?1:0) << (arity - 2);
		case 1:
			x = inputs[0]->eval(state);
			n += (x?1:0) << (arity - 1);
		}

		state.vals[id] = truthtab[n];
		return truthtab[n];
	}

//
//		public Gate(int id) {
//			super(id);
//		}
};

class Output : public Gate {
public:
	Output(int id0) : Gate(id0) {}
	string toString() {
		return string("[Output ") + id + " arity " + arity + "]";
	}
};

class Input : public GateBase {
public:
	int var;
	Input(int id0, int var0) : GateBase(id0), var(var0) {
		//this.var = var;
	}
	string toString() {
		return string("[Input ") + id + " (" + var + ") ]";
	}
	virtual bool eval(EvalState &state) {
		map<int,bool>::iterator b = state.vals.find(id);
		if (b == state.vals.end()) {
			///throw new RuntimeException("Input " + id + " is undefined");
			//TODO:
		}
		return b->second;
	}

//		public void write(PrintStream out) {
//			if (comment != null) {
//				out.println(id + " input  // " + comment);
//			} else {
//				out.println(id + " input  // " + var);
//			}
//		}

};

inline EvalState::EvalState(Circuit &cc, vector<bool> &in) {
	for (uint i=0; i<in.size(); ++i) {
		vals[cc.inputs[i]->id] = in[i];
	}
}



#define STD_TTS 1
#ifdef STD_TTS

#define VECTOR(a) vector<typeof(a[0])>(a, a + sizeof(a)/sizeof(a[0]))

static inline vector<bool> TT_XOR() {
	bool tt[] = { false, true, true, false };
	return VECTOR(tt);
}
static inline vector<bool> TT_XOR3() {
	bool tt[] = { false, true, true, false, true, false, false, true};
	return VECTOR(tt);
}
static inline vector<bool> TT_XNOR() {
	bool tt[] = { true, false, false, true };
	return VECTOR(tt);
}
static inline vector<bool> TT_AND() {
	bool tt[] = { false, false, false, true };
	return VECTOR(tt);
}
static inline vector<bool> TT_OR() {
	bool tt[] = { false, true, true, true };
	return VECTOR(tt);
}
static inline vector<bool> TT_ANDNOT() {
	//boolean[] tt = { false, true, false, false };
	bool tt[] = { false, false, true, false };
	return VECTOR(tt);
}
static inline vector<bool> TT_AND3() {
	bool tt[] = { false, false, false, false, false, false, false, true };
	return VECTOR(tt);
}
static inline vector<bool> TT_ADDCARRY3() {
	// carry, left, right -> next carry
	bool tt[] = { false, false, false, true, false, true, true, true };
	return VECTOR(tt);
}
static inline vector<bool> TT_SUBCARRY3() {
	//boolean[] tt = { false, true, true, true, false, false, false, false };
	// carry, left, right -> next carry
	bool tt[] = { false, true, false, false, true, true, false, true };
	return VECTOR(tt);
}
static inline vector<bool> TT_EQ3() {
	bool tt[] = { false, false, false, false, true, false, false, true };
	return VECTOR(tt);
}
static inline vector<bool> TT_NEQ3() {
	bool tt[] = { false, true, true, false, true, true, true, true};
	return VECTOR(tt);
}

// if arg1 true: arg2, false: arg3
static inline vector<bool> TT_MUX() {
	bool tt[] = { false, true, false, true, false, false, true, true};
	return VECTOR(tt);
}
#undef VECTOR

#endif




} // namespace shdl
#endif
