package sfe.sfdl;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

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
		boolean[] tt = { false, true, false, false };
		return tt;
	}
	static boolean[] TT_AND3() {
		boolean[] tt = { false, false, false, false, false, false, false, true };
		return tt;
	}
	static boolean[] TT_CARRY3() {
		boolean[] tt = { false, false, false, true, false, true, true, true };
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
			throw new RuntimeException("truthtab must have 4 entries");
		return g;
	}
	Gate newGate(GateBase in1, GateBase in2, GateBase in3, boolean[] tt) {
		Gate g = new Gate(newId());
		g.arity = 3;
		g.inputs = new GateBase[] {in1, in2, in3};
		g.truthtab = tt;
		if (tt.length != 8) 
			throw new RuntimeException("truthtab must have 8 entries");
		return g;
	}
	
	static class CompilerError extends RuntimeException {
		CompilerError(String msg) {
			super(msg);
		}
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
	GateBase[] bitExtend(GateBase[] cc, int len) {
		if (cc.length > len) {
			throw new CompilerError("gate too long " + cc.length + " " + len);
		}
		if (cc.length == len)
			return cc;
		GateBase[] newcc = new GateBase[len];
		System.arraycopy(cc, 0, newcc, 0, cc.length);
		for (int i=cc.length; i<len; ++i) {
			newcc[i] = FALSE_GATE;
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
		return (CircuitCompilerOutput) ex.compile(this);
	}

	CircuitCompilerOutput.FunctionOutput compileFunction(FunctionDef fun) {
		openScope();
		
		ArrayList<Input> fnInputs = new ArrayList<Input>();
		for (NamedObj obj : fun.scope.entities.values()) {
			if (obj instanceof VarDef) {
				for (LValExpr lv : ((VarDef) obj).getAllSubLVals()) {
					D("param: " + lv.uniqueStr());
					GateBase[] incc = new GateBase[lv.getType().bitWidth()];

					for (int i=0; i<incc.length; ++i) {
						if (fun.isParam(((VarDef)obj))) {
							int varno = newId();
							incc[i] = new Input(varno, varno);
							System.out.println("Add input gate " + incc[i]);
							fnInputs.add((Input) incc[i]);
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
			System.arraycopy(rc, 0, cc, 0, rc.length);
			// TODO: consider signed/unsigned for sign extension
			for (int i=rc.length; i<cc.length; ++i) {
				cc[i] = FALSE_GATE;
			}
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
	
	public CompilerOutput compileAddExpr(AddExpr expr) {
		GateBase[] lc = compileExpr(expr.left).cc;
		GateBase[] rc = compileExpr(expr.right).cc;
		
		lc = bitExtend(lc, expr.type.bitWidth());
		rc = bitExtend(rc, expr.type.bitWidth());
		
		IntType type = castToInt(expr.type);
		
		Gate[] cc = new Gate[type.bits];
		Gate[] carry = new Gate[type.bits];
		cc[0] = newGate(lc[0], rc[0], TT_XOR());
		carry[0] = newGate(lc[0], rc[0], TT_AND());
		for(int i=1; i<type.bits; ++i) {
			cc[i] = newGate(lc[i], rc[i], carry[i-1], TT_XOR3());
			carry[i] = newGate(lc[i], rc[i], carry[i-1], TT_CARRY3());
		}
		
		return new CircuitCompilerOutput(cc);
	}
	
	public CompilerOutput compileIntConst(IntConst intConst) {
		D("compileIntConst " + intConst + " len=" + intConst.number.bitLength());
		int n = intConst.number.bitLength(); 
		if (n==0) {
			return new CircuitCompilerOutput(FALSE_GATE);
		}
		GateBase[] cc = new GateBase[n];
		for (int i=0; i<n; ++i) {
			cc[i] = intConst.number.testBit(i) ? TRUE_GATE : FALSE_GATE;
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
	public CompilerOutput compileSubExpr(SubExpr expr) {
		throw new CompilerError("not yet implemented: subExpr");
	}
	public CompilerOutput compileXorExpr(XorExpr expr) {
		GateBase[] lc = compileExpr(expr.left).cc;
		GateBase[] rc = compileExpr(expr.right).cc;	
		
		lc = bitExtend(lc, expr.type.bitWidth());
		rc = bitExtend(rc, expr.type.bitWidth());
		
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
		lc = bitExtend(lc, eqLen);
		rc = bitExtend(rc, eqLen);

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
		lc = bitExtend(lc, eqLen);
		rc = bitExtend(rc, eqLen);

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
		lc = bitExtend(lc, eqLen);
		rc = bitExtend(rc, eqLen);

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
		throw new CompilerError("not yet implemented: GreaterThanExpr");
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

	public static void main(String[] args) throws Exception {
		BufferedReader r = new BufferedReader(new FileReader(args[0]));
		SFDLParser p = new SFDLParser(r);
		p.parse();
		r.close();
		
		Circuit circ = compile(p.sfdl, p.programname);
		if (true) {
			Optimizer opt = new Optimizer();
			opt.optimize(circ);
			opt.renumber(circ);
		}
		CircuitWriter.write(circ);
	}
}
