package count;

import java.util.ArrayList;

public class SegmentBitmap extends BitMap {

	int k;
	
	static void addBucket(String s, ArrayList<Bucket> bucks) { // "01010.."
		int bytes = (s.length()+7)/8;
		
		byte[] bb = new byte[bytes];
		for (int i=0; i<s.length(); ++i) {
			if (s.charAt(i)=='1') {
				int bit = i%8;
				if (bit==0) bit=8;
				bb[i/8] |= 1<<((7-(i%8)));
			}
			
		}
		
		bucks.add(new Bucket(bb, s.length()));
	}
	
	
	public void init() {
		DEBUG=false;
		ArrayList<Bucket> bucks = new ArrayList<Bucket>();
		// 000 001  01  10  11
		// 00000 00001  0001 0010 0011  010 011  100 101  110 111 
		k=8;
		Bucket b;
		
		addBucket("0", bucks);
		addBucket("1", bucks);
		/*
		addBucket("000", bucks);
		addBucket("001", bucks);
		addBucket("01", bucks);
		addBucket("10", bucks);
		addBucket("11", bucks);
		*/
		
		for (int j=1; j<k; ++j) {
			ArrayList<Bucket> bucks2 = bucks;
			bucks = new ArrayList<Bucket>();
			
			String b0 = bucks2.get(0).prefixStr();
			addBucket(b0+"00", bucks);
			addBucket(b0+"01", bucks);
			addBucket(b0+"1", bucks);
			
			for (int i=1; i<bucks2.size(); ++i) {
				String bb = bucks2.get(i).prefixStr();
				addBucket(bb+"0", bucks);
				addBucket(bb+"1", bucks);
			} 
			/*
			for (int q=0; q<bucks.size(); ++q) {
				D(bucks.get(q).prefixStr());
			}
			System.out.println();
			*/
		}
		
		buckets = bucks.toArray(new Bucket[0]);
		bucks = null;
		sortBuckets();
	}
	
	public void initg() {
	
		// k segments
		k=4;
		int nbit=2;
		//
		nbit=3;
		for (int i=1; i<k; ++i) {
			for (int bit=0; bit<nbit; ++bit) {
				// 
				Bucket bu = new Bucket(null, 0);
				
			}
			nbit *= 2;
		}
	}

	public double count() {
		double estnum=0;
		double estden=0;
		
		int bitlen = buckets[0].prefixlen;
		int zero = 0;
		int observed = 0;
		for (int i=0; i<buckets.length; ++i) {
			if (buckets[i].prefixlen == bitlen) {
				++observed;
				if (!buckets[i].isset)
					++zero;
			} else {
				double est = 0;
				if (zero>0) {
					double tot = Math.pow(2, bitlen);
					est = tot*Math.log(observed/(double)zero);
					estnum += est*(observed/(double)tot);
					estden += (observed/(double)tot);
				}
				D("bitsize " + bitlen + " z: "+zero+"/"+observed+"  est: "+est);
				bitlen = buckets[i].prefixlen;
				zero = 0;
				observed = 0;
				--i;
			}
		}
		double est = 0;
		if (zero>0) {
			double tot = Math.pow(2, bitlen);
			est = tot*Math.log(observed/(double)zero);
			estnum += est*(observed/(double)tot);
			estden += (observed/(double)tot);
		}
		
		D("bitsize " + bitlen + " z: "+zero+"/"+observed+"  est: "+est);
		D("est: " + estnum + " / " + estden);
		return estnum/estden;
	}
}
