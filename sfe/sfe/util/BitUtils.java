package sfe.util;

import sfe.shdl.Circuit;
import sfe.shdl.Circuit.GateBase;

public class BitUtils {

	public static void endian_swap(Circuit.GateBase[] g) {  
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

	public static Circuit.GateBase[] bit_reverse(Circuit.GateBase[] g) {
		for (int i=0; i<g.length/2; ++i) {
			Circuit.GateBase tmp = g[i];
			g[i] = g[g.length-1-i];
			g[g.length-1-i] = tmp;
		}
		return g;
	}

	public static boolean[] bit_reverse(boolean[] g) {
		for (int i=0; i<g.length/2; ++i) {
			boolean tmp = g[i];
			g[i] = g[g.length-1-i];
			g[g.length-1-i] = tmp;
		}
		return g;
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

}
