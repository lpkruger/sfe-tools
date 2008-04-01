package sfe.editdist2;

import java.io.*;
import java.math.*;
import java.net.*;

import sfe.crypto.*;

// Lin-Tzeng Millionaires GT protocol
public class LinTzeng {
	static class Alice {
		ObjectInputStream in;
		ObjectOutputStream out;
		ElGamal.EncKey enc;
		ElGamal.DecKey dec;
		
		public Alice(ObjectInputStream in,
				ObjectOutputStream out) {
			this.in = in;
			this.out = out;
		}
		
		public void setKeys(ElGamal.EncKey enc, ElGamal.DecKey dec) {
			this.enc = enc;
			this.dec = dec;
		}
		
		public boolean go(BigInteger a, int maxlen) throws Exception {
			out.writeInt(maxlen);
			
			ElGamal.Ciphertext[][] table = 
				new ElGamal.Ciphertext[maxlen][2];
			for (int i=0; i<maxlen; ++i) {
				if (a.testBit(i)) {
					table[i][1] = enc.random();
					table[i][0] = enc.encrypt(BigInteger.ONE);
				} else {
					table[i][0] = enc.random();
					table[i][1] = enc.encrypt(BigInteger.ONE);
				}
			}
			
			out.writeObject(table);	
			
			ElGamal.Ciphertext[] outtab =
				(ElGamal.Ciphertext[]) in.readObject();
			
			for (int i=0; i<maxlen; ++i) {
				if (dec.decrypt(outtab[i]).equals(BigInteger.ONE)) {
					out.writeBoolean(true);
					return true;
				}
			}
			
			out.writeBoolean(false);
			return false;
		}
		
		
	}
	
	static class Bob {
		ObjectInputStream in;
		ObjectOutputStream out;

		ElGamal.EncKey enc;
		
		public Bob(ObjectInputStream in,
				ObjectOutputStream out) {
			this.in = in;
			this.out = out;
		}
		
		public void setKeys(ElGamal.EncKey enc) {
			this.enc = enc;
		}
		
		public boolean go(BigInteger b) throws Exception {
			int maxlen = in.readInt();
			
			ElGamal.Ciphertext[][] table = 
				(ElGamal.Ciphertext[][]) in.readObject();
			
			ElGamal.Ciphertext[] outtab = 
				new ElGamal.Ciphertext[maxlen];
			
			ElGamal.Ciphertext cummult = enc.encrypt(BigInteger.ONE);
			
			for (int i=maxlen-1; i>=0; --i) {
				if (b.testBit(i)) {
					outtab[i] = enc.random();
					cummult = enc.mult(cummult, table[i][1]);
				} else {
					outtab[i] = enc.mult(cummult, table[i][1]);
					cummult = enc.mult(cummult, table[i][0]);
				}
			}
			
			// TODO: randomly permute table
			
			out.writeObject(outtab);
			
			return in.readBoolean();
		}
	}
}
