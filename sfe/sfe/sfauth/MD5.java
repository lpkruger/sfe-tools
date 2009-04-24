package sfe.sfauth;

import java.security.MessageDigest;

import sfe.sfdl.*;
import sfe.shdl.*;
import sfe.shdl.Circuit.Gate;
import sfe.util.*;

public class MD5 {

	boolean[] Itt = new boolean[8];
	boolean[] Htt = new boolean[8];
	boolean[] Gtt = new boolean[8];
	boolean[] Ftt = new boolean[8];
	{
		for (int i=0; i<8; ++i) {
			boolean x=(0!=(4&i));
			boolean y=(0!=(2&i));
			boolean z=(0!=(1&i));
			Ftt[i] = (x & y) | (!x & z);
			Gtt[i] = (x & z) | (y & !z);
			Htt[i] = (x ^ y ^ z);
			Itt[i] = y ^ (x | !z);
		}
	}
	//int id;
	
	int[] r = {7, 12, 17, 22,  7, 12, 17, 22,  7, 12, 17, 22,  7, 12, 17, 22,
			5,  9, 14, 20,  5,  9, 14, 20,  5,  9, 14, 20,  5,  9, 14, 20,
			4, 11, 16, 23,  4, 11, 16, 23,  4, 11, 16, 23,  4, 11, 16, 23,
			6, 10, 15, 21,  6, 10, 15, 21,  6, 10, 15, 21,  6, 10, 15, 21};
	
	long[] k = new long[64];
	{
		for (int i=0; i<64; ++i) {
			k[i] = (long) Math.floor(Math.abs(Math.sin(i + 1)) * Math.pow(2, 32));
			//k[i] = (long) Math.floor(Math.scalb(Math.abs(Math.sin(i + 1)), 32));
			//System.out.printf("%08x\n", k[i]);
		}
	}
	
	public CircuitCompiler comp;
	
	Circuit.GateBase[] debug_output = null;	// for debugging
	
	void dbg_out(Circuit.GateBase[] a, Circuit.GateBase[] b,
			Circuit.GateBase[] c, Circuit.GateBase[] d)
	{
		debug_output = new Circuit.GateBase[128];
		System.arraycopy(d,0,debug_output,0,32);
		System.arraycopy(c,0,debug_output,32,32);
		System.arraycopy(b,0,debug_output,64,32);
		System.arraycopy(a,0,debug_output,96,32);
		
	}
	
	Circuit.Gate[] h0;
	Circuit.Gate[] h1;
	Circuit.Gate[] h2;
	Circuit.Gate[] h3;
	
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
		
		cc.outputs  = new Circuit.Output[128];
		

		h0 = new Circuit.Gate[32];
		h1 = new Circuit.Gate[32];
		h2 = new Circuit.Gate[32];
		h3 = new Circuit.Gate[32];
		
		comp.const2Gates(h0, 0x67452301);
		comp.const2Gates(h1, 0xefcdab89);
		comp.const2Gates(h2, 0x98badcfe);
		comp.const2Gates(h3, 0x10325476);
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
		BitUtils.endian_swap(lc);
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
		Circuit.GateBase[] a = h0.clone();
		Circuit.GateBase[] b = h1.clone();
		Circuit.GateBase[] c = h2.clone();
		Circuit.GateBase[] d = h3.clone();
		BitUtils.endian_swap(a);
		BitUtils.endian_swap(b);
		BitUtils.endian_swap(c);
		BitUtils.endian_swap(d);
		
		for (int i=0; i<32; ++i) {
			cc.outputs[i] = new Circuit.Output((Circuit.Gate) d[i]);
			cc.outputs[i].setComment("output.md$"+(i));
		}
		for (int i=0; i<32; ++i) {
			cc.outputs[32+i] = new Circuit.Output((Circuit.Gate) c[i]);
			cc.outputs[32+i].setComment("output.md$"+(32+i));
		}
		for (int i=0; i<32; ++i) {
			cc.outputs[64+i] = new Circuit.Output((Circuit.Gate) b[i]);
			cc.outputs[64+i].setComment("output.md$"+(64+i));
		}
		for (int i=0; i<32; ++i) {
			cc.outputs[96+i] = new Circuit.Output((Circuit.Gate) a[i]);
			cc.outputs[96+i].setComment("output.md$"+(96+i));
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
		Circuit.GateBase[] tmp;
		
		boolean[] f;
		int g;
		
		
		
		for (int i=0; i<64; ++i) {
			if (i < 16) {
				f = Ftt;
				g = i;
			} else if (i<32) {
				f = Gtt;
				g = (5*i + 1) % 16;
			} else if (i<48) {
				f = Htt;
				g = (3*i + 5) % 16;
			} else {
				f = Itt;
				g = (7*i) % 16;
			}
			
			Circuit.Gate[] ff = new Circuit.Gate[32];
			for (int j=0; j<32; ++j) {
				ff[j] = comp.newGate();
				ff[j].setComment("f " + i + " $ "+ j);
				ff[j].arity = 3;
				ff[j].inputs = new Circuit.GateBase[] {b[j], c[j], d[j]};
				ff[j].truthtab = f.clone();
			}
			
			tmp = d;
			d = c;
			c = b;
			//b = new Circuit.Gate[128];
			// b = ((a + f + k[i] + w(g)) leftrotate r[i]) + b
			
			Circuit.Gate[] kk = new Circuit.Gate[32];
			comp.const2Gates(kk, k[i]);
			Circuit.GateBase[] ww = new Circuit.GateBase[32];
			System.arraycopy(cc.inputs, input_offset+32*g, ww, 0, 32);
			BitUtils.bit_reverse(ww);
			BitUtils.endian_swap(ww);
			Circuit.Gate[] aplusf = comp.createAdder(a, ff);
			Circuit.Gate[] kplusw = comp.createAdder(kk, ww);
			for (int j=0; j<32; ++j) {
				if (kk[j]==null)
					throw new RuntimeException("kk["+j+"] is null");
				//System.out.println(kk[j]);
				if (ww[j]==null)
					throw new RuntimeException("ww["+j+"] is null");
				//System.out.println(ww[j]);
			}
			Circuit.Gate[] bnew = comp.createAdder(aplusf, kplusw);
						
			for (int j=0; j<32; ++j) {
				aplusf[j].setComment("a+f "+i+" $ "+j);
				kplusw[j].setComment("k+w "+i+" $ "+j);
				bnew[j].setComment("bnew "+i+" $ "+j);
			}
			Circuit.Gate[] bnew2 = new Circuit.Gate[32];
			//    [0] [1] [2] [3]
			// [] [1] [2] [3] [0]
			
			/*
			bit_reverse(bnew);
			System.arraycopy(bnew, 0, bnew2, 32-r[i], r[i]);
			System.arraycopy(bnew, r[i], bnew2, 0, 32-r[i]);
			bit_reverse(bnew2);
			*/
			//MD5 calls for left rot, but we store bits backwards so need a right rot instead
			System.arraycopy(bnew, 0, bnew2, r[i], 32-r[i]);
			System.arraycopy(bnew, 32-r[i], bnew2, 0, r[i]);
			
			
			b = comp.createAdder(b, bnew2);
			
			a = tmp;
			
			/*
			Circuit.Gate[] www = new Circuit.Gate[32];
			for (int j=0; j<32; ++j) {
				www[j] = comp.newIdentityGate(ww[j]);
			}*/
			/// debug ///
			//if (i==60) debug_output = dbg_out(b,ff,www,kk);
			
		}
		
		h0 = comp.createAdder(a, h0);
		h1 = comp.createAdder(b, h1);
		h2 = comp.createAdder(c, h2);
		h3 = comp.createAdder(d, h3);
	}
	
	//01234567 89012345 67890123 45678901
	
	public static boolean[] prepare_md5_input(boolean[] inputs) {
		boolean[] inputs2 = new boolean[512];
		int pad = (511 - inputs.length) + 448;
		if (pad<0) pad+=512;
		pad %= 512;
		System.out.println("input len "+inputs.length+"  pad "+pad);
		System.arraycopy(inputs, 0, inputs2, 0, inputs.length);
		inputs2[inputs.length]=true;
		byte[] len = new byte[8];
		len[0] = (byte) (inputs.length & 0xff);
		len[1] = (byte) ((inputs.length>>8) & 0xff);
		len[2] = (byte) ((inputs.length>>16) & 0xff);
		len[3] = (byte) ((inputs.length>>24) & 0xff);
		// should do 4 more...
		System.arraycopy(BitUtils.bytes2bool(len), 0, inputs2, 448, 64);
		return inputs2;
	}
	public static boolean[] compute_md5(Circuit cc, boolean[] inputs) {
		return compute_md5(cc, new boolean[][] { inputs });
	}
	public static boolean[] compute_md5(Circuit cc, boolean[][] inputz) {
		boolean[] inputs3 = new boolean[512*inputz.length];
		for (int i=0; i<inputz.length; ++i) {
			boolean[] inputs = inputz[i];
			boolean[] inputs2 = prepare_md5_input(inputs);
			
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
		MessageDigest md = MessageDigest.getInstance("MD5");
		System.out.println(Base64.encodeBytes(md.digest(args[0].getBytes())));
		
		MD5Test md5t = new MD5Test();
		md5t.Update(args[0].getBytes());
		System.out.println(Base64.encodeBytes(md5t.Final()));
		Circuit cc = new MD5().generate();
		boolean[][] inputs = { BitUtils.bytes2bool(args[0].getBytes()) };
		byte[] out = BitUtils.bool2bytes(compute_md5(cc, inputs));
		//for (int i=0; i<out.length; ++i) {
			//System.out.printf("%02x%s",out[i], (((i+1)%4)==0?" ":""));
		//}
		System.out.println(Base64.encodeBytes(out));
		//CircuitWriter.write(cc);
		System.out.println();
	}
}
