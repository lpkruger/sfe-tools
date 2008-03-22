package sfe.bdd.bdd;

import java.util.*;

import sfe.bdd.bdd.BDD.BaseNode;
import sfe.bdd.bdd.BDD.DNode;
import sfe.bdd.bdd.BDD.TNode;

public class BDDInference {
	static class GNode implements Comparable<GNode> {
		int var;
		int prio = -1;
		GNode(int n) {
			var = n;
		}
		
		public int compareTo(GNode g) {
			if (prio < g.prio) return -1;
			if (prio > g.prio) return 1;
			if (var < g.var) return -1;
			if (var > g.var) return 1;
			return 0;
		}
	}
	
	static void inferVarOrder(BDD bdd) {
		Map<Integer,GNode> map = new TreeMap<Integer,GNode>();
		Set<BDD.DNode> visited = new HashSet<BDD.DNode>();
		
		for (BaseNode node : bdd.root) {
			if (node instanceof DNode)
				tree_rec((DNode)node, visited, map, 0);
		}
	
		ArrayList<GNode> allvars = new ArrayList<GNode>(map.values());
		Collections.sort(allvars);
		
		bdd.varorder = new int[allvars.size()];
		for (int i=0; i<bdd.varorder.length; ++i) {
			bdd.varorder[i] = allvars.get(i).var;
		}
	}

	private static GNode getGNode(Map<Integer,GNode> map, int n) {
		GNode g = map.get(n);
		if (g == null) {
			g = new GNode(n);
			map.put(n, g);
		}
		return g;
		
	}
	static private void tree_rec(BDD.DNode node, Set<BDD.DNode> visited,
			Map<Integer,GNode> map, int depth) {
		
		GNode gcur = getGNode(map, node.var);
		
		if (visited.contains(node) && depth <= gcur.prio)
			return;
		
		visited.add(node);
		
		if (depth > gcur.prio)
			gcur.prio = depth;
		
		if (node.lo instanceof BDD.DNode)
			tree_rec((BDD.DNode)node.lo, visited, map, depth + 1);
		if (node.hi instanceof BDD.DNode)
			tree_rec((BDD.DNode)node.hi, visited, map, depth + 1);
	}
	
	public static void main(String[] args) throws Exception {
		BDD bdd = BDD.readFile(args[0]);
		System.out.println("BDD " + bdd.root.length);
		int[] vorder = bdd.varorder;
		for (int v : vorder) {
			System.out.print(" " + v);
		}
		System.out.println();
		
		bdd.varorder = null;
		inferVarOrder(bdd);
		
		System.out.println("BDD " + bdd.root.length);
		vorder = bdd.varorder;
		int maxVar = -1;
		for (int v : vorder) {
			System.out.print(" " + v);
			if (v > maxVar) {
				maxVar = v;
			}
		}
		System.out.println();
	}
	
}
