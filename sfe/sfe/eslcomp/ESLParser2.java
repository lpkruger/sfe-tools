/*
 * Created on Jun 5, 2003
 *
 * To change the template for this generated file go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
package sfe.eslcomp;

import java.util.Iterator;
import java.util.List;

/**
 * @author lpkruger
 *
 * ESL Parser phase 2
 */
public class ESLParser2 implements TokenizerConstants {
	ESLParser eslp;
	ESL esl;
	EDL edl;
	MapList umes;

	ESLParser2(ESLParser eslp) {
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
				ESLParser.UnprocessedMatchEvent ume =
					(ESLParser.UnprocessedMatchEvent) it2.next();
				process(ume);
			}
		}
	}

	// generate EDL match rules
	public void process(ESLParser.UnprocessedMatchEvent ume) {
		ESL.Match match = new ESL.Match();
		match.policy = ume.policy;
		match.evtName = ume.event.name;
		match.cexprs = new ESL.CExpr[ume.cexprs.size()];
		if (ume.ablock != null) {
			String abstr = ume.ablock.str.trim();
			if (!abstr.endsWith(";")) {
				abstr += ";";
			}
			match.ablock = new ESL.CExpr(abstr);

		}
		for (int i = 0; i < match.cexprs.length; ++i) {
			match.cexprs[i] =
				new ESL.CExpr(((Tokenizer.Token) ume.cexprs.get(i)).str);
		}
		match.argrules = new ESL.Rule[ume.args.size()];
		for (int i = 0; i < match.argrules.length; ++i) {
			Tokenizer.Token tok = (Tokenizer.Token) ume.args.get(i);
			EDL.Type declaredtype = ume.event.args[i].type;

			if (tok.type == TOK_STRING) {
				//	assume regex
				match.argrules[i] = new ESL.RegexRule(tok.str, false);
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
					match.argrules[i] = new ESL.TypeRule(expectedtype);
				} else {
					// maybe its a variable
					ESL.VarDecl var = (ESL.VarDecl) esl.vars.get(tok.str);
					if (var != null) {
						if (EDL.isSubtype(var.type.getEDL().typeString, var.type)) {
							match.argrules[i] =
								new ESL.RegexRule(var.name, true);
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
				match.argrules[i] = new ESL.TrueRule();
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
