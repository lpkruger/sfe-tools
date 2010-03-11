/*
 * SHA512pw.h
 *
 *  Created on: Sep 4, 2009
 *      Author: louis
 */

#ifndef SHA512PW_H_
#define SHA512PW_H_

#include <openssl/sha.h>
#include <string>
#include "silly.h"
#include "sillytype.h"
//#define DEBUG 1
#include "sillydebug.h"
using namespace silly::misc;
/*
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
*/
class SHA512er {
	SHA512_CTX ctx;
public:
	SHA512er() {
		SHA512_Init(&ctx);
	}
	void reset() {

		SHA512_Init(&ctx);
	}
	void update(const byte_buf &buf) {
		SHA512_Update(&ctx, &buf[0], buf.size());
	}
	void update(const byte_buf &buf, int off, int len) {
		SHA512_Update(&ctx, &buf[off], len);
	}
	byte_buf digest() {
		byte_buf out(64);
		SHA512_Final(&out[0], &ctx);
		return silly_move(out);
	}

	static bit_vector prepare_SHA512_input(bit_vector inputs) {
		bit_vector inputs2(1024);
		int pad = (1023 - inputs.size()) + 896;
		if (pad<0) pad+=1024;
		pad %= 1024;
		DC("input len " << inputs.size() << "  pad " << pad);
		for (uint i=0; i<inputs.size(); ++i) {
			inputs2[i] = inputs[i];
		}
		inputs2[inputs.size()]=true;
		byte_buf len(16);
		len[15] = (byte) (inputs.size() & 0xff);
		len[14] = (byte) ((inputs.size()>>8) & 0xff);
		len[13] = (byte) ((inputs.size()>>16) & 0xff);
		len[12] = (byte) ((inputs.size()>>24) & 0xff);
		// should do 12 more...
		bit_vector len_bits = bytes2bool(len);
		for (int i=0; i<128; ++i) {
			inputs2[896+i] = len_bits[i];
		}
		return inputs2;
	}
};

//static const string b64t = "./0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";
struct SHA512pw {
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


	static vector<byte_buf> SHA512_pw_999(const byte_buf &pw, const byte_buf &salt) {
		SHA512er md;
		SHA512er alt;

		md.update(pw);
		md.update(salt);

		alt.update(pw);
		alt.update(salt);
		alt.update(pw);

		byte_buf alt_result = alt.digest();

		//print(fin);
		int cnt;
		for (cnt = pw.size(); cnt > 64; cnt -= 64)
			md.update(alt_result, 0, 64);
		md.update(alt_result, 0, cnt);

		for (cnt = pw.size(); cnt > 0; cnt >>= 1)
			if ((cnt & 1) != 0)
				md.update(alt_result, 0, 64);
			else
				md.update(pw);

		alt_result = md.digest();
		alt.reset();

		/* For every character in the password add the entire password.  */
		for (cnt = 0; cnt < pw.size(); ++cnt)
			alt.update(pw);

		/* Finish the digest.  */
		byte_buf temp_result = alt.digest();

		alt.reset();

		/* Create byte sequence P.  */
		byte_buf p_bytes(pw.size());
		int cp = 0;
		for (cnt = pw.size(); cnt >= 64; cnt -= 64) {
			memcpy(&p_bytes[cp], &temp_result[0], 64);
			cp += 64;
		}
		memcpy(&p_bytes[cp], &temp_result[0], cnt);

		/* For every character in the password add the entire password.  */
		for (cnt = 0; cnt < 16 + alt_result[0]; ++cnt)
			alt.update(salt);

		temp_result = alt.digest();

		/* Create byte sequence S.  */
		byte_buf s_bytes(salt.size());
		cp = 0;
		for (cnt = salt.size(); cnt >= 64; cnt -= 64) {
			memcpy(&s_bytes[cp], &temp_result[0], 64);
			cp += 64;
		}
		memcpy(&s_bytes[cp], &temp_result[0], cnt);

		/* Repeatedly run the collected hash value through SHA512 to burn
     CPU cycles.  */
		for (int i=0; i<4999; ++i) {
			md.reset();
			if ((i & 1)!=0) {
				md.update(p_bytes);
			} else {
				md.update(alt_result);
			}
			if ((i % 3)!=0) {
				md.update(s_bytes);
			}
			if ((i % 7)!=0) {
				md.update(p_bytes);
			}
			if ((i & 1)!=0) {
				md.update(alt_result);
			} else {
				md.update(p_bytes);
			}
			alt_result = md.digest();
		}
		vector<byte_buf> ret;
		ret.push_back(alt_result);
		ret.push_back(p_bytes);
		ret.push_back(s_bytes);
		return ret;
	}

	static string SHA512_pw(const byte_buf &pw, const byte_buf &salt) {
		SHA512er md;
		SHA512er alt;

		md.update(pw);
		md.update(salt);

		alt.update(pw);
		alt.update(salt);
		alt.update(pw);

		byte_buf alt_result = alt.digest();

		//print(fin);
		int cnt;
		for (cnt = pw.size(); cnt > 64; cnt -= 64)
			md.update(alt_result, 0, 64);
		md.update(alt_result, 0, cnt);

		for (cnt = pw.size(); cnt > 0; cnt >>= 1)
			if ((cnt & 1) != 0)
				md.update(alt_result, 0, 64);
			else
				md.update(pw);

		alt_result = md.digest();
		alt.reset();

		/* For every character in the password add the entire password.  */
		for (cnt = 0; cnt < pw.size(); ++cnt)
			alt.update(pw);

		/* Finish the digest.  */
		byte_buf temp_result = alt.digest();

		alt.reset();

		/* Create byte sequence P.  */
		byte_buf p_bytes(pw.size());
		int cp = 0;
		for (cnt = pw.size(); cnt >= 64; cnt -= 64) {
			memcpy(&p_bytes[cp], &temp_result[0], 64);
			cp += 64;
		}
		memcpy(&p_bytes[cp], &temp_result[0], cnt);

		/* For every character in the password add the entire password.  */
		for (cnt = 0; cnt < 16 + alt_result[0]; ++cnt)
			alt.update(salt);

		temp_result = alt.digest();

		/* Create byte sequence S.  */
		byte_buf s_bytes(salt.size());
		cp = 0;
		for (cnt = salt.size(); cnt >= 64; cnt -= 64) {
			memcpy(&s_bytes[cp], &temp_result[0], 64);
			cp += 64;
		}
		memcpy(&s_bytes[cp], &temp_result[0], cnt);

		/* Repeatedly run the collected hash value through SHA512 to burn
	     CPU cycles.  */
		for (int i=0; i<4999; ++i) {
			md.reset();
			if ((i & 1)!=0) {
				md.update(p_bytes);
			} else {
				md.update(alt_result);
			}
			if ((i % 3)!=0) {
				md.update(s_bytes);
			}
			if ((i % 7)!=0) {
				md.update(p_bytes);
			}
			if ((i & 1)!=0) {
				md.update(alt_result);
			} else {
				md.update(p_bytes);
			}
			alt_result = md.digest();
		}

		string sb;

		sb.append(b64_from_24bit (alt_result[0], alt_result[21], alt_result[42], 4));
		sb.append(b64_from_24bit (alt_result[22], alt_result[43], alt_result[1], 4));
		sb.append(b64_from_24bit (alt_result[44], alt_result[2], alt_result[23], 4));
		sb.append(b64_from_24bit (alt_result[3], alt_result[24], alt_result[45], 4));
		sb.append(b64_from_24bit (alt_result[25], alt_result[46], alt_result[4], 4));
		sb.append(b64_from_24bit (alt_result[47], alt_result[5], alt_result[26], 4));
		sb.append(b64_from_24bit (alt_result[6], alt_result[27], alt_result[48], 4));
		sb.append(b64_from_24bit (alt_result[28], alt_result[49], alt_result[7], 4));
		sb.append(b64_from_24bit (alt_result[50], alt_result[8], alt_result[29], 4));
		sb.append(b64_from_24bit (alt_result[9], alt_result[30], alt_result[51], 4));
		sb.append(b64_from_24bit (alt_result[31], alt_result[52], alt_result[10], 4));
		sb.append(b64_from_24bit (alt_result[53], alt_result[11], alt_result[32], 4));
		sb.append(b64_from_24bit (alt_result[12], alt_result[33], alt_result[54], 4));
		sb.append(b64_from_24bit (alt_result[34], alt_result[55], alt_result[13], 4));
		sb.append(b64_from_24bit (alt_result[56], alt_result[14], alt_result[35], 4));
		sb.append(b64_from_24bit (alt_result[15], alt_result[36], alt_result[57], 4));
		sb.append(b64_from_24bit (alt_result[37], alt_result[58], alt_result[16], 4));
		sb.append(b64_from_24bit (alt_result[59], alt_result[17], alt_result[38], 4));
		sb.append(b64_from_24bit (alt_result[18], alt_result[39], alt_result[60], 4));
		sb.append(b64_from_24bit (alt_result[40], alt_result[61], alt_result[19], 4));
		sb.append(b64_from_24bit (alt_result[62], alt_result[20], alt_result[41], 4));
		sb.append(b64_from_24bit ((byte)0, (byte)0, alt_result[63], 2));

		string salt_str(salt.begin(), salt.end());
		return string("$6$")+salt_str+"$"+sb;
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

	static string toB64(byte_buf alt_result) {
		string sb;
		sb.append(b64_from_24bit (alt_result[0], alt_result[21], alt_result[42], 4));
		sb.append(b64_from_24bit (alt_result[22], alt_result[43], alt_result[1], 4));
		sb.append(b64_from_24bit (alt_result[44], alt_result[2], alt_result[23], 4));
		sb.append(b64_from_24bit (alt_result[3], alt_result[24], alt_result[45], 4));
		sb.append(b64_from_24bit (alt_result[25], alt_result[46], alt_result[4], 4));
		sb.append(b64_from_24bit (alt_result[47], alt_result[5], alt_result[26], 4));
		sb.append(b64_from_24bit (alt_result[6], alt_result[27], alt_result[48], 4));
		sb.append(b64_from_24bit (alt_result[28], alt_result[49], alt_result[7], 4));
		sb.append(b64_from_24bit (alt_result[50], alt_result[8], alt_result[29], 4));
		sb.append(b64_from_24bit (alt_result[9], alt_result[30], alt_result[51], 4));
		sb.append(b64_from_24bit (alt_result[31], alt_result[52], alt_result[10], 4));
		sb.append(b64_from_24bit (alt_result[53], alt_result[11], alt_result[32], 4));
		sb.append(b64_from_24bit (alt_result[12], alt_result[33], alt_result[54], 4));
		sb.append(b64_from_24bit (alt_result[34], alt_result[55], alt_result[13], 4));
		sb.append(b64_from_24bit (alt_result[56], alt_result[14], alt_result[35], 4));
		sb.append(b64_from_24bit (alt_result[15], alt_result[36], alt_result[57], 4));
		sb.append(b64_from_24bit (alt_result[37], alt_result[58], alt_result[16], 4));
		sb.append(b64_from_24bit (alt_result[59], alt_result[17], alt_result[38], 4));
		sb.append(b64_from_24bit (alt_result[18], alt_result[39], alt_result[60], 4));
		sb.append(b64_from_24bit (alt_result[40], alt_result[61], alt_result[19], 4));
		sb.append(b64_from_24bit (alt_result[62], alt_result[20], alt_result[41], 4));
		sb.append(b64_from_24bit ((byte)0, (byte)0, alt_result[63], 2));
		return sb;
	}
	static byte_buf fromB64(string str) {
		byte_buf z(64);
		////////////
		int pos = 0;
		byte_buf b = b24_from_64(str.substr(pos, 4)); pos+=4;
		z[0] = b[0]; z[21] = b[1]; z[42] = b[2];
		b = b24_from_64(str.substr(pos, 4)); pos+=4;
		z[22] = b[0]; z[43] = b[1]; z[1] = b[2];
		b = b24_from_64(str.substr(pos, 4)); pos+=4;
		z[44] = b[0]; z[2] = b[1]; z[23] = b[2];
		b = b24_from_64(str.substr(pos, 4)); pos+=4;
		z[3] = b[0]; z[24] = b[1]; z[45] = b[2];
		b = b24_from_64(str.substr(pos, 4)); pos+=4;
		z[25] = b[0]; z[46] = b[1]; z[4] = b[2];
		b = b24_from_64(str.substr(pos, 4)); pos+=4;
		z[47] = b[0]; z[5] = b[1]; z[26] = b[2];
		b = b24_from_64(str.substr(pos, 4)); pos+=4;
		z[6] = b[0]; z[27] = b[1]; z[48] = b[2];
		b = b24_from_64(str.substr(pos, 4)); pos+=4;
		z[28] = b[0]; z[49] = b[1]; z[7] = b[2];
		b = b24_from_64(str.substr(pos, 4)); pos+=4;
		z[50] = b[0]; z[8] = b[1]; z[29] = b[2];
		b = b24_from_64(str.substr(pos, 4)); pos+=4;
		z[9] = b[0]; z[30] = b[1]; z[51] = b[2];
		b = b24_from_64(str.substr(pos, 4)); pos+=4;
		z[31] = b[0]; z[52] = b[1]; z[10] = b[2];
		b = b24_from_64(str.substr(pos, 4)); pos+=4;
		z[53] = b[0]; z[11] = b[1]; z[32] = b[2];
		b = b24_from_64(str.substr(pos, 4)); pos+=4;
		z[12] = b[0]; z[33] = b[1]; z[54] = b[2];
		b = b24_from_64(str.substr(pos, 4)); pos+=4;
		z[34] = b[0]; z[55] = b[1]; z[13] = b[2];
		b = b24_from_64(str.substr(pos, 4)); pos+=4;
		z[56] = b[0]; z[14] = b[1]; z[35] = b[2];
		b = b24_from_64(str.substr(pos, 4)); pos+=4;
		z[15] = b[0]; z[36] = b[1];z[57] = b[2];
		b = b24_from_64(str.substr(pos, 4)); pos+=4;
		z[37] = b[0]; z[58] = b[1]; z[16] = b[2];
		b = b24_from_64(str.substr(pos, 4)); pos+=4;
		z[59] = b[0]; z[17] = b[1]; z[38] = b[2];
		b = b24_from_64(str.substr(pos, 4)); pos+=4;
		z[18] = b[0]; z[39] = b[1]; z[60] = b[2];
		b = b24_from_64(str.substr(pos, 4)); pos+=4;
		z[40] = b[0]; z[61] = b[1]; z[19] = b[2];
		b = b24_from_64(str.substr(pos, 4)); pos+=4;
		z[62] = b[0]; z[20] = b[1]; z[41] = b[2];
		b = b24_from_64(str.substr(pos));
		z[63] = b[0];

		////////////
		return z;
	}
};


#endif /* SHA512PW_H_ */
