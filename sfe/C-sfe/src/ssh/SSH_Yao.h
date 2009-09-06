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

const static int sync_const = 0x541C4A0;
const static bool use_permute = true;
const static bool use_prng = true;

using crypto::Random;
using crypto::SecureRandom;
using crypto::cipher::PseudoRandom;

class SSHYao {
protected:
	DataOutput *out;
	DataInput *in;
	bool no_fast_sync;
	bool no_check_sync;
	SecureRandom srandom;
public:
	SSHYao() {
		char *opt;
		opt = getenv("NOFASTSYNC");
		no_fast_sync = (opt!=NULL && strcmp(opt, "0"));
		opt = getenv("NOCHECKSYNC");
		no_check_sync = (opt!=NULL && strcmp(opt, "0"));

	}
	void setStreams(DataInput *in0, DataOutput *out0) {
		in = in0;
		out = out0;
	}

	void fast_sync() {
		if (no_fast_sync)
			return;
		out->writeInt(sync_const + 0x1234567);
		in->skip(4);
		out->flush();
	}

	void check_sync() {
		if (no_check_sync && no_fast_sync)
			return;
		out->writeInt(sync_const);
		if (no_check_sync) {
			in->skip(4);
		} else {
			int magic = in->readInt();
			if (magic != sync_const)
				throw ProtocolException("protocol synchronization failure");
		}
	}

};

class SSHYaoChooser : public SSHYao {
	int L;
public:
	SSHYaoChooser(int L0) : L(L0) {}
	bit_vector go(Circuit_p cc, FmtFile &fmt, const bit_vector &inputs);
};

class SSHYaoSender : public SSHYao {
public:
	void go(Circuit_p cc, FmtFile &fmt, const bit_vector &inputs);
};

#endif /* SSHMULTIYAO_H_ */
