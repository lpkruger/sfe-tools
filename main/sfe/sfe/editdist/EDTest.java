package sfe.editdist;

import java.math.BigInteger;

import sfe.shdl.Circuit;
import sfe.shdl.CircuitParser;

public class EDTest {
	public static void main(String[] args) {
		Circuit circuit = CircuitParser.readFile("splitmin3.txt.Opt.circuit");
		boolean[] inputs = new boolean[4*7];
		mapBits(0, inputs, 12, 15);
		mapBits(1, inputs, 0, 3);
		mapBits(2, inputs, 20, 23);
		mapBits(2, inputs, 24, 27);
		
		boolean[] out = circuit.eval(inputs);
		for (int i=0; i<out.length; ++i) {
			System.out.println("out" + i + " " + out[i]);
		}
	}
	
	static void mapBits(int nb, boolean[] vals, int lsb, int msb) {
		BigInteger n = BigInteger.valueOf(nb);
		D("map bits " + n);
		D("  from " + lsb + " to " + msb);
		for (int i=0; i<msb-lsb+1; ++i) {
			D("put " + n.testBit(i) + " in " + (lsb+i));   // lsb first
            vals[lsb+i] = n.testBit(i);
		}
	}
	
	static void D(Object x) {
		System.out.println(x);
	}
}
