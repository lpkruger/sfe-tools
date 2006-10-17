// LessOperator.java.
// Copyright (C) 2004 Naom Nisan, Ziv Balshai, Amir Levy.
// See full copyright license terms in file ../GPL.txt

package fairplay.Compiler;

/**
 * A class for representing &lt; operator expressions that can be defined
 * in the program.
 */
public class LessOperator extends Operator implements Multi2SingleBit {
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

		Type boolType = new BooleanType();
		
		int local = 0;
		
		
		LvalExpression curP;
		LvalExpression lastP = null;
		
		int diff = right.size() - left.size();
		
		Type tmpType = new IntType((diff > 0) ? right.size() : left.size());
		LvalExpression tmp = Function.addTempLocalVar(lhs.getName() + "$T", tmpType);
		LvalExpression know = Function.addTempLocalVar(lhs.getName() + "$K", tmpType);
		
		int i=0;
		if (diff > 0) {
            for (i=0; i<diff; ++i) {
            	Expression tlast = (i>0) ? tmp.bitAt(tmp.size()-i) 
            			: new BooleanConstant(false);
            	Expression klast = (i>0) ? tmp.bitAt(tmp.size()-i) 
            			: new BooleanConstant(false);
            	
            	result.addStatement(new AssignmentStatement(
            			// lhs
            			tmp.lvalBitAt(tmp.size()-i-1),
            			// rhs
            			new BinaryOpExpression(new PrimitiveOperator(
            					PrimitiveOperator.OR_OP),
            					tlast, right.bitAt(right.size()-i-1))));

            	result.addStatement(new AssignmentStatement(
            			// lhs
            			know.lvalBitAt(know.size()-i-1),
            			// rhs
            			new BinaryOpExpression(new PrimitiveOperator(
            					PrimitiveOperator.OR_OP),
            					tlast, right.bitAt(right.size()-i-1))));
            }
		}
		
		if (diff < 0) {
			for (i=0; i<(-diff); ++i) {
			  	Expression tlast = (i>0) ? tmp.bitAt(tmp.size()-i) 
            			: new BooleanConstant(true);
			  	Expression klast = (i>0) ? know.bitAt(tmp.size()-i) 
            			: new BooleanConstant(false);
            	
            	result.addStatement(new AssignmentStatement(
            			// lhs
            			tmp.lvalBitAt(tmp.size()-i-1),
            			// rhs
            			new BinaryOpExpression(new PrimitiveOperator(
            					PrimitiveOperator.AND_OP),
            					tlast, left.bitAt(left.size()-i-1))));
            	
            	result.addStatement(new AssignmentStatement(
            			// lhs
            			know.lvalBitAt(know.size()-i-1),
            			// rhs
            			new BinaryOpExpression(new PrimitiveOperator(
            					PrimitiveOperator.ORN_OP),
            					tlast, right.bitAt(right.size()-i-1))));
			}
		}
		
		int loff = (diff>0) ? -diff : 0;
		int roff = (diff<0) ? diff : 0;
		
		boolean[] OR_NEQ = { false, true, true, false, true, true, true, true };
		boolean[] LT = { false, true, false, false };
		
		LvalExpression lttmp;
		
		for (; i<tmp.size(); ++i) {
		  	Expression tlast = (i>0) ? tmp.bitAt(tmp.size()-i) 
        			: new BooleanConstant(false);
		  	Expression klast = (i>0) ? know.bitAt(know.size()-i) 
        			: new BooleanConstant(false);
		  	
		  	lttmp = Function.addTempLocalVar(lhs.getName() + "$L" + i, boolType);
			
		  	result.addStatement(new AssignmentStatement(
		  			lttmp.lvalBitAt(0),
		  			new BinaryOpExpression(
							new PrimitiveOperator(LT),
							left.bitAt(loff+left.size()-i-1), 
							right.bitAt(roff+right.size()-i-1))));
		  			
		  	result.addStatement(new AssignmentStatement(
        			// lhs
        			tmp.lvalBitAt(tmp.size()-i-1),
        			// rhs
        			new TrinaryOpExpression(new PrimitiveOperator(
        					PrimitiveOperator.MUX_OP),
        					klast, tlast, lttmp.bitAt(0))));
		  	
			result.addStatement(new AssignmentStatement(
        			// lhs
        			know.lvalBitAt(know.size()-i-1),
        			// rhs
        			new TrinaryOpExpression(new PrimitiveOperator(OR_NEQ),
        					klast, left.bitAt(loff+left.size()-i-1), 
        					right.bitAt(roff+right.size()-i-1))));
		}
		
		for (i = 0; i < lhs.size(); i++)
			result.addStatement(new AssignmentStatement(
			// lhs
			lhs.lvalBitAt(i), new UnaryOpExpression(new PrimitiveOperator(
					PrimitiveOperator.ID_OP),
					tmp.bitAt(0))));

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
