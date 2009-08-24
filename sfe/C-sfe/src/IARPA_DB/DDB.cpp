/*
 * DB.cpp
 *
 *  Created on: Aug 7, 2009
 *      Author: louis
 */

#include "DDB.h"

DDB::DDB() {
	// TODO Auto-generated constructor stub

}

DDB::~DDB() {
	// TODO Auto-generated destructor stub
}

//static class Key implements Comparable {
//static class Val {

void DDB::put(int k, int v) {
	BigInteger kk, vv;
	kk = BN_new();
	vv = BN_new();
	BN_set_word(kk, k);
	BN_set_word(vv, v);
	thedb[kk] = vv;
}
void DDB::put(BigInteger k, BigInteger v) {
	thedb[k] = v;
}

