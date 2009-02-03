package sfe.sfdl;

import java.io.*;
import java.util.*;
import sfe.shdl.Circuit;
import sfe.shdl.CircuitParser;
import sfe.shdl.Optimizer;
import sfe.shdl.Circuit.*;
import sfe.util.NullOutputStream;

// Write a dot file
public class TestGraph {

	public static void main(String[] args) throws Exception {
		if (args[0].endsWith(".circ")) {
			Circuit circ = CircuitParser.readFile(args[0]);
			new TestGraph().writeCircuit(circ);
			return;
		}
		
		PrintStream stdout = System.out;
		System.setOut(new PrintStream(new NullOutputStream()));
		
	
		BufferedReader r = new BufferedReader(new FileReader(args[0]));
		SFDLParser p = new SFDLParser(r);
		p.parse();
		r.close();
		
		CircuitCompiler comp = new CircuitCompiler();
		Circuit circ = comp.compile(p.sfdl, p.programname);
		boolean useopt = true;
		if (System.getProperty("NOOPT") != null)
			useopt = false;
		if (useopt) {
			Optimizer opt = new Optimizer();
			opt.optimize(circ);
			opt.renumber(circ);
		}
		
		System.setOut(stdout);
		new TestGraph().writeCircuit(circ);
	}
	
	PrintStream out = System.out;
	int nodeNo;
	Set<Integer> seen = new HashSet<Integer>();
	
	void writeCircuit(Circuit circ) {
		out.println("digraph circuit {");
		//uncomment for left->right
		//out.println("rankdir=\"LR\";");
		for (GateBase g : circ.outputs) {
			writeGate(g);
		}
		
		// make all inputs at same rank and in order
		out.println("edge [style = invis];");
		out.println("{rank=same; ");
		for (GateBase g : circ.inputs) {
			out.println("\"Node"+g.id+"\";");
		}
		out.println("}");
		out.print("Node"+circ.inputs[0].id);
		for (int i=1; i<circ.inputs.length; ++i) {
			out.print("->Node"+circ.inputs[i].id);
		}
		out.println();
		// same for outputs
		out.println("{rank=same; ");
		for (GateBase g : circ.outputs) {
			out.println("\"Node"+g.id+"\";");
		}
		out.println("}");
		
		out.print("Node"+circ.outputs[0].id);
		for (int i=1; i<circ.outputs.length; ++i) {
			out.print("->Node"+circ.outputs[i].id);
		}
		out.println();
		
		out.println("}");
	}
	
	void writeGate(GateBase g) {
		if (seen.contains(g.id)) {
			return;
		}
		
		int n = g.id;
		seen.add(n);
		out.println("Node" + n + " [");
		if (g instanceof Input) {
			out.println("label = \"Input " + n + "\",");
			out.println("shape = box");
			out.println("];");
			return;
		} 

		String shape = "box";
		String style = "\"\"";
		
		Gate gg = (Gate) g;
		StringBuffer labelbuf = new StringBuffer(gg.truthtab[0] ? "1" : "0");
		for (int i=1; i<gg.truthtab.length; ++i) {
			labelbuf.append(" ").append(gg.truthtab[i] ? "1" : "0");
		}
		String label = labelbuf.toString();
		
		if (label.equals("0 1 1 1") || label.equals("0 1 1 1 1 1 1 1"))
			// or gate
			shape = "invtrapezium";
		if (label.equals("0 0 0 1") || label.equals("0 0 0 0 0 0 0 1")) {
			// and gate
			shape = "invhouse";
			style = "rounded";
		}
		if (label.equals("0 1 1 0") || label.equals("0 1 1 0 1 0 0 1")) {
			// xor gate
			shape = "invtriangle";
			style = "diagonals";
		}
		if (label.equals("0 1 0 1 0 0 1 1")) {
			// mux gate
			shape = "parallelogram";
		}
		if (label.equals("0 1"))
			shape = "invtriangle";
		
		if (g instanceof Output) {
			out.println("label = \"Output " + g.id + "\\n" + label + "\",");
		} else {
			out.println("label = \"" + label + "\",");
		}
		out.println("shape = "+shape+",");
		out.println("style = "+style+",");
		out.println("];");	
		
		for (int i=0; i<gg.inputs.length; ++i) {
			GateBase g2 = gg.inputs[i];
			writeGate(g2);
			int n2 = g2.id;
			out.println("Node" + n2 + " -> Node" + n + " [");
			out.println("headlabel = " + i);
			out.println("];");
		}
	}
}