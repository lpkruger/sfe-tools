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
		return gcc.allGates[id - gcc.nInputs];
	}
	byte_buf_p eval_rec(GarbledGate_p g, GarbledCircuit &gcc, map<int, byte_buf_p> &vals);
public:
	GCircuitEval();
	virtual ~GCircuitEval();

	vector<bool> eval(GarbledCircuit &gcc, vector<SecretKey_p> &insk);

};

#endif /* GCIRCUITEVAL_H_ */
