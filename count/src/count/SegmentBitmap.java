package count;

import java.util.ArrayList;

public class SegmentBitmap extends BitMap {

	//int k;
	
	String myPrefix;
	
	static void addBucket(String s, ArrayList<Bucket> bucks) { // "01010.."
		bucks.add(createBucket(s));
	}
	

	public void init() {
		init(8, "");
		
		//resid = 1;  // 1 ^ c where c is epoch
		
	
	}
	public void init(int k, String prefix) {
		myPrefix = prefix;
		nhat = 0;
		resid = Math.pow(0.5, myPrefix.length());
		
		DEBUG=false;
		ArrayList<Bucket> bucks = new ArrayList<Bucket>();
		// 000 001  01  10  11
		// 00000 00001  0001 0010 0011  010 011  100 101  110 111 

		Bucket b;
		
		addBucket(prefix+"0", bucks);
		addBucket(prefix+"1", bucks);
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
		
		//System.out.println("SegmentBitmap: k="+k+"  ->  "+buckets.length+" buckets");
	}
	
	/*
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
	*/
	
	double resid;
	double nhat;
	
	boolean incremental;
	
	public void clear() {
		super.clear();
		nhat = 0;
		resid = 1;
		resid = Math.pow(0.5, myPrefix.length());
	}
	
	public void set(byte[] hash) {
		Bucket buck = set0(hash);
		// update martingale estimator
		if (buck==null)
			return;
		
		int i = buck.prefixlen;
		nhat += 1.0 / resid;
		resid -= Math.pow(0.5, i);
		//System.out.println("nhat = " + nhat + "  resid = " + resid + "  i = " + i);
	}
	
	public double count() {
		if (incremental)
			return countinc();
		else
			return countone();
	}
	
	public double countinc() {
		//System.out.println("nhat = " + nhat);
		//System.exit(0);
		return nhat;
	}
	public double countone() {
		//Avg esttotal = new Avg();
		
		double estsum=0;
		double estobserved=0;
		int bitlen = buckets[0].prefixlen;
		int zero = 0;
		int observed = 0;
		for (int i=0; i<buckets.length; ++i) {
			if (!buckets[i].ismasked()) {
				estobserved += Math.pow(0.5, buckets[i].prefixlen);
				if (buckets[i].prefixlen == bitlen) {
					++observed;
					if (!buckets[i].isset)
						++zero;
				} else {
					double est = 0;
					if (zero>0) {
						double tot = Math.pow(2, bitlen);
						
						//est = tot*Harmonic.valuebz(observed, zero);
						//esttotal.add(est, observed);
						
						est = observed*Harmonic.valuebz(observed, zero);
						estsum += est;
					}
					//System.out.println("bitsize " + bitlen + " z: "+zero+"/"+observed+"  est: "+est);
					bitlen = buckets[i].prefixlen;
					zero = 0;
					observed = 0;
					--i;
				}
			}
		}
		double est = 0;
		if (zero>0) {
			double tot = Math.pow(2, bitlen);
			//est = tot*Harmonic.valuebz(observed, zero);
			//esttotal.add(est, observed);
			
			est = observed*Harmonic.valuebz(observed, zero);
			estsum += est;
		}
		
		//System.out.println("bitsize " + bitlen + " z: "+zero+"/"+observed+"  est: "+est);
		//D("est: " + estnum + " / " + estden);
		
		//return esttotal.avg();
		return estsum/estobserved;
	}

	public double count0() {
		double estnum=0;
		double estden=0;
		
		int bitlen = buckets[0].prefixlen;
		int zero = 0;
		int observed = 0;
		for (int i=0; i<buckets.length; ++i) {
			if (!buckets[i].ismasked()) {
				if (buckets[i].prefixlen == bitlen) {
					++observed;
					if (!buckets[i].isset)
						++zero;
				} else {
					double est = 0;
					if (zero>0) {
						double tot = Math.pow(2, bitlen);
						//est = tot*Math.log(observed/(double)zero);
						est = tot*Harmonic.valuebz(observed, zero);
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
