package sfe.editdist;

import java.io.*;
import java.math.BigInteger;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Random;
import java.util.TreeMap;
import java.util.TreeSet;

import fairplay.BOAL.AliceLib;
import fairplay.BOAL.BobLib;

import sfe.shdl.*;
import sfe.util.*;
// import sfe.bdd.proto.Protocol;
import sfe.crypto.*;

/*
 * recurrence:
 * Alice has a(i,j), Bob has b(i,j) where a(i,j)+b(i,j)=D(i,j)
 * input: a(i-1,j-1),a(i-1,j),a(i,j-1),b(i-1,j-1),b(i-1,j),b(i,j-1)
 * compute min(X,Y,Z)-R and R
 * X = a(i-1,j)+b(i-1,j)
 * Y = a(i,j-1)+b(i,j-1)
 * Z = a(i-1,j-1)+b(i-1,j-1)+t(i,j)
 * 
 */

public class EDProtoBig2 {
	
	static void D(Object s) {
		System.out.println(s);
	}
	
	static BigInteger TWO = BigInteger.valueOf(2);
	static final int N_BITS=8;
	static final int CHAR_BITS=8;
	static final BigInteger MAX_BIGINT = TWO.pow(N_BITS).subtract(BigInteger.ONE);
	
	static final boolean use_fairplay = false;
	
	public static void main(String[] args) throws Exception {
		if (System.getProperty("BOB") != null) {
			Bob.main(args);
		} else if (System.getProperty("ALICE") != null) {
			Alice.main(args);
		} else System.out.println("Must use -DALICE or -DBOB");
	}
	
	public static BigInteger getRandom() {
		return new BigInteger(N_BITS, new Random());
		// DEBUG:
		//return BigInteger.ZERO;
	}
	
	public static class Alice {
		
		Socket bob;
		ObjectInputStream in;
		ObjectOutputStream out;
		long startTime;
		ByteCountOutputStreamSFE byteCount;
		
		String str1;
		
		Alice(String to, int port, String str1) throws IOException {
			bob = new Socket(to, port);
			
			startTime = System.currentTimeMillis();
			byteCount = 
				new ByteCountOutputStreamSFE(
						bob.getOutputStream());
			out = new ObjectOutputStream(byteCount);
			out.flush();
			in = new ObjectInputStream
			(new BufferedInputStream
					(bob.getInputStream()));
			
			this.str1 = str1;
			
		}
		
		public static void main(String[] args) throws Exception {
			String to = args[0];
			int port = Integer.parseInt(args[1]);
			String str1 = args[2];
			
			new Alice(to, port, str1).go();
		}
		
		void go() throws Exception {
			int astrlen = str1.length();
			out.writeInt(astrlen);
			out.flush();
			
			int bstrlen = in.readInt();
				
			ByteCountObjectOutputStream out = ByteCountObjectOutputStream.wrapObjectStream(this.out);
			out.flush();
			ObjectInputStream in = new ObjectInputStream(this.in);
			
			D("prepare circuit");
			// evaluate min circuit
			Circuit circuit = CircuitParser.readFile("DYN_MN/circ_" + N_BITS + "_" + astrlen + "_" + bstrlen + ".txt.Opt.circuit");
			FmtFile fmt = FmtFile.readFile("DYN_MN/circ_" + N_BITS + "_" + astrlen + "_" + bstrlen + ".txt.Opt.fmt");
			VarDesc bdv = fmt.getVarDesc();
			VarDesc aliceVars = bdv.filter("A");
			VarDesc bobVars = bdv.filter("B");
			
			TreeMap<Integer,Boolean> vals = new TreeMap<Integer,Boolean>();
			
			for (int i=str1.length()-1; i>=0; --i) {
				byte c0 = (byte) str1.charAt(i);
				fmt.mapBits(BigInteger.valueOf(c0), vals, "input.alice.x[" + i + "]");
			}
	
			D("eval circuit");
			sfe.shdl.Protocol.Alice calice = new sfe.shdl.Protocol.Alice(in, out, circuit);
			calice.go(vals, 
					new TreeSet<Integer>(aliceVars.who.keySet()),
					new TreeSet<Integer>(bobVars.who.keySet()));
			
			System.out.println("Alice circuit wrote " + out.getCount() + " bytes");
			
			// for DEBUG
			//BigInteger bobVal = (BigInteger) in.readObject();
			//BigInteger combined = r0.add(bobVal).and(MAX_BIGINT);
			//System.out.println("result after stage: " + combined);
		}
	}
	
	public static class Bob {
		ServerSocket listen;
		Socket alice;
		ObjectInputStream in;
		ObjectOutputStream out;
		ByteCountOutputStreamSFE byteCount;
		long startTime;
		
		Domain domain;
		
		String str2;
		
		Bob(int port, String str2) throws IOException {
			listen = new ServerSocket(port);
			alice = listen.accept();
			startTime = System.currentTimeMillis();
			byteCount = 
				new ByteCountOutputStreamSFE(
						alice.getOutputStream());
			out = new ObjectOutputStream(byteCount);
			out.flush();
			in = new ObjectInputStream
			(new BufferedInputStream
					(alice.getInputStream()));
			
			this.str2 = str2;
		}
		
		public static void main(String[] args) throws Exception {
			int port = Integer.parseInt(args[0]);
			String str2 = args[1];
			
			new Bob(port, str2).go();
		}
		
		void go() throws Exception {
			int astrlen = in.readInt();
			int bstrlen = str2.length();
			out.writeInt(bstrlen);
			out.flush();
			
			ByteCountObjectOutputStream out = ByteCountObjectOutputStream.wrapObjectStream(this.out);
			out.flush();
			ObjectInputStream in = new ObjectInputStream(this.in);
			
			TreeMap<Integer,Boolean> vals = new TreeMap<Integer,Boolean>();
			
			///
			
			int cBits=CHAR_BITS;
			int bStart=0;
			
			for (int i=str2.length()-1; i>=0; --i) {
				byte c1 = (byte) str2.charAt(i);
				mapBits(BigInteger.valueOf(c1), vals, bStart, (bStart+=cBits)-1);
			}
			
			boolean[] vv = new boolean[vals.size()];
			int vi=0;
			for (Boolean bb : vals.values()) {
				vv[vi] = bb;
				vi++;
			}
			sfe.shdl.Protocol.Bob cbob = new sfe.shdl.Protocol.Bob(in, out, vv);
			cbob.go();
					
			BigInteger zz = BigInteger.ZERO;
			
			for (int ri=N_BITS; ri<cbob.result.length; ++ri) {
				System.out.print(cbob.result[ri] ? "1" : "0");
				if (cbob.result[ri]) {
					zz = zz.setBit(ri-N_BITS);
				}
			}
			
			System.out.println();
			System.out.println();
			
			System.out.println("Bob circuit wrote " + out.getCount() + " bytes");
			
			System.out.println();
			
			System.out.println("state eval: " + zz);
			//out.writeObject(zz);
			//out.flush();
			// end DEBUG
			
		}
	}
	
	static void mapBits(BigInteger n, TreeMap<Integer,Boolean> vals, int lsb, int msb) {
		D("map bits " + n);
		D("  from " + lsb + " to " + msb);
		for (int i=0; i<msb-lsb+1; ++i) {
			//D("put " + n.testBit(i) + " in " + (lsb+i));   // lsb first
            vals.put(lsb+i, n.testBit(i));
		}
	}
}
