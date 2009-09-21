/*
 * HKOT.cpp
 *
 *  Created on: Sep 20, 2009
 *      Author: louis
 */

#include "HKOT.h"
#include "SecureRandom.h"
#include "cryptoio.h"
#include "silly.h"

using crypto::SecureRandom;

namespace iarpa {
namespace hkot {

static inline byte_buf H(const byte_buf &x) {
	byte_buf buf = x;
	buf.resize(lgB*M/8);
	return buf;
}

static inline int getBlock(const byte_buf &k, int n) {
	switch(B) {
	case 256:
		return k[n];
	case 16:
		if (n%2 == 0)
			return (k[n/2]>>4) & 0xF;
		else
			return (k[n/2]) & 0xF;
	case 4:
		switch(n%4) {
		case 0:
			return (k[n/4]>>6) & 0x3;
		case 1:
			return (k[n/4]>>4) & 0x3;
		case 2:
			return (k[n/4]>>2) & 0x3;
		case 3:
			return (k[n/4]) & 0x3;
		}
	default:
		break;
	}
	throw ProtocolException("unsupported block size");

}
void Server::precompute() {
	SecureRandom rand;
	byte_buf buf(Beta);
	smx.resize(M);
	for (int i=0; i<M; ++i) {
		smx[i].resize(B);
		for (int j=0; j<B; ++j) {
			rand.getBytes(buf);
			// TODO: set lg_2(M) bits to zero
			// this is overkill if M<<256 and underkill if M>=256
			buf[0] = 0;
			smx[i][j] = BigInt::toPosBigInt(buf);

		}
	}
}

BigInt Server::get_value(const byte_buf &key_in) {
	const byte_buf key = H(key_in);
	BigInt sum(0);
	for (int i=0; i<M; ++i) {
		int j = getBlock(key, i);
		sum += smx[i][j];
//		std::cout << "(" << i << "," << j << "): += " <<
//			smx[i][getBlock(key, i)].toHexString() <<
//			" = " << sum.toHexString() << std::endl;
	}
	return sum;
}

using silly::misc::currentTimeMillis;

void Client::precompute(const byte_buf &key_in) {
	long time_start = currentTimeMillis();
	const byte_buf key = H(key_in);
	SecureRandom rand;
	decKey = Paillier::genKey(HKeySz);
	const PaillierEncKey encKey = decKey.encKey();
	const BigInt ZERO(0);
	const BigInt ONE(1);
	cmx.resize(M);
	for (int i=0; i<M; ++i) {
		cmx[i].resize(B);
		int block = getBlock(key, i);
		for (int j=0; j<B; ++j) {
			//std::cout << "(" << i << "," << j << ")";
			if (block == j)
				cmx[i][j] = encKey.encrypt(ONE);
			else
				cmx[i][j] = encKey.encrypt(ZERO);
		}
		fprintf(stderr, "*");
		//std::cout << std::endl;
	}
	fprintf(stderr, "\n");
	long time_end = currentTimeMillis();
	fprintf(stderr, "Precomputation done in %0.3f secs\n",
			(time_end - time_start) / 1000.0);
}

//#define DBG_OT 1

BigInt Client::online() {
	long time_start = currentTimeMillis();
	writeObject(out, decKey.encKey().n);
	writeObject(out, decKey.encKey().g);
#if DBG_OT
	writeObject(out, decKey.lambda);
	writeObject(out, decKey.u);
#endif
	writeVector(out, cmx);
	out->flush();
	BigInt value;
	readObject(in, value);
	value = decKey.decrypt(value);
	long time_end = currentTimeMillis();
	fprintf(stderr, "Online protocol done in %0.3f secs\n",
			(time_end - time_start) / 1000.0);
	return value;

}

void Server::online() {
	BigInt n, g;
	readObject(in, n);
	readObject(in, g);
#if DBG_OT
	PaillierDecKey decKey(n, g, 0, 0);
	readObject(in, decKey.lambda);
	readObject(in, decKey.u);
#endif
	PaillierEncKey enc(n, g);
	BigInt_Mtrx cmx;
	readVector(in, cmx);
	std::cout << "server recv done" << std::endl;

	BigInt sum(enc.encrypt(BigInt(0)));
	for (int i=0; i<M; ++i) {
		for (int j=0; j<B; ++j) {
			//std::cout << "(" << i << "," << j << ")";
			BigInt prod = enc.multByPlain(cmx[i][j], smx[i][j]);
			sum = enc.add(sum, prod);
#if DBG_OT
			std::cout << ": += " << decKey.decrypt(cmx[i][j]).toHexString() <<
					" * " << smx[i][j].toHexString() <<	std::endl;
			std::cout << ": += " << decKey.decrypt(prod).toHexString() <<
				" = " << decKey.decrypt(sum).toHexString() << std::endl;
#endif


		}
		fprintf(stderr, "*");
		//std::cout << std::endl;
	}
	fprintf(stderr, "\n");

	writeObject(out, sum);
	out->flush();
}

}
}

#include "silly.h"
using namespace std;
using namespace silly::misc;
using namespace silly::net;
int iarpa::hkot::test_ot(int argc, char **argv) {
	if (argc==1) {
		cout << "A or B" << endl;
		return 1;
	}

	byte key_a[] = {1,2,3,4};
	byte_buf key(key_a, key_a+4);

	if (!strcmp(argv[1], "A")) {
		Client cc;

		cout << "Key is: " << toHexString(key) << endl;
		cc.precompute(key);
		cout << "precomputed" << endl;
		Socket *s = new Socket("localhost", 1238);

		cc.setStreams(s->getInput(), s->getOutput());
		BigInt out = cc.online();
		cout << "OT value: " <<
				out.toHexString() << endl;
	} else if (!strcmp(argv[1], "B")) {
		Server ss;
		ss.precompute();
		cout << "Server should say: " <<
				ss.get_value(key).toHexString() << endl;
		ServerSocket sock(1238);
		Socket *s = sock.accept();
		ss.setStreams(s->getInput(), s->getOutput());
		ss.online();
		cout << "Expected: " <<	ss.get_value(key).toHexString() << endl;
	}
	return 0;
}

static int _main(int argc, char **argv) {
	return iarpa::hkot::test_ot(argc, argv);
}
#include "sillymain.h"
MAIN("hkottest")
