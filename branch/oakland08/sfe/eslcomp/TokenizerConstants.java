/*
 * Created on Apr 19, 2003
 *
 * To change the template for this generated file go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */

package sfe.eslcomp;
/**
 * @author lpkruger
 *
 * To change the template for this generated type comment go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
public interface TokenizerConstants {
	static final int TOK_IDENT = 1;
	static final int TOK_NUM = 2;
	static final int TOK_STRING = 3;
	static final int TOK_CEXPR = 4;

	static final int TOK_LBRACE = 10;
	static final int TOK_RBRACE = 11;
	static final int TOK_LPAREN = 12;
	static final int TOK_RPAREN = 13;
	static final int TOK_PIPE = 14;
	static final int TOK_SEMICOLON = 15;
	static final int TOK_COMMA = 16;
	static final int TOK_EQUAL = 17;
	static final int TOK_ASTERISK = 18;
	static final int TOK_LBRACKET = 19;
	static final int TOK_RBRACKET = 20;
	
	static final int TOK_RARROW = 30; // ->

	static final int TOK_EOF = 99;

}
