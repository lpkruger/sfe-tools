// DivOperator.java.
// Copyright (C) 2004 Naom Nisan, Ziv Balshai, Amir Levy, (C) 2005 Louis Kruger
// See full copyright license terms in file ../GPL.txt

package fairplay.Compiler;


/**
 * A class that represents integer divide (/) operator that can be defined in the
 * program.
 */
class DivOperator extends Operator implements Multi2SingleBit {
	//~ Methods ----------------------------------------------------------------

	/**
	 * Transforms this multibit expression into singlebit statements
	 * and returns the result.
	 * Note: x-y &lt;==&gt; x+(-y).
	 * @param obj the AssignmentStatement that holds this MinusOperator.
	 * @return a BlockStatement containing the result statements.
	 */
	public BlockStatement multi2SingleBit(Object obj) {
		AssignmentStatement as     = ((AssignmentStatement) obj);
		LvalExpression      lhs    = as.getLHS(); //LHS of the param statement
		BinaryOpExpression  rhs    = (BinaryOpExpression) (as.getRHS());
		BlockStatement      result = new BlockStatement();

		Expression          right = rhs.getRight();
		Expression          left  = rhs.getLeft();

		int size = (lhs.size() < rhs.size()) ? lhs.size() : rhs.size();

		LvalExpression curP = null;
		LvalExpression lastP;

		Type rightType = new IntType(right.size());
		Type boolType = new BooleanType();

		//System.out.println("DIV right " + right.size() + "  left " + left.size() + "  lhs " + lhs.size());

		// one iteration for each bit in divisor (or output)
		for (int i=0; i<size; ++i) {
		    //System.out.println("DIV iteration: " + i);
		    lastP = curP;
		    curP =
			Function.addTempLocalVar(lhs.getName() + "$P" + i, rightType);
		    //curP.setAssigningStatement(as);

		    // left shift P register and bring down next bit
		    result.addStatement(new AssignmentStatement(
		        // lhs
		        curP.lvalBitAt(0),
		        // rhs
			new UnaryOpExpression(new PrimitiveOperator(PrimitiveOperator.ID_OP),
			    left.bitAt(size-i-1))));

		    if (i == 0) {
			for (int j=1; j<left.size(); ++j) {
			    result.addStatement(new AssignmentStatement(
		                // lhs
		                curP.lvalBitAt(j), //currentFunction.fromName(tmp.getName()+"$"+j),
		                // rhs
			        new UnaryOpExpression(new PrimitiveOperator(PrimitiveOperator.ID_OP),
			            new BooleanConstant(false))));
			} 
		    }
		    if (i > 0) {		    
			for (int j=1; j<left.size(); ++j) {
			    result.addStatement(new AssignmentStatement(
			        // lhs
			        curP.lvalBitAt(j),
			        // rhs
			        new UnaryOpExpression(new PrimitiveOperator(PrimitiveOperator.ID_OP),
				    lastP.bitAt(j-1))));
			}
		    }

		    // subtract dividend from P.  if - , output 0.  if pos, output 1 and keep result
		    LvalExpression subQ = Function.addTempLocalVar(lhs.getName() + "$Q" + i, rightType);
		    AssignmentStatement subAs =
			new AssignmentStatement(
			// lhs
			subQ,
			// rhs
			new BinaryOpExpression(new MinusOperator(), curP, right));
		    result.addStatement(subAs.multi2SingleBit(null));

		    // output bit
		    result.addStatement(
			new AssignmentStatement(
			// lhs
			lhs.lvalBitAt(size-i-1),
			// rhs
			new UnaryOpExpression(new PrimitiveOperator(PrimitiveOperator.NOT_OP),
			    subQ.bitAt(subQ.size()-1))));

		    // update curP if necessary, using muxes
		    LvalExpression curP2 = Function.addTempLocalVar(lhs.getName() + "$PP" + i, rightType);
		    for (int j=0; j<curP2.size(); ++j) {
			result.addStatement(new AssignmentStatement(
			    curP2.lvalBitAt(j),
			    new TrinaryOpExpression(new PrimitiveOperator(PrimitiveOperator.MUX_OP),
			        subQ.bitAt(subQ.size()-1),
				curP.bitAt(j),
				subQ.bitAt(j))));
		    }

		    curP = curP2;
		}


		return result;
	}

	/**
	 * Returns 2 as the arity of this UnaryMinusOperator.
	 * Arity is 1 for unary ops; 2 for binary ops; 3 for ternary ops; 0 for constants
	 * @return 2 as the arity of this UnaryMinusOperator.
	 */
	public int arity() {
		return 2;
	}

	/**
	 * Returns a string representation of the object.
	 */
	public String toString() {
		return "/";
	}

	/**
	 * Returns an int theat represents the priority of the operator
	 * @return an int theat represents the priority of the operator
	 */
	public int priority() {
		return 3;
	}
}
