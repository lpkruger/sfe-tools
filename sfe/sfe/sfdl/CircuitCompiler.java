package sfe.sfdl;

import java.io.BufferedReader;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

import sfe.sfdl.Parser.ParseError;
import sfe.sfdl.SFDL.*;
import sfe.shdl.*;
import sfe.shdl.Circuit.Gate;
import sfe.shdl.Circuit.GateBase;
import sfe.shdl.Circuit.Input;
import sfe.shdl.Circuit.Output;

public class CircuitCompiler implements Compile {
	static void D(Object o) {
		System.out.println(o);
	}
	
	static boolean[] TT_XOR() {
		boolean[] tt = { false, true, true, false };
		return tt;
	}
	static final boolean[] TT_XOR3() {
		boolean[] tt = { false, true, true, false, true, false, false, true};
		return tt;
	}
	static boolean[] TT_XNOR() {
		boolean[] tt = { true, false, false, true };
		return tt;
	}
	static boolean[] TT_AND() {
		boolean[] tt = { false, false, false, true };
		return tt;
	}
	static boolean[] TT_ANDNOT() {
		//boolean[] tt = { false, true, false, false };
		boolean[] tt = { false, false, true, false };
		return tt;
	}
	static boolean[] TT_AND3() {
		boolean[] tt = { false, false, false, false, false, false, false, true };
		return tt;
	}
	static boolean[] TT_ADDCARRY3() {
		boolean[] tt = { false, false, false, true, false, true, true, true };
		return tt;
	}
	static boolean[] TT_SUBCARRY3() {
		boolean[] tt = { false, true, true, true, false, false, false, false };
		return tt;
	}
	static boolean[] TT_EQ3() {
		boolean[] tt = { false, false, false, false, true, false, false, true };
		return tt;
	}
	static boolean[] TT_NEQ3() {
		boolean[] tt = { false, true, true, false, true, true, true, true};
		return tt;
	}
	
	// if arg1 true: arg2, false: arg3
	static boolean[] TT_MUX() {
		boolean[] tt = { false, true, false, true, false, false, true, true};
		return tt;
	}

	
	int gateId;
	int newId() {
		return gateId++;
	}
	
	Gate TRUE_GATE = newTrueGate();
	Gate FALSE_GATE = newFalseGate();
	
	Gate newTrueGate() {
		Gate g = new Gate(-1);
		g.arity = 0;
		g.inputs = new GateBase[0];
		g.truthtab = new boolean[] { true };
		return g;
	}
	
	Gate newFalseGate() {
		Gate g = new Gate(-2);
		g.arity = 0;
		g.inputs = new GateBase[0];
		g.truthtab = new boolean[] { false };
		return g;
	}
	
	Gate newIdentityGate(GateBase in) {
		Gate g = new Gate(newId());
		g.arity = 1;
		g.inputs = new GateBase[] { in };
		g.truthtab = new boolean[] { false, true };
		return g;
	}
	
	Gate newNotGate(GateBase in) {
		Gate g = new Gate(newId());
		g.arity = 1;
		g.inputs = new GateBase[] { in };
		g.truthtab = new boolean[] { true, false };
		return g;
	}
	
	Gate newGate() {
		return new Gate(newId());
	}
	Gate newGate(GateBase in1, GateBase in2, boolean[] tt) {
		Gate g = new Gate(newId());
		g.arity = 2;
		g.inputs = new GateBase[] {in1, in2};
		g.truthtab = tt;
		if (tt.length != 4) 
			throw new InternalCompilerError("truthtab must have 4 entries");
		return g;
	}
	Gate newGate(GateBase in1, GateBase in2, GateBase in3, boolean[] tt) {
		Gate g = new Gate(newId());
		g.arity = 3;
		g.inputs = new GateBase[] {in1, in2, in3};
		g.truthtab = tt;
		if (tt.length != 8) 
			throw new InternalCompilerError("truthtab must have 8 entries");
		return g;
	}
	
	IntType castToInt(Type type) {
		if (!(type instanceof IntType)) {
			throw new CompilerError("Type " + type.toShortString() + " is not integer type");
		}
		return (IntType) type;
	}
	
	static Circuit compile(SFDL sfdl, String name) {
		CircuitCompiler comp = new CircuitCompiler();
		FunctionDef mainfn = sfdl.getFn("output");
		return comp.compileFunction(mainfn).circuit;
	}
	
	// 0-extend to desired width
	GateBase[] bitExtend(GateBase[] cc, int len, boolean signExtend) {
		if (cc.length > len) {
			throw new CompilerError("gate too long " + cc.length + " " + len);
		}
		if (cc.length == len)
			return cc;
		GateBase[] newcc = new GateBase[len];
		System.arraycopy(cc, 0, newcc, 0, cc.length);
		for (int i=cc.length; i<len; ++i) {
			newcc[i] = signExtend ? newIdentityGate(cc[cc.length-1]) : FALSE_GATE;
		}
		return newcc;
	}
	
	static class Scope {
		HashMap<String, GateBase[]> varMap = new HashMap<String, GateBase[]>();
		Scope parent;
		Scope(Scope parent) {
			this.parent = parent;
		}
	}
	
	Scope root = new Scope(null);
	Scope current = root;
	
	// this bit is MUXed with all side effects
	GateBase conditionBit = TRUE_GATE;
	
	void openScope() {
		current = new Scope(current);
	}
	
	void closeScope() {
		current = current.parent;
	}
	
	String circ2str(GateBase[] cc) {
		StringBuffer sb = new StringBuffer("circ[" + cc.length + "]\n");
		for (int i=0; i<cc.length; ++i) {
			sb.append(cc[i]).append("\n");
		}
		return sb.toString();
	}
	
	GateBase[] assignToVar(LValExpr lval, GateBase[] cc) {
		if (lval.getType().bitWidth() != cc.length) {
			throw new CompilerError("assigning to variable of wrong length");
		}
		D("assignToVar " + lval.uniqueStr() + " := " + circ2str(cc));
		String id = lval.uniqueStr();
		current.varMap.put(id, cc);
		return cc;
	}
	
	GateBase[] retrieveFromVar(LValExpr lval) {
		String id = lval.uniqueStr();
		GateBase[] cc = current.varMap.get(id);
		if (cc == null && lval.getType() instanceof CompoundType) {
			LValExpr[] alllvals = lval.createLValExprs();
			ArrayList<GateBase> allgates = new ArrayList<GateBase>();
			for (LValExpr sublval : alllvals) {
				//D("sublval :" + sublval.uniqueStr());
				//D("sublval :" + current.varMap.get(sublval.uniqueStr()));
				allgates.addAll(Arrays.asList(current.varMap.get(sublval.uniqueStr())));
			}
			cc = allgates.toArray(new GateBase[allgates.size()]);
		}
		if (cc == null) {
			throw new CompilerError("Not found in scope " + id);
		}
		return cc;
	}
	
	CircuitCompilerOutput compileExpr(Expr ex) {
		CircuitCompilerOutput out = (CircuitCompilerOutput) ex.compile(this);
		if (out.cc.length != ex.getType().bitWidth()) {
			System.err.println("Expr " + ex.toString());
			throw new InternalCompilerError("Compiler error: Expr " + ex.getClass() + " of type " + 
					ex.getType().toShortString() + " compiled to " + 
					out.cc.length + " bits");
			
		}
		return out;
	}

	CircuitCompilerOutput.FunctionOutput compileFunction(FunctionDef fun) {
		openScope();
		
		ArrayList<Input> fnInputs = new ArrayList<Input>();
		for (NamedObj obj : fun.scope.entities.values()) {
			if (obj instanceof VarDef) {
				D("param: " + obj);
				for (LValExpr lv : ((VarDef) obj).getAllSubLVals()) {
					D("param0: " + lv.uniqueStr());
					GateBase[] incc = new GateBase[lv.getType().bitWidth()];

					for (int i=0; i<incc.length; ++i) {
						if (fun.isParam(((VarDef)obj))) {
							int varno = newId();
							incc[i] = new Input(varno, varno);
							System.out.println("Add input gate " + incc[i]);
							fnInputs.add((Input) incc[i]);
							incc[i].setComment(lv.uniqueStr() + "$" + i);
						} else {
							incc[i] = FALSE_GATE;
						}
					}
					
					assignToVar(lv, incc);
				}
			}
		}
		
		compileBlock(fun.body); 
		
		Circuit circ = new Circuit();
		circ.inputs = fnInputs.toArray(new Input[fnInputs.size()]);
		//GateBase[] cc = top.varMap.get(fun.name);
		GateBase[] cc = retrieveFromVar(new VarRef((VarDef)fun.scope.get(fun.name)));
		if (cc == null) {
			D("return value not in scope: " + fun.name);
		}
		circ.outputs = new Output[cc.length];
		for (int i=0; i<cc.length; ++i) {
			circ.outputs[i] = new Output(newIdentityGate(cc[i]));
		}
		
		closeScope();
		
		return new CircuitCompilerOutput.FunctionOutput(circ);

	}
	
	public CompilerOutput compileBlock(Block block) {
		for (Expr expr : block.stmts) {
			compileExpr(expr);
		}
		return new CircuitCompilerOutput(new GateBase[0]);
	}
	
	public CompilerOutput compileAssignExpr(AssignExpr expr) {
		GateBase[] cc = new Gate[expr.type.bitWidth()];
		GateBase[] rc = compileExpr(expr.value).cc;
		if (rc.length>=cc.length) {
			System.arraycopy(rc, 0, cc, 0, cc.length);
		} else {
			cc = bitExtend(rc, cc.length, expr.type.isSigned());
		}
		
		if (conditionBit != TRUE_GATE) {
			GateBase[] oldcc = retrieveFromVar(expr.lval);
			for (int i=0; i<cc.length; ++i) {
				cc[i] = newGate(conditionBit, cc[i], oldcc[i], TT_MUX());
			}
		}
		
		assignToVar(expr.lval, cc);
		return new CircuitCompilerOutput(cc);
	}
	
	Gate[] createAdder(GateBase[] lc, GateBase[] rc) {
		if (lc.length != rc.length) {
			throw new InternalCompilerError("Internal compiler error: " + 
					lc.length + " != " + rc.length);
		}
		int len = lc.length;
		
		Gate[] cc = new Gate[len];
		Gate[] carry = new Gate[len];
		cc[0] = newGate(lc[0], rc[0], TT_XOR());
		carry[0] = newGate(lc[0], rc[0], TT_AND());
		for(int i=1; i<len; ++i) {
			cc[i] = newGate(lc[i], rc[i], carry[i-1], TT_XOR3());
			carry[i] = newGate(lc[i], rc[i], carry[i-1], TT_ADDCARRY3());
		}
		return cc;
	}
	public CompilerOutput compileAddExpr(AddExpr expr) {
		GateBase[] lc = compileExpr(expr.left).cc;
		GateBase[] rc = compileExpr(expr.right).cc;
		
		lc = bitExtend(lc, expr.type.bitWidth(), expr.left.type.isSigned());
		rc = bitExtend(rc, expr.type.bitWidth(), expr.right.type.isSigned());
		
		IntType type = castToInt(expr.type);
		
		Gate[] cc = createAdder(lc, rc);
		
		return new CircuitCompilerOutput(cc);
	}
	
	public CompilerOutput compileIntConst(IntConst intConst) {
		D("compileIntConst " + intConst + " len=" + intConst.number.bitLength());
		int n = intConst.number.bitLength(); 
		if (n==0) {
			return new CircuitCompilerOutput(FALSE_GATE);
		}
		++n;
		GateBase[] cc = new GateBase[n];
		for (int i=0; i<n; ++i) {
			cc[i] = intConst.number.testBit(i) ? TRUE_GATE : FALSE_GATE;
			System.out.println("cc " + i + " + " + cc[i]);
		}
		return new CircuitCompilerOutput(cc);
	}
	public CompilerOutput compilerVarRef(VarRef varRef) {
		GateBase[] cc = current.varMap.get(varRef.uniqueStr());
		System.out.println(varRef.uniqueStr() + " -> " + cc);
		// TODO: must resolve substructs ???
		return new CircuitCompilerOutput(cc);
	}
	
	

	public CompilerOutput compileStructRef(StructRef structRef) {
		// assume all structrefs are lstructrefs
		LStructRef sRef = (LStructRef) structRef;
		GateBase[] cc = current.varMap.get(sRef.uniqueStr());
		System.out.println(sRef.uniqueStr() + " -> " + cc);
		// TODO: must resolve substructs ???
		return new CircuitCompilerOutput(cc);	
	}
	
	Gate[] createSubCircuit(GateBase[] left, GateBase[] right, boolean extraSignBit) {
		if (left.length != right.length) {
			throw new InternalCompilerError("Internal compiler error: " + 
					left.length + " != " + right.length);
		}
		int len = left.length;
		if (extraSignBit)
			++len;
		Gate[] cc = new Gate[len];
		Gate[] carry = new Gate[len];
		if (extraSignBit)
			--len;
		cc[0] = newGate(left[0], right[0], TT_XOR());
		carry[0] = newGate(right[0], left[0], TT_ANDNOT());
		for(int i=1; i<len; ++i) {
			cc[i] = newGate(left[i], right[i], carry[i-1], TT_XOR3());
			carry[i] = newGate(left[i], right[i], carry[i-1], TT_SUBCARRY3());
		}
		if (extraSignBit) {
			cc[len] = carry[len-1];
		}
		return cc;
	}
	
	public CompilerOutput compileSubExpr(SubExpr expr) {
		GateBase[] lc = compileExpr(expr.left).cc;
		GateBase[] rc = compileExpr(expr.right).cc;
		
		lc = bitExtend(lc, expr.type.bitWidth()-1, expr.left.type.isSigned());
		rc = bitExtend(rc, expr.type.bitWidth()-1, expr.right.type.isSigned());
		
		Gate[] result = createSubCircuit(lc, rc, true);
		System.out.println("sub result.len = " + result.length + " type width = " + expr.type.bitWidth());
		return new CircuitCompilerOutput(result);
		
	}
	
	public CompilerOutput compileDivExpr(DivExpr expr) {
		//throw new CompilerError("not yet implemented: divExpr");
		GateBase[] lc = compileExpr(expr.left).cc;
		GateBase[] rc = compileExpr(expr.right).cc;
		
		lc = bitExtend(lc, expr.type.bitWidth(), expr.left.type.isSigned());
		rc = bitExtend(rc, expr.type.bitWidth(), expr.left.type.isSigned());
		
		Gate[] result = new Gate[expr.type.bitWidth()];

		int leftsize =  expr.left.type.bitWidth();
		int rightsize = expr.right.type.bitWidth();
		int circsize = leftsize < rightsize ? leftsize : rightsize;

		GateBase[] curP = null;
		GateBase[] lastP;

		//System.out.println("DIV right " + right.size() + "  left " + left.size() + "  lhs " + lhs.size());

		// one iteration for each bit in divisor (or output)
		for (int i=0; i<circsize; ++i) {
			//System.out.println("DIV iteration: " + i);
			lastP = curP;
			curP = new GateBase[rightsize];
			// left shift P register and bring down next bit
			curP[0] = lc[circsize-i-1];

			if (i == 0) {
				for (int j=1; j<leftsize; ++j) {
					curP[j] = FALSE_GATE;
				}		
			} else {
				for (int j=1; j<leftsize; ++j) {
					curP[j] = lastP[j-1];
				}
			}

			// subtract 
			GateBase[] subQ = new GateBase[rightsize];
			subQ = createSubCircuit(curP, rc, false);
			
			// output bit
			result[circsize-i-1] = newNotGate(subQ[rightsize-1]);
				
			// update curP if necessary, using muxes
			GateBase[] curP2 = new GateBase[rightsize];
					
			for (int j=0; j<rightsize; ++j) {
				// TODO: check order of MUX arguments
				curP2[j] = newGate(subQ[rightsize-1], curP[j], subQ[j], TT_MUX());
			}

			curP = curP2;
		}


		return new CircuitCompilerOutput(result);
	}

	/*
	GateBase[] createMultiplier(GateBase[] left, GateBase[] right) {
		int len = left.length + right.length;
		Gate[] result = new Gate[left.length];
		for (int j=0; j<left.length; ++j) {
			result[j] = newGate(right[0], left[j], FALSE_GATE, TT_MUX());
		}
		for (int i=1; i<right.length; ++i) {
			Gate[] term = new Gate[left.length + i];
			for (int j=0; j<left.length; ++j) {
				term[j] = newGate(right[i], left[i+j], FALSE_GATE, TT_MUX());
			}
			Gate[] resnew = new Gate[result.length + 1];
			System.arraycopy(result, 0, resnew, 0, result.length);
			resnew[result.length] = FALSE_GATE;
			result = createAdder(resnew, term);
		}
		
		return result;
	}
	*/
	
	public CompilerOutput compileMulExpr(MulExpr expr) {
		GateBase[] lc = compileExpr(expr.left).cc;
		GateBase[] rc = compileExpr(expr.right).cc;
		//throw new CompilerError("not yet implemented: mulExpr");
		Gate[] lastrow;
		Gate[] thisrow = new Gate[lc.length+rc.length];
		for (int j=0; j<lc.length; ++j) {
			thisrow[j] = newGate(rc[0], lc[j], TT_AND());
		}
		for (int j=lc.length; j<lc.length+rc.length; ++j) {
			thisrow[j] = FALSE_GATE;
		}
		for (int i=1; i<rc.length; ++i) {
			lastrow = thisrow;
			thisrow = new Gate[lc.length+rc.length];
			for (int j=0; j<i; ++j) {
				thisrow[j] = FALSE_GATE;
			}
			for (int j=0; j<lc.length; ++j) {
				thisrow[i+j] = newGate(rc[i], lc[j], TT_AND());
			}
			for (int j=i+lc.length; j<rc.length+lc.length; ++j) {
				thisrow[j] = FALSE_GATE;
			}
			thisrow = createAdder(lastrow, thisrow);
		}
		
		return new CircuitCompilerOutput(thisrow);
	}
	
	public CompilerOutput compileXorExpr(XorExpr expr) {
		GateBase[] lc = compileExpr(expr.left).cc;
		GateBase[] rc = compileExpr(expr.right).cc;	
		
		lc = bitExtend(lc, expr.type.bitWidth(), expr.left.type.isSigned());
		rc = bitExtend(rc, expr.type.bitWidth(), expr.right.type.isSigned());
		
		IntType type = castToInt(expr.type);
		
		Gate[] cc = new Gate[type.bits];
		for(int i=0; i<type.bits; ++i) {
			cc[i] = newGate(lc[i], rc[i], TT_XOR());
		}
		
		return new CircuitCompilerOutput(cc);
	}
	public CompilerOutput compileEqExpr(EqExpr expr) {
		GateBase[] lc = compileExpr(expr.left).cc;
		GateBase[] rc = compileExpr(expr.right).cc;

		int eqLen = Math.max(lc.length, rc.length);
		lc = bitExtend(lc, eqLen, expr.left.type.isSigned());
		rc = bitExtend(rc, eqLen, expr.right.type.isSigned());

		Gate[] cc = new Gate[eqLen];
		cc[0] = newGate(lc[0], rc[0], TT_XNOR());
		for(int i=1; i<eqLen; ++i) {
			cc[i] = newGate(cc[i-1], lc[i], rc[i], TT_EQ3());
		}
		
		return new CircuitCompilerOutput(cc[cc.length-1]);
	}
	public CompilerOutput compilerNotEqExpr(NotEqExpr expr) {
		GateBase[] lc = compileExpr(expr.left).cc;
		GateBase[] rc = compileExpr(expr.right).cc;

		int eqLen = Math.max(lc.length, rc.length);
		lc = bitExtend(lc, eqLen, expr.left.type.isSigned());
		rc = bitExtend(rc, eqLen, expr.right.type.isSigned());

		Gate[] cc = new Gate[eqLen];
		cc[0] = newGate(lc[0], rc[0], TT_XOR());
		for(int i=1; i<eqLen; ++i) {
			cc[i] = newGate(cc[i-1], lc[i], rc[i], TT_NEQ3());
		}
		
		return new CircuitCompilerOutput(cc[cc.length-1]);
	}
	public CompilerOutput compileLessThanExpr(LessThanExpr expr) {
		GateBase[] lc = compileExpr(expr.left).cc;
		GateBase[] rc = compileExpr(expr.right).cc;

		int eqLen = Math.max(lc.length, rc.length);
		lc = bitExtend(lc, eqLen, expr.left.type.isSigned());
		rc = bitExtend(rc, eqLen, expr.right.type.isSigned());

		Gate[] cc = new Gate[eqLen];
		cc[eqLen-1] = newGate(rc[eqLen-1], lc[eqLen-1], TT_ANDNOT());
		for(int i=eqLen-2; i>=0; --i) {
			// a or (b and ~c)
			cc[i] = newGate(cc[i+1], rc[i], lc[i], new boolean[] {
					false, true, false, false, true, true, true, true
			});
		}
		
		return new CircuitCompilerOutput(cc[0]);

		
	}
	public CompilerOutput compileGreaterThanExpr(GreaterThanExpr expr) {
		throw new InternalCompilerError("not yet implemented: GreaterThanExpr");
	}
	public CompilerOutput compileIfExpr(IfExpr ife) {
		CircuitCompilerOutput out = compileExpr(ife.cond);
		GateBase oldCondBit = conditionBit;
		this.conditionBit = newGate(oldCondBit, out.cc[0], TT_AND());
		compileBlock(ife.tblock);
		this.conditionBit = newGate(oldCondBit, out.cc[0], TT_ANDNOT());
		compileBlock(ife.fblock);
		this.conditionBit = oldCondBit;
		return new CircuitCompilerOutput(new Gate[0]);
	}
	
	GateBase[] createLeftBarrelShifter(GateBase[] in, GateBase[] amt) {
		GateBase[] cc = in;
		GateBase[] mux1;
		GateBase[] mux2;
		
		int ipow = 1;	// TODO: use BigInteger here?
		int j;
		for (int i=0; i<amt.length; ++i) {
			mux1 = new GateBase[in.length];
			mux2 = cc;
			for (j=0; j<ipow; ++j) {
				mux1[j] = FALSE_GATE;
			}
			for (; j<in.length; ++j) {
				mux1[j] = cc[j-ipow];
			}
			
			cc = new GateBase[in.length];
			for (j=0; j<in.length; ++j) {
				cc[j] = newGate(amt[i], mux1[j], mux2[j], TT_MUX());
			}
			
			ipow += ipow;
		}
		return cc;
	}
	
	public CompilerOutput compileLeftShiftExpr(LeftShiftExpr expr) {
		GateBase[] lc = compileExpr(expr.left).cc;
		GateBase[] rc = compileExpr(expr.right).cc;	
		GateBase[] cc = createLeftBarrelShifter(lc, rc);
		return new CircuitCompilerOutput(cc);
	}

	static GateBase[] reverse(GateBase[] in) {
		GateBase[] ret = new GateBase[in.length];
		for (int i=0; i<in.length; ++i) {
			ret[in.length-1-i] = in[i];
		}
		return ret;
	}
	
	public CompilerOutput compileRightShiftExpr(RightShiftExpr expr) {
		GateBase[] lc = compileExpr(expr.left).cc;
		GateBase[] rc = compileExpr(expr.right).cc;
		lc = reverse(lc);
		GateBase[] cc = createLeftBarrelShifter(lc, rc);	
		cc = reverse(cc);
		return new CircuitCompilerOutput(cc);
	}
	
	
	static class NullOutputStream extends java.io.OutputStream {
		public void write(int x) { }
		public void write(byte[] b) { }
        public void write(byte[] b, int off, int len) { } 
	}

	public static void main(String[] args) throws Exception {
		boolean debug = (System.getProperty("D") != null);
		if (!debug) {
			System.setOut(new PrintStream(new NullOutputStream()));
		}
		boolean noopt = (System.getProperty("O0") != null);
		Circuit circ = null;
		try {
			BufferedReader r = new BufferedReader(new FileReader(args[0]));
			System.err.println("parsing...");
			SFDLParser p = new SFDLParser(r);
			p.parse();
			r.close();

			System.err.println("compiling...");
			circ = compile(p.sfdl, p.programname);
			if (!noopt) {
				System.err.println("optimizing...");
				Optimizer opt = new Optimizer();
				opt.optimize(circ);
				opt.renumber(circ);
			}
		} catch (ParseError err) {
			System.err.println("Error: " + err.getMessage());
			System.exit(1);
		} catch (CompilerError err) {
			System.err.println("Error: " + err.getMessage());
			System.exit(1);
		} catch (Exception err) {
			System.err.println("Internal Compiler Error:");
			err.printStackTrace(System.err);
			System.exit(2);
		}
		String outfile = args[0];
		if (outfile.endsWith(".txt")) {
			outfile = outfile.substring(0, outfile.length()-4);
			outfile += ".circ";
		}
		PrintStream cout = new PrintStream(new FileOutputStream(outfile));
		System.setOut(cout);
		CircuitWriter.write(circ);
		System.setOut(new PrintStream(new NullOutputStream()));
		cout.close();
		System.err.println("done!");
	}
}