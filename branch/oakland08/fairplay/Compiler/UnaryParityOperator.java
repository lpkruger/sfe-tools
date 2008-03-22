// UnaryMinusOperator.java.
// Copyright (C) 2004 Naom Nisan, Ziv Balshai, Amir Levy.
// See full copyright license terms in file ../GPL.txt

package fairplay.Compiler;


/**
 *
 */
class UnaryParityOperator extends Operator implements Multi2SingleBit {
	//~ Methods ----------------------------------------------------------------

	/**
	 * Transforms this multibit expression into singlebit statements
	 * and return the result.
	 * Note: -y is (!y)+1.
	 * @param obj the AssignmentStatement that holds this UnaryMinusOperator.
	 * @return a BlockStatement containing the result statements
	 */
	public BlockStatement multi2SingleBit(Object obj) {
		AssignmentStatement as     = ((AssignmentStatement) obj);
		LvalExpression      lhs    = as.getLHS(); //LHS of the param statement
		UnaryOpExpression   rhs    = (UnaryOpExpression) (as.getRHS());
		BlockStatement      result = new BlockStatement();

		// !y
		// create a temporary lvalue that will hold the mid expression result
		LvalExpression tmp =
			Function.addTempLocalVar(lhs.getName() + "$par", 
					new IntType(rhs.size()));

		
		
		for (int i=0; i<tmp.size(); ++i) {
			Expression last = (i>0) ? tmp.bitAt(i-1) 
        			: new BooleanConstant(false);
			
			result.addStatement(new AssignmentStatement(
        			// lhs
        			i<tmp.size()-1 ? tmp.lvalBitAt(i) : lhs.lvalBitAt(0),
        			// rhs
        			new BinaryOpExpression(new PrimitiveOperator(
        					PrimitiveOperator.XOR_OP),
        					last, rhs.getMiddle().bitAt(i))));
		}

		return result;
	}

	/**
	 * Returns 1 as the arity of this UnaryMinusOperator.
	 * Arity is 1 for unary ops; 2 for binary ops; 3 for ternary ops; 0 for constants
	 * @return 1 as the arity of this UnaryMinusOperator.
	 */
	public int arity() {
		return 1;
	}

	/**
	 * Returns a string representation of the object.
	 * @return a string representation of the object.
	 */
	public String toString() {
		return "@";
	}

	/**
	 * Returns an int theat represents the priority of the operator
	 * @return an int theat represents the priority of the operator
	 */
	public int priority() {
		return 3;
	}
}
