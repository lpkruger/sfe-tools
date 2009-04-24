package sfe.shdl;

import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.*;

import javax.crypto.SecretKey;

import sfe.crypto.SFECipher;
import sfe.crypto.SFEKey;
import sfe.shdl.CircuitCrypt.AliceData;
import sfe.shdl.Circuit.Gate;
import sfe.shdl.Circuit.GateBase;
import sfe.shdl.Circuit.Input;
import sfe.shdl.Circuit.Output;

public class CircuitCryptPermute extends CircuitCrypt {
	
	Map<Circuit.GateBase, Boolean> flip;
	
	public CircuitCryptPermute(SecureRandom rand) {
		super(rand);
	}
	public AliceData encrypt(Circuit cc) {
		String NOPADDING = "//NoPadding";
		try {
			C = SFECipher.getInstance(CIPHER + NOPADDING);
		} catch (GeneralSecurityException ex) {
			ex.printStackTrace();
			throw new RuntimeException("error init cipher");
		}
		
		flip = new HashMap<Circuit.GateBase, Boolean>();
		Circuit.calcDeps(cc);
		for (int i=0; i<cc.outputs.length; ++i) {
			doFlip_rec(cc.outputs[i]);
		}
		
	    AliceData ret = super.encrypt(cc);
	    
	    for (int i=0; i<cc.inputs.length; ++i) {
	    	Boolean b = flip.get(cc.inputs[i]);
	    	if (b!=null && b) {
	    		SecretKey tmp = ret.inputSecrets[i][1];
	    		ret.inputSecrets[i][1] = ret.inputSecrets[i][0];
	    		ret.inputSecrets[i][0] = tmp;
	    	}
	    }
	    
	    ret.gcc.use_permute = true;
	    
	    return ret;
	}
	
	protected SecretKey[] genKeyPair(Circuit.GateBase g) {
		byte[][] sk2 = new byte[][] { KG.generateKey().getEncoded(), KG.generateKey().getEncoded() };
		sk2[0][0] &= (0xfe);
		sk2[1][0] |= (0x01);
		return new SFEKey[] { new SFEKey(sk2[0]), new SFEKey(sk2[1]) };
	}
	
	boolean doFlip_rec(GateBase g) {
		Boolean f = flip.get(g);
		if (f != null)
			return f;
		
		if (g instanceof Output) {
			// TODO: need to garble Alice's output bits only
			f = false;
		} else {
			f = random.nextBoolean();
		}
		
		flip.put(g, f);
		
		if (g instanceof Input) {
			return f;
		}
		
		Gate gg = (Gate) g;
				
		for (int i=0; i<gg.arity; ++i) {
			if (doFlip_rec(gg.inputs[i])) {
				permuteInput(gg.truthtab, gg.arity, i);
			}
		}
		
		if (f) {
			for (int i=0; i<gg.truthtab.length; ++i)
				gg.truthtab[i] = !gg.truthtab[i];
		}
		
		return f;
	}
	
	// permutes the truth table according to the variable pos being flipped
	void permuteInput(boolean[] tt, int arity, int pos) {
		
			// "partial evaluation" of a single truth table
			boolean[] ntt = new boolean[tt.length >> 1];
			
			int q = arity - 1 - pos;
			int qq = 1 << q;
			
			int j=0;
			for (int i=0; i<tt.length; ++i) {
				if (((i>>q)&1) == 0) {
					boolean tmp = tt[i+qq];
					tt[i+qq] = tt[i];
					tt[i] = tmp;
				}
			}
	}
}
