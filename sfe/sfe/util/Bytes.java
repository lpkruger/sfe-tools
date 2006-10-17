/**
 * 
 */
package sfe.util;

import java.io.Serializable;

public class Bytes implements Serializable, Comparable<Bytes> {
	public byte[] bytes;
	
	public Bytes(byte[] b) {
		bytes = b;
	}
	public int hashCode() {
		int sum=0;
		for (int i=0; i<bytes.length; ++i)
			sum = (sum*13) + bytes[i];
		return sum;
	}
	
	public boolean equals(Object obj) {
		if (!(obj instanceof Bytes))
			return false;
		if (bytes.length != ((Bytes) obj).bytes.length)
			return false;
		for (int i=0; i<bytes.length; ++i) {
			if (bytes[i] != ((Bytes) obj).bytes[i])
				return false;                              
		}
		return true;
	}
	
	public int compareTo(Bytes obj) {
		if (bytes.length < obj.bytes.length)
			return -1;
		if (bytes.length > obj.bytes.length)
			return 1;
		
		for (int i=0; i<bytes.length; ++i) {
			if (bytes[i] < obj.bytes[i])
				return -1;                              
			if (bytes[i] < obj.bytes[i])
				return 1;
		}
		return 0;
		
	}
	
}