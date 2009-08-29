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
#include "GCircuitEval.h"

class YaoProtocol {
public:
};


class YaoSender {
	DataOutput *out;
	DataInput *in;
public:
	void setStreams(DataInput *in0, DataOutput *out0) {
		in = in0;
		out = out0;
	}
	void go(Circuit_p cc, FmtFile &fmt, const vector<bool> &inputs);
};

class YaoChooser {
	DataOutput *out;
	DataInput *in;
	vector<bool> inputs;
public:
	void setStreams(DataInput *in0, DataOutput *out0) {
		in = in0;
		out = out0;
	}
	vector<bool> go(Circuit_p cc, FmtFile &fmt, const vector<bool> &inputs);
};


#endif /* YAOPROTOCOL_H_ */
