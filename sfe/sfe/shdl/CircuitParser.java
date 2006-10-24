package sfe.shdl;

import java.io.*;
import java.util.*;

public class CircuitParser {
	void check(boolean b) {
		if (!b) {
			System.out.println("Error on line " + lineNo);
			System.out.println(line);
			System.out.println();
			throw new RuntimeException();
		}
	}
	
	int lineNo;
	String line;
	
	public int varNo;
	
	public Circuit parse(BufferedReader r) throws IOException {
		ArrayList<Circuit.GateBase> gates = new ArrayList<Circuit.GateBase>();
		ArrayList<Circuit.Input> inputs = new ArrayList<Circuit.Input>();
		ArrayList<Circuit.Output> outputs = new ArrayList<Circuit.Output>();
		lineNo = 0;
		varNo = 0;
		int inputNo = 0;
		while ((line = r.readLine()) != null) {
			//System.out.println(line);
			++lineNo;
			line = line.trim();
			int comment = line.indexOf("//");
			if (comment >= 0) {
				line = line.substring(0, comment-1).trim();
			}
			if (line.length() == 0)
				continue;
			String[] toks = line.split(" ");
			int gid = Integer.parseInt(toks[0]);
			check(gid == varNo && gid == gates.size());
			++varNo;
			int n = 0;
			boolean isOutput = false;
			boolean isDone = false;
			while(++n < toks.length) {
				check(!isDone);
				if (toks[n].equals("input")) {
					check(n+1 == toks.length);
					Circuit.Input gate = new Circuit.Input(gid, inputNo++); 
					//gate.write(System.out);
					gates.add(gate);
					inputs.add(gate);
					isDone = true;
					continue;
				}
				if (toks[n].equals("output")) {
					isOutput = true;
					continue;
				}
				if (toks[n].equals("gate")) {
					check(toks[++n].equals("arity"));
					Circuit.Gate gate = isOutput ? new Circuit.Output(gid) 
							: new Circuit.Gate(gid);
					
					gate.arity = Integer.parseInt(toks[++n]);
					check(gate.arity >= 0 && gate.arity <= 3);
					check(toks[++n].equals("table"));
					++n;
					check(toks[n].equals("[") || toks[n].equals("[0]")
							|| toks[n].equals("[1]"));
					if (toks[n].equals("[0]")) {
						check(gate.arity == 0);
						gate.truthtab = new boolean[] { false };
					} else if (toks[n].equals("[1]")) {
							check(gate.arity == 0);
							gate.truthtab = new boolean[] { true };
					} else {
						int ttSize = (1 << gate.arity);
						gate.truthtab = new boolean[ttSize];
						for (int i=0; i<ttSize; ++i) {
							int tv = Integer.parseInt(toks[++n]);
							check(tv == 0 || tv == 1);
							gate.truthtab[i] = (tv==1);
						}
						check(toks[++n].equals("]"));
					}
					if (gate.arity == 0) {
						gate.inputs = new Circuit.GateBase[0];
						n+=2;
					} else {
						check(toks[++n].equals("inputs"));
						check(toks[++n].equals("["));
						gate.inputs = new Circuit.GateBase[gate.arity]; 
						for (int i=0; i<gate.arity; ++i) {
							int inputid = Integer.parseInt(toks[++n]);
							//gate.inputs[i] = gates.get(inputid);
							// they are freaking backwards.
							gate.inputs[gate.arity - 1 - i] = gates.get(inputid);
						}
						check(toks[++n].equals("]"));
					}
					//gate.write(System.out);
					gates.add(gate);
					if (isOutput)
						outputs.add((Circuit.Output)gate);
					isDone = true;
					continue;
				}
				
				check(false);
			}
		}
		Circuit cc = new Circuit();
		cc.inputs = inputs.toArray(new Circuit.Input[0]);
		cc.outputs = outputs.toArray(new Circuit.Output[0]);
		return cc;
	}
	
	public static Circuit readFile(String file) {
		try {
			BufferedReader in = new BufferedReader(new FileReader(file));
			Circuit cc = new CircuitParser().parse(in);
			in.close();
			return cc;
		} catch (IOException ex) {
			ex.printStackTrace();
			throw new RuntimeException("Error reading circuit file: " + file);
		}
	}
	
	public static void main(String[] args) throws IOException {
		BufferedReader in = new BufferedReader(new FileReader(args[0]));
		new CircuitParser().parse(in);
	}
}