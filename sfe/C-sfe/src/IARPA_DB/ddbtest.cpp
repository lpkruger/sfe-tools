/*
 * DBMain.cpp
 *
 *  Created on: Aug 22, 2009
 *      Author: louis
 */

#include <vector>
#include "silly.h"
#include "DDB.h"
#include "KO.h"


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
	return size;
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

using silly::misc::currentTimeMillis;

int iarpa::ko::test_ko(int argc, char **argv) {
//	for (int i=0; i<argc; ++i) {
//		printf("arg: %s\n", argv[i]);
//	}

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


static int _main_dbtest(int argc, char **argv) {
	return iarpa::ko::test_ko(argc, argv);
}


#include "../sillylib/sillymain.h"
MAINF("dbtest", _main_dbtest);



#include "sillyio.h"
#include <stdexcept>
using silly::net::ServerSocket;
using silly::net::Socket;

extern int iarpa_ko_populate_test_db(iarpa::DDB &ddb, int size);

static int _main_kotest(int argc, char **argv) {
	vector<string> args(argc-1);
	for (int i=1; i<argc; ++i) {
		args[i-1] = argv[i];
	}
	try {
		Socket *s;
		args.at(0);
		if (args[0] == ("A")) {
			args.at(1);
			cout << "connecting" << endl;
			s = new Socket("localhost", 5436);
			DataOutput *out = s->getOutput();
			DataInput *in = s->getInput();
			iarpa::ko::Client cli;
			cli.setStreams(in, out);
			byte_buf result = cli.online(BigInt(atoi(args[1].c_str())));
			cout << toHexString(result) << endl;
			BigInt result_num = BigInt::toPosBigInt(result);
			cout << result_num.toString() << endl;
			delete out;
			delete in;
			delete s;
		} else if (args[0] == ("B")) {
			args.at(1);
			iarpa::DDB ddb;
			iarpa_ko_populate_test_db(ddb, atoi(args[1].c_str()));
			iarpa::ko::Server serv;
			serv.precompute(ddb);
			cout << "listening" << endl;
			ServerSocket *ss = new ServerSocket(5436);
			s = ss->accept();
			DataOutput *out = s->getOutput();
			DataInput *in = s->getInput();
			serv.setStreams(in, out);
			serv.online();
			delete s;
			delete ss;
			delete out;
			delete in;
		} else {
			fprintf(stderr, "Please specify A or B\n");
			return 1;
		}

		return 0;
	} catch (std::out_of_range) {
		fprintf(stderr, "koproto A key_num (client)\n  or\nkoproto B dbsize (server)\n");
		return 1;
	}
}

MAINF("koproto", _main_kotest);


