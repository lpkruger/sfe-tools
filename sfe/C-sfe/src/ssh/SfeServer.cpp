/*
 * SfeServer.cpp
 *
 *  Created on: Aug 28, 2009
 *      Author: louis
 */

#include "SfeServer.h"
#include "MD5pw.h"


extern void new_sfe_server(char*);
extern void sfe_server_receive_packet(byte*,int);
extern bool sfe_server_get_failflag();
extern bool sfe_server_get_doneflag();
extern bool sfe_server_get_success();

static SfeServer *server;

void new_sfe_server(char *pwcrypt) {
	server = new SfeServer(string(pwcrypt));
}
void sfe_server_receive_packet(byte *pkt, int len) {
	server->receivePacket(pkt, len);
}
bool sfe_server_get_failflag() {
	return server->failure_flag;
}
bool sfe_server_get_doneflag() {
	return server->done;
}
bool sfe_server_get_success() {
	return server->success;
}




static int _main(int argc, char **argv) {
	silly::mem::count_printer<byte_buf>   bytebuf_cnt;
	silly::mem::count_printer<SFEKey>  cipherkey_cnt;

	vector<string> args(argc-1);
	for (int i=1; i<argc; ++i) {
		args[i-1] = argv[i];
	}

	DataOutput *out_raw;
	DataInput *in_raw;
	string pw;
	int num_circuits;
	try {
		int port = strtol(args.at(0).c_str(), NULL, 0);
		 //pw = args.size() ==1 ? string("$1$QyenZBsY$w92OuQyOOk02pRUjZTjr20")
		num_circuits = args.size() < 2 ? AuthStreams::num_circuits_default :
			strtol(args.at(1).c_str(), NULL, 0);
		 pw = args.size() < 3 ? string("$1$G1tl1u3T$u86xxKN8OWDi.w29KF4PX.") //MD5("Q")
				: args.at(2);
		//cout << pw << endl;
		ServerSocket *listen = new ServerSocket(port);
		Socket *client_sock = listen->accept();
		//long startTime = System.currentTimeMillis();
		out_raw = client_sock->getOutput();
		in_raw = client_sock->getInput();

	} catch (std::out_of_range) {
		printf("sshserver port [numcircs] [pwcrypt]\n");
		return 1;
	}
	SfeServer serv(pw, num_circuits);
	serv.go2(out_raw, in_raw);
	return 0;


}

#include "sillymain.h"
MAIN("sshserver")

