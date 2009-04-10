package sfe.sfauth;

import java.io.*;

import sfe.crypto.SFEKey;
import sfe.editdistsw.SWProtoBlock.Bob;
import sfe.shdl.*;
import sfe.util.ByteCountOutputStreamSFE;

import java.io.*;
import java.math.BigInteger;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;

import sfe.util.Base64;

// this class provides an API to invoke SFE from dropbear.
// the interface uses primitive types, mostly byte[] as circuits
// and OT data are passed around as binary blobs through the SSh
// protocol
public class DropbearSfeServer extends DropbearAuthStreams {
	//public final static String dir="/home/louis/sfe/build"
	String passwdcrypt;	// set from native to the string in /etc/shadow
	
	String salt;
	byte[] cryptpw;
	
	boolean done;
	boolean success;
	
	public DropbearSfeServer(String passwdcrypt) {
		System.err.println("In SFE server");
		this.passwdcrypt = passwdcrypt;
	}

	public void start() {
		waiting = true;
		startProtoThread();
	}

	public void go() throws Exception {
		ObjectInputStream in = new ObjectInputStream(authIn);
		ObjectOutputStream out = new ObjectOutputStream(authOut);
		go2(out, in);
		out.close();
	}

	private void go2(ObjectOutputStream out, ObjectInputStream in) throws Exception {
		D("using passwdcrypt "+passwdcrypt);
		int dollar = passwdcrypt.lastIndexOf('$');
		salt = passwdcrypt.substring(3,dollar);
		System.out.println("salt: "+salt);
		cryptpw = MD5pw.fromB64(passwdcrypt.substring(dollar+1));
		System.out.println(cryptpw.length);
		byte[] tmp = new byte[16];
		System.arraycopy(cryptpw, 0, tmp, 0, 16);
		cryptpw = tmp;
		if (false) {
			System.out.print("crpw: ");
			for (int i=0; i<cryptpw.length; ++i) {
				System.out.printf("%02x%s", cryptpw[i], (i+1)%4==0?" ":"");
			}
			System.out.println();
		}
	
		out.writeUTF(salt);
		out.flush();
		
		//FmtFile fmt = FmtFile.readFile("priveq.fmt");
		FmtFile fmt = FmtFile.readFile("/etc/dropbear/md5_pw_cmp.fmt");
		TreeMap<Integer,Boolean> vals = new TreeMap<Integer,Boolean>();
		fmt.mapBits(cryptpw, vals, "input.bob.y");

		boolean[] vv = new boolean[vals.size()];
		int vi=0;
		for (Boolean bb : vals.values()) {
			vv[vi] = bb;
			vi++;
		}
		sfe.shdl.Protocol.Bob cbob = new sfe.shdl.Protocol.Bob(in, out, vv);
		cbob.go();

		BigInteger zz;
		zz = fmt.readBits(cbob.result, "output.bob");
		done = true;
		if (zz.equals(BigInteger.ZERO))
			success = false;
		else
			success = true;
		System.err.println(zz);
	}
	

	
	public static void main(String[] args) throws Exception {
		int port = Integer.parseInt(args[0]);
		String pw = args.length==1 ? "$1$QyenZBsY$w92OuQyOOk02pRUjZTjr20"
				: args[1];

		ServerSocket listen = new ServerSocket(port);
		Socket client_sock = listen.accept();
		long startTime = System.currentTimeMillis();
		ByteCountOutputStreamSFE byteCount = new ByteCountOutputStreamSFE(
				client_sock.getOutputStream());
		ObjectOutputStream out_raw = new ObjectOutputStream(byteCount);
		out_raw.flush();
		ObjectInputStream in_raw = new ObjectInputStream
		(new BufferedInputStream
				(client_sock.getInputStream()));
		
		DropbearSfeServer serv = new DropbearSfeServer(pw);
		//serv.password = pw;
		serv.go2(out_raw, in_raw);

	}
	

}
