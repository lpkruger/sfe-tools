package sfe.sfauth;

import sfe.shdl.Circuit;

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
	int id;
	
	int[] r = {7, 12, 17, 22,  7, 12, 17, 22,  7, 12, 17, 22,  7, 12, 17, 22,
			5,  9, 14, 20,  5,  9, 14, 20,  5,  9, 14, 20,  5,  9, 14, 20,
			4, 11, 16, 23,  4, 11, 16, 23,  4, 11, 16, 23,  4, 11, 16, 23,
			6, 10, 15, 21,  6, 10, 15, 21,  6, 10, 15, 21,  6, 10, 15, 21};
	
	int[] k = new int[64];
	{
		for (int i=0; i<63; ++i) {
			k[i] = (int) Math.floor(Math.abs(Math.sin(i + 1)) * Math.pow(2, 32));
		}
	}
	
	
	void generate() {
		Circuit cc = new Circuit();
		cc.inputs = new Circuit.Input[128];
		for (int i=0; i<128; ++i) {
			cc.inputs[i] = new Circuit.Input(id,id);
			id++;
		}
		Circuit.Gate[] h0 = new Circuit.Gate[32];
		Circuit.Gate[] h1 = new Circuit.Gate[32];
		Circuit.Gate[] h2 = new Circuit.Gate[32];
		Circuit.Gate[] h3 = new Circuit.Gate[32];
		
		const2Gates(h0, 0x67452301);
		const2Gates(h1, 0xefcdab89);
		const2Gates(h2, 0x98badcfe);
		const2Gates(h3, 0x10325476);
		
		Circuit.Gate[] a = h0;
		Circuit.Gate[] b = h1;
		Circuit.Gate[] c = h2;
		Circuit.Gate[] d = h3;
		Circuit.Gate[] tmp;
		
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
			
			tmp = d;
			d = c;
			c = b;
			b = new Circuit.Gate[128];
			// b = ((a + f + k[i] + w(g)) leftrotate r[i]) + b
			a = tmp;
		}
		
	}
	
	void const2Gates(Circuit.Gate[] g, int k) {
		for (int i=0; i<32; ++i) {
			g[i] = new Circuit.Gate(id++);
			g[i].arity = 0;
			g[i].inputs = new Circuit.GateBase[0];
			g[i].truthtab = new boolean[1];
			if (0 != (k & (1<<i))) {
				g[i].truthtab[0] = true;
			}
		}
	}

}
