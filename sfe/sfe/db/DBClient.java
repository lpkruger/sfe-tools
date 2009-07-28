package sfe.db;

import java.math.BigInteger;
import java.util.Random;

public class DBClient {
	Random rand;
	public DBClient() {
	}
	
	public DBClient(DB srv) {
		this.g = srv.g;
		this.p = srv.p;
	}
	
	BigInteger g,p;
	
	void setup() {
		BigInteger r1 = new BigInteger(p.bitLength()+16, rand).mod(p);
		BigInteger r2; // = ???
	}
}
