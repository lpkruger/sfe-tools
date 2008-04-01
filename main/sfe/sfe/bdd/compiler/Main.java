package sfe.bdd.compiler;

import java.io.*;
import java.util.Arrays;
import java.util.Random;

import sfe.shdl.Circuit;
import sfe.shdl.CircuitParser;

import net.sf.javabdd.*;

/**
 * 
 * Boolean circuit to BDD transforming compiler
 * 
 * @author Louis
 *
 */
public class Main {
	
	public static boolean eval(int n, Circuit c, boolean[] inputs) {
		return c.evaln(n, inputs);
	}
	
	public static boolean eval(BDD bdd, boolean[] inputs) {
		BDDFactory fact = bdd.getFactory();
		BDD constraints = fact.one();
		for (int i=0; i<inputs.length; ++i) {
			if (inputs[i])
				constraints = constraints.and(fact.ithVar(i));
			else 
				constraints = constraints.and(fact.nithVar(i));
				
		}
		BDD result = bdd.constrain(constraints);
		if (result.equals(fact.one()))
			return true;
		if (result.equals(fact.zero()))
			return false;
		System.out.println(result);
		throw new RuntimeException("Non-constant BDD");
	}
	
	public static void sift(BDDFactory fact) {
		int[] oldOrder = fact.getVarOrder();
	}
	
	public static void main(String[] args) throws Exception {
		String[] ordering = null;
		boolean optimize = false;
		int optimizeInterval = -1;
		String fname = null;
		String dotbase = null;
		String outputFile = null;
		if (true) {
			int i = 0;
			for (i=0; i<args.length; ++i) {
				if (args[i].equals("-n")) {
					ordering = args[++i].split(",");
				} else if (args[i].startsWith("-O")) {
					optimize = true;
					if (args[i].length() > 2) {
						optimizeInterval = Integer.parseInt(args[i].substring(2));
					} else
						optimizeInterval = Integer.MAX_VALUE;
				} else if (args[i].equals("-dot")) {
					dotbase = args[++i];
				} else if (args[i].equals("-o")) {
					outputFile = args[++i];
				} else break;
			}
			fname = args[i];
		}
		
		//BDDFactory fact = BDDFactory.init("j",1000,1000);
		BDDFactory fact = BDDFactory.init("buddy",5000000,1000000);
		BufferedReader in = new BufferedReader(new FileReader(fname));
		CircuitParser parser = new CircuitParser();
		Circuit cc = parser.parse(in);
		System.out.println(parser.varNo + " vars  " + cc.inputs.length + " inputs  " + cc.outputs.length + " outputs");
		System.out.println();
		
		fact.setVarNum(cc.inputs.length);
		
		if (ordering != null) {
			int[] pat = new int[ordering.length];
			for (int j=0; j<pat.length; ++j) {
				pat[j] = Integer.parseInt(ordering[j]);
			}
			fact.setVarOrder(Ordering.pattern(cc.inputs.length-1, pat));
		}
		
		if (optimize)
			fact.varBlockAll();
		
		//fact.autoReorder(BDDFactory.REORDER_WIN3);
		BDD[] bdd;
		if (System.getProperty("USEOLDCOMPILE") == null) {
			bdd = Compile.compile2(cc, fact, optimizeInterval);
		} else {
			bdd = Compile.compile(cc, fact, optimize);
		}
		
		System.out.println(fact.getGCStats());
		System.out.println("nodes: " + fact.getNodeNum());
		System.out.println();
		
		if (optimize)
			fact.reorder(BDDFactory.REORDER_SIFTITE);
		
		int[] vorder = fact.getVarOrder();
		for (int v : vorder) {
			System.out.print(v + " ");
		}
		System.out.println();
		System.out.println("nodes in BDDs: " + fact.nodeCount(Arrays.asList(bdd)));
		//System.out.println(fact.getGCStats());
		System.out.println("nodes in factory: " + fact.getNodeNum());
		System.out.println();
		
		PrintStream out = outputFile == null ? System.out : 
			new PrintStream(new BufferedOutputStream(
					new FileOutputStream(outputFile)));
		//BDDWriter.write(new PrintWriter(out), bdd[0]);
		BDDWriter.write(new PrintWriter(out), bdd);
		if (outputFile != null) out.close();
		
		if (dotbase != null) {
			PrintStream sysOut = System.out;
			OutputStream fs = new FileOutputStream(dotbase + ".dot");
			System.setOut(new PrintStream(fs));
			BDD.printDot(bdd);
			System.setOut(sysOut);
			fs.close();
			
			/*
			for (int i=0; i<bdd.length; ++i) {
				OutputStream fs = new FileOutputStream(dotbase + i + ".dot");
				System.setOut(new PrintStream(fs));
				bdd[i].printDot();
				System.setOut(sysOut);
				fs.close();
			}
			*/
		}
	    
		if (false) {
			boolean[] inputs = new boolean[cc.inputs.length];
			Random rand = new Random();
			for (int i=0; i<2; ++i) {
				for (int j=0; j<inputs.length; ++j) {
					inputs[j] = rand.nextBoolean();
					System.out.print((inputs[j]?1:0) + " ");
				}
				
				/*
				inputs = new boolean[] { false, false, true, false, false, false, false, true };
				if (i==1)
					inputs = new boolean[] { false, false, true, true, false, false, false, false };
				*/
				System.out.println();
				
				for (int j=0; j<bdd.length; ++j) {
					System.out.println("Circuit "+j+" evaluates to: " + eval(j, cc, inputs));
					System.out.println("    BDD "+j+" evaluates to: " + eval(bdd[j], inputs));
				}
			}
		}
	}
	
	public static void main2(String[] args) {
		new Main().go();
	}
	

	
	void go() {
		BDDFactory fact;
		fact = BDDFactory.init("j",1000,1000); 
		fact.setVarNum(2);
		BDD x = fact.ithVar(0);
		BDD y = fact.ithVar(1);
		System.out.println("X: " + x);
		System.out.println("Y: " + y);
		
		BDD z = x.or(y);
		System.out.println("Z: " + z);
	}
}
