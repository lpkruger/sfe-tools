package bdd.proto.copy;
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
			String[] labelAndSK = decLink(skv, skl, cur.child1);
			if (labelAndSK == null)
				labelAndSK = decLink(skv, skl, cur.child2);
			curn = obdd.nodes.get(labelAndSK[0]);
			skv = stringToKey(labelAndSK[1]);
			
			//System.out.println("curn=" + curn + "  skv=" + skv);
		}
		
		return ((ObfuscatedBDD.TNode) curn).val;
	}
	
	String[] decLink(SecretKey sv1, SecretKey sl, String data) {
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
			// return Base64.encodeBytes(C.doFinal(stringToBytes(data)));
			String plain = bytesToString(C.doFinal(Base64.decode(data, false)));
			String[] ret = plain.split(",");
			if (ret.length != 2) {
				//throw new RuntimeException("Decrypted data is invalid");
				return null;
			}
			if (!check64(ret[0])) return null;
			if (!check64(ret[1])) return null;
			System.out.println("  -->  " + plain);
			return ret;
		} catch (GeneralSecurityException ex) {
			ex.printStackTrace();
			throw new RuntimeException("Can't decrypt data");
		}
	}
	
	static boolean check64(String s) {
		for (int i=0; i<s.length(); ++i) {
			char c = s.charAt(i);
			if (c>='a' && c<='z')
				continue;
			if (c>='A' && c<='Z')
				continue;
			if (c>='0' && c<='9')
				continue;
			if (c=='+' || c=='/' || c=='=')
				continue;
			return false;
		}
		return true;
	}
}
