/*
 * GCircuitEval.cpp
 *
 *  Created on: Aug 26, 2009
 *      Author: louis
 */

#include "GCircuitEval.h"
#include <string>

//#define DEBUG
#include "sillydebug.h"

bit_vector GCircuitEval::eval(GarbledCircuit &gcc, vector<SecretKey_p> &insk) {
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
	map<int,byte_buf*> vals;
	DF("adding %d input secrets", insk.size());
	for (uint i=0; i<insk.size(); ++i) {
		vals[i] = new byte_buf(insk[i]->getEncoded());
		add_garbage(vals[i]);
	}

	bit_vector ret(gcc.outputs.size());
	for (uint i=0; i<gcc.outputs.size(); ++i) {
		byte_buf* retval = eval_rec(getGate(gcc.outputs[i], gcc), gcc, vals);
		SFEKey retkey(retval);

		/*
			System.out.println("O " + i + " " + Base64.encodeBytes(retval) + " " +
					Base64.encodeBytes(gcc.outputSecrets[i][0].getEncoded()) + " " +
					Base64.encodeBytes(gcc.outputSecrets[i][1].getEncoded()));
		 */

		DC("output ");
		int cout_width = cout.width(6);
		DC(i);
		cout.width(cout_width);
		DC("  " << toHexString(*retval) << endl);
		DC("true           " << toHexString(gcc.outputSecrets[i].s1->getEncoded()) << endl);
		DC("false          " << toHexString(gcc.outputSecrets[i].s0->getEncoded()) << endl);

		if (gcc.outputSecrets[i][0]->equals(&retkey))
			ret[i] = false;
		else if (gcc.outputSecrets[i][1]->equals(&retkey))
			ret[i] = true;
		else {
			// s1, s0 backwards?
			throw ProtocolException("eval error : no matching secret");
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

template<class T,class U> static inline T* map_get(const std::map<U,T*> &m, const U &key) {
#if DEBUG_WISEPTR
	std::D("map get: wise_ptr overload" << std::endl;
#endif
	//DF("map has %d elements", m.size());
	typedef typename map<U,T*>::const_iterator map_it;
	map_it it = m.find(key);
	if (it == m.end())
		return (T*)NULL;
	return it->second;
}

void D(map<int,byte_buf*> &m) {
	map<int,byte_buf*>::iterator it;
	fprintf(stderr, "dump vals map\n");
	for (it=m.begin(); it!=m.end(); ++it) {
		fprintf(stderr, "{ %d, %s }\n", it->first, toHexString(*it->second).c_str());
	}
}

byte_buf* GCircuitEval::eval_rec(GarbledGate_p g, GarbledCircuit &gcc, map<int,byte_buf*> &vals) {
	if (g->arity == 0) {
		vals[g->id] = &g->truthtab[0];
		return &g->truthtab[0];
	}

	byte_buf* ink = map_get(vals, g->inputs[0]);
	if (ink == NULL) {
		if (g->inputs[0] < gcc.nInputs) {
			fprintf(stderr, "bad0!\n");
			D(vals);
		}
		ink = eval_rec(getGate(g->inputs[0], gcc), gcc, vals);
	}

	//D("gate " << g->id << " arity " << g->arity << endl);
	for (int i=1; i<g->arity; ++i) {
		byte_buf* ink2 = map_get(vals, g->inputs[i]);
		if (ink2 == NULL) {
			if (g->inputs[i] < gcc.nInputs)
				fprintf(stderr, "bad%d!\n", g->inputs[i]);
			ink2 = eval_rec(getGate(g->inputs[i], gcc), gcc, vals);
		}
		//D("lengths: " << ink->size() << "  " << ink2->size() << endl);
		ink = new byte_buf(SFEKey::xxor(*ink, *ink2));
		add_garbage(ink);
		//D("newlens: " << ink->size() << "  " << ink2->size() << endl);
	}

	byte_buf* out;
	SFEKey sk(ink);

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
			out = new byte_buf(C.doFinal(g->truthtab[i]));
			add_garbage(out);
			if (out->size() == KG.getLength()/8)
				break;
		} catch (bad_padding ex) {
			// it wasn't the right one, keep trying
		}
	}

	if (out == NULL) {
		throw ProtocolException("Can't decrypt TT with key "); // + Base64.encodeBytes(ink));
	}

	vals[g->id] = out;
	return out;
}

