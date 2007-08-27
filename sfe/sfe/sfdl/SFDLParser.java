/*
 * Created on Apr 21, 2003
 *
 * To change the template for this generated file go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */

package sfe.sfdl;

import java.util.*;
import java.io.*;
import java.math.BigInteger;

import fairplay.Compiler.IntConstant;

import sfe.sfdl.SFDL.ArrayRef;
import sfe.sfdl.SFDL.LValExpr;
import sfe.sfdl.Tokenizer.Token;
import sfe.util.VarDesc;
import static sfe.sfdl.TokenizerConstants.*;
import static sfe.sfdl.SFDLReservedWords.*;

/**
 * @author lpkruger
 *
 * This is an immediate execution parser.  It creates an SFDL
 * representation of the program
 */

import static sfe.sfdl.SFDLReservedWords.*;

public class SFDLParser extends Parser {

	static {
		add("program", TOK_PROGRAM);
		add("type", TOK_TYPE);
		add("var", TOK_VAR);
		add("const", TOK_CONST);
		add("struct", TOK_STRUCT);
		add("Boolean", TOK_BOOLEAN);
		add("Int", TOK_INT);
		add("if", TOK_IF);
		add("else", TOK_ELSE);
		add("for", TOK_FOR);
		add("to", TOK_TO);
		add("by", TOK_BY);
		add("function", TOK_FUNCTION);
	}
	
	SFDL sfdl = new SFDL();
	String programname;
	
	MapList umes = new MapList(); // unprocessed events go here awaiting
	// 2nd pass

	public SFDLParser(BufferedReader r) {
		toker = new Tokenizer(r);
		//debug = true;

	}

	void parse() {
		switch (nextToken().type) {
			case TOK_PROGRAM:
				parseProgram();
				break;
			default :
				throw new ParseError("Expected \"program\"", tok);
		}
	}
	
	void parseProgram() {
		expect(tok, TOK_PROGRAM);
		expect(nextToken(), TOK_IDENT);
		programname = tok.str;
		expect(nextToken(), TOK_LBRACE);
		nextToken();
		mainloop:
		for (;;) {
			switch(tok.type) {
			case TOK_RBRACE:
				break mainloop;
			case TOK_CONST:
				parseConstDef();
				break;
			case TOK_TYPE:
				parseTypeDef();
				break;
			case TOK_FUNCTION:
				parseFunctionDef();
				break;
			default:
				throw new ParseError("Unexpected token ", tok);
			}
		}	
	}
	
	void parseConstDef() {
		expect(tok, TOK_CONST);
		expect(nextToken(), TOK_IDENT);
		String name = tok.str;
		expect(nextToken(), TOK_EQUAL);
		nextToken();
		SFDL.Expr expr = parseExpr();
		
		if (!(expr instanceof SFDL.ConstValue)) {
			throw new ParseError("non constant value " + expr, tok);
		}
		SFDL.ConstDef cdef = new SFDL.ConstDef(name, (SFDL.ConstValue) expr);
		sfdl.addToScope(cdef);
		nextToken();
	}
	
	void parseTypeDef() {
		expect(tok, TOK_TYPE);
		expect(nextToken(), TOK_IDENT);
		String name = tok.str;
		expect(nextToken(), TOK_EQUAL);
		nextToken();
		SFDL.Type type = parseTypeExpr();
		SFDL.TypeDef typedef = new SFDL.TypeDef(name, type);
		sfdl.addToScope(typedef);
		nextToken();
	}
	
	void parseFunctionDef() {
		expect(tok, TOK_FUNCTION);
		nextToken();
		SFDL.Type type = parseTypeExpr();
		expect(tok, TOK_IDENT);
		String name = tok.str;
		expect(nextToken(), TOK_LPAREN);
		
		sfdl.openScope();
		
		// function name is return variable
		sfdl.addToScope(new SFDL.VarDef(name, type));
		
		ArrayList<SFDL.VarDef> parms = new ArrayList<SFDL.VarDef>();
		
		boolean expectComma = false;
		for(;;) {
			if(nextToken().type == TOK_RPAREN)
				break;
			
			if (expectComma) {
				expect(tok, TOK_COMMA);
				nextToken();
			}
			
			SFDL.Type paramType = parseTypeExpr();
			expect(tok, TOK_IDENT);
			String paramName = tok.str;
			//parmTypes.add(paramType);
			//parmNames.add(paramName);
		
			SFDL.VarDef vd = new SFDL.VarDef(paramName, paramType);
			parms.add(vd);
			sfdl.addToScope(vd);
			
			expectComma = true;
		}
		
		expect(tok, TOK_RPAREN);
		expect(nextToken(), TOK_LBRACE);
		
		SFDL.Block body = parseBlock();
		
		// parse function body
		// parseExpr()
		// check for TOK_RBRACE
		
		SFDL.FunctionDef fndef = new SFDL.FunctionDef(name, type, parms, body, sfdl.current);
		sfdl.closeScope();
		
		sfdl.addToScope(fndef);
	}
	
	SFDL.Type parseStruct() {
		expect(tok, TOK_STRUCT);
		expect(nextToken(), TOK_LBRACE);
		
		ArrayList<SFDL.Type> structTypes = new ArrayList<SFDL.Type>();
		ArrayList<String> structNames = new ArrayList<String>();
		
		boolean expectComma = false;
		for(;;) {
			if(nextToken().type == TOK_RBRACE)
				break;
			
			if (expectComma) {
				expect(tok, TOK_COMMA);
				nextToken();
			}
			
			SFDL.Type paramType = parseTypeExpr();
			expect(tok, TOK_IDENT);
			String paramName = tok.str;
			structTypes.add(paramType);
			structNames.add(paramName);
			expectComma = true;
		}
		
		nextToken();
		return new SFDL.StructType(structNames, structTypes);
	}
	
	int parseIntConst() {
		int num;
		switch(tok.type) {
		case TOK_NUM:
			num = Integer.parseInt(tok.str);
			break;
		case TOK_IDENT:
			SFDL.Expr value = sfdl.evalVar(tok.str);
			if (!value.isConst())
				throw new ParseError("Constant expression required", tok);

			// throw new ParseError("Integer constant expression required", tok);
			SFDL.IntConst ival = (SFDL.IntConst) value;
			num = ival.number.intValue();
			break;
		default:
			throw new ParseError("Unexpected int width ", tok);
		}
		nextToken();
		return num;
	}
	
	SFDL.Type parseTypeExpr() {
		SFDL.Type type;
		maintype:
			switch(tok.type) {
			case TOK_IDENT:
				type = sfdl.getType(tok.str);
				if (type == null)
					throw new ParseError("Undefined type", tok);

				nextToken();
				break maintype;

			case TOK_STRUCT:
				type = parseStruct();
				break maintype;

			case TOK_INT:
				expect(nextToken(), TOK_LT);
				nextToken();
				int intlen = parseIntConst();

				expect(tok, TOK_GT);
				nextToken();
				
				type = new SFDL.IntType(intlen);
				break maintype;
				
			default:
				throw new ParseError("not a type ", tok);
			}
		
		// is it an array type?
		while (tok.type == TOK_LBRACKET) {
			nextToken();
			int arraylen = parseIntConst();
			expect(tok, TOK_RBRACKET);
			nextToken();
			type = new SFDL.ArrayType(type, arraylen);
		}
		
		return type;
	}

	SFDL.Expr parseVarDef() {
		expect(tok, TOK_VAR);
		nextToken();
		SFDL.Type type = parseTypeExpr();
		expect(tok, TOK_IDENT);
		sfdl.addToScope(new SFDL.VarDef(tok.str, type));
		nextToken();
		return null;	
	}
	
	SFDL.Block parseBlock() {
		expect(tok, TOK_LBRACE);
		ArrayList<SFDL.Expr> statements = new ArrayList<SFDL.Expr>(); 
		nextToken();
		while(tok.type != TOK_RBRACE) {
			SFDL.Expr expr = parseStmt();
			
			if (expr != null) {
				statements.add(expr);
			}
		}
		nextToken();
		return new SFDL.Block(statements);
		
	}
	
	SFDL.Expr parseStmt () {
		SFDL.Expr expr = parseStmt0();
		if(tok.type == TOK_SEMICOLON)
			nextToken();
		return expr;
	}
	
	SFDL.Expr parseStmt0 () {
		if (debug) System.out.println("parseStmt");
		switch(tok.type) {
		case TOK_VAR:
			return parseVarDef();
		case TOK_IF:
			nextToken();
			SFDL.Expr cond = parseExpr();
			if (debug) System.out.println("if then");
			SFDL.Block tblock = parseBlock();
			if (debug) System.out.println("if then done");
			SFDL.Block fblock = null;
			if (tok.type == TOK_ELSE) {
				if (debug) System.out.println("if else");
				nextToken();
				fblock = parseBlock();
				if (debug) System.out.println("if else done");
			}
			return new SFDL.IfExpr(cond, tblock, fblock);
		case TOK_FOR:
			nextToken();
			SFDL.Expr left = parseExpr(TOK_EQUAL);
			System.out.println(left);
			if (!(left instanceof SFDL.LValExpr)) {
				throw new ParseError("for loop requires lval", tok);
			}
			expect(tok, TOK_EQUAL);
			expect(nextToken(), TOK_NUM);			// TODO should allow consts
			int begin = Integer.parseInt(tok.str);
			expect(nextToken(), TOK_TO);
			expect(nextToken(), TOK_NUM);			// TODO should allow consts
			int end = Integer.parseInt(tok.str);
			nextToken();
			SFDL.Block loopblock = parseBlock();
			return new SFDL.ForExpr((SFDL.LValExpr)left, begin, end, 1, loopblock);
			
		default:
			return parseExpr();
		}
	}
	
	SFDL.Expr parseExpr() {
		return parseExpr(-1);
	}
	
	SFDL.Expr parseExpr(int prio) {
		if (debug) System.out.println("parseExpr prio=" + prio);
		SFDL.Expr leftexpr;
		switch(tok.type) {
		case TOK_NUM:
			leftexpr = new SFDL.IntConst(new BigInteger(tok.str));
			break;
		case TOK_IDENT:
			leftexpr = sfdl.evalVar(tok.str);
			if (leftexpr == null) {
				throw new ParseError("Invalid variable reference ", tok);
			}
			break;
		case TOK_LPAREN:
			nextToken();
			leftexpr = parseExpr();
			expect(tok, TOK_RPAREN);
			break;
		default:
			throw new ParseError("unknown expression ", tok);
		}
		
		nextToken();
		do {
			if(!greaterPrio(tok.type, prio)) {
				if (debug) System.out.println("leave parseExpr, lower prio");
				return leftexpr;
			}
			
			SFDL.Expr rightexpr;
			SFDL.Expr expr;
			
			switch(tok.type) {
			case TOK_EQUAL:
				if (!(leftexpr instanceof SFDL.LValExpr)) {
					throw new ParseError("Illegal lval at assignment", tok);
				}
				SFDL.LValExpr lval = (SFDL.LValExpr) leftexpr; 
				nextToken();
				rightexpr = parseExpr(TOK_EQUAL);
				// TODO: type check
				expr = new SFDL.AssignExpr(lval, rightexpr);
				break;
			case TOK_LBRACKET:	// array reference
				if (!(leftexpr.type instanceof SFDL.ArrayType))
					throw new ParseError("expression is not an array", tok);
				nextToken();
				SFDL.Expr ind = parseExpr();
				expect(tok, TOK_RBRACKET);
				if (leftexpr instanceof SFDL.LValExpr) {
					expr = new SFDL.LArrayRef((LValExpr)leftexpr, ind);
				} else {
					expr = new SFDL.ArrayRef(leftexpr, ind);
				}
				
				nextToken();
				break;
				
			case TOK_PERIOD:	// struct reference
				//System.out.println(leftexpr.type);
				if (!(leftexpr.type instanceof SFDL.StructType))
					throw new ParseError("expression is not a struct", tok);
				expect(nextToken(), TOK_IDENT);
				SFDL.StructRef srexpr;
				if (leftexpr instanceof SFDL.LValExpr) {
					srexpr = new SFDL.LStructRef((LValExpr) leftexpr, tok.str);
				} else {
					srexpr = new SFDL.StructRef(leftexpr, tok.str);
				}
				if (srexpr.type == null) {
					throw new ParseError("field not found", tok);
				}
				System.out.println("structref: " + srexpr);
				expr = srexpr;
				nextToken();
				break;
			case TOK_PLUS:
				// TODO: type check
				nextToken();
				rightexpr = parseExpr(TOK_PLUS);
				expr = new SFDL.AddExpr(leftexpr, rightexpr);
				break;
			case TOK_ASTERISK:
				// TODO: type check
				nextToken();
				rightexpr = parseExpr(TOK_ASTERISK);
				expr = new SFDL.MulExpr(leftexpr, rightexpr);
				break;
			case TOK_DASH:
				// TODO: type check
				nextToken();
				rightexpr = parseExpr(TOK_DASH);
				expr = new SFDL.SubExpr(leftexpr, rightexpr);
				break;
			case TOK_SLASH:
				// TODO: type check
				nextToken();
				rightexpr = parseExpr(TOK_SLASH);
				expr = new SFDL.DivExpr(leftexpr, rightexpr);
				break;
			case TOK_LLT:
				// TODO: type check
				nextToken();
				rightexpr = parseExpr(TOK_LLT);
				expr = new SFDL.LeftShiftExpr(leftexpr, rightexpr);
				break;
			case TOK_GGT:
				// TODO: type check
				nextToken();
				rightexpr = parseExpr(TOK_GGT);
				expr = new SFDL.RightShiftExpr(leftexpr, rightexpr);
				break;
			case TOK_CARET:
				// TODO: type check
				nextToken();
				rightexpr = parseExpr(TOK_CARET);
				expr = new SFDL.XorExpr(leftexpr, rightexpr);
				break;
			case TOK_EQUALEQUAL:
				// TODO: type check
				nextToken();
				rightexpr = parseExpr(TOK_EQUALEQUAL);
				expr = new SFDL.EqExpr(leftexpr, rightexpr);
				break;
			case TOK_NOTEQUAL:
				// TODO: type check
				nextToken();
				rightexpr = parseExpr(TOK_NOTEQUAL);
				expr = new SFDL.NotEqExpr(leftexpr, rightexpr);
				break;
			case TOK_GT:
				// TODO: type check
				nextToken();
				rightexpr = parseExpr(TOK_GT);
				expr = new SFDL.GreaterThanExpr(leftexpr, rightexpr);
				break;
			case TOK_LT:
				// TODO: type check
				nextToken();
				rightexpr = parseExpr(TOK_LT);
				expr = new SFDL.LessThanExpr(leftexpr, rightexpr);
				break;
			default:
				System.out.println("leave parseExpr, unknown symbol");
				return leftexpr;
			}
			
			if (debug) System.out.println("post-op expr: " + expr);
			leftexpr = expr;
		} while (true);
	}
	
	static final int tok_prios[] = new int[TOK_LAST];
	static {
		tok_prios[TOK_EQUAL] = 10;
		tok_prios[TOK_EQUALEQUAL] = 12;
		tok_prios[TOK_NOTEQUAL] = 12;
		tok_prios[TOK_GT] = 12;
		tok_prios[TOK_LT] = 12;
		tok_prios[TOK_PLUS] = 20;
		tok_prios[TOK_DASH] = 20;
		tok_prios[TOK_CARET] = 20;
		tok_prios[TOK_ASTERISK] = 30;
		tok_prios[TOK_SLASH] = 30;
		tok_prios[TOK_LLT] = 31;
		tok_prios[TOK_GGT] = 31;
		tok_prios[TOK_PERIOD] = 100;
		tok_prios[TOK_LBRACKET] = 110;
		tok_prios[TOK_LPAREN] = 200;
		//tok_prios[TOK_RPAREN] = 200;
	}
	
	// return true if right binds tighter than left
	boolean greaterPrio(int right, int left) {
		
		if (right==TOK_SEMICOLON || right==TOK_LBRACE || right==TOK_RPAREN 
				|| right==TOK_TO || right==TOK_BY)
			return false;
		
		// -1 is always top level
		if (left==-1)
			return true;
		
		if (tok_prios[right]==0) {
			throw new ParseError("Unknown post-expr ", tok);
		}
		if (tok_prios[right]==0) {
			throw new ParseError("compiler error: Unknown post-expr " + left, tok);
		}
		
		return tok_prios[right] > tok_prios[left];
	}
/*
			case TOK_NUM :
				var.initialValue = IntPool.create(Integer.parseInt(tok.str));
*/
	
	public static void main(String[] args) throws Exception {
		BufferedReader r = new BufferedReader(new FileReader(args[0]));
		SFDLParser p = new SFDLParser(r);
		p.parse();
		System.out.println("\n\n\n");
		SFDL.printScope(p.sfdl.current);
		r.close();
	}
	
}
