/*
 * GarbledCircuit.cpp
 *
 *  Created on: Aug 17, 2009
 *      Author: louis
 */

#include "GarbledCircuit.h"
#include "shdl.h"

using namespace shdl;

GarbledCircuit::GarbledCircuit() {
	use_permute = false;
}

GarbledCircuit::~GarbledCircuit() {
	// TODO Auto-generated destructor stub
}

string SecretKey::toHexString() {
	return silly::io::toHexString(*getEncoded());
}
void GarbledCircuit::writeCircuit(DataOutput *out) {
	out->writeBoolean(use_permute);
	out->writeInt(nInputs);
	out->writeInt(outputs.size());
	for (uint i=0; i<outputs.size(); ++i)
		out->writeInt(outputs[i]);

	out->writeInt(outputSecrets[0][0]->getEncoded()->size());

	out->writeInt(outputSecrets.size());

	for (uint i=0; i<outputSecrets.size(); ++i) {
//		cout << "write osecret0 len " << outputSecrets[i][0]->getEncoded()->size() << endl;
//		cout << "write osecret1 len " << outputSecrets[i][1]->getEncoded()->size() << endl;
		out->write(*outputSecrets[i][0]->getEncoded());
		out->write(*outputSecrets[i][1]->getEncoded());
	}

	out->writeInt(allGates[0]->truthtab[0].size());
	out->writeInt(allGates.size());
	for (uint i=0; i<allGates.size(); ++i) {
		out->writeByte(allGates[i]->arity);
		for (int j=0; j<allGates[i]->arity; ++j) {
			out->writeInt(allGates[i]->inputs[j]);
		}
		for (uint j=0; j<allGates[i]->truthtab.size(); ++j) {
//			cout << "write truthtab len " << allGates[i]->truthtab[j].size() << endl;
			out->write(allGates[i]->truthtab[j]);
		}
	}
}

GarbledCircuit_p GarbledCircuit::readCircuit(DataInput *in) {
	GarbledCircuit *gcc_ptr = new GarbledCircuit();
	GarbledCircuit &gcc = *gcc_ptr;
	gcc.use_permute = in->readBoolean();
	gcc.nInputs = in->readInt();
	gcc.outputs.resize(in->readInt());
	for (uint i=0; i<gcc.outputs.size(); ++i)
		gcc.outputs[i] = in->readInt();

	int seclen = in->readInt();

	gcc.outputSecrets.resize(in->readInt());
	// each one needs 2
	for (uint i=0; i<gcc.outputSecrets.size(); ++i) {
		byte_buf buf(seclen);
		in->readFully(&buf[0], seclen);
		gcc.outputSecrets[i].s0 = SFEKey_p(new SFEKey(buf));
		buf.resize(seclen);
		in->readFully(&buf[0], seclen);
		gcc.outputSecrets[i].s1 = SFEKey_p(new SFEKey(buf));
	}

	seclen = in->readInt();

	gcc.allGates.resize(in->readInt());
	for (uint i=0; i<gcc.allGates.size(); ++i) {
		gcc.allGates[i] = GarbledGate_p(new GarbledGate());
		gcc.allGates[i]->id = i + gcc.nInputs;
		gcc.allGates[i]->arity = in->readByte();
		gcc.allGates[i]->inputs.resize(gcc.allGates[i]->arity);
		for (int j=0; j<gcc.allGates[i]->arity; ++j) {
			gcc.allGates[i]->inputs[j] = in->readInt();
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
			throw ProtocolException(string_printf("Unexpected arity: %d", tts).c_str());
		}

		gcc.allGates[i]->truthtab.resize(tts);
		for (uint j=0; j<gcc.allGates[i]->truthtab.size(); ++j) {
			gcc.allGates[i]->truthtab[j].resize(seclen);
			in->readFully(gcc.allGates[i]->truthtab[j]);
		}
	}
	return GarbledCircuit_p(gcc_ptr);
}



void GarbledCircuit::hashCircuit(byte_buf &md) {
	BytesDataOutput out;
	writeCircuit(&out);
	md.resize(20);
	SHA1((const uchar*)(&out.buf[0]), out.buf.size(), (uchar*)(&md[0]));
}
