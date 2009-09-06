/*
 * SfeClient.h
 *
 *  Created on: Sep 4, 2009
 *      Author: louis
 */

#ifndef SFECLIENT_H_
#define SFECLIENT_H_

// this class provides an API to invoke SFE from dropbear.
// the interface uses primitive types, mostly byte[] as circuits
// and OT data are passed around as binary blobs through the SSh
// protocol


#include "AuthStreams.h"
#include <string>
#include "sillysocket.h"
#include "silly.h"
#include "../shdl/shdl.h"
#include "SSH_Yao.h"
#include "MD5pw.h"

using namespace silly::net;
using namespace silly::misc;
using namespace std_obj_rw;
using namespace shdl;

class SfeClient : public AuthStreams {

	//public final static String dir="/home/louis/sfe/build"

	string password;
public:
	SfeClient(string passw0) {
		password = passw0;
	}

	string salt;

	void start() {
		Dio("start");
		waitingForIO = true;
		if (useSeperateSocket) {
			waitingForIO = true;
		}
		startProtoThread();
		DC("returning from start()");
	}

	void go() {
		DataInput *in;
		DataOutput *out;
		Dio("go");
		if (useSeperateSocket) {
			Dio("connect to socket");
			usleep(200000);
			Socket *bob = new Socket("localhost", 1236);

			in = bob->getInput();
			out = bob->getOutput();
			out->flush();
		} else {
			out = authOut;
			out->flush();
			in = authIn;

//			out = new ObjectOutputStream(authOut);
//			out.flush();
//			in = new ObjectInputStream(authIn);
		}

		go2(out, in);
		out->close();
	}

	void go2(DataOutput *out, DataInput *in) {
		if (password.empty()) {
			fprintf(stderr, "empty password\n");
			failure_flag = true;
			done_flag = true;
			return;
		}
		// pre-crypt
		DC("pre-crypt circuit");
		Circuit_p cc;
		FmtFile fmt;
		string rstr(use_R ? "_r" : "");

		// for simple eq test:

		if (!useMD5) {
			string fmtfile = string("/etc/dropbear/priveq")+rstr+".fmt";
			string circfile = string("/etc/dropbear/priveq")+rstr+".circ";
			fprintf(stderr, "circuit: %s\n", (string("/etc/dropbear/priveq")+rstr+".circ").c_str());
			ifstream fmtin(fmtfile.c_str());
			fmt = FmtFile::parseFmt(fmtin);
			ifstream circin(circfile.c_str());
			cc = Circuit::parseCirc(circin);

		} else {
			string fmtfile = string("/etc/dropbear/md5_pw_cmp")+rstr+".fmt";
			string circfile = string("/etc/dropbear/md5_pw_cmp")+rstr+".circ";
			fprintf(stderr, "circuit: %s\n", (string("/etc/dropbear/md5_pw_cmp")+rstr+".circ").c_str());
			ifstream fmtin(fmtfile.c_str());
			fmt = FmtFile::parseFmt(fmtin);
			ifstream circin(circfile.c_str());
			cc = Circuit::parseCirc(circin);
		}

//		FmtFile::VarDesc bdv = fmt.getVarDesc();
//		FmtFile::VarDesc aliceVars = bdv.filter("A");
//		FmtFile::VarDesc bobVars = bdv.filter("B");


		long time = currentTimeMillis();

		/*
		MD5 md5 = new MD5();
		cc = md5.generate();
		TreeSet<Integer> aliceVars = new TreeSet<Integer>();
		TreeSet<Integer> bobVars = new TreeSet<Integer>();
		*/

		DC("start protocol");
		readObject(in, salt);
		DC("salt = " << salt);

		byte_buf cryptpw;
		byte_buf password_buf(password.begin(), password.end());
		byte_buf salt_buf(salt.begin(), salt.end());
		// for simple eq test:
		if (!useMD5) {
			string cryptpwstr = MD5pw::md5_pw(password_buf, salt_buf);
			DC(cryptpwstr);
			int dollar = cryptpwstr.rfind("$");
			cryptpwstr = cryptpwstr.substr(1 + dollar);
			cryptpw = MD5pw::fromB64(cryptpwstr);
		} else {
			cryptpw = MD5pw::md5_pw_999(password_buf, salt_buf);
			DC("pre-hash: " << MD5pw::toB64(cryptpw));
			D(bytes2bool(cryptpw));
		}

		//D("using password "+password);
		DC("prepare inputs");
//		try {
			MD5er md5;
			md5.update(password_buf);
			md5.update(password_buf);
			md5.update(cryptpw);
			byte_buf fin = md5.digest();
			DC("fin: ");
			//for (int i=0; i<fin.length; ++i) {
				//System.out.printf("%02x%s", fin[i], (i+1)%4==0?" ":"");
			//}
			DC(MD5pw::toB64(fin));
			D(bytes2bool(fin));

//		} catch (...) { throw; }

		int pwlen = password_buf.size();
		byte_buf md5in(pwlen*2 + cryptpw.size());
		memcpy(&md5in[0], &password_buf[0], pwlen);
		memcpy(&md5in[pwlen], &password_buf[0], pwlen);
		memcpy(&md5in[2*pwlen], &cryptpw[0], cryptpw.size());
		bit_vector md5inbits = MD5er::prepare_md5_input(bytes2bool(md5in));

		//System.out.println("alice size: " + aliceVars.who.size());
		//System.out.println("bob size: " + bobVars.who.size());

		map<int,bool> vals;
		if (!useMD5) {
			fmt.mapBits(bytes2bool(cryptpw), vals, "input.alice.x");
		} else {
			fmt.mapBits(md5inbits, vals, "input.alice.x");
		}
		if (use_R) {
			byte_buf rval(128/8);
			RAND_bytes(&rval[0], rval.size());
			DC("R = " << toHexString(rval));
			fmt.mapBits(bytes2bool(rval), vals, "input.alice.r");
		}


		bit_vector vv(vals.size());
		int vi=0;
		map<int,bool>::iterator it;
		for (it=vals.begin(); it!=vals.end(); ++it) {
			vv[vi] = it->second;
			vi++;
		}

		D("eval circuit");
		SSHYaoSender calice;
		calice.setStreams(in, out);
		calice.go(cc, fmt, vv);

		long time2 = currentTimeMillis();
		cout << "Time: " << ((time2-time)/1000.0) << endl;

		//System.out.println("Alice circuit wrote " + out.getCount() + " bytes");

		// for DEBUG
		//BigInteger bobVal = (BigInteger) in.readObject();
		//BigInteger combined = r0.add(bobVal).and(MAX_BIGINT);
		//System.out.println("result after stage: " + combined);
	}
};


#endif /* SFECLIENT_H_ */
