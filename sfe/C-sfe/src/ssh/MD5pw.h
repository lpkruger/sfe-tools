/*
 * MD5pw.h
 *
 *  Created on: Sep 4, 2009
 *      Author: louis
 */

#ifndef MD5PW_H_
#define MD5PW_H_

#include <openssl/md5.h>
#include <string>
#include "silly.h"
#include "sillytype.h"
//#define DEBUG 1
#include "sillydebug.h"
using namespace silly::misc;

static string testsalt_str("QyenZBsY");
static string magic_str("$1$");
static byte_buf testsalt(testsalt_str.begin(), testsalt_str.end());
static byte_buf magic(magic_str.begin(), magic_str.end());

static void bit_reverse(bit_vector &g) {
	for (uint i=0; i<g.size()/2; ++i) {
		boolean tmp = g[i];
		g[i] = g[g.size()-1-i];
		g[g.size()-1-i] = tmp;
	}
}
static bit_vector bytes2bool(const byte_buf &inputs) {
	bit_vector bb(inputs.size() * 8);
	for (uint i=0; i<inputs.size(); ++i) {
		int b = inputs[i] & 0xff;
		bb[0+8*i] = (b & 0x80)!=0;
		bb[1+8*i] = (b & 0x40)!=0;
		bb[2+8*i] = (b & 0x20)!=0;
		bb[3+8*i] = (b & 0x10)!=0;
		bb[4+8*i] = (b & 0x08)!=0;
		bb[5+8*i] = (b & 0x04)!=0;
		bb[6+8*i] = (b & 0x02)!=0;
		bb[7+8*i] = (b & 0x01)!=0;
	}
	return bb;
}

class MD5er {
	MD5_CTX ctx;
public:
	MD5er() {
		MD5_Init(&ctx);
	}
	void reset() {
		MD5_Init(&ctx);
	}
	void update(const byte_buf &buf) {
		MD5_Update(&ctx, &buf[0], buf.size());
	}
	byte_buf digest() {
		byte_buf out(16);
		MD5_Final(&out[0], &ctx);
		return silly_move(out);
	}

	static bit_vector prepare_md5_input(bit_vector inputs) {
		bit_vector inputs2(512);
		int pad = (511 - inputs.size()) + 448;
		if (pad<0) pad+=512;
		pad %= 512;
		DC("input len " << inputs.size() << "  pad " << pad);
		for (uint i=0; i<inputs.size(); ++i) {
			inputs2[i] = inputs[i];
		}
		inputs2[inputs.size()]=true;
		byte_buf len(8);
		len[0] = (byte) (inputs.size() & 0xff);
		len[1] = (byte) ((inputs.size()>>8) & 0xff);
		len[2] = (byte) ((inputs.size()>>16) & 0xff);
		len[3] = (byte) ((inputs.size()>>24) & 0xff);
		// should do 4 more...
		bit_vector len_bits = bytes2bool(len);
		for (int i=0; i<64; ++i) {
			inputs2[448+i] = len_bits[i];
		}
		return inputs2;
	}
};

static const string b64t = "./0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";
struct MD5pw {
	static byte_buf b24_from_64(const string &s) {
		int N = s.length();
		int w = 0;
		int r = 0;
		for (int i=0; i<N; ++i) {
			w |= (b64t.find(s.at(i)) << r);
			r += 6;
		}
		if (N==4) {
			byte_buf z(3);
			z[0] = (byte) ((w>>16)&0xff);
			z[1] = (byte) ((w>>8)&0xff);
			z[2] = (byte) ((w)&0xff);
			return z;
		}
		if (N==2) {
			byte_buf z(1);
			z[0] = (byte) w;
			return z;
		}
		return byte_buf();
	}

	static string b64_from_24bit(byte BB2, byte BB1, byte BB0, int N) {
		int B0 = ((int)BB0)&0xff;
		int B1 = ((int)BB1)&0xff;
		int B2 = ((int)BB2)&0xff;
		string sb;
		int w = ((B2) << 16) | ((B1) << 8) | (B0);
		int n = (N);
		while (n-- > 0) {
			sb.push_back(b64t.at(w & 0x3f));
			w >>= 6;
		}
		return sb;
	}


	static bit_vector concat(const vector<bit_vector> &x) {
		bit_vector y;
		for (uint i=0; i<x.size(); ++i) {
			y.insert(y.end(), x[i].begin(), x[i].end());
		}
		return silly_move(y);
	}

	static byte_buf md5_pw_999(const byte_buf &pw, const byte_buf &salt) {
		MD5er md;
		MD5er finmd;

		md.update(pw);
		md.update(magic);
		md.update(salt);


		finmd.update(pw);
		finmd.update(salt);
		finmd.update(pw);
		byte_buf fin = finmd.digest();

		//print(fin);

		for (int pl = pw.size(); pl>0; pl-=16) {
			byte_buf z(pl>16 ? 16 : pl);
			memcpy(&z[0], &fin[0], z.size());
			md.update(z);
		}

		// weird thing 1
		byte_buf con0(1);
		byte_buf pw0(1, pw[0]);
		for (uint i = pw.size(); i>0; i >>= 1) {
			 md.update((i & 1)==1 ? con0 : pw0);
		}

		fin = md.digest();

		// do 1000 MD5s, minus 1
		for (int i=0; i<999; ++i) {
			md.reset();
			if ((i & 1)!=0) {
				md.update(pw);
			} else {
				md.update(fin);
			}
			if ((i % 3)!=0) {
				md.update(salt);
			}
			if ((i % 7)!=0) {
				md.update(pw);
			}
			if ((i & 1)!=0) {
				md.update(fin);
			} else {
				md.update(pw);
			}
			fin=md.digest();
		}

		return fin;
	}

	static string md5_pw(const byte_buf &pw, const byte_buf &salt) {
		MD5er md;
		MD5er finmd;

		md.update(pw);
		md.update(magic);
		md.update(salt);


		finmd.update(pw);
		finmd.update(salt);
		finmd.update(pw);
		byte_buf fin = finmd.digest();

		//print(fin);

		for (int pl = pw.size(); pl>0; pl-=16) {
			byte_buf z(pl>16 ? 16 : pl);
			memcpy(&z[0], &fin[0], z.size());
			md.update(z);
		}

		// weird thing 1
		byte_buf con0(1);
		byte_buf pw0(1, pw[0]);
		for (uint i = pw.size(); i>0; i >>= 1) {
			md.update((i & 1)==1 ? con0 : pw0);
		}

		fin = md.digest();


		// do 1000 MD5s
		for (int i=0; i<1000; ++i) {
			md.reset();
			if ((i & 1)!=0) {
				md.update(pw);
			} else {
				md.update(fin);
			}
			if ((i % 3)!=0) {
				md.update(salt);
			}
			if ((i % 7)!=0) {
				md.update(pw);
			}
			if ((i & 1)!=0) {
				md.update(fin);
			} else {
				md.update(pw);
			}
			fin=md.digest();
		}

		string sb;
		sb += (b64_from_24bit (fin[0], fin[6], fin[12], 4));
		sb.append(b64_from_24bit (fin[1], fin[7], fin[13], 4));
		sb.append(b64_from_24bit (fin[2], fin[8], fin[14], 4));
		sb.append(b64_from_24bit (fin[3], fin[9], fin[15], 4));
		sb.append(b64_from_24bit (fin[4], fin[10], fin[5], 4));
		sb.append(b64_from_24bit ((byte)0, (byte)0, fin[11], 2));

		string salt_str(salt.begin(), salt.end());
		if (true) return magic_str+salt_str+"$"+sb;
#if 0
		String epw = sb.toString().replace('.', '+');	// convert to "standard" base64
		System.out.println(sb);
		epw += "A=";
		System.out.println(epw);

		byte[] epwbytes = Base64.decode(epw);
		System.out.println(epwbytes.length);
		byte[] zz = new byte[16];
		System.arraycopy(epwbytes,0,zz,0,zz.length);

		System.out.println(Base64.encodeBytes(epwbytes));
		System.out.println(Base64.encodeBytes(zz));
		for (int i=0; i<epwbytes.length; ++i) {
			System.out.printf("%02x ",epwbytes[i]);
		}
		System.out.println();
		zz=Base64.decode(Base64.encodeBytes(zz));
		for (int i=0; i<zz.length; ++i) {
			System.out.printf("%02x ",zz[i]);
		}
		System.out.println();

		return null;
#endif
	}

	static string toB64(byte_buf fin) {
		string sb;
		sb.append(b64_from_24bit (fin[0], fin[6], fin[12], 4));
		sb.append(b64_from_24bit (fin[1], fin[7], fin[13], 4));
		sb.append(b64_from_24bit (fin[2], fin[8], fin[14], 4));
		sb.append(b64_from_24bit (fin[3], fin[9], fin[15], 4));
		sb.append(b64_from_24bit (fin[4], fin[10], fin[5], 4));
		sb.append(b64_from_24bit ((byte)0, (byte)0, fin[11], 2));
		return sb;
	}
	static byte_buf fromB64(string str) {
		byte_buf z(16);
		byte_buf b = b24_from_64(str.substr(0, 4));
		z[0] = b[0]; z[6] = b[1]; z[12] = b[2];
		b = b24_from_64(str.substr(4, 4));
		z[1] = b[0]; z[7] = b[1]; z[13] = b[2];
		b = b24_from_64(str.substr(8, 4));
		z[2] = b[0]; z[8] = b[1]; z[14] = b[2];
		b = b24_from_64(str.substr(12, 4));
		z[3] = b[0]; z[9] = b[1]; z[15] = b[2];
		b = b24_from_64(str.substr(16, 4));
		z[4] = b[0]; z[10] = b[1]; z[5] = b[2];
		b = b24_from_64(str.substr(20));
		z[11] = b[0];
		return z;
	}
};


#endif /* MD5PW_H_ */
