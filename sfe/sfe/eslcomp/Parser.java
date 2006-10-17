/*
 * Created on May 21, 2003
 *
 * To change the template for this generated file go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */

package sfe.eslcomp;
import java.util.*;

/**
 * @author lpkruger
 *
 * A base class which provides some generic tools for both the ESL
 * and EDL parsers.
 */
public abstract class Parser implements TokenizerConstants {
	class ParseError extends RuntimeException {
		ParseError(String msg, Tokenizer.Token tok) {
			super(msg + ": \"" + tok + "\"" + " at line: "+			Integer.toString(tok.line)+"; column: "+			Integer.toString(tok.col));
		}
	}

	protected static Map<String, Integer> reservedMap = new HashMap<String, Integer>();

	protected static void add(String str, int n) {
		reservedMap.put(str, new Integer(n));
	}

	Tokenizer toker;
	Tokenizer.Token tok;
    boolean debug = false;
    
	Tokenizer.Token nextToken() {
		tok = toker.nextToken();
		if (tok.type == TOK_IDENT) {
			Integer nn = reservedMap.get(tok.str);
			if (nn != null)
				tok.type = nn.intValue();
		}

		if (debug) System.err.println("Read token \"" + tok.str + "\" type = " + tok.type);
		//if (debug) Thread.dumpStack();
		
		return tok;
	}
	
	void expect(Tokenizer.Token tok, int type) {
		if (tok.type != type) {
			//			TODO: replace number by name
			throw new ParseError("Expected " + type, tok);
		}
	}
}
