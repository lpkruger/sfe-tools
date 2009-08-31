/*
 * SSHMultiYao.cpp
 *
 *  Created on: Aug 30, 2009
 *      Author: louis
 */

#ifndef __INTEL_COMPILER // doesn't work yet

#include <algorithm>
#include "SSH_Yao.h"
#define DEBUG 1
#include "sillydebug.h"

template<class T> inline void
flatten(vector<vector<T> > &in, vector<T> &out, vector<int> &geom) {
	out.clear();
	geom.clear();
	geom.resize(in.size());
	for (uint i=0; i<in.size(); ++i) {
		geom[i] = in[i].size();
		out.insert(out.end(), in[i].begin(), in[i].end());
	}
}
template<class T> inline void
unflatten(const vector<T> &in, vector<vector<T> > &out, const vector<int> &geom) {
	out.clear();
	uint index = 0;
	out.resize(geom.size());
	for (uint i=0; i<geom.size(); ++i) {
		out[i].resize(geom[i]);
		for (int j=0; j<geom[i]; ++j) {
			out[i][j]=in.at(index++);
		}
	}
	if (index != in.size())
		throw bad_argument("index != in.size()");
}

// access a vector through a permutation
template<class T> class vector_perm {
	vector<T> &the;
	vector<int> perm;
public:
	vector_perm(vector<T> &v, vector<int> &p) :
		the(v), perm(p) {}

	T& at(int i) {
		return the.at(perm.at(i));
	}
	T& operator[] (int i) {
		return the.at(perm.at(i));
	}
	uint size() {
		return perm.size();
	}
	vector<int> getPerm() {
		return perm;
	}
	class iterator {
		vector_perm &v;
		int pos;
	public:
		iterator(vector_perm &v0, int p0) : v(v0), pos(p0) {}
		T& operator*() {
			return v[pos];
		}
		T* operator->() {
			return &v[pos];
		}
		int operator-(const iterator &o) const {
			return pos - o.pos;
		}
		bool operator==(const iterator &o) const {
			return pos==o.pos && &v == &o.v;
		}
		bool operator!=(const iterator &o) const {
			return ! operator==(o);
		}
		iterator& operator++() {
			++pos;
			return *this;
		}
		iterator operator++(int) {
			iterator ret(v,pos);
			++pos;
			return ret;
		}
	};

	iterator begin() {
		return iterator(*this, 0);
	}
	iterator end() {
		return iterator(*this, the.size());
	}
	vector_perm inverse_perm() {
		vector<int> invperm(the.size(), -1);
		for (uint i=0; i<perm.size(); ++i) {
			invperm.at(perm[i]) = i;
		}
		return vector_perm(the, invperm);
	}
	vector_perm negate_perm() {
		vector<bool> mark(the.size());
		for (uint i=0; i<perm.size(); ++i) {
			mark.at(perm[i]) = true;
		}
		vector<int> negperm;
		for (uint i=0; i<mark.size(); ++i) {
			if (!mark[i] )
				negperm.push_back(i);
		}
		return vector_perm(the, negperm);
	}
};

template < class InputIterator, class OutputIterator,
		class Predicate, class T >
inline OutputIterator copy_if ( InputIterator first, InputIterator last,
		OutputIterator result, Predicate pred) {
	for (; first != last; ++first)
		if (pred(*first)) {
			*result = *first;
			++result;
		}
	return result;
}

void flatten_test() {
	//vector<vector<int> > two_d;
	tensor<int, 2>::type two_d;
	//vector<int> one_d;
	tensor<int, 1>::type one_d;
	vector<int> geom;
	flatten(two_d, one_d, geom);
	unflatten(one_d, two_d, geom);
}

using std_obj_rw::readObject;
using std_obj_rw::writeObject;

//struct VarParty {
//	string who;
//	VarParty(const string &w) : who(w) {}
//	bool operator()()
//};
void SSHYaoSender::go(Circuit_p cc, FmtFile &fmt, const bit_vector &inputs) {
	FmtFile::VarDesc vars = fmt.getVarDesc();

	CircuitCrypt crypt;
	vector<vector<boolean_secrets> > inputSecrets(L);

	vector<GarbledCircuit_p> gcc(L);
	for (int n=0; n<L; ++n) {
		gcc[n] = crypt.encrypt(*cc, inputSecrets[n]);
	}

	vector<vector<boolean_secrets> > all_myinpsecs(L);
	vector<vector<boolean_secrets> > all_yourinpsecs(L);
	for (int n=0; n<L; ++n) {
		for (uint i=0; i<inputSecrets[n].size(); ++i) {
			//if (myvars.who.find(i) != myvars.who.end()) {
			if (vars.who.at(i) == "A") {
				all_myinpsecs[n].push_back(inputSecrets[n][i]);
			}
			if (vars.who.at(i) == "B") {
				all_yourinpsecs[n].push_back(inputSecrets[n][i]);
			}
		}
	}

	BigInt_Cube all_otvals(L);

	for (int n=0; n<L; ++n) {
		all_otvals[n].resize(all_yourinpsecs[n].size());
		for (uint i=0; i<all_yourinpsecs[n].size(); ++i) {
			all_otvals[n][i] = inputSecrets[n][i].toBigIntVector();
		}
	}

	BigInt_Mtrx otvals;
	vector<int> geom;

	flatten(all_otvals, otvals, geom);

	//out->writeInt(otvals.size());
	PinkasNaorOT ot;

	OTSender sender(otvals, &ot);
	sender.setStreams(in, out);
	sender.precalc();
	sender.online();

	check_sync();

	for (int n=0; n<L; ++n) {
		gcc[n]->writeCircuit(out);
    }

	check_sync();

	vector<int> choices;
	readVector(in, choices);

	vector_perm<vector<boolean_secrets > >
			all_your_chosen_secrets(all_yourinpsecs, choices);

	vector_perm<vector<boolean_secrets > >
			all_my_chosen_secrets(all_myinpsecs, choices);

	for (uint i=0; i<choices.size(); ++i) {
		writeVector(out, all_your_chosen_secrets[i]);
		writeVector(out, all_my_chosen_secrets[i]);
	}

	check_sync();

	vector_perm<vector<boolean_secrets > > all_my_unchosen_secrets =
			all_my_chosen_secrets.negate_perm();

	vector<vector<SFEKey_p> > my_sent_secrets(all_my_unchosen_secrets.size());

	for (uint n=0; n<my_sent_secrets.size(); ++n) {
		my_sent_secrets[n].resize(inputs.size());
		for (uint i=0; i<inputs.size(); ++i) {
			my_sent_secrets[n][i] =
					all_my_unchosen_secrets[n].at(i)[inputs[i]];
		}
	}
	writeVector(out, my_sent_secrets);

	check_sync();
}

bit_vector SSHYaoChooser::go(Circuit_p cc, FmtFile &fmt, const bit_vector &inputs) {
	DC("have " << inputs.size() << " inputs");
	FmtFile::VarDesc vars = fmt.getVarDesc();

	bit_vector allinputs;
	for (int i=0; i<L; ++i) {
		allinputs.insert(allinputs.end(), inputs.begin(), inputs.end());
	}

	PinkasNaorOT ot;
	OTChooser chooser(allinputs, &ot);
	chooser.setStreams(in, out);
	chooser.precalc();
	BigInt_Vect myinpsecs_flat =
			chooser.online();

	check_sync();

	vector<int> geom(L, inputs.size());
	BigInt_Mtrx myinputsecs;

	DC("read " << myinpsecs_flat.size() << " from OT")
	unflatten(myinpsecs_flat, myinputsecs, geom);

	vector<GarbledCircuit_p> gcc(L);
	for (int i=0; i<L; ++i) {
		gcc[i] = GarbledCircuit::readCircuit(in);
	}

	check_sync();

	vector<int> choices;
	for (int i=0; i<L; i+=2) {
		choices.push_back(i);	// for testing choose even numbers
		// TODO: choose randomly
	}
	writeVector(out, choices);

	vector<vector<boolean_secrets > > all_my_chosen_secrets(choices.size());
	vector<vector<boolean_secrets > > all_your_chosen_secrets(choices.size());
	for (uint i=0; i<choices.size(); ++i) {
		readVector(in, all_my_chosen_secrets[i]);
		readVector(in, all_your_chosen_secrets[i]);
	}

	check_sync();

	vector_perm<GarbledCircuit_p> verify_gcc(gcc, choices);
	// TODO: verify

	vector<vector<SFEKey_p> > yourinputsecs;
	readVector(in, yourinputsecs);

	GCircuitEval geval;
	vector_perm<GarbledCircuit_p> eval_gcc =
			verify_gcc.negate_perm();

	vector<SecretKey_p> gcirc_input(cc->inputs.size());
	bit_vector circ_out;
	bit_vector output0(eval_gcc.size());
	for (uint n=0; n<eval_gcc.size(); ++n) {
		int ja=0;
		int jb=0;
		for (uint i=0; i<gcirc_input.size(); ++i) {
			if (vars.who.at(i) == "A") {
				gcirc_input[i] = yourinputsecs[n].at(ja++);
			} else if (vars.who.at(i) == "B") {
				gcirc_input[i] = new SFEKey(
						BigInt::fromPaddedBigInt(myinputsecs[n].at(jb++)));
			}
		}
		circ_out = geval.eval(*gcc[n], gcirc_input);
		if (circ_out.size() != 1)
			throw ProtocolException("circuit should have 1 output");
		output0[n] = circ_out[0];
	}

	check_sync();
	return output0;
}







#if 1		// test code
#include "sillysocket.h"
#include <iostream>
#include <fstream>
using namespace silly::net;

static bit_vector toBits(BigInt n, int size) {
	bit_vector b;
	int bits = n.bitLength();
	DC("toBits expect " << size << " nbits " << bits);
	if (size<bits)
		throw bad_argument("toBits: size too small");
	b.resize(size);
	for (int i=0; i<bits; ++i) {
		b[i] = n.testBit(i);
	}

	return b;
}
static int _main(int argc, char **argv) {
	vector<string> args(argc-1);
	for (int i=1; i<argc; ++i) {
		args[i-1] = argv[i];
	}

	args.at(0);
	//SSHYaoProtocol yao;
	Socket *s;

	ifstream fmtin("/home/louis/sfe/priveq.fmt");
	FmtFile fmt = FmtFile::parseFmt(fmtin);
	cout << "Read fmt file" << endl;
	ifstream in("/home/louis/sfe/priveq.circ");
	Circuit_p cc = Circuit::parseCirc(in);

	if (args[0] == ("A")) {
		args.at(1);
		BigInt n = BigInt::parseString(args[1]);
		bit_vector input_bits = toBits(n, fmt.numInputs(0));

		cout << "connecting" << endl;
		s = new Socket("localhost", 5437);
		DataOutput *out = s->getOutput();
		DataInput *in = s->getInput();
		SSHYaoSender cli;
		cli.setStreams(in, out);
		cli.go(cc, fmt, input_bits);
		delete out;
		delete in;
		delete s;
	} else if (args[0] == ("B")) {
		args.at(1);
		BigInt n = BigInt::parseString(args[1]);
		bit_vector input_bits = toBits(n, fmt.numInputs(1));

		args.at(1);
		SSHYaoChooser srv;

		cout << "listening" << endl;
		ServerSocket *ss = new ServerSocket(5437);
		s = ss->accept();
		DataOutput *out = s->getOutput();
		DataInput *in = s->getInput();
		srv.setStreams(in, out);
		bit_vector circ_out = srv.go(cc, fmt, input_bits);
		for (uint i=0; i<circ_out.size(); ++i) {
			cout << "output " << i << ": " << circ_out[i] << endl;
		}
		delete s;
		delete ss;
		delete out;
		delete in;
	}
	return 0;
}


#include "sillymain.h"
MAIN("sshyao")
#endif

#endif // __INTEL_COMPILER
