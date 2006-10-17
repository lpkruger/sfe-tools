package sfe.crypto;

import java.math.BigInteger;

public class HomomorphicCipher {
	public static interface DecKey {
		BigInteger decrypt(BigInteger z);
		EncKey encKey();
	}
	
	public static interface EncKey extends java.io.Serializable {
		BigInteger encrypt(BigInteger M);
	}
	
	public static HomomorphicCipher getInstance() {
		return new HomomorphicCipher();
		//return new DPE();
	}
}
