package sfe.shdl;

import java.io.*;
import java.util.*;

import sfe.shdl.Circuit.Gate;

public class CircuitWriter {
	
	public static void write(Circuit cc) {
		Set<Gate> seen = new HashSet<Gate>();
		
		for (int i=0; i<cc.inputs.length; ++i) {
			cc.inputs[i].write(System.out);
		}
				
		//Gate[] outputs = new Gate[cc.outputs.length];
		for (int i=0; i<cc.outputs.length; ++i) {
			writeGate(cc.outputs[i], seen);
		}
		/*
		for (int i=0; i<cc.outputs.length; ++i) {
			cc.outputs[i].id = getID();
		}
		*/
	}

	static void writeGate(Gate g, Set<Gate> seen) {
		if (seen.contains(g))
			return;
	
		for (int i=0; i<g.arity; ++i) {
			if (g.inputs[i] instanceof Gate) {
				Gate gg = (Gate) g.inputs[i];
				writeGate(gg, seen);
			}	
		}
		
		g.write(System.out);
		seen.add(g);
	}
	
	static void write2(Circuit circuit) {
		
		//int num_evaluated = 0;  // for statkeeping and progress
		
		HashSet<Circuit.GateBase> seen = new HashSet<Circuit.GateBase>();
		LinkedList<Circuit.Gate> stack = new LinkedList<Circuit.Gate>();
		
		/*
		for (int i=0; i<circuit.inputs.length; ++i) {
			circuit.inputs[i].write(System.out);
		}
		*/
		
		for (int i=0; i<circuit.outputs.length; ++i) {
			stack.addFirst(circuit.outputs[i]);
		}
		
		while (!stack.isEmpty()) {
			Circuit.Gate gate = stack.removeLast();
			boolean missinginput = false;
			for (Circuit.GateBase inp : gate.inputs) {
				if (!seen.contains(inp)) {
					if (inp instanceof Circuit.Input) {
					    inp.write(System.out);
					    seen.add(inp);
					    continue;
					}
					if (!missinginput) {
						stack.addLast(gate);
						missinginput = true;
					}
					stack.addLast((Circuit.Gate)inp);
				}
			}
			if (missinginput)
				continue;
			
			if (seen.contains(gate))
				continue;
			
			// all inputs are satisfied, time to write
			gate.write(System.out);
			
			//System.out.print(".");
			seen.add(gate);
		}
	}
	
	public static void main(String[] args) throws IOException {
		BufferedReader in = new BufferedReader(new FileReader(args[0]));
		Circuit cc = new CircuitParser().parse(in);
		CircuitWriter.write(cc);
	}
}