/*
 * Created on Oct 1, 2003
 *
 * To change the template for this generated file go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
package sfe.eslcomp;

import java.io.*;

/**
 * @author lpkruger
 *
 * To change the template for this generated type comment go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */


public class TestLogFn {

	public static void main(String[] args) throws IOException {
		String edlfile = args[0];
		String hfile = args[1];
		//String eslfile = args[2];
		
		//FileReader fr = new FileReader("c:\\lpk\\sm\\test.edl");
		FileReader fr = new FileReader(edlfile);
		EDLParser p = new EDLParser(new BufferedReader(fr), edlfile);
		p.parse();
		EDL edl = p.edl;
		//		edl.dump();
		PrintStream os;
		if (hfile.equals("-")) {
			os = System.out;
		} else {
			os = new PrintStream(new FileOutputStream(hfile));
		}
		//EDLCHeader ech = new EDLCHeader(edl, System.out);
		EDLLogFnOutput.genLogRoutines(edl, os);
		
		os.close();

	}

	
}