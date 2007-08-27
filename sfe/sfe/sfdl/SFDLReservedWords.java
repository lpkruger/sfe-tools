/**
 * 
 */
package sfe.sfdl;

import java.lang.reflect.Field;

interface SFDLReservedWords {
	static final int TOK_PROGRAM = 100;
	static final int TOK_TYPE = 101;
	static final int TOK_VAR = 102;
	static final int TOK_CONST = 103;
	static final int TOK_STRUCT = 104;
	static final int TOK_BOOLEAN = 105;
	static final int TOK_INT = 106;
	static final int TOK_IF = 107;
	static final int TOK_ELSE = 108;
	static final int TOK_FOR = 109;
	static final int TOK_TO = 110;
	static final int TOK_BY = 111;
	static final int TOK_FUNCTION = 112;
	
	// must be greater than all others
	static final int TOK_LAST = 200;
}