/*
 * Created on Apr 19, 2003
 *
 * To change the template for this generated file go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */

package sfe.sfdl;
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
	
	static final int TOK_LT = 21;
	static final int TOK_GT = 22;
	static final int TOK_PERIOD = 23;
	
	static final int TOK_NOTEQUAL = 30;
	static final int TOK_EQUALEQUAL = 31;
	// static final int TOK_RARROW = 30; // ->
	
	static final int TOK_PLUS = 40;
	static final int TOK_DASH = 41;
	static final int TOK_SLASH = 43;
	static final int TOK_CARET = 44;
	
	static final int TOK_SLASHSLASH = 90;
	static final int TOK_SLASHSTAR = 91;
	static final int TOK_STARSLASH = 92;
	

	static final int TOK_EOF = 99;
}
