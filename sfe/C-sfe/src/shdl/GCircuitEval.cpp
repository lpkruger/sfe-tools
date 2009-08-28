/*
 * GCircuitEval.cpp
 *
 *  Created on: Aug 26, 2009
 *      Author: louis
 */

#include "GCircuitEval.h"
#include <string>

#define D(x) std::cout << x

GCircuitEval::GCircuitEval() {
	// TODO Auto-generated constructor stub

}

GCircuitEval::~GCircuitEval() {
	// TODO Auto-generated destructor stub
}

vector<bool> GCircuitEval::eval(GarbledCircuit &gcc, vector<SecretKey_p> &insk) {
#if 0
	if (gcc.use_permute) {
		string NOPADDING = "//NoPadding";
		try {
			C = SFECipher.getInstance(CIPHER + NOPADDING);
		} catch (GeneralSecurityException ex) {
			ex.printStackTrace();
			throw new RuntimeException("error init cipher");
		}
	}
#endif
	map<int,byte_buf_p> vals;
	for (uint i=0; i<insk.size(); ++i) {
		vals[i] = insk[i]->getEncoded();
	}

	vector<bool> ret(gcc.outputs.size());
	for (uint i=0; i<gcc.outputs.size(); ++i) {
		byte_buf_p retval = eval_rec(getGate(gcc.outputs[i], gcc), gcc, vals);
		SecretKey_p retkey = SFEKey::bytesToKey(*retval, CIPHER);

		/*
			System.out.println("O " + i + " " + Base64.encodeBytes(retval) + " " +
					Base64.encodeBytes(gcc.outputSecrets[i][0].getEncoded()) + " " +
					Base64.encodeBytes(gcc.outputSecrets[i][1].getEncoded()));
		 */

		D("output ");
		int cout_width = cout.width(6);
		D(i);
		cout.width(cout_width);
		D("  " << SFEKey::toHexString(*retval) << endl);
		D("true           " << SFEKey::toHexString(*gcc.outputSecrets[i].s1->getEncoded()) << endl);
		D("false          " << SFEKey::toHexString(*gcc.outputSecrets[i].s0->getEncoded()) << endl);

		if (gcc.outputSecrets[i][0]->equals(retkey.get()))
			ret[i] = false;
		else if (gcc.outputSecrets[i][1]->equals(retkey.get()))
			ret[i] = true;
		else {
			// s1, s0 backwards?
			throw std::logic_error("eval error : no matching secret");
		}

	}

	return ret;
}

//byte_buf_p map_get(const std::map<int,byte_buf_p> &map, const int &key) {
//	typedef std::map<int,byte_buf_p>::const_iterator map_it;
//	map_it it = map.find(key);
//	if (it == map.end())
//		return byte_buf_p(NULL);
//	return it->second;
//}

template<class T,class U> static inline wise_ptr<T> map_get(const std::map<U,wise_ptr<T> > &map, const U &key) {
#if DEBUG_WISEPTR
	std::D("map get: wise_ptr overload" << std::endl;
#endif
	typedef typename std::map<U,wise_ptr<T> >::const_iterator map_it;
	map_it it = map.find(key);
	if (it == map.end())
		return wise_ptr<T>(NULL);
	return it->second;
}

byte_buf_p GCircuitEval::eval_rec(GarbledGate_p g, GarbledCircuit &gcc, map<int,byte_buf_p> &vals) {
	if (g->arity == 0) {
		vals[g->id] = byte_buf_p(new byte_buf(g->truthtab[0]));
		return byte_buf_p(new byte_buf(g->truthtab[0]));
	}

	byte_buf_p ink = map_get(vals, g->inputs[0]);
	if (ink.get() == NULL) {
		ink = eval_rec(getGate(g->inputs[0], gcc), gcc, vals);
	}

	//D("gate " << g->id << " arity " << g->arity << endl);
	for (int i=1; i<g->arity; ++i) {
		byte_buf_p ink2 = map_get(vals, g->inputs[i]);
		if (ink2.get() == NULL) {
			ink2 = eval_rec(getGate(g->inputs[i], gcc), gcc, vals);
		}
		//D("lengths: " << ink->size() << "  " << ink2->size() << endl);
		ink = SFEKey::xxor(ink, ink2);
		//D("newlens: " << ink->size() << "  " << ink2->size() << endl);
	}

	byte_buf_p out;
	SecretKey_p sk = SFEKey::bytesToKey(*ink, CIPHER);

	int ttstart = 0;
	if (gcc.use_permute) {
		for (int i=0; i<g->arity; ++i) {
			int bit = (*vals.at(g->inputs[i]))[0] & 0x01;
			ttstart = (ttstart << 1) | bit;
		}
	}

	// TODO: don't use bad_padding exception, it's slow
	for (uint i=ttstart; i<g->truthtab.size(); ++i) {
		try {
			C.init(C.DECRYPT_MODE, sk);
			out = byte_buf_p(new byte_buf(C.doFinal(g->truthtab[i])));
			if (out->size() == KG.getLength()/8)
				break;
		} catch (bad_padding ex) {
			// it wasn't the right one, keep trying
		}
	}

	if (out.get() == NULL) {
		throw logic_error("Can't decrypt TT with key "); // + Base64.encodeBytes(ink));
	}

	vals[g->id] = out;
	return out;
}

