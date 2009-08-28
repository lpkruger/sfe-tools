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

class YaoSender {
	DataOutput *out;
	DataInput *in;

	void go(Circuit_p cc, FmtFile &fmt) {
		CircuitCrypt crypt;
		vector<boolean_secrets> inputSecrets;
		GarbledCircuit gcc = crypt.encrypt(*cc, inputSecrets);
		gcc.writeCircuit(out);
		//cout << "write input secrets" << endl;
		PinkasNaorOT ot;
		FmtFile::VarDesc vars = fmt.getVarDesc();
		FmtFile::VarDesc myvars = vars.filter("A");
		FmtFile::VarDesc yourvars = vars.filter("B");
		BigInt_Mtrx otvals(inputSecrets.size());
		int j=0;
		for (uint i=0; i<inputSecrets.size(); ++i) {
			if (yourvars.who.find(i) != yourvars.who.end()) {
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
		PinkasNaorOT ot;
		OTChooser chooser(inputs, &ot);
		chooser.setStreams(in, out);
		chooser.precalc();
		chooser.online();

	}
};


#endif /* YAOPROTOCOL_H_ */
