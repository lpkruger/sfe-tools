package sfe.sfdl;

import sfe.sfdl.SFDL.AddExpr;
import sfe.sfdl.SFDL.AndExpr;
import sfe.sfdl.SFDL.ArrayRef;
import sfe.sfdl.SFDL.AssignExpr;
import sfe.sfdl.SFDL.Block;
import sfe.sfdl.SFDL.DivExpr;
import sfe.sfdl.SFDL.EqExpr;
import sfe.sfdl.SFDL.Expr;
import sfe.sfdl.SFDL.ForExpr;
import sfe.sfdl.SFDL.GreaterThanExpr;
import sfe.sfdl.SFDL.GreaterThanOrEqExpr;
import sfe.sfdl.SFDL.IfExpr;
import sfe.sfdl.SFDL.IntConst;
import sfe.sfdl.SFDL.LArrayRef;
import sfe.sfdl.SFDL.LStructRef;
import sfe.sfdl.SFDL.LeftShiftExpr;
import sfe.sfdl.SFDL.LessThanExpr;
import sfe.sfdl.SFDL.LessThanOrEqExpr;
import sfe.sfdl.SFDL.MulExpr;
import sfe.sfdl.SFDL.NotEqExpr;
import sfe.sfdl.SFDL.OrExpr;
import sfe.sfdl.SFDL.RightShiftExpr;
import sfe.sfdl.SFDL.StructRef;
import sfe.sfdl.SFDL.SubExpr;
import sfe.sfdl.SFDL.VarRef;
import sfe.sfdl.SFDL.XorExpr;

public interface Compile {

	CompilerOutput compileIntConst(IntConst intConst);

	CompilerOutput compileVarRef(VarRef varRef);

	CompilerOutput compileStructRef(StructRef structRef);

	CompilerOutput compileAddExpr(AddExpr addExpr);

	CompilerOutput compileSubExpr(SubExpr subExpr);

	CompilerOutput compileXorExpr(XorExpr xorExpr);

	CompilerOutput compileAndExpr(AndExpr andExpr);

	CompilerOutput compileOrExpr(OrExpr orExpr);	

	CompilerOutput compileEqExpr(EqExpr eqExpr);

	CompilerOutput compilerNotEqExpr(NotEqExpr notEqExpr);

	CompilerOutput compileLessThanExpr(LessThanExpr lessThanExpr);

	CompilerOutput compileGreaterThanExpr(GreaterThanExpr greaterThanExpr);
	
	CompilerOutput compileLessThanOrEqExpr(LessThanOrEqExpr lessThanOrEqExpr);

	CompilerOutput compileGreaterThanOrEqExpr(GreaterThanOrEqExpr greaterThanOrEqExpr);

	CompilerOutput compileAssignExpr(AssignExpr assignExpr);

	CompilerOutput compileIfExpr(IfExpr ifExpr);

	CompilerOutput compileBlock(Block block);

	CompilerOutput compileMulExpr(MulExpr mulExpr);

	CompilerOutput compileDivExpr(DivExpr divExpr);

	CompilerOutput compileLeftShiftExpr(LeftShiftExpr leftShiftExpr);

	CompilerOutput compileRightShiftExpr(RightShiftExpr rightShiftExpr);

	CompilerOutput compileForExpr(ForExpr forExpr);

	CompilerOutput compileArrayRef(ArrayRef arrayRef);

	void compileAssignVarRef(VarRef varRef, CompilerOutput val, Expr ex);

	void compileAssignStructRef(LStructRef structRef,
			CompilerOutput val, Expr ex);

	void compileAssignArrayRef(LArrayRef arrayRef, CompilerOutput val, Expr ex);
}