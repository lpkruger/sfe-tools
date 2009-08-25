/*
 * DBMain.cpp
 *
 *  Created on: Aug 22, 2009
 *      Author: louis
 */

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

static int _main(int argc, char **argv) {
	for (int trial=0; trial<min(100,KO::test_sizes_length); ++trial) {
		DDB ddb;
		for (int i=0; i<KO::test_sizes[trial]; ++i) {
			int x = i+2;
			int xx = x*x;
			ddb.put(x, xx);
		}

		KO ok;
		ok.n = ddb.thedb.size();

		long time1 = currentTimeMillis();
		ok.servercommit(ddb);
		long time2 = currentTimeMillis();
		BigInt b_key(2);
		ok.clientxfer1(b_key);
		ok.serverxfer();
		ok.clientxfer2(b_key);
		long time3 = currentTimeMillis();
		printf("DB size %d: %ld online %ld offline\n",
				ok.n, (time3-time2), (time2-time1));
		//System.out.println();
	}
}


#include "../sillylib/sillymain.h"
MAIN("dbtest");
