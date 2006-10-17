// Bob.java - Bob's part of the 2-party SFE protocol.
// Copyright (C) 2004 Dahlia Malkhi, Yaron Sella.
// See full copyright license terms in file ../GPL.txt

package fairplay.BOAL;

import java.io.*;
import java.util.*;
import java.util.regex.*;


//---------------------------------------------------------------   

/**
 * This class implements Bob - the sender in the two-party
 * SFE protocl.
 *
 * @author Louis Kruger
 */
public class BobLib {
    private static final MyLogger logger = new MyLogger();
    Formatter f;

    //---------------------------------------------------------------   

    /**
     * Bob Constructor
     *
     * @param circuit_filename - circuit filename
     * @param fmt_filename - format filename
     * @param sot_type - type of OT to perform (String)
     */
    public BobLib(String circuit_filename, String fmt_filename, String sseed, 
		 ObjectInputStream fromAlice, ObjectOutputStream toAlice, 
                 String[] bobArgs, String sot_type)
        throws IOException {
        int i, j;
        int num_of_circuits;
        int ot_type = Integer.parseInt(sot_type);
	int cc_num;
        int[] bob_io_size = new int[2];
        Parser p = null; // the parser and builder of the circuit
        OT ot;
        Vector bob_results;

        // Preparations
        MyUtil.init(sseed);
	MyUtil.WRITE_MODE=MyUtil.MODE_SETUP;
        MyUtil.sendInt(toAlice, ot_type, true);
        ot = new OT(ot_type);
        int aint = MyUtil.receiveInt(fromAlice);
	num_of_circuits = aint & 0xff ;

        Vector vEncPayload = new Vector (num_of_circuits);
        Vector vSecPayload = new Vector (num_of_circuits);
        byte[] EncPayload;
        byte[] SecPayload;
        byte[] InpPayload;
        byte[] OutPayload;
        int EncPayloadSize=0;
        int SecPayloadSize=0;
        int InpPayloadSize;
        int OutPayloadSize;

        Circuit c;

        for (i = 0; i < 1; i++) {
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
                logger.error("Bob: cannot open/close " + fmt_filename + " - " +
                    e.getMessage());
            } catch (FormatterError e) {
                logger.error("Bob: parsing " + fmt_filename + " failed.");
            } catch (Exception e) {
                logger.error("Bob: exception - " + e.getMessage());
            }

            // Parse circuit file
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
                logger.error("Bob: cannot open/close " + circuit_filename +
                    " - " + e.getMessage());
                System.exit(1);
            } catch (Exception e) {
                logger.error("Bob: exception - " + e.getMessage());
                System.exit(1);
            }

            c = p.getCircuit();        // Obtain a circuit object
	    f.markIO (c, bob_io_size); // Mark its inputs & outputs
            InpPayloadSize = bob_io_size[0];
            OutPayloadSize = bob_io_size[1];

	    // Run the SFE protocol
	    // ====================

	    MyUtil.WRITE_MODE=MyUtil.MODE_CIRCUIT;
            // Repeatedly encrypt circuit, extract data from it and save
	    for (j = 0; j < num_of_circuits; j++) {

               c.generateEncCircuit(); // Encrypt it

               if (j == 0) { // Compute sizes only on first iteration
                  EncPayloadSize = c.cmeasureEncPayload();
                  SecPayloadSize = c.cmeasureSecPayload();
               }

               EncPayload = c.cextractEncPayload(EncPayloadSize);
               vEncPayload.add (EncPayload);
               MyUtil.sendBytes(toAlice, EncPayload, false);
               SecPayload = c.cextractSecPayload(SecPayloadSize);
               vSecPayload.add (SecPayload);
	    }
	    toAlice.flush();

	    // Receive cc_num from Alice
            cc_num = MyUtil.receiveInt(fromAlice);

            // Send encrypted circuits secrets (except the chosen one) to Alice
	    for (j = 0; j < num_of_circuits; j++) {
               if (j != cc_num) {
                  SecPayload = (byte[]) vSecPayload.elementAt(j);
                  MyUtil.sendBytes (toAlice, SecPayload, false);
	       }
	    }
	    toAlice.flush();

            // Upload the secrets of the chosen circuit
            SecPayload = (byte[]) vSecPayload.elementAt(cc_num);
            c.cinjectSecPayload(SecPayload);

	    // Read Bob's inputs + update circuit accordingly
            //f.getBobInput(c, br); 
	    f.getInput(c, false, bobArgs);

            // Send Bob's inputs (garbled) to Alice
            InpPayload = f.fextractInpPayload(c, InpPayloadSize, false);
            MyUtil.sendBytes(toAlice, InpPayload, true);

            // OTs - Bob is the sender
	    MyUtil.WRITE_MODE=MyUtil.MODE_OT;
            OT.SenderOTs(c, f, ot, toAlice, fromAlice);

	    // Get Bob garbled results from Alice & print them
            OutPayload = new byte[OutPayloadSize];
            MyUtil.receiveBytes (fromAlice, OutPayload, OutPayloadSize);
            f.finjectOutPayload (c, OutPayload, false);

            outputs = f.getOutputVals(c, false); 
        }

    }
    public long[] outputs;

    //---------------------------------------------------------------

    /**
     * This routine is for debugging socket communication
     */
    public void pingpong(ObjectOutputStream toAlice,
        ObjectInputStream fromAlice, int a) {
        System.out.println("Attempting to read num from Alice");

        int u = MyUtil.receiveInt(fromAlice);
        System.out.println("Got Int from Alice " + u);
        System.out.println("Sending " + a + " to Alice");
        MyUtil.sendInt(toAlice, a, true);
    }

    //---------------------------------------------------------------
 
    public static void bobUsage(int err_code) {
        System.out.println("Bob activation error code = " + err_code);
        System.out.println("Usage: java SFE.BOAL.Bob -e|-c[n]|-r[n] <filename> <seed> <ot_type>");
        System.out.println(" -e = EDIT, -c = COMPILE, -r = RUN, [n] = NoOpt)");
        System.out.println(" (<seed>, <ot_type> expected only with -r[n])") ;
        System.out.println(" Examples: 1. java SFE.Bob -c Maximum.txt");
        System.out.println("           2. java SFE.Bob -r Maximum.txt bQ91:d_aV!|l 4");
        System.exit(1);
    }

    //---------------------------------------------------------------

    /**
     * Main program for activating Bob
     *
     * @param args - command line arguments.
     *               args[0] should be -e, -c[n], -r[n]
     *               args[1] should be filename
     *               args[2] should be ot_type (only with -r[n])
     */
    /*
    public static void main(String[] args) throws Exception {
        String filename;
        String circ_fname;
        String fmt_fname;
	boolean edit = false;
	boolean compile = false;
	boolean run = false;
	boolean opt = false;

        // Load logging configuration file
	PropertyConfigurator.configure(MyUtil.pathFile("SFE_logcfg.lcf"));

	// Various legality tests on command line parameters

        if ((args.length < 2) || (args.length > 4))
            bobUsage(1);

	edit = args[0].equals("-e");
	compile = args[0].equals("-c") || args[0].equals("-cn");
	run = args[0].equals("-r") || args[0].equals("-rn");
	opt = args[0].equals("-r") || args[0].equals("-c");

        filename = new String(args[1]);
	if (opt) {
           circ_fname = new String(filename + ".Opt.circuit");
           fmt_fname = new String(filename + ".Opt.fmt");
	}
	else {
           circ_fname = new String(filename + ".NoOpt.circuit");
           fmt_fname = new String(filename + ".NoOpt.fmt");
	}

         if (run) {
             File f1 = new File(circ_fname);
             File f2 = new File(fmt_fname);

             if (!f1.exists() || !f2.exists()) {
                if (!f1.exists())
                   System.out.println("Input file " + circ_fname + " not found");
                if (!f2.exists())
                   System.out.println("Input file " + fmt_fname + " not found");
                bobUsage(5);
	     }
         }

         // Do something (finally...)

         if (run) {
            System.out.println("Running Bob...");
            try {
                Bob b = new Bob(circ_fname, fmt_fname, args[2], args[3]);
            } catch (Exception e) {
                System.out.println("Bob's main err: " + e.getMessage());
                e.printStackTrace();
            }
         }
    }
    */
}
