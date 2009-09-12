/*
 * SfeClient.cpp
 *
 *  Created on: Sep 4, 2009
 *      Author: louis
 */

#include "SfeClient.h"

// interface from dropbear
extern void start_sfe_client(char*);
extern void stop_sfe_client();
extern void sfe_client_receive_packet(byte*,int);
extern bool sfe_client_get_failflag();
//////


static SfeClient *client;

void start_sfe_client(char *pwcrypt) {
	if (client)
		delete client;
	client = new SfeClient(string(pwcrypt));
	client->start();
}
void stop_sfe_client() {
	if (client)
		delete client;
	client = NULL;
}
void sfe_client_receive_packet(byte *pkt, int len) {
	client->receivePacket(pkt, len);
}
bool sfe_client_get_failflag() {
	return client->failure_flag;
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

	auto_ptr<Socket> server_sock;
	auto_ptr<DataOutput> out_raw;
	auto_ptr<DataInput> in_raw;
	string pw;
	try {
		string to = args.at(0);
		int port = strtol(args.at(1).c_str(), NULL, 0);
		pw = args.at(2);

		server_sock = auto_ptr<Socket>(new Socket(to.c_str(), port));

		//long startTime = System.currentTimeMillis();
		//	ByteCountOutputStreamSFE byteCount = new ByteCountOutputStreamSFE(
		//			server_sock.getOutputStream());
		//	ObjectOutputStream out_raw = new ObjectOutputStream(byteCount);
		//	out_raw.flush();
		//	ObjectInputStream in_raw = new ObjectInputStream
		//		(new BufferedInputStream
		//				(server_sock.getInputStream()));


		out_raw = auto_ptr<DataOutput>(
				new BufferedDataOutput(server_sock->getOutput()));
		in_raw = auto_ptr<DataInput>(
				new FlushDataInput(server_sock->getInput(),
						out_raw.get()));


	} catch (std::out_of_range) {
		printf("sshclient to port pw\n");
		return 2;
	}
	SfeClient cli(pw);
	try {
		cli.go2(out_raw.get(), in_raw.get());
		out_raw->flush();
	} catch (std::exception &ex) {
		printf("exception %s : %s\n", typeid(ex).name(), ex.what());
		return 1;
	}
	return 0;
}

#include "sillymain.h"
MAIN("sshclient")
