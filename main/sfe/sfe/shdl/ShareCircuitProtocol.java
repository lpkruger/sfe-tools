package sfe.shdl;

import java.io.*;
import java.math.BigInteger;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;

import sfe.crypto.*;
import sfe.shdl.Protocol.Alice;
import sfe.shdl.Protocol.Bob;
import sfe.util.ByteCountOutputStreamSFE;
import sfe.util.VarDesc;

import static java.math.BigInteger.ZERO;
import static java.math.BigInteger.ONE;

/**
 * 
 * @author lpkruger
 * "secure evaluation with shares"
 */

public class ShareCircuitProtocol {
	
	public static void main(String[] args) throws Exception {
		if (System.getProperty("BOB") != null) {
			Bob.main(args);
		} else if (System.getProperty("ALICE") != null) {
			Alice.main(args);
		} else System.out.println("Must use -DALICE or -DBOB");
	}

	static boolean isIt1(Circuit.Gate g, boolean t0, boolean t1) {
		return g.arity==1 && g.truthtab[0] == t0 && g.truthtab[1] == t1;
	}
	
	static boolean isIt2(Circuit.Gate g, boolean t0, boolean t1, boolean t2, boolean t3) {
		return g.arity==1 && g.truthtab[0] == t0 && g.truthtab[1] == t1 && g.truthtab[2] == t2 && g.truthtab[3] == t3;
	}
	
	static int boolAToInt(boolean[] bb) {
		int n = 0;
		for (int i=0; i<bb.length; ++i) {
			n*=2;
			if (bb[i])
				n++;
		}
		return n;	
	}
	static int getTTVal(Circuit.Gate g) {
		return boolAToInt(g.truthtab);
	}
	
	static class EvalStrategy {
		Circuit cc;
		LinkedList<Circuit.Gate> stack = new LinkedList<Circuit.Gate>();
		HashSet<Circuit.Gate> marked = new HashSet<Circuit.Gate>();
		
		EvalStrategy(Circuit cc) {
			this.cc = cc;
			
			for (int i=cc.outputs.length-1; i>=0; --i) {
				stack.addFirst(cc.outputs[i]);
			}
		}
		
		Circuit.Gate nextGate() {
			if (stack.isEmpty()) {
				return null;
			}
			
			Circuit.Gate g = stack.removeFirst();
			boolean next;
			
			do {
				next = false;

				for (int i=0; i<g.inputs.length; ++i) {
					if (g.inputs[i] instanceof Circuit.Gate && !marked.contains(g.inputs[i])) {
						stack.addFirst(g);
						g = (Circuit.Gate) g.inputs[i];
						next = true;
						break;
					}
				}
			} while (next);
			
			marked.add(g);
			return g;
		}
	}
	
	public static class Alice {
		Socket bob;
		ObjectInputStream in;
		ObjectOutputStream out;
		long startTime;
		Circuit cc;
		ByteCountOutputStreamSFE byteCount;
		
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
			long timeStart = System.currentTimeMillis();
			
			HashMap<Circuit.GateBase, Boolean> gatevals = new HashMap<Circuit.GateBase, Boolean>();
			
			for (int i=0; i<cc.inputs.length; ++i) {
				if (aliceVars.contains(i)) {
					//input[i] = vals.get(i);
					gatevals.put(cc.inputs[i], vals.get(i));
				} else {
					gatevals.put(cc.inputs[i], false);
				}
			}
			
			// start protocol
			EvalStrategy strat = new EvalStrategy(cc);
			Circuit.Gate g;
			
			do {
				g = strat.nextGate();
				if (g==null) break;
				
				System.out.println("Eval:");
				//System.out.println(g);
				g.write(System.out);
				
				boolean gvals[] = new boolean[g.inputs.length];
				for (int i=0; i<gvals.length; ++i) {
					gvals[i] = gatevals.get(g.inputs[i]);
					System.out.print(gvals[i] ? "1 " : "0 ");
				}
				gatevals.put(g, evalGate(g, gvals));
				System.out.println(" ->  " + (gatevals.get(g) ? "1" : "0"));
		
			} while (g != null);
			
			if (byteCount != null) {
				System.out.println();
				System.out.println("Alice sent " + byteCount.cnt + " bytes");
				//byteCount.printStats();
			}
			
			long timeEnd = System.currentTimeMillis();
			System.out.println("Eval time: " + (timeEnd - timeStart) / 1000.0);
		}
		
		boolean evalGate(Circuit.Gate g, boolean[] vals) {
			int ttval = getTTVal(g); /* truth table represented in binary */
			switch(g.arity) {
			
			case 1:
			// what is it?
				if (isIt1(g, false, true)) {
					return vals[0];
				} else if (isIt1(g, true, false)) {
					return !vals[0];
				}
				throw new RuntimeException("Unsupported gate " + g);
			case 2:
				switch(ttval) {
				case 6:  /* XOR */
					System.out.println("XOR");
					return vals[0] ^ vals[1];
				case 9:  /* XNOR */
					System.out.println("XNOR");
					return !(vals[0] ^ vals[1]);
				case 1:
				case 2:
				case 4:
				case 7:
				case 8:
				case 11:
				case 13:
				case 14:
					/* need to do OT */
					return evalGateOT2(g, vals);
				}
				throw new RuntimeException("Unsupported gate " + g);
			case 3:
				return evalGateOT3(g, vals);
				
			default:
				throw new RuntimeException("Unsupported arity " + g.arity);
			}
		}
		
		boolean evalGateOT2(Circuit.Gate g, boolean[] vals) {
	
			boolean[] bobout = new boolean[4];
			
			// setup 1-of-4 OT
			bobout[0] = g.truthtab[boolAToInt(vals)];
			vals[1] = !vals[1];
			bobout[1] = g.truthtab[boolAToInt(vals)];
			vals[0] = !vals[0];
			bobout[3] = g.truthtab[boolAToInt(vals)];
			vals[1] = !vals[1];
			bobout[2] = g.truthtab[boolAToInt(vals)];
			vals[0] = !vals[0];
			
			Random rand = new Random();
			
			BigInteger[] M = new BigInteger[4];
			
			// assign shares randomly
			boolean aliceout = rand.nextBoolean();

			// START DEBUG
			//aliceout = false;
			// ENV DEBUG
			
			for (int i=0; i<4; ++i) {
				bobout[i] = bobout[i] ^ aliceout;
				M[i] = bobout[i] ? ONE : ZERO;
			}
			
			OTN.Sender sender = new OTN.Sender(M, OT.QQQ, OT.GGG);
			sender.setStreams(in, out);
			
			try {
				sender.go();
			} catch (Exception ex) {
				ex.printStackTrace();
				throw new RuntimeException(ex);
			}
			
			return aliceout;
		}
		
		boolean evalGateOT3(Circuit.Gate g, boolean[] vals) {
			boolean[] bobout = new boolean[8];
			
			// setup 1-of-8 OT
			bobout[0] = g.truthtab[boolAToInt(vals)];
			vals[2] = !vals[2];
			bobout[1] = g.truthtab[boolAToInt(vals)];
			vals[1] = !vals[1];
			bobout[3] = g.truthtab[boolAToInt(vals)];
			vals[2] = !vals[2];
			bobout[2] = g.truthtab[boolAToInt(vals)];
			vals[1] = !vals[1];
			
			vals[0] = !vals[0];
		
			bobout[4] = g.truthtab[boolAToInt(vals)];
			vals[2] = !vals[2];
			bobout[5] = g.truthtab[boolAToInt(vals)];
			vals[1] = !vals[1];
			bobout[7] = g.truthtab[boolAToInt(vals)];
			vals[2] = !vals[2];
			bobout[6] = g.truthtab[boolAToInt(vals)];
			vals[1] = !vals[1];
			
			vals[0] = !vals[0];
			
			Random rand = new Random();
			
			BigInteger[] M = new BigInteger[8];
			
			// assign shares randomly
			boolean aliceout = rand.nextBoolean();
			
			// START DEBUG
			//aliceout = false;
			// ENV DEBUG
			
			
			for (int i=0; i<8; ++i) {
				bobout[i] = bobout[i] ^ aliceout;
				M[i] = bobout[i] ? ONE : ZERO;
			}
			
			OTN.Sender sender = new OTN.Sender(M, OT.QQQ, OT.GGG);
			sender.setStreams(in, out);
			
			try {
				sender.go();
			} catch (Exception ex) {
				ex.printStackTrace();
				throw new RuntimeException(ex);
			}
			
			return aliceout;
		}
	}
	
	
	public static class Bob {
		ServerSocket listen;
		Socket alice;
		ObjectInputStream in;
		ObjectOutputStream out;
		ByteCountOutputStreamSFE byteCount;
		long startTime;
		Circuit cc;
		public boolean[] result;
		
		Bob(int port, Circuit cc) throws IOException {
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
			
			this.cc = cc;
		}
		
		public static void main(String[] args) throws Exception {
			int port = Integer.parseInt(args[0]);
			
			String bddfile = args[1];
			String bdddescfile = args[2];
			
			Random rand = null;
			boolean randArgs = false;
			
			Circuit cc = CircuitParser.readFile(bddfile);
			VarDesc bdv = VarDesc.readFile(bdddescfile);
			VarDesc aliceVars = bdv.filter("A");
			VarDesc bobVars = bdv.filter("B");
			
			if (args.length == 4 && args[3].equals("random")) {
				randArgs = true;
				rand = new Random();
			} else if (args.length-3 != bobVars.who.size()) {
				throw new RuntimeException((args.length-3) + " != " + bobVars.who.size());
			}
			
			Iterator<Integer> bit = bobVars.who.keySet().iterator();
			TreeMap<Integer,Boolean> vals = new TreeMap<Integer,Boolean>();
			
			for (int i=0; i<bobVars.who.size(); ++i) {
				if (randArgs) {
					vals.put(bit.next(), rand.nextBoolean());
				} else if (args[i+3].equals("1")) {
					vals.put(bit.next(), true);
				} else if (args[i+3].equals("0")) {
					vals.put(bit.next(), false);
				} else {
					System.out.println("args must be 1 or 0");
					return;
				}
			}
				
			Bob cbob = new Bob(port, cc);
			cbob.go(vals, new TreeSet<Integer>(aliceVars.who.keySet()),
					new TreeSet<Integer>(bobVars.who.keySet()));
			
			/*
			for (int i=0; i<cbob.result.length; ++i) {
				System.out.println("val" + i + " = " + cbob.result[i]);
			}
			*/
		}
		
		public void go(TreeMap<Integer,Boolean> vals, TreeSet<Integer> aliceVars, TreeSet<Integer> bobVars) throws Exception {
			long timeStart = System.currentTimeMillis();
			
			// init Alice inputs to 0, Bob inputs to value
			HashMap<Circuit.GateBase, Boolean> gatevals = new HashMap<Circuit.GateBase, Boolean>();
			
			for (int i=0; i<cc.inputs.length; ++i) {
				if (bobVars.contains(i)) {
					gatevals.put(cc.inputs[i], vals.get(i));
				} else {
					gatevals.put(cc.inputs[i], false);
				}
			}
			
			// start protocol
			EvalStrategy strat = new EvalStrategy(cc);
			Circuit.Gate g;
			
			do {
				g = strat.nextGate();
				if (g==null) break;
				
				System.out.println("Eval:");
				//System.out.println(g);
				g.write(System.out);
				
				boolean gvals[] = new boolean[g.inputs.length];
				for (int i=0; i<gvals.length; ++i) {
					gvals[i] = gatevals.get(g.inputs[i]);
					System.out.print(gvals[i] ? "1 " : "0 ");
				}
				gatevals.put(g, evalGate(g, gvals));
				System.out.println(" ->  " + (gatevals.get(g) ? "1" : "0"));
			
			} while (g != null);
			
			if (byteCount != null) {
				System.out.println();
				System.out.println("Bob sent " + byteCount.cnt + " bytes");
				//byteCount.printStats();
			}
			
			long timeEnd = System.currentTimeMillis();
			System.out.println("Eval time: " + (timeEnd - timeStart) / 1000.0);
		}
		
		
		boolean evalGate(Circuit.Gate g, boolean[] vals) {
			int ttval = getTTVal(g); /* truth table represented in binary */
			switch(g.arity) {
			case 1:
				return vals[0];
			case 2:
				switch(ttval) {
				case 6:  /* XOR */
				case 9:  /* XNOR */
					System.out.println(ttval==6 ? "XOR" : "XNOR");
					return vals[0] ^ vals[1];
				case 1:
				case 2:
				case 4:
				case 7:
				case 8:
				case 11:
				case 13:
				case 14:
					/* need to do OT */
					return evalGateOT(g, vals);
				}
				throw new RuntimeException("Unsupported gate " + g);
			case 3:
				return evalGateOT(g, vals);
				
			default:
				throw new RuntimeException("Unsupported arity " + g.arity);
			}	
		}
		
		boolean evalGateOT(Circuit.Gate g, boolean[] vals) {
			int inval = boolAToInt(vals);
			
			OTN.Chooser chooser = new OTN.Chooser(inval, OT.QQQ, OT.GGG);
			chooser.setStreams(in, out);
			BigInteger N;
			
			try {
				N = chooser.go();
			} catch (Exception ex) {
				ex.printStackTrace();
				throw new RuntimeException(ex);
			}
			
			if (N.equals(ONE))
				return true;
			if (N.equals(ZERO))
				return false;
			
			throw new RuntimeException("N = " + N);
		}
	}
}
