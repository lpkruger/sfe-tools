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

import static java.math.BigInteger.ZERO;
import static java.math.BigInteger.ONE;

/**
 * @author lpkruger
 *
 * To change the template for this generated type comment go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
public class SFDL {

	static abstract class Obj {
		void emit(PrintStream ps) {};
	}

	static abstract class Type extends Obj {
	}

	static class IntType extends Type {
		BigInteger bits;
		IntType(BigInteger n) {
			this.bits = n;
		}
	}
	static class StructType extends Type {
		String[] fieldnames;
		Type[] fieldtypes;
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
	}
	
	static class VarRef extends Expr {
		VarDef var;
		VarRef(VarDef var) {
			super(var.type);
			this.var = var;
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
			BigInteger z = lt.bits.max(rt.bits).add(ONE);
			this.type = new IntType(z);
		}
	}
	
	static class SubExpr extends BinaryOpExpr {
		SubExpr(Expr left, Expr right) {
			super(left, right);
			IntType lt = (IntType) left.type;
			IntType rt = (IntType) right.type;
			BigInteger z = lt.bits.max(rt.bits);
			this.type = new IntType(z);
		}
	}

	static class XorExpr extends BinaryOpExpr {
		XorExpr(Expr left, Expr right) {
			super(left, right);
			IntType lt = (IntType) left.type;
			IntType rt = (IntType) right.type;
			BigInteger z = lt.bits.max(rt.bits);
			this.type = new IntType(z);
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
	}

	static class VarDef extends NamedObj {
		Type type;
		VarDef(String name, Type type) {
			super(name);
			this.type = type;
		}
	}


	static class FunctionDef extends NamedObj {
		String name;
		Type type;
		VarDef[] args;
		Expr body;

		FunctionDef(String name) {
			super(name);
		}
	}

	static class Scope extends Obj {
		Map<String, Obj> entities = new HashMap<String, Obj>();
		Scope parent;

		Scope(Scope parent) {
			this.parent = parent;
		}

		Obj get(String name) {
			Obj z = entities.get(name);
			if (z==null && parent!= null) {
				return parent.get(name);
			}
			return z;
		}
	}

	Type getType(String str) {
		Obj z = top.get(str);
		if (z instanceof Type) {
			return (Type) z;
		}
		return null;
	}


	String progname;
	Scope top;
	Scope current;

	//MapList evmap = new MapList();


	SFDL() {
		top = new Scope(null);
	}
}
