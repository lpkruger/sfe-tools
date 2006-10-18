package sfe.shdl;

import java.security.GeneralSecurityException;
import java.util.*;

import javax.crypto.BadPaddingException;
import javax.crypto.SecretKey;
import sfe.crypto.*;
import sfe.util.*;
import sfe.shdl.GarbledCircuit.Gate;

public class GCircuitEval extends CircuitCrypt {
	Gate getGate(int id, GarbledCircuit gcc) {
		return gcc.allGates[id - gcc.nInputs];
	}
	
	public boolean[] eval(GarbledCircuit gcc, SecretKey[] insk) {
		if (gcc.use_permute) {
			String NOPADDING = "//NoPadding";
			try {
				C = SFECipher.getInstance(CIPHER + NOPADDING);
			} catch (GeneralSecurityException ex) {
				ex.printStackTrace();
				throw new RuntimeException("error init cipher");
			}
		}
		TreeMap<Integer, byte[]> vals = new TreeMap<Integer, byte[]>();
		for (int i=0; i<insk.length; ++i) {
			vals.put(i, insk[i].getEncoded());
		}
		boolean[] ret = new boolean[gcc.outputs.length];
		for (int i=0; i<gcc.outputs.length; ++i) {
			byte[] retval = eval_rec(getGate(gcc.outputs[i], gcc), gcc, vals);
			SecretKey retkey = SFEKey.bytesToKey(retval, CIPHER);
			
			/*
			System.out.println("O " + i + " " + Base64.encodeBytes(retval) + " " + 
					Base64.encodeBytes(gcc.outputSecrets[i][0].getEncoded()) + " " + 
					Base64.encodeBytes(gcc.outputSecrets[i][1].getEncoded()));
			*/
			if (gcc.outputSecrets[i][0].equals(retkey))
				ret[i] = false;
			else if (gcc.outputSecrets[i][1].equals(retkey))
				ret[i] = true;
			else {
				System.out.println("output " + Base64.encodeBytes(retval));
				System.out.println("false  " + Base64.encodeBytes(gcc.outputSecrets[i][1].getEncoded()));
				System.out.println("true   " + Base64.encodeBytes(gcc.outputSecrets[i][0].getEncoded()));
				throw new RuntimeException("eval error");
			}
			
		}
		
		return ret;
	}
	
	byte[] eval_rec(Gate g, GarbledCircuit gcc, TreeMap<Integer, byte[]> vals) {
		if (g.arity == 0) {
			vals.put(g.id, g.truthtab[0]);
			return g.truthtab[0];
		}
		
		byte[] ink = vals.get(g.inputs[0]);
		if (ink == null) {
			ink = eval_rec(getGate(g.inputs[0], gcc), gcc, vals);
		}
		
		for (int i=1; i<g.arity; ++i) {
			byte[] ink2 = vals.get(g.inputs[i]);
			if (ink2 == null) {
				ink2 = eval_rec(getGate(g.inputs[i], gcc), gcc, vals);
			}			
			ink = SFEKey.xor(ink, ink2);
		}
		
		byte[] out = null;
		SecretKey sk = SFEKey.bytesToKey(ink, CIPHER);
		
		int ttstart = 0;
		if (gcc.use_permute) {
			for (int i=0; i<g.arity; ++i) {
				int bit = vals.get(g.inputs[i])[0] & 0x01;
				ttstart = (ttstart << 1) | bit;
			}
		}
		
		for (int i=ttstart; i<g.truthtab.length; ++i) {
			try {
				C.init(C.DECRYPT_MODE, sk);
				out = C.doFinal(g.truthtab[i]);
				if (out.length == KG.getLength()/8)
					break;
			} catch (BadPaddingException ex) {
				// it wasn't the right one, keep trying
			} catch (GeneralSecurityException ex) {
				ex.printStackTrace();
				throw new RuntimeException("Can't decrypt data");
			}
		}
		
		if (out == null) {
			throw new RuntimeException("Can't decrypt TT with key " + Base64.encodeBytes(ink));
		}
		
		vals.put(g.id, out);
		return out;
	}
}
