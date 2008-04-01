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
	TreeMap<Integer, Integer> outputmap = new TreeMap<Integer, Integer>();
	
	public void mapBits(int n, TreeMap<Integer,Boolean> vals, String name) {
		mapBits(BigInteger.valueOf(n), vals, name);
	}
	
	public void mapBits(BigInteger n, TreeMap<Integer,Boolean> vals, String name) {
		//System.out.println("set bits: " + name + " = " + n);
		Obj obj = mapping.get(name);
		for (int j=0; j<obj.bits.length; ++j) {
			int i = obj.bits[j];
			//System.out.println("set bit " + j + " of " + name + " (" + i + ") = " + n.testBit(j));
            vals.put(i, n.testBit(j));
		}
	}
	
	public void mapBits(long nn, TreeMap<Integer,Boolean> vals, String name) {
		BigInteger n = BigInteger.valueOf(nn);
		mapBits(n, vals, name);
	}
	
	public void mapBits(boolean[] n, TreeMap<Integer,Boolean> vals, String name) {
		Obj obj = mapping.get(name);
		for (int j=0; j<obj.bits.length; ++j) {
			int i = obj.bits[j];
			//System.out.println("set bit " + j + " of " + name + " (" + i + ") = " + n.testBit(j));
            vals.put(i, n[j]);
		}
	}
	
	// BUG: outputmap is wrong if format file is not monotonic
	
	public BigInteger readBits(boolean[] vals, String name) {
		//System.out.print("get bits: " + name);
		Obj obj = mapping.get(name);
		BigInteger zz = BigInteger.ZERO;
		for (int j=0; j<obj.bits.length; ++j) {
			int i = obj.bits[j];
			int ri = outputmap.get(i);
			
			//System.out.print(vals[ri] ? "1" : "0");
			if (vals[ri]) {
				zz = zz.setBit(j);
			}
		}
		//System.out.println(" = " + zz);
		return zz;

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
	

	public VarDesc getVarDesc() {
		return vardesc;
	}
	
	public static FmtFile readFile(String file) {
		try {
			BufferedReader in = new BufferedReader(new FileReader(file));
			String line;
			
			FmtFile fmt = new FmtFile();
			int outputNum = 0;
			
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
	
					if (spl[1].equals("input")) {
						fmt.vardesc.who.put(obj.bits[i-5], obj.party==1 ? "B" : "A");
						//System.out.println("inp " + obj.bits[i-5] + " : " + (obj.party==1 ? "B" : "A"));
					}
					
					if (spl[1].equals("output")) {
						fmt.outputmap.put(obj.bits[i-5], outputNum++);
					}
				}
				
				fmt.mapping.put(obj.name, obj);
			}
			
			outputNum = 0;
			for (Integer i : fmt.outputmap.keySet()) {
				fmt.outputmap.put(i, outputNum++);
			}
			
			in.close();
			
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
