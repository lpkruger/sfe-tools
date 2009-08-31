/*
 * CircuitCrypt.cpp
 *
 *  Created on: Aug 26, 2009
 *      Author: louis
 */

#include "CircuitCrypt.h"

#undef DEBUG
#include "sillydebug.h"

const string CircuitCrypt::CIPHER = "SHA-1";

CircuitCrypt::~CircuitCrypt() {
	// TODO Auto-generated destructor stub
}

CircuitCrypt::CircuitCrypt() {
	C = SFECipher();
	KG = SFEKeyGenerator();
	//KG.init(128);
	//KG.init(80);
	//		try {
	//			C = SFECipher.getInstance(CIPHER);
	//			KG = SFEKeyGenerator.getInstance(CIPHER, random);
	//KG.init(128);
	//		} catch (GeneralSecurityException ex) {
	//			ex.printStackTrace();
	//			throw new RuntimeException("Can't initialize cipher");
	//		}
}

GarbledCircuit_p CircuitCrypt::encrypt(Circuit &cc, vector<boolean_secrets> &inputSecrets) {
	GarbledCircuit *gcc_ptr = new GarbledCircuit();
	GarbledCircuit &gcc = *gcc_ptr;
	gcc.nInputs = cc.inputs.size();
	gcc.outputs.resize(cc.outputs.size());
	gcc.outputSecrets.resize(cc.outputs.size());
	inputSecrets.resize(cc.inputs.size());

	gateid.clear();
	themap.clear();
	secrets.clear();

	// TODO: EDProto3 injection hack

	curId = cc.inputs.size();

	for (uint i=0; i<cc.outputs.size(); ++i) {
		gcc.outputs[i] = encGate_rec(cc.outputs[i]);
		gcc.outputSecrets[i] = secrets.at(gcc.outputs[i]);

		// special case
		if (cc.outputs[i]->arity == 0) {
			boolean val = cc.outputs[i]->truthtab[0];
			gcc.outputSecrets[i][val ? 1 : 0] =
					SFEKey_p(new SFEKey(themap.at(cc.outputs[i])->truthtab[0]));
			// TODO: replace call to SFEKey constructor
		}
	}

	for (uint i=0; i<cc.inputs.size(); ++i) {
		inputSecrets[i] = secrets.at(i);
		DC("i " << i << "  " << inputSecrets[i].s0->toHexString() <<
				"  " << inputSecrets[i].s1->toHexString() << endl);

		// TODO avoid null pointer on unused inputs
		//			if (data.inputSecrets[i] == null) {
		//				data.inputSecrets[i] = new SecretKey[] { KG.generateKey(), KG.generateKey() };
		//			}
	}

	//gcc.allGates = gateid.tailMap(cc.inputs.length).values().toArray(new GarbledCircuit.Gate[0]);
	gcc.allGates.clear();
	//gcc.allGates.reserve(gateid.size());
	// TODO: use an iterator range instead
	for (gateid_t::iterator it = gateid.begin(); it != gateid.end(); ++it) {
		//DC(it->first << " >= " << cc.inputs.size() << " "<<(it->first >= cc.inputs.size()) << endl;
		//DC("  tt size: " << it->second->truthtab.size() << endl;
		if (it->first >= int(cc.inputs.size()))
			gcc.allGates.push_back(it->second);

	}
	return GarbledCircuit_p(gcc_ptr);
}

boolean_secrets CircuitCrypt::genKeyPair(GateBase_p g) {
	boolean_secrets ret;
	ret.s0 = KG.generateKey();
	ret.s1 = KG.generateKey();
	return ret;
}

int CircuitCrypt::encGate_rec(Gate_p gate) {
	gatemap_t::iterator egate_it = themap.find(gate);
	if (egate_it != themap.end())
		return egate_it->second->id;

	GarbledGate_p egate(new GarbledGate());
	egate->arity = gate->arity;
	egate->inputs.resize(egate->arity);
	vector<boolean_secrets> inpsecs(egate->arity);

	for (int i=0; i<egate->arity; ++i) {
		//Input_p inp;
		//if (gate->inputs[i].dyncast_to(inp)) {
		Input_p inp = dynamic_pointer_cast<Input>(gate->inputs[i]);
		if (inp.get()) {
			int var = inp->var;
			egate->inputs[i] = var;
			secretmap_t::iterator sec_it = secrets.find(var);
			if (sec_it == secrets.end()) {
				inpsecs[i] = genKeyPair(gate->inputs[i]);
				secrets[var] = inpsecs[i];
			}
		} else {
			egate->inputs[i] = encGate_rec(
					dynamic_pointer_cast<Gate>(gate->inputs[i]));
			inpsecs[i] = secrets.at(egate->inputs[i]);
		}
	}

	egate->id = getId();

	gateid[egate->id] = egate;
	themap[gate] = egate;
	boolean_secrets secr = genKeyPair(gate);
	secrets[egate->id] = secr;

	//		TODO
	//		if (egate->arity == 0) {  // special case to avoid NullPointerException
	//			inpsecs = new SecretKey[][] { genKeyPair(null) };
	//		}

	//System.out.println("inpsecs.length : " + inpsecs.length);
	//System.out.println("gate.truthtab.length : " + gate.truthtab.length);

	egate->truthtab.resize(gate->truthtab.size());
	DC("resize gate " << egate->id << " truthtab to " << egate->truthtab.size() << endl);
	for (uint i=0; i<egate->truthtab.size(); ++i) {
		SFEKey_p thisKey = ((i >> (egate->arity-1) & 0x1) == 0) ? inpsecs[0][0] : inpsecs[0][1];
		for (int j=1; j<egate->arity; ++j) {
			DC("enc xor " << thisKey->getEncoded()->size() << " " <<
			(((i >> (egate->arity-j-1) & 0x1) == 0) ? inpsecs[j][0] : inpsecs[j][1])->getEncoded()->size()
			<< endl);
			thisKey = SFEKey::xxor(thisKey,
					((i >> (egate->arity-j-1) & 0x1) == 0) ? inpsecs[j][0] : inpsecs[j][1],
							CIPHER);
		}

		//			try {
		C.init(C.ENCRYPT_MODE, thisKey);
		egate->truthtab[i] = C.doFinal(*secr[gate-> truthtab[i]?1:0]->getEncoded());
		DC(toHexString(egate->truthtab[i]) << endl);

		//			} catch (GeneralSecurityException ex) {
		//				ex.printStackTrace();
		//				throw new RuntimeException("encryption error");
		//			}

		// variation: randomly permute the truth table
		// see CircuitCryptPermute
	}

	return egate->id;
}

