package sfe.sfauth;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import sfe.crypto.SFEKey;
import sfe.editdistsw.SWProtoBlock.Alice;
import sfe.shdl.Circuit;
import sfe.shdl.CircuitCrypt;
import sfe.shdl.CircuitCryptPermute;
import sfe.shdl.CircuitParser;
import sfe.shdl.FmtFile;
import sfe.shdl.CircuitCrypt.AliceData;
import sfe.util.ByteCountObjectOutputStream;
import sfe.util.ByteCountOutputStreamSFE;
import sfe.util.VarDesc;

import java.io.*;
import java.math.BigInteger;
import java.net.Socket;
import java.security.MessageDigest;
import java.util.*;

import sfe.util.Base64;

// this class provides an API to invoke SFE from dropbear.
// the interface uses primitive types, mostly byte[] as circuits
// and OT data are passed around as binary blobs through the SSh
// protocol
public class DropbearSfeClient extends DropbearAuthStreams {
	
	//public final static String dir="/home/louis/sfe/build"
	
	String password;
	public DropbearSfeClient(String password) {
		this.password = password;
	}
	String salt;
	
	//MD5 md5 = new MD5();



	public void start() {
		waiting = false;
		startProtoThread();
	}
	
	public void go() throws Exception {
		ObjectOutputStream out = new ObjectOutputStream(authOut);
		out.flush();
		ObjectInputStream in = new ObjectInputStream(authIn);
		go2(out, in);
		out.close();
	}

	void go2(ObjectOutputStream out, ObjectInputStream in) throws Exception {
		/*
		MD5 md5 = new MD5();
		cc = md5.generate();
		TreeSet<Integer> aliceVars = new TreeSet<Integer>();
		TreeSet<Integer> bobVars = new TreeSet<Integer>();
		*/
		
		salt = in.readUTF();
		
		// for simple eq test:
		//String cryptpwstr = MD5pw.md5_pw(password.getBytes(), salt.getBytes());
		//System.out.println(cryptpwstr);
		//cryptpwstr = cryptpwstr.substring(1 + cryptpwstr.lastIndexOf('$'));
		//byte[] cryptpw = MD5pw.fromB64(cryptpwstr);
		//Circuit cc = CircuitParser.readFile("priveq.circ");
		//FmtFile fmt = FmtFile.readFile("priveq.fmt");
		D("salt = "+salt);
		byte[] cryptpw = MD5pw.md5_pw_999(password.getBytes(), salt.getBytes());
		
		D("pre-hash: " + MD5pw.toB64(cryptpw));
		//D("using password "+password);
		D("prepare circuit");
		try {
			MessageDigest md5 = MessageDigest.getInstance("MD5");
			md5.update(password.getBytes());
			md5.update(password.getBytes());
			md5.update(cryptpw);
			byte[] fin = md5.digest();
			System.out.print("fin: ");
			for (int i=0; i<fin.length; ++i) {
				System.out.printf("%02x%s", fin[i], (i+1)%4==0?" ":"");
			}
			System.out.println();
		} catch (RuntimeException ex) {}
		
		int pwlen = password.getBytes().length;
		byte[] md5in = new byte[pwlen*2 + cryptpw.length];
		System.arraycopy(password.getBytes(), 0, md5in, 0, pwlen);
		System.arraycopy(password.getBytes(), 0, md5in, pwlen, pwlen);
		System.arraycopy(cryptpw, 0, md5in, 2*pwlen, cryptpw.length);
		boolean[] md5inbits = MD5.prepare_md5_input(MD5.bytes2bool(md5in));
		Circuit cc = CircuitParser.readFile("/etc/dropbear/md5_pw_cmp.circ");
		FmtFile fmt = FmtFile.readFile("/etc/dropbear/md5_pw_cmp.fmt");
		VarDesc bdv = fmt.getVarDesc();
		VarDesc aliceVars = bdv.filter("A");
		VarDesc bobVars = bdv.filter("B");
		
		//System.out.println("alice size: " + aliceVars.who.size());
		//System.out.println("bob size: " + bobVars.who.size());
		
		TreeMap<Integer,Boolean> vals = new TreeMap<Integer,Boolean>();
		fmt.mapBits(md5inbits, vals, "input.alice.x");

		D("eval circuit");
		sfe.shdl.Protocol.Alice calice = new sfe.shdl.Protocol.Alice(in, out, cc);
		calice.go(vals, 
				new TreeSet<Integer>(aliceVars.who.keySet()),
				new TreeSet<Integer>(bobVars.who.keySet()));
		
		//System.out.println("Alice circuit wrote " + out.getCount() + " bytes");
		
		// for DEBUG
		//BigInteger bobVal = (BigInteger) in.readObject();
		//BigInteger combined = r0.add(bobVal).and(MAX_BIGINT);
		//System.out.println("result after stage: " + combined);
	}

	public static void main(String[] args) throws Exception {
		String to = args[0];
		int port = Integer.parseInt(args[1]);
		String pw = args[2];
		
		Socket server_sock = new Socket(to, port);
		
		long startTime = System.currentTimeMillis();
		ByteCountOutputStreamSFE byteCount = new ByteCountOutputStreamSFE(
				server_sock.getOutputStream());
		ObjectOutputStream out_raw = new ObjectOutputStream(byteCount);
		out_raw.flush();
		ObjectInputStream in_raw = new ObjectInputStream
		(new BufferedInputStream
				(server_sock.getInputStream()));
		
		DropbearSfeClient cli = new DropbearSfeClient(pw);
		//cli.password = pw;
		cli.go2(out_raw, in_raw);
	}
}
