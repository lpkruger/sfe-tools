package sfe.shdl;

import java.security.GeneralSecurityException;

import sfe.crypto.SFECipher;
import sfe.crypto.SFEKey;
import sfe.crypto.SFEKeyGenerator;
import java.util.*;

import javax.crypto.SecretKey;

public class CircuitCrypt {

	//public String CIPHER = "AES";
	public String CIPHER = "SHA-1";
	
	SFECipher C;
	public SFEKeyGenerator KG;
	{
		try {
			C = SFECipher.getInstance(CIPHER);
			KG = SFEKeyGenerator.getInstance(CIPHER);
			//KG.init(128);
		} catch (GeneralSecurityException ex) {
			ex.printStackTrace();
			throw new RuntimeException("Can't initialize cipher");
		}
	}
	
	public static class AliceData {
		GarbledCircuit gcc;
		SecretKey[][] inputSecrets;
	}
	
	TreeMap<Integer, GarbledCircuit.Gate> gateid;
	Map<Circuit.Gate, GarbledCircuit.Gate> map;
	Map<Integer, SecretKey[]> secrets;

	AliceData encrypt(Circuit cc) {
		AliceData data = new AliceData();
		data.gcc = new GarbledCircuit();
		data.gcc.nInputs = cc.inputs.length;
		data.gcc.outputs = new int[cc.outputs.length];
		data.inputSecrets = new SecretKey[cc.inputs.length][];
		data.gcc.outputSecrets = new SecretKey[cc.outputs.length][];
		
		gateid = new TreeMap<Integer, GarbledCircuit.Gate>();
		map = new HashMap<Circuit.Gate, GarbledCircuit.Gate>();
		secrets = new HashMap<Integer, SecretKey[]>();
		
	    curId = cc.inputs.length;
	    
		for (int i=0; i<cc.outputs.length; ++i) {
			data.gcc.outputs[i] = encGate_rec(cc.outputs[i]);
			data.gcc.outputSecrets[i] = secrets.get(data.gcc.outputs[i]);
			
			// special case
			if (cc.outputs[i].arity == 0) {
				boolean val = cc.outputs[i].truthtab[0];
				data.gcc.outputSecrets[i][val ? 1 : 0] = 
					new SFEKey(map.get(cc.outputs[i]).truthtab[0]);
				// TODO: replace call to SFEKey constructor
			}
		}
		
		for (int i=0; i<cc.inputs.length; ++i) {
			data.inputSecrets[i] = secrets.get(i);
			
			// avoid null pointer on unused inputs
			if (data.inputSecrets[i] == null) {
				data.inputSecrets[i] = new SecretKey[] { KG.generateKey(), KG.generateKey() };
			}
		}
		
		data.gcc.allGates = gateid.tailMap(cc.inputs.length).values().toArray(new GarbledCircuit.Gate[0]);
		return data;
	}

	protected SecretKey[] genKeyPair(Circuit.GateBase g) {
		return new SecretKey[] { KG.generateKey(), KG.generateKey() };
	}
	
	private int encGate_rec(Circuit.Gate gate) {
		GarbledCircuit.Gate egate = map.get(gate);
		if (egate != null)
			return egate.id;
		
		egate = new GarbledCircuit.Gate();
		egate.arity = gate.arity;
		egate.inputs = new int[gate.arity];
		SecretKey[][] inpsecs = new SecretKey[gate.arity][];
		
		for (int i=0; i<gate.arity; ++i) {
			if (gate.inputs[i] instanceof Circuit.Input) {
				int var = ((Circuit.Input) gate.inputs[i]).var;
				egate.inputs[i] = var;
				inpsecs[i] = secrets.get(var);
				if (inpsecs[i] == null) {
					inpsecs[i] = genKeyPair(gate.inputs[i]);
					secrets.put(var, inpsecs[i]);
				}
			} else {
				egate.inputs[i] = encGate_rec((Circuit.Gate) gate.inputs[i]);
				inpsecs[i] = secrets.get(egate.inputs[i]);
			}
		}
		
		egate.id = getId();
		
		gateid.put(egate.id, egate);
		map.put(gate, egate);
		SecretKey[] secr = genKeyPair(gate);
		secrets.put(egate.id, secr);
		
		if (gate.arity == 0) {  // special case to avoid NullPointerException
			inpsecs = new SecretKey[][] { genKeyPair(null) };
		}
		
		//System.out.println("inpsecs.length : " + inpsecs.length);
		//System.out.println("gate.truthtab.length : " + gate.truthtab.length);
		
		egate.truthtab = new byte[gate.truthtab.length][];
		for (int i=0; i<gate.truthtab.length; ++i) {
			SecretKey thisKey = ((i >> (gate.arity-1) & 0x1) == 0) ? inpsecs[0][0] : inpsecs[0][1];
			for (int j=1; j<gate.arity; ++j) {
				thisKey = SFEKey.xor(thisKey,
						((i >> (gate.arity-j-1) & 0x1) == 0) ? inpsecs[j][0] : inpsecs[j][1],
								CIPHER);
			}
			
			try {
				C.init(C.ENCRYPT_MODE, thisKey);
				egate.truthtab[i] = C.doFinal(secr[gate.truthtab[i]?1:0].getEncoded());
			} catch (GeneralSecurityException ex) {
				ex.printStackTrace();
				throw new RuntimeException("encryption error");
			}
			
			// TODO: randomly permute the truth table
		}
		
		return egate.id;
	}

	int curId;
	
	int getId() {
		return curId++;
	}

}
