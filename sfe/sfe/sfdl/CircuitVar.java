package sfe.sfdl;

import java.util.*;

import sfe.sfdl.SFDL.*;
import sfe.shdl.Circuit.*;
import sfe.util.ListMap;
import sun.reflect.generics.tree.FieldTypeSignature;

public abstract class CircuitVar {
	String name;
	//abstract void assign(GateBase[] cc);
	abstract List<Input> assignInputs(CircuitCompiler comp);
	abstract void assignGate(Gate gate);
	abstract void assign(CircuitCompiler comp, GateBase[] cc);
	abstract ArrayList<GateBase> getAllGates();
	abstract void setAsOutputs(CircuitCompiler comp);
	
	
	boolean isLeaf() { return false; }


	CircuitVar(String name) {
		this.name = name;
	}
	static class Leaf extends CircuitVar {
		GateBase[] cc;
		Leaf(IntType type, String name) {
			super(name);
			this.cc = new GateBase[type.bits];
		}
		boolean isLeaf() { return true; }

		List<Input> assignInputs(CircuitCompiler comp) {
			for (int i=0; i<cc.length; ++i) {
				int varNo = comp.newId();
				cc[i] = new Input(varNo, varNo);
				cc[i].setComment(name + "$" + i);
			}
			ArrayList<Input> inps = new ArrayList<Input>(cc.length);
			for (GateBase g : cc) {
				inps.add((Input) g);
			}
			return inps;
		}
		void setAsOutputs(CircuitCompiler comp) {
			for (int i=0; i<cc.length; ++i) {
				Output out = new Output(comp.newIdentityGate(cc[i]));
				out.setComment(name + "$" + i);
				cc[i] = out;
			}
			comp.formatMap.put(name, cc);
		}
		void assignGate(Gate g) {
			for (int i=0; i<cc.length; ++i) {
				cc[i] = g;
			}
		}

		void assign(CircuitCompiler comp, GateBase[] newcc) {
			if (comp.lvalStack.size() != 0) {
				throw new InternalCompilerError("lvalStack not empty, size="+comp.lvalStack.size());
			}
			if (cc.length != newcc.length) {
				throw new CompilerError("assigning to variable of wrong length");
			}


			if (comp.conditionBit != comp.TRUE_GATE) {
				for (int i=0; i<cc.length; ++i) {
					cc[i] = comp.newGate(comp.conditionBit, newcc[i], cc[i], 
							CircuitCompiler.TT_MUX());
				}
			} else {
				System.arraycopy(newcc, 0, cc, 0, cc.length);
			}

			//D("assignToVar " + lval.uniqueStr() + " := " + circ2str(cc));
			
		}
		
		ArrayList<GateBase> getAllGates() {
			return new ArrayList<GateBase>(Arrays.asList(cc));
		}
	}

	static class Struct extends CircuitVar {
		ListMap<String, CircuitVar> fields;
		Struct(String name, StructType type) {
			super(name);
			fields = new ListMap<String, CircuitVar>();
		}
		ArrayList<Input> assignInputs(CircuitCompiler comp) {
			ArrayList<Input> inps = new ArrayList<Input>();
			for (CircuitVar cv : fields.values()) {
				inps.addAll(cv.assignInputs(comp));
			}
			return inps;
		}
		ArrayList<GateBase> getAllGates() {
			ArrayList<GateBase> inps = new ArrayList<GateBase>();
			for (CircuitVar cv : fields.values()) {
				inps.addAll(cv.getAllGates());
			}
			return inps;
		}
		void assignGate(Gate g) {
			for (CircuitVar cv : fields.values()) {
				cv.assignGate(g);
			}
		}
		void assign(CircuitCompiler comp, GateBase[] newcc) {
			LStructRef ref = (LStructRef) comp.lvalStack.pop();
			fields.get(ref.field).assign(comp, newcc);
			comp.lvalStack.push(ref);
		}
		void setAsOutputs(CircuitCompiler comp) {
			for (CircuitVar cv : fields.values()) {
				cv.setAsOutputs(comp);
			}
		}
	}
	static class Array extends CircuitVar {
		CircuitVar[] els;
		Array(String name, ArrayType type) {
			super(name);
			els = new CircuitVar[type.len];
		}
		ArrayList<Input> assignInputs(CircuitCompiler comp) {
			ArrayList<Input> inps = new ArrayList<Input>();
			for (CircuitVar cv : els) {
				inps.addAll(cv.assignInputs(comp));
			}
			return inps;
		}
		ArrayList<GateBase> getAllGates() {
			ArrayList<GateBase> inps = new ArrayList<GateBase>();
			for (CircuitVar cv : els) {
				inps.addAll(cv.getAllGates());
			}
			return inps;
		}
		void assignGate(Gate g) {
			for (CircuitVar cv : els) {
				cv.assignGate(g);
			}
		}
		void assign(CircuitCompiler comp, GateBase[] newcc) {
			LArrayRef ref = (LArrayRef) comp.lvalStack.pop();

			if (false && ref.el instanceof SFDL.IntConst) {
				IntConst ind = (IntConst) ref.el;
				els[ind.number.intValue()].assign(comp, newcc);
			} else {
				//GateBase[] indcc = comp.compileExpr(ref.el);
				for (int i=0; i<els.length; ++i) {
					SFDL.EqExpr eqex = new SFDL.EqExpr(ref.el, new SFDL.IntConst(i));
					CircuitCompilerOutput out = comp.compileExpr(eqex);
					GateBase oldCondBit = comp.conditionBit;
					comp.conditionBit = comp.newGate(oldCondBit, out.cc[0], 
							CircuitCompiler.TT_AND());
					System.out.println("i="+i+" set " + comp.gateId + "...");
					els[i].assign(comp, newcc);
					comp.conditionBit = oldCondBit;
					
				}
			}
			
			comp.lvalStack.push(ref);
		}
		void setAsOutputs(CircuitCompiler comp) {
			for (CircuitVar cv : els) {
				cv.setAsOutputs(comp);
			}
		}
	}


	static CircuitVar createVars(Type type, String name) {
		if (type instanceof IntType) {
			return new Leaf((IntType) type, name);
		} else if (type instanceof StructType) {
			Struct cv = new Struct(name, (StructType) type);
			StructType stype = (StructType) type;
			for (int i=0; i<stype.fieldnames.length; ++i) {
				cv.fields.put(stype.fieldnames[i], 
						createVars(stype.fieldtypes[i], name + "." + stype.fieldnames[i]));
			}
			return cv;
		} else if (type instanceof ArrayType) {
			Array arr = new Array(name, (ArrayType) type);
			ArrayType atype = (ArrayType) type;
			for (int i=0; i<atype.len; ++i) {
				arr.els[i] = createVars(atype.basetype, name + "[" + i + "]");
			}
			return arr;
		} else {
			throw new InternalCompilerError("unexpected type " + type);
		}
	}
}
