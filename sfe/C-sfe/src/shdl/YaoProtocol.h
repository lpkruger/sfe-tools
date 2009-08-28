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

	void go(Circuit_p cc) {
		CircuitCrypt crypt;
		vector<boolean_secrets> inputSecrets;
		GarbledCircuit gcc = crypt.encrypt(*cc, inputSecrets);
		gcc.writeCircuit(out);
		cout << "write input secrets" << endl;
		PinkasNaorOT ot;
		BigInt_Mtrx otvals(inputSecrets.size());
		for (uint i=0; i<inputSecrets.size(); ++i) {
			otvals[i].resize(2);
			otvals[i][0] = BigInt::toPaddedBigInt(*inputSecrets[i].s0->getEncoded());
			otvals[i][1] = BigInt::toPaddedBigInt(*inputSecrets[i].s1->getEncoded());
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

	void go(Circuit_p cc) {
		GarbledCircuit gcc = GarbledCircuit::readCircuit(in);


	}
};


#endif /* YAOPROTOCOL_H_ */
