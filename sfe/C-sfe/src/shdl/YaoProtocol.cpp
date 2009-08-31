/*
 * YaoProtocol.cpp
 *
 *  Created on: Aug 26, 2009
 *      Author: louis
 */

#include "YaoProtocol.h"
#define DEBUG
#include "sillydebug.h"




void YaoSender::go(Circuit_p cc, FmtFile &fmt, const bit_vector &inputs) {
	CircuitCrypt crypt;
	vector<boolean_secrets> inputSecrets;
	GarbledCircuit_p gcc = crypt.encrypt(*cc, inputSecrets);
	gcc->writeCircuit(out);
	//cout << "write input secrets" << endl;
	PinkasNaorOT ot;
	FmtFile::VarDesc vars = fmt.getVarDesc();
//		FmtFile::VarDesc myvars = vars.filter("A");
//		FmtFile::VarDesc yourvars = vars.filter("B");

	vector<SFEKey_p> myinpsecs;
	int j=0;
	for (uint i=0; i<inputSecrets.size(); ++i) {
		//if (myvars.who.find(i) != myvars.who.end()) {
		if (vars.who.at(i) == "A") {
#if __INTEL_COMPILER
			myinpsecs.resize(myinpsecs.size()+1);
#else
			myinpsecs.push_back();
#endif
			myinpsecs[j] = inputSecrets[i][inputs[j]];
			++j;
		}
	}
	writeVector(out, myinpsecs);
	BigInt_Mtrx otvals;

	for (uint i=0; i<inputSecrets.size(); ++i) {
		if (vars.who.at(i) == "B") {
			otvals.push_back(inputSecrets[i].toBigIntVector());
		}
	}
	out->writeInt(otvals.size());

	OTSender sender(otvals, &ot);
	sender.setStreams(in, out);
	sender.precalc();
	sender.online();
}


bit_vector YaoChooser::go(Circuit_p cc, FmtFile &fmt, const bit_vector &inputs) {
	FmtFile::VarDesc vars = fmt.getVarDesc();

	GarbledCircuit_p gcc = GarbledCircuit::readCircuit(in);
	vector<SFEKey_p> yourinpsecs;
	readVector(in, yourinpsecs);
	uint ot_size = in->readInt();
	if (ot_size != inputs.size())
		throw new ProtocolException(string_printf(
				"ot_size %d != inputs.size %d", ot_size, inputs.size()).c_str());

	PinkasNaorOT ot;
	bit_vector inputs_copy(inputs);
	OTChooser chooser(inputs_copy, &ot);
	chooser.setStreams(in, out);
	chooser.precalc();
	BigInt_Vect myinpsecs = chooser.online();

	GCircuitEval geval;
	vector<SecretKey_p> gcirc_input(cc->inputs.size());

	int ja=0;
	int jb=0;
	for (uint i=0; i<gcirc_input.size(); ++i) {
		if (vars.who.at(i) == "A") {
			gcirc_input[i] = yourinpsecs.at(ja++);
		} else if (vars.who.at(i) == "B") {
			gcirc_input[i] = SFEKey_p(new SFEKey(
					BigInt::fromPaddedBigInt(myinpsecs.at(jb++))));
		}
	}
	bit_vector circ_out = geval.eval(*gcc, gcirc_input);
	return circ_out;
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
	//YaoProtocol yao;
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
		YaoSender cli;
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
		YaoChooser srv;

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
MAIN("yao")
#endif
