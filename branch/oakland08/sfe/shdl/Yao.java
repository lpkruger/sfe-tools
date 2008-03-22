package sfe.shdl;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

/*
gate:
inputs x,y   (garbled)
output   z   (garbled)

For each node:
Alice chooses 2 symbols, one for hi, one for lo, Z1, Z0
input will be X1 or X0, and Y1 or Y0
encrypt:
E(X0 xor Y0) (Z_n(0,0))
E(X0 xor Y1) (Z_n(0,1))
E(X1 xor Y0) (Z_n(1,0))
E(X1 xor Y1) (Z_n(1,1))
 */
public class Yao {
	
	
	
	public static void main(String[] args) throws IOException {
		BufferedReader in = new BufferedReader(new FileReader(args[0]));
		Circuit cc = new CircuitParser().parse(in);
		CircuitCrypt.AliceData data = new CircuitCrypt().encrypt(cc);
		System.out.println(data);
	}
}
