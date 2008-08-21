package count;

import java.util.*;

public abstract class BitMap implements Counter {
	protected static boolean DEBUG=true;
	protected void D(Object obj) {
		if (DEBUG)
			System.out.println(obj);
	}
	
	public static Bucket createBucket(String s) {
		int bytes = (s.length()+7)/8;

		byte[] bb = new byte[bytes];
		for (int i=0; i<s.length(); ++i) {
			if (s.charAt(i)=='1') {
				int bit = i%8;
				if (bit==0) bit=8;
				bb[i/8] |= 1<<((7-(i%8)));
			}
		}
		return new Bucket(bb, s.length());
	}
	
	static class Bucket {
		final byte[] prefix;
		final int prefixlen; // in bits
		boolean isset;
		boolean isMasked;
		
		
		Bucket(byte[] prefix, int len) {
			this.prefix = prefix;
			this.prefixlen = len;
			this.isset = false;
		}
		
		Bucket copy() {
			Bucket bb = new Bucket(this.prefix, this.prefixlen);
			return bb;
		}
		
		public boolean match(byte[] hash) {	
			for (int i=0; i<prefix.length-1; ++i) {
				if (prefix[i]!=hash[i]) {
					return false;
				}
			}
			
			byte last = hash[prefix.length-1];
			int nbits=(prefixlen%8);
			int mask;
			
			if (nbits==0) 
				mask = 0xff;
			else
				mask = (0xfff-((1<<(8-nbits))-1)) & 0xff;
			//System.out.println("nbits="+nbits+"  mask="+Integer.toBinaryString(mask));
			if ((last&mask) != (prefix[prefix.length-1]&mask)) {
				return false;
			}
			
			return true;
		}
		
		boolean ismasked() {
			return isMasked;
		}
		
		/*
		public boolean set(byte[] hash) {
			if (match(hash)) {
				isset = true;
				return true;
			}
			return false;
		}*/
		
		public String prefixStr() {
			StringBuilder sb = new StringBuilder();
			for (int i=0; i<prefix.length-1; ++i) {
				String ss = Integer.toBinaryString(prefix[i] & 0x0ff);
				while (ss.length() < 8) {
					ss = "0"+ss;
				}
				sb.append(ss);
			}
			int last = prefix[prefix.length-1] & 0x0ff;
			int nbits=(prefixlen%8);
			if (nbits==0) nbits=8;
		    String ss = Integer.toBinaryString(prefix[prefix.length-1] & 0x0ff);
			while (ss.length() < 8) {
				ss = "0"+ss;
			}
			sb.append(ss.substring(0, nbits));
			return sb.toString();
		}
	}
	
	Bucket[] buckets;  // ordered from longest to shortest prefixlen
	
	public void clear() {
		for (int i=0; i<buckets.length; ++i) {
			buckets[i].isset = false;
			buckets[i].isMasked = false;
		}
	}
	
	protected void sortBuckets() {
		Arrays.sort(buckets, new Comparator<Bucket>() {
			public int compare(Bucket o1, Bucket o2) {
				if (o1.prefixlen>o2.prefixlen)
					return -1;
				if (o1.prefixlen<o2.prefixlen)
					return 1;
				return 0;
			}
		
		});
	}
	
	/*
	public double maskedBits() {
		double sum=0;
		for (int i=0; i<buckets.length; ++i) {
			sum += buckets[i].masked;
		}
		return sum;
	}	
	
	public int countMasked() {
		int count=0;
		for (int i=0; i<buckets.length; ++i) {
			if (buckets[i].masked != 0)
				++count;
		}
		return count;
	}
	public int countBits() {
		int count=0;
		for (int i=0; i<buckets.length; ++i) {
			if (buckets[i].isset && buckets[i].ismasked())
				++count;
		}
		return count;
	}
	*/
	
	public void set(byte[] hash) {
		set0(hash);
	}
	public Bucket set0(byte[] hash) {
		for (int i=0; i<buckets.length; ++i) {
			if ( buckets[i].match(hash) ) {
				if (buckets[i].isset)
					return null;
				buckets[i].isset = true;
				return buckets[i];
			}
		}
		return null;
	}

	public void mask(byte[] hash, Bucket oldbuck) {
		int  oldlen = oldbuck.prefixlen;
		//boolean oldmask = oldbuck.isMasked;
		for (int i=0; i<buckets.length; ++i) {
			if ( buckets[i].match(hash) ) {
				/*
				if (buckets[i].prefixlen>oldlen) {
					throw new RuntimeException("no good");
				}
				double mask=1;
				for (int j=oldlen; j<buckets[i].prefixlen; ++j) {
					mask /= 2.0;
				}
				
				buckets[i].masked += mask*(1-oldmask);
				*/
				buckets[i].isMasked = true;
			}
		}
	}
	public abstract double count();
	
	public boolean isNewEpoch() {
		return false;
	}
	
	public int capacity() {
		return buckets.length;
	}
	/*
	6:
	11111100
	= 11111111 ^ 2^3-1
	*/
	
	public void printme() {
		for (int i=0; i<buckets.length; ++i) {
			System.out.print(buckets[i].prefixStr());
			if(buckets[i].isset)
				System.out.print('*');
			if(buckets[i].ismasked()) 
				System.out.print('^');
			System.out.print(' ');
		}
		System.out.println();
	}
}
