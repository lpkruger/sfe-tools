package sfe.util;
import java.io.*;
import java.util.*;

import sfe.bdd.proto.*;

/**
 * Description of variables
 * Currently only contains ownership (Alice or Bob)
 * 
 * @author lpkruger
 * 
 */
public class VarDesc implements Serializable {
	public TreeMap<Integer, String> who = new TreeMap<Integer, String>();
	
	public static VarDesc readFile(String file) {
		VarDesc bdv = new VarDesc();
		try {
			BufferedReader in = new BufferedReader(new FileReader(file));
			String line;
			while ((line = in.readLine()) != null) {
				line = line.trim();
				if (line.length() == 0 || line.charAt(0)=='#')
					continue;
				String[] fld = line.split(" ");
				if (fld.length != 2)
					throw new RuntimeException("Bad line: " + line);
				int n = Integer.parseInt(fld[0]);
				bdv.who.put(n, fld[1]);
			}
			return bdv;
		} catch (IOException ex) {
			ex.printStackTrace();
			throw new RuntimeException("Error reading BDD Desc file: " + file);
		}
	}
	
	public VarDesc filter(String w) {
		VarDesc bdv = new VarDesc();
		for (Map.Entry<Integer,String> ent : who.entrySet()) {
			if (w.equals(ent.getValue())) {
				bdv.who.put(ent.getKey(), ent.getValue());
			}
		}
		return bdv;
	}
}
