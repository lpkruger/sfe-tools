package sfe.sfauth;

import java.io.FileOutputStream;
import java.io.PrintStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

import sfe.shdl.Circuit;
import sfe.shdl.CircuitWriter;
import sfe.util.Base64;

public class MD5pw {
	static byte[] testsalt = "QyenZBsY" .getBytes();
	static byte[] magic = "$1$".getBytes();
	
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
		String pwcr = md5_pw(pw, testsalt);
		System.out.println(pwcr);
		pwcr = pwcr.substring(pwcr.lastIndexOf('$')+1);
		System.out.println(pwcr);
		System.out.println(toB64(fromB64(pwcr)));
		byte[] fin = md5_pw_999(pw, testsalt);
		
		/*
		MessageDigest md5 = MessageDigest.getInstance("MD5");
		md5.update(pw);
		md5.update(pw);
		md5.update(fin);
		fin = md5.digest();*/
		
		MD5 md5gen = new MD5();
		/*
		md5gen.initialize(2);
		md5gen.update(0);
		md5gen.update(0);
		md5gen.update(1);
		Circuit cc = md5gen.digest();*/
		Circuit cc = md5gen.generate();
		PrintStream stdout = System.out;
		
		FileOutputStream circout1 = new FileOutputStream("md5_std.circ");
		System.setOut(new PrintStream(circout1));
		CircuitWriter.write(cc);
		circout1.close();
		System.setOut(stdout);
		
		boolean[] in1 = MD5.bytes2bool(args[0].getBytes());
		boolean[] in2 = MD5.bytes2bool(args[0].getBytes());
		boolean[] in3 = MD5.bytes2bool(fin);
		boolean[][] inputs = { in1, in2, in3 };
		fin = MD5.bool2bytes(MD5.compute_md5(cc, concat(inputs)));
		System.out.println(new String(magic)+new String(testsalt)+"$"+toB64(fin));
		
		cc = md5gen.generateWithPrivEq();
		cc.outputs[0].setComment("output.bob$0");
		boolean[] in4 = MD5.bytes2bool(fin);
		//boolean[] in4 = new boolean[128];
		boolean[][] eqinputs = { MD5.prepare_md5_input(concat(inputs)), in4 };
		boolean[] out = cc.eval(concat(eqinputs));
		System.out.println("eq: " + out[0]);
		
		FileOutputStream circout = new FileOutputStream("md5_pw_cmp.circ");
		System.setOut(new PrintStream(circout));
		CircuitWriter.write(cc);
		circout.close();
		FileOutputStream fmtout = new FileOutputStream("md5_pw_cmp.fmt");
		System.setOut(new PrintStream(fmtout));
		System.out.print("Alice input integer \"input.alice.x\" [");
		for (int i=0; i<512; ++i) {
			System.out.print(" "+i);
		}
		System.out.println(" ]");
		System.out.print("Bob input integer \"input.bob.y\" [");
		for (int i=0; i<128; ++i) {
			System.out.print(" "+(512+i));
		}
		System.out.println(" ]");
		System.out.println("Bob output integer \"output.bob\" [ "+
				cc.outputs[0].id + " ]");
		
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
	
	public static byte[] md5_pw_999(byte[] pw, byte[] salt) {
		MessageDigest md;
		MessageDigest finmd;
		try {
			 md = MessageDigest.getInstance("MD5");
			 finmd = MessageDigest.getInstance("MD5");
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace(System.err);
			throw new NullPointerException(e.getMessage());
		}
		md.update(pw);
		md.update(magic);
		md.update(salt);
		
		
		finmd.update(pw);
		finmd.update(salt);
		finmd.update(pw);
		byte[] fin = finmd.digest();
		
		//print(fin);
		
		for (int pl = pw.length; pl>0; pl-=16) {
			byte[] z = new byte[pl>16 ? 16 : pl];
			System.arraycopy(fin, 0, z, 0, z.length);
			md.update(z);
		}
		
		// weird thing 1
		byte[] con0 = { 0 };
		byte[] pw0 = { pw[0] };
		for (int i = pw.length; i>0; i >>= 1) {
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
	
	public static String md5_pw(byte[] pw, byte[] salt) {
		MessageDigest md;
		MessageDigest finmd;
		try {
			 md = MessageDigest.getInstance("MD5");
			 finmd = MessageDigest.getInstance("MD5");
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace(System.err);
			throw new NullPointerException(e.getMessage());
		}
		md.update(pw);
		md.update(magic);
		md.update(salt);
		
		
		finmd.update(pw);
		finmd.update(salt);
		finmd.update(pw);
		byte[] fin = finmd.digest();
		
		//print(fin);
		
		for (int pl = pw.length; pl>0; pl-=16) {
			byte[] z = new byte[pl>16 ? 16 : pl];
			System.arraycopy(fin, 0, z, 0, z.length);
			md.update(z);
		}
		
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

	static String toB64(byte[] fin) {
		StringBuilder sb = new StringBuilder();
		sb.append(b64_from_24bit (fin[0], fin[6], fin[12], 4));
		sb.append(b64_from_24bit (fin[1], fin[7], fin[13], 4));
		sb.append(b64_from_24bit (fin[2], fin[8], fin[14], 4));
		sb.append(b64_from_24bit (fin[3], fin[9], fin[15], 4));
		sb.append(b64_from_24bit (fin[4], fin[10], fin[5], 4));
		sb.append(b64_from_24bit ((byte)0, (byte)0, fin[11], 2));
		return sb.toString();
	}
	static byte[] fromB64(String str) {
		byte[] z = new byte[16];
		byte[] b = b24_from_64(str.substring(0, 4));
		z[0] = b[0]; z[6] = b[1]; z[12] = b[2];
		b = b24_from_64(str.substring(4, 8));
		z[1] = b[0]; z[7] = b[1]; z[13] = b[2];
		b = b24_from_64(str.substring(8, 12));
		z[2] = b[0]; z[8] = b[1]; z[14] = b[2];
		b = b24_from_64(str.substring(12, 16));
		z[3] = b[0]; z[9] = b[1]; z[15] = b[2];
		b = b24_from_64(str.substring(16, 20));
		z[4] = b[0]; z[10] = b[1]; z[5] = b[2];
		b = b24_from_64(str.substring(20));
		z[11] = b[0];
		return z;
	}
	static byte[] fromB64_wrong(String str) {
		byte[] zz = new byte[0];
		int len = str.length();
		for (int i=0; i<len; i+=4) {
			String ss = str.substring(i, i+4>len ? i+2 : i+4);
			byte[] bb = b24_from_64(ss);
			byte[] zzz = new byte[zz.length + bb.length];
			System.arraycopy(zz, 0, zzz, 0, zz.length);
			System.arraycopy(bb, 0, zzz, zz.length, bb.length);
			zz = zzz;
		}
		return zz;
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