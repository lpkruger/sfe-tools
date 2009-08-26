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

Circuit *Circuit::parseCirc(istream &in) {
	map<int,GateBase*> gates;
	vector<Input*> inputs;
	vector<Output*> outputs;

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
				Input *g = new Input(id, id);
				g->setComment(comment);
				g->write(cout);
				gates[id] = g;
				inputs.push_back(g);
			} else if (gtype == "gate" || gtype == "output") {
				Gate *g;
				if (gtype == "output") {
					lp >> tmp; check(tmp, "gate");
					Output *out = new Output(id);
					g = out;
					outputs.push_back(out);
				} else {
					g = new Gate(id);
				}

				lp >> tmp; check(tmp, "arity");
				int arity;
				lp >> arity;
				lp >> tmp; check(tmp, "table");
				lp >> tmp; check(tmp, "[");
				vector<bool> tt;
				while ( (lp >> tmp), (tmp != "]")) {
					if (tmp == "1") {
						tt.push_back(true);
						continue;
					}
					check(tmp, "0");
					if (tmp == "0")
						tt.push_back(false);
				}
				if (tt.size() != (1<<arity)) {
					ostringstream msg;
					msg << "check fail: arity " << arity << " but tt.size " << tt.size();
					throw ParseException(msg.str());
				}
				lp >> tmp; check(tmp, "inputs");
				lp >> tmp; check(tmp, "[");
				vector<GateBase*> inputs;
				while ( (lp >> tmp), (tmp != "]")) {
					int inp = parseInt(tmp);
					inputs.push_back(gates.at(inp));
				}
				if (inputs.size() != arity) {
					ostringstream msg;
					msg << "check fail: arity " << arity << " but inputs.size " << tt.size();
					throw ParseException(msg.str());

				}
				reverse(inputs.begin(), inputs.end());

				g->arity = arity;
				g->inputs.swap(inputs);
				g->truthtab.swap(tt);
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

	Circuit *cc = new Circuit();
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
		Circuit *cc = Circuit::parseCirc(in);
		delete cc;
		return 0;
	} catch (ParseException ex) {
		cerr << ex.what() << endl;
		throw;
	}
}
#include "sillymain.h"

MAIN("shdlparse")

