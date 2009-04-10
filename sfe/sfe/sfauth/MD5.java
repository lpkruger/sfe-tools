package sfe.sfauth;

import java.security.MessageDigest;

import sfe.sfdl.*;
import sfe.shdl.Circuit;
import sfe.shdl.CircuitWriter;
import sfe.shdl.Optimizer;
import sfe.shdl.Circuit.Gate;
import sfe.util.Base64;

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
	
	CircuitCompiler comp;
	
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
		
		const2Gates(h0, 0x67452301);
		const2Gates(h1, 0xefcdab89);
		const2Gates(h2, 0x98badcfe);
		const2Gates(h3, 0x10325476);
	}
	
	Circuit generateWithPrivEq() {
		initialize(512 + 128);
		for (int i=0; i<512; ++i) {
			cc.inputs[i].setComment("input.alice.x$"+i);
		}
		for (int i=0; i<128; ++i) {
			cc.inputs[512+i].setComment("input.bob.y$"+i);
		}
		update(0);
		
		Circuit.GateBase[] lc = new Circuit.GateBase[128];
		System.arraycopy(h0, 0, lc, 0, 32);
		System.arraycopy(h1, 0, lc, 32, 32);
		System.arraycopy(h2, 0, lc, 64, 32);
		System.arraycopy(h3, 0, lc, 96, 32);
		endian_swap(lc);
		//bit_reverse(lc);
		
		Circuit.GateBase[] rc = new Circuit.GateBase[128];
		System.arraycopy(cc.inputs, 512, rc, 0, 128);
		
		Gate[] eqz = new Gate[128];
		eqz[0] = comp.newGate(lc[0], rc[0], comp.TT_XNOR());
		for(int i=1; i<128; ++i) {
			eqz[i] = comp.newGate(eqz[i-1], lc[i], rc[i], comp.TT_EQ3());
		}
		
		Gate eq = (eqz[eqz.length-1]);
		cc.outputs = new Circuit.Output[] { new Circuit.Output(eq) };
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
		endian_swap(a);
		endian_swap(b);
		endian_swap(c);
		endian_swap(d);
		
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
			const2Gates(kk, k[i]);
			Circuit.GateBase[] ww = new Circuit.GateBase[32];
			System.arraycopy(cc.inputs, input_offset+32*g, ww, 0, 32);
			bit_reverse(ww);
			endian_swap(ww);
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
	
	static void endian_swap(Circuit.GateBase[] g) {  
		// convert big-endian to little-endian by reversing groups of 8
		int j = g.length-8;
		for (int i=0; i<g.length/2; ++i) {
			Circuit.GateBase tmp = g[i];
			g[i] = g[j];
			g[j] = tmp;
			++j;
			if(j%8 == 0)
				j-=16;
		}
	}
	
	
	static Circuit.GateBase[] bit_reverse(Circuit.GateBase[] g) {
		for (int i=0; i<g.length/2; ++i) {
			Circuit.GateBase tmp = g[i];
			g[i] = g[g.length-1-i];
			g[g.length-1-i] = tmp;
		}
		return g;
	}
	static boolean[] bit_reverse(boolean[] g) {
		for (int i=0; i<g.length/2; ++i) {
			boolean tmp = g[i];
			g[i] = g[g.length-1-i];
			g[g.length-1-i] = tmp;
		}
		return g;
	}
	
	void const2Gates(Circuit.Gate[] g, long k) {
		for (int i=0; i<32; ++i) {
			g[i] = comp.newGate();
			g[i].arity = 0;
			g[i].inputs = new Circuit.GateBase[0];
			g[i].truthtab = new boolean[1];
			if (0 != (k & (1<<i))) {
				g[i].truthtab[0] = true;
			}
		}
	}
	
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
		System.arraycopy(bytes2bool(len), 0, inputs2, 448, 64);
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
		bit_reverse(out);
		return out;
	}
	
	public static byte[] bool2bytes(boolean[] inputs) {
		byte[] bb = new byte[inputs.length / 8];
		for (int i=0; i<inputs.length; ++i) {
			if (inputs[i]) {
				bb[i/8] |= (byte)(1<<(7-i%8));
			}
		}
		return bb;
	}
	public static boolean[] bytes2bool(byte[] inputs) {
		boolean[] bb = new boolean[inputs.length * 8];
		for (int i=0; i<inputs.length; ++i) {
			int b = inputs[i] & 0xff;
			bb[0+8*i] = (b & 0x80)!=0;
			bb[1+8*i] = (b & 0x40)!=0;
			bb[2+8*i] = (b & 0x20)!=0;
			bb[3+8*i] = (b & 0x10)!=0;
			bb[4+8*i] = (b & 0x08)!=0;
			bb[5+8*i] = (b & 0x04)!=0;
			bb[6+8*i] = (b & 0x02)!=0;
			bb[7+8*i] = (b & 0x01)!=0;
		}
		return bb;
	}
	
	public static void main(String[] args) throws Exception {
		MessageDigest md = MessageDigest.getInstance("MD5");
		System.out.println(Base64.encodeBytes(md.digest(args[0].getBytes())));
		
		MD5Test md5t = new MD5Test();
		md5t.Update(args[0].getBytes());
		System.out.println(Base64.encodeBytes(md5t.Final()));
		Circuit cc = new MD5().generate();
		boolean[][] inputs = { bytes2bool(args[0].getBytes()) };
		byte[] out = bool2bytes(compute_md5(cc, inputs));
		//for (int i=0; i<out.length; ++i) {
			//System.out.printf("%02x%s",out[i], (((i+1)%4)==0?" ":""));
		//}
		System.out.println(Base64.encodeBytes(out));
		//CircuitWriter.write(cc);
		System.out.println();
	}
}
