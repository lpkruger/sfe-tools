package sfe.shdl;

import java.math.*;
//import static java.math.BigInteger.ZERO;
import static java.math.BigInteger.ONE;
import java.io.*;
import java.net.*;
import java.util.*;
import java.util.zip.*;

import javax.crypto.*;

import sfe.crypto.*;
import sfe.util.*;

/**
 * Protocol to securely evaluate BDD
 * Alice and Bob routines are included
 * 
 * @author lpkruger
 * 
 */
public class Protocol {
	
	//static BigInteger TWO = ONE.add(ONE);
	//static BigInteger QQQ = TWO.pow(129).nextProbablePrime();
	//static BigInteger GGG = OT.findGenerator(QQQ);
	
	static boolean useGZIP = false;  // TODO: buggy if true, fix
	
	public static void main(String[] args) throws Exception {
		if (System.getProperty("BOB") != null) {
			Bob.main(args);
		} else if (System.getProperty("ALICE") != null) {
			Alice.main(args);
		} else System.out.println("Must use -DALICE or -DBOB");
	}
	public static class Alice {
		static boolean usePartialEval = false; // MUST be false
	
		Socket bob;
		ObjectInputStream in;
		ObjectOutputStream out;
		long startTime;
		Circuit cc;
		ByteCountOutputStreamSFE byteCount;
		
		// special hack for EDProto3, April 2007 variation
		public SecretKey[] injectKeysAtInputZero = null;
		
		public Alice(ObjectInputStream in, ObjectOutputStream out, Circuit cc) {
			this.in = in;
			this.out = out;
			this.cc = cc;
		}
		
		Alice(String to, int port, Circuit cc) throws IOException {
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
			this.cc = cc;
		}
		
		public static void main(String[] args) throws Exception {
			String to = args[0];
			int port = Integer.parseInt(args[1]);
			String bddfile = args[2];
			String bdddescfile = args[3];
			
			Random rand = null;
			boolean randArgs = false;
			
			Circuit cc = CircuitParser.readFile(bddfile);
			VarDesc bdv = VarDesc.readFile(bdddescfile);
			VarDesc aliceVars = bdv.filter("A");
			VarDesc bobVars = bdv.filter("B");
			
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
			
			new Alice(to, port, cc).go(vals, new TreeSet<Integer>(aliceVars.who.keySet()),
					new TreeSet<Integer>(bobVars.who.keySet()));
		}
		
		public void go(TreeMap<Integer,Boolean> vals, TreeSet<Integer> aliceVars, TreeSet<Integer> bobVars) throws Exception {
			long timeCryptStart = System.currentTimeMillis();
			
			//CircuitCrypt crypt = new CircuitCrypt();
			CircuitCrypt crypt = new CircuitCryptPermute();
			
			// special hack for EDProto3
			if (injectKeysAtInputZero != null) {
				crypt.injectKeysAtInputZero = injectKeysAtInputZero;
			}
			CircuitCrypt.AliceData data = crypt.encrypt(cc);
			
			
			
			
			/*for (int i : aliceVars) {
				if (crypt.flip.get(cc.inputs[i]))
					vals.put(i, !vals.get(i));
			}*/
			
			
			ByteCountOutputStreamSFE.WRITE_MODE =
				ByteCountOutputStreamSFE.MODE_CIRCUIT;
			
			GZIPOutputStream gzout = useGZIP ? new GZIPOutputStream(out) : null; 
			ObjectOutputStream zout = useGZIP ? 
					new ObjectOutputStream(gzout) : out;
					
			/*
			System.out.println("OBDD has " + data.obdd.nodes.size() + " nodes");
			*/
					
			long timeCCStart = System.currentTimeMillis();
		
					
			//zout.writeObject(data.gcc);
			data.gcc.writeCircuit(zout);
			
			//zout.writeObject(data.rootsk);
			zout.writeObject(usePartialEval ? null : aliceVars);
			zout.writeObject(bobVars);
			
			if (usePartialEval) {
				zout.writeObject(new BigInteger[0]);
			} else {
				BigInteger[] aliceVarsSK = new BigInteger[aliceVars.size()];
				int j = 0;
				for (int i=0; i<data.inputSecrets.length; ++i) {
					if (aliceVars.contains(i)) {
						//System.out.println("alice[" + j + "] = vals[" + i + "] = " + vals.get(i));
						aliceVarsSK[j] = 
							SFEKey.keyToBigInt(vals.get(i) ? data.inputSecrets[i][1] 
							                                                 : data.inputSecrets[i][0]);
						j++;
					}
				}
				zout.writeObject(aliceVarsSK);
			}
			
			if (useGZIP)
				gzout.finish();
			
			out.flush();
			
			long timeOTStart = System.currentTimeMillis();
			
			BigInteger[][] otarray = new BigInteger[bobVars.size()][2];
			int j = 0;
			for (int i=0; i<data.inputSecrets.length; ++i) {
				if (bobVars.contains(i)) {
					//System.out.println("secrets " + i);
					//System.out.println(data.inputSecrets[i]);
					
					otarray[j][0] = SFEKey.keyToBigInt(data.inputSecrets[i][0]);
					otarray[j][1] = SFEKey.keyToBigInt(data.inputSecrets[i][1]);
					j++;
				}
			}
			
			ByteCountOutputStreamSFE.WRITE_MODE =
				ByteCountOutputStreamSFE.MODE_OT;
			
			//OTFairPlay.Sender send = new OTFairPlay.Sender(otarray);
			OT.Sender send = new OT.Sender(otarray, OT.QQQ, OT.GGG);
			send.setStreams(in, out);
			send.go();
			out.flush();
			long timeEnd = System.currentTimeMillis();
			
			System.out.println("Alice done");
			
			/*
			System.out.println("BDD orig size: " + bddOrigSize);
			System.out.println("BDD full size: " + bddFullSize);
			System.out.println("BDD eval size: " + bddEvalSize);
			System.out.println("BDD totl size: " + bddTotlSize);
			System.out.println("BDD send size: " + data.obdd.nodes.size());
			*/
		
			if (byteCount != null) {
				System.out.println("Alice sent " + byteCount.cnt + " bytes");
				byteCount.printStats();
			}
			
			if (false) {
				System.out.println("Crypt time: " + (timeCCStart - timeCryptStart) / 1000.0);
				System.out.println("CC    time: " + (timeOTStart - timeCCStart) / 1000.0);
				System.out.println("OT    time: " + (timeEnd - timeOTStart) / 1000.0);
			}
			
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
		public boolean[] result;
		

		// special hack for EDProto3, April 2007 variation
		public SecretKey injectKeyAtInputZero = null;
		
		public Bob(ObjectInputStream in, ObjectOutputStream out, boolean[] vals) {
			this.in = in;
			this.out = out;
			this.vals = vals;
		}
		
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
			
			Bob cbob = new Bob(port, vals);
			cbob.go();
			for (int i=0; i<cbob.result.length; ++i) {
				System.out.println("val" + i + " = " + cbob.result[i]);
			}
		}
		
		public void go() throws Exception {
			GCircuitEval crypt = new GCircuitEval();
			
			ObjectInputStream zin = useGZIP ? new ObjectInputStream(
					new GZIPInputStream(in)) : in;
			//GarbledCircuit gcc = (GarbledCircuit) zin.readObject();
			GarbledCircuit gcc = GarbledCircuit.readCircuit(zin);
				
			TreeSet<Integer> aliceVars = (TreeSet<Integer>) zin.readObject();
			if (aliceVars == null) {
				aliceVars = new TreeSet<Integer>();
			}
			TreeSet<Integer> bobVars = (TreeSet<Integer>) zin.readObject();
			BigInteger[] aliceInputSK1 = (BigInteger[]) zin.readObject();
			
			int[] otarray;
			
			if (vals == null) { // use random arguments
				System.out.println("Using random arguments for testing");
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
			
			long timeOTStart = System.currentTimeMillis();
			OT.Chooser choose = new OT.Chooser(otarray, OT.QQQ, OT.GGG);
			//OTFairPlay.Chooser choose = new OTFairPlay.Chooser(otarray);
			choose.setStreams(in, out);
			BigInteger[] bobInpSK1 = choose.go();
			
			long timeEvalStart = System.currentTimeMillis();
			
			int keySize = crypt.KG.generateKey().getEncoded().length;
			TreeMap<Integer,SecretKey> inputSK = new TreeMap<Integer,SecretKey>();

			int j = 0;
			for (int i : aliceVars) {
				inputSK.put(i, SFEKey.bigIntToKey(aliceInputSK1[j], keySize, crypt.CIPHER));
				j++;
			}
			j = 0;
			for (int i : bobVars) {
				inputSK.put(i, SFEKey.bigIntToKey(bobInpSK1[j], keySize, crypt.CIPHER));
				j++;
			}
			
			// special hack for EDProto3
			if (injectKeyAtInputZero != null) {
				inputSK.put(0, injectKeyAtInputZero);
			}
			
			SecretKey[] insk = new SecretKey[1+inputSK.lastKey()];
			for (Map.Entry<Integer,SecretKey> ent : inputSK.entrySet()) {
				insk[ent.getKey()] = ent.getValue();
			}
			//SecretKey[] levsk = levsk1.values().toArray(new SecretKey[0]);
			
			System.out.println("Eval Circuit");
			GCircuitEval eval = new GCircuitEval();
			result = eval.eval(gcc, insk);
			long timeEnd = System.currentTimeMillis();
			
			for (int i=0; i<result.length; ++i) {
				//System.out.println("val" + i + " = " + result[i]);
			}
			
			if (byteCount != null) {
				System.out.println();
				System.out.println("Bob sent " + byteCount.cnt + " bytes");
				byteCount.printStats();
			}
			System.out.println("OT   time: " + (timeEvalStart - timeOTStart) / 1000.0);
			System.out.println("Eval time: " + (timeEnd - timeEvalStart) / 1000.0);
		}	
	}
}
