/*
 * Parser.cpp
 *
 *  Created on: Aug 17, 2009
 *      Author: louis
 */

#include <iostream>
#include <fstream>
#include <string>
#include <map>
#include <vector>
#include <algorithm>
#include "shdl.h"

using namespace shdl;

//extern void _exit(int n);

#define D(x) cout << x << endl
#define DD(x)
//template<class T, class D> inline void wise_ptr<T,D>::dump() {
//	//T& obj = p==NULL ? "<null>" : *(T*)p;
//	std::cout << "@" << this << " : " << p << " " << prev << " " << next << "     " << endl;
//}

static void trim(string &s) {
	while (!s.empty() && s[s.length()-1] == ' ') {
		s.erase(s.length()-1);
	}
	while (!s.empty() && s[0] == ' ') {
		s.erase(0, 1);
	}
}

static void check(string s1, const char *s2) {
	if (s1 != s2) {
		ostringstream msg;
		msg << "check fail: "<< s1 << " != " << s2;
		throw ParseException(msg.str());
	}
}

int parseInt(string s) {
	int inp;
	istringstream ss(s);
	ss >> inp;
	if (ss.fail()) {
		ostringstream msg;
		msg << "Not an integer: " << s;
		throw ParseException(msg.str());
	}
	return inp;
}

Circuit_p Circuit::parseCirc(istream &in) {
	map<int,GateBase_p> gates;
	vector<Input_p> inputs;
	vector<Output_p> outputs;

	string line;
	string tmp;
	try {
		while (!in.eof()) {
			getline(in, line);
			if (in.fail())
				continue;
			//D("read line: " << line);
			string comment;
			uint comment_pos = line.find("//");
			if (comment_pos != string::npos) {
				comment = line.substr(comment_pos+2);
				if (!comment.empty() && comment[0]==' ')
					comment.erase(0, 1);
				line.erase(comment_pos);
			}
			trim(line);
			//D("chop line: " << line);
			if (line.empty())
				continue;

			istringstream lp(line);
			lp.exceptions(ios::failbit);
			int id;
			lp >> id;
			string gtype;
			lp >> gtype;
			//D("gtype: " << gtype);
			if (gtype == "input") {
				//D("input");
				Input_p g(new Input(id, id));
				DD(g.dump();)
				g->setComment(comment);
				gates[id] = g;
				inputs.push_back(g);
				DD(g.dump();)
				DD(gates[id].dump();)
				DD(inputs[inputs.size()-1].dump();)
				//g->write(cout);
			} else if (gtype == "gate" || gtype == "output") {
				Gate_p g;
				if (gtype == "output") {
					lp >> tmp; check(tmp, "gate");
					Output_p out(new Output(id));
					//out.dump();
					g = out;
					//g.dump();
					outputs.push_back(out);
					//out.dump();
				} else {
					g = Gate_p(new Gate(id));
				}
				//g.dump();

				lp >> tmp; check(tmp, "arity");
				int arity;
				lp >> arity;
				lp >> tmp; check(tmp, "table");
				lp >> tmp; check(tmp, "[");
				while ( (lp >> tmp), (tmp != "]")) {
					if (tmp == "1") {
						g->truthtab.push_back(true);
						continue;
					}
					check(tmp, "0");
					if (tmp == "0")
						g->truthtab.push_back(false);
				}
				if (g->truthtab.size() != (1<<arity)) {
					ostringstream msg;
					msg << "check fail: arity " << arity << " but tt.size " << g->truthtab.size();
					throw ParseException(msg.str());
				}
				lp >> tmp; check(tmp, "inputs");
				lp >> tmp; check(tmp, "[");

				while ( (lp >> tmp), (tmp != "]")) {
					int inp = parseInt(tmp);
					g->inputs.push_back(gates.at(inp));
				}
				if (g->inputs.size() != arity) {
					ostringstream msg;
					msg << "check fail: arity " << arity << " but inputs.size " << g->truthtab.size();
					throw ParseException(msg.str());

				}
				reverse(g->inputs.begin(), g->inputs.end());

				g->arity = arity;
				g->setComment(comment);

				g->write(cout);

				gates[id] = g;
			} else {
				ostringstream msg;
				msg << "unknown gate type " << gtype;
				throw ParseException(msg.str());
			}
		}
	} catch (ios::failure ex) {
		throw ParseException(ex.what());
	}

	Circuit_p cc(new Circuit());
	cc->inputs.swap(inputs);
	cc->outputs.swap(outputs);
	return cc;
}

static void shdltest() {
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

FmtFile::VarDesc FmtFile::VarDesc::filter(string w) {
	VarDesc bdv;
	map<int,string>::iterator it;
	for (it=who.begin(); it!=who.end(); ++it) {
		if (w == it->second) {
			bdv.who[it->first] = it->second;
		}
	}
	return bdv;
}


#if 0
public static VarDesc readFile(String file) {
		VarDesc bdv = new VarDesc();
		try {
			BufferedReader in = new BufferedReader(new FileReader(file));
			String line;
			while ((line = in.readLine()) != null) {
				line = line.trim();
				if (line.length() == 0 || line.charAt(0)=='#')
					continue;
				String[] fld = line.split(" ");
				if (fld.length != 2)
					throw new RuntimeException("Bad line: " + line);
				int n = Integer.parseInt(fld[0]);
				bdv.who.put(n, fld[1]);
			}
			return bdv;
		} catch (IOException ex) {
			ex.printStackTrace();
			throw new RuntimeException("Error reading BDD Desc file: " + file);
		}
	}
#endif

void FmtFile::mapBits(BigInt &n, valmap vals, string name) {
	//System.out.println("set bits: " + name + " = " + n);
	Obj obj = mapping.at(name);
	for (uint j=0; j<obj.bits.size(); ++j) {
		int i = obj.bits.at(j);
		//System.out.println("set bit " + j + " of " + name + " (" + i + ") = " + n.testBit(j));
		vals[i] = n.testBit(j);
	}
}

void FmtFile::mapBits(long n, valmap vals, string name) {
	BigInt nn(n);
	mapBits(nn, vals, name);
}

/*
	public void FmtFile::mapBits(byte[] bytes, TreeMap<Integer, Boolean> vals, String name) {
		mapBits(new BigInteger(1, bytes), vals, name);
	}
*/


void FmtFile::mapBits(vector<bool> n, valmap vals, string name) {
	Obj obj = mapping.at(name);
	for (uint j=0; j<obj.bits.size(); ++j) {
		int i = obj.bits.at(j);
		//System.out.println("set bit " + j + " of " + name + " (" + i + ") = " + n.testBit(j));
		vals[i] = n.at(j);
	}
}

// BUG: outputmap is wrong if format file is not monotonic

BigInt FmtFile::readBits(vector<bool> vals, string name) {
	//System.out.print("get bits: " + name);
	Obj obj = mapping.at(name);
	BigInt zz(0);
	for (uint j=0; j<obj.bits.size(); ++j) {
		int i = obj.bits.at(j);
		int ri = outputmap.at(i);

		//System.out.print(vals[ri] ? "1" : "0");
		if (vals.at(ri)) {
			zz = zz.setBit(j);
		}
	}
	//System.out.println(" = " + zz);
	return zz;

}

BigInt FmtFile::readBits(valmap vals, string name) {
	BigInt n(0);
	Obj obj = mapping.at(name);
	for (uint j=0; j<obj.bits.size(); ++j) {
		int i = obj.bits.at(j);
		if (vals.at(i))
			n = n.setBit(j);
	}
	return n;
}


FmtFile FmtFile::parseFmt(istream &in) {

	//BufferedReader in = new BufferedReader(new FileReader(file));
	string line;

	FmtFile fmt;
	int outputNum = 0;

	while (!in.eof()) {
		getline(in, line);
		if (in.fail())
			continue;
		vector<string> spl = split(line, " ");
		if (spl.size() < 5)
			continue;
		if (spl[2] != ("integer")) {
			ostringstream msg;
			msg << "Unknown type " << spl[2];
			throw ParseException(msg.str());
		}
		Obj obj;
		if (spl[0] == ("Alice"))
			obj.party = 0;
		else if (spl[0] == ("Bob"))
			obj.party = 1;
		else {
			ostringstream msg;
			msg << "Unknown actor " << spl[0];
			throw ParseException(msg.str());
		}

		if (spl[3].at(0) != '"' || spl[3].at(spl[3].length()-1) != '"') {
			ostringstream msg;
			msg << "Bad name " << spl[3];
			throw ParseException(msg.str());
		}

		obj.name = spl[3].substr(1, spl[3].length()-1);

		obj.bits.resize(spl.size() - 6);

		for (int i=5; i<spl.size()-1; ++i) {
			obj.bits[i-5] = parseInt(spl[i]);

			if (spl[1] == ("input")) {
				fmt.vardesc.who[obj.bits[i-5]] = (obj.party==1 ? "B" : "A");
				//System.out.println("inp " + obj.bits[i-5] + " : " + (obj.party==1 ? "B" : "A"));
			}

			if (spl[1] == ("output")) {
				fmt.outputmap[obj.bits[i-5]] = (outputNum++);
			}
		}

		fmt.mapping[obj.name] = obj;
	}

	outputNum = 0;

	map<int,int>::iterator it;
	for (it=fmt.outputmap.begin(); it != fmt.outputmap.end(); ++it) {
		it->second = (outputNum++);
	}

//	for (Integer i : fmt.outputmap.keySet()) {
//		fmt.outputmap.put(i, outputNum++);
//	}

	return fmt;
}

#include "CircuitCrypt.h"
#include "sillyio.h"

static void writeObject(DataOutput *out, SFEKey_p &key) {
	out->write(*key->getEncoded());
}

static void writeObject(DataOutput *out, boolean_secrets &secr) {
	writeObject(out, secr.s0);
	writeObject(out, secr.s1);
}

#include "GCircuitEval.h"

static int _main(int argc, char **argv) {
	try {
		cout << "1 " << 1 << " " << parseInt("1") << endl;
		//shdl::shdltest();
		// test fmtfile
		ifstream fmtin("/home/louis/sfe/priveq.fmt");
		FmtFile::parseFmt(fmtin);
		cout << "Read fmt file" << endl;
		// test circuit
		ifstream in("/home/louis/sfe/priveq.circ");
		Circuit_p cc = Circuit::parseCirc(in);

		for (int i=0; i<cc->outputs.size() ; ++i) {
			DD(cc->outputs[i].dump();)
		}

		CircuitCrypt crypt;
		vector<boolean_secrets> inputSecrets;
		GarbledCircuit gcc = crypt.encrypt(*cc, inputSecrets);
		//cout << (&gcc) << endl;
		cout << "write garbled circuit" << endl;
		{
			ofstream fout("gcircuit.bin");
			silly::io::ostreamDataOutput fdout(fout);
			gcc.writeCircuit(&fdout);
			fout.close();
		}
		cout << "write input secrets" << endl;
		{
			ofstream fout("gsecrets.bin");
			silly::io::ostreamDataOutput fdout(fout);
			writeVector(&fdout, inputSecrets);
			fout.close();
		}
		cout << "evaluate garbled circuit" << endl;
		GCircuitEval geval;
		vector<SecretKey_p> gcirc_input(inputSecrets.size());
		for (uint i=0; i<gcirc_input.size(); ++i) {
			gcirc_input[i] = inputSecrets[i].s0;
		}
		vector<bool> circ_out = geval.eval(gcc, gcirc_input);
		for (uint i=0; i<circ_out.size(); ++i) {
			cout << "output " << i << ": " << circ_out[i] << endl;
		}
	} catch (ParseException ex) {
		cerr << ex.what() << endl;
		throw;
	}

	return 0;
}
#include "sillymain.h"

MAIN("shdlparse")

