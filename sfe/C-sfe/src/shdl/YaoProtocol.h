/*
 * YaoProtocol.h
 *
 *  Created on: Aug 26, 2009
 *      Author: louis
 */

#ifndef YAOPROTOCOL_H_
#define YAOPROTOCOL_H_

#include "CircuitCryptPermute.h"
#include "sillyio.h"
#include "../crypto/PinkasNaorOT.h"
#include "GCircuitEval.h"


class YaoSender {
	DataOutput *out;
	DataInput *in;
public:
	void setStreams(DataInput *in0, DataOutput *out0) {
		in = in0;
		out = out0;
	}
	void go(Circuit_p cc, FmtFile &fmt, const bit_vector &inputs);
};

class YaoChooser {
	DataOutput *out;
	DataInput *in;
	//bit_vector inputs;
public:
	void setStreams(DataInput *in0, DataOutput *out0) {
		in = in0;
		out = out0;
	}
	bit_vector go(Circuit_p cc, FmtFile &fmt, const bit_vector &inputs);
};


#endif /* YAOPROTOCOL_H_ */
