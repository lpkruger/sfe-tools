package sfe.sfauth;

import java.security.SecureRandom;

public abstract class DropbearAuthStreams implements Runnable {
	boolean failure_flag;

	static final boolean useMD5 = true;
	static final boolean use_R = true;
	static final boolean useSeperateSocket = false;
	static final int num_circuits = 2;
	
	final static void D(Object s) {
		System.err.println(s);
	}
	
	final static void Dio(Object s) {
		//System.err.println(s);
	}
	
	// there will be 2 threads: One is Pure Java and executes the protocol,
	// the other is called from native code and goes back and forth
	protected DropbearAuthInputStream authIn;
	protected DropbearAuthOutputStream authOut;
	protected Thread protocolThread;
	
	//byte[] outputbytes;
	byte[] inputbytes;
	int inputbytesunread;
	boolean waiting;
	SecureRandom random = new SecureRandom(new byte[] { 0 });
	

	DropbearAuthStreams() {
		authIn = new DropbearAuthInputStream(this);
		authOut = new DropbearAuthOutputStream(this);
	}
	
	public synchronized void startProtoThread() {
		protocolThread = new Thread(this);
		protocolThread.start();
		if (!waiting)
			waitForOutput();		
	}
	// entry point from Native - packet received
	public synchronized void receivePacket(byte[] pkt) {
		Dio("receiving packet len "+pkt.length);

		if (inputbytes == null) {
			inputbytes = pkt;
			inputbytesunread = pkt.length;
		} else {
			byte[] oldbytes = inputbytes;
			inputbytes = new byte[inputbytesunread+pkt.length];
			System.arraycopy(oldbytes, oldbytes.length - inputbytesunread - 1,
					inputbytes, 0, inputbytesunread);
			System.arraycopy(pkt, 0, inputbytes, inputbytesunread, pkt.length);
			inputbytesunread += pkt.length;
		}
		waiting = false;
		notifyAll();
		waitForOutput();
	}
		
	@SuppressWarnings("deprecation")
	synchronized void waitForOutput() {
		try {
			while(true) {
				Dio("looping...");
				while (authOut.size() == 0 && !waiting && !authOut.closed && !failure_flag) {
					Dio("Thread sleeping...");
					wait();
				}
				if (failure_flag) {
					System.err.println("protocol aborted");
					return;
				}
				if (authOut.size() > 0) {
					byte[] outputbytes = authOut.toByteArray();
					Dio("Write a packet ("+outputbytes.length+")");
					writePacket(outputbytes);
					authOut.reset();
					Dio("Wrote a packet");
				}
				if (waiting || authOut.closed) {
					if(waiting)
						Dio("Waiting for input");
					if (authOut.closed) {
						authOut = null;
						if (!waiting)
							authIn = null;
					}
					if (authOut == null && authIn == null)
						protocolThread.stop();
					Dio("returning to native");
					return;
				}

			}
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
	
    private native boolean writePacket(byte[] b);

    public abstract void go() throws Exception;
    public abstract void start();
    
    public void run() {
		try {
			Dio("Thread starting...");
			go();
		} catch (Exception ex) {
			ex.printStackTrace(System.err);
			synchronized (this) {
				failure_flag = true;
				notifyAll();
			}
		}
		// start Alice or Bob
	}	
	
}
