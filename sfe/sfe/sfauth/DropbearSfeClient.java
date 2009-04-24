package sfe.sfauth;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.math.BigInteger;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.MessageDigest;
import java.util.Random;
import java.util.TreeMap;
import java.util.TreeSet;

import sfe.shdl.*;
import sfe.shdl.CircuitCrypt.AliceData;
import sfe.util.*;

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
		Dio("start");
		waiting = false;
		if (useSeperateSocket) {
			waiting = true;
		}
		startProtoThread();
	}
	
	public void go() throws Exception {
		ObjectInputStream in; 
		ObjectOutputStream out;
		Dio("go");
		if (useSeperateSocket) {
			Dio("connect to socket");
			Thread.sleep(200);
			Socket bob = new Socket("localhost", 1236);
			System.out.println(bob);
			out= new ObjectOutputStream(new BufferedOutputStream(bob.getOutputStream()));
			out.flush();
			in = new ObjectInputStream(new BufferedInputStream(bob.getInputStream()));	
		} else {
			out = new ObjectOutputStream(authOut);
			out.flush();
			in = new ObjectInputStream(authIn);
		}
		
		go2(out, in);
		out.close();
	}
	
	void go2(ObjectOutputStream out, ObjectInputStream in) throws Exception {
		// pre-crypt
		System.err.println("pre-crypt circuit");
		Circuit cc;
		FmtFile fmt;
		String rstr = use_R ? "_r" : "";
		
		// for simple eq test:	

		if (!useMD5) {
			cc = CircuitParser.readFile("/etc/dropbear/priveq"+rstr+".circ");
			fmt = FmtFile.readFile("/etc/dropbear/priveq"+rstr+".fmt");
		} else {
			cc = CircuitParser.readFile("/etc/dropbear/md5_pw_cmp"+rstr+".circ");
			fmt = FmtFile.readFile("/etc/dropbear/md5_pw_cmp"+rstr+".fmt");
		}
		
		VarDesc bdv = fmt.getVarDesc();
		VarDesc aliceVars = bdv.filter("A");
		VarDesc bobVars = bdv.filter("B");
		sfe.shdl.Protocol.Alice calice = new sfe.shdl.Protocol.Alice(null, null, cc, random);
		calice.setNumCircuits(num_circuits);

		calice.cryptWithCutChoose(
				new TreeSet<Integer>(aliceVars.who.keySet()), 
				new TreeSet<Integer>(bobVars.who.keySet()));
		long time = System.currentTimeMillis();
		
		/*
		MD5 md5 = new MD5();
		cc = md5.generate();
		TreeSet<Integer> aliceVars = new TreeSet<Integer>();
		TreeSet<Integer> bobVars = new TreeSet<Integer>();
		*/
		
		System.err.println("start protocol");
		salt = in.readUTF();
		D("salt = "+salt);
		
		byte[] cryptpw;
		// for simple eq test:	
		if (!useMD5) {
			String cryptpwstr = MD5pw.md5_pw(password.getBytes(), salt.getBytes());
			System.out.println(cryptpwstr);
			cryptpwstr = cryptpwstr.substring(1 + cryptpwstr.lastIndexOf('$'));
			cryptpw = MD5pw.fromB64(cryptpwstr);
		} else {
			cryptpw = MD5pw.md5_pw_999(password.getBytes(), salt.getBytes());
			D("pre-hash: " + MD5pw.toB64(cryptpw));
		}
		
		//D("using password "+password);
		D("prepare inputs");
		try {
			MessageDigest md5 = MessageDigest.getInstance("MD5");
			md5.update(password.getBytes());
			md5.update(password.getBytes());
			md5.update(cryptpw);
			byte[] fin = md5.digest();
			System.out.print("fin: ");
			//for (int i=0; i<fin.length; ++i) {
				//System.out.printf("%02x%s", fin[i], (i+1)%4==0?" ":"");
			//}
			System.out.println(MD5pw.toB64(fin));
			System.out.println();
		} catch (RuntimeException ex) {}
		
		int pwlen = password.getBytes().length;
		byte[] md5in = new byte[pwlen*2 + cryptpw.length];
		System.arraycopy(password.getBytes(), 0, md5in, 0, pwlen);
		System.arraycopy(password.getBytes(), 0, md5in, pwlen, pwlen);
		System.arraycopy(cryptpw, 0, md5in, 2*pwlen, cryptpw.length);
		boolean[] md5inbits = MD5.prepare_md5_input(BitUtils.bytes2bool(md5in));
		
		//System.out.println("alice size: " + aliceVars.who.size());
		//System.out.println("bob size: " + bobVars.who.size());
		
		TreeMap<Integer,Boolean> vals = new TreeMap<Integer,Boolean>();
		if (!useMD5) {
			fmt.mapBits(BitUtils.bytes2bool(cryptpw), vals, "input.alice.x");
		} else { 
			fmt.mapBits(md5inbits, vals, "input.alice.x");
		}
		if (use_R) {
			BigInteger rval = new BigInteger(128, new Random());
			System.out.println("R = " + rval);
			fmt.mapBits(rval, vals, "input.alice.r");
		}

		D("eval circuit");
		calice.setStreams(in, out);
		calice.onlineWithCutChoose(vals, 
				new TreeSet<Integer>(aliceVars.who.keySet()),
				new TreeSet<Integer>(bobVars.who.keySet()));
		
		long time2 = System.currentTimeMillis();
		System.out.println("Time: " + (time2-time)/1000.0);
		
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
		
		//long startTime = System.currentTimeMillis();
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
