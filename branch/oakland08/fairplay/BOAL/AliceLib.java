// Alice.java - Alice's part of the 2-party SFE protocol. 
// Copyright (C) 2004 Dahlia Malkhi, Yaron Sella. 
// See full Copyright license terms in file ../GPL.txt

package fairplay.BOAL;

import java.io.*;
import java.util.*;
import java.util.regex.*;


//---------------------------------------------------------------

/**
 * This class implements Alice - the chooser in the two-party
 * SFE protocol.
 *
 * @author Louis Kruger
 */
public class AliceLib {
    private static final MyLogger logger = new MyLogger();
    private static final int num_of_circuits = 1;
    Formatter f = null;

    //---------------------------------------------------------------

    /**
     * Alice Constructor
     *
     * @param circuit_filename - circuit filename
     * @param fmt_filename - format filename
     * @param hostname - where to find Bob
     * @param stats - print run statistics in the end
     */
    public AliceLib(String circuit_filename, String fmt_filename, String sseed,
		 ObjectInputStream fromBob, ObjectOutputStream toBob, 
		 String[] aliceArgs, boolean stats) throws Exception {

        int i, j;
        int ot_type;
	int cc_num;
        int[] bob_io_size = new int[2];
	Parser p = null;
        OT ot;
        Vector bob_results;
	long sum1=0, sum2=0, sum3=0, sum4=0;

        // Preparations
        MyUtil.init(sseed);
	MyUtil.WRITE_MODE = MyUtil.MODE_SETUP;
        MyUtil.sendInt(toBob, num_of_circuits, true);
        ot_type = MyUtil.receiveInt(fromBob);
        ot = new OT(ot_type);

        Vector vEncPayload = new Vector (num_of_circuits);
        byte[] EncPayload;
        byte[] SecPayload;
        byte[] InpPayload;
        byte[] OutPayload;
        int EncPayloadSize=0;
        int SecPayloadSize=0;
        int InpPayloadSize;
        int OutPayloadSize;
        Circuit c;

        for (i = 0; i < 1; i++) {  // num iterations

            MyUtil.deltaTime (true);

            logger.info("Iteration no = " + i);

            // Parse the IOformat file and prepare the inputs
            try {
                // Preparations
                FileReader fmt = new FileReader(fmt_filename);
                StreamTokenizer fmtst = new StreamTokenizer(fmt);

                // IO Formatting
                f = new Formatter(fmtst);
                f.parse();

                // Cleanup
                fmt.close();
            } catch (IOException e) {
                logger.error("Alice: cannot open/close " + fmt_filename + " - " +
                    e.getMessage());
            } catch (FormatterError e) {
                logger.error("Alice: parsing " + fmt_filename + " failed.");
            } catch (Exception e) {
                logger.error("Alice: exception - " + e.getMessage());
            }

            // Parse the circuit file
            try {
                // Preparations
                FileReader fr = new FileReader(circuit_filename);
                StreamTokenizer st = new StreamTokenizer(fr);

                // Parsing
                p = new Parser(st);
                p.parse();

                // Cleanup
                fr.close();
            } catch (IOException e) {
                logger.error("Alice: cannot open/close " + circuit_filename +
                    " - " + e.getMessage());
                System.exit(1);
            } catch (Exception e) {
                logger.error("Alice: exception - " + e.getMessage());
                System.exit(1);
            }

            c = p.getCircuit();        // Obtain a circuit object
	    f.markIO (c, bob_io_size); // Mark its inputs & outputs
            InpPayloadSize = bob_io_size[0];
            OutPayloadSize = bob_io_size[1];
            c.generateEncCircuit();    // Encrypt it (dummy)
            EncPayloadSize = c.cmeasureEncPayload();
            SecPayloadSize = c.cmeasureSecPayload();

            sum1 += MyUtil.deltaTime (false);

	    // Run the SFE protocol
	    // ====================

	    // Receive encrypted circuits payload from Bob
            for (j = 0; j < num_of_circuits ; j++) {
               EncPayload = new byte[EncPayloadSize];
               MyUtil.receiveBytes (fromBob, EncPayload, EncPayloadSize);
               vEncPayload.add (EncPayload);
            }

	    // Choose a circuit to evaluate and tell Bob
	    cc_num = MyUtil.randomByte() ;
	    if (cc_num < 0) cc_num += 256 ;
	    cc_num = cc_num % num_of_circuits;
            logger.debug("Alice: chose circuit number " + cc_num + " for evaluation");

	    MyUtil.WRITE_MODE = MyUtil.MODE_CIRCUIT;
            MyUtil.sendInt(toBob, cc_num, true);

            // Receive encrypted circuits with secrets
	    // (except the chosen one) from Bob
            for (j = 0; j < num_of_circuits ; j++) {
               if (j != cc_num) {
                  EncPayload = (byte[]) vEncPayload.elementAt(j);
                  c.cinjectEncPayload (EncPayload);
                  SecPayload = new byte[SecPayloadSize];
                  MyUtil.receiveBytes (fromBob, SecPayload, SecPayloadSize);
                  c.cinjectSecPayload (SecPayload);
		  if (!c.isCorrect()) {
                     logger.error("Alice: caught Bob cheating!");
                     System.exit(1);
		  }
	       }
	    }

	    // Receive Bob's inputs for the chosen circuit and place them in it
            InpPayload = new byte[InpPayloadSize];
            MyUtil.receiveBytes (fromBob, InpPayload, InpPayloadSize);
            f.finjectInpPayload (c, InpPayload, false);

            sum2 += MyUtil.deltaTime (false);

	    // Read Alice's inputs 
            //f.getAliceInput(c, br); 
	    f.getInput(c, true, aliceArgs);

	    // OTs - Alice is the chooser + 
	    // place Alice's inputs in the chosen circuit
	    MyUtil.WRITE_MODE = MyUtil.MODE_OT;
            OT.ChooserOTs(c, f, ot, toBob, fromBob);

            sum3 += MyUtil.deltaTime (false);

	    MyUtil.WRITE_MODE = MyUtil.MODE_EVAL;

            c.evalGarbledCircuit(true, false);
            logger.info("circuit evaluation completed!");

	    // Send Bob his garbled results
	    OutPayload = f.fextractOutPayload (c, OutPayloadSize, false);
            MyUtil.sendBytes (toBob, OutPayload, true);

	    // print Alice's output
            outputs = f.getOutputVals(c, true); 

            sum4 += MyUtil.deltaTime (false);
        } // end of iteration

	if (stats) {
           System.out.println("Initial calculations   [sum1] = " + (float)sum1/1000.0);
           System.out.println("Circuits communication [sum2] = " + (float)sum2/1000.0);
           System.out.println("Oblivious Transfers    [sum3] = " + (float)sum3/1000.0);
           System.out.println("Evaluation & output    [sum4] = " + (float)sum4/1000.0);
	}
    }
    public long[] outputs;


    //---------------------------------------------------------------

    /**
     * This routine is for debugging socket communication
     */
    public void pongping(ObjectOutputStream toBob, ObjectInputStream fromBob,
        int a) {
        System.out.println("Sending " + a + " to Bob");
        MyUtil.sendInt(toBob, a, true);
        System.out.println("Attempting to read num from Bob");

        int u = MyUtil.receiveInt(fromBob);
        System.out.println("Got Int from Bob " + u);
    }

    //---------------------------------------------------------------
 
    public static void aliceUsage(int err_code) {
        System.out.println("Alice activation error code = " + err_code);
        System.out.println("Usage: java SFE.BOAL.Alice -e|-c[n]|-r[n] <filename> <seed> <hostname> <num_iterations>");
        System.out.println(" -e = EDIT, -c = COMPILE, -r = RUN, [n] = NoOpt)");
        System.out.println(" (<seed> <hostname>, <num_iterations> expected only with -r[n])");
        System.out.println(" Examples: 1. java SFE.Alice -c Maximum.txt");
        System.out.println("           2. java SFE.Alice -r Maximum.txt Xb@&5H1m!p sands 100");
        System.exit(1);
    }

    //---------------------------------------------------------------

    /*
    public static void main(String[] args) throws Exception {
        String filename;
        String circ_fname;
        String fmt_fname;
	int num_iterations;
        boolean edit = false;
        boolean compile = false;
	boolean run_stats = false;
	boolean run = false;
	boolean opt = false;

	if (opt) {
           circ_fname = new String(filename + ".Opt.circuit");
           fmt_fname = new String(filename + ".Opt.fmt");
	}
	else {
           circ_fname = new String(filename + ".NoOpt.circuit");
           fmt_fname = new String(filename + ".NoOpt.fmt");
	}

            System.out.println("Running Alice...");
            try {
                if (args.length < 5)
                   num_iterations = 1 ;
		else
                   num_iterations = Integer.parseInt(args[4]);
                Alice a = new Alice(circ_fname, fmt_fname, args[2], args[3], num_iterations, run_stats);
            } catch (Exception e) {
                System.out.println("Alice's main err: " + e.getMessage());
                e.printStackTrace();
            }
	 }
    }
    */
}
