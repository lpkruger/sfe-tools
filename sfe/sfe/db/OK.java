package sfe.db;


import java.math.BigInteger;
import java.security.*;
import java.util.Set;

import sfe.crypto.DeterministicRandom;
import sfe.crypto.RSA;
import sfe.db.DB.Key;

public class OK {
	public void go() {
	
		
		try {
			MessageDigest md = MessageDigest.getInstance("MD5");
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		}
	}
	SecureRandom rand = new SecureRandom();
	
	static int[] test_sizes = { 1,2,3, 10, 20, 50, 100, 200, 500, 1000, 2000, 5000, 10000 };
	public static void main(String[] args) {
		for (int trial=0; trial<Math.min(100,test_sizes.length); ++trial) {
			System.gc();
			DB db = new DB();
			for (int i=0; i<test_sizes[trial]; ++i) {
				int x = i+2;
				int xx = x*x;
				db.put(BigInteger.valueOf(x), BigInteger.valueOf(xx));
				
			}
	
			OK ok = new OK();
			ok.n = db.thedb.size();
			
			System.gc();
			long time1 = System.currentTimeMillis();
			ok.servercommit(db);
			long time2 = System.currentTimeMillis();
			ok.clientxfer1(BigInteger.valueOf(1));
			ok.serverxfer();
			ok.clientxfer2(BigInteger.valueOf(1));
			long time3 = System.currentTimeMillis();
			System.out.println("DB size "+ok.n+": "+(time3-time2)+" online ms, "+(time2-time1)+" offline ms");
			System.out.println();
		}
	}
	
	int L=8;	// security param
	BigInteger r,e;
	int n;
	static BigInteger H(BigInteger x) {
		return x;
	}
	static byte[] Gxor(BigInteger[] x, byte[] m) {
		byte[] z;
		byte[] zz = new byte[0];
		byte[] zzz;
		for (int i=0; i<x.length; ++i) {
			z = x[i].toByteArray();
			zzz = new byte[zz.length + z.length];
			System.arraycopy(zz, 0, zzz, 0, zz.length);
			System.arraycopy(z, 0, zzz, zz.length, z.length);
			zz = zzz;	
		}
		BigInteger xx = new BigInteger(zz);
		//return xx;
		
		SecureRandom prng = DeterministicRandom.getRandom(xx.toByteArray());
		byte[] r = new byte[m.length];
		prng.nextBytes(r);
		for (int i=0; i<r.length; ++i) {
			r[i] ^= m[i];
		}
		return r;
	}
	void clientxfer1(BigInteger w) {
		BigInteger N = rsa.modulus;
		r = new BigInteger(N.bitLength()+16, rand).mod(N);
		BigInteger Y = r.modPow(e, N).multiply(H(w)).mod(N);
		Y_ = Y;
	}
	void clientxfer2(BigInteger w) {
		BigInteger X = X_;
		BigInteger N = rsa.modulus;
		BigInteger what = X.multiply(r.modInverse(N)).mod(N);
		
		search:
		for (int i=0; i<n; ++i) {
			byte[] mi = Gxor(new BigInteger[] {w, what, BigInteger.valueOf(i)}, 
					mhat[i]);
		
			for (int j=0; j<L; ++j) {
				// test for leading 0s
				if (mi[j]!=0)
					continue search;
			}
			
			
			for (int j=0; j<mi.length; ++j) {
				System.out.printf("%02x ", mi[j]);
			}
			System.out.println();
			
		}
		
	}
	//// xfer variables
	BigInteger Y_, X_;
	byte[][] mhat;
	BigInteger[] what;
	//// server variables
	RSA rsa;
	void servercommit(DB db) {
		rsa = new RSA(1024);
		BigInteger N = rsa.modulus;
		BigInteger d = rsa.privateKey;
		e = rsa.publicKey;
		Set<Key> keys = db.thedb.keySet();
		what = new BigInteger[keys.size()];
		mhat = new byte[keys.size()][];
		int i=0;
		for (DB.Key key : keys) {
			BigInteger w = key.k;
			BigInteger m = db.thedb.get(key).v;
			what[i] = H(w).modPow(d, N);
			byte[] mm = m.toByteArray();
			byte[] mpad = new byte[mm.length+L];
			System.arraycopy(mm, 0, mpad, L, mm.length);
			mhat[i] = Gxor(new BigInteger[] {w, what[i], BigInteger.valueOf(i)}, mpad);
			++i;
		}
		
		
	}
	void serverxfer() {
		BigInteger N = rsa.modulus;
		BigInteger Y = Y_;
		BigInteger X = Y.modPow(rsa.privateKey, N);
		X_ = X;
	}
	
}
