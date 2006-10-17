
/*
 * Created on May 21, 2003
 *
 * To change the template for this generated file go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */

package sfe.eslcomp;

import java.io.PrintStream;
import java.util.Iterator;
import java.util.Map;

/**
 * @author lpkruger
 *
 * To change the template for this generated type comment go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
public class EDLCHeader {
	EDL edl;
	PrintStream ps;

	EDLCHeader(EDL edl, PrintStream ps) {
		this.edl = edl;
		this.ps = ps;
	}

	void emitHeader() {

		ps.println("/**");
		ps.println(" * Interface for EDL \"" + edl.edlname + "\"");
		ps.println(" * this file is automatically generated, do not edit");
		ps.println(" */");
		ps.println();
		String edlh = edl.edlname.toUpperCase();
		int slash = edlh.lastIndexOf('\\');
		if (slash > 0) {
			edlh = edlh.substring(slash + 1);
		}
		slash = edlh.lastIndexOf('/');
		if (slash > 0) {
			edlh = edlh.substring(slash + 1);
		}
		edlh = edlh.replace('.', '_');
		edlh += "_H";

		ps.println("#ifndef " + edlh);
		ps.println("#define " + edlh);
		ps.println();
	}

	void emitFooter() {
		ps.println("#endif");
	}

	void emitBoilerplate() {
		ps.println("#define EDL_ACCEPT 0");
		ps.println("#define EDL_DENY -1");
		ps.println();

		ps.println("typedef char * string;");
		ps.println("typedef void * ptr;");
		ps.println("/* typedef map hashtable thingy */");
		ps.println();
	}

	void emit() {
		emitHeader();
		emitBoilerplate();
		Iterator it = edl.namedTypes.entrySet().iterator();

		while (it.hasNext()) {
			Map.Entry ent = (Map.Entry) it.next();
			String name = (String) ent.getKey();
			EDL.Type type = (EDL.Type) ent.getValue();
			if (!type.isEmitted()) {
				emitType(type);
				type.markEmitted();
			}
			if (!name.equals(type.getName())) {
				ps.println("typedef " + type.getName() + " " + name + ";");
				// very special case, replace struct union name with 1st typedef
				if (type instanceof EDL.UnionType
					&& type.getName().startsWith("struct ")) {
					((EDL.UnionType) type).name = name;

				}
			}
		}

		ps.println();

		it = edl.namedTypes.entrySet().iterator();
		while (it.hasNext()) {
			Map.Entry ent = (Map.Entry) it.next();
			String name = (String) ent.getKey();
			EDL.Type type = (EDL.Type) ent.getValue();
			ps.println(
				"#define TYPE_" + name + "_CONST " + type.getTypeConst());
		}
		ps.println();

		it = edl.eventList.iterator();
		while (it.hasNext()) {
			EDL.Event ev = (EDL.Event) it.next();
			emitEvent(ev);
			ps.println();
		}

		emitFooter();
	}

	void emitEvent(EDL.Event ev) {
		StringBuffer sb =
			new StringBuffer(
				"typedef int __stdcall eventcall_" + ev.name + "_type(");
		String comma = "";
		
		for (int i = 0; i < ev.args.length; ++i) {
			if (!ev.args[i].type.isEmitted()) {
				emitType(ev.args[i].type);
				ev.args[i].type.markEmitted();
			}
			sb.append(comma + "int " + ev.args[i].name + "_type, ");
			if (!ev.args[i].out)
				sb.append("const ");
			sb.append(ev.args[i].type.getName() + " " + ev.args[i].name);
			comma = ", ";
		}
	
		ps.print(sb);
		//	So that the C compiler won't generate the stupid
		// warning message about prototype not being provided
		if (ev.args.length == 0) {
			ps.println("void);");
		} else {
			ps.println(");");
		}
		sb.append(");");
		
		if (!ev.rettype.isEmitted()) {
			emitType(ev.rettype);
			ev.rettype.markEmitted();
		}

		sb.insert(
			sb.length() - 2,
			comma + "int nmatch, " + ev.rettype.getName() + " ret");
		sb.replace(27, 31, "ret");
		sb.replace(8, 11, "void");
		ps.println(sb);

		ps.println();
		ps.println("#ifdef EDL_IMPL");
		ps.println("eventcall_" + ev.name + "_type eventcall_" + ev.name + ";");
		ps.println("eventret_" + ev.name + "_type eventret_" + ev.name + ";");
		ps.println("#else");
		ps.println(
			"extern eventcall_"
				+ ev.name
				+ "_type *eventcall_"
				+ ev.name
				+ ";");
		ps.println(
			"extern eventret_" + ev.name + "_type *eventret_" + ev.name + ";");
		ps.println("#endif");
		ps.println();
		//ps.println(
		//	"void eventret_" + ev.name + "(" + ev.rettype.getName() + " ret);");
	}
	void emitType(EDL.Type type) {

		if (type instanceof EDL.BasicType)
			return; //handled by boilerplate
		else if (type instanceof EDL.SubType) {
			EDL.SubType t = (EDL.SubType) type;
			//				TODO: replace pp with unification
			if (!t.pp.isEmitted()) {
				emitType(t.pp);
				t.pp.markEmitted();
			}
			ps.println("typedef " + t.pp.getName() + " " + t.name + ";");
		} else if (type instanceof EDL.PtrType) {
			EDL.PtrType t = (EDL.PtrType) type;
			if (!t.ptrto.isEmitted()) {
				emitType(t.ptrto);
				t.ptrto.markEmitted();
			}
			// nothing to print
		} else if (type instanceof EDL.ArrayType) {
			EDL.ArrayType t = (EDL.ArrayType) type;
			if (!t.eltype.isEmitted()) {
				emitType(t.eltype);
				t.eltype.markEmitted();
			}
			// nothing to print
		} else if (type instanceof EDL.UnionType) {
			emitUnion((EDL.UnionType) type);
		} else if (type instanceof EDL.StructType) {
			emitStruct((EDL.StructType) type);
		} else if (type instanceof EDL.UnsignedType) {
			EDL.UnsignedType ut = (EDL.UnsignedType) type;
			ps.println("typedef unsigned "+ut.realType.getName()+" "+ut.getName()+";"); 
		}
		else {
			throw new RuntimeException("Can't emit " + type);
		}

	}
	void emitUnion(EDL.UnionType u) {
		ps.println(u.getName() + " {");
		ps.println("  int typeId;");
		ps.println("  union { ");
		for (int i = 0; i < u.uu.length; ++i) {
			ps.println("    " + u.uu[i].getName() + " " + "u" + (i + 1) + "; ");
		}
		ps.println("  } val;");
		ps.println("};");
	}

	void emitStruct(EDL.StructType s) {
		ps.println(s.getName() + " {");
		for (int i = 0; i < s.ftypes.length; ++i) {
			if (s.ftypes[i] instanceof EDL.DefinateArrayType) {
				// we need to do something special
				int len = ((EDL.DefinateArrayType) s.ftypes[i]).getArraySize();
				ps.println(
					"    " + s.ftypes[i].getName() + " " + s.fnames[i] +"["+len+"]; ");
			}
			else if (s.ftypes[i] instanceof EDL.IndefinateArrayType) {
				ps.println(
					"    " + s.ftypes[i].getName() + " ** " +  s.fnames[i] +"; ");
			}
			else {
				ps.println(
					"    " + s.ftypes[i].getName() + " " + s.fnames[i] + "; ");
			}
		}
		ps.println("};");

	}
}