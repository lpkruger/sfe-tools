package bdd.proto.singlebit;
import java.math.*;
//import static java.math.BigInteger.ZERO;
import static java.math.BigInteger.ONE;
import java.io.*;
import java.net.*;
import java.util.*;
import java.util.zip.*;

import javax.crypto.*;

/**
 * Protocol to securely evaluate BDD
 * Alice and Bob routines are included
 * 
 * @author lpkruger
 * 
 */
public class Protocol {
	
	static BigInteger TWO = ONE.add(ONE);
	static BigInteger QQQ = TWO.pow(129).nextProbablePrime();
	static BigInteger GGG = OT.findGenerator(QQQ);
	
	public static void main(String[] args) throws Exception {
		if (System.getProperty("BOB") != null) {
			Bob.main(args);
		} else if (System.getProperty("ALICE") != null) {
			Alice.main(args);
		} else System.out.println("Must use -DALICE or -DBOB");
	}
	public static class Alice {
		static boolean usePartialEval = true;
		
		Socket bob;
		ObjectInputStream in;
		ObjectOutputStream out;
		long startTime;
		BDD bdd;
		ByteCountOutputStreamSFE byteCount;
		
		Alice(String to, int port, BDD bdd) throws IOException {
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
			this.bdd = bdd;
		}
		
		public static void main(String[] args) throws Exception {
			String to = args[0];
			int port = Integer.parseInt(args[1]);
			String bddfile = args[2];
			String bdddescfile = args[3];
			
			Random rand = null;
			boolean randArgs = false;
			
			BDD bdd = BDD.readFile(bddfile);
			BDDVarDesc bdv = BDDVarDesc.readFile(bdddescfile);
			BDDVarDesc aliceVars = bdv.filter("A");
			BDDVarDesc bobVars = bdv.filter("B");
			
			if (args.length == 5 && args[4].equals("random")) {
				randArgs = true;
				rand = new Random();
			} else if (args.length-4 != aliceVars.who.size()) {
				throw new RuntimeException((args.length-4) + " != " + aliceVars.who.size());
			}
			Iterator<Integer> ait = aliceVars.who.keySet().iterator();
			TreeMap<Integer,Boolean> vals = new TreeMap<Integer,Boolean>();
			
			for (int i=0; i<aliceVars.who.size(); ++i) {
				if (randArgs) {
					vals.put(ait.next(), rand.nextBoolean());
				} else if (args[i+4].equals("1")) {
					vals.put(ait.next(), true);
				} else if (args[i+4].equals("0")) {
					vals.put(ait.next(), false);
				} else {
					System.out.println("args must be 1 or 0");
					return;
				}
			}
			
			new Alice(to, port, bdd).go(vals, new TreeSet<Integer>(aliceVars.who.keySet()),
					new TreeSet<Integer>(bobVars.who.keySet()));
		}
		
		void go(TreeMap<Integer,Boolean> vals, TreeSet<Integer> aliceVars, TreeSet<Integer> bobVars) throws Exception {
			BDDCrypt crypt = new BDDCrypt();
			if (usePartialEval)
				BDD.partialEval(bdd, vals);
			
			BDDCrypt.AliceData data = crypt.encryptBDD(bdd);
			
			ByteCountOutputStreamSFE.WRITE_MODE =
				ByteCountOutputStreamSFE.MODE_CIRCUIT;
			
			GZIPOutputStream gzout = new GZIPOutputStream(out); 
			ObjectOutputStream zout = new ObjectOutputStream(gzout);
					
			System.out.println("OBDD has " + data.obdd.nodes.size() + " nodes");
			
			zout.writeObject(data.obdd);
			zout.writeObject(data.rootsk);
			zout.writeObject(usePartialEval ? new TreeSet<Integer>() : aliceVars);
			zout.writeObject(bobVars);
			
			if (usePartialEval) {
				zout.writeObject(new BigInteger[0]);
			} else {
				BigInteger[] aliceVarsSK = new BigInteger[aliceVars.size()];
				int j = 0;
				for (int i=0; i<data.levsk.length; ++i) {
					if (aliceVars.contains(i)) {
						aliceVarsSK[j] = 
							BDDCrypt.keyToBigInt(vals.get(i) ? data.levsk[i][1] 
							                                                 : data.levsk[i][0]);
						j++;
					}
				}
				zout.writeObject(aliceVarsSK);
			}
			gzout.finish();
			out.flush();
			
			BigInteger[][] otarray = new BigInteger[bobVars.size()][2];
			int j = 0;
			for (int i=0; i<data.levsk.length; ++i) {
				if (bobVars.contains(i)) {
					otarray[j][0] = BDDCrypt.keyToBigInt(data.levsk[i][0]);
					otarray[j][1] = BDDCrypt.keyToBigInt(data.levsk[i][1]);
					j++;
				}
			}
			
			ByteCountOutputStreamSFE.WRITE_MODE =
				ByteCountOutputStreamSFE.MODE_OT;
			
			OTFairPlay.Sender send = new OTFairPlay.Sender(otarray);
			//OT.Sender send = new OT.Sender(otarray, QQQ, GGG);
			send.setStreams(in, out);
			send.go();
			out.flush();
			System.out.println("Alice done");
			System.out.println("Alice sent " + byteCount.cnt + " bytes");
			byteCount.printStats();
		}
	}
	
	public static class Bob {
		ServerSocket listen;
		Socket alice;
		ObjectInputStream in;
		ObjectOutputStream out;
		ByteCountOutputStreamSFE byteCount;
		long startTime;
		boolean[] vals;
		
		
		Bob(int port, boolean[] vals) throws IOException {
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
			
			this.vals = vals;
		}
		
		public static void main(String[] args) throws Exception {
			int port = Integer.parseInt(args[0]);
			
			boolean[] vals = null;
			
			if (!args[1].equals("random")) {
				vals = new boolean[args.length-1];
				for (int i=0; i<args.length-1; ++i) {
					if (args[i+1].equals("1"))
						vals[i] = true;
					else if (args[i+1].equals("0"))
						vals[i] = false;
					else {
						System.out.println("args must be 1 or 0");
						return;
					}
				}
			}
			
			new Bob(port, vals).go();
		}
		
		void go() throws Exception {
			OBDDEval crypt = new OBDDEval();
			
			ObjectInputStream zin = new ObjectInputStream(
					new GZIPInputStream(in));
			ObfuscatedBDD obdd = (ObfuscatedBDD) zin.readObject();
			SecretKey rootsk = (SecretKey) zin.readObject();
			TreeSet<Integer> aliceVars = (TreeSet<Integer>) zin.readObject();
			TreeSet<Integer> bobVars = (TreeSet<Integer>) zin.readObject();
			BigInteger[] aliceLevSK1 = (BigInteger[]) zin.readObject();
			
			int[] otarray;
			
			if (vals == null) { // use random arguments
				Random rand = new Random();
				otarray = new int[bobVars.size()];
				for (int i=0; i<otarray.length; ++i) {
					otarray[i] = rand.nextInt(2);
				}
			} else {
				otarray = new int[vals.length];
				if (otarray.length != bobVars.size()) {
					throw new RuntimeException(otarray.length + " != " + bobVars.size());
				}
				for (int i=0; i<otarray.length; ++i) {
					otarray[i] = vals[i] ? 1 : 0;
				}
			}
			
			ByteCountOutputStreamSFE.WRITE_MODE =
				ByteCountOutputStreamSFE.MODE_OT;
			
			//OT.Chooser choose = new OT.Chooser(otarray, QQQ, GGG);
			OTFairPlay.Chooser choose = new OTFairPlay.Chooser(otarray);
			choose.setStreams(in, out);
			BigInteger[] bobLevSK1 = choose.go();
			
			int keySize = crypt.KG.generateKey().getEncoded().length;
			TreeMap<Integer,SecretKey> levsk1 = new TreeMap<Integer,SecretKey>();
			
			int j = 0;
			for (int i : aliceVars) {
				levsk1.put(i, BDDCrypt.bigIntToKey(aliceLevSK1[j], keySize, crypt.CIPHER));
				j++;
			}
			j = 0;
			for (int i : bobVars) {
				levsk1.put(i, BDDCrypt.bigIntToKey(bobLevSK1[j], keySize, crypt.CIPHER));
				j++;
			}
			
			SecretKey[] levsk = new SecretKey[1+levsk1.lastKey()];
			for (Map.Entry<Integer,SecretKey> ent : levsk1.entrySet()) {
				levsk[ent.getKey()] = ent.getValue();
			}
			//SecretKey[] levsk = levsk1.values().toArray(new SecretKey[0]);
			
			System.out.println("Eval BDD");
			OBDDEval eval = new OBDDEval();
			boolean result = eval.eval(obdd, levsk, rootsk, vals);
			System.out.println("val = " + result);
			System.out.println();
			System.out.println("Bob sent " + byteCount.cnt + " bytes");
			byteCount.printStats();
		}	
	}
}
