/*
 * Created on Apr 21, 2003
 *
 * To change the template for this generated file go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */

package sfe.eslcomp;

import java.util.*;
import java.io.*;

import Test1.TOKENS;

import sfe.eslcomp.Tokenizer.Token;

/**
 * @author lpkruger
 *
 * This is an immediate execution parser.  It creates an SDL
 * representation of defined types and events, which can be used
 * to generate headers for the C interface
 */
public class ESLParser extends Parser {

	static final int TOK_EDL = 200;
	static final int TOK_MATCH = 201;
	static final int TOK_ALLOW = 202;
	static final int TOK_DENY = 203;

	/*
	static {
		add("edl", TOK_EDL);
		add("match", TOK_MATCH);
		add("allow", TOK_ALLOW);
		add("deny", TOK_DENY);
	}
*/
	
	static {
		add("func", TOK_FUNC); 
		add("for", TOKENS.FOR);
		add("int", TOKENS.FOR);
		add("if", TOKENS.IF);
		add("else", TOKENS.ELSE);
	}
	EDL edl;
	ESL esl;
	String eslname;

	MapList umes = new MapList(); // unprocessed events go here awaiting
	// 2nd pass

	public ESLParser(BufferedReader r, String name) {
		toker = new Tokenizer(r);
		this.eslname = name;
		debug = true;

	}

	public void parse() {
		nextToken();
		parseESL();
		while (tok.type != TOK_EOF) {
			parseStatement();
		}
	}

	void parseStatement() {
		switch (tok.type) {
			case TOK_MATCH :
				parseMatch();
				break;
			case TOK_IDENT :
				parseVarDecl();
				break;
			default :
				throw new ParseError("Expected statement", tok);
		}
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
				policy = ESL.POLICY_ALLOW;
				nextToken();
				break;
			case TOK_DENY :
				// deny
				policy = ESL.POLICY_DENY;
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
		ESL.VarDecl var = esl.vars.get(tok.str);
		if (var != null) {
			throw new ParseError("Variable already declared", tok);
		}
		var = new ESL.VarDecl();
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

		esl = new ESL(edl);
		esl.eslname = this.eslname;
	}
}
