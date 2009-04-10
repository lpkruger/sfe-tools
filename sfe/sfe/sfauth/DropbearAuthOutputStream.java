package sfe.sfauth;

import java.io.*;

public class DropbearAuthOutputStream extends ByteArrayOutputStream {
	DropbearAuthStreams streams;
    boolean closed;
    
	public DropbearAuthOutputStream(DropbearAuthStreams str) {
		streams = str;
	}

    public void close() throws IOException {
    	DropbearAuthStreams.Dio("output stream closing");
    	super.close();
    	synchronized (streams) {
    		closed = true;
    		System.out.println("notifying...");
			streams.notifyAll();
		}
    }

}
