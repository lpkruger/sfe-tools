package sfe.editdist;

import java.io.*;
import java.math.BigInteger;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Random;
import java.util.TreeMap;
import java.util.TreeSet;

import sfe.shdl.*;
import sfe.util.*;
// import sfe.bdd.proto.Protocol;
import sfe.crypto.*;

/*
 * recurrence:
 * Alice has a(i,j), Bob has b(i,j) where a(i,j)+b(i,j)=D(i,j)
 * input: a(i-1,j-1),a(i-1,j),a(i,j-1),b(i-1,j-1),b(i-1,j),b(i,j-1)
 * compute min(X,Y,Z)-R and R
 * X = a(i-1,j)+b(i-1,j)
 * Y = a(i,j-1)+b(i,j-1)
 * Z = a(i-1,j-1)+b(i-1,j-1)+t(i,j)
 * 
 */

public class EDProtoBlock {
	
	static void D(Object s) {
		System.out.println(s);
	}
	
	static BigInteger TWO = BigInteger.valueOf(2);
	static final int N_BITS=8;
	static final int CHAR_BITS=8;
	
	static int BLOCK_SIZE=4;  // block size
	static {
		String blocksize = System.getProperty("BLOCKSIZE");
		if (blocksize != null) {
			BLOCK_SIZE=Integer.parseInt(blocksize);
		}
	}

	static boolean parallelize = false;
	static {
		String par = System.getProperty("OTBATCH");
		if (par != null) {
			parallelize = "true".equals(par);
		}
	}
	
	public static void main(String[] args) throws Exception {
		if (System.getProperty("BOB") != null) {
			Bob.main(args);
		} else if (System.getProperty("ALICE") != null) {
			Alice.main(args);
		} else System.out.println("Must use -DALICE or -DBOB");
	}
	
	//public static BigInteger getRandom() {
		//return new BigInteger(N_BITS, new Random());
		// DEBUG:
		//return BigInteger.ZERO;
	//}
	
	public static Random rand = new Random();
	
	public static class Alice {
		
		Socket bob;
		ObjectInputStream in_raw;
		ObjectInputStream in;
		ObjectOutputStream out_raw;
		ByteCountObjectOutputStream out;
		long startTime;
		ByteCountOutputStreamSFE byteCount;
		
		String str1;
		FmtFile fmt;
		VarDesc aliceVars;
		VarDesc bobVars;
		
		int[][] matrix;
		
		Alice(String to, int port, String str1) throws IOException {
			bob = new Socket(to, port);
			
			startTime = System.currentTimeMillis();
			byteCount = 
				new ByteCountOutputStreamSFE(
						bob.getOutputStream());
			out_raw = new ObjectOutputStream(byteCount);
			out_raw.flush();
			in_raw = new ObjectInputStream
			(new BufferedInputStream
					(bob.getInputStream()));
			
			this.str1 = str1;
			
		}
		
		public static void main(String[] args) throws Exception {
			String to = args[0];
			int port = Integer.parseInt(args[1]);
			String str1 = args[2];
			
			new Alice(to, port, str1).go();
		}
		
		void go() throws Exception {
			int astrlen = str1.length();
			out_raw.writeInt(astrlen);
			out_raw.flush();
			
			int bstrlen = in_raw.readInt();
			
			matrix = new int[astrlen+1][bstrlen+1];
			
			for (int i=1; i<=astrlen; ++i) {
				matrix[i][0] = i;
			}
			for (int j=1; j<=bstrlen; ++j) {
				matrix[0][j] = j;
			}
				
			out = ByteCountObjectOutputStream.wrapObjectStream(this.out_raw);
			out.flush();
			in = new ObjectInputStream(this.in_raw);
			
			//circuit = CircuitParser.readFile("editdist/block_" + N_BITS + "_" + BLOCK_SIZE + "_" + BLOCK_SIZE + ".txt.Opt.circuit");
			fmt = FmtFile.readFile("editdist/block_" + N_BITS + "_" + BLOCK_SIZE + "_" + BLOCK_SIZE + ".txt.Opt.fmt");
			
			VarDesc bdv = fmt.getVarDesc();
			aliceVars = bdv.filter("A");
			bobVars = bdv.filter("B");
			
			if (!parallelize) {
				for (int i=0; i<astrlen; i+=BLOCK_SIZE) {
					for (int j=0; j<bstrlen; j+=BLOCK_SIZE) {
						//Runtime.getRuntime().gc();
						long free = Runtime.getRuntime().freeMemory();
						long total = Runtime.getRuntime().totalMemory();
						System.out.println("Used " + (total-free) + "  Free: " + free + "  out of " + total);
						System.out.println("Iteration " + i + ", " + j);
						computeRecurrence(i,j);
						out.reset();
					}
				}
			} else {
				ArrayList<int[]> diagonal = new ArrayList();
				for (int i=0; i<astrlen+bstrlen-1; i+=BLOCK_SIZE) {
					diagonal.clear();
					long free = Runtime.getRuntime().freeMemory();
					long total = Runtime.getRuntime().totalMemory();
					System.out.println("Used " + (total-free) + "  Free: " + free + "  out of " + total);
					for (int j=0; j<bstrlen; j+=BLOCK_SIZE) {
						if (i-j>=0 && i-j<astrlen) {
							diagonal.add(new int[] {i-j, j});
							System.out.println("(" + (i-j) + "," + j + ")");
						}
					}
					int[][] pairs = diagonal.toArray(new int[0][]);
					computeRecurrenceParallel(pairs);
				}
			}
			
			out.writeInt(matrix[astrlen][bstrlen]);
			out.flush();
			
			System.out.println("Alice circuit wrote " + out.getCount() + " bytes");
			
			// for DEBUG
			//BigInteger bobVal = (BigInteger) in.readObject();
			//BigInteger combined = r0.add(bobVal).and(MAX_BIGINT);
			//System.out.println("result after stage: " + combined);
		}
		
		public void computeRecurrenceParallel(int[][] pairs) throws Exception {
			D("prepare circuit");

			Circuit[] ccs = new Circuit[pairs.length];
			
			TreeMap<Integer,Boolean>[] vals = new TreeMap[pairs.length];
			
			for (int z=0; z<pairs.length; ++z) {
				vals[z] = new TreeMap<Integer,Boolean>();
				ccs[z] = CircuitParser.readFile("editdist/block_" + N_BITS + "_" + BLOCK_SIZE + "_" + BLOCK_SIZE + ".txt.Opt.circuit");
				int ibase = pairs[z][0];
				int jbase = pairs[z][1];

				for (int i=0; i<=BLOCK_SIZE; ++i) {
					if (i<BLOCK_SIZE) {
						byte c0 = (byte) str1.charAt(ibase + i);
						fmt.mapBits(BigInteger.valueOf(c0), vals[z], "input.alice.x[" + i + "]");
					}

					fmt.mapBits(BigInteger.valueOf(matrix[ibase+i][jbase]), vals[z], 
							"input.alice.dd_" + i + "_0_a");
					fmt.mapBits(BigInteger.valueOf(matrix[ibase][jbase+i]), vals[z], 
							"input.alice.dd_0_" + i + "_a");

					if (i>0) {
						matrix[ibase+i][jbase+BLOCK_SIZE] = rand.nextInt() & ((1<<N_BITS)-1);
						matrix[ibase+BLOCK_SIZE][jbase+i] = rand.nextInt() & ((1<<N_BITS)-1);

						fmt.mapBits(BigInteger.valueOf(matrix[ibase+i][jbase+BLOCK_SIZE]), vals[z], 
								"input.alice.out_" + i + "_" + BLOCK_SIZE + "_a");
						fmt.mapBits(BigInteger.valueOf(matrix[ibase+BLOCK_SIZE][jbase+i]), vals[z], 
								"input.alice.out_" + BLOCK_SIZE + "_" + i + "_a");
					}
				}
			}
			
			D("eval circuit");
			
			sfe.shdl.MultiProtocol.Alice calice = 
				new sfe.shdl.MultiProtocol.Alice(in, out, ccs);
			calice.go(vals, 
					new TreeSet<Integer>(aliceVars.who.keySet()),
					new TreeSet<Integer>(bobVars.who.keySet()));
		}		
	
		public void computeRecurrence(int ibase, int jbase) throws Exception {
			D("prepare circuit");

			Circuit circuit = CircuitParser.readFile("editdist/block_" + N_BITS + "_" + BLOCK_SIZE + "_" + BLOCK_SIZE + ".txt.Opt.circuit");
			TreeMap<Integer,Boolean> vals = new TreeMap<Integer,Boolean>();
			
			for (int i=0; i<=BLOCK_SIZE; ++i) {
				if (i<BLOCK_SIZE) {
					byte c0 = (byte) str1.charAt(ibase + i);
					fmt.mapBits(BigInteger.valueOf(c0), vals, "input.alice.x[" + i + "]");
				}
				
				fmt.mapBits(BigInteger.valueOf(matrix[ibase+i][jbase]), vals, 
						"input.alice.dd_" + i + "_0_a");
				fmt.mapBits(BigInteger.valueOf(matrix[ibase][jbase+i]), vals, 
						"input.alice.dd_0_" + i + "_a");
				
				if (i>0) {
					matrix[ibase+i][jbase+BLOCK_SIZE] = rand.nextInt() & ((1<<N_BITS)-1);
					matrix[ibase+BLOCK_SIZE][jbase+i] = rand.nextInt() & ((1<<N_BITS)-1);
					
					fmt.mapBits(BigInteger.valueOf(matrix[ibase+i][jbase+BLOCK_SIZE]), vals, 
							"input.alice.out_" + i + "_" + BLOCK_SIZE + "_a");
					fmt.mapBits(BigInteger.valueOf(matrix[ibase+BLOCK_SIZE][jbase+i]), vals, 
							"input.alice.out_" + BLOCK_SIZE + "_" + i + "_a");
				}
			}
		
			D("eval circuit");
			
			sfe.shdl.Protocol.Alice calice = new sfe.shdl.Protocol.Alice(in, out, circuit);
			calice.go(vals, 
					new TreeSet<Integer>(aliceVars.who.keySet()),
					new TreeSet<Integer>(bobVars.who.keySet()));
		}		
	}

	
	
	public static class Bob {
		ServerSocket listen;
		Socket alice;
		ObjectInputStream in_raw;
		ObjectInputStream in;
		ObjectOutputStream out_raw;
		ByteCountObjectOutputStream out;
		ByteCountOutputStreamSFE byteCount;
		long startTime;
		
		Domain domain;
		
		String str2;
		
		FmtFile fmt;
		
		int[][] matrix;
		
		Bob(int port, String str2) throws IOException {
			listen = new ServerSocket(port);
			alice = listen.accept();
			startTime = System.currentTimeMillis();
			byteCount = 
				new ByteCountOutputStreamSFE(
						alice.getOutputStream());
			out_raw = new ObjectOutputStream(byteCount);
			out_raw.flush();
			in_raw = new ObjectInputStream
			(new BufferedInputStream
					(alice.getInputStream()));
			
			this.str2 = str2;
		}
		
		public static void main(String[] args) throws Exception {
			int port = Integer.parseInt(args[0]);
			String str2 = args[1];

			new Bob(port, str2).go();
		}
		
		void go() throws Exception {
			int astrlen = in_raw.readInt();
			int bstrlen = str2.length();
			out_raw.writeInt(bstrlen);
			out_raw.flush();
			
			matrix = new int[astrlen+1][bstrlen+1];
			
			out = ByteCountObjectOutputStream.wrapObjectStream(this.out_raw);
			out.flush();
			in = new ObjectInputStream(this.in_raw);
			
			fmt = FmtFile.readFile("editdist/block_" + N_BITS + "_" + BLOCK_SIZE + "_" + BLOCK_SIZE + ".txt.Opt.fmt");
			

			if (!parallelize) {
				for (int i=0; i<astrlen; i+=BLOCK_SIZE) {
					for (int j=0; j<bstrlen; j+=BLOCK_SIZE) {
						computeRecurrence(i,j);
						out.reset();
					}
				}
			} else {
				ArrayList<int[]> diagonal = new ArrayList();
				for (int i=0; i<astrlen+bstrlen-1; i+=BLOCK_SIZE) {
					diagonal.clear();
					for (int j=0; j<bstrlen; j+=BLOCK_SIZE) {
						if (i-j>=0 && i-j<astrlen) {
							diagonal.add(new int[] {i-j, j});
						}
					}
					int[][] pairs = diagonal.toArray(new int[0][]);
					computeRecurrenceParallel(pairs);
				}
			}
			
			int aliceout = in.readInt();
		
			System.out.println("Alice out = " + aliceout);
			System.out.println("Bob out = " + matrix[astrlen][bstrlen]);
			System.out.println("Ans = " + (matrix[astrlen][bstrlen] ^ aliceout));
			System.out.println();
			
			System.out.println("Bob circuit wrote " + out.getCount() + " bytes");
			
			System.out.println();
			
			//System.out.println("state eval: " + zz);
			//out.writeObject(zz);
			//out.flush();
			// end DEBUG
			
		}
		
		void computeRecurrenceParallel(int[][] pairs) throws Exception {
			
			TreeMap<Integer,Boolean>[] vals = new TreeMap[pairs.length];
			boolean[][] vv = new boolean[pairs.length][];
			
			for (int z=0; z<pairs.length; ++z) {
				vals[z] = new TreeMap<Integer,Boolean>();
				int ibase = pairs[z][0];
				int jbase = pairs[z][1];

				for (int i=0; i<=BLOCK_SIZE; ++i) {
					if (i<BLOCK_SIZE) {
						byte c0 = (byte) str2.charAt(jbase + i);
						fmt.mapBits(BigInteger.valueOf(c0), vals[z], "input.bob.x[" + i + "]");
					}

					fmt.mapBits(BigInteger.valueOf(matrix[ibase+i][jbase]), vals[z], 
							"input.bob.dd_" + i + "_0_b");
					fmt.mapBits(BigInteger.valueOf(matrix[ibase][jbase+i]), vals[z], 
							"input.bob.dd_0_" + i + "_b");

				}

				vv[z] = new boolean[vals[z].size()];
				int vi=0;
				for (Boolean bb : vals[z].values()) {
					vv[z][vi] = bb;
					vi++;
				}
			}
			
			sfe.shdl.MultiProtocol.Bob cbob = new sfe.shdl.MultiProtocol.Bob(in, out, vv);
			cbob.go();
			
			for (int z=0; z<pairs.length; ++z) {
				int ibase = pairs[z][0];
				int jbase = pairs[z][1];
				for (int i=1; i<=BLOCK_SIZE; ++i) {
					BigInteger zz;
					zz = fmt.readBits(cbob.result[z], "output.bob.out_" + i + "_" + BLOCK_SIZE + "_b");
					matrix[ibase+i][jbase+BLOCK_SIZE] = zz.intValue();

					zz = fmt.readBits(cbob.result[z], "output.bob.out_" + BLOCK_SIZE + "_" + i + "_b");
					matrix[ibase+BLOCK_SIZE][jbase+i] = zz.intValue();
				}
			}
		}
		
		void computeRecurrence(int ibase, int jbase) throws Exception {
	
			TreeMap<Integer,Boolean> vals = new TreeMap<Integer,Boolean>();
			
			for (int i=0; i<=BLOCK_SIZE; ++i) {
				if (i<BLOCK_SIZE) {
					byte c0 = (byte) str2.charAt(jbase + i);
					fmt.mapBits(BigInteger.valueOf(c0), vals, "input.bob.x[" + i + "]");
				}
				
				fmt.mapBits(BigInteger.valueOf(matrix[ibase+i][jbase]), vals, 
						"input.bob.dd_" + i + "_0_b");
				fmt.mapBits(BigInteger.valueOf(matrix[ibase][jbase+i]), vals, 
						"input.bob.dd_0_" + i + "_b");
				
			}
			
			boolean[] vv = new boolean[vals.size()];
			int vi=0;
			for (Boolean bb : vals.values()) {
				vv[vi] = bb;
				vi++;
			}
			sfe.shdl.Protocol.Bob cbob = new sfe.shdl.Protocol.Bob(in, out, vv);
			cbob.go();
			
			for (int i=1; i<=BLOCK_SIZE; ++i) {
				BigInteger zz;
				zz = fmt.readBits(cbob.result, "output.bob.out_" + i + "_" + BLOCK_SIZE + "_b");
				matrix[ibase+i][jbase+BLOCK_SIZE] = zz.intValue();
				
				zz = fmt.readBits(cbob.result, "output.bob.out_" + BLOCK_SIZE + "_" + i + "_b");
				matrix[ibase+BLOCK_SIZE][jbase+i] = zz.intValue();
			}
	
		}
	}
}
