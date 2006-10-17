package sfe.shdl;

import java.io.*;
import java.math.BigInteger;
import java.util.*;

import sfe.util.VarDesc;

public class FmtFile {
	static class Obj {
		String name;
		int party;  // 0 for Alice, 1 for Bob
		int[] bits;
	}
	
	HashMap<String, Obj> mapping = new HashMap<String, Obj>();
	VarDesc vardesc = new VarDesc();
	
	public void mapBits(BigInteger n, TreeMap<Integer,Boolean> vals, String name) {
		Obj obj = mapping.get(name);
		for (int j=0; j<obj.bits.length; ++j) {
			int i = obj.bits[j];
            vals.put(i, n.testBit(j));
		}
	}
	
	public VarDesc getVarDesc() {
		return vardesc;
	}
	
	public BigInteger readBits(TreeMap<Integer,Boolean> vals, String name) {
		BigInteger n = BigInteger.ZERO;
		Obj obj = mapping.get(name);
		for (int j=0; j<obj.bits.length; ++j) {
			int i = obj.bits[j];
			if (vals.get(i))
				n = n.setBit(j);
		}
		return n;
	}
	
	public static FmtFile readFile(String file) {
		try {
			BufferedReader in = new BufferedReader(new FileReader(file));
			String line;
			
			FmtFile fmt = new FmtFile();
			
			while ((line = in.readLine()) != null) {
				String[] spl = line.split(" ");
				if (spl.length < 5)
					continue;
				if (!spl[2].equals("integer"))
					throw new RuntimeException("Unknown type " + spl[2]);
				Obj obj = new Obj();
				if (spl[0].equals("Alice"))
					obj.party = 0;
				else if (spl[0].equals("Bob"))
					obj.party = 1;
				else
					throw new RuntimeException("Unknown actor " + spl[0]);
				
				if (spl[3].charAt(0) != '"' || spl[3].charAt(spl[3].length()-1) != '"')
					throw new RuntimeException("Bad name " + spl[3]);
				
				obj.name = spl[3].substring(1, spl[3].length()-1);
				
				obj.bits = new int[spl.length - 6];
				
				for (int i=5; i<spl.length-1; ++i) {
					obj.bits[i-5] = Integer.parseInt(spl[i]);
					fmt.vardesc.who.put(obj.bits[i-5], obj.party==1 ? "B" : "A");
				}
				
				fmt.mapping.put(obj.name, obj);
			}
			
			return fmt;
		} catch (IOException ex) {
			ex.printStackTrace();
			return null;
		}
	}
	
	public static void main(String[] args) {
		readFile(args[0]);
		
	}
}
