package sfe.bdd.compiler;


import java.util.*;

import sfe.bdd.bdd.BDD;
import sfe.bdd.bdd.BDD.BaseNode;
import sfe.bdd.bdd.BDD.DNode;
import sfe.bdd.bdd.BDD.TNode;
import sfe.shdl.Circuit;
import sfe.shdl.CircuitWriter;
import sfe.shdl.Optimizer;
import sfe.shdl.Circuit.Gate;
import sfe.shdl.Circuit.GateBase;
import sfe.shdl.Circuit.Input;
import sfe.shdl.Circuit.Output;


public class BDD2Circuit {
	int curId;
	
	int getID() {
		return curId++;
	}
	
	Gate newGate() {
		return new Gate(getID());
	}
	
	TreeMap<Integer, GateBase> inputGates = new TreeMap<Integer, GateBase>();
	HashMap<BaseNode, Gate> allGates = new HashMap<BaseNode, Gate>();
	
	public Circuit compile(BDD bdd) {
		Circuit circuit = new Circuit();
		
		circuit.inputs = new Input[bdd.levels];
		for (int i=0; i<bdd.levels; ++i) {
			int n = getID();
			circuit.inputs[i] = new Input(n, n);
			inputGates.put(n, circuit.inputs[i]);
		}

		Gate[] outputs = new Gate[bdd.root.length];
		for (int i=0; i<bdd.root.length; ++i) {
			BaseNode node = bdd.root[i];
			outputs[i] = node2Gate(node);
		}
		
		circuit.outputs = new Output[bdd.root.length];
		for (int i=0; i<bdd.root.length; ++i) {
			circuit.outputs[i] = makeOutput(outputs[i]);
		}
		
		return circuit;
	}
	
	//boolean[] ID = {false, true};
	//boolean[] MUX = {false, false, true, false, false, false, false, true};
	
	boolean[] ID() {
		return new boolean[] {false, true};
	}
	boolean[] MUX() {
		// if (0) then (2) else (1)
		return new boolean[] {false, false, true, true, false, true, false, true};
	}
	Output makeOutput(Gate g) {
		// create Arity 1 gate
		Output out = new Output(getID());
		out.arity = 1;
		out.inputs = new GateBase[] { g };
		out.truthtab = ID();
		return out;
	}
	
	Gate node2Gate(BaseNode node) {
		Gate g = allGates.get(node);
		if (g != null)
			return g;
		
		if (node instanceof TNode) {
			g = newGate();
			g.arity = 0;
			g.inputs = new GateBase[0];
			g.truthtab = new boolean[] { ((TNode)node).val };
			allGates.put(node, g);
			return g;
		}
		
		DNode cur = (DNode) node;
		Gate hi = node2Gate(cur.hi);
		Gate lo = node2Gate(cur.lo);
		Input var = (Input) inputGates.get(cur.var);
		
		g = newGate();
		g.arity = 3;
		g.inputs = new GateBase[] {var, lo, hi};
		g.truthtab = MUX();
		allGates.put(node, g);
		return g;
	}
	
	public static void main(String[] args) throws Exception {
		BDD bdd = BDD.readFile(args[0]);
		BDD2Circuit z = new BDD2Circuit();
		Optimizer opt = new Optimizer();
		Circuit cc = z.compile(bdd);
		
		if (System.getProperty("O") != null) {
			System.err.println("Optimizing...");
			opt.optimize(cc);
			System.err.println("Renumbering...");
			opt.renumber(cc);
		}
		
		CircuitWriter.write(cc);
		
		Random rand = new Random();
		
		if (System.getProperty("test") != null) {
			for (int i=0; i<10000; ++i) {
				boolean[] inputs = new boolean[cc.inputs.length];
				
				for (int j=0; j<inputs.length; ++j) {
					inputs[j] = rand.nextBoolean();
					//System.out.print((inputs[j]?1:0) + " ");
				}
				//System.out.println();
				
				for (int j=0; j<bdd.root.length; ++j) {
					boolean outcir = cc.evaln(j, inputs);
					boolean outbdd = eval(bdd.root[j], inputs);
					if (outcir != outbdd) {
						System.err.println("Answeres differed on output " + j + " ,  trial " + i);
						System.err.println("cir=" + outcir + "  bdd=" + outbdd);
						for (int k=0; k<inputs.length; ++k) {
							System.err.print((inputs[k]?1:0) + " ");
						}
						System.err.println();
						return;
					}
					//System.out.println("Circuit "+j+" evaluates to: " + cc.evaln(j, inputs));
					//System.out.println("    BDD "+j+" evaluates to: " + eval(bdd.root[j], inputs));
				}
			}
		}		
	}
	

	static public boolean eval(BaseNode node, boolean[] vals) {
		if (node instanceof TNode) {
			return ((TNode) node).val;
		}
		DNode cur = (DNode) node;
		if (vals[cur.var])
			return eval(cur.hi, vals);
		else
			return eval(cur.lo, vals);
	}
	
}
