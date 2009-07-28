package sfe.db;

import java.math.BigInteger;
import java.util.*;

import javax.crypto.Cipher;

import sfe.crypto.*;

public class DB {
	static class Key implements Comparable {
		BigInteger k;
		public Key(int kk) {
			k = BigInteger.valueOf(kk);
		}
		public Key(BigInteger kk) {
			k = kk;
		}
		public int compareTo(Object o) {
			return k.compareTo(((Key)o).k);
		}
	}
	static class Val {
		BigInteger v;
		public Val(int vv) {
			v = BigInteger.valueOf(vv);
		}
		public Val(BigInteger vv) {
			v = vv;
		}
	}

	Map<Key,Val> thedb = new TreeMap<Key,Val>();	
	
	public void put(int k, int v) {
		thedb.put(new Key(k), new Val(v));
	}
	public void put(BigInteger k, BigInteger v) {
		thedb.put(new Key(k), new Val(v));
	}
}
