package bdd.proto.singlebit;
import java.math.*;
import java.util.*;
import java.io.*;
import java.net.*;
import java.security.MessageDigest;
import static java.math.BigInteger.ZERO;
import static java.math.BigInteger.ONE;
import SFE.BOAL.MyUtil;
import SFE.BOAL.OTr.OT;
import SFE.BOAL.OTr.OTTASK;
import SFE.BOAL.OTr.OTMESS;


public class OTFairPlay {
	
	static byte[] pack(byte[] m) {
		byte[] r = new byte[m.length + 2];
		System.arraycopy(m, 0, r, 2, m.length);
		r[0] = (byte) ((m.length>>8) & 0xff);
		if (r[0] == 0)  r[0] = (byte) 0x7f;
		r[1] = (byte) ((m.length) & 0xff);
		return r;
	}
	
	static byte[] unpack(byte[] r) {
		if (r[0] == 0x7f)  r[0] = 0;
		int len = (r[0] << 8) + (r[1]);
		byte[] m = new byte[len];
		System.out.println(r.length + " " + len);
		System.arraycopy(r, 2, m, 0, len);
		return m;
	}
	
	static final int ot_type = 4;
	
	final private static void D(String s) {
		//System.out.println(s);
	}
	
	// 1 of 2 - primitive
	public static class Sender {
		ObjectInputStream in;
		ObjectOutputStream out;
		Vector<OTTASK> v = new Vector<OTTASK>();

		public Sender(BigInteger[][] M) {
			for (int i=0; i<M.length; ++i) {
				if (M[i].length != 2)
					throw new RuntimeException("Must have exactly 2 choices");
				OTTASK task = new OTTASK(0, 0);
				for (int j=0; j<M[i].length; ++j) {
					//System.out.println("len: " + M[i][j].toByteArray().length);
					//System.out.println("num: " + M[i][j].toString(16));
					task.addElement(pack(M[i][j].toByteArray()));
				}
				v.add(task);
			}
		}
		
		
		public void setStreams(ObjectInputStream in, ObjectOutputStream out) {
			this.in = in;
			this.out = out;
		}
		
		public void go() throws Exception {
			MyUtil.init("123asd");
		    OT ot = new OT(ot_type);
		    ot.executeSenderOTs(v, out, in);
		}
	}
	
	public static class Chooser {
		ObjectInputStream in;
		ObjectOutputStream out;
		Vector<OTTASK> v = new Vector<OTTASK>();

		
		public Chooser(int[] s) {
			// CHECK: s is all 0s and 1s
			for (int i=0; i<s.length; ++i)
			v.add(new OTTASK(0,0,s[i]));
		}
		
		public void setStreams(ObjectInputStream in, ObjectOutputStream out) {
			this.in = in;
			this.out = out;
		}
		
		public BigInteger[] go() throws Exception {
			MyUtil.init("123asd");
		    OT ot = new OT(ot_type);
		    ot.executeChooserOTs(v, out, in);
			BigInteger[] ret = new BigInteger[v.size()];
			for (int i=0; i<ret.length; ++i) {
				//System.out.println("len: " + v.get(i).transferred_value.length);
			    ret[i] = new BigInteger(unpack(v.get(i).transferred_value));
			    //System.out.println("num: " + ret[i].toString(16));
			}
			return ret;
 	    }				
	}
	
	
	// simple test program
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
		
		
		if (args[0].equals("A")) {
			Sender send = new Sender(new BigInteger[][] {
					{BigInteger.valueOf(123456), BigInteger.valueOf(789012)},
					{BigInteger.valueOf(123), BigInteger.valueOf(789)},
					{BigInteger.valueOf(1003), BigInteger.valueOf(83784)}
			});
			send.setStreams(in, out);
			send.go();
		} else if (args[0].equals("B")) {
			int[] ss = new int[args.length - 1];
			for (int i=1; i<args.length; ++i) {
				ss[i-1] = Integer.parseInt(args[i]);
				if (ss[i-1]!=0 && ss[i-1]!=1)
					throw new RuntimeException("Bad s: " + ss[i-1]);
			}
			Chooser choos = new Chooser(ss);
			choos.setStreams(in, out);
			BigInteger[] val = choos.go();
			for (int i=0; i<val.length; ++i) {
				System.out.println("Value " + i + " = " + val[i]);
			}
		}
	}
}
