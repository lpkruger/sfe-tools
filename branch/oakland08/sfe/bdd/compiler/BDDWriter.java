package sfe.bdd.compiler;

import net.sf.javabdd.*;
import java.io.*;
import java.util.*;

public class BDDWriter {
	public static void write(PrintWriter out, BDD bdd) {
		write(out, new BDD[] { bdd });
	}
	
	public static void write(PrintWriter out, BDD[] bddz) {
		out.print("BDD " + bddz.length);
		int[] vorder = bddz[0].getFactory().getVarOrder();
		for (int v : vorder) {
			out.print(" " + v);
		}
		out.println();
		
	    HashMap<Integer,BDD> map = new HashMap<Integer,BDD>();
	    HashMap<BDD,Integer> rmap = new HashMap<BDD,Integer>();
	    TreeSet<Integer> wq = new TreeSet<Integer>();
	    
	    int nodeCnt = 0;
	    
	    for (BDD bdd : bddz) {
	    	map.put(nodeCnt,bdd);
	    	rmap.put(bdd, nodeCnt);
	    	wq.add(nodeCnt);
	    	
	    	nodeCnt ++;
	    }
	    
	    BDD bdd;
	    
	    while(!wq.isEmpty()) {
	    	int curNum = wq.first();
	    	wq.remove(curNum);
	    	bdd = map.get(curNum);
	    	
	    	if (bdd.isOne()) {
	    		out.println(curNum + " T");
	    		continue;
	    	}
	    	if (bdd.isZero()) {
	    		out.println(curNum + " F");
	    		continue;
	    	}
	    	BDD hi = bdd.high();
	    	BDD lo = bdd.low();
	    	
	    	Integer hiNum = rmap.get(hi);
	    	if (hiNum == null) {
	    		hiNum = nodeCnt++;
	    		map.put(hiNum, hi);
	    		rmap.put(hi, hiNum);
	    		wq.add(hiNum);
	    	}
	    	Integer loNum = rmap.get(lo);
	    	if (loNum == null) {
	    		loNum = nodeCnt++;
	    		map.put(loNum, lo);
	    		rmap.put(lo, loNum);
	    		wq.add(loNum);
	    	}
	    	out.println(curNum + " " + bdd.var() + " " + hiNum + " " + loNum);
	    }
	    out.flush();
	}
}
