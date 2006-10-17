package sfe.bdd.bdd;

import java.io.*;
import java.util.*;


public class BDDWriter {
	public static void write(PrintWriter out, BDD bdd) {
		out.println("BDD " + bdd.root.length);
		int[] vorder = bdd.varorder;
		for (int v : vorder) {
			out.print(" " + v);
		}
		out.println();
		
	    HashMap<Integer,BDD.BaseNode> map = new HashMap<Integer,BDD.BaseNode>();
	    HashMap<BDD.BaseNode,Integer> rmap = new HashMap<BDD.BaseNode,Integer>();
	    TreeSet<Integer> wq = new TreeSet<Integer>();
	    
	    int nodeCnt = 0;
	    
	    for (BDD.BaseNode cur : bdd.root) {
	    	map.put(nodeCnt, cur);
	    	rmap.put(cur, nodeCnt);
	    	wq.add(nodeCnt);
	    	
	    	nodeCnt ++;
	    }
	    
	    BDD.BaseNode cur;
	    
	    while(!wq.isEmpty()) {
	    	int curNum = wq.first();
	    	wq.remove(curNum);
	    	cur = map.get(curNum);
	    	
	    	if (cur instanceof BDD.TNode) {
	    		out.println(curNum + (((BDD.TNode)cur).val ? " T" : " F"));
	    		continue;
	    	}
	    	
	    	BDD.DNode curn = (BDD.DNode) cur;
	    	
	    	BDD.BaseNode hi = curn.hi;
	    	BDD.BaseNode lo = curn.lo;
	    	
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
	    	out.println(curNum + " " + curn.var + " " + hiNum + " " + loNum);
	    }
	    out.flush();
	}
	
	public static void printDot(BDD bdd, PrintStream out) {
        out.println("digraph G {");
        out.println("0 [shape=box, label=\"0\", style=filled, shape=box, height=0.3, width=0.3];");
        out.println("1 [shape=box, label=\"1\", style=filled, shape=box, height=0.3, width=0.3];");

        Set<BDD.BaseNode> visited = new HashSet<BDD.BaseNode>();
        
        
        HashMap<BDD.BaseNode, Integer> map = new HashMap<BDD.BaseNode, Integer>();
        map.put(BDD.T_FALSE, new Integer(0));
        map.put(BDD.T_TRUE, new Integer(1));
        int current = 1;
        for (BDD.BaseNode node : bdd.root) {
        	if (node instanceof BDD.DNode)
        		current = printdot_rec((BDD.DNode)node, out, current, visited, map);
        }
        for (BDD.BaseNode node : bdd.orphans) {
        	if (node instanceof BDD.DNode)
        		current = printdot_rec((BDD.DNode)node, out, current, visited, map);
        }
        
        out.println("}");
    }

    static int printdot_rec(BDD.DNode node, PrintStream out, int current, 
    		Set<BDD.BaseNode> visited, HashMap<BDD.BaseNode, Integer> map) {
        
        
        if (visited.contains(node))
        	return current;
       
        visited.add(node);
        
        Integer ri = ((Integer) map.get(node));
        if (ri == null) {
        	map.put(node, ri = new Integer(++current));
        }
   
        int r = ri.intValue();
        
        // TODO: support labelling of vars.
        out.println(r+" [label=\""+node.var+"\"];");

        BDD.BaseNode lo = node.lo;
        BDD.BaseNode hi = node.hi;
        
        Integer lon = ((Integer) map.get(lo));
        if (lon == null) {
            map.put(lo, lon = new Integer(++current));
        }
     
        Integer hin = ((Integer) map.get(hi));
        if (hin == null) {
            map.put(hi, hin = new Integer(++current));
        }
  

        out.println(r+" -> "+lon+" [style=dotted];");
        out.println(r+" -> "+hin+" [style=filled];");

        if (lo instanceof BDD.DNode)
        	current = printdot_rec((BDD.DNode)lo, out, current, visited, map);
        if (hi instanceof BDD.DNode)
        	current = printdot_rec((BDD.DNode)hi, out, current, visited, map);
        return current;
    }
    
    public static void main(String[] args) {
    	BDD bdd = BDD.readFile(args[0]);
	if (args.length>1 && args[1].equals("-f"))
    	    bdd.completeBDD(bdd, bdd.varorder);
    	BDDWriter.printDot(bdd, System.out);
    	
    }
}
