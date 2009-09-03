/*
 * CircuitCryptPermute.cpp
 *
 *  Created on: Sep 2, 2009
 *      Author: louis
 */

#include "CircuitCryptPermute.h"
#include <openssl/rand.h>



void CircuitCryptPermute::reset() {
	flip.clear();
	super::reset();
}

GarbledCircuit_p CircuitCryptPermute::encrypt(Circuit &cc, vector<boolean_secrets> &inputsecrets) {
//		String NOPADDING = "//NoPadding";
//		try {
//			C = SFECipher.getInstance(CIPHER + NOPADDING);
//		} catch (GeneralSecurityException ex) {
//			ex.printStackTrace();
//			throw new RuntimeException("error init cipher");
//		}

	//C.use_padding = false;
	flip.clear();
	cc.calcDeps();
	for (uint i=0; i<cc.outputs.size(); ++i) {
		doFlip_rec(cc.outputs[i]);
	}

	GarbledCircuit_p ret; ret = super::encrypt(cc, inputsecrets);

	for (uint i=0; i<cc.inputs.size(); ++i) {
		flip_it_t it = flip.find(cc.inputs[i]);
		if (it != flip.end() && it->second) {

			SFEKey_p tmp = inputsecrets[i][1];
			inputsecrets[i].s1 = inputsecrets[i].s0;
			inputsecrets[i].s0 = tmp;

		}
	}
	ret->use_permute = true;

	return ret;
}


boolean_secrets CircuitCryptPermute::genKeyPair(GateBase_p g) {
	boolean_secrets sk2 = CircuitCrypt::genKeyPair(g);
	byte_buf *b0 = sk2.s0->getRawBuffer();
	byte_buf *b1 = sk2.s1->getRawBuffer();
	(*b0)[0] &= (0xfe);
	(*b1)[0] |= (0x01);
	return sk2;
}

bool CircuitCryptPermute::doFlip_rec(GateBase_p g) {
	map<GateBase_p, bool>::iterator it = flip.find(g);
	if (it != flip.end()) {
		return it->second;
	}
	bool f;
	if (g->isOuput()) {
		// TODO: need to garble Alice's output bits only
		f = false;
	} else {
		byte b = 0;
		RAND_bytes(&b, 1);
		f = b & 0x01;
	}

	flip[g] = f;
	if (g->isInput()) {
		return f;
	}

	Gate_p gg = dynamic_pointer_cast<Gate>(g);

	for (int i=0; i<gg->arity; ++i) {
		if (doFlip_rec(gg->inputs[i])) {
			permuteInput(gg->truthtab, gg->arity, i);
		}
	}

	if (f) {
		for (uint i=0; i<gg->truthtab.size(); ++i)
			gg->truthtab[i] = !gg->truthtab[i];
	}

	return f;
}

// permutes the truth table according to the variable pos being flipped
void CircuitCryptPermute::permuteInput(bit_vector &tt, int arity, int pos) {

	// "partial evaluation" of a single truth table
	//bit_vector ntt(tt.size() >> 1);

	int q = arity - 1 - pos;
	int qq = 1 << q;

	//int j=0;
	for (uint i=0; i<tt.size(); ++i) {
		if (((i>>q)&1) == 0) {
			bool tmp = tt[i+qq];
			tt[i+qq] = tt[i];
			tt[i] = tmp;
		}
	}
}


