/*
 * DBMain.cpp
 *
 *  Created on: Aug 22, 2009
 *      Author: louis
 */

#include <vector>
#include <sys/time.h>
#include "DDB.h"
#include "KO.h"
static inline long currentTimeMillis() {
	struct timeval tv;
	gettimeofday(&tv, NULL);
	long ret = tv.tv_sec*1000;
	ret += (tv.tv_usec/1000);
	return ret;
}

using namespace iarpa;
using namespace iarpa::ko;

static const int test_sizes[] = { 1,2,3, 10, 20, 50, 100, 200, 500, 1000, 2000, 5000, 10000 };
static const int test_sizes_length = sizeof(test_sizes)/sizeof(int);
//static const int test_sizes_length = 3;

int iarpa_ko_populate_test_db(DDB &ddb, int size) {
	for (int i=0; i<size; ++i) {
		int x = i+2;
		int xx = x*x;
		ddb.put(x, xx);
	}
}

static BigInt bi_str(const char *s) {
	return BigInt::toPaddedBigInt(byte_buf(s, s+strlen(s)));
}
static byte_buf str(const char *s) {
	return byte_buf(s, s+strlen(s));
}
static const char* str(byte_buf &b) {
	static string str;
	b.push_back(0);
	string((const char*)&b[0]).swap(str);
	return str.c_str();
}

int iarpa::ko::test_ko(int argc, char **argv) {
	for (int trial=0; trial<min(100,test_sizes_length); ++trial) {
		DDB ddb;
		iarpa_ko_populate_test_db(ddb, test_sizes[trial]);
		ddb.put(str("foo"), str("bar"));
		Server okS;
		Client okC;

		long time1 = currentTimeMillis();
		okS.servercommit(ddb);
		okC.rsa_n = okS.rsa_n();
		okC.rsa_e = okS.rsa_e();
		long time2 = currentTimeMillis();

		//BigInt b_key(2);
		BigInt b_key(bi_str("foo"));

		BigInt Y_ = okC.clientxfer1(b_key);
		BigInt X_ = okS.serverxfer(Y_);
		byte_buf mi = okC.clientxfer2(b_key, X_, okS.mhat);
		for (uint j=0; j<mi.size(); ++j) {
			printf("%02x ", mi[j]);
		}
		printf("\n");
		printf("As string: %s\n", str(mi));


		long time3 = currentTimeMillis();
		printf("DB size %d: %ld online %ld offline\n",
				ddb.size(), (time3-time2), (time2-time1));
		//System.out.println();
	}
	return 0;
}


static int _main(int argc, char **argv) {
	return iarpa::ko::test_ko(argc, argv);
}
#include "../sillylib/sillymain.h"
MAIN("dbtest");

