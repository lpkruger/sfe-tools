package sfe.shdl;

import java.util.HashSet;
import java.util.Set;

import sfe.shdl.Circuit.*;


public class Optimizer {
	int curId;

	int getID() {
		return curId++;
	}

	
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
	
	Gate optimizeGate(Gate g, Set<Gate> seen) {
		if (seen.contains(g))
			return g;
		
		boolean delta = false;
		//boolean again = false;
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
					delta=true;
					continue;
				}
				
				// optimize case where input has arity 1 (and is not constant)
				if ((g.inputs[i] instanceof Gate) && ((Gate)g.inputs[i]).arity == 1) {
					Gate g2 = (Gate) g.inputs[i];
					//System.out.println("1child: " + g + " <- " + g2);
					// already know not constant, so is [0 1] or [1 0]
					if (g2.truthtab[0] == g2.truthtab[1])
						throw new RuntimeException("should not have constant gate");
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
					delta=true;
					continue;	
				}
				
				// optimize duplicate inputs
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
						delta=true;
						continue;
					}
				}
				
				// optimize irrelevant inputs
				boolean[] tt0 = slice(g.truthtab, i, false);
				boolean[] tt1 = slice(g.truthtab, i, true);
				boolean same = true;
				for (int l=0; l<tt0.length; ++l) {
					if (tt0[l] != tt1[l]) {
						same = false;
						break;
					}
				}
				if (same) {
					//System.out.println("same");
					--g.arity;
					g.truthtab = tt0;
					GateBase[] newinputs = new GateBase[g.arity];
					int k=0;
					for (int j=0; j<=g.arity; ++j) {
						if (j != i)
							newinputs[k++] = g.inputs[j];
					}
					g.inputs = newinputs;
					--i;
					delta=true;
					continue;
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
						delta=true;
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
				delta=true;
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
			
			if (delta) {
				delta = false;
				--pass;
				continue;
			}
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
		// if pos and pos2 are same variable
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
	
	public void renumber(Circuit cc) {
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
				sfe.shdl.Circuit.Gate gg = (Gate) g.inputs[i];
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
	
}
