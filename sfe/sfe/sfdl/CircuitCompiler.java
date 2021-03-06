package sfe.sfdl;

import java.io.BufferedReader;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.PrintStream;
import java.math.BigInteger;
import java.util.*;

import sfe.sfauth.MD5;
import sfe.sfdl.Parser.ParseError;
import sfe.sfdl.SFDL.*;
import sfe.shdl.*;
import sfe.shdl.Circuit.Gate;
import sfe.shdl.Circuit.GateBase;
import sfe.shdl.Circuit.Input;
import sfe.shdl.Circuit.Output;
import sfe.util.ListMap;
import sfe.util.NullOutputStream;

public class CircuitCompiler implements Compile {
	boolean debug = false;
	
	void D(Object o) {
		if (debug) System.out.println(o);
	}
	
	static boolean[] TT_XOR() {
		boolean[] tt = { false, true, true, false };
		return tt;
	}
	static final boolean[] TT_XOR3() {
		boolean[] tt = { false, true, true, false, true, false, false, true};
		return tt;
	}
	public static boolean[] TT_XNOR() {
		boolean[] tt = { true, false, false, true };
		return tt;
	}
	public static boolean[] TT_AND() {
		boolean[] tt = { false, false, false, true };
		return tt;
	}
	static boolean[] TT_OR() {
		boolean[] tt = { false, true, true, true };
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
		// carry, left, right -> next carry
		boolean[] tt = { false, false, false, true, false, true, true, true };
		return tt;
	}
	static boolean[] TT_SUBCARRY3() {
		//boolean[] tt = { false, true, true, true, false, false, false, false };
		// carry, left, right -> next carry
		boolean[] tt = { false, true, false, false, true, true, false, true };
		return tt;
	}
	public static boolean[] TT_EQ3() {
		boolean[] tt = { false, false, false, false, true, false, false, true };
		return tt;
	}
	static boolean[] TT_NEQ3() {
		boolean[] tt = { false, true, true, false, true, true, true, true};
		return tt;
	}
	
	// if arg1 true: arg2, false: arg3
	public static boolean[] TT_MUX() {
		boolean[] tt = { false, true, false, true, false, false, true, true};
		return tt;
	}

	
	int gateId;
	public int newId() {
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
	
	public Gate newFalseGate() {
		Gate g = new Gate(-2);
		g.arity = 0;
		g.inputs = new GateBase[0];
		g.truthtab = new boolean[] { false };
		return g;
	}
	
	public Gate newIdentityGate(GateBase in) {
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
	
	public Gate newGate() {
		return new Gate(newId());
	}
	public Gate newGate(GateBase in1, GateBase in2, boolean[] tt) {
		Gate g = new Gate(newId());
		g.arity = 2;
		g.inputs = new GateBase[] {in1, in2};
		g.truthtab = tt;
		if (tt.length != 4) 
			throw new InternalCompilerError("truthtab must have 4 entries");
		return g;
	}
	public Gate newGate(GateBase in1, GateBase in2, GateBase in3, boolean[] tt) {
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
			//newcc[i] = signExtend ? newIdentityGate(cc[cc.length-1]) : FALSE_GATE;
			newcc[i] = signExtend ? cc[cc.length-1] : FALSE_GATE;
		}
		return newcc;
	}
	
	static class Scope {
		HashMap<String, CircuitVar> varMap = new HashMap<String, CircuitVar>();
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

	Map<String,GateBase[]> formatMap = new ListMap<String,GateBase[]>();
	
	static String circ2str(GateBase[] cc) {
		StringBuffer sb = new StringBuffer("circ[" + cc.length + "]\n");
		for (int i=0; i<cc.length; ++i) {
			sb.append(cc[i]).append("\n");
		}
		return sb.toString();
	}
	
	/*
	GateBase[] assignToVar(LValExpr lval, GateBase[] cc) {
	}*/
	
	/*
	GateBase[] retrieveFromVar(VarRef lval, boolean isOutput) {
	*/
	
	static class GateExpr extends SFDL.Expr {
		GateBase[] cc;
		GateExpr(Type type, GateBase[] cc) {
			super(type);
			this.cc = cc;
		}
		
		CompilerOutput compile(Compile compile) {
			return new CircuitCompilerOutput(cc);
		}
		
		SFDL.ConstValue evalAsConst() throws NotConstantException {
			BigInteger bigint = BigInteger.ZERO;
			for (int i=0; i<cc.length; ++i) {
				if (!(cc[i] instanceof Gate)) {
					//System.out.println("wasn't a gate : not const");
					throw new NotConstantException();

				}
				Gate g = (Gate) cc[i];
				if (g.arity != 0) {
					//System.out.println("arity=" + g.arity + " : not const");
					//System.out.println(circ2str(cc));
					throw new NotConstantException();

				}
				if (g.truthtab[0])
					bigint = bigint.setBit(i);
			}
			//System.out.println("is const " + bigint);
			//System.out.println(circ2str(cc));
			return new SFDL.IntConst(bigint);
		}
	}
	
	CircuitCompilerOutput compileExpr(Expr ex) {
		D("Expr " + ex.toString());
		
		try {
			SFDL.ConstValue cv = ex.evalAsConst();
			ex = cv;
		} catch (NotConstantException e) {
			// it wasn't constant, move on
		}
		
		CircuitCompilerOutput out = (CircuitCompilerOutput) ex.compile(this);
		
		if (ex.getType().isCompoundType()) {
			
			// TODO: something
			/*
			if (out.cc.length != ex.getType().bitWidth()) {	
			}
			throw new InternalCompilerError(
					"compound type expressions, expecting width " + ex.getType().bitWidth()
					+ " got " + out.cc.length);
			*/
		} else if (out.cc.length != ex.getType().bitWidth()) {
			D("Expr " + ex.toString());
			throw new InternalCompilerError("Compiler error: Expr " + ex.getClass() + " of type " + 
					ex.getType().toShortString() + " compiled to " + 
					out.cc.length + " bits");
			
		}
		return out;
	}

	CircuitCompilerOutput.FunctionOutput compileFunction(FunctionDef fun) {
		openScope();
		
		List<Input> fnInputs = new ArrayList<Input>();
		for (NamedObj obj : fun.scope.entities.values()) {
			String name = obj.name;
			if (obj instanceof VarDef) {
				VarDef var = (VarDef) obj;
				
				D("param: " + obj);
				CircuitVar cvar = CircuitVar.createVars(var.type, name);

				if (fun.isParam(var)) {
					fnInputs = cvar.assignInputs(this);
				} else {
					cvar.assignGate(FALSE_GATE);
				}

				current.varMap.put(var.name, cvar);

			}
		}

		compileBlock(fun.body); 
		
		Circuit circ = new Circuit();
		circ.inputs = fnInputs.toArray(new Input[fnInputs.size()]);
		//GateBase[] cc = top.varMap.get(fun.name);
		VarDef outputVar = (VarDef)fun.scope.get(fun.name);
		CircuitVar cv = current.varMap.get(outputVar.name);
		cv.setAsOutputs(this);
		GateBase[] cc = cv.getAllGates().toArray(new GateBase[0]);
		if (cc == null) {
			D("return value not in scope: " + fun.name);
		}

		circ.outputs = new Output[cc.length];
		for (int i=0; i<cc.length; ++i) {
			circ.outputs[i] = (Output) cc[i];
		}
		
		CircuitCompilerOutput.FunctionOutput ret = 
			new CircuitCompilerOutput.FunctionOutput(circ);
		closeScope();
		return ret;

	}
	
	public CompilerOutput compileBlock(Block block) {
		for (Expr expr : block.stmts) {
			compileExpr(expr);
		}
		return new CircuitCompilerOutput(new GateBase[0]);
	}
	
	Stack<LValExpr> lvalStack = new Stack<LValExpr>(); 

	public void compileAssignArrayRef(LArrayRef arrayRef,
			CompilerOutput val, Expr ex) {
		lvalStack.push(arrayRef);
		((LValExpr)arrayRef.left).compileAssign(this, val, ex);
	}

	public void compileAssignStructRef(LStructRef structRef,
			CompilerOutput val, Expr ex) {
		lvalStack.push(structRef);
		((LValExpr)structRef.left).compileAssign(this, val, ex);
	}

	public void compileAssignVarRef(VarRef varRef, CompilerOutput val, Expr ex) {
		GateBase[] cc = ((CircuitCompilerOutput) val).cc;
		CircuitVar cv = current.varMap.get(varRef.var.name);
		cv.assign(this, cc, ex);
		lvalStack.clear();
	}
	
	public CompilerOutput compileAssignExpr(AssignExpr expr) {
		//typeCheck(expr.value.type, expr.vardef.type);
		GateBase[] cc = new GateBase[expr.type.bitWidth()];
		CircuitCompilerOutput cout = compileExpr(expr.value); 
		
		GateBase[] rc = cout.cc;
		if (rc.length>=cc.length) {
			System.arraycopy(rc, 0, cc, 0, cc.length);
		} else {
			cc = bitExtend(rc, cc.length, expr.type.isSigned());
		}
		
		cout = new CircuitCompilerOutput(cc);
		expr.lval.compileAssign(this, cout, expr.value);
		return cout;
	}
	
	public Gate[] createAdder(GateBase[] lc, GateBase[] rc) {
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
			//System.out.println("cc " + i + " + " + cc[i]);
		}
		return new CircuitCompilerOutput(cc);
	}
	
	public CompilerOutput compileVarRef(VarRef varRef) {
		CircuitVar cv = current.varMap.get(varRef.var.name);
		return new CircuitCompilerOutput(cv.evalVar(this));
	}


	Stack<Expr> refStack = new Stack<Expr>();
	
	public CompilerOutput compileStructRef(StructRef structRef) {
		refStack.push(structRef);
		CompilerOutput cout = compileExpr(structRef.left);
		refStack.pop();
		return cout;
	}
	
	public CompilerOutput compileArrayRef(ArrayRef arrayRef) {
		refStack.push(arrayRef);
		CompilerOutput cout = compileExpr(arrayRef.left);
		refStack.pop();
		return cout;
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
			//carry[i] = newGate(left[i], right[i], carry[i-1], TT_SUBCARRY3());
			carry[i] = newGate(carry[i-1], left[i], right[i], TT_SUBCARRY3());
		}
		if (extraSignBit) {
			cc[len] = carry[len-1];
		}
		return cc;
	}
	
	public CompilerOutput compileSubExpr(SubExpr expr) {
		GateBase[] lc = compileExpr(expr.left).cc;
		GateBase[] rc = compileExpr(expr.right).cc;
		
		try {
			GateExpr gex1 = new GateExpr(expr.left.type, lc);
			GateExpr gex2 = new GateExpr(expr.right.type, rc);
			SFDL.SubExpr cex = new SFDL.SubExpr(gex1, gex2);
			CircuitCompilerOutput cout = compileExpr(cex.evalAsConst());
			//System.out.println("sub constant: " + cex.evalAsConst());
			cout.cc = bitExtend(cout.cc, expr.type.bitWidth(), expr.type.isSigned());
		} catch (NotConstantException ex) {
			System.out.println("not constant subtraction expr:" + expr);
			// wasn't a constant
		}
		lc = bitExtend(lc, expr.type.bitWidth()-1, expr.left.type.isSigned());
		rc = bitExtend(rc, expr.type.bitWidth()-1, expr.right.type.isSigned());
		
		Gate[] result = createSubCircuit(lc, rc, true);
		//System.out.println("sub result.len = " + result.length + " type width = " + expr.type.bitWidth());
		return new CircuitCompilerOutput(result);
		
	}
	
	public CompilerOutput compileDivExpr(DivExpr expr) {
		//throw new CompilerError("not yet implemented: divExpr");
		GateBase[] lc = compileExpr(expr.left).cc;
		GateBase[] rc = compileExpr(expr.right).cc;
		
		//lc = bitExtend(lc, expr.type.bitWidth(), expr.left.type.isSigned());
		//rc = bitExtend(rc, expr.type.bitWidth(), expr.left.type.isSigned());
		
		Gate[] result = new Gate[expr.type.bitWidth()];

		int leftsize =  expr.left.type.bitWidth();
		int rightsize = expr.right.type.bitWidth();
		//int circsize = leftsize < rightsize ? leftsize : rightsize;
		int circsize = leftsize;

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
				for (int j=1; j<rightsize; ++j) {
					curP[j] = FALSE_GATE;
				}		
			} else {
				for (int j=1; j<rightsize; ++j) {
					curP[j] = lastP[j-1];
				}
			}

			// subtract 
			GateBase[] subQ = new GateBase[rightsize];
			subQ = createSubCircuit(curP, rc, true);
			
			// output bit
			result[circsize-i-1] = newNotGate(subQ[rightsize]);
				
			// update curP if necessary, using muxes
			GateBase[] curP2 = new GateBase[rightsize];
					
			for (int j=0; j<rightsize; ++j) {
				// TODO: check order of MUX arguments
				curP2[j] = newGate(subQ[rightsize], curP[j], subQ[j], TT_MUX());
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
	public CompilerOutput compileAndExpr(AndExpr expr) {
		GateBase[] lc = compileExpr(expr.left).cc;
		GateBase[] rc = compileExpr(expr.right).cc;	
		
		lc = bitExtend(lc, expr.type.bitWidth(), expr.left.type.isSigned());
		rc = bitExtend(rc, expr.type.bitWidth(), expr.right.type.isSigned());
		
		IntType type = castToInt(expr.type);
		
		Gate[] cc = new Gate[type.bits];
		for(int i=0; i<type.bits; ++i) {
			cc[i] = newGate(lc[i], rc[i], TT_AND());
		}
		
		return new CircuitCompilerOutput(cc);
	}
	public CompilerOutput compileOrExpr(OrExpr expr) {
		GateBase[] lc = compileExpr(expr.left).cc;
		GateBase[] rc = compileExpr(expr.right).cc;	
		
		lc = bitExtend(lc, expr.type.bitWidth(), expr.left.type.isSigned());
		rc = bitExtend(rc, expr.type.bitWidth(), expr.right.type.isSigned());
		
		IntType type = castToInt(expr.type);
		
		Gate[] cc = new Gate[type.bits];
		for(int i=0; i<type.bits; ++i) {
			cc[i] = newGate(lc[i], rc[i], TT_OR());
		}
		
		return new CircuitCompilerOutput(cc);
	}
	
	public Gate createEqTest(int eqLen, GateBase[] lc, GateBase[] rc) {
		Gate[] cc = new Gate[eqLen];
		cc[0] = newGate(lc[0], rc[0], TT_XNOR());
		for(int i=1; i<eqLen; ++i) {
			cc[i] = newGate(cc[i-1], lc[i], rc[i], TT_EQ3());
		}
		return cc[cc.length-1];
	}
	public CompilerOutput compileEqExpr(EqExpr expr) {
		GateBase[] lc = compileExpr(expr.left).cc;
		GateBase[] rc = compileExpr(expr.right).cc;

		int eqLen = Math.max(lc.length, rc.length);
		lc = bitExtend(lc, eqLen, expr.left.type.isSigned());
		rc = bitExtend(rc, eqLen, expr.right.type.isSigned());
		Gate cc = createEqTest(eqLen, lc, rc);
		return new CircuitCompilerOutput(cc);
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
		cc[0] = newGate(rc[0], lc[0], TT_ANDNOT());

		for(int i=1; i<eqLen; ++i) {
			// (b and ~c) or (a and (b xnor c))
			cc[i] = newGate(cc[i-1], rc[i], lc[i], new boolean[] {
					false, false, true, false, true, false, true, true
			});
		}
		
		Gate result = cc[eqLen-1];
		if (expr.left.type.isSigned()) {
			if (expr.right.type.isSigned()) {
				// TODO: can optimize more, use cc[eqLen-2] instead of result
				result = newGate(result, lc[eqLen-1], rc[eqLen-1],	
						new boolean[] 
						            { false, false, true, false, true, false, true, true});
			} else {
				result = newGate(result, lc[eqLen-1], TT_OR());
			}
		} else if (expr.right.type.isSigned()) {
			// right signed, left not
			result = newGate(result, rc[eqLen-1], TT_ANDNOT());
		}
		return new CircuitCompilerOutput(result);
	}
	public CompilerOutput compileGreaterThanExpr(GreaterThanExpr expr) {
		LessThanExpr ltex = new LessThanExpr(expr.right, expr.left);
		return compileLessThanExpr(ltex);
	}
	public CompilerOutput compileLessThanOrEqExpr(LessThanOrEqExpr expr) {
		// same as not greater than
		LessThanExpr ltex = new LessThanExpr(expr.right, expr.left);
		CircuitCompilerOutput cout = (CircuitCompilerOutput) compileLessThanExpr(ltex);
		return new CircuitCompilerOutput(new Gate[] { newNotGate(cout.cc[0]) });
	}
	public CompilerOutput compileGreaterThanOrEqExpr(GreaterThanOrEqExpr expr) {
		// same as not less than
		LessThanExpr ltex = new LessThanExpr(expr.left, expr.right);
		CircuitCompilerOutput cout = (CircuitCompilerOutput) compileLessThanExpr(ltex);
		return new CircuitCompilerOutput(new Gate[] { newNotGate(cout.cc[0]) });
	}
	
	public CompilerOutput compileForExpr(ForExpr fore) {
		for (int loopctr = fore.begin; loopctr<=fore.end; loopctr += fore.by) {
			System.out.println("loopctr " + loopctr);
			compileAssignExpr(new SFDL.AssignExpr(fore.var, new SFDL.IntConst(loopctr)));
			compileBlock(fore.body);
		}
		return new CircuitCompilerOutput(new Gate[0]);
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

	public void const2Gates(Gate[] g, long k) {
		for (int i=0; i<g.length; ++i) {
			g[i] = newGate();
			g[i].arity = 0;
			g[i].inputs = new GateBase[0];
			g[i].truthtab = new boolean[1];
			if (0L != (k & (1L<<i))) {
				g[i].truthtab[0] = true;
			}
		}
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
	
	
	void printFmtFile() {
		for (Map.Entry<String, GateBase[]> ent : formatMap.entrySet()) {
			String who = "Unknown";
			String key = ent.getKey();
			if (key.contains("alice")) {
				who = "Alice";
			} else if (key.contains("bob")) {
				who = "Bob";
			} else {
				System.err.println("Warning: unknown owner of i/o variable " + key);
				System.err.println("  (should contain \"alice\" or \"bob\" as substring)");
			}
			String what;
			if (ent.getValue()[0] instanceof Input) {
				what = "input";
			} else {
				what = "output";
			}
			
			System.out.print(who + " " + what + " integer \"");
			System.out.print(ent.getKey());
			System.out.print("\" [");
			for (GateBase g : ent.getValue()) {
				System.out.print(" " + g.id);
			}
			System.out.println(" ]");
		}
	}

	CircuitCompilerOutput.FunctionOutput lastout;
	Circuit compile(SFDL sfdl, String name) {
		FunctionDef mainfn = sfdl.getFn("output");
		CircuitCompilerOutput.FunctionOutput out = compileFunction(mainfn);
		lastout = out;
		return out.circuit;
	}

	public static void main(String[] args) throws Exception {
		if (args.length==0) {
			System.out.println("Usage: compile circuit.txt");
			System.exit(1);
		}
		boolean debug = (System.getProperty("D") != null);
		boolean debug2 = (System.getProperty("DD") != null);
		
		debug |= debug2;
		
		if (!debug) {
			System.setOut(new PrintStream(new NullOutputStream()));
		}
	
		boolean noopt = (System.getProperty("O0") != null);
		
		CircuitCompiler comp = new CircuitCompiler();
		
		Circuit circ = null;
		
		try {
			BufferedReader r = new BufferedReader(new FileReader(args[0]));
			System.err.println("parsing...");
			SFDLParser p = new SFDLParser(r);
			if (debug2) {
				p.debug = true;
				comp.debug = true;
			}
			p.parse();
			r.close();

			System.err.println("compiling...");
			circ = comp.compile(p.sfdl, p.programname);
			if (!noopt) {
				System.err.println("optimizing...");
				Optimizer opt = new Optimizer();
				opt.optimize(circ);
				opt.renumber(circ);
			}
		} catch (ParseError err) {
			if (debug) err.printStackTrace(System.err);
			else System.err.println("Error: " + err.getMessage());
			System.exit(1);
		} catch (CompilerError err) {
			if (debug) err.printStackTrace(System.err);
			else System.err.println("Error: " + err.getMessage());
			System.exit(1);
		} catch (Exception err) {
			System.err.println("Internal Compiler Error:");
			err.printStackTrace(System.err);
			System.exit(2);
		}
		String outfile = args[0];
		if (outfile.endsWith(".txt")) {
			outfile = outfile.substring(0, outfile.length()-4);
		}
		outfile += ".circ";
		FileOutputStream outfilestream = new FileOutputStream(outfile);
		PrintStream cout = new PrintStream(outfilestream);
		System.setOut(cout);
		CircuitWriter.write(circ);
		System.setOut(new PrintStream(new NullOutputStream()));
		cout.close();
		
		outfile = args[0];
		if (outfile.endsWith(".txt")) {
			outfile = outfile.substring(0, outfile.length()-4);
		}
		outfile += ".fmt";
		cout = new PrintStream(new FileOutputStream(outfile));
		System.setOut(cout);
		comp.printFmtFile();
		System.setOut(new PrintStream(new NullOutputStream()));
		cout.flush();
		outfilestream.close();
		
		System.err.println("done!");
	}
}