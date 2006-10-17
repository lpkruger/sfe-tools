package sfe.bdd.compiler;


import java.util.*;

import sfe.bdd.bdd.BDD;
import sfe.bdd.bdd.BDD.BaseNode;
import sfe.bdd.bdd.BDD.DNode;
import sfe.bdd.bdd.BDD.TNode;
import sfe.shdl.Circuit;
import sfe.shdl.CircuitWriter;
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
		Circuit cc = z.compile(bdd);
		
		if (System.getProperty("O") != null) {
			System.err.println("Optimizing...");
			z.optimize(cc);
			System.err.println("Renumbering...");
			z.renumber(cc);
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
	
	
	//// optimizer
	
	public void optimize(Circuit cc) {
		Set<Gate> seen = new HashSet<Gate>();
		for (int i=0; i<cc.outputs.length; ++i) {
			cc.outputs[i] = (Output) optimizeGate(cc.outputs[i], seen);
			int con = isConst(cc.outputs[i]);
			if (con != 0) {
				cc.outputs[i].arity = 0;
				cc.outputs[i].inputs = new GateBase[0];
				cc.outputs[i].truthtab = new boolean[] {con==1};
			}
		}
		
	}
	
	void swap(boolean[] vv, int x, int y) {
		boolean tmp = vv[x];
		vv[x] = vv[y];
		vv[y] = tmp;
	}
	
	GateBase optimizeGate(Gate g, Set<Gate> seen) {
		if (seen.contains(g))
			return g;
		
		boolean delta = false;
		boolean again = false;
		for (int pass = 1; pass<=2; ++pass) {
			for (int i=0; i<g.arity; ++i) {
				int con = isConst(g.inputs[i]);
				if (con != 0) { 
					// constant propagation
					delta = true;
					--g.arity;
					g.truthtab = slice(g.truthtab, i, con==1);
					
					GateBase[] newinputs = new GateBase[g.arity];
					int k=0;
					for (int j=0; j<=g.arity; ++j) {
						if (j != i)
							newinputs[k++] = g.inputs[j];
					}
					g.inputs = newinputs;
					--i;
					continue;
				}
				
				if ((g.inputs[i] instanceof Gate) && ((Gate)g.inputs[i]).arity == 1) {
					Gate g2 = (Gate) g.inputs[i];
					//System.out.println("1child: " + g + " <- " + g2);
					// already know not constant
					boolean inv = g2.truthtab[0];
					GateBase g3 = g2.inputs[0];
					g.inputs[i] = g3;
					if (inv) {
						int i2 = 1;
						for (int j=0; j<g.arity-i-1; ++j)
							i2 <<= 1;
						int i3 = i2<<1;
						//System.out.println("i=" + i + "  i2="+i2 + "  i3="+i3);
						//System.out.println(pr(g.truthtab));
						for (int j=0; j<g.truthtab.length; ++j) {
							if (j%i3 < i2)
								swap(g.truthtab, j, j+i2);
						}
						//System.out.println(pr(g.truthtab));
					}
					--i;
					continue;	
				}
				
				for (int l=0; l<i; ++l) {
					if (g.inputs[i] == g.inputs[l]) {
						delta = true;
						--g.arity;
						g.truthtab = slice(g.truthtab, i, l);
						
						GateBase[] newinputs = new GateBase[g.arity];
						int k=0;
						for (int j=0; j<=g.arity; ++j) {
							if (j != i)
								newinputs[k++] = g.inputs[j];
						}
						g.inputs = newinputs;
						--i;
						continue;
					}
				}
			}
			
			if (g.arity == 2) {
				for (int i=0; i<g.arity; ++i) {
					if (g.inputs[i] instanceof Gate && ((Gate)g.inputs[i]).arity == 2) {
						// combine 2+2 -> 3
						Gate g2 = (Gate)g.inputs[i];
						g.inputs = new GateBase[] { g.inputs[1-i], g2.inputs[0], g2.inputs[1] };
						g.truthtab = new boolean[] {
							g2.truthtab[0] ? g.truthtab[1] : g.truthtab[0],
							g2.truthtab[1] ? g.truthtab[1] : g.truthtab[0],
						    g2.truthtab[2] ? g.truthtab[1] : g.truthtab[0],
						    g2.truthtab[3] ? g.truthtab[1] : g.truthtab[0],
						    g2.truthtab[0] ? g.truthtab[3] : g.truthtab[2],
						    g2.truthtab[1] ? g.truthtab[3] : g.truthtab[2],
							g2.truthtab[2] ? g.truthtab[3] : g.truthtab[2],
							g2.truthtab[3] ? g.truthtab[3] : g.truthtab[2]};
						g.arity = 3;
						break;
					}
				}
			}
			
			if (g.arity == 1 && !g.truthtab[0] && g.truthtab[1] && g.inputs[0] instanceof Gate) {
				Gate g2 = (Gate) g.inputs[0];	
				g.arity = g2.arity;
				g.inputs = new GateBase[g2.arity];
				System.arraycopy(g2.inputs, 0, g.inputs, 0, g2.arity);
				g.truthtab = new boolean[g2.truthtab.length];
				System.arraycopy(g2.truthtab, 0, g.truthtab, 0, g2.truthtab.length);
			}
			
			/*
			if (g.arity == 1) {
				// eliminate arity 1 nodes when possible
				if (g.truthtab[0] != g.truthtab[1]) {
					boolean inv = g.truthtab[0];
					if (g.inputs[0] instanceof Gate) {
						Gate g2 = (Gate) g.inputs[0];	
						g.arity = g2.arity;
						g.inputs = g2.inputs;
						g.truthtab = g2.truthtab;
						if (inv)
							for (int i=0; i<g.truthtab.length; ++i)
								g.truthtab[i] = !g.truthtab[i];
						
						--pass;
						continue;
					} else {
						if (!(g instanceof Output) && g.truthtab[0] == true)
							return g.inputs[0];
					}
				} else {
					return g;
				}
			}
            */
			
			if (pass == 2) break;
			
			for (int i=0; i<g.arity; ++i) {
				if (g.inputs[i] instanceof Gate)
					g.inputs[i] = optimizeGate((Gate)g.inputs[i], seen);
			}
		}
		
		seen.add(g);
		return g;
	}
	
	// 0 if not constant, 1 if true, -1 if false
	int isConst(GateBase gg) {
		if (gg instanceof Input)
			return 0;
		
		Gate g = (Gate) gg;
		boolean v0 = g.truthtab[0];
		
		for (int i=1; i<g.truthtab.length; ++i)
			if (g.truthtab[i] != v0)
				return 0;
		
		return v0 ? 1 : -1;
	}
	
	boolean[] slice(boolean[] tt, int pos, int pos2) {
		// "partial evaluation" of a single truth table
		boolean[] ntt = new boolean[tt.length >> 1];
		
		int l = 0;
		// TODO: should this be <= or < ???
		for (int i=1; i<=tt.length; i<<=2)
			++l;
		int q = l - pos;
		int q2 = l - pos2;
		
		int j=0;
		for (int i=0; i<tt.length; ++i) {
			if (((i>>q)&1) == ((i>>q2)&1)) {
				ntt[j++] = tt[i];
			}
		}
/*	
		System.out.println("slice pos = " + pos + " val = " + val);
		System.out.println(pr(tt));
		System.out.println(pr(ntt));
*/
		
		return ntt;
	}
	
	boolean[] slice(boolean[] tt, int pos, boolean val) {
		// "partial evaluation" of a single truth table
		boolean[] ntt = new boolean[tt.length >> 1];
		
		int q = tt.length;
		for (int i=0; i<=pos; ++i)
			q >>= 1;
		
		int j = 0;
		for (int i=0; i<tt.length; ++i) {
			if (i%q == 0)
				val = !val;
			if (val) {
				ntt[j++] = tt[i];
			}
		}
/*	
		System.out.println("slice pos = " + pos + " val = " + val);
		System.out.println(pr(tt));
		System.out.println(pr(ntt));
*/
		
		return ntt;
	}
	
	String pr(boolean[] vv) {
		String sp = "";
		StringBuffer sb = new StringBuffer();
		for (boolean v : vv) {
			sb.append(sp).append(v ? "1" : "0");
			sp = " ";
		}
		return sb.toString();
	}
	
	void renumber(Circuit cc) {
		Set<Gate> seen = new HashSet<Gate>();
		curId = cc.inputs.length;
		//Gate[] outputs = new Gate[cc.outputs.length];
		for (int i=0; i<cc.outputs.length; ++i) {
			renumberGate(cc.outputs[i], seen);
		}
		/*
		for (int i=0; i<cc.outputs.length; ++i) {
			cc.outputs[i].id = getID();
		}
		*/
	}

	void renumberGate(Gate g, Set<Gate> seen) {
		if (seen.contains(g))
			return;
	
		for (int i=0; i<g.arity; ++i) {
			if (g.inputs[i] instanceof Gate) {
				Gate gg = (Gate) g.inputs[i];
				renumberGate(gg, seen);
			}
		}
		
		//System.out.println("Renumber gate from " + g.id + " to ");
		//if (!(g instanceof Output)) {
			g.id = getID();
			//g.write(System.out);
			seen.add(g);
		//}
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
