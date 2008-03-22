/*
 * Created on Apr 20, 2003
 *
 * To change the template for this generated file go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
package sfe.eslcomp;

import java.util.*;
import java.io.*;

/**
 * @author lpkruger
 *
 * To change the template for this generated type comment go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
public class ESL {
	public static final int POLICY_ALLOW = 1;
	public static final int POLICY_DENY = 2;
	
	static abstract class Rule {
		void emit(PrintStream ps) {};
	}
	
	static class RegexRule extends Rule {
		 String regex;
		 boolean isVariable;
		 RegexRule(String regex, boolean isVariable) {
		 	this.regex = regex;
		 	this.isVariable = isVariable;
		 }
	}
	
	static class TypeRule extends Rule {
		EDL.Type type;
		TypeRule(EDL.Type type) {
			this.type = type;
		}
	}
	
	static class TrueRule extends Rule {
	}
	static class FalseRule extends Rule {
	}
	
	static class CExpr {
		String expr;
		CExpr(String expr) {
			this.expr = expr;
		}
	}
	
	static class Match {
		String evtName;
		Rule[] argrules;
		CExpr[] cexprs;
		CExpr ablock;
		int policy;
	}
	
	static class VarDecl {
		String name;
		EDL.Type type;
		Object initialValue;
	}
	
	EDL edl;
	String eslname;
	
	MapList evmap = new MapList();
	Map<String, ESL.VarDecl> vars = new HashMap<String, ESL.VarDecl>();
	
	ESL(EDL edl) {
		this.edl = edl;
	}
	
}
