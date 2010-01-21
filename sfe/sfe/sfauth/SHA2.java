package sfe.sfauth;

import java.security.MessageDigest;

import sfe.sfdl.*;
import sfe.shdl.*;
import sfe.shdl.Circuit.Gate;
import sfe.shdl.Circuit.GateBase;
import sfe.util.*;

public class SHA2 {
	int variant;
	int wordsize;
	int blocklen;
	
	public SHA2(int n) {
		variant = n;
		switch(n) {
		case 512:
			wordsize = 64;
			blocklen = 1024;
			break;
		case 256:
			wordsize = 32;
			blocklen = 512;
			break;
		default:
			throw new RuntimeException("unknown SHA2: " + n);
		}
	}
	
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
	
	static final long[] K_512 = {
	 0x428a2f98d728ae22L, 0x7137449123ef65cdL, 0xb5c0fbcfec4d3b2fL, 0xe9b5dba58189dbbcL,
	 0x3956c25bf348b538L, 0x59f111f1b605d019L, 0x923f82a4af194f9bL, 0xab1c5ed5da6d8118L,
	 0xd807aa98a3030242L, 0x12835b0145706fbeL, 0x243185be4ee4b28cL, 0x550c7dc3d5ffb4e2L,
	 0x72be5d74f27b896fL, 0x80deb1fe3b1696b1L, 0x9bdc06a725c71235L, 0xc19bf174cf692694L,
	 0xe49b69c19ef14ad2L, 0xefbe4786384f25e3L, 0x0fc19dc68b8cd5b5L, 0x240ca1cc77ac9c65L,
	 0x2de92c6f592b0275L, 0x4a7484aa6ea6e483L, 0x5cb0a9dcbd41fbd4L, 0x76f988da831153b5L,
	 0x983e5152ee66dfabL, 0xa831c66d2db43210L, 0xb00327c898fb213fL, 0xbf597fc7beef0ee4L,
	 0xc6e00bf33da88fc2L, 0xd5a79147930aa725L, 0x06ca6351e003826fL, 0x142929670a0e6e70L,
	 0x27b70a8546d22ffcL, 0x2e1b21385c26c926L, 0x4d2c6dfc5ac42aedL, 0x53380d139d95b3dfL,
	 0x650a73548baf63deL, 0x766a0abb3c77b2a8L, 0x81c2c92e47edaee6L, 0x92722c851482353bL,
	 0xa2bfe8a14cf10364L, 0xa81a664bbc423001L, 0xc24b8b70d0f89791L, 0xc76c51a30654be30L,
	 0xd192e819d6ef5218L, 0xd69906245565a910L, 0xf40e35855771202aL, 0x106aa07032bbd1b8L,
	 0x19a4c116b8d2d0c8L, 0x1e376c085141ab53L, 0x2748774cdf8eeb99L, 0x34b0bcb5e19b48a8L,
	 0x391c0cb3c5c95a63L, 0x4ed8aa4ae3418acbL, 0x5b9cca4f7763e373L, 0x682e6ff3d6b2b8a3L,
	 0x748f82ee5defb2fcL, 0x78a5636f43172f60L, 0x84c87814a1f0ab72L, 0x8cc702081a6439ecL,
	 0x90befffa23631e28L, 0xa4506cebde82bde9L, 0xbef9a3f7b2c67915L, 0xc67178f2e372532bL,
	 0xca273eceea26619cL, 0xd186b8c721c0c207L, 0xeada7dd6cde0eb1eL, 0xf57d4f7fee6ed178L,
	 0x06f067aa72176fbaL, 0x0a637dc5a2c898a6L, 0x113f9804bef90daeL, 0x1b710b35131c471bL,
	 0x28db77f523047d84L, 0x32caab7b40c72493L, 0x3c9ebe0a15c9bebcL, 0x431d67c49c100d4cL,
	 0x4cc5d4becb3e42b6L, 0x597f299cfc657e2aL, 0x5fcb6fab3ad6faecL, 0x6c44198c4a475817L };
	
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
			k[i] = (0xFFFFFFFFL) & (K_512[i] >>> 32);
		
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
		Circuit.GateBase[][] nn = {h, g, f, e, d, c, b, a};
		int offset = 0;
		for (int i=0; i<nn.length; ++i) {
			System.arraycopy(nn[i],0,debug_output,offset,wordsize);
			offset += wordsize;
		}
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
		
		cc.outputs = new Circuit.Output[variant];
		

		h0 = new Circuit.Gate[wordsize];
		h1 = new Circuit.Gate[wordsize];
		h2 = new Circuit.Gate[wordsize];
		h3 = new Circuit.Gate[wordsize];
		h4 = new Circuit.Gate[wordsize];
		h5 = new Circuit.Gate[wordsize];
		h6 = new Circuit.Gate[wordsize];
		h7 = new Circuit.Gate[wordsize];
		
		if (variant == 512) {
			comp.const2Gates(h0, 0x6a09e667f3bcc908L);
			comp.const2Gates(h0, 0xbb67ae8584caa73bL);
			comp.const2Gates(h0, 0x3c6ef372fe94f82bL);
			comp.const2Gates(h0, 0xa54ff53a5f1d36f1L);
			comp.const2Gates(h0, 0x510e527fade682d1L);
			comp.const2Gates(h0, 0x9b05688c2b3e6c1fL);
			comp.const2Gates(h0, 0x1f83d9abfb41bd6bL);
			comp.const2Gates(h0, 0x5be0cd19137e2179L);
		} else if (variant == 256) {
			comp.const2Gates(h0, 0x6a09e667);
			comp.const2Gates(h1, 0xbb67ae85);
			comp.const2Gates(h2, 0x3c6ef372);
			comp.const2Gates(h3, 0xa54ff53a);
			comp.const2Gates(h4, 0x510e527f);
			comp.const2Gates(h5, 0x9b05688c);
			comp.const2Gates(h6, 0x1f83d9ab);
			comp.const2Gates(h7, 0x5be0cd19);
		}
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
		
		int offset = 0;

		Circuit.GateBase[][] n = { h7, h6, h5, h4, h3, h2, h1, h0 };
		for (int ch=0; ch<n.length; ++ch) {
			for (int i=0; i<wordsize; ++i) {
				cc.outputs[offset+i] = new Circuit.Output((Circuit.Gate) n[ch][i]);
				cc.outputs[offset+i].setComment("output.md$"+(offset+i));
			}
			offset += wordsize;
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

		//dbg_out(a, b, c, d, e, f, g, h);
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
	
	public boolean[] prepare_sha2_input(boolean[] inputs) {
		int lenoff = blocklen - (blocklen/8);
		
		boolean[] inputs2 = new boolean[blocklen];
		int pad = (blocklen - 1 - inputs.length) + lenoff;
		if (pad<0) pad+=blocklen;
		pad %= blocklen;
		System.out.println("input len "+inputs.length+"  pad "+pad);
		System.arraycopy(inputs, 0, inputs2, 0, inputs.length);
		inputs2[inputs.length]=true;
		byte[] len = new byte[blocklen/64];
		len[wordsize/4-4] = (byte) ((inputs.length>>24) & 0xff);
		len[wordsize/4-3] = (byte) ((inputs.length>>16) & 0xff);
		len[wordsize/4-2] = (byte) ((inputs.length>>8) & 0xff);
		len[wordsize/4-1] = (byte) ((inputs.length) & 0xff);
		// should do 8 or 16 ...
		System.arraycopy(BitUtils.bytes2bool(len), 0, inputs2, lenoff, blocklen/8);
		return inputs2;
	}
	public boolean[] compute_sha2(Circuit cc, boolean[] inputs) {
		return compute_sha2(cc, new boolean[][] { inputs });
	}
	public boolean[] compute_sha2(Circuit cc, boolean[][] inputz) {
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
		MessageDigest md = MessageDigest.getInstance("SHA-512");
		byte[] out = md.digest(args[0].getBytes());
		for (int i=0; i<out.length; ++i) {
			System.out.printf("%02x%s",out[i], (((i+1)%4)==0?" ":""));
		}
		System.out.println();
		System.out.println(Base64.encodeBytes(out));
		
		md = MessageDigest.getInstance("SHA-256");
		out = md.digest(args[0].getBytes());
		for (int i=0; i<out.length; ++i) {
			System.out.printf("%02x%s",out[i], (((i+1)%4)==0?" ":""));
		}
		System.out.println();
		System.out.println(Base64.encodeBytes(out));
		
		/*
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
		*/
		
		
//		MD5Test md5t = new MD5Test();
//		md5t.Update(args[0].getBytes());
//		System.out.println(Base64.encodeBytes(md5t.Final()));
		SHA2 sha2cc = new SHA2(256);
		Circuit cc = sha2cc.generate();
		boolean[][] inputs = { BitUtils.bytes2bool(args[0].getBytes()) };
		out = BitUtils.bool2bytes(sha2cc.compute_sha2(cc, inputs));
		for (int i=0; i<out.length; ++i) {
			System.out.printf("%02x%s",out[i], (((i+1)%4)==0?" ":""));
		}
		System.out.println();
		System.out.println(Base64.encodeBytes(out));
		//CircuitWriter.write(cc);
		System.out.println();
	}
}
