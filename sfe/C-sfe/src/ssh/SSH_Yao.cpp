/*
 * SSHMultiYao.cpp
 *
 *  Created on: Aug 30, 2009
 *      Author: louis
 */

//#define DEBUG 1
//#define DEBUG2 1
#include <algorithm>
#include "SSH_Yao.h"

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
void D_ON(const boolean_secrets &secr, int lev=0) {
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
	DF("test_eval %d %u\n", testlen, gcirc_input.size());
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
	Random *rand;
	CircuitCrypter(int n0, Circuit_p c0, GarbledCircuit_p *g0, vector<boolean_secrets> *i0
			, Random *r0) : n(n0), cc(c0), gcc(g0), inpsecs(i0), rand(r0) {}
	void* run() {
#if NDEBUG
		fprintf(stderr, "#");
#else
		DF(stderr, "circuitcrypter %d starting\n", n);
#endif


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
			CircuitCryptPermute crypt(rand);
			*gcc = crypt.encrypt(*copy, *inpsecs);
		} else {
			CircuitCrypt crypt(rand);
			*gcc = crypt.encrypt(*copy, *inpsecs);
		}
#undef copy
		return NULL;
	}
};
#endif

struct CircuitCryptChecker : public Runnable {
	int n;
	Circuit_p cc;
	const byte_buf &check_hash;
	const FmtFile::VarDesc &vars;
	const byte_buf &prng_key;
	const vector<boolean_secrets> &my_secrets;
	const vector<boolean_secrets> &your_secrets;

	string failure;
	CircuitCryptChecker(int n0, Circuit_p copy0, const byte_buf &hash0, const byte_buf &key, const FmtFile::VarDesc &var0,
			const vector<boolean_secrets> &my, const vector<boolean_secrets> &your) :
				n(n0), check_hash(hash0), vars(var0), prng_key(key), my_secrets(my), your_secrets(your) {
		cc = copy0;
	}

	void* run() {
		GarbledCircuit_p verify_gcc;
		// checking all chosen circuits
		vector<boolean_secrets> test_inpsecs;
		DF("checking circuit %d with seed %s\n", n, toHexString(prng_key).c_str());
		fprintf(stderr, "v");
		vector<boolean_secrets> check_inpsecs(cc->inputs.size());
		int ja=0;
		int jb=0;
		for (uint i=0; i<check_inpsecs.size(); ++i) {
			if (vars.who.at(i) == "A") {
				check_inpsecs[i] = your_secrets.at(ja++);
			} else if (vars.who.at(i) == "B") {
				check_inpsecs[i] = my_secrets.at(jb++);
			}
		}

		CircuitCryptPermute crypt(new PseudoRandom(prng_key));
		GarbledCircuit_p test_gcc = crypt.encrypt(*cc, test_inpsecs);

		//printf("checking secrets...\n");

		if (check_inpsecs != test_inpsecs) {
			failure = string_printf("Input secrets don't match in circuit %d", n);
			return NULL;
		}

		DF("checking garbled circ...\n");
		if (test_gcc->hashCircuit() != check_hash) {
			failure = string_printf("Garbled circuit hash mismatch in circuit %d", n);
		}
//		BytesDataOutput out1;
//		test_gcc->writeCircuit(&out1);
		//DF("circ%db %s\n",   choices[n], toHexString(out1.buf).c_str());

//		BytesDataOutput out2;
//		check_gcc->writeCircuit(&out2);
//		//DF("circ%da %s\n\n", choices[n], toHexString(out2.buf).c_str());
//		if (out1.buf.size() != out2.buf.size() ||
//				!std::equal(out1.buf.begin(), out1.buf.end(), out2.buf.begin())) {
//			failure = string_printf("Garbled circuits don't match in circuit %d", n);
//		}
		return NULL;

	}
};

#define bench_printf(...) do {				\
	string tmpstr;							\
	tmpstr = string_printf(__VA_ARGS__);	\
	benchmark_buffer.append(tmpstr);		\
	DF("\n%s", tmpstr.c_str());			\
} while(0)

#ifdef BUILDNAME
#define STRINGIFY(X) #X
#define GET_STRING(X) STRINGIFY(X)
#define BENCHNAME(n) "SSH " n " Benchmarks for build " GET_STRING(BUILDNAME) "\n"
#else
#define BENCHNAME ""
#endif

void SSHYaoSender::go(Circuit_p cc, FmtFile &fmt, const bit_vector &inputs) {
	string benchmark_buffer(BENCHNAME("Client"));
	FmtFile::VarDesc vars = fmt.getVarDesc();

	D("inputs:");
	D(inputs);

	int L = in->readInt();
	vector<vector<boolean_secrets> > inputSecrets(L);

	long time_start = currentTimeMillis();

	vector<GarbledCircuit_p> gcc(L);

	vector<byte_buf> prng_keys;

#if USE_THREADS
	{
		ThreadPool pool(numCPUs()*2);
		CircuitCrypter *crypters[L];
		for (int n=0; n<L; ++n) {
			Random *rn;
			if (use_prng) {
				prng_keys.resize(L);
				prng_keys[n].resize(128/8);
				srandom.getBytes(prng_keys[n]);
				rn = new PseudoRandom(prng_keys[n]);
				//printf("seed #%d : %s\n", n, toHexString(prng_keys[n]).c_str());
			} else {
				rn = new SecureRandom();
			}

			crypters[n] =
					new CircuitCrypter(n, cc->deepCopy(), &gcc[n], &inputSecrets[n], rn);
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
				DF"cryptp circuit %d\n", n+1);
				gcc[n] = crypt.encrypt(*cc, inputSecrets[n]);
			}
		} else {
			CircuitCrypt crypt;
			for (int n=0; n<L; ++n) {
				DF"crypt circuit %d\n", n+1);
				gcc[n] = crypt.encrypt(*cc, inputSecrets[n]);
		}
	}
#endif
	long time_crypt = currentTimeMillis();
	bench_printf("crypted %d circuits in %0.3f secs  (%0.3f s per circuit)\n",
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
	DF("OT precalc\n");
	sender.precalc();
	long ot_precalc_end = currentTimeMillis();
	check_sync();
	long ot_online_start = currentTimeMillis();
	DF("OT online\n");
	sender.online();
	long ot_end = currentTimeMillis();
	check_sync();
	long ot_finished = currentTimeMillis();
	bench_printf("OT in %0.3f secs (%0.3f + %0.3f + %0.3f + %0.3f)\n", (ot_finished-ot_start)/1000.0,
				(ot_precalc_end-ot_start)/1000.0, (ot_online_start-ot_precalc_end)/1000.0,
				(ot_end-ot_online_start)/1000.0, (ot_finished-ot_end)/1000.0);

	long time_before_circ = currentTimeMillis();
	long bytes_before_circ = out->total;
	bench_printf("written before circuits: %lu\n", out->total);

	for (int n=0; n<L; ++n) {
		if (use_prng) {
			byte_buf hash = gcc[n]->hashCircuit();
			writeObject(out, hash);
		} else {
			gcc[n]->writeCircuit(out);
			fast_sync();
		}
		//fprintf(stderr, "written: %d\n", out->total);
    }
	check_sync();
	long time_after_circ = currentTimeMillis();
	bench_printf("circuit writing: %0.3f MB, %0.3f sec, speed: %0.3f MB/s\n",
			(out->total - bytes_before_circ)/1000000.0,
			(time_after_circ-time_before_circ)/1000.0,
			(out->total - bytes_before_circ)*0.001/(time_after_circ-time_before_circ));


	vector<int> choices;
	readVector(in, choices);

	vector_perm<vector<boolean_secrets > >
			all_your_chosen_secrets(all_yourinpsecs, choices);

	vector_perm<vector<boolean_secrets > >
			all_my_chosen_secrets(all_myinpsecs, choices);

	for (uint i=0; i<choices.size(); ++i) {
		if (use_prng)
			writeVector(out, prng_keys.at(choices[i]));
		writeVector(out, all_your_chosen_secrets[i]);
		writeVector(out, all_my_chosen_secrets[i]);
	}

	if (use_prng) {
		check_sync();
		vector_perm<GarbledCircuit_p> chosen_gcc(gcc, choices);
		vector_perm<GarbledCircuit_p> eval_gcc(chosen_gcc.negate_perm());
		for (uint n=0; n<eval_gcc.size(); ++n) {
			eval_gcc[n]->writeCircuit(out);
			fast_sync();
		}
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
	DF(stderr, "done\n");
	long time_done = currentTimeMillis();
	bench_printf("communication time %0.3f\n", (time_done-ot_finished)/1000.0);
	bench_printf("client done in %0.3f seconds\n", (time_done-time_start)/1000.0);
	//check_sync();
	//long time_end = currentTimeMillis();
	//bench_printf("all done in %03f seconds\n", (time_end-time_start)/1000.0);
	bench_printf("online time %0.3f seconds\n", (time_done-ot_online_start)/1000.0);
	bench_printf("total bytes written: %lu\n", out->total);
	cerr << endl << "----------------------------" << endl;
	cerr << benchmark_buffer;
	cerr << "----------------------------" << endl << endl;

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
#if NDEBUG
		fprintf(stderr, "$");
#else
		DF(stderr, "circuitevaluator %d starting\n", n);
#endif
		circ_out = geval.eval(*gcc, *inpsecs);
		return NULL;
	}
};
#endif

bit_vector SSHYaoChooser::go(Circuit_p cc, FmtFile &fmt, const bit_vector &inputs) {
	string benchmark_buffer(BENCHNAME("Server"));
	long time_start = currentTimeMillis();
	D("inputs:");
	D(inputs);
	if (L<2)
		throw bad_argument("L must be at least 2");
	out->writeInt(L);
	DC("have " << inputs.size() << " inputs");
	FmtFile::VarDesc vars = fmt.getVarDesc();

	bit_vector allinputs;
	for (int n=0; n<L; ++n) {
		allinputs.insert(allinputs.end(), inputs.begin(), inputs.end());
	}

	D("allinputs:");
	D(allinputs);
	PinkasNaorOT ot;
	OTChooser chooser(allinputs, &ot);
	chooser.setStreams(in, out);
	long ot_start = currentTimeMillis();
	DF(stderr, "OT precalc\n");
	chooser.precalc();
	long ot_precalc_end = currentTimeMillis();
	check_sync();
	long ot_online_start = currentTimeMillis();
	DF(stderr, "OT online\n");
	BigInt_Vect all_myotsecs_flat =
			chooser.online();
	long ot_end = currentTimeMillis();
	bench_printf("OT in %0.3f secs (%0.3f + %0.3f + %0.3f)\n", (ot_end-ot_start)/1000.0,
					(ot_precalc_end-ot_start)/1000.0, (ot_online_start-ot_precalc_end)/1000.0,
					(ot_end-ot_online_start)/1000.0);
	check_sync();

	vector<SFEKey_p> all_myinpsecs_flat(all_myotsecs_flat.size());
	for (uint i=0; i<all_myinpsecs_flat.size(); ++i) {
		all_myinpsecs_flat[i] = SFEKey_p(new SFEKey(
				new byte_buf(BigInt::fromPaddedBigInt(all_myotsecs_flat[i])), true));
	}
	vector<int> geom(L, inputs.size());
	vector<vector<SFEKey_p> > all_myinputsecs;

	DC("read " << all_myinpsecs_flat.size() << " from OT");
	unflatten(all_myinpsecs_flat, all_myinputsecs, geom);
	//D(myinputsecs);
	vector<GarbledCircuit_p> gcc(L);
	vector<byte_buf> gcc_hashes(use_prng ? L : 0);
	for (int n=0; n<L; ++n) {
		if (use_prng) {
			readObject(in, gcc_hashes[n]);
		} else {
			gcc[n] = GarbledCircuit::readCircuit(in);
			fast_sync();
		}
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

	vector<byte_buf> chosen_prng_keys(use_prng ? choices.size() : 0);
	vector<vector<boolean_secrets > > all_my_chosen_secrets(choices.size());
	vector<vector<boolean_secrets > > all_your_chosen_secrets(choices.size());

	//vector<vector<boolean_secrets> > all_check_input_secs(choices.size());

	ThreadPool checkpool(numCPUs());
	CircuitCryptChecker *checkers[choices.size()];

	for (uint n=0; n<choices.size(); ++n) {
		if (use_prng)
			readVector(in, chosen_prng_keys[n]);
		readVector(in, all_my_chosen_secrets[n]);
		readVector(in, all_your_chosen_secrets[n]);

		if (use_prng) {
			checkers[n] = new CircuitCryptChecker(choices[n], cc->deepCopy(), gcc_hashes.at(choices[n]),
					chosen_prng_keys[n], vars,
					all_my_chosen_secrets[n], all_your_chosen_secrets[n]);

			checkpool.submit(checkers[n]);
		}
	}
	vector_perm<GarbledCircuit_p> verify_gcc(gcc, choices);
	vector_perm<GarbledCircuit_p> eval_gcc =
			verify_gcc.negate_perm();

	if (use_prng) {
		check_sync();
		vector_perm<GarbledCircuit_p> chosen_gcc(gcc, choices);
		vector_perm<GarbledCircuit_p> eval_gcc(chosen_gcc.negate_perm());
		for (uint n=0; n<eval_gcc.size(); ++n) {
			eval_gcc[n] = GarbledCircuit::readCircuit(in);
			fast_sync();
		}

		string failure;
		checkpool.stopWait();
		for (uint n=0; n<choices.size(); ++n) {
			if (failure.empty() && !checkers[n]->failure.empty())
				failure = checkers[n]->failure;
			delete checkers[n];
		}
		if (!failure.empty()) {
			throw ProtocolException(failure.c_str());
		}
	}

	check_sync();



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

	long time_eval_start = currentTimeMillis();
	bench_printf("communication time %0.3f\n", (time_eval_start-ot_end)/1000.0);

	uint expected_outputs = fmt.mapping.at("output.bob").bits.size();
	bit_vector output0;
#if USE_THREADS
	ThreadPool pool(numCPUs()*2);
	CircuitEvaluator *evaluators[LL];
	for (int n=0; n<LL; ++n) {
		//fprintf(stderr, "eval circuit %u\n", n);
//		for (uint q=0; q<gcirc_input[n].size(); ++q) {
//			D(gcirc_input[n][q]);
//		}
		evaluators[n] = new CircuitEvaluator(
				n, eval_gcc[n], &gcirc_input[n]);
		pool.submit(evaluators[n]);
		//evaluators[n]->run();
	}
	pool.stopWait();
	for (int n=0; n<LL; ++n) {
		if (evaluators[n]->circ_out.size() != expected_outputs)
			throw ProtocolException(
					string_printf("circuit should have %d output",
							expected_outputs).c_str());
		DF("received %d outputs", evaluators[n]->circ_out.size());
		if (n==0) {
			output0 = evaluators[n]->circ_out;
		} else {
			if (evaluators[n]->circ_out != output0)
				throw ProtocolException(
						string_printf(
								"detected differing outputs in circuit %d", n).c_str());
		}
		bool success = false;
		for (uint j=0; j<evaluators[n]->circ_out.size(); ++j) {
			if (evaluators[n]->circ_out[j])
				success = true;
		}
		DF(": %d\n", success);
		if (!success)
			throw ProtocolException(
					string_printf("failed circuit %d", n).c_str());

		delete evaluators[n];
	}

#else
	// TODO: this is all wrong
	GCircuitEval geval;
	for (int n=0; n<LL; ++n) {
		bit_vector circ_out = geval.eval(*eval_gcc[n], gcirc_input[n]);

		if (circ_out.size() != expected_outputs)
			throw ProtocolException(
					string_printf("circuit should have %d output",
							expected_outputs).c_str());
		bool succ = false;
		for (int i=0; i<circ_out[i]; ++i) {
			if (circ_out[i])
				succ = true;
		}
	}
#endif

	long time_eval_end = currentTimeMillis();
	bench_printf("evaled %d circuits in %0.3f secs  (%0.3f s per circuit)\n",
				LL, (time_eval_end-time_eval_start)/1.0e3,
				    (time_eval_end-time_eval_start)/(1.0e3*LL));
	bench_printf("server done in %0.3f seconds\n", (time_eval_end-time_start)/1000.0);
	//check_sync();
	//long time_end = currentTimeMillis();
	bench_printf("online time %0.3f seconds\n", (time_eval_end-ot_online_start)/1000.0);

	cerr << endl << "----------------------------" << endl;
	cerr << benchmark_buffer;
	cerr << "----------------------------" << endl << endl;

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
