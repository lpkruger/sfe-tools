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

#include <errno.h>
static const char *circ_path[] = { "/etc/dropbear/", "/tmp/dropbear/", "./" };
	// "/home/louis/sfe/", "/u/l/p/lpkruger/research/sfe/" };
static void open_file(ifstream &in, const char *fname) {
	string str;
	for (uint i=0; i<sizeof(circ_path)/sizeof(const char*); ++i) {
		str = string(circ_path[i]) + fname;
		//cerr << "try: " << str << endl;
		in.open(str.c_str());
		if (in.is_open()) {
			cerr << "open: " << str << endl;
			return;
		}
	}
	cerr << "cannot open file " << fname << endl;
	throw new IOException(ENOENT, fname);
}

struct FlushDataInput : public BufferedDataInput {
	DataOutput *out;
	FlushDataInput(DataInput *under, DataOutput *out0) :
		BufferedDataInput(under), out(out0) {}
	virtual int tryRead(byte *c, int len) _QUICK {
		out->flush();
		return BufferedDataInput::tryRead(c, len);
	}
};

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
		no_fast_sync = (opt==NULL || !strcmp(opt, "0"));
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
			out->flush();
		} else {
			out->flush();
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
	void go(Circuit_p cc, FmtFile &fmt, const bit_vector &inputs,
			const string &circ);
};

#endif /* SSHMULTIYAO_H_ */
