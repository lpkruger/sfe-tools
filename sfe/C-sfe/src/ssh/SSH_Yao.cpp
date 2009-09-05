/*
 * SSHMultiYao.cpp
 *
 *  Created on: Aug 30, 2009
 *      Author: louis
 */

#define DEBUG 1
#define DEBUG2 1
#include <algorithm>
#include "SSH_Yao.h"
#include "AuthStreams.h"

#include "sillydebug.h"

static bool use_permute = false;

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

#if DEBUG

void D_ON(const SecretKey_p &k, int lev=0) {
	D(k->getEncoded(), lev);
}
void D_ON(const SFEKey_p &k, int lev=0) {
	D(k->getEncoded(), lev);
}
void D(const boolean_secrets &secr, int lev=0) {
	D(secr.s0, lev);
	D(secr.s1, lev);
}
#endif

//struct VarParty {
//	string who;
//	VarParty(const string &w) : who(w) {}
//	bool operator()()
//};

static void test_eval(int n, GarbledCircuit &gcc, vector<boolean_secrets> &inputSecrets) {
	const char *test = "11110100001010011011100000101100011011000101011010000010100101011111100001001111011001000010001110111010101111011001101000000000";
	int testlen=strlen(test);
	GCircuitEval geval;
	vector<SecretKey_p> gcirc_input(inputSecrets.size());
	for (uint i=0; i<gcirc_input.size(); ++i) {
		gcirc_input[i] = inputSecrets[i][test[i%testlen]-'0'];
	}
	bit_vector circ_out = geval.eval(gcc, gcirc_input);
	DC("test eval " << n);
	D(circ_out);
	printf("test_eval %d %u\n", testlen, gcirc_input.size());
}

#define USE_THREADS 4

#if USE_THREADS
#include "../sillylib/sillythread.h"
using namespace silly::thread;
class CircuitCrypter : public Runnable {
	NOCOPY(CircuitCrypter)

public:
	int n;
	Circuit_p cc;
	GarbledCircuit_p *gcc;
	vector<boolean_secrets> *inpsecs;
	CircuitCrypter(int n0, Circuit_p c0, GarbledCircuit_p *g0, vector<boolean_secrets> *i0)
			: n(n0), cc(c0), gcc(g0), inpsecs(i0) {}
	void* run() {
		fprintf(stderr, "circuitcrypter %d starting\n", n);



#if 0 // just a test
		Circuit copy;
		copy = cc->deepCopy();
		if (use_permute) {
			CircuitCryptPermute crypt;
			*gcc = crypt.encrypt(copy, *inpsecs);
		} else {
			CircuitCrypt crypt;
			*gcc = crypt.encrypt(copy, *inpsecs);
		}
		test_eval(n, **gcc, *inpsecs);
		copy = cc->deepCopy();
		if (!use_permute) {
			CircuitCryptPermute crypt;
			*gcc = crypt.encrypt(*cc, *inpsecs);
		} else {
			CircuitCrypt crypt;
			*gcc = crypt.encrypt(*cc, *inpsecs);
		}
		test_eval(1000+n, **gcc, *inpsecs);
		copy = cc->deepCopy();
#else
#define copy cc
#endif

		if (use_permute) {
			CircuitCryptPermute crypt;
			*gcc = crypt.encrypt(*copy, *inpsecs);
		} else {
			CircuitCrypt crypt;
			*gcc = crypt.encrypt(*copy, *inpsecs);
		}
#undef copy
		return NULL;
	}
};
#endif

void SSHYaoSender::go(Circuit_p cc, FmtFile &fmt, const bit_vector &inputs) {
	FmtFile::VarDesc vars = fmt.getVarDesc();

	D("inputs:");
	D(inputs);

	int L = in->readInt();
	vector<vector<boolean_secrets> > inputSecrets(L);

	long time_start = currentTimeMillis();

	vector<GarbledCircuit_p> gcc(L);

#if USE_THREADS
	{
		ThreadPool pool(numCPUs()*2);
		CircuitCrypter *crypters[L];
		for (int n=0; n<L; ++n) {
			crypters[n] = new CircuitCrypter(n, cc->deepCopy(), &gcc[n], &inputSecrets[n]);
			pool.submit(crypters[n]);

		}
		pool.stopWait();
		//pool.waitIdle();
		for (int n=0; n<L; ++n) {
			delete crypters[n];
		}

	}

#else
	{
		if (use_permute) {
			CircuitCryptPermute crypt;
			for (int n=0; n<L; ++n) {
				fprintf(stderr, "cryptp circuit %d\n", n+1);
				gcc[n] = crypt.encrypt(*cc, inputSecrets[n]);
			}
		} else {
			CircuitCrypt crypt;
			for (int n=0; n<L; ++n) {
				fprintf(stderr, "crypt circuit %d\n", n+1);
				gcc[n] = crypt.encrypt(*cc, inputSecrets[n]);
		}
	}
#endif
	long time_crypt = currentTimeMillis();
	fprintf(stderr, "crypted %d circuits in %0.3f secs  (%0.3f s per circuit)\n",
			L, (time_crypt-time_start)/1.0e3, (time_crypt-time_start)/(1.0e3*L));

#if 0 && DEBUG
	for (int n=0; n<L; ++n) {
		DD(printf("\ncopy %d:\n", n));
		D(inputSecrets[n]);
	}
	D("");
#endif


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

//	D("mine");
//	D(all_myinpsecs);
//	D("yours");
//	D(all_yourinpsecs);

	BigInt_Cube all_otvals(L);

	for (int n=0; n<L; ++n) {
		all_otvals[n].resize(all_yourinpsecs[n].size());
		for (uint i=0; i<all_yourinpsecs[n].size(); ++i) {
			all_otvals[n][i] = all_yourinpsecs[n][i].toBigIntVector();
		}
	}

	BigInt_Mtrx otvals;
	vector<int> geom;
	//D(all_otvals);
	flatten(all_otvals, otvals, geom);

	//out->writeInt(otvals.size());
	PinkasNaorOT ot;

	long ot_start = currentTimeMillis();

	OTSender sender(otvals, &ot);
	sender.setStreams(in, out);
	fprintf(stderr, "OT precalc\n");
	sender.precalc();
	check_sync();
	long ot_calc = currentTimeMillis();
	fprintf(stderr, "OT online\n");
	sender.online();
	long ot_end = currentTimeMillis();

	fprintf(stderr, "OT in %0.3f secs (%0.3f + %0.3f)\n", (ot_end-ot_start)/1000.0,
				(ot_end-ot_calc)/1000.0, (ot_calc-ot_start)/1000.0);

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

	DF("chosen circuits:\n");
	D(all_my_chosen_secrets.getPerm());

	vector_perm<vector<boolean_secrets > > all_my_unchosen_secrets =
			all_my_chosen_secrets.negate_perm();


	DF("unchosen circuits:\n");
	D(all_my_unchosen_secrets.getPerm());

	for (uint i=0; i<all_my_unchosen_secrets.size(); ++i) {
		//DF("my unchosen secret %u:\n", i);
		//D(all_my_unchosen_secrets[i]);
	}

	vector<vector<SFEKey_p> > my_sent_secrets(all_my_unchosen_secrets.size());

	for (uint n=0; n<my_sent_secrets.size(); ++n) {
		my_sent_secrets[n].resize(inputs.size());
		for (uint i=0; i<inputs.size(); ++i) {
			my_sent_secrets[n][i] =
					all_my_unchosen_secrets[n].at(i)[inputs[i]];
		}
	}
	D("my sent secrets:");
	//D(my_sent_secrets);
	for (uint q=0; q<my_sent_secrets.size(); ++q) {
		for (uint qq=0; qq<my_sent_secrets[q].size(); ++qq) {
			//D(my_sent_secrets[q][qq]);
		}
	}
	writeVector(out, my_sent_secrets);
	fprintf(stderr, "done\n");
	check_sync();
}

#if USE_THREADS
class CircuitEvaluator : public Runnable {
	NOCOPY(CircuitEvaluator)
public:
	int n;
	GarbledCircuit_p gcc;
	vector<SecretKey_p> *inpsecs;
	vector<bool> circ_out;
	CircuitEvaluator(int n0, GarbledCircuit_p g0, vector<SecretKey_p> *i0)
			: n(n0), gcc(g0), inpsecs(i0) {}
	void* run() {
		GCircuitEval geval;
		fprintf(stderr, "circuitevaluator %d starting\n", n);
		circ_out = geval.eval(*gcc, *inpsecs);
		return NULL;
	}
};
#endif

bit_vector SSHYaoChooser::go(Circuit_p cc, FmtFile &fmt, const bit_vector &inputs) {
	D("inputs:");
	D(inputs);
	if (L<2)
		throw bad_argument("L must be at least 2");
	out->writeInt(L);
	DC("have " << inputs.size() << " inputs");
	FmtFile::VarDesc vars = fmt.getVarDesc();

	bit_vector allinputs;
	for (int i=0; i<L; ++i) {
		allinputs.insert(allinputs.end(), inputs.begin(), inputs.end());
	}

	D("allinputs:");
	D(allinputs);
	PinkasNaorOT ot;
	OTChooser chooser(allinputs, &ot);
	chooser.setStreams(in, out);
	long ot_start = currentTimeMillis();
	fprintf(stderr, "OT precalc\n");
	chooser.precalc();
	check_sync();
	long ot_calc = currentTimeMillis();
	fprintf(stderr, "OT online\n");
	BigInt_Vect all_myotsecs_flat =
			chooser.online();
	long ot_end = currentTimeMillis();
	fprintf(stderr, "OT in %0.3f secs (%0.3f + %0.3f)\n", (ot_end-ot_start)/1000.0,
			(ot_end-ot_calc)/1000.0, (ot_calc-ot_start)/1000.0);
	check_sync();

	vector<SFEKey_p> all_myinpsecs_flat(all_myotsecs_flat.size());
	for (uint i=0; i<all_myinpsecs_flat.size(); ++i) {
		all_myinpsecs_flat[i] = SFEKey_p(new SFEKey(
				new byte_buf(BigInt::fromPaddedBigInt(all_myotsecs_flat[i])), true));
	}
	vector<int> geom(L, inputs.size());
	vector<vector<SFEKey_p> > all_myinputsecs;

	DC("read " << all_myinpsecs_flat.size() << " from OT")
	unflatten(all_myinpsecs_flat, all_myinputsecs, geom);
	//D(myinputsecs);
	vector<GarbledCircuit_p> gcc(L);
	for (int i=0; i<L; ++i) {
		gcc[i] = GarbledCircuit::readCircuit(in);
	}

	check_sync();

	vector<int> choices;		// which ones to open
	// TODO: choose randomly
//	for (int i=2; i<L; ++i) {
//		choices.push_back(i);
//	}
	for (int i=0; i<L; i+=2) {
		choices.push_back(i);	// for testing choose even numbers
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

	GCircuitEval geval;
	vector_perm<GarbledCircuit_p> eval_gcc =
			verify_gcc.negate_perm();

	int LL = eval_gcc.size();
	vector<vector<SFEKey_p> > yourinputsecs;
	readVector(in, yourinputsecs);

	vector<int> eval_perm = eval_gcc.getPerm();
	vector_perm<vector<SFEKey_p> > myinputsecs(
			all_myinputsecs, eval_perm);

	vector<vector<SecretKey_p> > gcirc_input(LL);
	for (int n=0; n<LL; ++n) {
		gcirc_input[n].resize(cc->inputs.size());
		int ja=0;
		int jb=0;
		for (uint i=0; i<gcirc_input[n].size(); ++i) {
			if (vars.who.at(i) == "A") {
				gcirc_input[n][i] = yourinputsecs[n].at(ja++);
			} else if (vars.who.at(i) == "B") {
				gcirc_input[n][i] = myinputsecs[n].at(jb++);
			}
		}
	}

	bit_vector output0(LL);
	long time_start = currentTimeMillis();

#if USE_THREADS
	ThreadPool pool(numCPUs()*2);
	CircuitEvaluator *evaluators[LL];
	for (int n=0; n<LL; ++n) {
		//fprintf(stderr, "eval circuit %u\n", n);
		for (uint q=0; q<gcirc_input[n].size(); ++q) {
			//D(gcirc_input[n][q]);
		}
		evaluators[n] = new CircuitEvaluator(
				n, eval_gcc[n], &gcirc_input[n]);
		pool.submit(evaluators[n]);
		//evaluators[n]->run();
	}
	pool.stopWait();
	for (int n=0; n<LL; ++n) {

		printf("received %d outputs", evaluators[n]->circ_out.size());
		bool success = false;
		for (uint j=0; j<evaluators[n]->circ_out.size(); ++j) {
			if (evaluators[n]->circ_out[j])
				success = true;
		}
		printf(": %d\n", success);

		output0[n] = success;
		delete evaluators[n];
	}
#else
	for (int n=0; n<LL; ++n) {
		bit_vector circ_out = geval.eval(*eval_gcc[n], gcirc_input[n]);
		if (circ_out.size() != 1)
			throw ProtocolException("circuit should have 1 output");
		output0[n] = circ_out[0];
	}
#endif

	long time_eval = currentTimeMillis();
	fprintf(stderr, "evaled %d circuits in %0.3f secs  (%0.3f s per circuit)\n",
				L, (time_eval-time_start)/1.0e3, (time_eval-time_start)/(1.0e3*L));
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

	//ifstream fmtin("/home/louis/sfe/priveq.fmt");
	ifstream fmtin("/home/louis/sfe/md5_pw_cmp.fmt");

	FmtFile fmt = FmtFile::parseFmt(fmtin);
	cout << "Read fmt file" << endl;
	//ifstream in("/home/louis/sfe/priveq.circ");
	ifstream in("/home/louis/sfe/md5_pw_cmp.circ");
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
		int Lparam = 4;
		if (args.size() > 2)
			Lparam = strtol(args[2].c_str(), NULL, 10);
		SSHYaoChooser srv(Lparam);

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
