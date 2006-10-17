/*
 * Created on Apr 20, 2003
 *
 * To change the template for this generated file go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */

package sfe.eslcomp;
import java.io.BufferedReader;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintStream;

/**
 * @author lpkruger
 *
 * To change the template for this generated type comment go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
public class TestEDL {

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
		EDLCHeader ech = new EDLCHeader(edl, os);
		ech.emit();
		os.close();

		if (args.length > 2) {
			String test = args[2];

			os = new PrintStream(new FileOutputStream(test));
			EDLLogFnOutput.genLogRoutines(edl, os);
			os.close();
		}
	}
}
