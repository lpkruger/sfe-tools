
/*
 * ottest.cpp
 *
 *  Created on: Aug 22, 2009
 *      Author: louis
 */

#include <vector>
#include <string>
#include "silly.h"
#include "sillysocket.h"
#include "sillymem.h"
#include "ot/PinkasNaorOT.h"

using namespace crypto::ot;
using namespace std;
using namespace silly;
using net::Socket;
using net::ServerSocket;
static int main3(int argc, char **argv) {
	BigInt n = 24;
	n = n.pow(5); // TODO
	printf("%s\n", n.toString().c_str());
	return 0;
}

//template<class T> static inline wise_ptr<T> wp(T* p) {
//	return wise_ptr<T>(p);
//}
static int _main(int argc, char **argv) {
	vector<string> args(argc-1);
	for (int i=1; i<argc; ++i) {
		args[i-1] = argv[i];
	}
	try {
		wise_ptr<Socket> s;
		wise_ptr<ServerSocket> ss;
		args.at(0);
		if (args[0] == ("A")) {
			s = wise_ptr<Socket>(new net::Socket("localhost", 5435));
		} else if (args[0] == ("B")) {
			ss = wise_ptr<ServerSocket>(new net::ServerSocket(5435));
			s = wise_ptr<Socket>(ss->accept());
		} else {
			fprintf(stderr, "Please specify A or B\n");
			return 1;
		}

		//System.out.println("Socket: " + s);

		wise_ptr<DataOutput> out(s->getOutput());
		//	ObjectOutputStream out =
		//			new ObjectOutputStream(new BufferedOutputStream
		//					(s.getOutputStream()));
		out->flush();
		wise_ptr<DataInput> in(s->getInput());
		//	ObjectInputStream in =
		//			new ObjectInputStream(new BufferedInputStream
		//					(s.getInputStream()));

		pinkasnaor::OT ot;

		if (args[0] == ("A")) {
			BigInt_Mtrx M;
			resize(M, 3, 2);
			M[0][0]=123456;
			M[0][1]=789012;
			M[1][0]=123;
			M[1][1]=789;
			M[2][0]=1003;
			M[2][1]=83784;
			printf("M.size %d\n", M.size());
			pinkasnaor::Sender sender(M, &ot);
			sender.setStreams(in.to_ptr(), out.to_ptr());
			printf("M.size %d\n", M.size());
			sender.go();
		} else if (args[0] == ("B")) {
			bit_vector ss(args.size() - 1);
			for (uint i=1; i<args.size(); ++i) {
				if (args[i]!="0" && args[i]!="1") {
					throw bad_argument(cstr_printf("Bad s: %s", args[i].c_str()));
				}
				ss[i-1] = args[i]=="1" ? true : false;
			}
			pinkasnaor::Chooser choos(ss, &ot);
			choos.setStreams(in.to_ptr(), out.to_ptr());
			BigInt_Vect val = choos.go();
			for (uint i=0; i<val.size(); ++i) {
				printf("Value %d = %s\n", i, val[i].toString().c_str());
			}
		}
	} catch (std::out_of_range) {
		fprintf(stderr, "ottest A\n  or\nottest B bit1 bit2 bit3\n");
		return 1;
	}
	return 0;
}

#include "sillymain.h"
MAIN("ottest");
