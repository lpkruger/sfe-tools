/**
 * 
 */
package sfe.util;

public class NullOutputStream extends java.io.OutputStream {
	public void write(int x) { }
	public void write(byte[] b) { }
    public void write(byte[] b, int off, int len) { } 
}