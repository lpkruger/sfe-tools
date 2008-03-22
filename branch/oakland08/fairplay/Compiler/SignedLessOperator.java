// LessOperator.java.
// Copyright (C) 2004 Naom Nisan, Ziv Balshai, Amir Levy.
// See full copyright license terms in file ../GPL.txt

package fairplay.Compiler;

/**
 * A class for representing &lt; operator expressions that can be defined
 * in the program.
 */
public class SignedLessOperator extends Operator implements Multi2SingleBit {
	//~ Methods ----------------------------------------------------------------

	/**
	 * Returns a string representation of the object.
	 */
	public String toString() {
		return "<";
	}

	/**
	 * Returns 2 as the arity of this PlusOperator.
	 * Arity is 1 for unary ops; 2 for binary ops; 3 for ternary ops;
	 * 0 for constants
	 * @return 2 as the arity of this PlusOperator.
	 */
	public int arity() {
		return 2;
	}

	/**
	 * Transforms this multibit expression into singlebit statements
	 * and returns the result.
	 * Note:  x-y&lt;0 &lt;==&gt; x&lt;y.
	 * @param obj the AssignmentStatement that holds this GreaterOperator.
	 * @return a BlockStatement containing the result statements.
	 */
	public BlockStatement multi2SingleBit(Object obj) {	
		AssignmentStatement as     = ((AssignmentStatement) obj);
		LvalExpression      lhs    = as.getLHS(); //LHS of the param statement
		BinaryOpExpression  rhs    = (BinaryOpExpression) (as.getRHS());
		BlockStatement      result = new BlockStatement();

		Expression          right = rhs.getRight();
		Expression          left  = rhs.getLeft();

		// calculate x-y  (x-y<0 <==> x<y)
		LvalExpression leftMinusRight =
			Function.addTempLocalVar(lhs.getName() + "$leftMinusRight",
			                         new IntType((right.size() > left.size())
			                                     ? right.size() : left.size()));

		// create an assignment statement for calculating 
		// y-x and execute multi2SingleBit transforamtion on it
		AssignmentStatement minusAs =
			new AssignmentStatement(
			// lhs
			leftMinusRight,
			                        
			// rhs
			new BinaryOpExpression(new MinusOperator(), left, right));

		//execute multi2SingleBit transforamtion
		result.addStatement(minusAs.multi2SingleBit(null));

		// assign the most significant bit to the result
		// assign the most significant bit to the result
		//System.out.println("LESS lhs " + lhs.getClass() + " " + lhs.size());
		for (int i = 0; i < lhs.size(); i++)
			result.addStatement(new AssignmentStatement(
			// lhs
			lhs.lvalBitAt(i), //currentFunction.fromName(lhs.getName()+"$"+i),
			                                            new UnaryOpExpression(new PrimitiveOperator(PrimitiveOperator.ID_OP),
			                                                                  leftMinusRight.bitAt(leftMinusRight.size()))));

		return result;
	}

	/**
	 * Returns an int theat represents the priority of the operator
	 * @return an int theat represents the priority of the operator
	 */
	public int priority() {
		return 1;
	}
}
