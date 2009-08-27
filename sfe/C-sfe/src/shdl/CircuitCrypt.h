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
#include "sillytype.h"
#include "sillymem.h"
#include "GarbledCircuit.h"
#include "shdl.h"

using namespace std;

static const string CIPHER = "SHA-1";

class CircuitCrypt {
private:


//	protected SecureRandom random;
	SFECipher C;
//	public SFEKeyGenerator KG;

	typedef map<int, GarbledGate_p> gateid_t;
	gateid_t gateid;

	typedef map<Gate_p, GarbledGate_p> gatemap_t;
	gatemap_t themap;

	typedef map<int, vector<SFEKey_p> > secretmap_t;
	secretmap_t secrets;

	int curId;

	int getId() {
		return curId++;
	}
protected:
	vector<SFEKey_p> genKeyPair(GateBase_p g);
public:
	CircuitCrypt();
	virtual ~CircuitCrypt();
	virtual GarbledCircuit encrypt(Circuit &cc, atype<SFEKey_p>::matrix &inputsecrets);

private:
	int encGate_rec(Gate_p gate);

};
#endif /* CIRCUITCRYPT_H_ */
