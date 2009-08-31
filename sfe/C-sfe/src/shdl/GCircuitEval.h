/*
 * GCircuitEval.h
 *
 *  Created on: Aug 26, 2009
 *      Author: louis
 */

#ifndef GCIRCUITEVAL_H_
#define GCIRCUITEVAL_H_

#include "sillytype.h"
#include "GarbledCircuit.h"
#include "CircuitCrypt.h"

class GCircuitEval : public CircuitCrypt {
	GarbledGate_p getGate(int id, GarbledCircuit &gcc) {
		// DEBUG:
//		if (id<gcc.nInputs || id>20000)
//			memset(0,0,1);


		return gcc.allGates[id - gcc.nInputs];
	}
	byte_buf_p eval_rec(GarbledGate_p g, GarbledCircuit &gcc, map<int, byte_buf_p> &vals);
public:
	GCircuitEval();
	virtual ~GCircuitEval();

	bit_vector eval(GarbledCircuit &gcc, vector<SecretKey_p> &insk);

};

#endif /* GCIRCUITEVAL_H_ */
