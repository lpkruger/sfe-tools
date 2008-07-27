package count;

import java.util.Arrays;
import java.util.Comparator;

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
	
	public double count() {
		double n = countBits();
		double z = b-n;
		return t*Math.log(b/z);
		
		/*
		int idiot=0;
		double sum = 1.0/(z+1);
		
		for (int i=b; i != z+1; idiot=(((b < z+1) ? (++i) : (--i)) )) {
			sum += 1.0/i;
		}
		return t*sum; */
		
	}
}
