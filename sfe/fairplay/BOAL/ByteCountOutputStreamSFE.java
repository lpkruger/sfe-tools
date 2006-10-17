package fairplay.BOAL;
import java.io.*;

public class ByteCountOutputStreamSFE extends OutputStream {
    int cnt = 0;
    OutputStream os;
    
    int[] phasecnt = new int[4];

    ByteCountOutputStreamSFE(OutputStream os) {
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
	++phasecnt[MyUtil.WRITE_MODE];
    }
    public void write(byte[] b) throws IOException {
	os.write(b);
	cnt += b.length;
	phasecnt[MyUtil.WRITE_MODE] += b.length;
        }
    public void write(byte[] b, int x, int y) throws IOException {
	os.write(b, x, y);
	cnt += y;
	phasecnt[MyUtil.WRITE_MODE] += y;
    }

    public void printStats() {
        for (int i=0; i<phasecnt.length; ++i) {
	    System.out.println("Phase " + i + "  " + phasecnt[i] + " bytes");
	}
    }
}
