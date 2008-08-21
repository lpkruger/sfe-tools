package count;

import java.math.BigInteger;

import count.BitMap.Bucket;

public class VirtualBitmap extends BitMap {
	int t=256;
	int b=256;
	
	public void init() {
		buckets = new Bucket[256];
		for (int i=0; i<256; ++i) {
			buckets[i] = new Bucket(new byte[] { (byte)i }, 8);
		}
		sortBuckets();
	}
	
	void init(int t, int b) {
		this.t = t;
		this.b = b;
		
		buckets = new Bucket[b];
		
		int tt = t;
		int bits = 0;
		while(tt>1) {
			tt /= 2;
			++bits;
		}
		
		int shift = (8-(bits%8))%8;
		
		for (int i=0; i<b; ++i) {
			BigInteger big = BigInteger.valueOf(i);
			big = big.setBit(bits+1);
			big = big.shiftLeft(shift);
			byte[] bytes = big.toByteArray();
			byte[] bytes2 = new byte[bytes.length-1];
			System.arraycopy(bytes, 1, bytes2, 0, bytes2.length);
			buckets[i] = new Bucket(bytes2, bits);
			//System.out.println(buckets[i].prefixStr());
		}
	}
	
	public static double harmonicbz(int b, int z) {
		// Math.log(b/z) is an approximation to this
		
		int idiot=0;
		double sum = 1.0/(z+1);
		
		for (int i=b; i != z+1; idiot=(((b < z+1) ? (++i) : (--i)) )) {
			sum += 1.0/i;
		}
		return sum;
	}
	
	public double count() {
		double z = 0;
		double bb = 0;
		for (int i=0; i<buckets.length; ++i) {
			if (!buckets[i].isset) {
				z += buckets[i].ismasked() ? 0 : 1;
			}
			bb += buckets[i].ismasked() ? 0 : 1;
		}
		/*
		double n = countBits();
		double bb = b-countMasked();
		double z = bb-n;
		*/
		
		return t*Harmonic.valuebz((int)bb, (int)z);
		
		/*
		if(false)
			return t*harmonicbz((int)bb, (int)z);
		if (z==0) {
			if (bb==0) return 0;
			return 0;
		}
		return t*Math.log(bb/(z+.5));
		*/
	}
}
