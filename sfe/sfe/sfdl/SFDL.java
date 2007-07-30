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

import javax.lang.model.element.VariableElement;

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
	}

	static class VoidType extends Type {
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
		public String toString() {
			return "[type int<" + bits + "> ]";
		}
		public String toShortString() {
			return "int<" + bits + ">";
		}
	}
	static class StructType extends Type {
		String[] fieldnames;
		Type[] fieldtypes;
		// TODO: replace with VarDefs
		StructType(ArrayList<String> fn, ArrayList<Type> ft) {
			this.fieldnames = fn.toArray(new String[fn.size()]);
			this.fieldtypes = ft.toArray(new Type[fn.size()]);
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
		
		IntConst(BigInteger n) {
			super(null);
			this.number = n;
			this.type = new IntType(Math.max(1, n.bitLength()));
		}
		
		public String toString() {
			return "[const " + type.toShortString() + " " + number + " ]";
		}
	}

	static abstract class LValExpr extends Expr {
		LValExpr(Type type) {
			super(type);
		}
	}
	
	static class VarRef extends LValExpr {
		VarDef var;
		VarRef(VarDef var) {
			super(var.type);
			this.var = var;
		}
		public String toString() {
			return "[varref " + type.toShortString() + " " + var + " ]";
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
		
		public String toString() {
			return "[add+ " + type.toShortString() + " " + left + " , " + right + " ]";
		}
	}
	
	static class SubExpr extends BinaryOpExpr {
		SubExpr(Expr left, Expr right) {
			super(left, right);
			IntType lt = (IntType) left.type;
			IntType rt = (IntType) right.type;
			int z = Math.max(lt.bits, rt.bits);
			this.type = new IntType(z);
		}
		
		public String toString() {
			return "[sub- " + type.toShortString() + " " + left + " , " + right + " ]";
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
		
		public String toString() {
			return "[xor^ " + type.toShortString() + " " + left + " , " + right + " ]";
		}
	}
	
	static class EqExpr extends BinaryOpExpr {
		EqExpr(Expr left, Expr right) {
			super(left, right);
			this.type = type_Boolean;
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
	}
	
	static class GreaterThanExpr extends BinaryOpExpr {
		GreaterThanExpr(Expr left, Expr right) {
			super(left, right);
			this.type = type_Boolean;
		}
		public String toString() {
			return "[gt< " + type + " " + left + " , " + right + " ]";
		}
	}
	
	static class AssignExpr extends Expr {
		LValExpr lval;
		Expr value;
		AssignExpr(LValExpr lval, Expr expr) {
			super(lval.type);
			this.lval = lval;
			this.value = expr;
		}
		public String toString() {
			return "[assn= " + type.toShortString() + " " + lval + " := " + value + " ]";
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
	}
	
	static class Block extends Expr {
		Expr[] stmts;
		Block(ArrayList<Expr> stmts) {
			super(type_Void);
			this.stmts = stmts.toArray(new Expr[stmts.size()]);
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
	
	static class StructRefExpr extends LValExpr {
		Expr left;
		String field;
		int fieldPos = -1;
		StructRefExpr(Expr left, String field) {
			super(null);
			this.left = left;
			this.field = field;
			StructType st = (StructType) left.type;
			for (int i=0; i<st.fieldnames.length; ++i) {
				if (st.fieldnames[i].equals(field)) {
					this.type = st.fieldtypes[i];
					this.fieldPos = i;
					break;
				}
			}
		}
		
		public String toString() {
			return "[structref " + type.toShortString() + " " + left + " . " + field + " ]";
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

	Type getType(String str) {
		NamedObj z = top.get(str);
		if (z instanceof TypeDef) {
			return ((TypeDef) z).type;
		}
		return null;
	}


	public Expr evalVar(String str) {
		NamedObj z = top.get(str);
		if (z instanceof VarDef) {
			return new VarRef((VarDef)z);
		}
		if (z instanceof ConstDef) {
			return ((ConstDef)z).value;
		}
		return null;
	}
	
	void openScope() {
		Scope newscope = new Scope(top);
		top = newscope;
	}
	
	void closeScope() {
		top = top.parent;
	}
	
	static void printScope(Scope scope) {
		for (NamedObj o : scope.entities.values()) {
			System.out.println(o);
			System.out.println();
		}
	}
	
	String progname;
	Scope top;
	Scope current;

	//MapList evmap = new MapList();


	SFDL() {
		top = new Scope(null);
		current = top;
	}

	public void addToScope(NamedObj obj) {
		current.add(obj);
		
	}

}
