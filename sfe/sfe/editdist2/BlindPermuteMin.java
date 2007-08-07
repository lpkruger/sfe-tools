package sfe.editdist2;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.Random;

import sfe.crypto.ElGamal;
import sfe.crypto.HomomorphicCipher;

public class BlindPermuteMin {
	// Alice has a1, a2, a3
	// Bob has b1, b2, b3
	// find min of (ai+bi) using Purdue protocol
	// and LinTzeng for GT calculation
	
	static int bitsize = 32;
	
	public static class Alice {
		ObjectInputStream in;
		ObjectOutputStream out;
		HomomorphicCipher.EncKey aenc;
		HomomorphicCipher.DecKey adec;
		HomomorphicCipher.EncKey benc;
		ElGamal.DecKey egprkey;
		ElGamal.EncKey egpubkey;
		
		Random rand = new SecureRandom();
		
		public Alice(ObjectInputStream in, ObjectOutputStream out) {
			this.in = in;
			this.out = out;
		}
		
		public void setKeys(HomomorphicCipher.EncKey aenc,
				HomomorphicCipher.DecKey adec,
				HomomorphicCipher.EncKey benc,
				ElGamal.EncKey egpubkey,
				ElGamal.DecKey egprkey) {
			this.aenc = aenc;
			this.adec = adec;
			this.benc = benc;
			this.egprkey = egprkey;
			this.egpubkey = egpubkey;
		}
		
		public BigInteger go(BigInteger[] a) throws Exception {
			// Alice is the sender
			BigInteger[] encvals = new BigInteger[a.length];
			for (int i=0; i<encvals.length; ++i) {
				encvals[i] = aenc.encrypt(shiftup(a[i]));
			}
			
			out.writeObject(encvals);
			out.flush();
			
			encvals = (BigInteger[]) in.readObject();
			for (int i=0; i<a.length; ++i) {
				a[i] = shiftdown(adec.decrypt(encvals[i]));
				System.out.print(a[i] + " ");
			}
			System.out.println();
			
			encvals = null;
			
			// Alice and Bob switch roles and repeat
			encvals = (BigInteger[]) in.readObject();
			 
			Permute.permuteSeveral(new Object[][] {encvals, a}, rand);
			
			for (int i=0; i<encvals.length; ++i) {
				BigInteger blind = new BigInteger(40, rand);
				//encvals[i] = benc.add(encvals[i], benc.encrypt(blind));
				//a[i] = a[i].subtract(blind);
				System.out.print(a[i] + " ");
			}
			System.out.println();
			
			out.writeObject(encvals);
			out.flush();
			
			// now fully blind and permuted, so calculate min
		    for (int i=1; i<a.length; ++i) {
		    	// LTz returns true if (a+b)[i-1]>(a+b)[i]
		    	BigInteger aval = a[i-1].subtract(a[i]);
		    	System.out.println("Start LTz");
		    	LinTzeng.Alice ltalice = new LinTzeng.Alice(in, out);
		    	ltalice.setKeys(egpubkey, egprkey);
		    	if (!ltalice.go(aval, bitsize)) {
		    		a[i] = a[i-1];
		    	}
		    }
		    out.flush();
		    System.out.println("BPM done");
		    return a[a.length-1];
		}
	}
	
	public static class Bob {
		ObjectInputStream in;
		ObjectOutputStream out;
		HomomorphicCipher.EncKey benc;
		HomomorphicCipher.DecKey bdec;
		HomomorphicCipher.EncKey aenc;
		ElGamal.EncKey egpubkey;
		
		Random rand = new SecureRandom();

		public Bob(ObjectInputStream in, ObjectOutputStream out) {
			this.in = in;
			this.out = out;
		}
		
		public void setKeys(HomomorphicCipher.EncKey benc,
				HomomorphicCipher.DecKey bdec,
				HomomorphicCipher.EncKey aenc,
				ElGamal.EncKey egpubkey) {
			this.aenc = aenc;
			this.bdec = bdec;
			this.benc = benc;
			this.egpubkey = egpubkey;
		}
		
		public BigInteger go(BigInteger[] b) throws Exception {
			// Bob is the receiver
			BigInteger[] encvals = 
				(BigInteger[]) in.readObject();
			 
			Permute.permuteSeveral(new Object[][] {encvals, b}, rand);
			
			for (int i=0; i<encvals.length; ++i) {
				BigInteger blind = new BigInteger(40, rand);
				encvals[i] = aenc.add(encvals[i], aenc.encrypt(blind));
				b[i] = b[i].subtract(blind);
				System.out.print(b[i] + " ");
			}
			System.out.println();
			
			out.writeObject(encvals);
			
			// need a new object to bypass reflection cache
			encvals = new BigInteger[b.length];
			// Alice and Bob switch roles and repeat
			for (int i=0; i<encvals.length; ++i) {
				encvals[i] = benc.encrypt(shiftup(b[i]));
			}
			
			out.writeObject(encvals);
			out.flush();
			
			encvals = (BigInteger[]) in.readObject();
			for (int i=0; i<b.length; ++i) {
				b[i] = shiftdown(bdec.decrypt(encvals[i]));
				System.out.print(b[i] + " ");
			}
			System.out.println();
			
			// now fully blind and permuted, so calculate min
		    for (int i=1; i<b.length; ++i) {
		    	// LTz returns true if (a+b)[i-1]>(a+b)[i]
		    	BigInteger bval = b[i].subtract(b[i-1]);
		    	System.out.println("Start LTz");
		    	LinTzeng.Bob ltbob = new LinTzeng.Bob(in,out);
		    	ltbob.setKeys(egpubkey);
		    	if (!ltbob.go(bval)) {
		    		b[i] = b[i-1];
		    	}
		    }
		    out.flush();
		    System.out.println("BPM done");
		    return b[b.length-1];
			
		}
	}
	
	static BigInteger bignum = BigInteger.valueOf(10).pow(30);
	static BigInteger shiftup(BigInteger n) {
		return n.add(bignum);
	}
	static BigInteger shiftdown(BigInteger n) {
		return n.subtract(bignum);
	}
}
