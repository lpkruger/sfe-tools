package count;

import java.util.*;

import count.BitMap.Bucket;

public class LayerSegmentBitmap implements Counter {
	ArrayList<SegmentBitmap> epochs;
	int epochnum=0;
	int k=5;
	//int qmax = 16;
	
	String prefix="";
	
	boolean isnewepoch = false;
	
	void init() {
		epochs = new ArrayList<SegmentBitmap>();
		SegmentBitmap b0 = new SegmentBitmap();
		prefix="";
		epochnum=0;
		b0.init(k, "");
		epochs.add(b0);
	}
	
	public double count() {
		double sum = 0;
		for (int e=0; e<=epochnum; ++e) {
			BitMap bb = epochs.get(e);
			
			double est = bb.count();
			if (!Double.isNaN(est))
				sum += est;
			
			//System.out.println("epoch " + e + " :  " + est);
			
			
		}
		return sum;
	}

	public void set(byte[] hash) {
		isnewepoch = false;
		SegmentBitmap bb = epochs.get(epochnum); 
		Bucket oldbuck = bb.set0(hash);
		if (oldbuck != null) {
			int z=0;
			for (int i=0; i<bb.buckets.length; ++i) {
				if (!bb.buckets[i].isset)
					++z;
			}
			for (int e=0; e<epochnum; ++e) {
				// mask older epochs
				epochs.get(e).mask(hash, oldbuck);
			}
			if (z < bb.buckets.length/2)
				newEpoch();
		}
	}

	public void newEpoch() {
		isnewepoch = true;
		SegmentBitmap bold = epochs.get(epochnum);
		SegmentBitmap bnew;
		prefix += "00";
		if (++epochnum == epochs.size()) {
			bnew = new SegmentBitmap();
			bnew.init(k, prefix);
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
		prefix="";
		for (BitMap bb : epochs) {
			bb.clear();
		}
	}
	
	public void printme() {
		for (int i=0; i<=epochnum; ++i) {
			epochs.get(i).printme();
		}
	}
}
