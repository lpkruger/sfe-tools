package bdd.proto.singlebit;
import javax.crypto.*;
//port javax.crypto.spec.*;
import java.security.*;

/**
 * Routines to evaluate the obfuscated BDD
 * 
 * @author lpkruger
 * 
 */
public class OBDDEval extends BDDCrypt {
	
	public boolean eval(ObfuscatedBDD obdd, SecretKey[] levsk, SecretKey rootsk, boolean[] vars) {
		int level;
		SecretKey skv = rootsk;
		ObfuscatedBDD.BaseNode curn = obdd.root;
		
		while (curn instanceof ObfuscatedBDD.DNode) {
			ObfuscatedBDD.DNode cur = (ObfuscatedBDD.DNode) curn;
			level = cur.var;
			SecretKey skl = levsk[level];
			//System.out.println("var=" + vars[level] + "  skv=" + skv + "  skl=" + skl);
			//String[] labelAndSK = decLink(skv, skl, vars[level] ? cur.hi : cur.lo);
			byte[][] labelAndSK = decLink(skv, skl, cur.child1);
		    if (labelAndSK != null)
		    	curn = obdd.nodes.get(new Bytes(labelAndSK[0]));
		    else
		    	curn = null;
		    
			if (curn == null) {
				labelAndSK = decLink(skv, skl, cur.child2);
				curn = obdd.nodes.get(new Bytes(labelAndSK[0]));
			}
			skv = bytesToKey(labelAndSK[1]);
			
			//System.out.println("curn=" + curn + "  skv=" + skv);
		}
		
		return ((ObfuscatedBDD.TNode) curn).val;
	}
	
	byte[][] decLink(SecretKey sv1, SecretKey sl, byte[] data) {
		System.out.println("DEC: " + data);
		SecretKey esk = xor(sv1, sl);
		// String data = lv2 + "," + keyToString(sv2);
		try {
			C.init(C.DECRYPT_MODE, esk);
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
		} catch (GeneralSecurityException ex) {
			ex.printStackTrace();
			throw new RuntimeException("Can't decrypt data");
		}
	}
}
