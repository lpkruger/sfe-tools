package sfe.editdistsw;

import java.io.*;
import java.math.BigInteger;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.IntBuffer;
import java.util.Random;
import java.util.TreeMap;
import java.util.TreeSet;

import sfe.shdl.*;
import sfe.util.*;
// import sfe.bdd.proto.Protocol;
import sfe.crypto.*;
import sfe.editdist2.BlindPermuteMin;

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


// TODO:  gap penalty of -4 is hardcoded

public class SWProto2 {
	
	static void D(Object s) {
		System.out.println(s);
	}
	
	static BigInteger TWO = BigInteger.ONE.add(BigInteger.ONE);
	static BigInteger MINUSFOUR = BigInteger.valueOf(-4);
	static BigInteger QQQ = TWO.pow(128).nextProbablePrime();
	static BigInteger GGG = OT.findGenerator(QQQ);
	
	static int N_BITS=80;
	static {
		String nBitsStr = System.getProperty("NBITS");
		if (nBitsStr != null) {
			N_BITS = Integer.parseInt(nBitsStr);
		}
	}
	static final BigInteger MAX_BIGINT = TWO.pow(N_BITS).subtract(BigInteger.ONE);
	
	static final boolean use_fairplay = false;
	static boolean use_circuitonly = (System.getProperty("CIRCUITONLY") != null);
	
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
	
		/*
		Domain domain = new Domain(-20, 20,
	               java.math.BigInteger.valueOf(3).pow(32),
	               BigInteger.valueOf(500));
		*/
		
		String str1;
		
		SmithW sw;
		
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
			sw = new SmithW();
			sw.populateDB();
			
			int astrlen = str1.length();
			out.writeInt(astrlen);
			out.flush();
			
			int bstrlen = in.readInt();
			aState = new BigInteger[astrlen+1][bstrlen+1];
			
			// initial split: Alice gets values, Bob gets 0's
			
			for (int i=0; i<=astrlen; ++i) {
				aState[i][0] = BigInteger.valueOf(-4*i);
			}
			for (int j=1; j<=bstrlen; ++j) {
				aState[0][j] = BigInteger.valueOf(-4*j);
			}
			
			//D("write domain");
			//out.writeObject(domain);
			//out.flush();
			
			for (int i=1; i<=astrlen; ++i) {
				for (int j=1; j<=bstrlen; ++j) {
					System.out.println("(" + i + "," + j + ")");
					if (use_circuitonly) {
						computeRecurrenceCircuit(i,j);
					} else {
						computeRecurrence(i,j);
					}
					out.reset();
				}
			}
			
			System.out.println();
			System.out.println("Alice result:" + aState[astrlen][bstrlen]);
			
			System.out.println("Alice wrote " + byteCount.cnt + " bytes");
		}
		
		
		void computeRecurrenceCircuit (int i, int j) throws Exception {
			out.flush();
			
			ByteCountObjectOutputStream out = ByteCountObjectOutputStream.wrapObjectStream(this.out);
			out.flush();
			ObjectInputStream in = new ObjectInputStream(this.in);
			
			D("prepare circuit");
			// evaluate min circuit
			Circuit circuit = CircuitParser.readFile("editdist/proto2a_" + N_BITS + ".txt.Opt.circuit");
			FmtFile fmt = FmtFile.readFile("editdist/proto2a_" + N_BITS + ".txt.Opt.fmt");
			VarDesc bdv = fmt.getVarDesc();
			VarDesc aliceVars = bdv.filter("A");
			VarDesc bobVars = bdv.filter("B");
			
			TreeMap<Integer,Boolean> vals = new TreeMap<Integer,Boolean>();
			
			BigInteger c0 = aState[i-1][j-1];
			BigInteger b0 = aState[i-1][j].add(MINUSFOUR);
			BigInteger a0 = aState[i][j-1].add(MINUSFOUR);
			BigInteger r0 = getRandom();
			BigInteger x0 = BigInteger.valueOf(str1.charAt(i-1) & 0xff);
			
			aState[i][j] = r0;
			
			fmt.mapBits(x0, vals, "input.alice.x");
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
		}
		
		void computeRecurrencePurdue(int i, int j) throws Exception {
			BigInteger NUM = BigInteger.valueOf(3).pow(64);
			//HomomorphicCipher.DecKey prkey = DPE.genKey(NUM, NUM.bitLength());
			//HomomorphicCipher.DecKey prkey = DPE.genKey(domain.dint, domain.dint.bitLength());
			HomomorphicCipher.DecKey prkey = Paillier.genKey(256);
			HomomorphicCipher.EncKey pubkey = prkey.encKey();
			
			ElGamal.DecKey egprkey = ElGamal.genKey(128);
			ElGamal.EncKey egpubkey = egprkey.encKey();
			out.writeObject(pubkey);
			out.writeObject(egpubkey);
			
			out.flush();

			HomomorphicCipher.EncKey bobkey = 
				(HomomorphicCipher.EncKey) in.readObject();
			
			// do OT
			byte thisbyte = (byte) str1.charAt(i-1);
			OTN.Chooser chooser = new OTN.Chooser(thisbyte, QQQ, GGG);
			chooser.setStreams(in, out);
			D("Do OT, choose " + thisbyte);
			BigInteger aa0 = chooser.go();

			D("OT read: " + aa0);

			BigInteger[] ary = { aa0, 
					aState[i-1][j].add(BigInteger.ONE),
					aState[i][j-1].add(BigInteger.ONE)};
	
			D("3vals: " + ary[0] + " " + ary[1] + " " + ary[2]);
			
			BlindPermuteMin.Alice malice = new BlindPermuteMin.Alice(in, out);
			malice.setKeys(pubkey, prkey, bobkey, egpubkey, egprkey);
			aState[i][j] = malice.go(ary);
			

			// for DEBUG
			BigInteger bobVal = (BigInteger) in.readObject();
			BigInteger combined = aState[i][j].add(bobVal).and(MAX_BIGINT);
			System.out.println("result after stage: " + combined);
			// end DEBUG
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
			
			// do OT
			int symbol = sw.char2num(str1.charAt(j-1));
			BigInteger[] choose = new BigInteger[sw.alphabetMap.size()];
		
			OTN.Chooser chooser = new OTN.Chooser(symbol, QQQ, GGG);
			chooser.setStreams(in, out);
			D("Do OT, choose " + symbol);
			BigInteger aa0 = chooser.go();
			
			D("OT read: " + aa0);
			
			// decrypt
			BigInteger a0 = prkey.decrypt(aa0);
			BigInteger b0 = aState[i-1][j].add(MINUSFOUR);
			BigInteger c0 = aState[i][j-1].add(MINUSFOUR);
			BigInteger r0 = getRandom().negate();
			//r0 = BigInteger.ONE.add(BigInteger.ONE).add(BigInteger.ONE);

			aState[i][j] = r0;

			D("prepare circuit");
			// evaluate max circuit
			Circuit circuit = CircuitParser.readFile("proto2b_" + N_BITS + ".circ");

			FmtFile fmt = FmtFile.readFile("proto2b_" + N_BITS + ".fmt");
			VarDesc bdv = fmt.getVarDesc();
			VarDesc aliceVars = bdv.filter("A");
			VarDesc bobVars = bdv.filter("B");

			TreeMap<Integer,Boolean> vals = new TreeMap<Integer,Boolean>();

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
			byte[] intbytes = combined.toByteArray();
			if (intbytes.length > N_BITS/8) { 
				// need to reinterpret negative numbers in 2s complement
				byte[] intbytes2 = new byte[N_BITS/8];
				System.arraycopy(intbytes, intbytes.length-intbytes2.length, 
						intbytes2, 0, intbytes2.length);
				combined = new BigInteger(intbytes2);
			}
			System.out.println("result after stage: " + combined);
			//System.out.println("  in binary: " + combined.toString(2));
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
		
		SmithW sw;
		
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
			sw = new SmithW();
			sw.populateDB();
			
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
					if (use_circuitonly) {
						computeRecurrenceCircuit(i,j);
					} else {
						computeRecurrence(i,j);
					}
					out.reset();
				}
			}
			
			System.out.println("Bob result:" + bState[astrlen][bstrlen]);
			
			System.out.println("Bob wrote " + byteCount.cnt + " bytes");
		}
		
		void computeRecurrenceCircuit(int i, int j) throws Exception {
			ByteCountObjectOutputStream out = ByteCountObjectOutputStream.wrapObjectStream(this.out);
			out.flush();
			ObjectInputStream in = new ObjectInputStream(this.in);
			
			FmtFile fmt = FmtFile.readFile("editdist/proto2a_" + N_BITS + ".txt.Opt.fmt");
			
			TreeMap<Integer,Boolean> vals = new TreeMap<Integer,Boolean>();
			
			BigInteger c1 = bState[i-1][j-1];
			BigInteger b1 = bState[i-1][j];
			BigInteger a1 = bState[i][j-1];
			BigInteger x0 = BigInteger.valueOf(str2.charAt(j-1) & 0xff);
			
			fmt.mapBits(x0, vals, "input.bob.x");
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
					
			BigInteger zz = fmt.readBits(cbob.result, "output.bob");
			
			bState[i][j] = zz;
			
			System.out.println("Bob Iteration wrote " + out.getCount() + " bytes");
			
//			 for DEBUG
			System.out.println("state eval: " + zz);
			out.writeObject(zz);
			out.flush();
			// end DEBUG
			
			
		}
		
		void computeRecurrencePurdue(int i, int j) throws Exception {
			int start_cnt = byteCount.cnt;
			
			BigInteger NUM = BigInteger.valueOf(3).pow(64);
			//HomomorphicCipher.DecKey prkey = DPE.genKey(NUM, NUM.bitLength());
			//HomomorphicCipher.DecKey prkey = DPE.genKey(domain.dint, domain.dint.bitLength());
			HomomorphicCipher.DecKey prkey = Paillier.genKey(256);
			HomomorphicCipher.EncKey pubkey = prkey.encKey();
			out.writeObject(pubkey);
			
			out.flush();
			D("read hkey");
			HomomorphicCipher.EncKey alicekey = 
				(HomomorphicCipher.EncKey) in.readObject();
			D("read egkey");
			ElGamal.EncKey egpubkey = 
				(ElGamal.EncKey) in.readObject();

			//  first, compute state[i-1][j-1] + t(i,j) - r with Alice

			// choose random R
			BigInteger r = getRandom();
			BigInteger val = bState[i-1][j-1].subtract(r);
			BigInteger val2 = val.add(BigInteger.ONE);
			
			D("val0 : " + val);
			D("val1 : " + val2);
			
			// prepare the OT
			byte thisbyte = (byte) str2.charAt(j-1);
			BigInteger[] choose = new BigInteger[256];
			for (int k=0; k<choose.length; ++k) {
				choose[k] = (k==thisbyte ? val : val2);
			}
			
			// do OT
			OTN.Sender sender = new OTN.Sender(choose, QQQ, GGG);
			sender.setStreams(in, out);
			sender.go();
			
		////////////
			
			BigInteger[] ary = {r,
					bState[i-1][j],
					bState[i][j-1]};
			D("3vals: " + ary[0] + " " + ary[1] + " " + ary[2]);
			
			BlindPermuteMin.Bob mbob = new BlindPermuteMin.Bob(in, out);
			mbob.setKeys(pubkey, prkey, alicekey, egpubkey);
			BigInteger zz = mbob.go(ary);
			
		////////////
			bState[i][j] = zz;
			
			System.out.println("Bob Iteration wrote " + 
					(byteCount.cnt-start_cnt) + " bytes");
			
			// for DEBUG
			System.out.println("state eval: " + zz);
			out.writeObject(zz);
			out.flush();
			// end DEBUG
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
			
			// prepare the OT
			int symbol = sw.char2num(str2.charAt(j-1));
			BigInteger[] choose = new BigInteger[sw.alphabetMap.size()];
			for (int k=0; k<choose.length; ++k) {
				BigInteger score = BigInteger.valueOf(sw.matrix[symbol][k]);
				choose[k] = pubkey.add(val, pubkey.encrypt(score));
			}
			
			// do OT
			OTN.Sender sender = new OTN.Sender(choose, QQQ, GGG);
			sender.setStreams(in, out);
			sender.go();
			
//			 run circuit
			BigInteger a1 = r;
			BigInteger b1 = bState[i-1][j];
			BigInteger c1 = bState[i][j-1];
			
			BigInteger zz = BigInteger.ZERO;
			

			FmtFile fmt = FmtFile.readFile("proto2b_" + N_BITS + ".fmt");
			TreeMap<Integer,Boolean> vals = new TreeMap<Integer,Boolean>();

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
}
