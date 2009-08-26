/*
 * GarbledCircuit.cpp
 *
 *  Created on: Aug 17, 2009
 *      Author: louis
 */

#include "GarbledCircuit.h"
#include "shdl.h"

#ifdef NO_RVALREF
#define move(x) x
#endif

using namespace shdl;

GarbledCircuit::GarbledCircuit() {
	use_permute = 0;
}

GarbledCircuit::~GarbledCircuit() {
	// TODO Auto-generated destructor stub
}

void GarbledCircuit::writeCircuit(DataOutput &out) {
	out.writeBoolean(use_permute);
	out.writeInt(nInputs);
	out.writeInt(outputs.size());
	for (uint i=0; i<outputs.size(); ++i)
		out.writeInt(outputs[i]);

	out.writeInt(outputSecrets[0][0]->getEncoded().size());

	out.writeInt(outputSecrets.size());

	for (uint i=0; i<outputSecrets.size(); ++i) {
		out.write(outputSecrets[i][0]->getEncoded());
		out.write(outputSecrets[i][1]->getEncoded());
	}

	out.writeInt(allGates[0]->truthtab[0].size());
	out.writeInt(allGates.size());
	for (uint i=0; i<allGates.size(); ++i) {
		out.writeByte(allGates[i]->arity);
		for (int j=0; j<allGates[i]->arity; ++j) {
			out.writeInt(allGates[i]->inputs[j]);
		}
		for (uint j=0; j<allGates[i]->truthtab.size(); ++j) {
			out.write(allGates[i]->truthtab[j]);
		}
	}
}

GarbledCircuit GarbledCircuit::readCircuit(DataInput &in) {
	GarbledCircuit gcc;
	gcc.use_permute = in.readBoolean();
	gcc.nInputs = in.readInt();
	gcc.outputs.resize(in.readInt());
	for (uint i=0; i<gcc.outputs.size(); ++i)
		gcc.outputs[i] = in.readInt();

	int seclen = in.readInt();

	gcc.outputSecrets.resize(in.readInt());
	// each one needs 2
	for (uint i=0; i<gcc.outputSecrets.size(); ++i) {
		vector<byte> buf(seclen);
		in.readFully(&buf[0], seclen);
		gcc.outputSecrets[i].resize(2);
		gcc.outputSecrets[i][0] = new SFEKey(buf);
		buf.resize(seclen);
		in.readFully(&buf[0], seclen);
		gcc.outputSecrets[i][1] = new SFEKey(buf);
	}

	seclen = in.readInt();

	gcc.allGates.resize(in.readInt());
	for (uint i=0; i<gcc.allGates.size(); ++i) {
		gcc.allGates[i]->id = i + gcc.nInputs;
		gcc.allGates[i]->arity = in.readByte();
		gcc.allGates[i]->inputs.resize(gcc.allGates[i]->arity);
		for (int j=0; j<gcc.allGates[i]->arity; ++j) {
			gcc.allGates[i]->inputs[j] = in.readInt();
		}
		int tts = -1;
		switch(gcc.allGates[i]->arity) {
		case 0:
			tts = 1; break;
		case 1:
			tts = 2; break;
		case 2:
			tts = 4; break;
		case 3:
			tts = 8; break;
		default:
			do {} while(0);
			// TODO: throw new RuntimeException("Unexpected arity: " + tts);
		}

		gcc.allGates[i]->truthtab.resize(tts);
		for (uint j=0; j<gcc.allGates[i]->truthtab.size(); ++j) {
			gcc.allGates[i]->truthtab[j].resize(seclen);
			in.readFully(&gcc.allGates[i]->truthtab[j][0], gcc.allGates[i]->truthtab[j].size());
		}
	}
	return gcc;
}



void GarbledCircuit::hashCircuit(vector<byte> &md) {
	BytesDataOutput out;
	writeCircuit(out);
	md.resize(20);
	SHA1((const uchar*)(&out.buf[0]), out.buf.size(), (uchar*)(&md[0]));
}

