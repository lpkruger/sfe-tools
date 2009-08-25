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
	thedb[k] = v;
}
void DDB::put(BigInt k, BigInt v) {
	thedb[k] = v;
}

