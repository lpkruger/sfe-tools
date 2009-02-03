package sfe.editdistsw;

import java.io.*;
import java.math.BigInteger;
import java.util.*;
import sfe.util.*;

public class SmithW {
	
	public static void main(String[] args) throws Exception {
		new SmithW().go(args);
	}
	
	void go(String[] args) throws Exception {
		populateDB();
		boolean[] seq = str2bits(args[0]);
	}
	

	int char2num(char c0) {
		return alphabetMap.get(c0);
	}
	
	boolean[] str2bits(String s) {
		int nbits = 5;  // bits per char
		boolean[] b = new boolean[nbits*s.length()];
		for (int i=0; i<s.length(); ++i) {
			int j = nbits*i;
			int x = alphabetMap.get(Character.toUpperCase(s.charAt(i)));
			//System.out.print(" "+x);
			for (int k=0; k<nbits; ++k) {
				b[j+k] = (0!=((1<<k)&x));
			}
		}
		/*
		System.out.println();
		for (int i=0; i<b.length; ++i) {
			System.out.print(" "+(b[i]?"1":"0"));
		}
		System.out.println();
		*/
		return b;
	}
	
	Map<String, int[]> sequenceDB = new TreeMap<String, int[]>();
	Map<Character,Integer> alphabetMap = new TreeMap<Character,Integer>();
	int[][] matrix;
	
	void populateDB() throws IOException {
		BufferedReader in = new BufferedReader(new FileReader("blosum.txt"));
		String line;
		line = in.readLine();
		{
			int j=0;
			for (int i=0; i<line.length(); ++i) {
				char ch = line.charAt(i);
				if (Character.isLetter(ch)) {
					alphabetMap.put(ch, j++);
				}
			}
		}
		matrix = new int[alphabetMap.size()][alphabetMap.size()];
		for (int i=0; i<alphabetMap.size(); ++i) {
			line = in.readLine().trim();
			if (line.length()!=1)
				throw new RuntimeException("bad matrix");
			int ii = alphabetMap.get(line.charAt(0));
			if (i != ii)
				throw new RuntimeException("i != ii");
			for (int j=0; j<alphabetMap.size(); ++j) {
				matrix[i][j] = Integer.parseInt(in.readLine());
			}
		}
		in.close();
		
		in = new BufferedReader(new FileReader("db.txt"));
		while ((line = in.readLine()) != null) {
			//System.out.println("line: "+line);
			String[] ss = line.split("\\s+");
			if (ss.length!=2) 
				continue;
			String name=ss[0];
			String seqstr=ss[1];
			int[] seq = new int[seqstr.length()];
			for (int i=0; i<seq.length; ++i) {
				seq[i] = alphabetMap.get(seqstr.charAt(i));
			}
			
			sequenceDB.put(name, seq);
		}
	}

}
