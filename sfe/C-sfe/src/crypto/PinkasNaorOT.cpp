/*
 * PinkasNaorOT.cpp
 *
 *  Created on: Aug 22, 2009
 *      Author: louis
 */

#include "PinkasNaorOT.h"
#include <openssl/sha.h>

#undef DEBUG
#include "sillydebug.h"

PinkasNaorOT::PinkasNaorOT() {

	QQQ = BigInt(2).pow(128).nextProbablePrime();
			//BigInt::genPrime(129);
	GGG = findGenerator(QQQ);
}

PinkasNaorOT::~PinkasNaorOT() {

}

BigInt PinkasNaorOT::findGenerator(const BigInt &p) {
	// p should be prime
	BigInt k = p-1;
	BigInt x = 1;

	BigInt ZERO(0);

	while ((k%2) == 0) {
		k >>= 1;
		x <<= 1;
	}

	for (uint i=3; i<10001; i+=2) {
		while (k%i == 0) {
			k /= i;
			x *= i;
		}
	}

	return BigInt(2).modPowThis(x, p);
}

BigInt PinkasNaorOT::hash(const BigInt &p) {
	byte_buf md(20);
	byte_buf in = BigInt::fromPosBigInt(p);
	SHA1((const uchar*)(&in[0]), in.size(), (uchar*)(&md[0]));
	D("HASH:");
	D(in);
	D(md);
	return BigInt::toPosBigInt(md);
}

#if 1
inline void writeObject(DataOutput *out, const BigInt &a) {
	byte_buf buf = BigInt::MPIfromBigInt(a);
	out->write(buf);
}
inline void readObject(DataInput *in, BigInt &a) {
	int len = in->readInt();
	byte_buf buf(len+4);
	*reinterpret_cast<int*>(&buf[0]) = ntohl(len);
	in->readFully(&buf[4], len);
	//D(buf);
	a = BigInt::MPItoBigInt(buf);
}
#endif


void OTSender::go() {
	precalc();
	online();
}

void OTSender::precalc() {
	BigInt &g = ot->GGG;
	BigInt &q = ot->QQQ;
	resize(C, M.size());
	resize(rr, M.size());
	resize(E, M.size(), 2, 2);
	for (uint i=0; i<M.size(); ++i) {
		C[i] = BigInt::random(q);
		rr[i] = BigInt::random(q);
		E[i][0][0] = g.modPow(rr[i], q);
		E[i][1][0] = E[i][0][0];
	}
	D("C:");
	D(C);
	D("rr:");
	D(rr);
	resize(PK,M.size(),2);
}
void OTSender::online() {
	BigInt &q = ot->QQQ;
	D("send C");
	writeVector(out, C);
	out->flush();

	D("read PK0");
	BigInt_Vect PKM0;
	readVector(in, PKM0);
	D("PKM0:");
	D(PKM0);
	// CHECK PKM0.length == M.length
	if (PKM0.size() != M.size()) {
		fprintf(stderr, "PKM0.size(%d) != M.size(%d)\n", PKM0.size(), M.size());
		throw ProtocolException(
				cstr_printf("PKM0.size(%d) != M.size(%d)\n", PKM0.size(), M.size()));
	}

	for (uint i=0; i<M.size(); ++i) {
		PK[i][0] = PKM0[i];
		PK[i][1] = C[i].modDivide(PK[i][0], q);
	}
	D("PK:");
	D(PK);
	for (uint i=0; i<M.size(); ++i) {
		for (int j=0; j<=1; ++j) {
			E[i][j][1] = PinkasNaorOT::hash(PK[i][j].modPow(rr[i], q)).xxor(M[i][j]);
			//D("PK[" + i + "," + j + "] = " + PK[i][j].modPow(r, q));
		}
	}
	D("E:");
	D(E);
	D("send E");
	writeVector(out, E);
	out->flush();
}

BigInt_Vect OTChooser::go() {
	precalc();
	return online();
}
void OTChooser::precalc() {
	BigInt &g = ot->GGG;
	BigInt &q = ot->QQQ;
	resize(k, s.size());
	resize(PK, s.size(), 2);
	BigInt ZERO(0);
	//fprintf(stderr, "g is %s\n", g.toString().c_str());
	//fprintf(stderr, "q is %s\n", q.toString().c_str());
	for (uint i=0; i<s.size(); ++i) {
		do {
			k[i] = BigInt::random(q);
			//fprintf(stderr, "k[%d] = %s\n", i, k[i].toString().c_str());
		} while (k[i] == ZERO);
		PK[i][s[i]] = g.modPow(k[i], q);
		PK[i][1-s[i]] = PK[i][s[i]].modInverse(q);
	}
}

BigInt_Vect OTChooser::online() {
	//BigInt &g = ot->GGG;
	BigInt &q = ot->QQQ;
	D("read C");
	BigInt_Vect C;
	readVector(in, C);
	D("C:");
	D(C);

	// CHECK C.length = s.length
	if (C.size() != s.size()) {
		throw ProtocolException(
				cstr_printf("C.size(%d) != s.size(%d)\n", C.size(), s.size()));
	}
	BigInt_Vect PKS0(s.size());
	for (uint i=0; i<s.size(); ++i) {
		PK[i][1-s[i]] = C[i].modMultiply(PK[i][1-s[i]], q);
		PKS0[i] = PK[i][0];
	}
	D("send PK0");
	D("PK, PKS0:");
	D(PK);
	D(PKS0);
	writeVector(out, PKS0);
	out->flush();
	BigInt_Cube E;
	D("read E");
	readVector(in, E);
	D(E);

	//D("E = <" + E[s][0] + ", " + E[s][1]);
	//D("grk = " + E[s][0].modPow(k, q));

	BigInt_Vect ret(s.size());
	for (uint i=0; i<s.size(); ++i) {
		ret[i] = PinkasNaorOT::hash(E[i][s[i]][0].modPow(k[i], q)).xxor(E[i][s[i]][1]);
	}
	return ret;
}


