/*
 * Created on Jun 5, 2003
 *
 * To change the template for this generated file go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
package sfe.sfdl;

import java.util.Iterator;
import java.util.List;

/**
 * @author lpkruger
 *
 * ESL Parser phase 2
 */
public class ESLParser2 implements TokenizerConstants {
	SFDLParser eslp;
	SFDL esl;
	EDL edl;
	MapList umes;

	ESLParser2(SFDLParser eslp) {
		this.eslp = eslp;
		this.esl = eslp.esl;
		this.edl = eslp.edl;
		this.umes = eslp.umes;
	}

	public void process() {
		Iterator it = umes.keySet().iterator();
		while (it.hasNext()) {
			List l = umes.get(it.next());
			Iterator it2 = l.iterator();
			while (it2.hasNext()) {
				SFDLParser.UnprocessedMatchEvent ume =
					(SFDLParser.UnprocessedMatchEvent) it2.next();
				process(ume);
			}
		}
	}

	// generate EDL match rules
	public void process(SFDLParser.UnprocessedMatchEvent ume) {
		SFDL.Match match = new SFDL.Match();
		match.policy = ume.policy;
		match.evtName = ume.event.name;
		match.cexprs = new SFDL.CExpr[ume.cexprs.size()];
		if (ume.ablock != null) {
			String abstr = ume.ablock.str.trim();
			if (!abstr.endsWith(";")) {
				abstr += ";";
			}
			match.ablock = new SFDL.CExpr(abstr);

		}
		for (int i = 0; i < match.cexprs.length; ++i) {
			match.cexprs[i] =
				new SFDL.CExpr(((Tokenizer.Token) ume.cexprs.get(i)).str);
		}
		match.argrules = new SFDL.Obj[ume.args.size()];
		for (int i = 0; i < match.argrules.length; ++i) {
			Tokenizer.Token tok = (Tokenizer.Token) ume.args.get(i);
			EDL.Type declaredtype = ume.event.args[i].type;

			if (tok.type == TOK_STRING) {
				//	assume regex
				match.argrules[i] = new SFDL.RegexRule(tok.str, false);
			} else if (tok.type == TOK_IDENT) {
				EDL.Type expectedtype = edl.getType(tok.str);

				if (expectedtype != null) {
					// expectedtype better be a subtype of the
					// declaredtype
					if (!EDL.isSubtype(declaredtype, expectedtype)) {
						throw new ESLSemanticError(
							"type "
								+ expectedtype.getName()
								+ " is not of type "
								+ declaredtype.getName());
					}

					// do an instanceof type check
					match.argrules[i] = new SFDL.TypeRule(expectedtype);
				} else {
					// maybe its a variable
					SFDL.VarDecl var = (SFDL.VarDecl) esl.vars.get(tok.str);
					if (var != null) {
						if (EDL.isSubtype(var.type.getEDL().typeString, var.type)) {
							match.argrules[i] =
								new SFDL.RegexRule(var.name, true);
						} else {
							throw new ESLSemanticError(
								"don't (yet) know how to match type "
									+ var.type.getName());
						}
					} else {
						// TODO: maybe this could be legal in some circumstance?
						throw new ESLSemanticError(
							"unknown type or variable" + tok.str);
					}
				}
			} else {
				System.out.println("Unknown token " + tok);
				match.argrules[i] = new SFDL.TrueRule();
			}
		}
		esl.evmap.add(match.evtName, match);
	}

	static class ESLSemanticError extends RuntimeException {
		ESLSemanticError(String msg) {
			super(msg);
		}
	}
	/*
	  algorithm:
	  1) process all type arguments
	    - if TOK_STRING, assume regex - unify with String
	    - if TOK_NUMBER, unify with int
	    - if TOK_IDENTIFIER, unify with type
	  2) coalesce all rules of same event, in order with default final
	  ALLOW rule
	  3) generate C code
	  
	*/

}
