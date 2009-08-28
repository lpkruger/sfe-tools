/*
 * YaoProtocol.cpp
 *
 *  Created on: Aug 26, 2009
 *      Author: louis
 */

#include "YaoProtocol.h"

YaoProtocol::YaoProtocol() {
	// TODO Auto-generated constructor stub

}

YaoProtocol::~YaoProtocol() {
	// TODO Auto-generated destructor stub
}

#if 0		// test code
static int _main(int argc, char **argv) {
	vector<string> args(argc-1);
	for (int i=1; i<argc; ++i) {
		args[i-1] = argv[i];
	}

	args.at(0);

	if (args[0] == ("A")) {
		args.at(1);
		cout << "connecting" << endl;
		s = new Socket("localhost", 5437);
		DataOutput *out = s->getOutput();
		DataInput *in = s->getInput();
		YaoClient cli;
		cli.setStreams(in, out);
		cli.online(BigInt(atoi(args[1].c_str())));
		delete out;
		delete in;
		delete s;
	} else if (args[0] == ("B")) {
		args.at(1);
		iarpa::DDB ddb;
		iarpa_ko_populate_test_db(ddb, atoi(args[1].c_str()));
		YaoServer serv;
		serv.precompute(ddb);
		cout << "listening" << endl;
		ServerSocket *ss = new ServerSocket(5437);
		s = ss->accept();
		DataOutput *out = s->getOutput();
		DataInput *in = s->getInput();
		serv.setStreams(in, out);
		serv.online();
		delete s;
		delete ss;
		delete out;
		delete in;
	}
}
#endif
