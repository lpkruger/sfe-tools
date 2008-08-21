package count;

import java.util.*;

import count.BitMap.Bucket;

public class LayeredBitmap implements Counter {
	ArrayList<VirtualBitmap> epochs;
	int epochnum=0;
	int zmin = 16;
	
	boolean isnewepoch = false;
	
	void init() {
		epochs = new ArrayList<VirtualBitmap>();
		VirtualBitmap b0 = new VirtualBitmap();
		b0.init(32,32);
		epochs.add(b0);
	}
	
	public double count() {
		double sum = 0;
		for (int e=0; e<=epochnum; ++e) {
			VirtualBitmap bb = epochs.get(e);
			/*
			int zero=0;
			int counted=0;
			for (int i=0; i<bb.buckets.length; ++i) {
				if (!bb.buckets[i].ismasked) {
					++counted;
					if (!bb.buckets[i].isset) {
						++zero;
					}
				}
			}
			double est = 0;
			if (zero>0) {
				est = bb.t*Math.log(counted/(double)zero);
			}*/
			double est = bb.count();
			sum += est;
			
			//System.out.println("epoch " + e + " :  " + est);
			
			
		}
		return sum;
	}

	public void set(byte[] hash) {
		isnewepoch = false;
		VirtualBitmap bb = epochs.get(epochnum); 
		Bucket buck = bb.set0(hash);
		if (buck != null) {
			int z=0;
			for (int i=0; i<bb.buckets.length; ++i) {
				if (!bb.buckets[i].isset)
					++z;
			}

			for (int e=0; e<epochnum; ++e) {
				// mask older epochs
				epochs.get(e).mask(hash, buck);
			}
			if (z<zmin) {
				newEpoch();
			}
		}
	}
	

	public void newEpoch() {
		isnewepoch = true;
		VirtualBitmap bold = epochs.get(epochnum);
		VirtualBitmap bnew;
		if (++epochnum == epochs.size()) {
			bnew = new VirtualBitmap();
			bnew.init(bold.t * 2, bold.b);
			epochs.add(bnew);
		} else {
			bnew = epochs.get(epochnum);
		}
	}
	

	public boolean isNewEpoch() {
		return isnewepoch;
	}
	
	public int capacity() {
		return epochs.get(0).buckets.length;
	}
	
	public void clear() {
		epochnum = 0;
		for (VirtualBitmap bb : epochs) {
			bb.clear();
		}
	}
	
	public void printme() {
		for (int i=0; i<=epochnum; ++i) {
			epochs.get(i).printme();
		}
	}
}
