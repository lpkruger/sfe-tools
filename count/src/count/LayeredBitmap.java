package count;

import java.util.*;

public class LayeredBitmap implements Counter {
	ArrayList<BitMap> epochs = new ArrayList<BitMap>();
	{
		epochs.add(new VirtualBitmap());
	}
	
	public double count() {
		// estimator
		return 0;
	}

	public boolean set(byte[] hash) {
		return epochs.get(epochs.size()-1).set(hash);
	}
	

	public void newEpoch() {
		epochs.add(new VirtualBitmap());
	}
}
