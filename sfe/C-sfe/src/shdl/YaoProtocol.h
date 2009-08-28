/*
 * YaoProtocol.h
 *
 *  Created on: Aug 26, 2009
 *      Author: louis
 */

#ifndef YAOPROTOCOL_H_
#define YAOPROTOCOL_H_

#include "CircuitCrypt.h"
#include "sillyio.h"
#include "../crypto/PinkasNaorOT.h"

class YaoProtocol {
public:
	YaoProtocol();
	virtual ~YaoProtocol();
};

static void writeObject(DataOutput *out, SecretKey_p &key) {
	byte_buf_p enc = key->getEncoded();
	out->writeByte(enc->size());
	out->write(*enc);
}
static void readObject(DataInput *in, SecretKey_p &key) {
	int len = in->readByte();
	byte_buf enc(len);
	in->readFully(enc);
	key = SFEKey_p(new SFEKey(enc));
}

class YaoSender {
	DataOutput *out;
	DataInput *in;

	void go(Circuit_p cc, FmtFile &fmt, vector<bool> inputs) {
		CircuitCrypt crypt;
		vector<boolean_secrets> inputSecrets;
		GarbledCircuit gcc = crypt.encrypt(*cc, inputSecrets);
		gcc.writeCircuit(out);
		//cout << "write input secrets" << endl;
		PinkasNaorOT ot;
		FmtFile::VarDesc vars = fmt.getVarDesc();
		FmtFile::VarDesc myvars = vars.filter("A");
		FmtFile::VarDesc yourvars = vars.filter("B");

		vector<SFEKey_p> myinpsecs;
		int j=0;
		for (uint i=0; i<inputSecrets.size(); ++i) {
			if (myvars.who.find(i) != myvars.who.end()) {
				myinpsecs.push_back();
				myinpsecs[j] = inputSecrets[i][inputs[j]];
			}
		}
		writeVector(out, myinpsecs);

		BigInt_Mtrx otvals;

		j=0;
		for (uint i=0; i<inputSecrets.size(); ++i) {
			if (yourvars.who.find(i) != yourvars.who.end()) {
				otvals.push_back();
				otvals[j].resize(2);
				otvals[j][0] = BigInt::toPaddedBigInt(*inputSecrets[i].s0->getEncoded());
				otvals[j][1] = BigInt::toPaddedBigInt(*inputSecrets[i].s1->getEncoded());
				++j;
			}
		}
		OTSender sender(otvals, &ot);
		sender.setStreams(in, out);
		sender.precalc();
		sender.online();
	}
};
class YaoChooser {
	DataOutput *out;
	DataInput *in;
	vector<bool> inputs;

	void go(Circuit_p cc) {
		GarbledCircuit gcc = GarbledCircuit::readCircuit(in);
		vector<SFEKey_p> yourinpsecs;
		readVector(in, yourinpsecs);
		PinkasNaorOT ot;
		OTChooser chooser(inputs, &ot);
		chooser.setStreams(in, out);
		chooser.precalc();
		chooser.online();

	}
};


#endif /* YAOPROTOCOL_H_ */
