package sfe.js;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;

import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.serialize.*;

import sfe.crypto.Domain;
import sfe.util.ByteCountOutputStreamSFE;

public class ProtocolComm {
	public static class Party {
		Scriptable scope;
		
		Socket other;
		ScriptableInputStream in;
		ScriptableOutputStream out;
		long startTime;
		ByteCountOutputStreamSFE byteCount;
		
		Party(Scriptable scope) {
			this.scope = scope;
		}
		public void send(Object obj) throws IOException {
			out.writeObject(obj);
			out.flush();
		}
		public Object recv() throws Exception {
			return in.readObject();
		}
	}
	public static class Connector extends Party {
		/*
		Domain domain = new Domain(-20, 20,
	               java.math.BigInteger.valueOf(3).pow(32),
	               BigInteger.valueOf(500));
		*/
		
		Connector(Scriptable scope, String to, int port) throws IOException {
			super(scope);
			other = new Socket(to, port);
			
			startTime = System.currentTimeMillis();
			byteCount = 
				new ByteCountOutputStreamSFE(
						other.getOutputStream());
			out = new ScriptableOutputStream(byteCount, scope);
			out.flush();
			in = new ScriptableInputStream
			(new BufferedInputStream
					(other.getInputStream()), scope);
			
			
			
		}
	}
	
	public static class Listener extends Party {
		ServerSocket listen;
		
		Listener(Scriptable scope, int port) throws IOException {
			super(scope);
			listen = new ServerSocket(port);
			other = listen.accept();
			startTime = System.currentTimeMillis();
			byteCount = 
				new ByteCountOutputStreamSFE(
						other.getOutputStream());
			out = new ScriptableOutputStream(byteCount, scope);
			out.flush();
			in = new ScriptableInputStream
			(new BufferedInputStream
					(other.getInputStream()), scope);		
		}
	}
}
