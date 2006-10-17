package bdd.proto.copy;
import javax.crypto.*;
import javax.crypto.spec.*;

import java.math.BigInteger;
import java.security.*;
import java.util.*;

/**
 * Routines to encrypt a BDD into obfuscated form.
 * 
 * @author lpkruger
 * 
 */

public class BDDCrypt {
	public String CIPHER = "RC4";
	
	Cipher C;
	public KeyGenerator KG;
	{
		try {
			C = Cipher.getInstance(CIPHER);
			KG = KeyGenerator.getInstance(CIPHER);
			//KG.init(128);
		} catch (GeneralSecurityException ex) {
			ex.printStackTrace();
			throw new RuntimeException("Can't initialize cipher");
		}
	}
	
	// utility functions
	
	// xor byte arrays
	static byte[] xor(byte[] a, byte[] b) {
		if (a.length != b.length)
			throw new IllegalArgumentException(a.length + " != " + b.length);
		byte[] c = new byte[a.length];
		for (int i=0; i<c.length; ++i) {
			c[i] = (byte) (a[i] ^ b[i]);
		}
		return c;
	}
	
	// xor keys represented as byte arrays
	SecretKey xor(SecretKey a, SecretKey b) {
		return new SecretKeySpec(xor(a.getEncoded(),
				b.getEncoded()), CIPHER);
	}
	
	// simple struct with all return values from encryptBDD fucntion
	public static class AliceData {
		public SecretKey[][] levsk;
		public SecretKey rootsk;
		public ObfuscatedBDD obdd;
	}
	
	// take BDD in canonical form, and produce obfuscated version and 
	// associated keys
	public AliceData encryptBDD(BDD bdd) {
		ObfuscatedBDD obdd;
		obdd = new ObfuscatedBDD();
		obdd.T_TRUE.label = createLabel();
		obdd.T_FALSE.label = createLabel();
		obdd.nodes.put(obdd.T_TRUE.label, obdd.T_TRUE);
		obdd.nodes.put(obdd.T_FALSE.label, obdd.T_FALSE);
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
		
		BDD.DNode cur = (BDD.DNode) bdd.root;
		
		stack.add(cur);
		
		nodekey.put(BDD.T_TRUE, KG.generateKey());
		nodekey.put(BDD.T_FALSE, KG.generateKey());
		
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
					String hilabel = ohi.label;
					// TODO: randomize assignment of child1 and child2
					ocur.child1 = encLink(cursk, levsk[cur.var][1], skhi, hilabel);
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
					String lolabel = olo.label;
					ocur.child2 = encLink(cursk, levsk[cur.var][0], sklo, lolabel);
				}
				ocur.label = createLabel();
				obdd.nodes.put(ocur.label, ocur);
			}
		}
		
		obdd.root = mapped.get(bdd.root);
		
		AliceData ret = new AliceData();
		ret.obdd = obdd;
		ret.rootsk = nodekey.get(bdd.root);
		ret.levsk = levsk;
		
		return ret;
	}
	
	// generate a random label.  For conveniece, labels and keys have the
	// same bit length (it's not a requirement though)
	String createLabel() {
		return keyToString(KG.generateKey());
	}
	
	// encrypt a link according to BDD encryption algorithm
	String encLink(SecretKey sv1, SecretKey sl, SecretKey sv2, String lv2) {
		SecretKey esk = xor(sv1, sl);
		String data = lv2 + "," + keyToString(sv2);
		try {
			C.init(C.ENCRYPT_MODE, esk);
		} catch (InvalidKeyException ex) {
			ex.printStackTrace();
			throw new RuntimeException("Can't initialize cipher key");
		}
		try {
			String enc = Base64.encodeBytes(C.doFinal(stringToBytes(data)));
			System.out.println("ENC: " + data + "\n  -->  " + enc);
			return enc;
		} catch (GeneralSecurityException ex) {
			ex.printStackTrace();
			throw new RuntimeException("Can't encrypt data");
		}
	}
	
	// helper conversion function
	String keyToString(SecretKey k) {
		return Base64.encodeBytes(k.getEncoded());
	}
	
	// helper conversion function	
	SecretKey stringToKey(String k) {
		return new SecretKeySpec(Base64.decode(k, false), CIPHER);
	}
	
	// helper conversion function
	public static SecretKey bigIntToKey(BigInteger big, int size, String C) {
		byte[] bytes = big.toByteArray();
		if (bytes.length > size) {
			for (int i=0; i<bytes.length - size; ++i) {
				if (bytes[i] != 0)
					throw new RuntimeException("oops  " + bytes.length + " > " + size);
			}
			byte[] newbytes = new byte[size];
			System.arraycopy(bytes, bytes.length - size, newbytes, 
					0, size);
			bytes = newbytes;
		}
		if (bytes.length < size) {
			byte[] newbytes = new byte[size];
			System.arraycopy(bytes, 0, newbytes, 
					size - bytes.length, bytes.length);
			bytes = newbytes;
		}
		return new SecretKeySpec(bytes, C);
	}
	
	// helper conversion function
	public static BigInteger keyToBigInt(SecretKey sk) {
		return new BigInteger(1, sk.getEncoded());
	}
	
	// helper conversion function
	public static String bytesToString(byte[] b) {
		try {
			return new String(b, 0, b.length, "UTF-8");
		} catch (java.io.UnsupportedEncodingException ex) {
			throw new RuntimeException("Can't use UTF-8");
		}
	}
	
	// helper conversion function
	public static byte[] stringToBytes(String s) {
		try {
			return s.getBytes("UTF-8");
		} catch (java.io.UnsupportedEncodingException ex) {
			throw new RuntimeException("Can't use UTF-8");
		}
	}
}
