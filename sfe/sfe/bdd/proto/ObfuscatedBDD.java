package sfe.bdd.proto;
import java.util.*;
import java.io.*;

import sfe.util.Bytes;

/**
 * Obfuscated BDD representation
 * created by Alice during evaluation
 * 
 * @author lpkruger
 *
 */
public class ObfuscatedBDD implements java.io.Serializable {
	
	// level secrets, chosen from S and S' by Alice
	
	static class BaseNode implements java.io.Serializable {
		byte[] label;
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
	
	BaseNode[] root;
	Map<Bytes, BaseNode> nodes = new HashMap<Bytes, BaseNode>();
	
	
	
	static void writeInt(ObjectOutputStream out, int n, int bytes) throws IOException {
		switch(bytes) {
		case 4:
			out.writeByte((byte)((n>>24)&0xff));
		case 3:
			out.writeByte((byte)((n>>16)&0xff));
		case 2:
			out.writeByte((byte)((n>>8)&0xff));
		case 1:
			out.writeByte((byte)((n)&0xff));
		}
	}
	
	static int readInt(ObjectInputStream in, int bytes) throws IOException {
		int n = 0;
		switch(bytes) {
		case 4:
			n |= ((in.readByte() << 24) & 0xff000000);
		case 3:
			n |= ((in.readByte() << 16) & 0x00ff0000);
		case 2:
			n |= ((in.readByte() <<  8) & 0x0000ff00);
		case 1:
			n |= (in.readByte() & 0x000000ff);
		}
		return n;
	}
	void writeBDD(ObjectOutputStream out) throws IOException {
		out.writeInt(T_TRUE.label.length);
		for (BaseNode node : nodes.values()) {
			if (node instanceof DNode) {
				out.writeInt(((DNode)node).child1.length);
				break;
			}
		}
		int maxVar = -1;
		for (BaseNode node : nodes.values()) {
			if (node instanceof DNode) {
				if (((DNode)node).var > maxVar)
					maxVar = ((DNode)node).var;
			}
		}

		int varBytes = 1+(int)(Math.log(maxVar)/Math.log(2.0)/8.0);
		out.writeByte((byte) varBytes);
		
		out.write(T_TRUE.label);
		out.write(T_FALSE.label);
		
		out.writeInt(nodes.size() - 2);
		//System.out.println("Sending " + (nodes.size()-2) + " nodes");
		int CCCNT = 0;
		for (BaseNode node : nodes.values()) {
			if (node instanceof DNode) {
				++CCCNT;
				DNode dn = (DNode) node;
				writeInt(out, dn.var, varBytes);
				//System.out.println("label bytes: " + dn.label.length);
				out.write(dn.label);
				//System.out.println("child1 bytes: "+ dn.child1.length);
				out.write(dn.child1);
				//System.out.println("child2 bytes: "+ dn.child2.length);
				out.write(dn.child2);
			}
		}
		//System.out.println("Send " + CCCNT + " nodes");
		
		out.writeInt(root.length);
		for (BaseNode node : root) {
			out.write(node.label);
		}
	}
	
	static ObfuscatedBDD readBDD(ObjectInputStream in) throws IOException {
		ObfuscatedBDD obdd = new ObfuscatedBDD();
		int labellen = in.readInt();
		int linklen = in.readInt();
		int varlen = in.readByte();
		
		byte[] trueLabel = new byte[labellen];
		in.readFully(trueLabel);
		obdd.nodes.put(new Bytes(trueLabel), obdd.T_TRUE);
		byte[] falseLabel = new byte[labellen];
		in.readFully(falseLabel);
		obdd.nodes.put(new Bytes(falseLabel), obdd.T_FALSE);
		
		int numNodes = in.readInt();
		for (int i=0; i<numNodes; ++i) {
			DNode dn = new DNode();
			dn.var = readInt(in, varlen);
			dn.label = new byte[labellen];
			in.readFully(dn.label);
			dn.child1 = new byte[linklen];
			in.readFully(dn.child1);
			dn.child2 = new byte[linklen];
			in.readFully(dn.child2);
			
			obdd.nodes.put(new Bytes(dn.label), dn);
		}
		
		obdd.root = new BaseNode[in.readInt()];
		for (int i=0; i<obdd.root.length; ++i) {
			byte[] thisroot = new byte[labellen];
			in.readFully(thisroot);
			obdd.root[i] = obdd.nodes.get(new Bytes(thisroot));
		}
		return obdd;
	}
}

