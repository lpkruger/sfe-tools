/*
 * HKOT.cpp
 *
 *  Created on: Sep 20, 2009
 *      Author: louis
 */

#include "HKOT.h"
#include "random.h"
#include "../cryptoio.h"
#include "silly.h"
#include "sillythread.h"

using crypto::SecureRandom;

using namespace silly::misc;
using namespace silly::thread;

namespace crypto {
namespace ot {
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
void Sender::precompute() {
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

BigInt Sender::get_value(const byte_buf &key_in) {
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

class ChooserCrypter : public Runnable {
	//NOCOPY(Crypter);
	const PaillierEncKey &encKey;
	CBigInt &ZERO;
	CBigInt &ONE;
	BigInt_Vect &cmx_i;
	int block;
public:
	ChooserCrypter(const PaillierEncKey &e, CBigInt &zero, CBigInt &one,
			BigInt_Vect &cmx0, int bl0) :
		encKey(e), ZERO(zero), ONE(one), cmx_i(cmx0), block(bl0) {}

	void* run() {
		for (int j=0; j<B; ++j) {
			if (block == j)
				cmx_i[j] = encKey.encrypt(ONE);
			else
				cmx_i[j] = encKey.encrypt(ZERO);
		}
		fprintf(stderr, "*");
		return NULL;
	}
};


void Chooser::precompute(const byte_buf &key_in) {
	long time_start = currentTimeMillis();
	const byte_buf key = H(key_in);
	SecureRandom rand;
	decKey = Paillier::genKey(HKeySz);
	const PaillierEncKey encKey = decKey.encKey();
	const BigInt ZERO(0);
	const BigInt ONE(1);
	cmx.resize(M);
	ThreadPool pool(numCPUs()*2);
	ChooserCrypter *tasks[M];
	for (int i=0; i<M; ++i) {
		cmx[i].resize(B);
		int block = getBlock(key, i);
		tasks[i] = new ChooserCrypter(encKey, ZERO, ONE, cmx[i], block);
		pool.submit(tasks[i]);
		//std::cout << std::endl;
	}
	pool.stopWait();
	for (int i=0; i<M; ++i) {
//		std::cout << "(" << i << ",0) = " <<
//			cmx[i][0].toHexString() << std::endl;
		delete tasks[i];
	}
	fprintf(stderr, "\n");
	long time_end = currentTimeMillis();
	fprintf(stderr, "Precomputation done in %0.3f secs\n",
			(time_end - time_start) / 1000.0);
}


//#define DBG_OT 1

BigInt Chooser::online() {
 	ulong out_start = out->total;
 	ulong in_start = in->total;
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
	fprintf(stderr, "Wrote %lu bytes   Read %lu bytes\n",
			out->total-out_start, in->total-in_start);
	return value;

}

class SenderSummer : public Runnable {
	//NOCOPY(SenderSummer);
	const PaillierEncKey &enc;
	const BigInt_Vect &cmx_i;
	const BigInt_Vect &smx_i;
public:
	BigInt sum;

	SenderSummer(const PaillierEncKey &e,
			const BigInt_Vect &cmx0, const BigInt_Vect &smx0 ) :
		enc(e), cmx_i(cmx0), smx_i(smx0) {}

	void* run() {
		for (int j=0; j<B; ++j) {
			//std::cout << "(" << i << "," << j << ")";
			BigInt prod = enc.multByPlain(cmx_i[j], smx_i[j]);
			sum = j==0 ? prod :
					enc.add(sum, prod);
		}
		fprintf(stderr, "*");
		return NULL;
	}
};

void Sender::online() {
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

	ThreadPool pool(numCPUs()*2);
	SenderSummer *tasks[M];

	for (int i=0; i<M; ++i) {
		tasks[i] = new SenderSummer(enc, cmx[i], smx[i]);
		pool.submit(tasks[i]);
	}
	BigInt sum;
	pool.stopWait();
	for (int i=0; i<M; ++i) {
		sum = i==0 ? tasks[i]->sum :
				enc.add(sum, tasks[i]->sum);
#if DBG_OT
		std::cout << i << ": += " <<
		decKey.decrypt(tasks[i]->sum).toHexString() <<
		" = " << decKey.decrypt(sum).toHexString() << std::endl;
#endif

		delete tasks[i];
	}

	fprintf(stderr, "\n");

	writeObject(out, sum);
	out->flush();
}

}
}
}

#include "silly.h"
using namespace std;
using namespace silly::misc;
using namespace silly::net;
int crypto::ot::hkot::test_ot(int argc, char **argv) {
	if (argc==1) {
		cout << "A or B" << endl;
		return 1;
	}

	byte key_a[] = {1,2,3,4};
	byte_buf key(key_a, key_a+4);

	if (!strcmp(argv[1], "A")) {
		Chooser cc;

		cout << "Key is: " << toHexString(key) << endl;
		cc.precompute(key);
		//cout << "precomputed" << endl;
		Socket *s = new Socket("localhost", 1238);

		cc.setStreams(s->getInput(), s->getOutput());
		BigInt out = cc.online();
		cout << "OT value: " <<
				out.toHexString() << endl;
	} else if (!strcmp(argv[1], "B")) {
		Sender ss;
		ss.precompute();
		cout << "Sender should say: " <<
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
	return crypto::ot::hkot::test_ot(argc, argv);
}
#include "sillymain.h"
MAIN("hkottest")
