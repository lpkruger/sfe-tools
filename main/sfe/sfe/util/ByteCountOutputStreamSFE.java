package sfe.util;

import java.io.*;
//import SFE.BOAL.MyUtil;

public class ByteCountOutputStreamSFE extends OutputStream {
	public static int WRITE_MODE;
	int[] phasecnt = new int[4];
	public final static int MODE_SETUP=0; 
	public final static int MODE_CIRCUIT=1;
	public final static int MODE_OT=2;
	public final static int MODE_EVAL=3;
	
	public int cnt = 0;
	OutputStream os;
	
	
	public ByteCountOutputStreamSFE(OutputStream os) {
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
		++phasecnt[WRITE_MODE];
	}
	public void write(byte[] b) throws IOException {
		os.write(b);
		cnt += b.length;
		phasecnt[WRITE_MODE] += b.length;
	}
	public void write(byte[] b, int x, int y) throws IOException {
		os.write(b, x, y);
		cnt += y;
		phasecnt[WRITE_MODE] += y;
	}
	
	public void printStats() {
		for (int i=0; i<phasecnt.length; ++i) {
			System.out.println("Phase " + i + "  " + phasecnt[i] + " bytes");
		}
	}
	
}
