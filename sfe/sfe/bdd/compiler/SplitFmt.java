package sfe.bdd.compiler;

import java.io.*;
import java.util.*;

public class SplitFmt {
	public static void main(String[] args) throws Exception {
		BufferedReader in = new BufferedReader(new FileReader(args[0]));
		String line;
		Map<Integer, String> map = new TreeMap<Integer, String>();
		
		while ((line = in.readLine()) != null) {
			String[] spl = line.split(" ");
			if (spl.length < 5)
				continue;
			if (!spl[1].equals("input"))
				continue;
			String c;
			if (spl[0].equals("Alice"))
				c = "A";
			else if (spl[0].equals("Bob"))
				c = "B";
			else
				throw new RuntimeException("Unknown character " + spl[0]);
			
			
			for (int i=5; i<spl.length-1; ++i) {
				map.put(Integer.parseInt(spl[i]), c);
			}
		}
		
		for (Map.Entry<Integer, String> ent : map.entrySet()) {
			System.out.println(ent.getKey() + " " + ent.getValue());
		}
	}
}
