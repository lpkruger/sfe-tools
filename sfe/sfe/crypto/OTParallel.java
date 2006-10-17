package sfe.crypto;

import java.math.*;
import java.util.*;
import java.io.*;
import java.net.*;
import java.security.MessageDigest;
import static java.math.BigInteger.ZERO;
import static java.math.BigInteger.ONE;

//oblivious transfer primitives

/* Basic Algorithm:
 chooser - chooses 0 or 1
 sender - provides 2 strings
 
 0) Choose Zq and g - generator.
 1) sender choose random C in Zq - publish
 2) chooser pick random 1<=k<q, 
 public key: is PKs=(g^k) and PK1-s=(C/g^k)
 send PK0 to sender
 3) sender computer PK1 = C/PK0.
 Choose random r0, r1.
 E0 = g^r0, H(PK0^r0) xor M0
 E1 = g^r1, H(PK1^r1) xor M1
 send E0, E1
 4) Computer H((g^rs)^k), decrypt Ms
 */

public class OTParallel {
	final private static void D(String s) {
		//System.out.println(s);
	}
	
	static Random rand = new Random();
	
	// 1 of 2 - primitive
	static class Sender {
		ObjectInputStream in;
		ObjectOutputStream out;
		
		BigInteger[][] M;
		BigInteger q;
		BigInteger g;
		
		public Sender(BigInteger[][] M, BigInteger q, BigInteger g) {
			for (int i=0; i<M.length; ++i) {
				if (M[i].length != 2)
					throw new RuntimeException("Must have exactly 2 choices");
			}
			this.M = M;
			this.q = q;
			this.g = g;
		}
		
		void setStreams(ObjectInputStream in, ObjectOutputStream out) {
			this.in = in;
			this.out = out;
		}
		
		public void go() throws Exception {
			BigInteger[] C = new BigInteger[M.length];
			for (int i=0; i<M.length; ++i) {
				C[i] = new BigInteger(q.bitLength()+16, rand).mod(q);
			}
			D("send C");
			out.writeObject(C);
			out.flush();
			BigInteger[][] PK = new BigInteger[M.length][2];
			D("read PK0");
			BigInteger[] PKM0 = (BigInteger[]) in.readObject();
			// CHECK PKM0.length == M.length
			for (int i=0; i<M.length; ++i) {
				PK[i][0] = PKM0[i];
				PK[i][1] = C[i].multiply(PK[i][0].modInverse(q)).mod(q);
			}
			BigInteger[][][] E = new BigInteger[M.length][2][2];
			for (int i=0; i<M.length; ++i) {
				for (int j=0; j<=1; ++j) {
					BigInteger r = new BigInteger(q.bitLength()+16, rand).mod(q);
					E[i][j][0] = g.modPow(r, q);
					E[i][j][1] = hash(PK[i][j].modPow(r, q)).xor(M[i][j]);
					D("PK[" + i + "," + j + "] = " + PK[i][j].modPow(r, q));
				}
			}
			D("send E");
			out.writeObject(E);
			out.flush();
		}
	}
	
	static class Chooser {
		ObjectInputStream in;
		ObjectOutputStream out;
		
		BigInteger q;
		BigInteger g;
		int[] s; // 0 or 1
		
		public Chooser(int[] s, BigInteger q, BigInteger g) {
			// CHECK: s is all 0s and 1s
			this.s = s;
			this.q = q;
			this.g = g;
		}
		
		void setStreams(ObjectInputStream in, ObjectOutputStream out) {
			this.in = in;
			this.out = out;
		}
		
		public BigInteger[] go() throws Exception {
			D("read C");
			BigInteger[] C = (BigInteger[]) in.readObject();
			// CHECK C.length = s.length
			BigInteger[] k = new BigInteger[s.length];
			BigInteger[][] PK = new BigInteger[s.length][2];
			BigInteger[] PKS0 = new BigInteger[s.length];
			for (int i=0; i<s.length; ++i) {
				do {
					k[i] = new BigInteger(q.bitLength()+16, rand).mod(q);
				} while (k[i].equals(ZERO));
				PK[i][s[i]] = g.modPow(k[i], q);
				PK[i][1-s[i]] = C[i].multiply(PK[i][s[i]].modInverse(q)).mod(q);
				PKS0[i] = PK[i][0];
			}
			D("send PK0");
			out.writeObject(PKS0);
			out.flush();
			BigInteger[][][] E = (BigInteger[][][]) in.readObject();
			D("read E");
			
			//D("E = <" + E[s][0] + ", " + E[s][1]);
			//D("grk = " + E[s][0].modPow(k, q));
			
			/*
			 BigInteger m0 = hash(E[0][0].modPow(k, q)).xor(E[0][1]);
			 BigInteger m1 = hash(E[1][0].modPow(k, q)).xor(E[1][1]);
			 System.out.println("m0 = " + m0);
			 System.out.println("m1 = " + m1);
			 */
			BigInteger[] ret = new BigInteger[s.length];
			for (int i=0; i<s.length; ++i) {
				ret[i] = hash(E[i][s[i]][0].modPow(k[i], q)).xor(E[i][s[i]][1]);
			}
			return ret;
		}
	}
	
	private static MessageDigest SHA1;
	static {
		try {
			SHA1 = MessageDigest.getInstance("SHA-1");
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}
	static BigInteger hash(BigInteger m) {
		return new BigInteger(SHA1.digest(m.toByteArray()));
	}
	
	// find a pseudo-generator for q.  
	// with high probability it is a generator
	public static BigInteger findGenerator(BigInteger p) {
		// p should be prime
		BigInteger k = p.subtract(ONE);
		BigInteger x = ONE;
		
		// find product of small primes
		BigInteger TWO = BigInteger.valueOf(2);
		BigInteger ii = TWO;
		while (k.mod(ii).equals(ZERO)) {
			k = k.divide(ii);
			x = x.multiply(ii);
		}
		
		for (int i=3; i<10001; i+=2) {
			ii = BigInteger.valueOf(i);
			while (k.mod(ii).equals(ZERO)) {
				k = k.divide(ii);
				x = x.multiply(ii);
			}
		}
		
		return TWO.modPow(x, p);
	}
	
	
	public static void main(String[] args) throws Exception {
		Socket s;
		if (args[0].equals("A")) {
			s = new Socket("localhost", 5435);
		} else if (args[0].equals("B")) {
			ServerSocket ss = new ServerSocket(5435);
			s = ss.accept();
		} else return;
		
		System.out.println("Socket: " + s);
		
		ObjectOutputStream out = 
			new ObjectOutputStream(new BufferedOutputStream
					(s.getOutputStream()));
		out.flush();
		ObjectInputStream in = 
			new ObjectInputStream(new BufferedInputStream
					(s.getInputStream()));
		
		BigInteger TWO = ONE.add(ONE);
		BigInteger Q = TWO.pow(128).nextProbablePrime();
		BigInteger G = findGenerator(Q);
		
		if (args[0].equals("A")) {
			Sender send = new Sender(new BigInteger[][] {
					{BigInteger.valueOf(123456), BigInteger.valueOf(789012)},
					{BigInteger.valueOf(123), BigInteger.valueOf(789)},
					{BigInteger.valueOf(1003), BigInteger.valueOf(83784)}
			}, Q, G);
			send.setStreams(in, out);
			send.go();
		} else if (args[0].equals("B")) {
			int[] ss = new int[args.length - 1];
			for (int i=1; i<args.length; ++i) {
				ss[i-1] = Integer.parseInt(args[i]);
				if (ss[i-1]!=0 && ss[i-1]!=1)
					throw new RuntimeException("Bad s: " + ss[i-1]);
			}
			Chooser choos = new Chooser(ss, Q, G);
			choos.setStreams(in, out);
			BigInteger[] val = choos.go();
			for (int i=0; i<val.length; ++i) {
				System.out.println("Value " + i + " = " + val[i]);
			}
		}
	}
}
