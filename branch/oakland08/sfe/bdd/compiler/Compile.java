package sfe.bdd.compiler;

import net.sf.javabdd.*;

import java.util.*;

import sfe.shdl.Circuit;

/**
 * 
 * Boolean circuit to BDD transforming compiler
 * 
 * @author Louis
 *
 */
public class Compile {
	
	
	final static BDD evalUnary(BDD x, boolean[] tt) {
		BDDFactory fact = x.getFactory();
		if (tt[0] && tt[1])
			return fact.one();
		if (!tt[0] && !tt[1])
			return fact.zero();
		if (!tt[0] && tt[1])
			return x.id();
		return x.not();
	}
    
	/*
0   0 0 0 0 - zero()
1   0 0 0 1 - and
2   0 0 1 0 - diff  // set difference
3   0 0 1 1 - !X
4   0 1 0 0 - less  // less than
5   0 1 0 1 - Y
6   0 1 1 0 - xor
7   0 1 1 1 - or
8   1 0 0 0 - nor
9   1 0 0 1 - biimp   // bi-implication
10  1 0 1 0 - !Y
11  1 0 1 1 - invimp  // reverse-implication
12  1 1 0 0 - X
13  1 1 0 1 - imp     // implication
14  1 1 1 0 - nand
15  1 1 1 1 - one()
	 */

	final static BDD evalSpecialBinary(BDD x, BDD y, int n) {
		System.out.println("Binary special " + n);
		switch(n) {
		case 0: return x.getFactory().zero();
		case 1: return x.apply(y, BDDFactory.and);
		case 2: return x.apply(y, BDDFactory.diff);
		case 3: return x.not();
		case 4: return x.apply(y, BDDFactory.less);
		case 5: return y.id();
		case 6: return x.apply(y, BDDFactory.xor);
		case 7: return x.apply(y, BDDFactory.or);
		case 8: return x.apply(y, BDDFactory.nor);
		case 9: return x.apply(y, BDDFactory.biimp);
		case 10: return y.not();
		case 11: return x.apply(y, BDDFactory.invimp);
		case 12: return x.id();
		case 13: return x.apply(y, BDDFactory.imp);
		case 14: return x.apply(y, BDDFactory.nand);
		case 15: return x.getFactory().one();
		}
		return null;
	}
	// expansion:
	// f(x,y) = (f(0,y) & ~x) | (f(1,y) & yx
	// f(0,y) = f(0,0) & ~y | f(0,1) & y)
	// f(1,y) = f(1,0) & ~y | f(1,y) & y)
	
	final static BDD evalBinary(BDD x, BDD y, boolean[] tt) {
		return evalBinary(x,y,tt,0);
	}
	final static BDD evalBinary(BDD x, BDD y, boolean[] tt, int offs) {
        int n = 0;
        for (int i=offs; i<offs+4; ++i) {
        	n <<= 1;
        	if (tt[i]) n++;
        }
        //BDD special = evalSpecialBinary(x, y, n);
		//if (special != null) return special;
		
		BDDFactory fact = x.getFactory();
		BDD f00 = tt[0+offs] ? fact.one() : fact.zero();
		BDD f01 = tt[1+offs] ? fact.one() : fact.zero();
		BDD f10 = tt[2+offs] ? fact.one() : fact.zero();
		BDD f11 = tt[3+offs] ? fact.one() : fact.zero();
		
		BDD xnot = x.not();
		BDD ynot = y.not();
		BDD f00andynot = f00.and(ynot);
		BDD f01andy = f01.and(y);
		BDD f10andynot = f10.and(ynot);
		BDD f11andy = f11.and(y);
		
		BDD f0y = f00andynot.or(f01andy);
		BDD f1y = f10andynot.or(f11andy);
		
		BDD fxy = f0y.and(xnot).or(f1y.and(x));
		
		
		xnot.free();
		ynot.free();
		f00andynot.free();
		f01andy.free();
		f10andynot.free();
		f11andy.free();
		f0y.free();
		f1y.free();
		
		
		return fxy;
	}
	
	final static BDD evalSpecialTernary(BDD x, BDD y, BDD z, int n) {
		System.out.println("Ternary special " + n);
		switch(n) {
		// 0 0 0 1 0 1 1 1 = 23
		// ~x & y & z | x & (y | z)
		// y & z | x & (y or z)
		case 23:
			return y.and(z).or(x.and(y.or(z)));
		}
		return null;
	}
	
	// expansion:
	// f(x,y,z) = (f(0,y,z) & ~x) | (f(1,y,z) & x)
	final static BDD evalTernary(BDD x, BDD y, BDD z, boolean[] tt) {
		int n = 0;
        for (int i=0; i<0+8; ++i) {
        	n <<= 1;
        	if (tt[i]) n++;
        }
        //BDD special = evalSpecialTernary(x, y, z, n);
		//if (special != null) return special;
		
		BDD f0yz = evalBinary(y,z,tt,0);
		BDD f1yz = evalBinary(y,z,tt,4);
		BDD xnot = x.not();
		BDD f0yzandxnot = f0yz.and(xnot);
		BDD f1yzandx = f1yz.and(x);
		BDD fxyz = f0yzandxnot.or(f1yzandx);
		
		f0yz.free();
		f1yz.free();
		xnot.free();
		f0yzandxnot.free();
		f1yzandx.free();
		
		return fxyz;
	}
	
    static BDD[] compile(Circuit circuit, BDDFactory fact, boolean optimize) {
    	
    	//int num_evaluated = 0;  // for statkeeping and progress
    	
        HashMap<Circuit.GateBase, BDD> bmap = new HashMap<Circuit.GateBase, BDD>();
        
        fact.setVarNum(circuit.inputs.length);
        for (int i=0; i<circuit.inputs.length; ++i) {
        	bmap.put(circuit.inputs[i], fact.ithVar(i));
        }
        
        LinkedList<Circuit.Gate> stack = new LinkedList<Circuit.Gate>();
        
        for (int i=0; i<circuit.outputs.length; ++i) {
        	stack.addFirst(circuit.outputs[i]);
        }
        
        while (!stack.isEmpty()) {
        	Circuit.Gate gate = stack.removeLast();
        	boolean missinginput = false;
        	for (Circuit.GateBase inp : gate.inputs) {
        		if (!bmap.containsKey(inp)) {
        			if (!missinginput) {
        				stack.addLast(gate);
        				missinginput = true;
        			}
        			stack.addLast((Circuit.Gate)inp);
        		}
        	}
        	if (missinginput)
    			continue;
        
        	//System.out.println(gate);
        	// all inputs are satisfied, time to evaluate
        	BDD gbdd;
        	if (gate.inputs.length == 0) {
        	    gbdd = gate.truthtab[0] ? fact.one() : fact.zero();
        	} else if (gate.inputs.length == 1) {
        		gbdd = evalUnary(
        				bmap.get(gate.inputs[0]),
        				gate.truthtab);
        	} else if (gate.inputs.length == 2) {
        		gbdd = evalBinary(        				
        				bmap.get(gate.inputs[0]),
        				bmap.get(gate.inputs[1]),
        				gate.truthtab);
        	} else if (gate.inputs.length == 3) {
        		gbdd = evalTernary(        				
    				bmap.get(gate.inputs[0]),
    				bmap.get(gate.inputs[1]),
    				bmap.get(gate.inputs[2]),
    				gate.truthtab);
        	} else {
        		throw new RuntimeException("gate must have 1 - 3 inputs");      		
        	}
        
        	if (optimize) {
        		fact.reorder(BDDFactory.REORDER_SIFTITE);
        		int[] vorder = fact.getVarOrder();
        		for (int v : vorder) {
        			System.out.print(v + " ");
        		}
        		System.out.println();
        	}
        	//System.out.println("Evaluated gate " + (++num_evaluated));
        	System.out.print(".");
        	bmap.put(gate, gbdd);
        }
        
        for (Map.Entry<Circuit.GateBase, BDD> ent : bmap.entrySet()) {
        	if (ent.getKey() instanceof Circuit.Gate &&
        			!(ent.getKey() instanceof Circuit.Output)) {
        		ent.getValue().free();
        	}
        }
        
        BDD[] bout = new BDD[circuit.outputs.length];
        for (int i=0; i<circuit.outputs.length; ++i) {
        	bout[i] = bmap.get(circuit.outputs[i]);
        }
        
        return bout;
    }
    
    
    // experimental alternate implementation (more efficient)
    static BDD[] compile2(Circuit circuit, BDDFactory fact, int optimize) {
    	Circuit.calcDeps(circuit);
    	
    	int num_evaluated = 0;  // for statkeeping and progress
    	
        HashMap<Circuit.GateBase, BDD> bmap = new HashMap<Circuit.GateBase, BDD>();
        
        fact.setVarNum(circuit.inputs.length);
        for (int i=0; i<circuit.inputs.length; ++i) {
        	bmap.put(circuit.inputs[i], fact.ithVar(i));
        }
        
        LinkedList<Circuit.Gate> stack = new LinkedList<Circuit.Gate>();
        
        for (int i=0; i<circuit.outputs.length; ++i) {
        	stack.addLast(circuit.outputs[i]);
        }
        
        while (!stack.isEmpty()) {
        	Circuit.Gate gate = stack.removeLast();
        	
        	//System.out.println("consider gate: " + gate.id);
        	
        	if (bmap.containsKey(gate))
        		continue;
        	
        	boolean missinginput = false;
        	for (Circuit.GateBase inp : gate.inputs) {
        		if (!bmap.containsKey(inp)) {
        			if (!missinginput) {
        				stack.addLast(gate);
        				missinginput = true;
        			}
        			stack.addLast((Circuit.Gate)inp);
        		}
        	}
        	if (missinginput) {	
        		continue;
        	}
        
        	//System.out.println(gate);
        	// all inputs are satisfied, time to evaluate
        	BDD gbdd;
        	if (gate.inputs.length == 0) {
        	    gbdd = gate.truthtab[0] ? fact.one() : fact.zero();
        	} else if (gate.inputs.length == 1) {
        		gbdd = evalUnary(
        				bmap.get(gate.inputs[0]),
        				gate.truthtab);
        	} else if (gate.inputs.length == 2) {
        		gbdd = evalBinary(        				
        				bmap.get(gate.inputs[0]),
        				bmap.get(gate.inputs[1]),
        				gate.truthtab);
        	} else if (gate.inputs.length == 3) {
        		gbdd = evalTernary(        				
    				bmap.get(gate.inputs[0]),
    				bmap.get(gate.inputs[1]),
    				bmap.get(gate.inputs[2]),
    				gate.truthtab);
        	} else {
        		throw new RuntimeException("gate must have 1 - 3 inputs");      		
        	}
        
        	++num_evaluated;
        	
        	if (optimize>0 && (num_evaluated%optimize == 0)) {
        		System.out.println("Evaluated gate " + (num_evaluated));
        		fact.reorder(BDDFactory.REORDER_SIFTITE);
        		int[] vorder = fact.getVarOrder();
        		for (int v : vorder) {
        			System.out.print(v + " ");
        		}
        		System.out.println();
        	}
        
        	System.out.print(".");
        	bmap.put(gate, gbdd);
        	
        	// cleanup stage
        	
        	for(Circuit.GateBase ginp : gate.inputs) {
        		if (ginp instanceof Circuit.Gate) {
        			boolean alldeps = true;
        			for (Circuit.Gate gdep : ((Circuit.Gate)ginp).deps) {
        				if (!bmap.containsKey(gdep)) {
        					alldeps = false;
        				}
        			}
        			if (alldeps) {
        				//System.out.println("Free gate: " + ginp);
        				System.out.print("*");
        				bmap.get(ginp).free();
        				bmap.remove(ginp);
        			}
        		}
        	}
        }
        
        for (Map.Entry<Circuit.GateBase, BDD> ent : bmap.entrySet()) {
        	if (ent.getKey() instanceof Circuit.Gate &&
        			!(ent.getKey() instanceof Circuit.Output)) {
        		//System.out.println("Final free gate: " + ent.getKey());
        		ent.getValue().free();
        	}
        }
        
        BDD[] bout = new BDD[circuit.outputs.length];
        for (int i=0; i<circuit.outputs.length; ++i) {
        	bout[i] = bmap.get(circuit.outputs[i]).id();
        }
        
        return bout;
    }
}
