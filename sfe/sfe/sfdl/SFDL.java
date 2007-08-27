/*
 * Created on Apr 20, 2003
 *
 * To change the template for this generated file go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
package sfe.sfdl;

import java.util.*;
import java.io.*;
import java.math.BigInteger;

import javax.swing.UIDefaults.LazyValue;

import com.sun.org.apache.xalan.internal.xsltc.compiler.CompilerException;

import static java.math.BigInteger.ZERO;
import static java.math.BigInteger.ONE;

/**
 * @author lpkruger
 *
 * To change the template for this generated type comment go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
public class SFDL {

	static final IntType type_Boolean = new IntType(1);
	static final VoidType type_Void = new VoidType();
	
	static abstract class Obj {
		void emit(PrintStream ps) {};
	}

	static abstract class Type extends Obj {
		abstract String toShortString();
		boolean isCompoundType() {
			return false;
		}
		abstract int bitWidth();
		boolean isSigned() {
			return false;
		}
		void typeCheck(Expr other) {
			//if (!this.getClass().isInstance(other.type)) {
			if (this != other.type) {
				throw new CompilerError(other.type.toShortString() + 
						" is not instance of " + toShortString());
			} 
		}
		
	}

	static class VoidType extends Type {
		int bitWidth() {
			return 0;
		}
		public String toString() {
			return "[type void ]";
		}
		public String toShortString() {
			return "void";
		}
		
	}
	
	static class IntType extends Type {
		int bits;
		IntType(int n) {
			this.bits = n;
		}
		int bitWidth() {
			return bits;
		}
		public String toString() {
			return "[type int<" + bits + "> ]";
		}
		public String toShortString() {
			return "int<" + bits + ">";
		}
	}
	
	static abstract class CompoundType extends Type {
		abstract LValExpr[] createLValExprs(LValExpr expr);
		boolean isCompoundType() {
			return true;
		}
	}
	

	static class ArrayType extends CompoundType {
		Type basetype;
		int len;
		LValExpr[] createLValExprs(LValExpr expr) {
			typeCheck((Expr)expr);
			ArrayList<LValExpr> ret = new ArrayList<LValExpr>();
			this.typeCheck((Expr)expr);
			for (int i=0; i<len; ++i) {
				LArrayRef lar = new LArrayRef(expr, new IntConst(i));
				if (basetype.isCompoundType()) {
					ret.addAll(Arrays.asList(lar.createLValExprs()));
				} else {
					ret.add(lar);
					//System.out.println("I AM HERE: " + fieldnames[i]);
					//System.out.println("    " + lsr.field + " @ " + lsr.fieldPos + " of " + lsr.type);
				}
			}
			return ret.toArray(new LValExpr[ret.size()]);
		}
		int bitWidth() {
			return basetype.bitWidth() * len;
		}
		String toShortString() {
			return basetype.toShortString() + "[" + len + "]";
		}
		public String toString() {
			return "[type array " + basetype.toString() + " len " + len + " ]";
		}
		ArrayType(Type basetype, int len) {
			this.basetype = basetype;
			this.len = len;
		}
	}
	
	static class StructType extends CompoundType {
		String[] fieldnames;
		Type[] fieldtypes;
		// TODO: replace with VarDefs
		StructType(ArrayList<String> fn, ArrayList<Type> ft) {
			this.fieldnames = fn.toArray(new String[fn.size()]);
			this.fieldtypes = ft.toArray(new Type[fn.size()]);
		}
		LValExpr[] createLValExprs(LValExpr expr) {
			typeCheck((Expr)expr);
			ArrayList<LValExpr> ret = new ArrayList<LValExpr>();
			this.typeCheck((Expr)expr);
			for (int i=0; i<fieldnames.length; ++i) {
				LStructRef lsr = new LStructRef(expr, fieldnames[i]);
				if (fieldtypes[i].isCompoundType()) {
					ret.addAll(Arrays.asList(lsr.createLValExprs()));
				} else {
					ret.add(lsr);
					//System.out.println("I AM HERE: " + fieldnames[i]);
					//System.out.println("    " + lsr.field + " @ " + lsr.fieldPos + " of " + lsr.type);
				}
			}
			return ret.toArray(new LValExpr[ret.size()]);
		}
		int bitWidth() {
			int sum=0;
			for (Type type : fieldtypes) {
				sum += type.bitWidth();
			}
			return sum;
		}
		public String toString() {
			StringBuffer sb = new StringBuffer("[type struct ");
			boolean comma = false;
			for (int i=0; i<fieldnames.length; ++i) {
				if (comma)
					sb.append(", ");
				sb.append(fieldtypes[i]).append(" ")
				.append(fieldnames[i]).append(" ");
				comma = true;
			}
			sb.append("]");
			return sb.toString();
		}
		public String toShortString() {
			StringBuffer sb = new StringBuffer("struct{");
			boolean comma = false;
			for (int i=0; i<fieldnames.length; ++i) {
				if (comma)
					sb.append(",");
				sb.append(fieldnames[i]);
				comma = true;
			}
			return sb.append("}").toString();
		}
	}

	static abstract class Expr extends Obj {
		Type type;
		Expr(Type type) {
			this.type = type;
		}
		boolean isConst() {
			return false;
		}
		public Type getType() {
			return type;
		}
		abstract CompilerOutput compile(Compile compile);
	}
	
	static abstract class ConstValue extends Expr {
		boolean isConst() {
			return true;
		}

		ConstValue(Type type) {
			super(type);
		}
	}

	static class IntConst extends ConstValue {
		BigInteger number;
		IntConst(BigInteger n, Type type) {
			super(type);
			this.number = n;
		}
		IntConst(int n) {
			this(BigInteger.valueOf(n));
		}
		IntConst(BigInteger n) {
			super(null);
			this.number = n;
			// 1+ for sign bit
			this.type = new IntType(1+Math.max(1, n.bitLength()));
		}
		CompilerOutput compile(Compile comp) {
			return comp.compileIntConst(this);
		}
		public String toString() {
			return "[const " + type.toShortString() + " " + number + " ]";
		}
	}

	static interface LValExpr {
		String uniqueStr();
		Type getType();
		LValExpr[] createLValExprs();
	}
	
	static class VarRef extends Expr implements LValExpr {
		VarDef var;
		VarRef(VarDef var) {
			super(var.type);
			this.var = var;
		}
		CompilerOutput compile(Compile compile) {
			return compile.compilerVarRef(this);
		}
		public LValExpr[] createLValExprs() {
			if (type.isCompoundType()) {
				return ((CompoundType)type).createLValExprs(this);
			} else { 
				return new LValExpr[] { this };
			}
		}
		public String uniqueStr() {
			return var.name;
		}
		public String toString() {
			return "[varref " + type.toShortString() + " " + var + " ]";
		}	
	}
	
	// LVal struct reference
	static class LStructRef extends StructRef implements LValExpr {
		LStructRef(LValExpr left, String field) {
			super((Expr) left, field);
		}
		
		public String uniqueStr() {
			return ((LValExpr)left).uniqueStr() + "." + field;
		}
		
		public LValExpr[] createLValExprs() {
			if (type.isCompoundType()) {
				// TODO: put createLValExprs in base Type and remove if/cast
				return ((CompoundType) type).createLValExprs(this);
			}
			// this should not be reached until above change is made
			return new LValExpr[] { this };
		}
	}
	
	static class LArrayRef extends ArrayRef implements LValExpr {
		LArrayRef(LValExpr left, Expr el) {
			super((Expr) left, el);
		}

		public LValExpr[] createLValExprs() {
			return null;
		}

		public String uniqueStr() {
			return ((LValExpr) left).uniqueStr();
		}
		
	}
	static class ArrayRef extends Expr {
		Expr left;
		Expr el;
		ArrayRef(Expr left, Expr el) {
			super(null);
			this.left = left;
			this.el = el;
			this.type = ((ArrayType) left.type).basetype;
		}
		CompilerOutput compile(Compile compile) {
			throw new InternalCompilerError("ArrayRef not yet implemented");
		}
	}
	
	static class StructRef extends Expr  {
		Expr left;
		String field;
		int fieldPos = -1;
		StructRef(Expr left, String field) {
			super(null);
			this.left = left;
			this.field = field;
			StructType st = (StructType) left.type;
			for (int i=0; i<st.fieldnames.length; ++i) {
				//System.out.println("search " + field + " -- check " + st.fieldnames[i] + " of " + st.fieldtypes[i].toShortString());
				if (st.fieldnames[i].equals(field)) {
					
					this.type = st.fieldtypes[i];
					this.fieldPos = i;
					break;
				}
			}
		}
		
		CompilerOutput compile(Compile compile) {
			return compile.compileStructRef(this);
		}
		
		public String toString() {
			return "[structref " + type.toShortString() + " " + left + " . " + field + " ]";
		}
	}
	

	static abstract class BinaryOpExpr extends Expr {
		Expr left;
		Expr right;
		BinaryOpExpr(Expr left, Expr right) {
			super(null); // figure it out based on exprs
			this.left = left;
			this.right = right;
		}
	}
	
	static class AddExpr extends BinaryOpExpr {
		AddExpr(Expr left, Expr right) {
			super(left, right);
			IntType lt = (IntType) left.type;
			IntType rt = (IntType) right.type;
			int z = Math.max(lt.bits, rt.bits) + 1;
			this.type = new IntType(z);
		}
		CompilerOutput compile(Compile compile) {
			return compile.compileAddExpr(this);
		}
			
		public String toString() {
			return "[add+ " + type.toShortString() + " " + left + " , " + right + " ]";
		}

	}
	
	static class SubExpr extends BinaryOpExpr {
		SubExpr(Expr left, Expr right) {
			super(left, right);
			IntType lt = (IntType) left.type;
			IntType rt = (IntType) right.type;
			int z = Math.max(lt.bits, rt.bits) + 1;
			this.type = new IntType(z);
		}
		CompilerOutput compile(Compile compile) {
			return compile.compileSubExpr(this);
		}
		public String toString() {
			return "[sub- " + type.toShortString() + " " + left + " , " + right + " ]";
		}
	}

	static class MulExpr extends BinaryOpExpr {
		MulExpr(Expr left, Expr right) {
			super(left, right);
			IntType lt = (IntType) left.type;
			IntType rt = (IntType) right.type;
			int z = lt.bits + rt.bits;
			this.type = new IntType(z);
		}
		CompilerOutput compile(Compile compile) {
			return compile.compileMulExpr(this);
		}
			
		public String toString() {
			return "[mul* " + type.toShortString() + " " + left + " , " + right + " ]";
		}

	}
	
	static class DivExpr extends BinaryOpExpr {
		DivExpr(Expr left, Expr right) {
			super(left, right);
			IntType lt = (IntType) left.type;
			IntType rt = (IntType) right.type;
			int z = lt.bits;
			this.type = new IntType(z);
		}
		CompilerOutput compile(Compile compile) {
			return compile.compileDivExpr(this);
		}
			
		public String toString() {
			return "[div/ " + type.toShortString() + " " + left + " , " + right + " ]";
		}

	}
	
	static class LeftShiftExpr extends BinaryOpExpr {
		LeftShiftExpr(Expr left, Expr right) {
			super(left, right);
			this.type = left.type;
		}
		CompilerOutput compile(Compile compile) {
			return compile.compileLeftShiftExpr(this);
		}
			
		public String toString() {
			return "[lshft<< " + type.toShortString() + " " + left + " , " + right + " ]";
		}
	}
	
	static class RightShiftExpr extends BinaryOpExpr {
		RightShiftExpr(Expr left, Expr right) {
			super(left, right);
			this.type = left.type;
		}
		CompilerOutput compile(Compile compile) {
			return compile.compileRightShiftExpr(this);
		}
			
		public String toString() {
			return "[rshft>> " + type.toShortString() + " " + left + " , " + right + " ]";
		}
	}
	
	static class XorExpr extends BinaryOpExpr {
		XorExpr(Expr left, Expr right) {
			super(left, right);
			IntType lt = (IntType) left.type;
			IntType rt = (IntType) right.type;
			int z = Math.max(lt.bits, rt.bits);
			this.type = new IntType(z);
		}
		CompilerOutput compile(Compile compile) {
			return compile.compileXorExpr(this);
		}
		public String toString() {
			return "[xor^ " + type.toShortString() + " " + left + " , " + right + " ]";
		}

		
	}
	
	static class EqExpr extends BinaryOpExpr {
		EqExpr(Expr left, Expr right) {
			super(left, right);
			this.type = type_Boolean;
		}

		CompilerOutput compile(Compile compile) {
			return compile.compileEqExpr(this);
		}
		
		public String toString() {
			return "[eq== " + type.toShortString() + " " + left + " , " + right + " ]";
		}
	}
	
	static class NotEqExpr extends BinaryOpExpr {
		NotEqExpr(Expr left, Expr right) {
			super(left, right);
			this.type = type_Boolean;
		}
		
		CompilerOutput compile(Compile compile) {
			return compile.compilerNotEqExpr(this);
		}
		public String toString() {
			return "[neq== " + type.toShortString() + " " + left + " , " + right + " ]";
		}
	}
	
	static class LessThanExpr extends BinaryOpExpr {
		LessThanExpr(Expr left, Expr right) {
			super(left, right);
			this.type = type_Boolean;
		}
		public String toString() {
			return "[lt< " + type.toShortString() + " " + left + " , " + right + " ]";
		}
		CompilerOutput compile(Compile compile) {
			return compile.compileLessThanExpr(this);
		}
	}
	
	static class GreaterThanExpr extends BinaryOpExpr {
		GreaterThanExpr(Expr left, Expr right) {
			super(left, right);
			this.type = type_Boolean;
		}
		CompilerOutput compile(Compile compile) {
			return compile.compileGreaterThanExpr(this);
		}
		public String toString() {
			return "[gt< " + type + " " + left + " , " + right + " ]";
		}
	}
	
	static class AssignExpr extends Expr {
		LValExpr lval;
		Expr value;
		AssignExpr(LValExpr lval, Expr expr) {
			super(lval.getType());
			this.lval = lval;
			this.value = expr;
		}
		CompilerOutput compile(Compile compile) {
			return compile.compileAssignExpr(this);
		}
		public String toString() {
			return "[assn= " + type.toShortString() + " " + lval + " := " + value + " ]";
		}
		
	}
	
	static class ForExpr extends Expr {
		LValExpr var;
		int begin;
		int end;
		int by;
		Block body;
		ForExpr(LValExpr var, int begin, int end, int by, Block body) {
			super(type_Void);
			this.var = var;
			this.begin = begin;
			this.end = end;
			this.by = by;
			this.body = body;
		}
		CompilerOutput compile(Compile compile) {
			return compile.compileForExpr(this);
		}
	}
	static class IfExpr extends Expr {
		Expr cond;
		Block tblock;
		Block fblock;
		IfExpr(Expr cond, Block tblock, Block fblock) {
			super(type_Void);
			this.cond = cond;
			this.tblock = tblock;
			this.fblock = fblock;
			
			if (this.fblock==null) {
				ArrayList<Expr> nullBlock = new ArrayList<Expr>();
				this.fblock = new Block(nullBlock);
			}
		}
		public String toString() {
			return "[if " + type.toShortString() + " (cond " + cond + ") then " + tblock + " else " + fblock + " ]";
		}
		CompilerOutput compile(Compile compile) {
			return compile.compileIfExpr(this);
		}
	}
	
	static class Block extends Expr {
		Expr[] stmts;
		Block(ArrayList<Expr> stmts) {
			super(type_Void);
			this.stmts = stmts.toArray(new Expr[stmts.size()]);
		}
		CompilerOutput compile(Compile compile) {
			return compile.compileBlock(this);
		}
		public String toString() {
			StringBuffer sb = new StringBuffer("[block{\n");
			for (int i=0; i<stmts.length; ++i) {
				sb.append(stmts[i]).append("\n");
			}
			sb.append("}]");
			return sb.toString();
		}
	}
	
	static abstract class NamedObj extends Obj {
		String name;
		NamedObj(String name) {
			this.name = name;
		}
	}

	static class ConstDef extends NamedObj {
		ConstValue value;
		ConstDef(String name, ConstValue value) {
			super(name);
			this.value = value;
		}

		public String toString() {
			return "[def const " + name + " := " + value + " ]";
		}
	}

	static class VarDef extends NamedObj {
		Type type;
		VarDef(String name, Type type) {
			super(name);
			this.type = type;
		}
		// get all unique strings from elements
		public LValExpr[] getAllSubLVals() {
			return new VarRef(this).createLValExprs();
		}
		public String toString() {
			return "[def var " + name + " of " + type.toShortString() + " ]";
		}

	}
	
	static class TypeDef extends NamedObj {
		Type type;
		TypeDef(String name, Type type) {
			super(name);
			this.type = type;
		}
		public String toString() {
			return "[def type " + name + " is " + type + " ]";
		}
	}


	static class FunctionDef extends NamedObj {
		Type type;
		VarDef[] args;
		Block body;
		Scope scope;

		FunctionDef(String name, Type type, ArrayList<VarDef> args, Block body, Scope scope) {
			super(name);
			this.type = type;
			this.args = args.toArray(new VarDef[args.size()]);
			this.body = body;
			this.scope = scope;
		}
		
		public String toString() {
			StringBuffer sb = 
				new StringBuffer("[def fun " + name + " type " + type.toShortString()); 
			for (int i=0; i<args.length; ++i) {
				sb.append(" arg" + i + " " + args[i]);
			}
			sb.append(" = " + body + " ]");
			return sb.toString();
		}
		
		boolean isParam(VarDef vd) {
			for (VarDef v : args) {
				if (v == vd)
					return true;
			}
			return false;
		}
	}

	static class Scope extends Obj {
		Map<String, NamedObj> entities = new HashMap<String, NamedObj>();
		Scope parent;

		Scope(Scope parent) {
			this.parent = parent;
		}

		NamedObj get(String name) {
			NamedObj z = entities.get(name);
			if (z==null && parent!= null) {
				return parent.get(name);
			}
			return z;
		}

		void add(NamedObj obj) {
			// TODO: make duplicate binding an error
			entities.put(obj.name, obj);
		}
	}


	public FunctionDef getFn(String str) {
		NamedObj z = root.get(str);
		if (z instanceof FunctionDef) {
			return ((FunctionDef) z);
		}
		return null;
	}
	
	Type getType(String str) {
		NamedObj z = current.get(str);
		if (z instanceof TypeDef) {
			return ((TypeDef) z).type;
		}
		return null;
	}


	public Expr evalVar(String str) {
		NamedObj z = current.get(str);
		if (z instanceof VarDef) {
			return new VarRef((VarDef)z);
		}
		if (z instanceof ConstDef) {
			return ((ConstDef)z).value;
		}
		return null;
	}
	
	public void addToScope(NamedObj obj) {
		current.add(obj);	
	}
	
	void openScope() {
		Scope newscope = new Scope(current);
		current = newscope;
	}
	
	void closeScope() {
		current = current.parent;
	}
	
	static void printScope(Scope scope) {
		for (NamedObj o : scope.entities.values()) {
			System.out.println(o);
			System.out.println();
		}
	}
	
	String progname;
	Scope root;
	Scope current;

	//MapList evmap = new MapList();


	SFDL() {
		root = new Scope(null);
		current = root;
	}

}
