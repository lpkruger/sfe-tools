/*
 * Created on Apr 19, 2003
 *
 * To change the template for this generated file go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */

package sfe.sfdl;
import java.io.*;

/**
 * @author lpkruger
 *
 * To change the template for this generated type comment go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
import static sfe.sfdl.TokenizerConstants.*;
public class Tokenizer {

	public class TokenizerError extends RuntimeException {
		int line;
		int col;
		TokenizerError(String msg) {
			super(msg);
			this.line = Tokenizer.this.line;
			this.col = Tokenizer.this.col - 1;
		}
	}

	static class EOFException extends Exception {
	}

	public class Token {
		int type;
		int line;
		int col;
		String str;

		Token(int type, String str) {
			this.type = type;
			this.str = str;
			this.line = Tokenizer.this.line;
			this.col = Tokenizer.this.col - str.length();
		}

		public String toString() {
			return str;
		}
	}

	int line = 1;
	int col = 1;
	BufferedReader r;

	Token savedToken;

	public Tokenizer(BufferedReader r) {
		this.r = r;
	}

	char readChar() throws EOFException {
		int ch;
		if (oldchar > 0) {
			ch = oldchar;
			oldchar = 0;
		} else {
			try {
				ch = r.read();
				if (ch == -1) {
					r = null;
					throw new EOFException();
				}
			} catch (IOException ex) {
				throw new TokenizerError(ex.toString());
			}
		}
		oldline = line;
		oldcol = col;
		if (ch == '\n') {
			++line;
			col = 1;
		} else {
			++col;
		}
		return (char) ch;
	}

	int oldline;
	int oldcol;
	char oldchar;

	void pushBack(char ch) {
		oldchar = ch;
		line = oldline;
		col = oldcol;
	}

	public Token nextToken() {
		if (savedToken != null) {
			Token ret = savedToken;
			savedToken = null;
			return ret;
		}
		if (r == null)
			return new Token(TOK_EOF, null);
		char ch;
		try {
			while (Character.isWhitespace((ch = readChar())));

			if (Character.isLetter(ch) || ch == '_') {
				// read an identifier
				StringBuffer sb = new StringBuffer();
				do {
					sb.append(ch);
					ch = readChar();
				} while (Character.isLetterOrDigit(ch) || ch == '_');
				pushBack(ch);
				return new Token(TOK_IDENT, sb.toString());
			} else if (Character.isDigit(ch)) {
				// read a numeric constant
				StringBuffer sb = new StringBuffer();
				do {
					sb.append(ch);
					ch = readChar();
				} while (Character.isDigit(ch));
				pushBack(ch);
				return new Token(TOK_NUM, sb.toString());
			} else {
				switch (ch) {
					case '{' :
						return new Token(TOK_LBRACE, "{");
					case '}' :
						return new Token(TOK_RBRACE, "}");
					case '(' :
						return new Token(TOK_LPAREN, "(");
					case ')' :
						return new Token(TOK_RPAREN, ")");
					case '[' :
						return new Token(TOK_LBRACKET, "[");
					case ']' :
						return new Token(TOK_RBRACKET, "]");
					case '|' :
						return new Token(TOK_PIPE, "|");
					case ';' :
						return new Token(TOK_SEMICOLON, ";");
					case ',' :
						return new Token(TOK_COMMA, ",");
					case '*' :
						return new Token(TOK_ASTERISK, "*");
					case '=' :
						return new Token(TOK_EQUAL, "=");
					case '#' :
						while ((ch = readChar()) != '\n');
						return nextToken();
					case '"' :
						return readString();
					case '-' :
						ch = readChar();
						if (ch == '>')
							return new Token(TOK_RARROW, "->");
						break;
					case '<' :
						ch = readChar();
						if (ch == '<')
							return readCexpr();
				}

				throw new TokenizerError(
					"Unexpected character: \"" + ch + "\"");
			}
		} catch (EOFException ex) {
			return new Token(TOK_EOF, "");
		}
	}

	Token readCexpr() {
		StringBuffer sb = new StringBuffer();
		try {
			while (true) {
				char ch = readChar();
				if (ch == '>') {
					char ch2 = readChar();
					if (ch2 == '>') {
						return new Token(TOK_CEXPR, sb.toString());
					} else {
						pushBack(ch2);
						sb.append(ch);
					}

				} else {
					sb.append(ch);
				}
			}
		} catch (EOFException ex) {
			throw new TokenizerError("Unexpected EOF in cexpr");
		}
	}

	Token readString() {
		StringBuffer sb = new StringBuffer();
		char ch;
		try {
			while ((ch = readChar()) != '"') {
				if (ch != '\\') {
					sb.append(ch);
				} else {
					ch = readChar();
					switch (ch) {
						case 'n' :
							sb.append('\n');
							break;
						default :
							sb.append(ch);
							break;
					}
				}
			}
		} catch (EOFException ex) {
			throw new TokenizerError("Unexpected EOF in string constant");
		}

		// auto concatenation of consecutive strings
		// TODO: just realized this is recursive.  Not wrong, but less efficient
		Token next = nextToken();
		while (next.type == TOK_STRING) {
			sb.append(next.str);
			next = nextToken();
		}
		savedToken = next;

		return new Token(TOK_STRING, sb.toString());
	}
}
