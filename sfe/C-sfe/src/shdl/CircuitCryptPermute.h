/*
 * CircuitCryptPermute.h
 *
 *  Created on: Sep 2, 2009
 *      Author: louis
 */

#ifndef CIRCUITCRYPTPERMUTE_H_
#define CIRCUITCRYPTPERMUTE_H_

#include "CircuitCrypt.h"

class CircuitCryptPermute : public CircuitCrypt {
	typedef CircuitCrypt super;
	map<GateBase_p, bool> flip;
	typedef map<GateBase_p, bool>::iterator flip_it_t;
public:
	CircuitCryptPermute(Random *r0) : super(r0) {
		C.setUsePadding(false);
	}
	virtual ~CircuitCryptPermute() {}
	virtual GarbledCircuit_p encrypt(Circuit &cc, vector<boolean_secrets> &inputsecrets);
	virtual void reset();

	virtual boolean_secrets genKeyPair(GateBase_p g);

	boolean doFlip_rec(GateBase_p g);
	static void permuteInput(bit_vector &tt, int arity, int pos);
};

#endif /* CIRCUITCRYPTPERMUTE_H_ */
