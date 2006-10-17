package sfe.sfdl;

import java.io.*;
import java.util.*;

public class SFDL {
	static class Def {
		String name;
		String constr;	// null if reg
		String val;
	}
	
	static class FnCallArg {
		SfdlFnParam param;
		Def v;
	}
	
	static class SfdlFnParam {
		String name;
		int mode;  // 0 = read, 1 = write
	}
	static class SfdlFn {
		String name;
		String[] lines;
		SfdlFnParam[] params;
	}
	public static void main(String[] args) throws IOException {
		BufferedReader r = new BufferedReader(new FileReader(args[0]));
		new SFDL().parse(r);
	}
	
	Map<String, SfdlFn> sfdlFns = new HashMap<String, SfdlFn>();
	Map<String, Def> varDefs = new HashMap<String, Def>();
	
	void parse(BufferedReader r) throws IOException {
		PrintStream out = System.out;
		String line;
		while((line = r.readLine()) != null) {
			String orig = line;
			line = line.trim();
			if (line.length() == 0 || line.charAt(0) != '$') {
				out.println(orig);
				continue;
			}
			
			if (line.startsWith("$def")) {
				line = line.substring(4).trim();
				int lparen = line.indexOf("(");
				int rparen = line.indexOf(")");
				String parms = line.substring(lparen+1, rparen);
				int comma = parms.indexOf(",");
				String name = parms.substring(0, comma).trim();
				String rhs = parms.substring(comma+1).trim();
				Def def = new Def();
				def.name = name;
				//System.out.println("name = " + name + "  rhs = " + rhs);
				if (!rhs.startsWith("\"")) {
					def.val = rhs;
				} else {
					def.constr = rhs.substring(1, rhs.length()-1);
				}
				varDefs.put(def.name, def);
			} else if (line.startsWith("$asm")) {
				line = line.substring(4).trim();
				int lparen = line.indexOf("(");
				int rparen = line.indexOf(")");
				String name = line.substring(0, lparen).trim();
				//System.out.println("line = " + line + "  lp = " + lparen + "  rp = " + rparen);
				//System.out.println("params = " + line.substring(lparen+1, rparen));
				String[] parms = line.substring(lparen+1, rparen).split(",");
				ArrayList<SfdlFnParam> allpp = new ArrayList<SfdlFnParam>();
				for (int i=0; i<parms.length; ++i) {
					int mode = 0;
					if (parms[i].charAt(0) == '=') {
						parms[i] = parms[i].substring(1).trim();
						mode = 1;
					} else {
						parms[i] = parms[i].trim();
					}
					SfdlFnParam pp = new SfdlFnParam();
					pp.name = parms[i];
					pp.mode = mode;
					allpp.add(pp);
				}
				SfdlFn fn = new SfdlFn();
				fn.name = name;
				fn.params = allpp.toArray(new SfdlFnParam[0]);
				
				ArrayList<String> fnlines = new ArrayList<String>();
				while ((line = r.readLine()) != null) {
					line = line.trim();
					if (line.equals("}"))
						break;
					fnlines.add(line);
				}
				
				fn.lines = fnlines.toArray(new String[0]);
				sfdlFns.put(fn.name, fn);
			} else {
				// try function evaluation
				dofneval(line, out);
			}			
		}
	}
	
	void dofneval(String line, PrintStream out) {
//		try function evaluation
		int lparen = line.indexOf("(");
		int rparen = line.indexOf(")");
		String name = line.substring(1, lparen).trim();
		String[] parms = line.substring(lparen+1, rparen).split(",");
		SfdlFn fn = sfdlFns.get(name);
		if (fn == null) 
			throw new RuntimeException("Unknown fn: " + name);
		
		if (fn.params.length != parms.length)
			throw new RuntimeException("Expected " + fn.params.length + 
					" args, got " + parms.length);
		
		FnCallArg[] args = new FnCallArg[parms.length];
		for (int i=0; i<args.length; ++i) {
			Def d = varDefs.get(parms[i].trim());
			if (d == null)
				throw new RuntimeException("Unknown var: " + parms[i]);
			args[i] = new FnCallArg();
			args[i].param = fn.params[i];
			args[i].v = d;
		}
		eval(fn, args, out);
	}
	
	String replace(String line, String t, String r) {
		String test = " " + t + " ";
		String repl = " " + r + " ";
		
		line = line.replace(test, repl);
		
		test = test.substring(0, test.length()-1) + ",";
		repl = repl.substring(0, repl.length()-1) + ",";
		line = line.replace(test, repl);
		
		test = test.substring(0, test.length()-1);
		repl = repl.substring(0, repl.length()-1);
		if (line.endsWith(test)) {
			line = line.substring(0, line.length() - test.length()) + repl;
		}
		
		return line;
	}
	void eval(SfdlFn fn, FnCallArg[] args, PrintStream out) {
		for (int i=0; i<fn.lines.length; ++i) {
			String line = fn.lines[i];
			
			if (line.startsWith("$")) {
				line = line.substring(1);
				dofneval(line, out);
			}
			
			int gccargcnt = 0;
			ArrayList<FnCallArg> gccargs = new ArrayList<FnCallArg>();
			for (int j=0; j<args.length; ++j) {
				String test = args[j].param.name;
				String repl;
				if (args[j].v.constr != null) {
					repl="%" + (gccargcnt);
					
				} else {
					repl = args[j].v.name;
				}
				String oldline = line;
				//System.out.println("replace" + test + "with" + repl);
				
				line = replace(line, test, repl);
				
				if (args[j].v.constr != null && !oldline.equals(line)) {
					gccargs.add(args[j]);
					++gccargcnt;
				}
			}
			
			for (Def v : varDefs.values()) {
				if (v.val != null)
					line = replace(line, v.name, v.val);
			}
			
			out.print("asmv(\"" + line + "\"");
			if (gccargcnt == 0) {
				out.println(");");
			} else {
				boolean mode0 = false;
				out.println();
				out.print("  : ");
				boolean nocomma = true;
				for (FnCallArg v : gccargs) {
					if (!mode0 && v.param.mode == 0) {
						out.print(" : ");
						mode0 = true;
						nocomma = true;
					}
					if (!nocomma) {
						out.print(", ");
					}
					nocomma = false;
					out.print("\"" + (mode0 ? "" : "=") + v.v.constr + "\" "); 
					out.print("(" + v.v.name + ")");	
				}
				out.println(");");
				
			}
		}
	}
}
