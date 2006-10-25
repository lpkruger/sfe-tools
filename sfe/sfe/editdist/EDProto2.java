package sfe.editdist;

import java.io.*;
import java.math.BigInteger;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.GeneralSecurityException;
import java.util.Random;
import java.util.TreeMap;
import java.util.TreeSet;

import javax.crypto.BadPaddingException;

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

public class EDProto2 {
	
	static void D(Object s) {
		System.out.println(s);
	}
	
	static SFECipher C;
	
	static {
		try {
			C = SFECipher.getInstance("SHA-256");
		} catch (Exception ex) {
			ex.printStackTrace();
			throw new RuntimeException(ex);
		}
	}
	
	static BigInteger TWO = BigInteger.ONE.add(BigInteger.ONE);
	static BigInteger QQQ = TWO.pow(128).nextProbablePrime();
	static BigInteger GGG = OT.findGenerator(QQQ);
	
	static final int N_BITS=80;
	static final BigInteger MAX_BIGINT = TWO.pow(N_BITS).subtract(BigInteger.ONE);
	
	public static void main(String[] args) throws Exception {
		if (System.getProperty("BOB") != null) {
			Bob.main(args);
		} else if (System.getProperty("ALICE") != null) {
			Alice.main(args);
		} else System.out.println("Must use -DALICE or -DBOB");
	}
	
	public static BigInteger getRandom() {
		return new BigInteger(N_BITS-8, new Random());
		// DEBUG:
		//return BigInteger.ZERO;
	}
	
	public static class Alice {
		
		Socket bob;
		ObjectInputStream in;
		ObjectOutputStream out;
		long startTime;
		ByteCountOutputStreamSFE byteCount;
	
		/*
		Domain domain = new Domain(-20, 20,
	               java.math.BigInteger.valueOf(3).pow(32),
	               BigInteger.valueOf(500));
		*/
		
		String str1;
		
		// matrix of Alice's portion of split state.
		// is int[str1.length][str2.length]
		BigInteger aState[][];
		
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
			aState = new BigInteger[astrlen+1][bstrlen+1];
			
			// initial split: Alice gets values, Bob gets 0's
			
			for (int i=0; i<=astrlen; ++i) {
				aState[i][0] = BigInteger.valueOf(i);
			}
			for (int j=1; j<=bstrlen; ++j) {
				aState[0][j] = BigInteger.valueOf(j);
			}
			
			//D("write domain");
			//out.writeObject(domain);
			//out.flush();
			
			for (int i=1; i<=astrlen; ++i) {
				for (int j=1; j<=bstrlen; ++j) {
					computeRecurrence(i,j);
					out.reset();
				}
			}
			
			System.out.println();
			System.out.println("Alice result:" + aState[astrlen][bstrlen]);
		}
		
		
		
		void computeRecurrence(int i, int j) throws Exception {
			//  first, compute state[i-1][j-1] + t(i,j) - r with Bob
			// create keypair
			out.flush();
			
			ByteCountObjectOutputStream out = ByteCountObjectOutputStream.wrapObjectStream(this.out);
			out.flush();
			ObjectInputStream in = new ObjectInputStream(this.in);
			
			System.out.println("compute " + i + "," + j);
			BigInteger NUM = BigInteger.valueOf(3).pow(64);
			HomomorphicCipher.DecKey prkey = DPE.genKey(NUM, NUM.bitLength());
			//HomomorphicCipher.DecKey prkey = DPE.genKey(domain.dint, domain.dint.bitLength());
			//HomomorphicCipher.DecKey prkey = Paillier.genKey(256);
			HomomorphicCipher.EncKey pubkey = prkey.encKey();
			
			// send Bob public key
			D("Send key");
			out.writeObject(pubkey);
			
			D(pubkey);
			// send Bob E(a)
			BigInteger enc1 = pubkey.encrypt(aState[i-1][j-1]);
	
			D("Send E(a)");
			out.writeObject(enc1);
			out.flush();
			
			//BigInteger mm1 = (BigInteger) in.readObject();
			//BigInteger mm2 = (BigInteger) in.readObject();
			byte[] mm1 = (byte[]) in.readObject();
			byte[] mm2 = (byte[]) in.readObject();
			
			////////////////
			
			FmtFile fmt2c = FmtFile.readFile("editdist/proto2c_128.txt.Opt.fmt");
			TreeMap<Integer,Boolean> vals = new TreeMap<Integer,Boolean>();
			
			fmt2c.mapBits(BigInteger.valueOf(str1.charAt(i-1)), vals, "input.bob.y");
			
			boolean[] vv = new boolean[vals.size()];
			int vi=0;
			for (Boolean bb : vals.values()) {
				vv[vi] = bb;
				vi++;
			}
			sfe.shdl.Protocol.Bob cbob = new sfe.shdl.Protocol.Bob(in, out, vv);
			cbob.go();
			
			BigInteger kk0 = fmt2c.readBits(cbob.result, "output.bob.k");
			if (cbob.result[0] & 0x80 != 0) {
				
			}
			
			SFEKey k0 = decodeKey(kk0);
			
			BigInteger aa0 = null;
			try {
				C.init(C.DECRYPT_MODE, k0);
				byte[] m1 = C.doFinal(mm1);
				aa0 = new BigInteger(m1);
			} catch (BadPaddingException ex) {
			}
			
			if (aa0 == null) {
				C.init(C.DECRYPT_MODE, k0);
				byte[] m2 = C.doFinal(mm2);
				aa0 = new BigInteger(m2);
			}
			
			////////////////
			
			
			// decrypt
			BigInteger a0 = prkey.decrypt(aa0);
			BigInteger b0 = aState[i-1][j].add(BigInteger.ONE);
			BigInteger c0 = aState[i][j-1].add(BigInteger.ONE);
			BigInteger r0 = getRandom().negate();
			//r0 = BigInteger.ONE.add(BigInteger.ONE).add(BigInteger.ONE);
			
			aState[i][j] = r0;
			
			D("prepare circuit");
			// evaluate min circuit
			Circuit circuit = CircuitParser.readFile("editdist/proto2b_" + N_BITS + ".txt.Opt.circuit");
			
			FmtFile fmt = FmtFile.readFile("editdist/proto2b_" + N_BITS + ".txt.Opt.fmt");
			VarDesc bdv = fmt.getVarDesc();
			VarDesc aliceVars = bdv.filter("A");
			VarDesc bobVars = bdv.filter("B");
			
			vals.clear();
			
			fmt.mapBits(r0, vals, "input.alice.r");
			fmt.mapBits(c0, vals, "input.alice.c");
			fmt.mapBits(b0, vals, "input.alice.b");
			fmt.mapBits(a0, vals, "input.alice.a");
			
			D("eval circuit");
			sfe.shdl.Protocol.Alice calice = new sfe.shdl.Protocol.Alice(in, out, circuit);
			calice.go(vals, 
					new TreeSet<Integer>(aliceVars.who.keySet()),
					new TreeSet<Integer>(bobVars.who.keySet()));
		
			System.out.println("Alice Iteration wrote " + out.getCount() + " bytes");
			
			// for DEBUG
			BigInteger bobVal = (BigInteger) in.readObject();
			BigInteger combined = r0.add(bobVal).and(MAX_BIGINT);
			System.out.println("result after stage: " + combined);
			// end DEBUG
			

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

		// matrix of Bob's portion of split state.
		// is int[str1.length][str2.length]
		BigInteger bState[][];
		
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
			
			bState = new BigInteger[astrlen+1][bstrlen+1];
			
//			 initial split: Alice gets values, Bob gets 0's
			
			for (int i=0; i<=astrlen; ++i) {
				bState[i][0] = BigInteger.ZERO;
			}
			for (int j=1; j<=bstrlen; ++j) {
				bState[0][j] = BigInteger.ZERO;
			}
			
			// domain = (Domain) in.readObject();
			
			for (int i=1; i<=astrlen; ++i) {
				for (int j=1; j<=bstrlen; ++j) {	
					computeRecurrence(i,j);
					out.reset();
				}
			}
			
			System.out.println("Bob result:" + bState[astrlen][bstrlen]);
		}
		
		
		void computeRecurrence(int i, int j) throws Exception {
			//  first, compute state[i-1][j-1] + t(i,j) - r with Alice
			
			ByteCountObjectOutputStream out = ByteCountObjectOutputStream.wrapObjectStream(this.out);
			out.flush();
			ObjectInputStream in = new ObjectInputStream(this.in);
			
			HomomorphicCipher.EncKey pubkey = (HomomorphicCipher.EncKey) in.readObject();
			
			// alice's half
			BigInteger enc1 = (BigInteger) in.readObject();
			
			// a+b
			BigInteger enc2 = enc1.multiply(pubkey.encrypt(bState[i-1][j-1]));
			
			// choose random R
			BigInteger rsub = getRandom();
			
			//BigInteger rsub = TWO.pow(N_BITS).subtract(r);
			BigInteger r = rsub.negate();
			BigInteger val = pubkey.add(enc2, pubkey.encrypt(rsub));
			/*
			if (val.compareTo(BigInteger.ZERO) < 0) {
				val = val.add(TWO.pow(N_BITS));
			}
			*/
			
			BigInteger val2 = pubkey.add(val, pubkey.encrypt(BigInteger.ONE));

			D("val0 : " + val);
			D("val1 : " + val2);
			
			// encrypt two values with random keys
			SFEKeyGenerator keygen = new SFEKeyGenerator();
			keygen.init(120);
			SFEKey k0 = (SFEKey) keygen.generateKey();
			SFEKey k1 = (SFEKey) keygen.generateKey();
			

			
			byte[] m1, m2;
		
			try {
				C.init(C.ENCRYPT_MODE, k0);
				m1 = C.doFinal(val.toByteArray());
				C.init(C.ENCRYPT_MODE, k1);
				m2 = C.doFinal(val2.toByteArray());
				
				//m1 = new BigInteger(mm1);
				//m2 = new BigInteger(mm1);
			} catch (GeneralSecurityException ex) {
				ex.printStackTrace();
				throw new RuntimeException("encryption error");
			}
			
			out.writeObject(m1);
			out.writeObject(m2);
			out.flush();
			
			// run equality circuit
			Circuit circuit2c = CircuitParser.readFile("editdist/proto2c_128.txt.Opt.circuit");
			FmtFile fmt2c = FmtFile.readFile("editdist/proto2c_128.txt.Opt.fmt");
			TreeMap<Integer,Boolean> vals = new TreeMap<Integer,Boolean>();
			
			fmt2c.mapBits(encodeKey(k0), vals, "input.alice.k0");
			fmt2c.mapBits(encodeKey(k1), vals, "input.alice.k1");
			fmt2c.mapBits(BigInteger.valueOf(str2.charAt(j-1)), vals, "input.alice.x");
			
			VarDesc bdv = fmt2c.getVarDesc();
			VarDesc aliceVars = bdv.filter("A");
			VarDesc bobVars = bdv.filter("B");
			
			sfe.shdl.Protocol.Alice calice = new sfe.shdl.Protocol.Alice(in, out, circuit2c);
			calice.go(vals, 
					new TreeSet<Integer>(aliceVars.who.keySet()),
					new TreeSet<Integer>(bobVars.who.keySet()));
		
			
//			 run circuit
			BigInteger a1 = r;
			BigInteger b1 = bState[i-1][j];
			BigInteger c1 = bState[i][j-1];
			
			BigInteger zz = BigInteger.ZERO;
			
			FmtFile fmt = FmtFile.readFile("editdist/proto2b_" + N_BITS + ".txt.Opt.fmt");
			vals.clear();
			
			fmt.mapBits(c1, vals, "input.bob.c");
			fmt.mapBits(b1, vals, "input.bob.b");
			fmt.mapBits(a1, vals, "input.bob.a");
			
			boolean[] vv = new boolean[vals.size()];
			int vi=0;
			for (Boolean bb : vals.values()) {
				vv[vi] = bb;
				vi++;
			}
			sfe.shdl.Protocol.Bob cbob = new sfe.shdl.Protocol.Bob(in, out, vv);
			cbob.go();
			
			zz = fmt.readBits(cbob.result, "output.bob");
			System.out.println();
			
			bState[i][j] = zz;
			
			System.out.println("Bob Iteration wrote " + out.getCount() + " bytes");
			
			// for DEBUG
			System.out.println("state eval: " + zz);
			out.writeObject(zz);
			out.flush();
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
	
	static BigInteger encodeKey(SFEKey k) {
		byte[] bytes = k.getEncoded();
		byte[] encbytes = new byte[bytes.length+1];
		System.arraycopy(bytes, 0, encbytes, 1, bytes.length);
		encbytes[0] = 1;
		return new BigInteger(encbytes);
	}
	
	static SFEKey decodeKey(BigInteger n) {
		byte[] bytes = n.toByteArray();
		byte[] decbytes = new byte[bytes.length-1];
		System.arraycopy(bytes, 1, decbytes, 0, decbytes.length);
		return new SFEKey(decbytes);
	}
}
