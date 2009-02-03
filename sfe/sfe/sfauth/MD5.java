package sfe.sfauth;

import sfe.sfdl.*;
import sfe.shdl.Circuit;
import sfe.shdl.CircuitWriter;
import sfe.shdl.Optimizer;

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
			System.out.println(k[i]);
		}
	}
	
	CircuitCompiler comp;
	
	void generate() {
		comp = new CircuitCompiler();
		Circuit cc = new Circuit();
		cc.inputs = new Circuit.Input[512];
		for (int i=0; i<512; ++i) {
			int id = comp.newId();
			cc.inputs[i] = new Circuit.Input(id,id);
		}
		cc.outputs  = new Circuit.Output[128];
		Circuit.Gate[] h0 = new Circuit.Gate[32];
		Circuit.Gate[] h1 = new Circuit.Gate[32];
		Circuit.Gate[] h2 = new Circuit.Gate[32];
		Circuit.Gate[] h3 = new Circuit.Gate[32];
		
		const2Gates(h0, 0x67452301);
		const2Gates(h1, 0xefcdab89);
		const2Gates(h2, 0x98badcfe);
		const2Gates(h3, 0x10325476);
		
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
			System.arraycopy(cc.inputs, 32*g, ww, 0, 32);
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
			
			System.arraycopy(bnew, 0, bnew2, 32-r[i], r[i]);
			System.arraycopy(bnew, r[i], bnew2, 0, 32-r[i]);
			/* DEBUG: use above 2 lines instead */  
			//bnew2 = bnew;
			
			b = comp.createAdder(b, bnew2);
			
			a = tmp;
		}
		
		for (int i=0; i<32; ++i) {
			cc.outputs[i] = new Circuit.Output((Circuit.Gate) a[i]);
		}
		for (int i=0; i<32; ++i) {
			cc.outputs[32+i] = new Circuit.Output((Circuit.Gate) b[i]);
		}
		for (int i=0; i<32; ++i) {
			cc.outputs[64+i] = new Circuit.Output((Circuit.Gate) c[i]);
		}
		for (int i=0; i<32; ++i) {
			cc.outputs[96+i] = new Circuit.Output((Circuit.Gate) d[i]);
		}
		
		Optimizer opt = new Optimizer();
		opt.optimize(cc);
		opt.renumber(cc);
		
		CircuitWriter.write(cc);
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
	
	public static void main(String[] args) {
		new MD5().generate();
	}

}
