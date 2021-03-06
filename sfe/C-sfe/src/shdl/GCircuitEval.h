/*
 * GCircuitEval.h
 *
 *  Created on: Aug 26, 2009
 *      Author: louis
 */

#ifndef GCIRCUITEVAL_H_
#define GCIRCUITEVAL_H_

#include "sillytype.h"
#include "sillymem.h"
#include "GarbledCircuit.h"
#include "CircuitCrypt.h"

typedef map<int, byte_buf*> buf_map;

class GCircuitEval : public CircuitCrypt, public Reclaimer<byte_buf> {
	GarbledGate_p getGate(int id, GarbledCircuit &gcc) {
		// DEBUG:
//		if (id<gcc.nInputs || id>20000)
//			memset(0,0,1);


		return gcc.allGates[id - gcc.nInputs];
	}
	byte_buf* eval_rec(GarbledGate_p g, GarbledCircuit &gcc, buf_map &vals);
	//vector<byte_buf*> garbage;
public:
	GCircuitEval() : CircuitCrypt(NULL) {}
	bit_vector eval(GarbledCircuit &gcc, vector<SecretKey_p> &insk);

};

#endif /* GCIRCUITEVAL_H_ */
