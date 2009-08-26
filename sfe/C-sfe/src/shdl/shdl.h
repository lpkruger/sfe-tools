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
#include <string.h>

using namespace std;

#include "silly.h"
#include "sillyio.h"
#include "bigint.h"

namespace shdl {

class ParseException :public std::exception {
	char* msg;

public:
	ParseException() {}
	ParseException(const char* msg0) {
		msg = new char[strlen(msg0)];
		strcpy(msg, msg0);
	}
	ParseException(const string& msgs) {
		const char* msg0 = msgs.c_str();
		msg = new char[strlen(msg0)];
		strcpy(msg, msg0);
	}

	// virtual overrides
	const char* what() throw() {
		return msg;
	}
	~ParseException() throw() {
		if (!msg)
			delete[] msg;
	}

};
using namespace silly::misc;
using namespace bigint;

class Circuit;
class Gate;
class Input;
class Output;


struct FmtFile {
	struct Obj {
		string name;
		int party;  // 0 for Alice, 1 for Bob
		vector<int> bits;
	};
	struct VarDesc {
		map<int,string> who;
		VarDesc filter(string w);
	};

	map<string,Obj> mapping;
	VarDesc vardesc;
	map<int,int> outputmap;

	typedef map<int,bool> valmap;

	void mapBits(BigInt &n, valmap vals, string name);
	void mapBits(long n, valmap vals, string name);
	void mapBits(vector<bool> n, valmap vals, string name);
	BigInt readBits(vector<bool> vals, string name);
	BigInt readBits(valmap vals, string name);
	VarDesc getVarDesc() {
			return vardesc;
	}

	static FmtFile parseFmt(istream &in);
};


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
	virtual void write(ostream &out) = 0;
	//virtual GateBase copy_rec(Map<GateBase,GateBase> map);
};

class Circuit {
public:
	vector<Input*> inputs;
	vector<Output*> outputs;

	static Circuit *parseCirc(istream &in);
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

	void write0(ostream &out) {
		out << " arity " << arity << " table [ ";
		for (uint i=0; i<truthtab.size(); ++i) {
			out << (truthtab[i] ? "1 " : "0 ");
		}
		out << "] inputs [ ";
		for (uint i=0; i<inputs.size(); ++i) {
			out << inputs[inputs.size()-1-i]->id << " ";
		}
		out << "]";
		if (!comment.empty()) {
			out << " // " << comment;
		}
		out << endl;
	}
	void write(ostream &out) {
		out << id << " gate";
		write0(out);
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
	void write(ostream &out) {
		out << id << " output gate";
		write0(out);
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
	virtual void write(ostream &out) {
		if (!comment.empty()) {
			out << id << " input  // " << comment << endl;
		} else {
			out << id << " input  // " << var << endl;
		}
	}

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
