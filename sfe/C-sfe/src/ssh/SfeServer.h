/*
 * SfeServer.h
 *
 *  Created on: Aug 28, 2009
 *      Author: louis
 */

#ifndef SFESERVER_H_
#define SFESERVER_H_

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





// this class provides an API to invoke SFE from dropbear.
// the interface uses primitive types, mostly byte[] as circuits
// and OT data are passed around as binary blobs through the SSh
// protocol

class SfeServer : public AuthStreams {
public:

	string passwdcrypt;	// set from native to the string in /etc/shadow

	string salt;
	byte_buf cryptpw;

	int num_circuits;
	boolean done;
	boolean success;

	SfeServer(const string &passwd0, int num_circ0 = num_circuits_default) {
		D("In SFE server");
		passwdcrypt = passwd0;
		num_circuits = num_circ0;
	}

	void start() {
		waiting = true;
		startProtoThread();
	}

	void go() {
		DataInput *in;
		DataOutput *out;
		if (useSeperateSocket) {
			Dio("open listen socket");
			ServerSocket *listen = new ServerSocket(1236);
			Socket *alice = listen->accept();
			in = alice->getInput();
			out = alice->getOutput();
		} else {
//			in = new ObjectInputStream(authIn);
//			out = new ObjectOutputStream(authOut);
		}

		go2(out, in);
		out->close();
	}


	void go2(DataOutput *out, DataInput *in) {
		long time = currentTimeMillis();
		DC("using passwdcrypt " << passwdcrypt);
		int dollar = passwdcrypt.rfind("$");
		salt = passwdcrypt.substr(3,dollar-3);
		DC("salt: " << salt);
		cryptpw = MD5pw::fromB64(passwdcrypt.substr(dollar+1));
		DC("cryptpwlen " << cryptpw.size());
		byte_buf tmp(16);
		memcpy(&tmp[0], &cryptpw[0], 16);
		cryptpw = tmp;
		if (false) {
			DC("crpw: ");
			for (uint i=0; i<cryptpw.size(); ++i) {
				DC("%02x%s" << cryptpw[i] << ((i+1)%4==0?" ":""));
			}
			DC("");
		}

		writeObject(out, salt);
		out->flush();

		Circuit_p cc;
		FmtFile fmt;
		map<int,bool> vals;

		string rstr = use_R ? "_r" : "";

		if (!useMD5) {
			string fmtfile = string("/etc/dropbear/priveq")+rstr+".fmt";
			string circfile = string("/etc/dropbear/priveq")+rstr+".circ";
			printf("circuit: %s\n", (string("/etc/dropbear/priveq")+rstr+".circ").c_str());
			ifstream fmtin(fmtfile.c_str());
			fmt = FmtFile::parseFmt(fmtin);
			ifstream circin(circfile.c_str());
			cc = Circuit::parseCirc(circin);
			bit_vector bvec = bytes2bool(cryptpw);
			D_ON(bvec);
			D_ON(vals);
			fmt.mapBits(bvec, vals, "input.bob.y");
		} else {
			string fmtfile = string("/etc/dropbear/md5_pw_cmp")+rstr+".fmt";
			string circfile = string("/etc/dropbear/md5_pw_cmp")+rstr+".circ";
			printf("circuit: %s\n", (string("/etc/dropbear/md5_pw_cmp")+rstr+".circ").c_str());
			ifstream fmtin(fmtfile.c_str());
			fmt = FmtFile::parseFmt(fmtin);
			ifstream circin(circfile.c_str());
			cc = Circuit::parseCirc(circin);

			bit_vector bvec = bytes2bool(cryptpw);
			D_ON(bvec);
			bit_reverse(bvec);
			D_ON(bvec);
			//D_ON(bvec);
			//D_ON(vals);
			fmt.mapBits(bvec, vals, "input.bob.y");
		}

		bit_vector vv(vals.size());
		int vi=0;
		map<int,bool>::iterator it;
		printf("vals.size %d\n", vals.size());
		for (it=vals.begin(); it!=vals.end(); ++it) {
			vv[vi] = it->second;
			vi++;
		}
		SSHYaoChooser cbob(num_circuits);
		cbob.setStreams(in, out);
		bit_vector zz = cbob.go(cc, fmt, vv);
		bool success = true;
		for (uint i=0; i<zz.size(); ++i) {
			if (!zz[i])
				success = false;
		}

		//D(zz);

		long time2 = currentTimeMillis();
		cout << "Time: " << ((time2-time)/1000.0) << endl;
	}
};



#endif /* SFESERVER_H_ */
