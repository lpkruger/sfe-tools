/*
 * Created on Apr 21, 2003
 *
 * To change the template for this generated file go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */

package sfe.sfdl;

import java.util.*;
import java.io.*;

import sfe.sfdl.Tokenizer.Token;
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
		add("for", TOK_FOR);
		add("function", TOK_FUNCTION);
	}
	
	SFDL sfdl;
	String programname;
	
	MapList umes = new MapList(); // unprocessed events go here awaiting
	// 2nd pass

	public SFDLParser(BufferedReader r) {
		toker = new Tokenizer(r);
		debug = true;

	}

	/*
	public void parse() {
		expect(nextToken(), TOK_PROGRAM);
		parseESL();
		while (tok.type != TOK_EOF) {
			parseStatement();
		}
	}
    */
	
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
		mainloop:
		for (;;) {
			switch(nextToken().type) {
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
		// TODO: add definition to scope
	}
	
	void parseTypeDef() {
		expect(tok, TOK_TYPE);
		expect(nextToken(), TOK_IDENT);
		String name = tok.str;
		expect(nextToken(), TOK_EQUAL);
		nextToken();
		SFDL.Type type = parseTypeExpr();
	}
	
	void parseFunctionDef() {
		expect(tok, TOK_FUNCTION);
		nextToken();
		SFDL.Type type = parseTypeExpr();
		expect(nextToken(), TOK_IDENT);
		String name = tok.str;
		expect(nextToken(), TOK_LPAREN);
		
		ArrayList<SFDL.Type> parmTypes = new ArrayList<SFDL.Type>();
		ArrayList<String> parmNames = new ArrayList<String>(); 
		for(;;) {
			if(nextToken().type == TOK_RPAREN)
				break;
			
			SFDL.Type paramType = parseTypeExpr();
			expect(nextToken(), TOK_IDENT);
			String paramName = tok.str;
			parmTypes.add(paramType);
			parmNames.add(paramName);
		}
		
		expect(tok, TOK_RPAREN);
		expect(nextToken(), TOK_LBRACE);
		
		// parse function body
		// parseExpr()
		// check for TOK_RBRACE
		
		
		
	}
	SFDL.Type parseTypeExpr() {
		switch(tok.type) {
		case TOK_IDENT:
			SFDL.Type type = sfdl.getType(tok.str);
			if (type == null)
				throw new ParseError("Undefined type", tok);

			nextToken();
			return type;
			
		case TOK_STRUCT:
			throw new ParseError("unimplemented compiler feature", tok);
			
		case TOK_INT:
			expect(nextToken(), TOK_LBRACKET);
			SFDL.Expr value = parseExpr();
			if (!value.isConst())
				throw new ParseError("Constant expression required", tok);
			
			// throw new ParseError("Integer constant expression required", tok);
			SFDL.IntConst ival = (SFDL.IntConst) value;
			return new SFDL.IntType(ival.number);
		default:
			throw new ParseError("not a type ", tok);
			
			
		}
		return null;
	}
	
	SFDL.Expr parseExpr() {
		return null;		
	}
	
	void parseMatch() {
		expect(tok, TOK_MATCH);

		Tokenizer.Token nameTok;
		expect((nameTok = nextToken()), TOK_IDENT);

		EDL.Event event = edl.getEvent(nameTok.str);
		if (event == null) {
			throw new ParseError("Undefined event", nameTok);
		}

		expect(nextToken(), TOK_LPAREN);
		nextToken();
		ArrayList<Token> args = new ArrayList<Token>();
		ArrayList<Token> cexprs = new ArrayList<Token>();
		// array of tokens, until we process the meaning

		boolean expectcomma = false;
		while (tok.type != TOK_RPAREN) {
			if (expectcomma) {
				expect(tok, TOK_COMMA);
				nextToken();
			}
			if (tok.type == TOK_CEXPR) {
				cexprs.add(tok);
			} else {
				args.add(tok);
			}
			nextToken();
			expectcomma = true;
		}

		// TODO: optional = rettype
		// expect(nextToken(), TOK_EQUAL);
		// nextToken();
		// EDL.Type rettype = parseType();

		// -> policy
		expect(nextToken(), TOK_RARROW);
		int policy;
		switch (nextToken().type) {
			case TOK_ALLOW :
				// allow
				policy = SFDL.POLICY_ALLOW;
				nextToken();
				break;
			case TOK_DENY :
				// deny
				policy = SFDL.POLICY_DENY;
				nextToken();
				break;
			default :
				throw new ParseError("Expected policy", tok);
		}

		Tokenizer.Token ablock = null;
		if (tok.type == TOK_CEXPR) {
			// there's an action block here
			ablock = tok;
			nextToken();
		}

		UnprocessedMatchEvent ume =
			new UnprocessedMatchEvent(args, event, policy, cexprs, ablock);

		umes.add(ume.event.name, ume);

	}

	void parseVarDecl() {
		EDL.Type typ = edl.getType(tok.str);
		if (typ == null) {
			throw new ParseError("Unknown type", tok);
		}
		expect(nextToken(), TOK_IDENT);
		SFDL.VarDecl var = esl.vars.get(tok.str);
		if (var != null) {
			throw new ParseError("Variable already declared", tok);
		}
		var = new SFDL.VarDecl();
		var.name = tok.str;
		var.type = typ;
		esl.vars.put(var.name, var);
		nextToken();
		if (tok.type != TOK_EQUAL)
			return;
		// there's an initial value
		nextToken();
		switch (tok.type) {
			case TOK_STRING :
				var.initialValue = tok.str;
				break;
			case TOK_NUM :
				var.initialValue = IntPool.create(Integer.parseInt(tok.str));
				break;
		}
		nextToken();

	}

	static class UnprocessedMatchEvent {
		ArrayList<Token> args;
		ArrayList<Token> cexprs;
		EDL.Event event;
		int policy;
		Tokenizer.Token ablock;

		UnprocessedMatchEvent(
			ArrayList<Token> args,
			EDL.Event event,
			int policy,
			ArrayList<Token> cexprs,
			Tokenizer.Token ablock) {
			this.args = args;
			this.event = event;
			this.policy = policy;
			this.cexprs = cexprs;
			this.ablock = ablock;
		}
	}

	public void parseESL() {
		expect(tok, TOK_EDL);
		expect(nextToken(), TOK_STRING);
		try {
			FileReader fr = new FileReader(tok.str);
			EDLParser edlp = new EDLParser(new BufferedReader(fr), tok.str);
			edlp.parse();
			edl = edlp.edl;
			fr.close();
		} catch (IOException ex) {
			throw new ParseError("EDL file not found", tok);
		}
		nextToken();

		esl = new SFDL(edl);
		esl.eslname = this.eslname;
	}
}
