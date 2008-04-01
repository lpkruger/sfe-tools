package sfe.crypto;

import java.math.*;
import java.util.*;
import java.io.*;
import java.net.*;
import java.security.*;
import javax.crypto.*;
import javax.crypto.spec.*;

import static java.math.BigInteger.ZERO;
import static java.math.BigInteger.ONE;

//1 out of N OT
public class OTN {
	private static final void D(String s) {
		//System.out.println(s);
	}
	
	static Random rand = new Random();
	
	static Cipher AES;
	
	static {
		try {
			AES = Cipher.getInstance("AES");
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}
	
	// returns m such that 2^m >= n
	static int log2(int n) {
		int m = 0;
		long x = 1;
		while (x<n) {
			x *= 2;
			++m;
		}
		return m;
	}
	
	public static class Sender {
		ObjectInputStream in;
		ObjectOutputStream out;
		
		BigInteger[] M;
		BigInteger q;
		BigInteger g;
		
		public Sender(BigInteger[] M, BigInteger q, BigInteger g) {
			this.M = M;
			this.q = q;
			this.g = g;
		}
		
		public void setStreams(ObjectInputStream in, ObjectOutputStream out) {
			this.in = in;
			this.out = out;
		}
		
		public void go() throws Exception {
			BigInteger[][] keys =
				new BigInteger[log2(M.length)][2];
			SecretKey[][] skeys =
				new SecretKey[keys.length][2];
			
			out.writeInt(M.length);
			out.flush();
			
			for (int i=0; i<keys.length; ++i) {
				for (int j=0; j<2; ++j) {
					do {
						keys[i][j] = new BigInteger(136, rand);
					} while (keys[i][j].bitLength() < 129);
					
					//D("keybytes = " + keys[i][j].toByteArray().length);
					skeys[i][j] = new SecretKeySpec
					(keys[i][j].toByteArray(), 1, 16, "AES");
				}
			}
			byte[][] Y = new byte[M.length][];
			for (int i=0; i<Y.length; ++i) {
				Y[i] = M[i].toByteArray();
				for (int j=skeys.length-1; j>=0; --j) {
					int x = (i & (1<<j))!=0 ? 1 : 0;
					AES.init(Cipher.ENCRYPT_MODE, skeys[j][x]);
					Y[i] = AES.doFinal(Y[i]);
				}
			}
			// now do the OT keys.length times...
			OTParallel.Sender send = new OTParallel.Sender(keys, q, g);
			send.setStreams(in, out);
			send.go();
	
			out.writeObject(Y);
			out.flush();
		}
	}
	
	public static class Chooser {
		ObjectInputStream in;
		ObjectOutputStream out;
		
		int s;
		BigInteger q;
		BigInteger g;
		
		public Chooser(int s, BigInteger q, BigInteger g) {
			this.s = s;
			this.q = q;
			this.g = g;
		}
		
		public void setStreams(ObjectInputStream in, ObjectOutputStream out) {
			this.in = in;
			this.out = out;
		}
		
		public BigInteger go() throws Exception {
			int Mlength = in.readInt();
			
			SecretKey[] skeys =
				new SecretKey[log2(Mlength)];
			
			// now do the OT keys.length times...
			int[] x = new int[skeys.length];
			for (int i=0; i<skeys.length; ++i) {
				x[i] = (s & (1<<i))!=0 ? 1 : 0;
			}
			OTParallel.Chooser choos = new OTParallel.Chooser(x, q, g);
			choos.setStreams(in, out);
			BigInteger[] keys = choos.go();
			out.flush();
			
			for (int i=0; i<skeys.length; ++i) {
				skeys[i] = new SecretKeySpec
				(keys[i].toByteArray(), 1, 16, "AES");
			}
			byte[][] Y = (byte[][]) in.readObject();
			
			byte[] Z = Y[s];
			Y = null;
			for (int j=0; j<skeys.length; ++j) {
				D("decrypt round " + j + "  bytes = " + Z.length);
				AES.init(Cipher.DECRYPT_MODE, skeys[j]);
				Z = AES.doFinal(Z);
			}
			
			return new BigInteger(Z);
		}
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
		BigInteger G = OT.findGenerator(Q);
		
		if (args[0].equals("A")) {
			BigInteger[] M = {new BigInteger("123"),
					new BigInteger("456"),
					new BigInteger("789"),
					new BigInteger("12345"),
					new BigInteger("6789012")};
			
			Sender send = new Sender(M, Q, G);
			send.setStreams(in, out);
			send.go();
		} else if (args[0].equals("B")) {
			int ss = Integer.parseInt(args[1]);
			Chooser choos = new Chooser(ss, Q, G);
			choos.setStreams(in, out);
			System.out.println(choos.go());
		}
	}
}
