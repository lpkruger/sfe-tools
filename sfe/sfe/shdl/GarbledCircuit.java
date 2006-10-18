package sfe.shdl;

import java.io.*;
import java.security.GeneralSecurityException;
import java.util.*;

import javax.crypto.SecretKey;

import sfe.crypto.SFECipher;
import sfe.crypto.SFEKey;
import sfe.crypto.SFEKeyGenerator;

class GarbledCircuit implements Serializable {
	
	static class Gate implements Serializable {
		int id;
		int arity;
		int[] inputs;
		byte[][] truthtab;   // randomly permuted set of E(k)(Zn)
	}
	
	boolean use_permute;
	int nInputs;
	int[] outputs;
	SecretKey[][] outputSecrets;
	Gate[] allGates;
	
	
	
	void writeCircuit(ObjectOutputStream out) throws IOException {
		out.writeBoolean(use_permute);
		out.writeInt(nInputs);
		out.writeInt(outputs.length);
		for (int i=0; i<outputs.length; ++i)
			out.writeInt(outputs[i]);
		
		out.writeInt(outputSecrets[0][0].getEncoded().length);
		
		out.writeInt(outputSecrets.length);
		
		for (int i=0; i<outputSecrets.length; ++i) {
			out.write(outputSecrets[i][0].getEncoded());
			out.write(outputSecrets[i][1].getEncoded());
		}
		
		out.writeInt(allGates[0].truthtab[0].length);
		out.writeInt(allGates.length);
		for (int i=0; i<allGates.length; ++i) {
			out.writeByte(allGates[i].arity);
			for (int j=0; j<allGates[i].arity; ++j) {
				out.writeInt(allGates[i].inputs[j]);
			}
			for (int j=0; j<allGates[i].truthtab.length; ++j) {
				out.write(allGates[i].truthtab[j]);
			}
		}
	    
	}
		
	
	
	static GarbledCircuit readCircuit(ObjectInputStream in) throws IOException {
		GarbledCircuit gcc = new GarbledCircuit();
		gcc.use_permute = in.readBoolean();
		gcc.nInputs = in.readInt();
		gcc.outputs = new int[in.readInt()];
		for (int i=0; i<gcc.outputs.length; ++i)
			gcc.outputs[i] = in.readInt();
		
		int seclen = in.readInt();
		
		gcc.outputSecrets = new SecretKey[in.readInt()][2];
		for (int i=0; i<gcc.outputSecrets.length; ++i) {
			byte[] buf = new byte[seclen];	
			in.readFully(buf);
			gcc.outputSecrets[i][0] = new SFEKey(buf);
			buf = new byte[seclen];	
			in.readFully(buf);
			gcc.outputSecrets[i][1] = new SFEKey(buf);
		}
		
		seclen = in.readInt();
		
		gcc.allGates = new Gate[in.readInt()];
		for (int i=0; i<gcc.allGates.length; ++i) {
			gcc.allGates[i] = new Gate();
			gcc.allGates[i].id = i + gcc.nInputs;
			gcc.allGates[i].arity = in.readByte();
			gcc.allGates[i].inputs = new int[gcc.allGates[i].arity];
			for (int j=0; j<gcc.allGates[i].arity; ++j) {
				gcc.allGates[i].inputs[j] = in.readInt();
			}
			int tts = -1;
			switch(gcc.allGates[i].arity) {
			case 0:
				tts = 1; break;
			case 1:
				tts = 2; break;
			case 2:
				tts = 4; break;
			case 3:
				tts = 8; break;
			default:
				throw new RuntimeException("Unexpected arity: " + tts);
			}
			
			gcc.allGates[i].truthtab = new byte[tts][seclen];
			for (int j=0; j<gcc.allGates[i].truthtab.length; ++j) {
				in.readFully(gcc.allGates[i].truthtab[j]);
			}
		}
		return gcc;
	}
}