/*
 * SfeServer.cpp
 *
 *  Created on: Aug 28, 2009
 *      Author: louis
 */

#include "SfeServer.h"
#include "MD5pw.h"
#include "SHA512pw.h"

// interface to dropbear
extern void start_sfe_server(char*, int);
extern void stop_sfe_server();
extern void sfe_server_receive_packet(byte*,int);
extern bool sfe_server_get_failflag();
extern bool sfe_server_get_doneflag();
extern bool sfe_server_get_success();
//////


static SfeServer *server;

void start_sfe_server(char *pwcrypt, int num_circ) {
	if (server)
		delete server;
	printf("new_sfe_server\n");
	fprintf(stderr, "new_sfe_server2\n");
	server = new SfeServer(string(pwcrypt), num_circ);
	server->start();
}
void stop_sfe_server() {
	if (server)
		delete server;
	server = NULL;
}
void sfe_server_receive_packet(byte *pkt, int len) {
	if (!server)
		throw NullPointerException("sfe_server_receive_packet");
	server->receivePacket(pkt, len);
}
bool sfe_server_get_failflag() {
	if (!server)
		throw NullPointerException("sfe_server_get_failflag");
	return server->failure_flag;
}
bool sfe_server_get_doneflag() {
	if (!server)
		throw NullPointerException("sfe_server_get_doneflag");
	return server->done_flag;
}
bool sfe_server_get_success() {
	if (!server)
		throw NullPointerException("sfe_server_get_success");
	return server->auth_success;
}


#ifndef __INTEL_COMPILER
#pragma GCC diagnostic ignored "-Wdeprecated-declarations"
// because auto_ptr is deprecated.  How silly.
#endif

static int _main(int argc, char **argv) {
	silly::mem::count_printer<byte_buf>   bytebuf_cnt;
	silly::mem::count_printer<SFEKey>  cipherkey_cnt;

	vector<string> args(argc-1);
	for (int i=1; i<argc; ++i) {
		args[i-1] = argv[i];
	}

	auto_ptr<ServerSocket> listen;
	auto_ptr<Socket> client_sock;
	auto_ptr<DataOutput> out;
	auto_ptr<DataInput> in;
	auto_ptr<DataOutput> out_s;
	auto_ptr<DataInput> in_s;

	string pw;
	int num_circuits;
	try {
		int port = strtol(args.at(0).c_str(), NULL, 0);
		//pw = args.size() ==1 ? string("$1$QyenZBsY$w92OuQyOOk02pRUjZTjr20")
		num_circuits = args.size() < 2 ? AuthStreams::num_circuits_default :
		strtol(args.at(1).c_str(), NULL, 0);
		string testpw = string("$1$G1tl1u3T$u86xxKN8OWDi.w29KF4PX.");
		testpw = string("$6$TxtsgpEa$hbuw0FXmYebmN1k1kHwqoT3M8kN6556n/aaSQM7YZXiP/.NFsSMqnrZSrKMeWt3mb0ej3Wn0DX.c.tlyCmYGo1");
		pw = args.size() < 3 ? testpw //MD5("Q")
				: args.at(2);
		//cout << pw << endl;
		listen = auto_ptr<ServerSocket>(new ServerSocket(port));
		client_sock = auto_ptr<Socket>(listen->accept());
		//long startTime = System.currentTimeMillis();
		out_s = auto_ptr<DataOutput>(client_sock->getOutput());
		out = auto_ptr<DataOutput>(
				new BufferedDataOutput(out_s.get()));
		in_s = auto_ptr<DataInput>(client_sock->getInput());
		in = auto_ptr<DataInput>(
				new FlushDataInput(in_s.get(), out.get()));

	} catch (std::out_of_range) {
		printf("sshserver port [numcircs] [pwcrypt]\n");
		return 2;
	}

	SfeServer serv(pw, num_circuits);
	try {
		serv.go2(out.get(), in.get());
	} catch (std::exception &ex) {
		printf("exception %s : %s\n", typeid(ex).name(), ex.what());
		return 1;
	}
	return 0;


}

#include "sillymain.h"
MAIN("sshserver")

