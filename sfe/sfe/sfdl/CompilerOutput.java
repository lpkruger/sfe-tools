package sfe.sfdl;

import java.util.HashSet;
import java.util.Set;

import sfe.shdl.Circuit;
import sfe.shdl.CircuitWriter;
import sfe.shdl.Circuit.Gate;
import sfe.shdl.Circuit.GateBase;

public class CompilerOutput {
	static void write(GateBase[] cc) {
		for (int i=0; i<cc.length; ++i) {
			Set<Gate> seen = new HashSet<Gate>();
			System.out.println(i + ":");
			//CircuitWriter.writeGate((Gate)cc[i], seen);
		}
	}
	
	GateBase[] cc;
	CompilerOutput(GateBase[] cc) {
		this.cc = cc;
		//Thread.dumpStack();
		//write(cc);
	}
	CompilerOutput(GateBase g) {
		this.cc = new GateBase[] { g };
	}
	
	static class FunctionOutput extends CompilerOutput {
		Circuit circuit;
		FunctionOutput(Circuit circ) {
			super(circ.outputs);
			this.circuit = circ;
		}
	}
}
