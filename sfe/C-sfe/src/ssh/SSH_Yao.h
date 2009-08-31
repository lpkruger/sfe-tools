/*
 * SSHMultiYao.h
 *
 *  Created on: Aug 30, 2009
 *      Author: louis
 */

#ifndef SSHMULTIYAO_H_
#define SSHMULTIYAO_H_

#include <vector>
#include "sillytype.h"
#include "../shdl/YaoProtocol.h"
#include "../shdl/CircuitCrypt.h"

using std::vector;

const static int L = 8;
const static int sync_const = 0x541C4A0;

class SSHYao {
protected:
	DataOutput *out;
	DataInput *in;
public:
	void setStreams(DataInput *in0, DataOutput *out0) {
		in = in0;
		out = out0;
	}
	void check_sync() {
		out->writeInt(sync_const);
		int magic = in->readInt();
		if (magic != sync_const)
			throw ProtocolException("protocol synchronization failure");
	}
};

class SSHYaoChooser : public SSHYao {
public:
	bit_vector go(Circuit_p cc, FmtFile &fmt, const bit_vector &inputs);
};

class SSHYaoSender : public SSHYao {
public:
	void go(Circuit_p cc, FmtFile &fmt, const bit_vector &inputs);
};

#endif /* SSHMULTIYAO_H_ */
