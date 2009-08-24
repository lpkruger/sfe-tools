/*
 * DB.h
 *
 *  Created on: Aug 7, 2009
 *      Author: louis
 */

#ifndef DB_H_
#define DB_H_

#include <openssl/bn.h>
#include <map>

typedef BIGNUM* BigInteger;
using namespace std;

//bool bn_comp (BigInteger lhs, BigInteger rhs) {return BN_cmp(lhs,rhs)<0;}
struct bncmp {
  bool operator() (BigInteger lhs, BigInteger rhs) const
  {return BN_cmp(lhs,rhs)<0;}
};
class DDB {
public:
	map<BigInteger, BigInteger, bncmp> thedb;
	void put(int k, int v);
	void put(BigInteger k, BigInteger v);
	DDB();
	virtual ~DDB();
};

#endif /* DB_H_ */
