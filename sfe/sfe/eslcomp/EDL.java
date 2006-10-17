/*
 * Created on Apr 20, 2003
 *
 * To change the template for this generated file go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */

package sfe.eslcomp;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;

/**
 * @author lpkruger
 *
 * To change the template for this generated type comment go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
public class EDL {
	// to assign a constant for each type name
	private int typeconstcounter = 1000;

	/**
	 * all types, in order of definition, from typeconst -> type
	 * Map<Integer,EDL.Type>
	 */
	Map<Integer, Type> allTypes = new LinkedHashMap<Integer, Type>();

	/**
	 * all named types, in order of definition, from name -> type
	 * Map<String,EDL.Type>
	 */
	Map<String, Type> namedTypes = new LinkedHashMap<String, Type>();

	// basic types
	public final BasicType typeInt = new BasicType("int", "%d");
	public final BasicType typeLong = new BasicType("long", "%ld");
	public final BasicType typeShort = new BasicType("short", "%hd");
	public final BasicType typeChar = new BasicType("char", "%s");
	public final BasicType typeString = new BasicType("string", "%s");
	public final BasicType typePtr = new BasicType("ptr", "0x%08X");
	public final BasicType typeMap = new BasicType("map");
	public final UnsignedType typeUInt =
		new UnsignedType("u_int", "%u", typeInt);
	public final UnsignedType typeULong =
		new UnsignedType("u_long", "%lu", typeLong);
	public final UnsignedType typeUShort =
		new UnsignedType("u_short", "%hu", typeShort);
	public final UnsignedType typeUChar =
		new UnsignedType("u_char", "%c", typeChar);

	/**
	 * the name of this EDL
	 */
	String edlname;

	/**
	 * all events, in order of definition
	 */
	public ArrayList<Event> eventList = new ArrayList<Event>();

	int unionCounter = 1; // since unions are anonymous, distinguish them.

	EDL() {
		bind("int", typeInt);
		bind("long", typeLong);
		bind("short", typeShort);
		bind("char", typeChar);
		bind("string", typeString);
		bind("ptr", typePtr);
		bind("map", typeMap);
		bind("u_int", typeUInt);
		bind("u_short", typeUShort);
		bind("u_long", typeULong);
		bind("u_char", typeUChar);
	}

	public abstract class Type {
		public abstract String getName();
		PtrType ptrType;
		ArrayType arrayType;

		boolean emitted;
		void markEmitted() {
			emitted = true;
		};
		boolean isEmitted() {
			return emitted;
		};
		public PtrType getPtrType() {
			if (ptrType == null) {
				ptrType = new PtrType(this);
			}
			return ptrType;
		}
		public ArrayType getArrayType() {
			if (arrayType == null) {
				arrayType = new ArrayType(this);
			}
			return arrayType;
		}
		boolean isPointer() {
			return false;
		}
		boolean hasPointer() {
			return ptrType != null;
		}
		int typeConst;

		int getTypeConst() {
			return typeConst;
		}

		public String getTypeName() {
			return getName();
		}

		public EDL getEDL() {
			return EDL.this;
		}
		/**
		 * 
		 * @param ptr
		 */
		Type() {
			synchronized (EDL.this) {
				typeConst = (EDL.this.typeconstcounter++);
			}
			// Initialize some variables
			this.emitted = false;
			EDL.this.allTypes.put(IntPool.create(typeConst), this);
		}
	}

	public class BasicType extends Type {
		String name;
		String cformat = null;

		BasicType(String name) {
			this.name = name;
			markEmitted(); // basic types are handled specially
		}

		BasicType(String name, String cformat) {
			this.name = name;
			this.cformat = cformat;
			markEmitted(); // basic types are handled specially
		}

		public String getCformat() {
			return this.cformat;
		}

		public String toString() {
			return "[" + name + "," + typeConst + "]";
		}

		public String getName() {
			return name;
		}
	}

	/**
	 * Unsigned type
	 * @author Hao Wang
	 *
	 * To change the template for this generated type comment go to
	 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
	 */
	public class UnsignedType extends Type {
		BasicType realType = null;
		String name;
		String format;

		UnsignedType(String name, String format, BasicType t) {
			this.name = name;
			this.realType = t;
			this.format = format;
		}

		public String getCformat() {
			return this.format;
		}

		public String getName() {
			return this.name;
		}

		public Type getBaseType() {
			return this.realType;
		}

		public String toString() {
			return "[" + name + "," + typeConst + "]";
		}
	}

	public class UnionType extends Type {
		Type[] uu;
		String name;

		UnionType(Type[] uu, String name) {

			this.uu = uu;
			this.name = "struct " + name;
		}
		public String toString() {
			StringBuffer sb = new StringBuffer("[U " + typeConst + " " + uu[0]);
			for (int i = 1; i < uu.length; ++i) {
				sb.append(" | " + uu[i]);
			}
			return sb.append(" ]").toString();
		}

		public String getName() {
			return name;
		}

		public String getTypeName() {
			return this.name.substring(6).trim();
		}
	}

	public class StructType extends Type {
		Type[] ftypes;
		String[] fnames;

		String name;
		StructType(Type[] ftypes, String[] fnames, String name) {
			this.ftypes = ftypes;
			this.fnames = fnames;
			this.name = "struct " + name;
		}

		public String toString() {
			StringBuffer sb =
				new StringBuffer(
					"[S " + typeConst + " " + ftypes[0] + " " + fnames[0]);
			for (int i = 1; i < ftypes.length; ++i) {
				sb.append("; " + ftypes[i] + " " + fnames[i]);
			}
			return sb.append(" ]").toString();
		}

		public Type[] getTypes() {
			return ftypes;
		}

		public String getName() {
			return name;
		}

		public String getTypeName() {
			return this.name.substring(6).trim();
		}
	}

	public class SubType extends Type {
		String name;
		Type pp;

		SubType(String name, Type pp) {
			this.name = name;
			this.pp = pp;
		}
		public String toString() {
			return "[" + name + " " + typeConst + ": " + pp + " ]";
		}
		public String getName() {
			return name;
		}
		public Type getRealType() {
			Type type = pp;
			while (type instanceof SubType) {
				type = ((SubType) type).pp;
			}
			return type;
		}
	}

	public class PtrType extends Type {
		Type ptrto;
		PtrType(Type ptrto) {
			this.ptrto = ptrto;
		}

		public String getDepth() {
			return getDepth("*", "*");
		}

		public String getDepth(String first, String pre) {
			String depth = first;
			EDL.Type t = ptrto;

			while (t instanceof EDL.PtrType) {
				depth = pre + depth;
				t = ((EDL.PtrType) t).getBaseType();
			}
			return depth;
		}

		public String getName() {

			return getBaseType().getName() + getDepth() + " ";
		}

		/**
		 * This method returns the top-most parent type of current
		 * pointer type. i.e. it will travers to the top level
		 * @return
		 */
		public Type getBaseType() {
			EDL.Type t = ptrto;

			while (t instanceof EDL.PtrType) {
				t = ((EDL.PtrType) t).getPointToType();
			}
			return t;

		}
		/**
		 * This method returns the direct parent type of current
		 * pointer type.
		 * @return
		 */
		public Type getPointToType() {
			return this.ptrto;
		}

		public String toString() {
			return getBaseType().getName() + getDepth() + " " + typeConst;
		}

		public String getTypeName() {
			return getBaseType().getTypeName();
		}
	}

	public class ArrayType extends Type {
		Type eltype;
		Map<Integer, DefinateArrayType> deftypes;
		Map<Integer, IndefinateArrayType> indeftypes = null;

		ArrayType(Type eltype) {
			this.eltype = eltype;
		}

		public String getName() {
			// We can't use this since when emitting the header file
			// we do not necessarily know about the size of the array yet
			//return eltype.getName() + "[]";
			return eltype.getName();
		}

		public Type getElementType() {
			return eltype;
		}

		public DefinateArrayType getDefinateType(int len) {
			if (deftypes == null) {
				deftypes = new TreeMap<Integer, DefinateArrayType>();
			}
			Integer i = IntPool.create(len);
			DefinateArrayType dat = deftypes.get(i);
			if (dat != null) {
				return dat;
			}
			dat = new DefinateArrayType(eltype, len);
			deftypes.put(i, dat);
			return dat;
		}

		public DefinateArrayType findDefType(int typeConst) {
			DefinateArrayType def = null;

			if (deftypes != null) {
				Iterator<DefinateArrayType> it = deftypes.values().iterator();

				while (it.hasNext()) {
					DefinateArrayType d = it.next();
					if (d.typeConst == typeConst) {
						def = d;
						break;
					}
				}
			}

			return def;
		}

		public IndefinateArrayType findIndefTypes(int typeConst) {
			if (indeftypes != null) {
				Integer key = new Integer(typeConst);

				return indeftypes.get(key);
			} else {
				return null;
			}
		}

		protected void addIndefinateArrayType(
			int typeConst,
			IndefinateArrayType t) {
			if (indeftypes == null) {
				indeftypes = new TreeMap<Integer, IndefinateArrayType>();
			}
			Integer key = IntPool.create(typeConst);

			indeftypes.put(key, t);
		}

		public String toString() {
			return eltype.getName() + "[]" + " " + typeConst;
		}
	}

	public class DefinateArrayType extends ArrayType {
		int len;
		DefinateArrayType(Type eltype, int len) {
			super(eltype);
			this.len = len;
		}

		public int getArraySize() {
			return len;
		}
		/*
		public String getName() {
			return eltype.getName() + "[" + len + "]";
		}
		*/
	}

	public class IndefinateArrayType extends ArrayType {
		// the length depends on context.  This type should not be
		// cached and shared, but a new instance should be created each
		// time it is needed
		String len;
		EventArg lendef; // pointer to expression that defines length
		// TODO: allow more general expression 

		Tokenizer.Token lentok; // for error report
		IndefinateArrayType(Type eltype, String len, Tokenizer.Token lentok) {
			super(eltype);
			this.len = len;
			this.lentok = lentok;

			// Add to the list
			eltype.arrayType.addIndefinateArrayType(this.typeConst, this);
		}

		void setLengthDef(EventArg lendef) {
			// It could be a pointer to the length type
			Type t = lendef.type;
			if (t instanceof PtrType) {
				t = ((PtrType) t).getPointToType();
			}

			if (isSubtype(typeInt, t)
				|| isSubtype(typeShort, t)
				|| isSubtype(typeLong, t)) {
				this.lendef = lendef;
			} else {
				// DO Something
			}
		}
	}
	public static class EventArg {
		public Type type;
		public String name;
		public boolean in;
		public boolean out;
		EventArg(Type type, String name, boolean in, boolean out) {
			this.type = type;
			this.name = name;
			this.in = in;
			this.out = out;
		}
	}

	public static class Event {
		public String name;
		public EventArg[] args;
		public Type rettype;
		Event(String name, EventArg[] args, Type rettype) {
			this.name = name;
			this.args = args;
			this.rettype = rettype;
		}

		public String toString() {
			StringBuffer sb = new StringBuffer("event " + name + "(");
			String comma = "";
			for (int i = 0; i < args.length; ++i) {
				sb.append(comma + args[i].type + " " + args[i].name);
				comma = ", ";
			}
			sb.append(") = " + rettype);
			return sb.toString();
		}
	}

	static boolean isSubtype(Type type, Type subtype) {
		Type t = subtype;
		while (t instanceof SubType) {
			if (t == type)
				return true;
			t = ((SubType) t).pp;
		}
		return t == type;
	}

	Type[] getSubTypes(Type base) {
		Iterator<Type> it = namedTypes.values().iterator();
		ArrayList<Type> result = new ArrayList<Type>();
		while (it.hasNext()) {
			Type t = it.next();
			if (isSubtype(base, t)) {
				result.add(t);
			}
		}
		return (Type[]) result.toArray(new Type[0]);
	}

	String getName() {
		return edlname;
	}

	void setName(String name) {
		this.edlname = name;
	}

	/**
	 * Return the Type object corresponding to a typeID (i.e. typeConst)
	 * @param typeID the unique typeConst ID
	 * @return
	 */
	public Type getType(int typeID) {
		Iterator<Type> it = namedTypes.values().iterator();
		Type t = null;

		while (it.hasNext()) {

			Type p = it.next();

			if (p.typeConst == typeID) {
				t = p;
				break;
			}

			// see if it has a pointer type
			if ((p.ptrType != null) && (p.ptrType.typeConst == typeID)) {
				t = p.ptrType;
				break;
			}

			/* For DEBUG purpose 
			if (p.arrayType != null) {
				System.out.println(p.arrayType.toString());
				
				if (p.arrayType.deftypes != null) {
					System.out.println(p.arrayType.deftypes.toString());
				}
				if (p.arrayType.indeftypes != null) {
					System.out.println(p.arrayType.indeftypes.toString());
				}
			}
			*/

			// Or, array type
			if (p.arrayType != null) {
				if (p.arrayType.typeConst == typeID) {
					t = p.arrayType;
					break;
				}

				if ((t = p.arrayType.findDefType(typeID)) != null) {
					break;
				}

				if ((t = p.arrayType.findIndefTypes(typeID)) != null) {
					break;
				}
			}
		}
		if (t == null) {
			//dump();
		}
		return t;
	}

	public Type getType(String name) {
		return namedTypes.get(name);
	}

	boolean isAvailable(String name) {
		return namedTypes.get(name) == null;
	}

	void bind(String name, Type type) {
		namedTypes.put(name, type);
	}

	void event(Event evt) {
		eventList.add(evt);
	}

	void resetEmitted() {
		Iterator<Type> it = namedTypes.values().iterator();

		while (it.hasNext()) {
			Type t = it.next();
			t.emitted = false;
		}
	}

	/**
	 * This method returns all types defined in this edl.
	 * @param includeBasic if true, then the return set will contain basic types
	 * 					   if false, then the return set will only contain complex types
	 * @return a set of types defined in the EDL
	 */
	public String[] getAllType(boolean includeBasic) {
		ArrayList<String> list = new ArrayList<String>();

		Iterator it = namedTypes.entrySet().iterator();
		
		while (it.hasNext()) {
			Map.Entry ent = (Map.Entry) it.next();
			EDL.Type t = (EDL.Type) ent.getValue();
			if (includeBasic || !(t instanceof EDL.BasicType)) {
				String name = ent.getKey().toString();
				list.add(name);
			}
		}
		return (String[]) list.toArray(new String[0]);
	}

	/**
	 * Return an array of all event names
	 * @return
	 */
	public String[] getAllEvent() {
		Iterator<Event> it = eventList.iterator();
		ArrayList<String> list = new ArrayList<String>();
		while (it.hasNext()) {
			list.add(it.next().name);
		}
		return (String[]) list.toArray(new String[0]); 
	}
	
	/*
	void dump() {
		Iterator<Event> it = namedTypes.entrySet().iterator();
		while (it.hasNext()) {
			Map.Entry ent = (Map.Entry) it.next();
			System.out.println("bind " + ent.getKey() + " = " + ent.getValue());
		}
		it = eventList.iterator();
		while (it.hasNext()) {
			System.out.println(it.next());
		}
	} // TODO: match param types - only if overloading is allowed
	*/
	
	public Event getEvent(String name) {
		Iterator<Event> it = eventList.iterator();
		while (it.hasNext()) {
			Event ev = null; // it.next();
			if (ev.name.equals(name)) {
				return ev;
			}
		}
		return null;
	}
}
