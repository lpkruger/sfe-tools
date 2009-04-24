package sfe.shdl;

import java.util.*;
import java.io.PrintStream;

public class Circuit {

	public Input[] inputs;
	public Output[] outputs;

	static public abstract class GateBase {
		public int id;
		public HashSet<Gate> deps = new HashSet<Gate>();
		GateBase(int id) {
			this.id = id;
		}
		public abstract boolean eval(EvalState state);
		public abstract void write(PrintStream out);
		
		String comment;
		public void setComment(String s) {
			this.comment = s;
		}
		public String getComment() {
			return comment;
		}
		abstract GateBase copy_rec(Map<GateBase,GateBase> map);
	}
	
    class EvalState {
		HashMap<Integer,Boolean> vals;
		EvalState(boolean[] in) {
			vals = new HashMap<Integer,Boolean>();
			for (int i=0; i<in.length; ++i) {
				vals.put(inputs[i].id,in[i]);
			}
		}
	}
    
	public static class Input extends GateBase {
		int var;
		public Input(int id, int var) {
			super(id);
			this.var = var;
		}
		public String toString() {
			return "[Input " + id + " (" + var + ") ]";
		}
		public boolean eval(EvalState state) {
			Boolean b = state.vals.get(id);
			if (b == null) {
				throw new RuntimeException("Input " + id + " is undefined");
			}
			return b;
		}
		
		public void write(PrintStream out) {
			if (comment != null) {
				out.println(id + " input  // " + comment);
			} else {
				out.println(id + " input  // " + var);
			}
		}

		GateBase copy_rec(Map<GateBase,GateBase> map) {
			Input i = new Input(id, var);
			map.put(this, i);
			return i;
		}
	}
	
	public static class Gate extends GateBase {
		public int arity;
		public boolean[] truthtab;  // truth table
		public GateBase[] inputs;
		
		public Gate(int id) {
			super(id);
		}
		
		Gate(Gate g) {
			this(g.id);
			arity = g.arity;
			truthtab = g.truthtab;
			inputs = g.inputs;
			deps = g.deps;
		}

		public String toString() {
			return "[Gate " + id + " arity " + arity + "]";
		}

		public void write0(PrintStream out) {
			out.print(" arity " + arity + " table [ ");
			for (int i=0; i<truthtab.length; ++i) {
				out.print((truthtab[i] ? "1 " : "0 "));
			}
			out.print("] inputs [ ");
			for (int i=0; i<inputs.length; ++i) {
				//try {
					out.print(inputs[inputs.length-1-i].id + " ");
				//} catch (NullPointerException ex) {
				//	out.print("null ");
				//}
			}
			out.print("]");
			if (comment != null) {
				out.print(" // " + comment);
			}
			out.println();
			
		}
		public void write(PrintStream out) {
			out.print(id + " gate");
			write0(out);
		}
		public boolean eval(EvalState state) {
			Boolean out = state.vals.get(id);
			if (out != null)
				return out;
			
			boolean x=false,y=false,z=false;
			Boolean b = state.vals.get(id);
			if (b != null)
				return b;
			
			int n = 0;
			switch(arity) {
			case 3:
				z = inputs[2].eval(state);
				n = (z?1:0);
			case 2:
				y = inputs[1].eval(state);
				n += (y?1:0) << (arity - 2);
			case 1:
				x = inputs[0].eval(state);
				n += (x?1:0) << (arity - 1);
			}
			
			state.vals.put(id, truthtab[n]);
			return truthtab[n];
		}

		GateBase copy_rec(Map<GateBase,GateBase> map) {
			Gate g = new Gate(id);
			g.id = id;
			g.deps = new HashSet<Gate>();  // don't copy this, it can be recalculated
			g.arity = arity;
			g.truthtab = truthtab.clone();
			g.inputs = new GateBase[inputs.length];
			for (int i=0; i<inputs.length; ++i) {
				GateBase gg = map.get(inputs[i]);
				if (gg == null) {
					gg = inputs[i].copy_rec(map);
				}
				g.inputs[i] = gg;
			}
			map.put(this, g);
			return g;
		}
	}
	
	public static class Output extends Gate {
		public Output(int id) {
			super(id);
		}
		
		public Output(Gate g) {
			super(g.id);
			arity = g.arity;
			truthtab = g.truthtab;
			inputs = g.inputs;
			deps = g.deps;
		}

		public String toString() {
			return "[Output " + id + " arity " + arity + "]";
		}
		
		public void write(PrintStream out) {
			out.print(id + " output gate");
			write0(out);
		}
		
		GateBase copy_rec(Map<GateBase,GateBase> map) {
			Gate g = (Gate) super.copy_rec(map);
			Output o = new Output(g);
			map.put(this, o);
			return o;
		}
	}
	
	public boolean evaln(int n, boolean[] inputs) {
		EvalState state = new EvalState(inputs);
		return outputs[n].eval(state);
	}
	public boolean[] eval(boolean[] inputs) {
		boolean[] outs = new boolean[outputs.length];
		for (int i=0; i<outs.length; ++i) {
			outs[i] = evaln(i, inputs);
		}
		return outs;
	}
	
	public static void calcDeps(Circuit circuit) {
	    LinkedList<Gate> stack = new LinkedList<Gate>();
	    
	    for (int i=0; i<circuit.outputs.length; ++i) {
	    	stack.addFirst(circuit.outputs[i]);
	    }
	    
	    HashSet<Gate> allGates = new HashSet<Gate>();
	    
	    while (!stack.isEmpty()) {
	    	Gate gate = stack.removeLast();
	    	if (allGates.contains(gate))
	    		continue;
	    	
	    	for (GateBase inp : gate.inputs) {
	    		if (inp instanceof Gate) {
	    			stack.addFirst((Gate)inp);
	    		}
	    		inp.deps.add(gate);
	    		allGates.add(gate);
	    	}
	    }
	}
	
	public Circuit copy() {
		Circuit newcc = new Circuit();
		newcc.inputs = new Input[inputs.length];
		newcc.outputs = new Output[outputs.length];
		Map<GateBase,GateBase> seen = new HashMap<GateBase,GateBase>();
		for (int i=0; i<inputs.length; ++i) {
			newcc.inputs[i] = (Input) inputs[i].copy_rec(seen);
		}
		for (int i=0; i<outputs.length; ++i) {
			newcc.outputs[i] = (Output) outputs[i].copy_rec(seen);
		}
		return newcc;
	}
}
