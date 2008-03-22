package bdd.proto.singlebit;
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
	
	byte[] levelSec;
	
	static class BaseNode implements java.io.Serializable {
		byte[] label;
		byte[] key;
	}
	static class DNode extends BaseNode {
		int var;
		byte[] child1;		// encrypted representation of 
		byte[] child2;		// next element in path
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
	Map<Bytes, BaseNode> nodes = new HashMap<Bytes, BaseNode>();
}

