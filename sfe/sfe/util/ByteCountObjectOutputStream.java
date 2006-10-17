package sfe.util;

import java.io.*;

public class ByteCountObjectOutputStream extends ObjectOutputStream {
	private ByteCountOutputStream bos;
	private ByteCountObjectOutputStream(OutputStream os) throws IOException {
		super(os);
	}
	
	//protected void writeStreamHeader() {
	//}
	
	public int getCount() {
		return bos.getCount();
	}
	
	public static ByteCountObjectOutputStream wrapObjectStream(OutputStream os) throws IOException {
		ByteCountOutputStream bos = new ByteCountOutputStream(os);
		ByteCountObjectOutputStream boos = new ByteCountObjectOutputStream(bos);
		boos.bos = bos;
		return boos;
	}
	
}
