package bdd.proto.singlebit;
import java.io.*;
import java.util.*;

/**
 * Canonical BDD representation
 * 
 * @author lpkruger
 *
 */
public class BDD {
	static class BaseNode {
		String label;
	}
	static class DNode extends BaseNode {
		String who;
		int var;
		BaseNode hi;
		BaseNode lo;
	}
	static class TNode extends BaseNode {
		boolean val;
		TNode(boolean val) {
			this.val = val;
		}
	}
	
	static final TNode T_TRUE = new TNode(true);
	static final TNode T_FALSE = new TNode(false);
	
	BaseNode root;
	int levels;
	
	/**
	 * evaluate the BDD with respect to some values.
	 * The BDD is modified, and potentially orphaned nodes
	 * potentially orphaned nodes are returned
	 */
	static public void partialEval(BDD bdd, TreeMap<Integer, Boolean> vals) {
		if (!(bdd.root instanceof DNode))
			return;
		
		LinkedList<DNode> wq = new LinkedList<DNode>();
		BDD.DNode cur = (DNode) bdd.root;
		while (vals.get(cur.var) != null) {
			
			bdd.root = vals.get(cur.var) ? cur.hi : cur.lo;
			if (!(bdd.root instanceof DNode))
				return;
			cur = (DNode) bdd.root;
		}
		
		wq.add(cur);
		
		while (!wq.isEmpty()) {
			cur = wq.removeFirst();
			
			while (cur.hi instanceof DNode && vals.get(((DNode)cur.hi).var) != null) {
				BDD.DNode hi = (DNode) cur.hi;
				cur.hi = vals.get(hi.var) ? hi.hi : hi.lo;
			}
			if (cur.hi instanceof DNode)
				wq.add((DNode)cur.hi);
			
			while (cur.lo instanceof DNode && vals.get(((DNode)cur.lo).var) != null) {
				BDD.DNode lo = (DNode) cur.lo;
				cur.lo = vals.get(lo.var) ? lo.hi : lo.lo;
			}
			if (cur.lo instanceof DNode)
				wq.add((DNode)cur.lo);
		}
	}
	
	static public BDD readFile(String file) {
		Map<String,BaseNode> nodes = new HashMap<String,BaseNode>();
		Map<String,String[]> children = new HashMap<String,String[]>();
		Set<Integer> allVars = new HashSet<Integer>();
		String rootLabel = null;
		
		try {
			BufferedReader in = new BufferedReader(new FileReader(file));
			String line;
			while ((line = in.readLine()) != null) {
				line = line.trim();
				if (line.length() == 0 || line.charAt(0)=='#')
					continue;
				String[] fld = line.split(" ");
				String label = fld[0].trim();
				if (rootLabel == null) {
					rootLabel = label;
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
			bdd.root = nodes.get(rootLabel);
			bdd.levels = allVars.size();
			return bdd;
		} catch (IOException ex) {
			ex.printStackTrace();
			throw new RuntimeException("Error reading BDD file: " + file);
		}
	}
}
