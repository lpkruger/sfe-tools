package sfe.db;

import java.math.BigInteger;

import sfe.crypto.HomomorphicCipher;
import sfe.crypto.SFECipher;
import sfe.crypto.SFEKey;

public class K_OT {
	HomomorphicCipher.EncKey enc;
	HomomorphicCipher.DecKey dec;
	BoyenBonehSig sig;
	SFECipher symciph;
	
	BigInteger G1, G2, Gr, g, p;
	BigInteger w[];
	BigInteger c[];
	void setup(BigInteger N, BigInteger s, BigInteger[] m) throws Exception {
		int l=24;  //security param
		enc.encrypt(N.shiftRight(1).add(s));
		
		SFEKey[] symkey = new SFEKey[m.length];
		
		for (int i=0; i<m.length; ++i) {
			// p-1 ?
			symkey[i] = new SFEKey(g.modPow(s.add(w[i]).modInverse(p), p).toByteArray());
			symciph.init(SFECipher.ENCRYPT_MODE, symkey[i]);
			c[i] = new BigInteger(symciph.doFinal(m[i].shiftRight(l).toByteArray()));
		}
	}
}
