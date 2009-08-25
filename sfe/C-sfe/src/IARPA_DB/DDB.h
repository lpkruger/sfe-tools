/*
 * DB.h
 *
 *  Created on: Aug 7, 2009
 *      Author: louis
 */

#ifndef DB_H_
#define DB_H_

//#include <openssl/bn.h>
#include "bigint.h"
#include <map>

//typedef BIGNUM* BigInteger;
using namespace std;
using namespace bigint;

namespace iarpa {

class DDB {
public:
	map<BigInt, BigInt> thedb;

	void put(CBigInt &k, CBigInt &v) {
		thedb[k] = v;
	}


	DDB() {}
	~DDB() {}
};

}
#endif /* DB_H_ */
