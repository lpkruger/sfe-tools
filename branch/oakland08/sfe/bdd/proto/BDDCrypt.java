package sfe.bdd.proto;
import javax.crypto.*;
import javax.crypto.spec.*;

import sfe.bdd.bdd.BDD;
import sfe.crypto.*;
import sfe.util.Bytes;

import java.security.*;
import java.util.*;

/**
 * Routines to encrypt a BDD into obfuscated form.
 * 
 * @author lpkruger
 * 
 */

public class BDDCrypt {
	//public String CIPHER = "AES";
	public String CIPHER = "SHA-1//NoPadding";
	
	SFECipher C;
	public SFEKeyGenerator KG;
	{
		try {
			C = SFECipher.getInstance(CIPHER);
			KG = SFEKeyGenerator.getInstance(CIPHER);
			//KG.init(128);
		} catch (GeneralSecurityException ex) {
			ex.printStackTrace();
			throw new RuntimeException("Can't initialize cipher");
		}
	}
	
	// simple struct with all return values from encryptBDD fucntion
	public static class AliceData {
		public SecretKey[][] levsk;
		public SecretKey[] rootsk;
		public ObfuscatedBDD obdd;
	}
	
	// take BDD in canonical form, and produce obfuscated version and 
	// associated keys
	public AliceData encryptBDD(BDD bdd) {
		int nodeCount = BDD.countNodesAndOrphans(bdd);
		labelBytes = 1 + (int)(Math.log(nodeCount) / Math.log(2.0) / 8.0);
		System.out.println("label size is " + labelBytes + " bytes");
		ObfuscatedBDD obdd;
		obdd = new ObfuscatedBDD();
		obdd.T_TRUE.label = createLabel();
		obdd.T_FALSE.label = createLabel();
		obdd.nodes.put(new Bytes(obdd.T_TRUE.label), obdd.T_TRUE);
		obdd.nodes.put(new Bytes(obdd.T_FALSE.label), obdd.T_FALSE);
		// generate level secrets
		SecretKey[][] levsk = new SecretKey[bdd.levels][2];
		for (int i=0; i<levsk.length; ++i) {
			levsk[i][0] = KG.generateKey();
			levsk[i][1] = KG.generateKey();
		}
		
		// traverse BDD depth first, create nodes
		HashMap<BDD.DNode,ObfuscatedBDD.DNode> mapped =
			new HashMap<BDD.DNode,ObfuscatedBDD.DNode>();
		HashMap<BDD.BaseNode,SecretKey> nodekey =
			new HashMap<BDD.BaseNode,SecretKey>();
		LinkedList<BDD.DNode> stack =
			new LinkedList<BDD.DNode>();
		

		nodekey.put(BDD.T_TRUE, KG.generateKey());
		nodekey.put(BDD.T_FALSE, KG.generateKey());
	
		for (BDD.BaseNode orph : bdd.orphans) {
			if (orph instanceof BDD.DNode) {
				stack.add((BDD.DNode)orph);
			}
		}
		for (int i=0; i<bdd.root.length; ++i) {
			if (bdd.root[i] instanceof BDD.DNode) {
				stack.add((BDD.DNode) bdd.root[i]);
			}
		}
		
		
		BDD.DNode cur;
		ObfuscatedBDD.DNode ocur;
		
		while (!stack.isEmpty()) {
			cur = stack.removeLast();
			ocur = mapped.get(cur);
			if (ocur != null && ocur.label != null)
				continue;
			
			if (ocur == null) {
				//System.out.println("1st visit: " + cur);
				// first visit
				ocur = new ObfuscatedBDD.DNode();
				ocur.var = cur.var;
				mapped.put(cur, ocur);
				stack.add(cur);  // we'll come back for second visit
				if (cur.hi instanceof BDD.TNode) {
					//ocur.hi = obdd.getTNode(((BDD.TNode)cur.hi).val);
				} else {
					stack.add((BDD.DNode)cur.hi);
				}
				if (cur.lo instanceof BDD.TNode) {
					//ocur.lo = obdd.getTNode(((BDD.TNode)cur.lo).val);
				} else {
					stack.add((BDD.DNode)cur.lo);
				}
			} else {
				//System.out.println("2nd visit: " + cur);
				// second visit
				SecretKey cursk = KG.generateKey(); 
				ocur.label = createLabel();
				nodekey.put(cur, cursk);
				{
					ObfuscatedBDD.BaseNode ohi;
					SecretKey skhi;
					if (cur.hi instanceof BDD.TNode) {
						ohi = obdd.getTNode(((BDD.TNode)cur.hi).val);
					} else {
						ohi = mapped.get((BDD.DNode)cur.hi);
					}
					skhi = nodekey.get(cur.hi);
					byte[] hilabel = ohi.label;
					ocur.child1 = encLink(ocur.label, cursk, levsk[cur.var][1], skhi, hilabel);
				}
				{
					ObfuscatedBDD.BaseNode olo;
					SecretKey sklo;
					if (cur.lo instanceof BDD.TNode) {
						olo = obdd.getTNode(((BDD.TNode)cur.lo).val);
					} else {
						olo = mapped.get((BDD.DNode)cur.lo);
					}
					sklo = nodekey.get(cur.lo);
					byte[] lolabel = olo.label;
					ocur.child2 = encLink(ocur.label, cursk, levsk[cur.var][0], sklo, lolabel);
				}
				
				// play the shell game so Bob doesn't know which is which
				// we'll sort them by ciphertext
				if (1 == new Bytes(ocur.child1).compareTo(new Bytes(ocur.child2))) {
					byte[] tmp = ocur.child2;
					ocur.child2 = ocur.child1;
					ocur.child1 = tmp;
				}
				
				obdd.nodes.put(new Bytes(ocur.label), ocur);
			}
		}
		
		obdd.root = new ObfuscatedBDD.BaseNode[bdd.root.length];
		for (int i=0; i<bdd.root.length; ++i) { 
			obdd.root[i] = mapped.get(bdd.root[i]);
			if (obdd.root[i] == null) {
				System.out.println("Move root " + i + " " + bdd.root[i]);
				obdd.root[i] = ((BDD.TNode)bdd.root[i]).val 
				? obdd.T_TRUE : obdd.T_FALSE;
			}
		}
		
		AliceData ret = new AliceData();
		ret.obdd = obdd;
		ret.rootsk = new SecretKey[bdd.root.length];
		for (int i=0; i<bdd.root.length; ++i) {
			ret.rootsk[i] = nodekey.get(bdd.root[i]);
		}
		ret.levsk = levsk;
		
		return ret;
	}
	
	// generate a random label.  For conveniece, labels and keys have the
	// same bit length (it's not a requirement though)
	// -- new: use shorter labels to save bytes
	// -- new new: make them contiguous to save even more bytes
	int labelBytes = -1;
	Set<Bytes> allLabels = new HashSet<Bytes>();
	byte[] createLabel() {
		Bytes bb;
		do {
			byte[] b = SFEKey.keyToBytes(KG.generateKey());
			bb = new Bytes(new byte[labelBytes]);
			System.arraycopy(b, b.length - labelBytes, bb.bytes, 0, labelBytes);
		} while (allLabels.contains(bb));
		allLabels.add(bb);
		return bb.bytes;
	}
	
	// encrypt a link according to BDD encryption algorithm
	byte[] encLink(byte[] iv, SecretKey sv1, SecretKey sl, SecretKey sv2, byte[] lv2) {
		SecretKey esk = SFEKey.xor(sv1, sl, CIPHER);
		byte[] sv2bytes = SFEKey.keyToBytes(sv2);
		byte[] data = new byte[lv2.length + sv2bytes.length + 1];
		data[0] = (byte) lv2.length;
		System.arraycopy(lv2, 0, data, 1, lv2.length);
		System.arraycopy(sv2bytes, 0, data, 1+lv2.length, sv2bytes.length);
		
		try {
			C.init(C.ENCRYPT_MODE, esk);
			C.setIV(iv);
		} catch (GeneralSecurityException ex) {
			ex.printStackTrace();
			throw new RuntimeException("Can't initialize cipher key");
		}
		try {
			byte[] enc = C.doFinal(data);
			//System.out.println("ENC: " + data + "\n  -->  " + enc);
			return enc;
		} catch (GeneralSecurityException ex) {
			ex.printStackTrace();
			throw new RuntimeException("Can't encrypt data");
		}
	}
}
