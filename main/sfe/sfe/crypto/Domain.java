package sfe.crypto;

import java.math.*;
import java.util.Random;

public class Domain implements java.io.Serializable {
	BigInteger illegalValue;
	
	Random rand = new Random();
	
	MathContext mc = new MathContext(32, RoundingMode.HALF_EVEN);
	static BigDecimal ONEHALF = new BigDecimal("0.5");
	BigDecimal denom;
	BigDecimal scale;
	BigDecimal offset;
	public BigInteger dint;
	BigInteger rot;		// rotation value, makes 0 -> 0
	
	// creates a domain from at least [min,max] in r steps
	public Domain(double min, double max, BigInteger r) {
		this(min, max, r, BigInteger.ZERO);
	}
	
	public BigInteger normalize(BigInteger x) { 
		if (x.compareTo(rot) <= 0)
			return x;
		else
			return x.subtract(dint);
	}
	
	public Domain(double min, double max, BigInteger r, BigInteger boost) {
		dint = r;
		illegalValue = r.add(BigInteger.ONE);
		denom = new BigDecimal(r);
		BigDecimal dmin = BigDecimal.valueOf(min);
		BigDecimal dmax = BigDecimal.valueOf(max);
		// // boost will increase the maximum by (max-min)*boost
		// dmax = dmax.add(dmax.subtract(dmin).multiply(new BigDecimal(boost)));
		BigDecimal boostd = new BigDecimal(boost);
		dmax = dmax.multiply(boostd.add(BigDecimal.ONE));
		dmin = dmin.multiply(boostd.add(BigDecimal.ONE));
		
		// constraints:
		// 0 -> min:
		// offset/denom = min
		// offset = denom*min
		offset = denom.multiply(dmin);
		
		// denom-1 -> max
		// ((denom-1)*scale + offset) / denom = max
		// ((denom-1)/denom)*scale + min = max
		// scale = (max - min) * (denom/(denom-1))
		scale = dmax.subtract(dmin).
		multiply(denom.divide(denom.subtract(BigDecimal.ONE), mc));
		
		rot = BigInteger.ZERO;
		rot = fromDouble(0);
	}
	
	public BigInteger fromDouble(double d) {
		// z = (d*denom - offset)/scale
		return BigDecimal.valueOf(d).multiply(denom)
		.subtract(offset).divide(scale, mc).toBigInteger().subtract(rot);
	}
	
	public double toDouble(BigInteger z) {
		// d = (z*scale + offset) / denom
		return new BigDecimal(z.add(rot)).multiply(scale).add(offset)
		.divide(denom, mc).doubleValue();
	}
	
	// VERIFY: is this truly random?
	public BigInteger getRandom() {
		BigInteger r = new BigInteger(dint.bitLength() + 16, rand);
		return r.mod(dint);
	}
	
	public BigInteger minValue() {
		return BigInteger.ZERO;
	}
	
	public BigInteger maxValue() {
		return dint.subtract(BigInteger.ONE);
	}
	
	// test
	public static void main(String[] args) {
		Domain domain = new Domain(-20, 20, 
				java.math.BigInteger.valueOf(3).pow(16));
		double d = 10;
		System.out.println(d);
		BigInteger z = domain.fromDouble(d);
		System.out.println(z);
		d = domain.toDouble(z);
		System.out.println(d);
		z = domain.fromDouble(d);
		System.out.println(z);
		d = domain.toDouble(z);
		System.out.println(d);
	}
}
