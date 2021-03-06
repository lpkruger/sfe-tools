/*
 * CircuitCrypt.h
 *
 *  Created on: Aug 26, 2009
 *      Author: louis
 */

#ifndef CIRCUITCRYPT_H_
#define CIRCUITCRYPT_H_

#include <string>
#include <map>
//#include <tr1/unordered_map>
#include "sillytype.h"
#include "sillymem.h"
#include "GarbledCircuit.h"
#include "shdl.h"
#include "../crypto/cipher/PseudoRandom.h"
//#include "../crypto/SecureRandom.h"
#include "sillystl.h"

using namespace std;
using crypto::Random;

class CircuitCrypt {
protected:
	Random *rand;

private:
	typedef vector_map<int, GarbledGate_p> gateid_t;
	gateid_t gateid;

	typedef map<Gate_p, GarbledGate_p> gatemap_t;
	gatemap_t themap;

	typedef vector_map<int, boolean_secrets> secretmap_t;
	secretmap_t secrets;

	int curId;

	int getId() {
		return curId++;
	}
protected:
	virtual boolean_secrets genKeyPair(GateBase_p g) _QUICK;

	static const string CIPHER;
	SFEKeyGenerator KG;
	SFECipher C;

public:
	CircuitCrypt(Random *r0);
	virtual ~CircuitCrypt() {
		delete rand;
	}
	virtual GarbledCircuit_p encrypt(Circuit &cc, vector<boolean_secrets> &inputsecrets);

	virtual void reset() {
		gateid.clear();
		themap.clear();
		secrets.clear();
		curId=0;
	}

private:
	int encGate_rec(Gate_p gate,
			Reclaimer<GarbledGate> &trash) _QUICK ;

};
#endif /* CIRCUITCRYPT_H_ */
