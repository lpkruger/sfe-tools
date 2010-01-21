package sfe.sfauth;

import java.security.MessageDigest;

import sfe.sfdl.*;
import sfe.shdl.*;
import sfe.shdl.Circuit.Gate;
import sfe.shdl.Circuit.GateBase;
import sfe.util.*;

public class SHA2 {
	
	Circuit.GateBase[] right_rotate(Circuit.GateBase[] in, int n) {
		// logical right rotate, but we go left because bits are stored backwards
		int len = in.length;
		Circuit.GateBase[] out = new Circuit.GateBase[len];
		System.arraycopy(in, n, out, 0, len-n);
		System.arraycopy(in, 0, out, len-n, n);
		return out;
	}
	Circuit.GateBase[] right_shift(Circuit.GateBase[] in, int n) {
		// logical right shift, but we go left because bits are stored backwards
		int len = in.length;
		Circuit.GateBase[] out = new Circuit.GateBase[len];
		System.arraycopy(in, n, out, 0, len-n);
		for (int i=len-n; i<len; ++i) {
			out[i] = comp.newGate();
			((Circuit.Gate)out[i]).arity = 0;
			((Circuit.Gate)out[i]).inputs = new GateBase[0];
			((Circuit.Gate)out[i]).truthtab = new boolean[1];
		}
		return out;
	}
	
	Circuit.GateBase[] left_rotate(Circuit.GateBase[] in, int n) {
		// left rot, but we store bits backwards so need a right rot instead
		int len = in.length;
		Circuit.GateBase[] out = new Circuit.GateBase[len];
		System.arraycopy(in, 0, out, n, len-n);
		System.arraycopy(in, len-n, out, 0, n);
		return out;
	}
	
	Circuit.Gate[] apply3(Circuit.GateBase[] x, Circuit.GateBase[] y, Circuit.GateBase[] z, boolean[] tt, String name) {
		int len = x.length;
		if (len != y.length || len != z.length) {
			throw new RuntimeException("wrong lengths");
		}
		Circuit.Gate[] out = new Circuit.Gate[len];
		for (int j=0; j<len; ++j) {
			out[j] = comp.newGate();
			out[j].setComment(name + " $ "+ j);
			out[j].arity = 3;
			out[j].inputs = new Circuit.GateBase[] {x[j], y[j], z[j]};
			out[j].truthtab = tt.clone();
		}
		return out;
	}
	
	static final long[] Kconst = { 
		   0x428a2f98, 0x71374491, 0xb5c0fbcf, 0xe9b5dba5, 0x3956c25b, 0x59f111f1, 0x923f82a4, 0xab1c5ed5,
		   0xd807aa98, 0x12835b01, 0x243185be, 0x550c7dc3, 0x72be5d74, 0x80deb1fe, 0x9bdc06a7, 0xc19bf174,
		   0xe49b69c1, 0xefbe4786, 0x0fc19dc6, 0x240ca1cc, 0x2de92c6f, 0x4a7484aa, 0x5cb0a9dc, 0x76f988da,
		   0x983e5152, 0xa831c66d, 0xb00327c8, 0xbf597fc7, 0xc6e00bf3, 0xd5a79147, 0x06ca6351, 0x14292967,
		   0x27b70a85, 0x2e1b2138, 0x4d2c6dfc, 0x53380d13, 0x650a7354, 0x766a0abb, 0x81c2c92e, 0x92722c85,
		   0xa2bfe8a1, 0xa81a664b, 0xc24b8b70, 0xc76c51a3, 0xd192e819, 0xd6990624, 0xf40e3585, 0x106aa070,
		   0x19a4c116, 0x1e376c08, 0x2748774c, 0x34b0bcb5, 0x391c0cb3, 0x4ed8aa4a, 0x5b9cca4f, 0x682e6ff3,
		   0x748f82ee, 0x78a5636f, 0x84c87814, 0x8cc70208, 0x90befffa, 0xa4506ceb, 0xbef9a3f7, 0xc67178f2
		   };
	
	boolean[] Xor_tt = new boolean[8];
	boolean[] Maj_tt = new boolean[8];
	boolean[] Ch_tt = new boolean[8];
	
	{
		for (int i=0; i<8; ++i) {
			boolean x=(0!=(4&i));
			boolean y=(0!=(2&i));
			boolean z=(0!=(1&i));
			
			Xor_tt[i] = x ^ y ^ z;
		    Maj_tt[i] = (x&y) ^ (x&z) ^ (y&z);
		    Ch_tt[i] = (x & y) ^ ((!x) & z);
		}
	}
	//int id;
	
//	int[] r = {7, 12, 17, 22,  7, 12, 17, 22,  7, 12, 17, 22,  7, 12, 17, 22,
//			5,  9, 14, 20,  5,  9, 14, 20,  5,  9, 14, 20,  5,  9, 14, 20,
//			4, 11, 16, 23,  4, 11, 16, 23,  4, 11, 16, 23,  4, 11, 16, 23,
//			6, 10, 15, 21,  6, 10, 15, 21,  6, 10, 15, 21,  6, 10, 15, 21};
	
	long[] k = new long[64];
	{
		for (int i=0; i<64; ++i) {
			k[i] = Kconst[i];
			//System.out.printf("%08x\n", k[i]);
		}
	}
	
	public CircuitCompiler comp;
	
	Circuit.GateBase[] debug_output = null;	// for debugging
	
	void dbg_out(Circuit.GateBase[] a, Circuit.GateBase[] b,
			Circuit.GateBase[] c, Circuit.GateBase[] d,
			Circuit.GateBase[] e, Circuit.GateBase[] f,
			Circuit.GateBase[] g, Circuit.GateBase[] h)
	{
		debug_output = new Circuit.GateBase[256];
		System.arraycopy(h,0,debug_output,0,32);
		System.arraycopy(g,0,debug_output,32,32);
		System.arraycopy(f,0,debug_output,64,32);
		System.arraycopy(e,0,debug_output,96,32);
		System.arraycopy(d,0,debug_output,128,32);
		System.arraycopy(c,0,debug_output,160,32);
		System.arraycopy(b,0,debug_output,192,32);
		System.arraycopy(a,0,debug_output,224,32);
	}
	
	Circuit.Gate[] h0;
	Circuit.Gate[] h1;
	Circuit.Gate[] h2;
	Circuit.Gate[] h3;
	Circuit.Gate[] h4;
	Circuit.Gate[] h5;
	Circuit.Gate[] h6;
	Circuit.Gate[] h7;
	
	Circuit cc;
	
	void initialize(int numinputs) {
		comp = new CircuitCompiler();

		cc = new Circuit();
		cc.inputs = new Circuit.Input[numinputs];
		for (int i=0; i<cc.inputs.length; ++i) {
			int id = comp.newId();
			cc.inputs[i] = new Circuit.Input(id,i);
			cc.inputs[i].setComment("input.x$"+i);
		}
		
		cc.outputs  = new Circuit.Output[256];
		

		h0 = new Circuit.Gate[32];
		h1 = new Circuit.Gate[32];
		h2 = new Circuit.Gate[32];
		h3 = new Circuit.Gate[32];
		h4 = new Circuit.Gate[32];
		h5 = new Circuit.Gate[32];
		h6 = new Circuit.Gate[32];
		h7 = new Circuit.Gate[32];
		
		comp.const2Gates(h0, 0x6a09e667);
		comp.const2Gates(h1, 0xbb67ae85);
		comp.const2Gates(h2, 0x3c6ef372);
		comp.const2Gates(h3, 0xa54ff53a);
		comp.const2Gates(h4, 0x510e527f);
		comp.const2Gates(h5, 0x9b05688c);
		comp.const2Gates(h6, 0x1f83d9ab);
		comp.const2Gates(h7, 0x5be0cd19);
	}
	
	Circuit generateWithPrivEq(boolean include_R) {
		int r_bits = include_R ? 128 : 0;
		
		initialize(512 + r_bits + 128);
		for (int i=0; i<512; ++i) {
			cc.inputs[i].setComment("input.alice.x$"+i);
		}
		if (include_R) {
			for (int i=0; i<r_bits; ++i) {
				cc.inputs[512+i].setComment("input.alice.r$"+i);
			}
		}
		for (int i=0; i<128; ++i) {
			cc.inputs[512+r_bits+i].setComment("input.bob.y$"+i);
		}
		update(0);
		
		Circuit.GateBase[] lc = new Circuit.GateBase[128];
		System.arraycopy(h0, 0, lc, 0, 32);
		System.arraycopy(h1, 0, lc, 32, 32);
		System.arraycopy(h2, 0, lc, 64, 32);
		System.arraycopy(h3, 0, lc, 96, 32);
		//bit_reverse(lc);
		
		Circuit.GateBase[] rc = new Circuit.GateBase[128];
		System.arraycopy(cc.inputs, 512+r_bits, rc, 0, 128);
		
		Gate eq = comp.createEqTest(128, lc, rc);
		
		if (!include_R) {
			cc.outputs = new Circuit.Output[] { new Circuit.Output(eq) };
		} else {
			cc.outputs = new Circuit.Output[r_bits];
			for (int i=0; i<r_bits; ++i) {
				cc.outputs[i] = new Circuit.Output(comp.newGate(eq, cc.inputs[512+i], comp.TT_AND()));
			}
		}
		Optimizer opt = new Optimizer();
		opt.optimize(cc);
		opt.renumber(cc);
		return cc;
	}
	Circuit generate() {
		initialize(512);
		update(0);
		return digest();
	}
	
	Circuit digest() {
//		Circuit.GateBase[] a = h0.clone();
//		Circuit.GateBase[] b = h1.clone();
//		Circuit.GateBase[] c = h2.clone();
//		Circuit.GateBase[] d = h3.clone();
//		Circuit.GateBase[] e = h4.clone();
//		Circuit.GateBase[] f = h5.clone();
//		Circuit.GateBase[] g = h6.clone();
//		Circuit.GateBase[] h = h7.clone();
		
		int stride = 32;
		int offset = 0;

		Circuit.GateBase[][] n = { h0, h1, h2, h3, h4, h5, h6, h7 };
		for (int ch=0; ch<n.length; ++ch) {
			for (int i=0; i<stride; ++i) {
				cc.outputs[offset+i] = new Circuit.Output((Circuit.Gate) n[ch][stride-i-1]);
				cc.outputs[offset+i].setComment("output.md$"+(offset+i));
			}
			offset += stride;
		}
		
		if (debug_output != null) {
			cc.outputs = new Circuit.Output[debug_output.length];
			for (int i=0; i<debug_output.length; ++i)
			cc.outputs[i] = new Circuit.Output((Circuit.Gate)debug_output[i]);
		}
		
		Optimizer opt = new Optimizer();
		opt.optimize(cc);
		opt.renumber(cc);
		return cc;	
	}
	void update(int input_offset) {
		//comp = new CircuitCompiler();

		Circuit.GateBase[] a = h0;
		Circuit.GateBase[] b = h1;
		Circuit.GateBase[] c = h2;
		Circuit.GateBase[] d = h3;
		Circuit.GateBase[] e = h4;
		Circuit.GateBase[] f = h5;
		Circuit.GateBase[] g = h6;
		Circuit.GateBase[] h = h7;

		//Extend the sixteen 32-bit words into sixty-four 32-bit words:
		Circuit.GateBase[][] ww = new Circuit.GateBase[64][];
		for (int i=0; i<16; ++i) {
			ww[i] = new Circuit.GateBase[32];
			System.arraycopy(cc.inputs, input_offset+32*i, ww[i], 0, 32);
			BitUtils.bit_reverse(ww[i]);
		}
		for (int i=16; i<64; ++i) {
			Circuit.Gate[] s0 = apply3(
					right_rotate(ww[i-15], 7),
					right_rotate(ww[i-15], 18),
					right_shift(ww[i-15], 3),
					Xor_tt, "ww_s0_"+i);
			Circuit.Gate[] s1 = apply3(
					right_rotate(ww[i-2], 17),
					right_rotate(ww[i-2], 19),
					right_shift(ww[i-2], 10),
					Xor_tt, "s1_"+i);
			Circuit.Gate[] tmp1 = createAdder(ww[i-16], s0, "w-16+s0 "+i);
			Circuit.Gate[] tmp2 = createAdder(ww[i-7], s1, "w-7+s1 "+i);
			ww[i] = createAdder(tmp1, tmp2, "w_"+i);
		}

		for (int i=0; i<64; ++i) {
			Circuit.Gate[] kk = new Circuit.Gate[32];
			comp.const2Gates(kk, k[i]);
			for (int j=0; j<32; ++j) {
				if (kk[j]==null)
					throw new RuntimeException("kk["+j+"] is null");
				//System.out.println(kk[j]);
				if (ww[i][j]==null)
					throw new RuntimeException("ww["+j+"] is null");
				//System.out.println(ww[j]);
			}

			Circuit.Gate[] s0 = apply3(
					right_rotate(a, 2),
					right_rotate(a, 13),
					right_rotate(a, 22),
					Xor_tt, "s0_"+i);
			Circuit.Gate[] maj = apply3(a, b, c, Maj_tt, "maj_"+i);
			Circuit.Gate[] t2 = createAdder(s0, maj, "t2_"+i);
			Circuit.Gate[] s1 = apply3(
					right_rotate(e, 6),
					right_rotate(e, 11),
					right_rotate(e, 25),
					Xor_tt, "s1_"+i);
			Circuit.Gate[] ch = apply3(e, f, g, Ch_tt, "ch_"+i);
			Circuit.Gate[] hPs1 = createAdder(h, s1, "h+s1 "+i);
			Circuit.Gate[] chPwi = createAdder(ch, ww[i], "ch+w["+i+"]");
			Circuit.Gate[] t1Mk = createAdder(hPs1, chPwi, "h+s1+ch+w["+i+"]");
		
			Circuit.Gate[] t1 = createAdder(t1Mk, kk, "t1_"+i);

			h = g;
			g = f;
			f = e;
			e = createAdder(d, t1, "e_"+i);
			d = c;
			c = b;
			b = a;
			a = createAdder(t1, t2, "a_"+i);
		}

		/*
		Circuit.Gate[] www = new Circuit.Gate[32];
		for (int j=0; j<32; ++j) {
			www[j] = comp.newIdentityGate(ww[j]);
		}*/
	/// debug ///
	//if (i==60) debug_output = dbg_out(b,ff,www,kk);

		
		h0 = comp.createAdder(a, h0);
		h1 = comp.createAdder(b, h1);
		h2 = comp.createAdder(c, h2);
		h3 = comp.createAdder(d, h3);
		h4 = comp.createAdder(e, h4);
		h5 = comp.createAdder(f, h5);
		h6 = comp.createAdder(g, h6);
		h7 = comp.createAdder(h, h7);
	}

	Circuit.Gate[] createAdder(Circuit.GateBase[] x, Circuit.GateBase[] y, String name) {
		Circuit.Gate[] out = comp.createAdder(x, y);
		for (int j=0; j<out.length; ++j) {
			out[j].setComment(name + " $ " + j);
		}
		return out;
	}
	
	//01234567 89012345 67890123 45678901
	
	public static boolean[] prepare_sha2_input(boolean[] inputs) {
		boolean[] inputs2 = new boolean[512];
		int pad = (511 - inputs.length) + 448;
		if (pad<0) pad+=512;
		pad %= 512;
		System.out.println("input len "+inputs.length+"  pad "+pad);
		System.arraycopy(inputs, 0, inputs2, 0, inputs.length);
		inputs2[inputs.length]=true;
		byte[] len = new byte[8];
		len[4] = (byte) ((inputs.length>>24) & 0xff);
		len[5] = (byte) ((inputs.length>>16) & 0xff);
		len[6] = (byte) ((inputs.length>>8) & 0xff);
		len[7] = (byte) ((inputs.length) & 0xff);
		// should do 4 more...
		System.arraycopy(BitUtils.bytes2bool(len), 0, inputs2, 448, 64);
		return inputs2;
	}
	public static boolean[] compute_sha2(Circuit cc, boolean[] inputs) {
		return compute_sha2(cc, new boolean[][] { inputs });
	}
	public static boolean[] compute_sha2(Circuit cc, boolean[][] inputz) {
		boolean[] inputs3 = new boolean[512*inputz.length];
		for (int i=0; i<inputz.length; ++i) {
			boolean[] inputs = inputz[i];
			boolean[] inputs2 = prepare_sha2_input(inputs);
			
			System.arraycopy(inputs2, 0, inputs3, 512*i, 512);
		}
		//Circuit cc = new MD5().generate();
		//System.out.println(Base64.encodeBytes(bool2bytes(inputs2)));
		//bit_reverse(inputs2);
		boolean[] out = cc.eval(inputs3);
		BitUtils.bit_reverse(out);
		return out;
	}
	
	public static void main(String[] args) throws Exception {
		MessageDigest md = MessageDigest.getInstance("SHA-256");
		byte[] out = md.digest(args[0].getBytes());
		for (int i=0; i<out.length; ++i) {
			System.out.printf("%02x%s",out[i], (((i+1)%4)==0?" ":""));
		}
		System.out.println();
		System.out.println(Base64.encodeBytes(out));
		
		
		Sha256 sha256 = new Sha256();
		sha256.resetContext();
		byte[] padding = Sha256.padBuffer(args[0].getBytes().length);
		byte[] inbytes = new byte[512/8];
		System.arraycopy(args[0].getBytes(), 0, inbytes, 0, args[0].getBytes().length);
		System.arraycopy(padding, 0, inbytes, args[0].getBytes().length, padding.length);
		sha256.transform(inbytes, 0);
		out = sha256.getResult();
		for (int i=0; i<out.length; ++i) {
			System.out.printf("%02x%s",out[i], (((i+1)%4)==0?" ":""));
		}
		System.out.println();
		System.out.println(Base64.encodeBytes(out));
		
		
//		MD5Test md5t = new MD5Test();
//		md5t.Update(args[0].getBytes());
//		System.out.println(Base64.encodeBytes(md5t.Final()));
		Circuit cc = new SHA2().generate();
		boolean[][] inputs = { BitUtils.bytes2bool(args[0].getBytes()) };
		out = BitUtils.bool2bytes(compute_sha2(cc, inputs));
		for (int i=0; i<out.length; ++i) {
			System.out.printf("%02x%s",out[i], (((i+1)%4)==0?" ":""));
		}
		System.out.println();
		System.out.println(Base64.encodeBytes(out));
		//CircuitWriter.write(cc);
		System.out.println();
	}
}
