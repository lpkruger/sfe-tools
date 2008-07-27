package count;

import java.util.*;

public abstract class BitMap implements Counter {
	protected static boolean DEBUG=true;
	protected void D(Object obj) {
		if (DEBUG)
			System.out.println(obj);
	}
	
	static class Bucket {
		byte[] prefix;
		int prefixlen; // in bits
		boolean isset;
		
		Bucket(byte[] prefix, int len) {
			this.prefix = prefix;
			this.prefixlen = len;
			this.isset = false;
		}
		
		public boolean set(byte[] hash) {	
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
			
			isset=true;
			return true;
		}
		
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
	
	public int countBits() {
		int count=0;
		for (int i=0; i<buckets.length; ++i) {
			if (buckets[i].isset)
				++count;
		}
		return count;
	}
	public boolean set(byte[] hash) {
		for (int i=0; i<buckets.length; ++i) {
			if ( buckets[i].set(hash) ) {
				return true;
			}
		}
		return false;
	}
	public abstract double count();
	
	/*
	6:
	11111100
	= 11111111 ^ 2^3-1
	*/
	
}
