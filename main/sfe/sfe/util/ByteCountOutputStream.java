package sfe.util;

import java.io.*;

public class ByteCountOutputStream extends OutputStream {
	public int cnt = 0;
	OutputStream os;
	
	public ByteCountOutputStream(OutputStream os) {
		this.os = os;
	}
	public void close() throws IOException {
		os.close();
	}
	public void flush() throws IOException {
		os.flush();
	}
	public void write(int c) throws IOException {
		os.write(c);
		++cnt;
	}
	public void write(byte[] b) throws IOException {
		os.write(b);
		cnt += b.length;
	}
	public void write(byte[] b, int x, int y) throws IOException {
		os.write(b, x, y);
		cnt += y;
	}
	
	public int getCount() {
		return cnt;
	}
}
