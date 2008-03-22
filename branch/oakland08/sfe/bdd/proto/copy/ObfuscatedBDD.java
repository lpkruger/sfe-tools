package bdd.proto.copy;
import java.util.*;

/**
 * Obfuscated BDD representation
 * created by Alice during evaluation
 * 
 * @author lpkruger
 *
 */
public class ObfuscatedBDD implements java.io.Serializable {
	
	// level secrets, chosen from S and S' by Alice
	
	String[] levelSec;
	
	static class BaseNode implements java.io.Serializable {
		String label;
		String key;
	}
	static class DNode extends BaseNode {
		int var;
		String child1;		// encrypted representation of 
		String child2;		// next element in path
		// it is randomized which child is hi and lo 
		// to prevent info leak
	}
	static class TNode extends BaseNode {
		boolean val;
		TNode(boolean val) {
			this.val = val;
		}
	}
	
	TNode T_TRUE = new TNode(true);
	TNode T_FALSE = new TNode(false);
	
	TNode getTNode(boolean v) {
		return v ? T_TRUE : T_FALSE;
	}
	
	BaseNode root;
	Map<String, BaseNode> nodes = new HashMap<String, BaseNode>();
}

