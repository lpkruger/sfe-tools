/*
 * Parser.h
 *
 *  Created on: Aug 17, 2009
 *      Author: louis
 */

#ifndef SHDL_H_
#define SHDL_H_

#include <vector>
#include <list>
#include <set>
#include <map>
#include <string>
#include <sstream>
#include <stdexcept>
#include <string.h>

using namespace std;

#include "silly.h"
#include "sillyio.h"
#include "sillymem.h"
#include "bigint.h"

namespace shdl {

struct ParseException : public silly::MsgBufferException {
	ParseException(const char *msg0) : MsgBufferException(msg0) {}
	ParseException(const string &msg0) : MsgBufferException(msg0.c_str()) {}
};

using namespace silly::misc;
using namespace silly::mem;
using namespace bigint;

class Circuit;
class GateBase;
class Gate;
class Input;
class Output;

typedef wise_ptr<Circuit> Circuit_p;
typedef GateBase* GateBase_p;
typedef Gate* Gate_p;
typedef Input* Input_p;
typedef Output* Output_p;

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

	void mapBits(const BigInt &n, valmap &vals, const string &name);
	void mapBits(long n, valmap &vals, const string &name);
	void mapBits(const bit_vector &n, valmap &vals, const string &name);
	BigInt readBits(const bit_vector &vals, const string &name);
	BigInt readBits(const valmap &vals, const string &name);
	VarDesc getVarDesc() {
			return vardesc;
	}
	int numPrefix(const char* prefix, int party);

	int numInputs(int party) {
		return numPrefix("input.", party);
	}
	int numOutputs(int party) {
		return numPrefix("output.", party);
	}
	static FmtFile parseFmt(istream &in);
};


class EvalState {
public:
	map<int,bool> vals;
	EvalState(Circuit &cc, bit_vector &in);
};

class GateBase {
protected:
	virtual GateBase* deepCopy0(map<const GateBase*,GateBase_p> &mapping, Reclaimer<GateBase> &trash) const = 0;
	virtual void deepCopy1(map<const GateBase*,GateBase_p> &mapping, GateBase *g, Reclaimer<GateBase> &trash) const {
		g->id = id;
	}

	GateBase_p deepCopy(map<const GateBase*,GateBase_p> &mapping, Reclaimer<GateBase> &trash) const {
		map<const GateBase*,GateBase_p>::iterator it;
		it = mapping.find(this);
		if (it != mapping.end()) {
			return it->second;
		}
		GateBase_p pp = GateBase_p(deepCopy0(mapping, trash));
		mapping[this] = pp;
		return pp;
	}
	friend class Circuit;
	friend class Gate;
public:
	int id;
	set<Gate_p> deps;
	GateBase(int id0) : id(id0) {
		//this.id = id0;
	}

	virtual bool isInput() { return false; }
	virtual bool isOuput() { return false; }

	virtual ~GateBase() {}

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

class Circuit : public Reclaimer<GateBase>{
public:
	vector<Input_p> inputs;
	vector<Output_p> outputs;

	static Circuit_p parseCirc(istream &in);

	void calcDeps();
	void clearDeps();

	Circuit_p deepCopy() const;

	Circuit() {}
	~Circuit() { clearDeps(); }
};

class Gate : public GateBase {
public:
	int arity;
	bit_vector truthtab;  // truth table
	vector<GateBase_p> inputs;

	Gate(int id0) : GateBase(id0) {}

	virtual GateBase* deepCopy0(map<const GateBase*,GateBase_p> &mapping, Reclaimer<GateBase> &trash) const {
		Gate *gg = new Gate(id);
		trash.add_garbage(gg);
		deepCopy1(mapping, gg, trash);
		return gg;
	}

	virtual void deepCopy1(map<const GateBase*,GateBase_p> &mapping, GateBase *gg, Reclaimer<GateBase> &trash) const {
		GateBase::deepCopy1(mapping, gg, trash);
		Gate *g = (Gate*) gg;
		g->arity = arity;
		g->truthtab = truthtab;
		g->inputs.resize(inputs.size());
		for (uint i=0; i < inputs.size(); ++i) {
			g->inputs[i] = inputs[i]->deepCopy(mapping, trash);
		}
	}
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
	Output(const Output& copy) : Gate(copy) {}
	virtual GateBase* deepCopy0(map<const GateBase*,GateBase_p> &mapping, Reclaimer<GateBase> &trash) const {
		Output *g = new Output(id);
		trash.add_garbage(g);
		deepCopy1(mapping, g, trash);
		return g;
	}

	string toString() {
		return string("[Output ") + id + " arity " + arity + "]";
	}
	void write(ostream &out) {
		out << id << " output gate";
		write0(out);
	}

	virtual bool isOuput() { return true; }
};

class Input : public GateBase {
public:
	int var;
	Input(int id0, int var0) : GateBase(id0), var(var0) {}

	virtual GateBase* deepCopy0(map<const GateBase*,GateBase_p> &mapping, Reclaimer<GateBase> &trash) const {
		Input *g = new Input(id, var);
		trash.add_garbage(g);
		return g;
	}

	virtual void deepCopy1(map<const GateBase*,GateBase_p> &mapping, GateBase *gg, Reclaimer<GateBase> &trash) const {}

	string toString() {
		return string("[Input ") + id + " (" + var + ") ]";
	}
	virtual bool eval(EvalState &state) {
		map<int,bool>::iterator b = state.vals.find(id);
		if (b == state.vals.end()) {
			throw std::logic_error(cstr_printf("Input %d is undefined", id));
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

	virtual bool isInput() { return true; }
	virtual bool isOuput() { return false; }
};


inline Circuit_p Circuit::deepCopy() const {
	Circuit_p pNewCirc = Circuit_p(new Circuit());
	Circuit& newCirc = *pNewCirc;
	map<const GateBase*,GateBase_p> mapping;
	newCirc.inputs.resize(inputs.size());
	newCirc.outputs.resize(outputs.size());
	for (uint i=0; i<inputs.size(); ++i) {
		//Input_p ppp(dynamic_pointer_cast<Input>(inputs[i]->deepCopy(mapping)));
		Input *ppp = static_cast<Input*>(inputs[i]->deepCopy(mapping, *pNewCirc));
		newCirc.inputs[i] = ppp;
	}
	for (uint i=0; i<outputs.size(); ++i) {
		//Output_p ppp(dynamic_pointer_cast<Output>(outputs[i]->deepCopy(mapping)));
		Output *ppp = static_cast<Output*>(outputs[i]->deepCopy(mapping, *pNewCirc));
		newCirc.outputs[i] = ppp;
	}
	return pNewCirc;
}

inline EvalState::EvalState(Circuit &cc, bit_vector &in) {
	for (uint i=0; i<in.size(); ++i) {
		vals[cc.inputs[i]->id] = in[i];
	}
}



#define STD_TTS 1
#ifdef STD_TTS

#define VECTOR(a) vector<typeof(a[0])>(a, a + sizeof(a)/sizeof(a[0]))

static inline bit_vector TT_XOR() {
	bool tt[] = { false, true, true, false };
	return VECTOR(tt);
}
static inline bit_vector TT_XOR3() {
	bool tt[] = { false, true, true, false, true, false, false, true};
	return VECTOR(tt);
}
static inline bit_vector TT_XNOR() {
	bool tt[] = { true, false, false, true };
	return VECTOR(tt);
}
static inline bit_vector TT_AND() {
	bool tt[] = { false, false, false, true };
	return VECTOR(tt);
}
static inline bit_vector TT_OR() {
	bool tt[] = { false, true, true, true };
	return VECTOR(tt);
}
static inline bit_vector TT_ANDNOT() {
	//boolean[] tt = { false, true, false, false };
	bool tt[] = { false, false, true, false };
	return VECTOR(tt);
}
static inline bit_vector TT_AND3() {
	bool tt[] = { false, false, false, false, false, false, false, true };
	return VECTOR(tt);
}
static inline bit_vector TT_ADDCARRY3() {
	// carry, left, right -> next carry
	bool tt[] = { false, false, false, true, false, true, true, true };
	return VECTOR(tt);
}
static inline bit_vector TT_SUBCARRY3() {
	//boolean[] tt = { false, true, true, true, false, false, false, false };
	// carry, left, right -> next carry
	bool tt[] = { false, true, false, false, true, true, false, true };
	return VECTOR(tt);
}
static inline bit_vector TT_EQ3() {
	bool tt[] = { false, false, false, false, true, false, false, true };
	return VECTOR(tt);
}
static inline bit_vector TT_NEQ3() {
	bool tt[] = { false, true, true, false, true, true, true, true};
	return VECTOR(tt);
}

// if arg1 true: arg2, false: arg3
static inline bit_vector TT_MUX() {
	bool tt[] = { false, true, false, true, false, false, true, true};
	return VECTOR(tt);
}
#undef VECTOR
#endif




} // namespace shdl
#endif
