package sfe.sfauth;

import java.io.IOException;
import java.io.InputStream;

public class DropbearAuthInputStream extends InputStream {

	DropbearAuthStreams streams;
	public DropbearAuthInputStream(DropbearAuthStreams str) {
		this.streams = str;
	}

	public int read() throws IOException {
		byte[] buf = new byte[1];
		read(buf);
		return buf[0];
	}

	public int read(byte b[]) throws IOException {
		synchronized (streams) {
			try {
				while (streams.inputbytes == null && !streams.failure_flag) {
					DropbearAuthStreams.Dio("wait to read");
					streams.waiting = true;
					streams.notifyAll();
					streams.wait();
				}
				if (streams.failure_flag) {
					throw new IOException("protocol aborted");
				}
				streams.waiting = false;
				
				int off = streams.inputbytes.length - streams.inputbytesunread;
				int len = Math.min(streams.inputbytesunread, b.length);
				System.arraycopy(streams.inputbytes, off, b, 0, len);
				streams.inputbytesunread -= len;
				if (streams.inputbytesunread == 0) 
					streams.inputbytes = null;

				//System.err.println("returning read "+len+" bytes");
				return len;

			} catch (InterruptedException e) {
				e.printStackTrace(System.err);
				throw new IOException(e);
			}
		}
	}

	public int read(byte b[], int off, int len) throws IOException {
		byte[] buf = new byte[len];
		int ret = read(buf);
		System.arraycopy(buf, 0, b, off, ret);
		return ret;
	}

}
