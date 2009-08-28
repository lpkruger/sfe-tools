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
	typedef map<BigInt, BigInt> db_map_t;
public:
	db_map_t thedb;


	uint size() const {
		return thedb.size();
	}
	void put(const byte_buf &key, const byte_buf &value) {
		thedb[BigInt::toPaddedBigInt(key)] =
				BigInt::toPaddedBigInt(value);
	}
	void put(CBigInt &k, CBigInt &v) {
		thedb[k] = v;
	}

	CBigInt *get(CBigInt &k) {
		db_map_t::iterator it = thedb.find(k);
		if (it == thedb.end())
			return NULL;
		return &it->second;
	}
	byte_buf get(byte_buf &k) {
		CBigInt *v = get(BigInt::toPaddedBigInt(k));
		if (!v)
			return byte_buf();
		return BigInt::fromPaddedBigInt(*v);
	}

	DDB() {}
	~DDB() {}
};

}
#endif /* DB_H_ */
