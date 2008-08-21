package count;

public interface Counter {
	void set(byte[] hash);
	double count();
	
	void clear();  // reset for reuse
	
	void printme();
	
	boolean isNewEpoch();
	
	int capacity();
}
