/**
 * 
 */
package bdd.proto.singlebit;

import java.io.Serializable;

public class Bytes implements Serializable {
	public byte[] bytes;
	
	public Bytes(byte[] b) {
		bytes = b;
	}
	public int hashCode() {
		int sum=0;
		for (int i=0; i<bytes.length; ++i)
			sum += bytes[i];
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
	
}