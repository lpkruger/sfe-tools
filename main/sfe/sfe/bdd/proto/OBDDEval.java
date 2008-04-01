package sfe.bdd.proto;
import javax.crypto.*;

import sfe.crypto.SFEKey;
import sfe.util.Base64;
import sfe.util.Bytes;

//port javax.crypto.spec.*;
import java.security.*;

/**
 * Routines to evaluate the obfuscated BDD
 * 
 * @author lpkruger
 * 
 */
public class OBDDEval extends BDDCrypt {
	
	public boolean[] eval(ObfuscatedBDD obdd, SecretKey[] levsk, SecretKey[] rootsk) {
		int level;
		SecretKey skv;
		
		boolean[] ret = new boolean[obdd.root.length];
		for (int i=0; i<obdd.root.length; ++i) {
			skv = rootsk[i];
			ObfuscatedBDD.BaseNode curn = obdd.root[i];
			
			while (curn instanceof ObfuscatedBDD.DNode) {
				ObfuscatedBDD.DNode cur = (ObfuscatedBDD.DNode) curn;
				level = cur.var;
				SecretKey skl = levsk[level];
				//System.out.println("var=" + vars[level] + "  skv=" + skv + "  skl=" + skl);
				//String[] labelAndSK = decLink(skv, skl, vars[level] ? cur.hi : cur.lo);
				byte[][] labelAndSK = decLink(cur.label, skv, skl, cur.child1);
				if (labelAndSK != null)
					curn = obdd.nodes.get(new Bytes(labelAndSK[0]));
				else
					curn = null;
				
				if (curn == null) {
					labelAndSK = decLink(cur.label, skv, skl, cur.child2);
					if (labelAndSK == null) {
						System.out.println("Decryption error cannot decrypt either:");
						System.out.println(Base64.encodeBytes(cur.child1));
						System.out.println(Base64.encodeBytes(cur.child2));
					}
					curn = obdd.nodes.get(new Bytes(labelAndSK[0]));
				}
				skv = SFEKey.bytesToKey(labelAndSK[1], CIPHER);
				
				// System.out.println("curn=" + curn + "  skv=" + skv);
			}
			ret[i] = ((ObfuscatedBDD.TNode) curn).val;
		}
		
		return ret;
	}
	
	byte[][] decLink(byte[] iv, SecretKey sv1, SecretKey sl, byte[] data) {
		//System.out.println("DEC: " + sv1 + " " + sl + " " + data);
		SecretKey esk = SFEKey.xor(sv1, sl, CIPHER);
		// String data = lv2 + "," + keyToString(sv2);
		try {
			C.init(C.DECRYPT_MODE, esk);
			C.setIV(iv);
		} catch (InvalidKeyException ex) {
			ex.printStackTrace();
			throw new RuntimeException("Can't initialize cipher key");
		}
		
		try {
			byte[] plain = C.doFinal(data);
			if (plain[0]<0 || plain.length-1-plain[0]<0)
				return null;
	
			byte[][] ret = new byte[2][];
			ret[0] = new byte[plain[0]];
			ret[1] = new byte[plain.length-1-plain[0]];
			System.arraycopy(plain, 1, ret[0], 0, plain[0]);
			System.arraycopy(plain, 1+plain[0], ret[1], 0, plain.length-1-plain[0]);
			return ret;
		} catch (BadPaddingException ex) {
			// it's not a valid ciphertext
			return null;
		} catch (GeneralSecurityException ex) {
			ex.printStackTrace();
			throw new RuntimeException("Can't decrypt data");
		}
	}
}
