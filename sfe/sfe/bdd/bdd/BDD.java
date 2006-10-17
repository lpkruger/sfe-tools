package sfe.bdd.bdd;
import java.io.*;
import java.util.*;

/**
 * Canonical BDD representation
 * 
 * @author lpkruger
 *
 */
public class BDD {
	public static class BaseNode {
		public String label;
	}
	public static class DNode extends BaseNode {
		public String who;
		public int var;
		public BaseNode hi;
		public BaseNode lo;
	}
	public static class TNode extends BaseNode {
		public boolean val;
		TNode(boolean val) {
			this.val = val;
		}
	}
	
	public static final TNode T_TRUE = new TNode(true);
	public static final TNode T_FALSE = new TNode(false);
	
	public BaseNode[] root;
	public int levels;
	
	public int[] varorder;
	
	// partial evaluation can orphan some nodes that must be tracked
	public Set<BaseNode> orphans = new HashSet<BaseNode>();
	
	/**
	 * Count the number of nodes in this BDD
	 */
	static public int countNodesAndOrphans(BDD bdd) {
		LinkedList<DNode> wq = new LinkedList<DNode>();
		BDD.DNode cur;
		HashSet<BaseNode> counted = new HashSet<BaseNode>();
		
		for (BaseNode curn : bdd.orphans) {
			counted.add(curn);
			if (!(curn instanceof DNode))
				continue;
				
			cur = (DNode) curn;
			wq.add(cur);
		}
		for (int i=0; i<bdd.root.length; ++i) {
			counted.add(bdd.root[i]);
			if (!(bdd.root[i] instanceof DNode))
				continue;
				
			cur = (DNode) bdd.root[i];
			wq.add(cur);
		}
		
		while (!wq.isEmpty()) {
			cur = wq.removeFirst();
			
			if (!counted.contains(cur.hi)) {
				counted.add(cur.hi);
				if (cur.hi instanceof DNode)
					wq.add((DNode)cur.hi);
			}
			
			if (!counted.contains(cur.lo)) {
				counted.add(cur.lo);
				if (cur.lo instanceof DNode)
					wq.add((DNode)cur.lo);
			}
		}
		
		return counted.size();
	}
	
	/**
	 * Count the number of nodes in this BDD
	 */
	static public int countNodes(BDD bdd) {
		LinkedList<DNode> wq = new LinkedList<DNode>();
		BDD.DNode cur;
		HashSet<BaseNode> counted = new HashSet<BaseNode>();
		
		for (int i=0; i<bdd.root.length; ++i) {
			counted.add(bdd.root[i]);
			if (!(bdd.root[i] instanceof DNode))
				continue;
				
			cur = (DNode) bdd.root[i];
			wq.add(cur);
		}
		
		while (!wq.isEmpty()) {
			cur = wq.removeFirst();
			
			if (!counted.contains(cur.hi)) {
				counted.add(cur.hi);
				if (cur.hi instanceof DNode)
					wq.add((DNode)cur.hi);
			}
			
			if (!counted.contains(cur.lo)) {
				counted.add(cur.lo);
				if (cur.lo instanceof DNode)
					wq.add((DNode)cur.lo);
			}
		}
		
		return counted.size();
	}
	
	
	static public void partialEval(BDD bdd, TreeMap<Integer, Boolean> vals) {
		LinkedList<BDD.DNode> wq = new LinkedList<BDD.DNode>();
		Set<DNode> dummies = new HashSet<DNode>();
		Set<DNode> visited = new HashSet<DNode>();
		BDD.DNode cur;
		
		
		for (int i=0; i<bdd.root.length; ++i) {
			if (bdd.root[i] instanceof DNode)
				wq.add((DNode)bdd.root[i]);
		}
		
		//int CCCNT = 0;
		while (!wq.isEmpty()) {
			cur = wq.removeFirst();
		    
			if (cur.var != -99) {
				if (visited.contains(cur))
					continue;
				visited.add(cur);
			}
			
			/*
			System.out.print(".");
			if (++CCCNT % 1000 == 0) {
				System.out.println();
				System.out.println("tot: " + CCCNT + "  wq: " + wq.size());
			}
			*/
			
			
			if (vals.get(cur.var) != null) {
				BaseNode orphan = vals.get(cur.var) ? cur.lo : cur.hi;
				if (orphan instanceof DNode && cur.lo != cur.hi) {
					if (vals.get(((DNode)orphan).var) == null) {
						// it's Bob's, so we need to root it.
						//System.out.println("Collect cur orphan: " + orphan);
						bdd.orphans.add(orphan);
						wq.add((DNode) orphan);
					} else {
						// it's alice, we need a fake parent for further processing
						//System.out.println("Dummy for cur orphan: " + orphan);
						if (!dummies.contains(orphan)) {
							BDD.DNode dummy = new BDD.DNode();
							dummy.var = -99;
							dummy.label = "dummy" + orphan;
							dummy.hi = orphan;
							wq.add(dummy);
							dummies.add((DNode)orphan);
						}
					}
					if (vals.get(cur.var))
						cur.lo = null;
					else
						cur.hi = null;
					
				}
			}
			
			while (cur.hi instanceof DNode && vals.get(((DNode)cur.hi).var) != null) {
				BDD.DNode hi = (DNode) cur.hi;
				BaseNode orphan = vals.get(hi.var) ? hi.lo : hi.hi;
				if (orphan instanceof DNode && hi.lo != hi.hi) {
					if (vals.get(((DNode)orphan).var) == null) {
						// it's Bob's, so we need to root it.
						//System.out.println("Collect hi orphan: " + orphan);
						bdd.orphans.add(orphan);
						wq.add((DNode) orphan);
					} else {
						// it's alice, we need a fake parent for further processing
						//System.out.println("Dummy for hi orphan: " + orphan);
						if (!dummies.contains(orphan)) {
							BDD.DNode dummy = new BDD.DNode();
							dummy.var = -99;
							dummy.label = "dummy" + orphan;
							dummy.hi = orphan;
							wq.add(dummy);
							dummies.add((DNode)orphan);
						}
					}
				}
				cur.hi = vals.get(hi.var) ? hi.hi : hi.lo;
			}
			if (cur.hi instanceof DNode) {
				wq.add((DNode)cur.hi);
			}
			
			if (cur.var == -99) {
				bdd.orphans.add(cur.hi);
				//System.out.println("Dummy collect orphan: " + cur.hi);
			}
		
			while (cur.lo instanceof DNode && vals.get(((DNode)cur.lo).var) != null) {
				BDD.DNode lo = (DNode) cur.lo;
				BaseNode orphan = vals.get(lo.var) ? lo.lo : lo.hi;
				if (orphan instanceof DNode && lo.lo != lo.hi) {
					if (vals.get(((DNode)orphan).var) == null) {
						// it's Bob's, so we need to root it.
						//System.out.println("Collect lo orphan: " + orphan);
						bdd.orphans.add(orphan);
						wq.add((DNode) orphan);
					} else {
						// it's alice, we need a fake parent for further processing
						//System.out.println("Dummy for lo orphan: " + orphan);
						if (!dummies.contains(orphan)) {
							BDD.DNode dummy = new BDD.DNode();
							dummy.var = -99;
							dummy.label = "dummy" + orphan;
							dummy.hi = orphan;
							wq.add(dummy);
							dummies.add((DNode)orphan);
						}
					}
					
				}
				cur.lo = vals.get(lo.var) ? lo.hi : lo.lo;
			}
			if (cur.lo instanceof DNode) {
				wq.add((DNode)cur.lo);
			}
			
			
			//seen.add(cur);
		}
		
		for (int i=0; i<bdd.root.length; ++i) {
			cur = (DNode) bdd.root[i];
			while (vals.get(cur.var) != null) {
				
				bdd.root[i] = vals.get(cur.var) ? cur.hi : cur.lo;
				// bdd.orphans.add(vals.get(cur.var) ? cur.lo : cur.hi);
				if (!(bdd.root[i] instanceof DNode))
					break;
				cur = (DNode) bdd.root[i];
			}
		}
	}

	static class NLtrio {
		BaseNode dest;
		int level;
		Set<Integer> contains;
		
		NLtrio(BaseNode dest, int level, Set<Integer> contains) {
			this.dest = dest;
			this.level = level;
			this.contains = contains;
		}
		
		public boolean equals(Object other) {
			NLtrio o = (NLtrio) other;
			if (contains==null && o.contains!=null ||
					contains!=null && o.contains==null ||
					(contains!=null && o.contains!=null && 
							!contains.equals(o.contains)))
				return false;
			return (dest == o.dest) && (level == o.level);
		}
		
		public int hashCode() {
			return dest.hashCode() + level;
		}
	}

	// contains should not include src
	static BaseNode getDummyChain(BaseNode dest, int src, int[] order, 
			Map<NLtrio, DNode> dests, TreeSet<Integer> contains) {
		
		int n;
		for (n=0; order[n] != src; ++n)
			;
		
		/*
		if (dest instanceof DNode)
			System.out.println("get ladder from " + order[n] + " to "
					+ ((DNode)dest).var);
		else
			System.out.println("get ladder from " + order[n] + " to "
					+ ((TNode)dest).val);
		*/
		
		DNode q = null;
		DNode p = null;
		BaseNode ret = null;
		for(; (dest instanceof DNode && order[n] != ((DNode)dest).var) ||
		(dest instanceof TNode && n<order.length); ++n) {
			if (contains != null && !contains.contains(order[n]))
				continue;
			if (contains != null) {
				contains = (TreeSet<Integer>) contains.clone();
				contains.remove(order[n]);
			}
			q = p;
			p = dests.get(new NLtrio(dest, order[n], contains));
		
			if (p != null) {
				if (ret == null)
					ret = p;
				if (q != null)
					q.hi = q.lo = p;
				return ret;
			} else {
				p = new DNode();
				p.var = order[n];
				p.label = dest + " - " + order[n];
				if (ret == null)
					ret = p;
				if (q != null)
					q.hi = q.lo = p;
				dests.put(new NLtrio(dest, order[n], contains), p);
			}
		}
		
		if (p != null)
			p.hi = p.lo = dest;
		if (ret == null)
			ret = dest;
		return ret;
	}

    // create dummy nodes to avoid info leak
	// creates less nodes than other algorithm
	static public void completeBDD(BDD bdd, int[] order) {
		Map<DNode, TreeSet<Integer>> map = new HashMap<DNode, TreeSet<Integer>>();
		
		for (int i=0; i<bdd.root.length; ++i) {
			BaseNode curn = bdd.root[i];
			if (curn instanceof DNode)
				complete_rec((DNode) curn, map, order);
		}
		Map<NLtrio, DNode> dests = new HashMap<NLtrio, DNode>();
		Set<DNode> visited = new HashSet<DNode>();
		for (int i=0; i<bdd.root.length; ++i) {
			BaseNode curn = bdd.root[i];
			if (curn instanceof DNode)
				complete_rec2((DNode) curn, map.get(curn), 
						visited, order, dests);
		}
		
	}
	
	static Set<Integer> complete_rec(DNode node, 
			Map<DNode, TreeSet<Integer>> map, int[] order) {
		TreeSet<Integer> succ = map.get(node);
		if (succ == null) {
			succ = new TreeSet<Integer>();
			map.put(node, succ);
			if (node.hi instanceof DNode) {
				succ.addAll(complete_rec((DNode)node.hi, map, order));
				succ.add(((DNode)node.hi).var);
			}	
			if (node.lo instanceof DNode) {
				succ.addAll(complete_rec((DNode)node.lo, map, order));
				succ.add(((DNode)node.lo).var);
			}
		}
		return succ;
	}
	
	static void complete_rec2(DNode node, TreeSet<Integer> succ, 
			Set<DNode> visited, int[] order, Map<NLtrio, DNode> dests) {
	    if (visited.contains(node)) 
	    	return;
	    visited.add(node);
	    succ = (TreeSet<Integer>) succ.clone();
	    succ.remove(node.var);
	    
	    if (node.hi instanceof DNode) {
	    	complete_rec2((DNode) node.hi, succ, visited, order, dests);
	    }
	    if (node.lo instanceof DNode) {
	    	complete_rec2((DNode) node.lo, succ, visited, order, dests);
	    }
	    node.hi = getDummyChain(node.hi, node.var, order, dests, succ);
    	node.lo = getDummyChain(node.lo, node.var, order, dests, succ);  
	}
	
	// create dummy nodes to avoid level skipping
	// this method should be called before orphans are created
	// because they are not considered
	static public void completeBDD2(BDD bdd, int[] order) {
		Map<NLtrio,DNode> dests = new HashMap<NLtrio,DNode>();
		
		LinkedList<DNode> wq = new LinkedList<DNode>();
		BDD.DNode cur;
		
		for (int i=0; i<bdd.root.length; ++i) {
			BaseNode curn = bdd.root[i];
			if (!(curn instanceof DNode))
				continue;
				
			cur = (DNode) curn;
			if (cur.var != order[0]) {
				bdd.root[i] = getDummyChain(cur, order[0], order, dests, null);
			}
			wq.add(cur);
		}
		
		int n;
		while (!wq.isEmpty()) {
			cur = wq.removeFirst();
			
			for (n=0; order[n]!=cur.var; ++n)
				;
			if (cur.hi instanceof TNode) {
				if (cur.var != order[order.length - 1]) {
					cur.hi = getDummyChain(cur.hi, order[n+1], order, dests, null);
				}
			} else {
				DNode hi = (DNode) cur.hi;
				if (order[n+1] != hi.var) {
					cur.hi = getDummyChain(cur.hi, order[n+1], order, dests, null);
				}
				wq.add(hi);
			}		
			if (cur.lo instanceof TNode) {
				if (cur.var != order[order.length - 1]) {
					cur.lo = getDummyChain(cur.lo, order[n+1], order, dests, null);
				}
			} else {
				DNode lo = (DNode) cur.lo;
				if (order[n+1] != lo.var) {
					cur.lo = getDummyChain(cur.lo, order[n+1], order, dests, null);
				}
				wq.add(lo);
			}
				
		}
	}
	
	// like completeBDD, but it produces fewer nodes by optimizing cases
	// where having all variables present is not necessary
	// TODO: impl
	static public void normalizeBDD(BDD bdd, int[] order) {
		
		Map<DNode, TreeSet<Integer>> map = new HashMap<DNode, TreeSet<Integer>>();
		
		for (int i=0; i<bdd.root.length; ++i) {
			BaseNode curn = bdd.root[i];
			if (curn instanceof DNode)
				normalize_rec((DNode) curn, map, order);
		}
		Map<NLtrio, DNode> dests = new HashMap<NLtrio, DNode>();
		Set<DNode> visited = new HashSet<DNode>();
		for (int i=0; i<bdd.root.length; ++i) {
			BaseNode curn = bdd.root[i];
			if (curn instanceof DNode)
				normalize_rec2((DNode) curn, map.get(curn), 
						visited, order, dests);
		}
		
	}
	
	static Set<Integer> normalize_rec(DNode node, 
			Map<DNode, TreeSet<Integer>> map, int[] order) {
		TreeSet<Integer> succ = map.get(node);
		if (succ == null) {
			succ = new TreeSet<Integer>();
			map.put(node, succ);
			if (node.hi instanceof DNode) {
				succ.addAll(complete_rec((DNode)node.hi, map, order));
				succ.add(((DNode)node.hi).var);
			}	
			if (node.lo instanceof DNode) {
				succ.addAll(complete_rec((DNode)node.lo, map, order));
				succ.add(((DNode)node.lo).var);
			}
		}
		return succ;	
	}
	
	static void normalize_rec2(DNode node, TreeSet<Integer> succ, 
			Set<DNode> visited, int[] order, Map<NLtrio, DNode> dests) {
	    if (visited.contains(node)) 
	    	return;
	    visited.add(node);
	    succ = (TreeSet<Integer>) succ.clone();
	    succ.remove(node.var);
	    
	    if (node.hi instanceof DNode) {
	    	complete_rec2((DNode) node.hi, succ, visited, order, dests);
	    }
	    if (node.lo instanceof DNode) {
	    	complete_rec2((DNode) node.lo, succ, visited, order, dests);
	    }
	    node.hi = getDummyChain(node.hi, node.var, order, dests, succ);
    	node.lo = getDummyChain(node.lo, node.var, order, dests, succ);  
	}
	
	public static BDD readFile(String file) {
		Map<String,BaseNode> nodes = new HashMap<String,BaseNode>();
		Map<String,String[]> children = new HashMap<String,String[]>();
		Set<Integer> allVars = new HashSet<Integer>();
		String[] rootLabel = null;
		
		try {
			BufferedReader in = new BufferedReader(new FileReader(file));
			String line;
			line = in.readLine();
			if (line == null || !line.startsWith("BDD ")) {
				throw new RuntimeException("Not a BDD file");
			}
			String[] firstLine = line.split(" ");
			int numOuts = Integer.parseInt(firstLine[1]);
			rootLabel = new String[numOuts];
			
			while ((line = in.readLine()) != null) {
				line = line.trim();
				if (line.length() == 0 || line.charAt(0)=='#')
					continue;
				String[] fld = line.split(" ");
				String label = fld[0].trim();
				for (int i=0; i<numOuts; ++i) {
					if (rootLabel[i] == null) {
						rootLabel[i] = label;
						break;
					}
				}
				if (nodes.containsKey(label)) {
					throw new RuntimeException("Duplicate node: " + label);
				}
				if (fld.length == 2) {
					if (fld[1].equalsIgnoreCase("T"))
						nodes.put(label, T_TRUE);
					else if (fld[1].equalsIgnoreCase("F"))
						nodes.put(label, T_FALSE);
					else throw new RuntimeException("Bad line: " + line);
					continue;
				}
				
				if (fld.length != 4)
					throw new RuntimeException("Bad line: " + line);
				
				DNode cur = new DNode();
				cur.label = label;
				cur.var = Integer.parseInt(fld[1]);
				allVars.add(cur.var);
				nodes.put(label, cur);
				children.put(label, new String[] {fld[2].trim(), fld[3].trim()});
			}
			
			for (BaseNode curn : nodes.values()) {
				if (curn instanceof DNode) {
					DNode cur = (DNode) curn;
					String[] hilo = children.get(curn.label);
					cur.hi = nodes.get(hilo[0]);
					cur.lo = nodes.get(hilo[1]);
				}
			}
			
			BDD bdd = new BDD();
			bdd.root = new BaseNode[numOuts];
			for (int i=0; i<numOuts; ++i) {	
				bdd.root[i] = nodes.get(rootLabel[i]);
			}
			
			if (firstLine.length > 2) {
				bdd.varorder = new int[firstLine.length - 2];
				for (int i=0; i<bdd.varorder.length; ++i) {
					bdd.varorder[i] = Integer.parseInt(firstLine[i+2]);
				}
			} else {
				BDDInference.inferVarOrder(bdd);
				
			}
			
			int maxVar = -1;
			for (int v : bdd.varorder) {
				if (v > maxVar) {
					maxVar = v;
				}
			}
			
			//bdd.levels = allVars.size();
			bdd.levels = maxVar + 1;
			//System.out.println(bdd.levels + " vars");
			
			return bdd;
		} catch (IOException ex) {
			ex.printStackTrace();
			throw new RuntimeException("Error reading BDD file: " + file);
		}
	}

	
}
