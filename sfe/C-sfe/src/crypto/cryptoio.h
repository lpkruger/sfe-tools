/*
 * cryptoio.h
 *
 *  Created on: Sep 20, 2009
 *      Author: louis
 */

#ifndef CRYPTOIO_H_
#define CRYPTOIO_H_

#include <sillyio.h>
#include <bigint.h>

inline void writeObject(silly::io::DataOutput *out,
		const silly::bigint::BigInt &a) {
	byte_buf buf = silly::bigint::BigInt::MPIfromBigInt(a);
	out->write(buf);
}
inline void readObject(silly::io::DataInput *in,
		silly::bigint::BigInt &a) {
	int len = in->readInt();
	byte_buf buf(len+4);
	*reinterpret_cast<int*>(&buf[0]) = ntohl(len);
	in->readFully(&buf[4], len);
	//D(buf);
	a = silly::bigint::BigInt::MPItoBigInt(buf);
}

inline std::ostream& operator<<(std::ostream &out,
		const silly::bigint::BigInt& num) {
	out << num.toString();
	return out;
}


#endif /* CRYPTOIO_H_ */
