/*
 * Created on Apr 19, 2003
 *
 * To change the template for this generated file go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */

package sfe.eslcomp;

import java.io.BufferedReader;
import java.util.ArrayList;

import sfe.eslcomp.EDL.EventArg;
import sfe.eslcomp.EDL.Type;

/**
 * @author lpkruger
 *
 * This is an EDL parser.  It creates an EDL
 * representation of defined types and events, which can be used
 * to generate headers for the C interface
 */
public class EDLParser extends Parser {

	static final int TOK_TYPEDEF = 100;
	static final int TOK_SUBTYPE = 101;
	static final int TOK_EVENT = 102;
	static final int TOK_UNION = 103;
	static final int TOK_STRUCT = 104;

	static final int TOK_IN = 110;
	static final int TOK_OUT = 111;
	static final int TOK_INOUT = 112;

	static {
		add("typedef", TOK_TYPEDEF);
		add("subtype", TOK_SUBTYPE);
		add("event", TOK_EVENT);
		add("union", TOK_UNION);
		add("struct", TOK_STRUCT);
		add("in", TOK_IN);
		add("out", TOK_OUT);
		add("inout", TOK_INOUT);
	}

	EDL edl;

	public EDLParser(BufferedReader r, String name) {
		toker = new Tokenizer(r);
		edl = new EDL();
		edl.edlname = name;
	}

	public void parse() {
		nextToken();
		while (tok.type != TOK_EOF) {
			parseStatement();
		}
	}

	void parseStatement() {
		switch (tok.type) {
			case TOK_TYPEDEF :
				parseTypedef();
				break;
			case TOK_SUBTYPE :
				parseSubtype();
				break;
			case TOK_EVENT :
				parseEvent();
				break;
			case TOK_STRUCT :
				parseStruct();
				break;
			default :
				throw new ParseError("Expected statement", tok);
		}

	}

	void parseTypedef() {
		expect(tok, TOK_TYPEDEF);

		Tokenizer.Token name;
		expect((name = nextToken()), TOK_IDENT);

		expect(nextToken(), TOK_EQUAL);

		if (!edl.isAvailable(name.str)) {
			throw new ParseError("Name is already bound", name);
		}

		nextToken();
		EDL.Type type = parseType();

		// Bind the name to the type
		edl.bind(name.str, type);
	}

	void parseSubtype() {
		expect(tok, TOK_SUBTYPE);
		Tokenizer.Token name;
		expect((name = nextToken()), TOK_IDENT);
		if (!edl.isAvailable(name.str)) {
			throw new ParseError("Name is already bound", name);
		}

		expect(nextToken(), TOK_EQUAL);

		nextToken();
		EDL.Type type = parseType();

		EDL.SubType stype = edl.new SubType(name.str, type);
		edl.bind(name.str, stype);
	}

	EDL.Type parseType() {
		EDL.Type type;
		switch (tok.type) {
			case TOK_UNION :
				type = parseUnion();
				break;
			case TOK_IDENT :
				type = parseTypeId();
				break;
			default :
				throw new ParseError("Expected type", tok);
		}
		while (tok.type == TOK_ASTERISK || tok.type == TOK_LBRACKET) {
			if (tok.type == TOK_ASTERISK) { // pointer
				type = type.getPtrType();
				nextToken();
			} else if (tok.type == TOK_LBRACKET) { // array
				type = type.getArrayType();
				nextToken();
				if (tok.type == TOK_RBRACKET) { // [] generic array
					nextToken();
				} else if (tok.type == TOK_NUM) { //  [100] definate array
					type =
						(((EDL.ArrayType) type)
							.getDefinateType(Integer.parseInt(tok.str)));
					expect(nextToken(), TOK_RBRACKET);
					nextToken();
				} else if (tok.type == TOK_IDENT) { // [varlength] 
					type =
						edl.new IndefinateArrayType(
							((EDL.ArrayType) type).eltype,
							tok.str,
							tok);
					expect(nextToken(), TOK_RBRACKET);
					nextToken();
				}
			}
		}
		return type;
	}

	EDL.UnionType parseUnion() {
		expect(tok, TOK_UNION);

		ArrayList<Type> tt = new ArrayList<Type>();
		do {
			nextToken();
			EDL.Type type = parseType();
			tt.add(type);
		} while (tok.type == TOK_PIPE);
		return edl.new UnionType(
			(EDL.Type[]) tt.toArray(new EDL.Type[0]),
			"union" + (edl.unionCounter++));

	}

	void parseStruct() {
		expect(tok, TOK_STRUCT);
		Tokenizer.Token name;
		expect(name = nextToken(), TOK_IDENT);
		if (!edl.isAvailable(name.str)) {
			throw new ParseError("Name is already bound", name);
		}
		expect(nextToken(), TOK_LPAREN);
		ArrayList<Type> ftypes = new ArrayList<Type>();
		ArrayList<String> fnames = new ArrayList<String>();
		do {
			nextToken();
			EDL.Type type = parseType();
			ftypes.add(type);
			expect(tok, TOK_IDENT);
			fnames.add(tok.str);
			nextToken();

		} while (tok.type == TOK_COMMA);
		expect(tok, TOK_RPAREN);
		nextToken();

		EDL.Type type =
			edl.new StructType(
				(EDL.Type[]) ftypes.toArray(new EDL.Type[0]),
				(String[]) fnames.toArray(new String[0]),
				name.str);
		edl.bind(name.str, type);
	}

	EDL.Type parseTypeId() {
		expect(tok, TOK_IDENT);
		try {
			EDL.Type type = edl.getType(tok.str);
			if (type == null) {
				throw new ParseError("Unknown type", tok);
			}
			nextToken();
			return type;
		} catch (ClassCastException ex) {
		}
		throw new ParseError("Expected type", tok);
	}

	void parseEvent() {
		expect(tok, TOK_EVENT);
		Tokenizer.Token name;
		expect((name = nextToken()), TOK_IDENT);
		expect(nextToken(), TOK_LPAREN);
		nextToken();
		ArrayList<EventArg> args = new ArrayList<EventArg>();

		boolean expectcomma = false;
		while (tok.type != TOK_RPAREN) {
			if (expectcomma) {
				expect(tok, TOK_COMMA);
				nextToken();
			}
			boolean argin = true;
			boolean argout = false;
			if (tok.type == TOK_IN
				|| tok.type == TOK_OUT
				|| tok.type == TOK_INOUT) {
				switch (tok.type) {
					case TOK_IN :
						argin = true;
						break;
					case TOK_OUT :
						argout = true;
						argin = false;
						break;
					case TOK_INOUT :
						argin = argout = true;
						break;
				}
				nextToken();
			}
			EDL.Type atype = parseType();

			expect(tok, TOK_IDENT);

			if (!edl.isAvailable(tok.str)) {
				throw new ParseError("Name is already bound", tok);
			}
			for (int i = 0; i < args.size(); ++i) {
				if (args.get(i).name.equals(tok.str)) {
					throw new ParseError("Name is already bound", tok);
				}
			}
			EDL.EventArg arg = new EDL.EventArg(atype, tok.str, argin, argout);
			args.add(arg);
			nextToken();
			expectcomma = true;
		}
		expect(nextToken(), TOK_EQUAL);
		nextToken();
		EDL.Type rettype = parseType();

		EDL.Event event =
			new EDL.Event(
				name.str,
				(EDL.EventArg[]) args.toArray(new EDL.EventArg[0]),
				rettype);
		edl.event(event);

		// resolve indefinate array types
		for (int i = 0; i < event.args.length; ++i) {
			if (event.args[i].type instanceof EDL.IndefinateArrayType) {
				EDL.IndefinateArrayType itype =
					(EDL.IndefinateArrayType) event.args[i].type;
			
				for (int j = 0; j < event.args.length; ++j) {
					if (itype.len.equals(event.args[j].name)) {
						itype.setLengthDef(event.args[j]);
						break;
					}
				}
				if (itype.lendef == null) {
					throw new ParseError("Invalid array length", itype.lentok);
				}
			}
		}
	}

	public EDL getEDL() {
		return this.edl;
	}
}
