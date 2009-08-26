/*
 * KO.cpp
 *
 *  Created on: Aug 6, 2009
 *      Author: louis
 */

//#undef _GLIBCXX_DEBUG
#include <iostream>

#include "KO.h"
#include <string.h>
#include <openssl/rc4.h>
#include "sillyio.h"

using namespace std;
using namespace silly::io;
//#define D(X) X  // to enable debugging msgs
#define D(X)

static void writeObject(DataOutput *out, const BigInt &a) {
	vector<byte> buf = a.toMPIByteArray();
	out->write(buf);
}
static void readObject(DataInput *in, BigInt &a) {
	int len = in->readInt();
	vector<byte> buf(len+4);
	*reinterpret_cast<int*>(&buf[0]) = ntohl(len);
	in->readFully(&buf[4], len);
	//D(buf);
	a = BigInt::fromMPIByteArray(buf);
}

BigInt iarpa::ko::Client::clientxfer1(CBigInt &w) {
	BN_rand(rr.writePtr(), BN_num_bits(rsa_n.ptr()), 0, 0);
	BigInt Y = rr.modPow(rsa_e, rsa_n);
	Y.modMultiplyThis(H(w), rsa_n);
	return Y;
}

void iarpa::ko::Client::clientxfer2(CBigInt &w, CBigInt &X, const vector<vector<byte> > &mhat) {
	BigInt wwhat = X.modDivide(rr, rsa_n);
	D(printf("C: %s %s %s\n", wwhat.toHexString().c_str(), rr.toHexString().c_str(), X.toHexString().c_str());)

	BigInt ii;
	for (uint i=0; i<mhat.size(); ++i) {
		ii = i;
		BNcPtr vals[3] = { w, wwhat, ii };
		vector<BNcPtr> input(vals, vals+3);

		D(printf("C: %s %s %02x\n", w.toHexString().c_str(), wwhat.toHexString().c_str(), i);)
		vector<byte> mi = Gxor(input, mhat[i]);

		for (int j=0; j<L; ++j) {
			// test for leading 0s
			if (mi[j]!=0)
				goto search;
		}

		for (uint j=0; j<mi.size(); ++j) {
			printf("%02x ", mi[j]);
		}
		printf("\n");

		search:
		do {} while(0);
	}
	printf("All done\n");
}

void iarpa::ko::Client::online(CBigInt &w) {
	readObject(in, rsa_n);
	readObject(in, rsa_e);
	BigInt Y = clientxfer1(w);
	writeObject(out, Y);
	BigInt X;
	vector<vector<byte> > mhat;
	readObject(in, X);
	readVector(in, mhat);
	clientxfer2(w, X, mhat);
}

void iarpa::ko::Server::servercommit(DDB & ddb) {
	rsa = RSA_generate_key(1024, 17, NULL, NULL);
	int dbsize = ddb.thedb.size();
	vector<BigInt> what(dbsize);
	mhat.resize(dbsize);
	int i=0;
	BigInt ii;
	map<BigInt,BigInt>::iterator it;
	for (it = ddb.thedb.begin(); it != ddb.thedb.end(); ++it) {
		const BigInt &w = it->first;
		const BigInt &m = it->second;
		//printf("A: %s %s\n",BN_bn2dec(w),BN_bn2dec(m));

		what[i] = H(w).modPow(rsa->d, rsa->n);
		int mlen = BN_num_bytes(m.ptr());

		vector<byte> mm(mlen+L, 0);
		BN_bn2bin(m.ptr(), &mm[L]);

		ii = i;
		BNcPtr vals[3] = {w, what[i], ii};
		vector<BNcPtr> input(vals, vals+3);
		D(printf("S: %s %s %02x\n", w.toHexString().c_str(), what[i].toHexString().c_str(), i);)
		mhat[i] = Gxor(input, mm);
		++i;
	}
}
void iarpa::ko::Server::precompute(DDB &ddb) {
	servercommit(ddb);
}

BigInt iarpa::ko::Server::serverxfer(CBigInt &Y) {
	return Y.modPow(rsa->d, rsa->n);
}

void iarpa::ko::Server::online() {
	writeObject(out, BigInt(rsa->n));
	writeObject(out, BigInt(rsa->e));
	BigInt Y;
	readObject(in, Y);
	BigInt X = serverxfer(Y);
	writeObject(out, X);
	writeVector(out, mhat);
}

BigInt iarpa::ko::H(BNcPtr x) {
	byte md[SHA_DIGEST_LENGTH];
	byte buf[BN_num_bytes(x)];
	BN_bn2bin(x, buf);
	SHA1(buf, BN_num_bytes(x), md);
	BigInt hh;
	BN_bin2bn(md, SHA_DIGEST_LENGTH, hh.writePtr());
	return hh;
}


vector<byte> iarpa::ko::Gxor(const vector<BNcPtr> &x, const vector<byte> &m) {
	vector<byte> zz;

	int totalsize=0;
	int zlen;
	for (uint i=0; i<x.size(); ++i) {
		zlen = BN_num_bytes(x[i].p);
		if(!zlen) ++zlen;
		totalsize+= zlen;
		//printf("x[%d]/%d total = %d\n", i, zlen, totalsize);
	}

	zz.resize(totalsize, 0);
	int count=0;
	for (uint i=0; i<x.size(); ++i) {
		zlen = BN_num_bytes(x[i]);
		if(!zlen) ++zlen;
		//zz.at(count);
		//printf("x[%d] = %d @ %d\n", i, zlen, count);
		BN_bn2bin(x[i], &zz[count]);
		count += zlen;
	}

	// use RC4 as PRNG
	vector<byte> out(m.size());
	RC4_KEY key;
	RC4_set_key(&key, zz.size(), &zz[0]);
	RC4(&key, m.size(), &m[0], &out[0]);
	return out;
}

#if 1  // disable to remove test code

#include "sillyio.h"
#include <stdexcept>
using silly::net::ServerSocket;
using silly::net::Socket;

extern int iarpa_ko_populate_test_db(iarpa::DDB &ddb, int size);

static int _main(int argc, char **argv) {
	vector<string> args(argc-1);
	for (int i=1; i<argc; ++i) {
		args[i-1] = argv[i];
	}
	try {
		Socket *s;
		args.at(0);
		if (args[0] == ("A")) {
			args.at(1);
			s = new Socket("localhost", 5436);
			DataOutput *out = s->getOutput();
			DataInput *in = s->getInput();
			iarpa::ko::Client cli;
			cli.setStreams(in, out);
			cli.online(BigInt(atoi(args[1].c_str())));
		} else if (args[0] == ("B")) {
			args.at(1);
			iarpa::DDB ddb;
			iarpa_ko_populate_test_db(ddb, atoi(args[1].c_str()));
			iarpa::ko::Server serv;
			serv.precompute(ddb);

			ServerSocket *ss = new ServerSocket(5436);
			s = ss->accept();
			DataOutput *out = s->getOutput();
			DataInput *in = s->getInput();
			serv.setStreams(in, out);
			serv.online();
		} else {
			fprintf(stderr, "Please specify A or B\n");
			return 1;
		}
		return 0;
	} catch (out_of_range) {
		fprintf(stderr, "koproto A key_num (client)\n  or\nkoproto B dbsize (server)\n");
		return 1;
	}
}

#include "sillymain.h"
MAIN("koproto");

#endif
