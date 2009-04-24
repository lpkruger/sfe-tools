package sfe.shdl;

import java.math.*;
import java.io.*;
import java.net.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.*;
import java.util.zip.*;

import javax.crypto.*;

import sfe.crypto.*;
import sfe.shdl.CircuitCrypt.AliceData;
import sfe.util.*;

/**
 * Protocol to securely evaluate Yao circuit
 * Alice and Bob routines are included
 * 
 * @author lpkruger
 * 
 */
public class Protocol {
	
	//static BigInteger TWO = ONE.add(ONE);
	//static BigInteger QQQ = TWO.pow(129).nextProbablePrime();
	//static BigInteger GGG = OT.findGenerator(QQQ);
	
	static final boolean useGZIP = false;  // TODO: buggy if true, fix
	
	// setting this flag uses Goyal-Mohassel-Smith
	static final boolean useHashOpt = true;
	
	public static void main(String[] args) throws Exception {
		if (System.getProperty("BOB") != null) {
			Bob.main(args);
		} else if (System.getProperty("ALICE") != null) {
			Alice.main(args);
		} else System.out.println("Must use -DALICE or -DBOB");
	}
	public static class Alice {
		static boolean usePartialEval = false; // MUST be false
	
		SecureRandom random;
		Socket bob;
		ObjectInputStream in;
		ObjectOutputStream out;
		long startTime;
		Circuit cc;
		ByteCountOutputStreamSFE byteCount;
		int num_copies = 1;		
		
		// special hack for EDProto3, April 2007 variation
		public SecretKey[] injectKeysAtInputZero = null;
		
		public Alice(ObjectInputStream in, ObjectOutputStream out, Circuit cc, SecureRandom rand) {
			this.in = in;
			this.out = out;
			this.cc = cc;
			this.random = rand;
		}
		public void setStreams(ObjectInputStream in, ObjectOutputStream out) {
			this.in = in;
			this.out = out;
		}
		public void setNumCircuits(int n) {
			this.num_copies = n;
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
			
			SecureRandom rand = null;
			boolean randArgs = false;
			
			Circuit cc = CircuitParser.readFile(bddfile);
			VarDesc bdv = VarDesc.readFile(bdddescfile);
			VarDesc aliceVars = bdv.filter("A");
			VarDesc bobVars = bdv.filter("B");
			
			if (args.length == 5 && args[4].equals("random")) {
				randArgs = true;
				rand = new SecureRandom();
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
		
		public void goWithCutChoose(TreeMap<Integer,Boolean> vals, TreeSet<Integer> aliceVars, TreeSet<Integer> bobVars) throws Exception {
			cryptWithCutChoose(aliceVars, bobVars);
			onlineWithCutChoose(vals, aliceVars, bobVars);
		}
		
		//public CircuitCrypt.AliceData[] cryptWithCutChoose(TreeSet<Integer> aliceVars, TreeSet<Integer> bobVars) {
		AliceData[] data;
		OT.Sender send;
		byte[][] rngseeds;
		byte[][] hashes;
		
		public void cryptWithCutChoose(TreeSet<Integer> aliceVars, TreeSet<Integer> bobVars) {
			long timeCryptStart = System.currentTimeMillis();
			data = new CircuitCrypt.AliceData[num_copies];

			//CircuitCrypt crypt = new CircuitCrypt();
	
			CircuitCrypt crypt = new CircuitCryptPermute(random);
			MessageDigest hash = null;
			
			if (useHashOpt) {
				rngseeds = new byte[num_copies][20];
				hashes = new byte[num_copies][];
				try {
					hash = MessageDigest.getInstance("SHA-1");
				} catch (NoSuchAlgorithmException e) {
					e.printStackTrace();
				}
			}
			
			for (int i=0; i<data.length; ++i) {
				System.out.println("encrypt copy "+i);
				if (useHashOpt) {
					random.nextBytes(rngseeds[i]);
					SecureRandom rng = DeterministicRandom.getRandom(rngseeds[i]);
					crypt = new CircuitCryptPermute(rng);
				}
				
				Circuit ccc = cc.copy();	// the permutation scrambles the truth tables
				data[i] = crypt.encrypt(ccc);	
				
				if (useHashOpt) {
					hash.reset();
					data[i].gcc.hashCircuit(hash);
					hashes[i] = hash.digest();
				}
			}
			
			if (true) {
				System.out.println("Circuit crypt time: " + 
						(System.currentTimeMillis() - timeCryptStart) / 1000.0);
			}
			
			BigInteger[][] otarray = new BigInteger[num_copies*bobVars.size()][2];
			for (int copy=0; copy<num_copies; ++copy) {
				int j = 0;
				for (int i=0; i<data[copy].inputSecrets.length; ++i) {
					if (bobVars.contains(i)) {
						//System.out.println("secrets " + i);
						//System.out.println(data.inputSecrets[i]);

						otarray[copy*bobVars.size()+j][0] = SFEKey.keyToBigInt(data[copy].inputSecrets[i][0]);
						otarray[copy*bobVars.size()+j][1] = SFEKey.keyToBigInt(data[copy].inputSecrets[i][1]);
						j++;
					}
				}
			}
			send = new OT.Sender(otarray, OT.QQQ, OT.GGG);
			send.precalc();
		}
		
		public void onlineWithCutChoose(TreeMap<Integer,Boolean> vals, TreeSet<Integer> aliceVars, TreeSet<Integer> bobVars) throws Exception {
			
			
			// special hack for EDProto3
			if (injectKeysAtInputZero != null || usePartialEval) {
				throw new RuntimeException("not implemented");
			}
			
			
			
			GZIPOutputStream gzout = useGZIP ? new GZIPOutputStream(out) : null; 
			ObjectOutputStream zout = useGZIP ? 
					new ObjectOutputStream(gzout) : out;
					

			zout.writeObject(aliceVars);
			zout.writeObject(bobVars);
			zout.flush();

			long timeOTStart = System.currentTimeMillis();

			ByteCountOutputStreamSFE.WRITE_MODE =
				ByteCountOutputStreamSFE.MODE_OT;

			//OTFairPlay.Sender send = new OTFairPlay.Sender(otarray);
			
			send.setStreams(in, out);
			send.go();
			out.flush();
			//zout.writeObject(data.gcc);
			//zout.writeInt(data.length);  // protocol change : write num
			//zout.flush();
			
			long timeCCStart = System.currentTimeMillis();
			ByteCountOutputStreamSFE.WRITE_MODE =
				ByteCountOutputStreamSFE.MODE_CIRCUIT;
		
			
			if (useHashOpt) {
				// write all circuit hashes		
				for (int i=0; i<data.length; ++i) {	
					zout.write(hashes[i]);
				}
			} else {
				// write all circuits			
				for (int i=0; i<data.length; ++i) {
					System.out.println("sending copy "+i);
					data[i].gcc.writeCircuit(zout);
				}
			}

			zout.flush();
			
			// read selected
			int selected = in.readInt();
			//System.out.println("read selected circuit "+selected);

			if (useHashOpt) {
				// send the selected circuit, and the RNG seed for the others
				data[selected].gcc.writeCircuit(zout);
				rngseeds[selected] = null;
				zout.writeObject(rngseeds);
			} else {
				// send secrets for all non-selected
				for (int i=0; i<data.length; ++i) {
					if (i != selected)
						zout.writeObject(data[i].inputSecrets);
				}
			}
		
			BigInteger[] aliceVarsSK = new BigInteger[aliceVars.size()];
			int jj = 0;
			for (int i=0; i<data[selected].inputSecrets.length; ++i) {
				if (aliceVars.contains(i)) {
					//System.out.println("alice[" + j + "] = vals[" + i + "] = " + vals.get(i));
					aliceVarsSK[jj] = 
						SFEKey.keyToBigInt(vals.get(i) ? data[selected].inputSecrets[i][1] 
						                                                      : data[selected].inputSecrets[i][0]);
					jj++;
				}
			}
			zout.writeObject(aliceVarsSK);
			
			
			if (useGZIP)
				gzout.finish();
			
			out.flush();
			
			
			long timeEnd = System.currentTimeMillis();
			
			System.out.println("Alice done");
			
			if (byteCount != null) {
				System.out.println("Alice sent " + byteCount.cnt + " bytes");
				byteCount.printStats();
			}
			
			if (true) {
				//System.out.println("Crypt time: " + (timeCCStart - timeCryptStart) / 1000.0);
				System.out.println("OT    time: " + (timeCCStart - timeOTStart) / 1000.0);
				System.out.println("CC    time: " + (timeEnd - timeCCStart) / 1000.0);
			}	
		}
		
		public void go(TreeMap<Integer,Boolean> vals, TreeSet<Integer> aliceVars, TreeSet<Integer> bobVars) throws Exception {
			long timeCryptStart = System.currentTimeMillis();
			
			//CircuitCrypt crypt = new CircuitCrypt();
			System.out.println("random is "+random);
			CircuitCrypt crypt = new CircuitCryptPermute(random);
			
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
		SecureRandom random;
		ServerSocket listen;
		Socket alice;
		ObjectInputStream in;
		ObjectOutputStream out;
		ByteCountOutputStreamSFE byteCount;
		long startTime;
		boolean[] vals;
		public boolean[] result;
		int num_copies = 1;
		
		// special hack for EDProto3, April 2007 variation
		public SecretKey injectKeyAtInputZero = null;
		
		public Bob(ObjectInputStream in, ObjectOutputStream out, boolean[] vals, SecureRandom rand) {
			this.in = in;
			this.out = out;
			this.vals = vals;
			this.random = rand;
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
		
		public void setNumCircuits(int n) {
			this.num_copies = n;
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
		
		public void goWithCutChoose(Circuit cc) throws Exception {
			ObjectInputStream zin = useGZIP ? new ObjectInputStream(
					new GZIPInputStream(in)) : in;
			//GarbledCircuit gcc = (GarbledCircuit) zin.readObject();	
			//int numCircs = zin.readInt();
			int numCircs = num_copies;
				
			TreeSet<Integer> aliceVars = (TreeSet<Integer>) zin.readObject();
			if (aliceVars == null) {
				aliceVars = new TreeSet<Integer>();
			}
			TreeSet<Integer> bobVars = (TreeSet<Integer>) zin.readObject();
			
			int[] otarray0;
			
			if (vals == null) { // use random arguments
				System.out.println("Using random arguments for testing");
				otarray0 = new int[bobVars.size()];
				for (int i=0; i<otarray0.length; ++i) {
					otarray0[i] = random.nextInt(2);
				}
			} else {
				otarray0 = new int[vals.length];
				if (otarray0.length != bobVars.size()) {
					throw new RuntimeException(otarray0.length + " != " + bobVars.size());
				}
				for (int i=0; i<otarray0.length; ++i) {
					otarray0[i] = vals[i] ? 1 : 0;
				}
			}
			
			int[] otarray = new int[otarray0.length * num_copies];
			for (int copy=0; copy<num_copies; ++copy) {
				System.arraycopy(otarray0, 0, otarray, copy*otarray0.length,
						otarray0.length);
			}
			ByteCountOutputStreamSFE.WRITE_MODE =
				ByteCountOutputStreamSFE.MODE_OT;
			
			long timeOTStart = System.currentTimeMillis();
			OT.Chooser choose = new OT.Chooser(otarray, OT.QQQ, OT.GGG);
			//OTFairPlay.Chooser choose = new OTFairPlay.Chooser(otarray);
			choose.setStreams(in, out);
			BigInteger[] bobInpSK1all = choose.go();
	
			GarbledCircuit[] gcc = new GarbledCircuit[numCircs];
			byte[][] hashes = null;
			
			if (useHashOpt) {	
				MessageDigest hash = MessageDigest.getInstance("SHA-1");
				int hashlen = hash.digest().length;
				 hashes = new byte[numCircs][hashlen];
				for (int i=0; i<gcc.length; ++i) {
					zin.read(hashes[i]);
					System.out.println("read hash "+i);
				}
			} else {
				for (int i=0; i<gcc.length; ++i) {
					gcc[i] = GarbledCircuit.readCircuit(zin);
					System.out.println("read copy "+i);
				}
			}
			
			int selected = random.nextInt(numCircs);
			out.writeInt(selected);
			out.flush();
			
			long timeVrfyStart = System.currentTimeMillis();
			
			byte[][] rngseeds;
			if (useHashOpt) {
				// read the selected circuit, and the RNG seed for the others
				gcc[selected] = GarbledCircuit.readCircuit(zin);				
				rngseeds = (byte[][]) zin.readObject();
				// verify non-selected circuits
				MessageDigest hash = MessageDigest.getInstance("SHA-1");
				for (int i=0; i<gcc.length; ++i) {
					if (i != selected) {
						SecureRandom rng = DeterministicRandom.getRandom(rngseeds[i]);
						CircuitCryptPermute crypt = new CircuitCryptPermute(rng);
						hash.reset();
						crypt.encrypt(cc.copy()).gcc.hashCircuit(hash);
						byte[] hashval = hash.digest();
						
						if (!Arrays.equals(hashes[i], hashval)) {
							throw new RuntimeException("hash mismatch  "+
									Arrays.toString(hashes[i])+" != "+
									Arrays.toString(hashval));
						}
					}
				}
			} else {
				SecretKey[][][] inputSecrets = new SecretKey[numCircs][][];
				// read secrets for non-chosen circuits
				for (int i=0; i<gcc.length; ++i) {
					if (i != selected)
						inputSecrets[i] = (SecretKey[][]) in.readObject();
				}
				// verify non-selected circuits
				CircuitCryptPermute crypt = new CircuitCryptPermute(random);
				for (int i=0; i<gcc.length; ++i) {
					if (i != selected) {
						
					}
				}
			}
			
			BigInteger[] aliceInputSK1 = (BigInteger[]) zin.readObject();
			
			
			long timeEvalStart = System.currentTimeMillis();
			
			GCircuitEval gceval = new GCircuitEval();
			
			int keySize = gceval.KG.generateKey().getEncoded().length;
			TreeMap<Integer,SecretKey> inputSK = new TreeMap<Integer,SecretKey>();


			BigInteger[] bobInpSK1 = new BigInteger[otarray0.length];
			System.arraycopy(bobInpSK1all, selected*otarray0.length, bobInpSK1, 0, otarray0.length);
			
			int j = 0;
			for (int i : aliceVars) {
				inputSK.put(i, SFEKey.bigIntToKey(aliceInputSK1[j], keySize, gceval.CIPHER));
				j++;
			}
			j = 0;
			for (int i : bobVars) {
				inputSK.put(i, SFEKey.bigIntToKey(bobInpSK1[j], keySize, gceval.CIPHER));
				j++;
			}
			
			// special hack for EDProto3
			if (injectKeyAtInputZero != null) {
				//inputSK.put(0, injectKeyAtInputZero);
				throw new RuntimeException("not supported");
			}
			
			SecretKey[] insk = new SecretKey[1+inputSK.lastKey()];
			for (Map.Entry<Integer,SecretKey> ent : inputSK.entrySet()) {
				insk[ent.getKey()] = ent.getValue();
			}
			//SecretKey[] levsk = levsk1.values().toArray(new SecretKey[0]);
			
			System.out.println("Eval Circuit");
			GCircuitEval eval = new GCircuitEval();
			result = eval.eval(gcc[selected], insk);
			long timeEnd = System.currentTimeMillis();
			
			for (int i=0; i<result.length; ++i) {
				//System.out.println("val" + i + " = " + result[i]);
			}
			
			if (byteCount != null) {
				System.out.println();
				System.out.println("Bob sent " + byteCount.cnt + " bytes");
				byteCount.printStats();
			}

			System.out.println("OT   time: " + (timeVrfyStart - timeOTStart) / 1000.0);
			System.out.println("Vrfy time: " + (timeEvalStart - timeVrfyStart) / 1000.0);
			System.out.println("Eval time: " + (timeEnd - timeEvalStart) / 1000.0);
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
				otarray = new int[bobVars.size()];
				for (int i=0; i<otarray.length; ++i) {
					otarray[i] = random.nextInt(2);
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
