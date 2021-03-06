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

//#define DEBUG 1
#include "sillydebug.h"

using namespace shdl;

//extern void _exit(int n);

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

	Circuit_p cc(new Circuit());

	string line;
	string tmp;
	try {
		while (!in.eof()) {
			getline(in, line);
			if (in.fail())
				continue;
			//DC("read line: " << line);
			string comment;
			size_t comment_pos = line.find("//");
			if (comment_pos != string::npos) {
				comment = line.substr(comment_pos+2);
				if (!comment.empty() && comment[0]==' ')
					comment.erase(0, 1);
				line.erase(comment_pos);
			}
			trim(line);
			//DC("chop line: " << line);
			if (line.empty())
				continue;

			istringstream lp(line);
			lp.exceptions(ios::failbit);
			int id;
			lp >> id;
			string gtype;
			lp >> gtype;
			//DC("gtype: " << gtype);
			if (gtype == "input") {
				//DC("input");
				Input_p g(new Input(id, id));
				cc->add_garbage(g);
				DD(g.dump());
				g->setComment(comment);
				gates[id] = g;
				inputs.push_back(g);
				DD(g.dump());
				DD(gates[id].dump());
				DD(inputs[inputs.size()-1].dump());
				//g->write(cout);
			} else if (gtype == "gate" || gtype == "output") {
				Gate_p g;
				if (gtype == "output") {
					lp >> tmp; check(tmp, "gate");
					Output_p out(new Output(id));
					cc->add_garbage(out);
					//out.dump();
					g = out;
					//g.dump();
					outputs.push_back(out);
					//out.dump();
				} else {
					g = Gate_p(new Gate(id));
					cc->add_garbage(g);
				}
				//g.dump();

				lp >> tmp; check(tmp, "arity");
				uint arity;
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
				if (g->truthtab.size() != (1u<<arity)) {
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

				//g->write(cout);

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

	cc->inputs.swap(inputs);
	cc->outputs.swap(outputs);
	return cc;
}

#if 0
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

	bit_vector bits;
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
#endif

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


int FmtFile::numPrefix(const char* prefix, int party) {
	int len = strlen(prefix);
	int count = 0;
	map<string,Obj>::iterator it;
	for (it=mapping.begin(); it!=mapping.end(); ++it) {
		Obj &obj = it->second;
		if (obj.party == party && obj.name.substr(0, len) == prefix)
			count += obj.bits.size();
	}
	return count;
}

void FmtFile::mapBits(const BigInt &n, valmap &vals, const string &name) {
	//System.out.println("set bits: " + name + " = " + n);
	Obj obj = mapping.at(name);
	for (uint j=0; j<obj.bits.size(); ++j) {
		int i = obj.bits.at(j);
		//System.out.println("set bit " + j + " of " + name + " (" + i + ") = " + n.testBit(j));
		vals[i] = n.testBit(j);
	}
}

void FmtFile::mapBits(long n, valmap &vals, const string &name) {
	BigInt nn(n);
	mapBits(nn, vals, name);
}

/*
	public void FmtFile::mapBits(byte[] bytes, TreeMap<Integer, Boolean> vals, String name) {
		mapBits(new BigInteger(1, bytes), vals, name);
	}
*/


void FmtFile::mapBits(const bit_vector &n, valmap &vals, const string &name) {
	Obj obj = mapping.at(name);
	for (uint j=0; j<obj.bits.size(); ++j) {
		int i = obj.bits.at(j);
		//System.out.println("set bit " + j + " of " + name + " (" + i + ") = " + n.testBit(j));
		vals[i] = n.at(j);
	}
}

// BUG: outputmap is wrong if format file is not monotonic

BigInt FmtFile::readBits(const bit_vector &vals, const string &name) {
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

BigInt FmtFile::readBits(const valmap &vals, const string &name) {
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
	vector<string> spl;

	while (!in.eof()) {
		getline(in, line);
		if (in.fail())
			continue;
		spl.clear();
		spl = split(line, " ");
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

		obj.name = spl[3].substr(1, spl[3].length()-2);

		obj.bits.resize(spl.size() - 6);

		for (uint i=5; i<spl.size()-1; ++i) {
			obj.bits[i-5] = parseInt(spl[i]);

			if (spl[1] == ("input")) {
				fmt.vardesc.who[obj.bits[i-5]] = (obj.party==1 ? "B" : "A");
				//System.out.println("inp " + obj.bits[i-5] + " : " + (obj.party==1 ? "B" : "A"));
			}

			if (spl[1] == ("output")) {
				fmt.outputmap[obj.bits[i-5]] = (outputNum++);
			}
		}
		DF("%s\n", obj.name.c_str());
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
#include "CircuitCryptPermute.h"
#include "sillyio.h"
#include "GCircuitEval.h"
#include "../crypto/cipher/PseudoRandom.h"

string getDefaultDir(const string &f) {
	char *home = getenv("HOME");
	string dir;
	if (home)
		(dir = home) += "/.sfe/";
	else
		dir = ("./");
	return (dir + f);
}

void get_circuits(const char* prefix, vector<GarbledCircuit_p> &gcc,
		vector<vector<boolean_secrets> > &inp_secs,	vector<byte_buf> &seeds, uint num, uint max) {
	for (uint n=0; n<max; ++n) {
		if (gcc.size() >= num)
			return;
		{
			ostringstream ostr;
			ostr << prefix << "." << n << ".grbl.bin";
			ifstream fin(ostr.str().c_str());
			if (!fin.is_open())
				continue;
			//cout << "read " << ostr.str() << endl;
			silly::io::istreamDataInput ufdin(fin);
			silly::io::BufferedDataInput fdin(&ufdin);
			gcc.push_back(GarbledCircuit::readCircuit(&fdin));
			fin.close();
		}
		{
			ostringstream ostr;
			ostr << prefix << "." << n << ".secr.bin";
			ifstream fin(ostr.str().c_str());
			if (!fin.is_open()) {
				gcc.pop_back();
				continue;
			}
			//cout << "read " << ostr.str() << endl;
			silly::io::istreamDataInput ufdin(fin);
			silly::io::BufferedDataInput fdin(&ufdin);
			vector<boolean_secrets> inputSecrets;
			readVector(&fdin, inputSecrets);
			fin.close();
			inp_secs.push_back(inputSecrets);
		}
		{
			ostringstream ostr;
			ostr << prefix << "." << n << ".seed.bin";
			ifstream fin(ostr.str().c_str());
			silly::io::istreamDataInput fdin(fin);
			if (!fin.is_open()) {
				gcc.pop_back();
				inp_secs.pop_back();
				continue;
			}
			//cout << "read " << ostr.str() << endl;
			byte_buf seed;
			readVector(&fdin, seed);
			fdin.close();
			seeds.push_back(seed);
		}
		cerr << "@";
	}
}
void pre_generate(Circuit *cc, const char* prefix, int min, int max) {
	ifstream fin;
	ofstream fout;
	crypto::SecureRandom rand;
	byte_buf seed(128/8);
	for (int n=min; n<=max; ++n)
	{
		ostringstream ostr;
		ostr << prefix << "." << n << ".grbl.bin";
		fin.open(ostr.str().c_str());
		if (fin.is_open()) {
			fin.close();
			continue;
		}
		fin.close();
		fout.open(ostr.str().c_str());
		if (!fout.is_open()) {
			continue;
		}

		rand.getBytes(seed);

		CircuitCryptPermute crypt(new crypto::cipher::PseudoRandom(seed));
		vector<boolean_secrets> inputSecrets;
		Circuit_p copy = cc->deepCopy();
		GarbledCircuit_p gcc = crypt.encrypt(*copy, inputSecrets);
		copy = Circuit_p();
		//cout << (&gcc) << endl;
		cout << "write garbled circuit " << n << " to " << ostr.str() << endl;
		{
			silly::io::ostreamDataOutput fdout(fout);
			gcc->writeCircuit(&fdout);
			fout.close();
		}
		{
			ostr.str("");
			ostr.clear();
			ostr << prefix << "." << n << ".secr.bin";
			fout.open(ostr.str().c_str());
			cout << "write input secrets " << n << " to " << ostr.str() << endl;
			silly::io::ostreamDataOutput fdout(fout);
			writeVector(&fdout, inputSecrets);
			fout.close();
		}
		{
			ostr.str("");
			ostr.clear();
			ostr << prefix << "." << n << ".seed.bin";
			fout.open(ostr.str().c_str());
			cout << "write seed " << n <<  " to " << ostr.str() << endl;
			silly::io::ostreamDataOutput fdout(fout);
			writeVector(&fdout, seed);
			fout.close();
		}
	}
}

static int _main(int argc, char **argv) {
	try {
		cout << "1 " << 1 << " " << parseInt("1") << endl;
		//shdl::shdltest();
		// test fmtfile
		//ifstream fmtin("/home/louis/sfe/priveq.fmt");
		//ifstream fmtin("/home/louis/sfe/priveq2.fmt");
		ifstream fmtin("/home/louis/sfe/md5_pw_cmp.fmt");
		FmtFile::parseFmt(fmtin);
		cout << "Read fmt file" << endl;
		// test circuit
		//ifstream in("/home/louis/sfe/priveq.circ");
		//ifstream in("/home/louis/sfe/priveq2.circ");
		ifstream in("/home/louis/sfe/md5_pw_cmp.circ");
		Circuit_p cc = Circuit::parseCirc(in);

		for (uint i=0; i<cc->outputs.size() ; ++i) {
			DD(cc->outputs[i].dump());
		}

		CircuitCryptPermute crypt(new crypto::SecureRandom());
		//CircuitCrypt crypt;
		vector<boolean_secrets> inputSecrets;
		Circuit_p copy = cc->deepCopy();
		GarbledCircuit_p gcc = crypt.encrypt(*copy, inputSecrets);
		copy = Circuit_p();
		//cout << (&gcc) << endl;
		cout << "write garbled circuit" << endl;
		{
			ofstream fout("gcircuit.bin");
			silly::io::ostreamDataOutput fdout(fout);
			gcc->writeCircuit(&fdout);
			fout.close();
		}
		cout << "write input secrets" << endl;
		{
			ofstream fout("gsecrets.bin");
			silly::io::ostreamDataOutput fdout(fout);
			writeVector(&fdout, inputSecrets);
			fout.close();
		}

		bool read_back=true;
		if (read_back) {
			{
				ifstream fin("gcircuit.bin");
				silly::io::istreamDataInput fdin(fin);
				gcc->reset();
				gcc = GarbledCircuit_p();
				gcc = GarbledCircuit::readCircuit(&fdin);
			}
			{
			    inputSecrets.clear();
			    ifstream fin("gsecrets.bin");
			    silly::io::istreamDataInput fdin(fin);
			    readVector(&fdin, inputSecrets);
			}
		}

		cout << "evaluate garbled circuit" << endl;
		GCircuitEval geval;
		vector<SecretKey_p> gcirc_input(inputSecrets.size());
		for (uint i=0; i<gcirc_input.size(); ++i) {
			gcirc_input[i] = inputSecrets[i][i%2?1:0];
		}
		bit_vector circ_out = geval.eval(*gcc, gcirc_input);
		for (uint i=0; i<circ_out.size(); ++i) {
			cout << "output " << i << ": " << circ_out[i] << endl;
		}

		if (argc>1) {
			int min = 1;
			int max = strtol(argv[1], NULL, 10);
			if (argc>2) {
				min = max;
				max = strtol(argv[2], NULL, 10);
			}
			//pre_generate(cc.to_ptr(), getDefaultDir("/md5_pw_cmp").c_str(), min, max);


		}
	} catch (ParseException ex) {
		cerr << ex.what() << endl;
		throw;
	}

	return 0;
}


#include "sillymain.h"

MAIN("shdlparse")

