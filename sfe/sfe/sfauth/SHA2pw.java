package sfe.sfauth;

import java.io.FileOutputStream;
import java.io.PrintStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import sfe.shdl.*;
import sfe.util.*;

public class SHA2pw {
	//static byte[] testsalt = "QyenZBsY" .getBytes();
	//static byte[] magic = "$1$".getBytes();
	static byte[] testsalt = "TxtsgpEa".getBytes();
	static byte[] magic = "$6$".getBytes();
	static void print(byte[] q) {
		System.out.println(new String(q));
	}
	
	static boolean[] concat(boolean[][] x) {
		int len=0;
		for (int i=0; i<x.length; ++i) {
			len += x[i].length;
		}
		boolean[] y = new boolean[len];
		int ind = 0;
		for (int i=0; i<x.length; ++i) {
			System.arraycopy(x[i], 0, y, ind, x[i].length);
			ind += x[i].length;
		}
		return y;
	}
	public static void main(String[] args) throws Exception {
		byte[] pw = args[0].getBytes();
		String pwcr = sha512_pw(pw, testsalt);
		System.out.println(pwcr);
		pwcr = pwcr.substring(pwcr.lastIndexOf('$')+1);
		System.out.println(pwcr);
		System.out.println(toB64(fromB64(pwcr)));
		byte[][] out999 = sha512_pw_999(pw, testsalt);
		byte[] fin = out999[0];
		byte[] p_bytes = out999[1];
		byte[] s_bytes = out999[2];
		
		/*
		MessageDigest md5 = MessageDigest.getInstance("MD5");
		md5.update(pw);
		md5.update(pw);
		md5.update(fin);
		fin = md5.digest();*/
		
		SHA2 sha2gen = new SHA2(512);
		/*
		md5gen.initialize(2);
		md5gen.update(0);
		md5gen.update(0);
		md5gen.update(1);
		Circuit cc = md5gen.digest();*/
		Circuit cc = sha2gen.generate();
		PrintStream stdout = System.out;
		
		if (args[0].equals("generate")) {
			System.out.println("Writing sha512_std.circ");
			FileOutputStream circout1 = new FileOutputStream("sha512_std.circ");
			System.setOut(new PrintStream(circout1));
			CircuitWriter.write(cc);
			circout1.close();
			System.setOut(stdout);
		}
		
		boolean[] in1 = BitUtils.bytes2bool(p_bytes);
		boolean[] in2 = BitUtils.bytes2bool(s_bytes);
		boolean[] in3 = BitUtils.bytes2bool(p_bytes);
		boolean[] in4 = BitUtils.bytes2bool(fin);
		
//		boolean[] in1 = BitUtils.bytes2bool(args[0].getBytes());
//		boolean[] in2 = BitUtils.bytes2bool(args[0].getBytes());
//		boolean[] in3 = BitUtils.bytes2bool(testsalt);
//		boolean[] in4 = BitUtils.bytes2bool(fin);
		boolean[][] inputs = { in1, in2, in3, in4 };
		fin = BitUtils.bool2bytes(new SHA2(512).compute_sha2(cc, concat(inputs)));
		System.out.println(new String(magic)+new String(testsalt)+"$"+toB64(fin));
		
		boolean use_R = false;
		cc = sha2gen.generateWithPrivEq(use_R);
		cc.outputs[0].setComment("output.bob$0");
		if (!use_R) {
			boolean[] in5 = BitUtils.bytes2bool(fin);
			//boolean[] in4 = new boolean[128];
			boolean[][] eqinputs = { new SHA2(512).prepare_sha2_input(concat(inputs)), in5 };
			//System.out.println(eqinputs[0].length + " " + eqinputs[1].length);
			boolean[] out = cc.eval(concat(eqinputs));
			System.out.println("eq: " + out[0]);
		}
		String rstr = use_R ? "_r" : "";
		
		if (args[0].equals("generate")) {
			System.out.println("Writing sha512_pw_cmp.circ");
			FileOutputStream circout = new FileOutputStream("sha512_pw_cmp"+rstr+".circ");
			System.setOut(new PrintStream(circout));
			CircuitWriter.write(cc);
			circout.close();
			System.out.println("Writing sha512_pw_cmp.fmt");
			FileOutputStream fmtout = new FileOutputStream("sha512_pw_cmp"+rstr+".fmt");
			System.setOut(new PrintStream(fmtout));
			System.out.print("Alice input integer \"input.alice.x\" [");
			for (int i=0; i<1024; ++i) {
				System.out.print(" "+i);
			}
			System.out.println(" ]");
			
			int r_bits = 0;
			if (cc.outputs.length > 1) {
				r_bits = cc.outputs.length;
				System.out.print("Alice input integer \"input.alice.r\" [");
				for (int i=0; i<r_bits; ++i) {
					System.out.print(" "+(1024+i));
				}
				System.out.println(" ]");
			}
	
			
			System.out.print("Bob input integer \"input.bob.y\" [");
			for (int i=0; i<512; ++i) {
				System.out.print(" "+(1024+r_bits+i));
			}
			System.out.println(" ]");
			
			System.out.print("Bob output integer \"output.bob\" [");
			for (int i=0; i<cc.outputs.length; ++i) {
				System.out.print(" "+ cc.outputs[i].id);
			}
			System.out.println(" ]");
		}
	}
	
	/*
	public static void md5_pw_circ() {
		MD5 md;
		MD5 finmd;
		
		int pw=0;
		int magic=1024;
		int salt=512;
		md.update(pw);
		md.update(magic);
		md.update(salt);
			
		finmd.update(pw);
		finmd.update(salt);
		finmd.update(pw);
		//byte[] fin = finmd.digest();
		int fin = 1024+512;
		
		//print(fin);
		
		for (int pl = pw.length; pl>0; pl-=16) {
			byte[] z = new byte[pl>16 ? 16 : pl];
			System.arraycopy(fin, 0, z, 0, z.length);
			md.update(z);
		}

		md.update(fin);
		
		// weird thing 1
		byte[] con0 = { 0 };
		byte[] pw0 = { pw[0] };
		for (int i = pw.length; i>0; i >>= 1) {
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

		StringBuilder sb = new StringBuilder();
		sb.append(b64_from_24bit (fin[0], fin[6], fin[12], 4));
		sb.append(b64_from_24bit (fin[1], fin[7], fin[13], 4));
		sb.append(b64_from_24bit (fin[2], fin[8], fin[14], 4));
		sb.append(b64_from_24bit (fin[3], fin[9], fin[15], 4));
		sb.append(b64_from_24bit (fin[4], fin[10], fin[5], 4));
		sb.append(b64_from_24bit ((byte)0, (byte)0, fin[11], 2));
		
		//if(true) return sb.toString();
		if (true) return new String(magic)+new String(salt)+"$"+sb;
		
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
	}
    */
	
	public static byte[][] sha512_pw_999(byte[] pw, byte[] salt) {
		MessageDigest md;
		MessageDigest alt;
		try {
			 md = MessageDigest.getInstance("SHA-512");
			 alt = MessageDigest.getInstance("SHA-512");
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace(System.err);
			throw new NullPointerException(e.getMessage());
		}
		md.update(pw);
		md.update(salt);
		
		alt.update(pw);
		alt.update(salt);
		alt.update(pw);
		
		byte[] alt_result = alt.digest();
		
		//print(fin);
		int cnt;
		for (cnt = pw.length; cnt > 64; cnt -= 64) 
			md.update(alt_result, 0, 64);
		md.update(alt_result, 0, cnt);
		
		for (cnt = pw.length; cnt > 0; cnt >>= 1)
			if ((cnt & 1) != 0)
				md.update(alt_result, 0, 64);
			else
				md.update(pw);
		
		alt_result = md.digest();
		alt.reset();
		
		/* For every character in the password add the entire password.  */
		for (cnt = 0; cnt < pw.length; ++cnt)
			alt.update(pw);

		/* Finish the digest.  */
		byte[] temp_result = alt.digest();
		
		alt.reset();

		/* Create byte sequence P.  */
		byte[] p_bytes = new byte[pw.length];
		int cp = 0;
		for (cnt = pw.length; cnt >= 64; cnt -= 64) {
			System.arraycopy(temp_result, 0, p_bytes, cp, 64);
			cp += 64;
		}
		System.arraycopy(temp_result, 0, p_bytes, cp, cnt);
		
		System.out.println(Base64.encodeBytes(p_bytes));
		
		/* For every character in the password add the entire password.  */
		for (cnt = 0; cnt < 16 + alt_result[0]; ++cnt)
			alt.update(salt);
			
		temp_result = alt.digest();

		/* Create byte sequence S.  */
		byte[] s_bytes = new byte[salt.length];
		cp = 0;
		for (cnt = salt.length; cnt >= 64; cnt -= 64) {
			System.arraycopy(temp_result, 0, s_bytes, cp, 64);
			cp += 64;
		}
		System.arraycopy(temp_result, 0, s_bytes, cp, cnt);
		
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
		return new byte[][] {alt_result, p_bytes, s_bytes};
	}
	
	public static String sha512_pw(byte[] pw, byte[] salt) {
		MessageDigest md;
		MessageDigest alt;
		try {
			 md = MessageDigest.getInstance("SHA-512");
			 alt = MessageDigest.getInstance("SHA-512");
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace(System.err);
			throw new NullPointerException(e.getMessage());
		}
		md.update(pw);
		md.update(salt);
		
		alt.update(pw);
		alt.update(salt);
		alt.update(pw);
		
		byte[] alt_result = alt.digest();
		
		//print(fin);
		int cnt;
		for (cnt = pw.length; cnt > 64; cnt -= 64) 
			md.update(alt_result, 0, 64);
		md.update(alt_result, 0, cnt);
		
		for (cnt = pw.length; cnt > 0; cnt >>= 1)
			if ((cnt & 1) != 0)
				md.update(alt_result, 0, 64);
			else
				md.update(pw);
		
		alt_result = md.digest();
		alt.reset();
		
		/* For every character in the password add the entire password.  */
		for (cnt = 0; cnt < pw.length; ++cnt)
			alt.update(pw);

		/* Finish the digest.  */
		byte[] temp_result = alt.digest();
		
		alt.reset();

		/* Create byte sequence P.  */
		byte[] p_bytes = new byte[pw.length];
		int cp = 0;
		for (cnt = pw.length; cnt >= 64; cnt -= 64) {
			System.arraycopy(temp_result, 0, p_bytes, cp, 64);
			cp += 64;
		}
		System.arraycopy(temp_result, 0, p_bytes, cp, cnt);
		
		/* For every character in the password add the entire password.  */
		for (cnt = 0; cnt < 16 + alt_result[0]; ++cnt)
			alt.update(salt);
			
		temp_result = alt.digest();

		/* Create byte sequence S.  */
		byte[] s_bytes = new byte[salt.length];
		cp = 0;
		for (cnt = salt.length; cnt >= 64; cnt -= 64) {
			System.arraycopy(temp_result, 0, s_bytes, cp, 64);
			cp += 64;
		}
		System.arraycopy(temp_result, 0, s_bytes, cp, cnt);
		
		/* Repeatedly run the collected hash value through SHA512 to burn
     CPU cycles.  */
		for (int i=0; i<5000; ++i) {
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

		StringBuilder sb = new StringBuilder();
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

		//if(true) return sb.toString();
		if (true) return new String(magic)+new String(salt)+"$"+sb;
		
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
	}

	static String toB64(byte[] alt_result) {
		StringBuilder sb = new StringBuilder();
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
		return sb.toString();
	}

	static byte[] fromB64(String str) {
		byte[] z = new byte[64];
		int pos = 0;
		byte[] b = b24_from_64(str.substring(pos, pos+4)); pos+=4;
		z[0] = b[0]; z[21] = b[1]; z[42] = b[2];
		b = b24_from_64(str.substring(pos, pos+4)); pos+=4;
		z[22] = b[0]; z[43] = b[1]; z[1] = b[2];
		b = b24_from_64(str.substring(pos, pos+4)); pos+=4;
		z[44] = b[0]; z[2] = b[1]; z[23] = b[2];
		b = b24_from_64(str.substring(pos, pos+4)); pos+=4;
		z[3] = b[0]; z[24] = b[1]; z[45] = b[2];
		b = b24_from_64(str.substring(pos, pos+4)); pos+=4;
		z[25] = b[0]; z[46] = b[1]; z[4] = b[2];
		b = b24_from_64(str.substring(pos, pos+4)); pos+=4;
		z[47] = b[0]; z[5] = b[1]; z[26] = b[2];
		b = b24_from_64(str.substring(pos, pos+4)); pos+=4;
		z[6] = b[0]; z[27] = b[1]; z[48] = b[2];
		b = b24_from_64(str.substring(pos, pos+4)); pos+=4;
		z[28] = b[0]; z[49] = b[1]; z[7] = b[2];
		b = b24_from_64(str.substring(pos, pos+4)); pos+=4;
		z[50] = b[0]; z[8] = b[1]; z[29] = b[2];
		b = b24_from_64(str.substring(pos, pos+4)); pos+=4;
		z[9] = b[0]; z[30] = b[1]; z[51] = b[2];
		b = b24_from_64(str.substring(pos, pos+4)); pos+=4;
		z[31] = b[0]; z[52] = b[1]; z[10] = b[2];
		b = b24_from_64(str.substring(pos, pos+4)); pos+=4;
		z[53] = b[0]; z[11] = b[1]; z[32] = b[2];
		b = b24_from_64(str.substring(pos, pos+4)); pos+=4;
		z[12] = b[0]; z[33] = b[1]; z[54] = b[2];
		b = b24_from_64(str.substring(pos, pos+4)); pos+=4;
		z[34] = b[0]; z[55] = b[1]; z[13] = b[2];
		b = b24_from_64(str.substring(pos, pos+4)); pos+=4;
		z[56] = b[0]; z[14] = b[1]; z[35] = b[2];
		b = b24_from_64(str.substring(pos, pos+4)); pos+=4;
		z[15] = b[0]; z[36] = b[1];z[57] = b[2];
		b = b24_from_64(str.substring(pos, pos+4)); pos+=4;
		z[37] = b[0]; z[58] = b[1]; z[16] = b[2];
		b = b24_from_64(str.substring(pos, pos+4)); pos+=4;
		z[59] = b[0]; z[17] = b[1]; z[38] = b[2];
		b = b24_from_64(str.substring(pos, pos+4)); pos+=4;
		z[18] = b[0]; z[39] = b[1]; z[60] = b[2];
		b = b24_from_64(str.substring(pos, pos+4)); pos+=4;
		z[40] = b[0]; z[61] = b[1]; z[19] = b[2];
		b = b24_from_64(str.substring(pos, pos+4)); pos+=4;
		z[62] = b[0]; z[20] = b[1]; z[41] = b[2];
		b = b24_from_64(str.substring(pos));
		z[63] = b[0];
		return z;
	}
	
	static byte[] b24_from_64(String s) {
		int N = s.length();
		int w = 0;
		int r = 0;
		for (int i=0; i<N; ++i) {
			w |= (b64t.indexOf(s.charAt(i)) << r);
			r += 6;
		}
		if (N==4) {
			byte[] z = new byte[3];
			z[0] = (byte) ((w>>16)&0xff);
			z[1] = (byte) ((w>>8)&0xff);
			z[2] = (byte) ((w)&0xff);
			return z;
		}
		if (N==2) {
			byte[] z = new byte[1];
			z[0] = (byte) w;
			return z;
		}
		return null;
	}
	static final String b64t = "./0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";
	static String  b64_from_24bit(byte BB2, byte BB1, byte BB0, int N) { 
		int B0 = ((int)BB0)&0xff;
		int B1 = ((int)BB1)&0xff;
		int B2 = ((int)BB2)&0xff;
		StringBuilder sb = new StringBuilder();                                                                   
		int w = ((B2) << 16) | ((B1) << 8) | (B0);                       
		int n = (N);                                                              
		while (n-- > 0) {                                             	                                                             
			sb.append(b64t.charAt(w & 0x3f));                                                                                               
			w >>= 6;                                                              
		}
		return sb.toString();
	}
}