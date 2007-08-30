package sfe.sfdl;

import java.util.HashSet;
import java.util.Set;

import sfe.shdl.Circuit;
import sfe.shdl.CircuitWriter;
import sfe.shdl.Circuit.Gate;
import sfe.shdl.Circuit.GateBase;

public class CircuitCompilerOutput implements CompilerOutput {
	static void write(GateBase[] cc) {
		for (int i=0; i<cc.length; ++i) {
			Set<Gate> seen = new HashSet<Gate>();
			System.out.println(i + ":");
			//CircuitWriter.writeGate((Gate)cc[i], seen);
		}
	}
	
	GateBase[] cc;  // can be null if circuitvar is non=null
	CircuitVar cv;  // is null if not a variable reference
	
	CircuitCompilerOutput(GateBase[] cc) {
		this.cc = cc;
		//Thread.dumpStack();
		//write(cc);
	}
	CircuitCompilerOutput(GateBase g) {
		this.cc = new GateBase[] { g };
	}
	CircuitCompilerOutput(CircuitVar var) {
		if (var instanceof CircuitVar.Leaf) {
			this.cc = ((CircuitVar.Leaf) var).cc;
		} else {
			this.cv = var;
		}
	}
	static class FunctionOutput extends CircuitCompilerOutput {
		Circuit circuit;
		FunctionOutput(Circuit circ) {
			super(circ.outputs);
			this.circuit = circ;
		}
	}
}
