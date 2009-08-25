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

class DDB {
public:
	map<BigInt, BigInt> thedb;
	void put(int k, int v);
	void put(BigInt k, BigInt v);
	DDB();
	virtual ~DDB();
};

#endif /* DB_H_ */
