/*
 * Created on Jun 29, 2003
 *
 * To change the template for this generated file go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
package sfe.sfdl;

import java.io.*;

/**
 * @author lpkruger
 *
 * To change the template for this generated type comment go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
public class TestESL {

	public static void main(String[] args) throws IOException {
		String eslfile = args[0];
		String cfile = args[1];
		
		FileReader fr = new FileReader(eslfile);
		SFDLParser p = new SFDLParser(new BufferedReader(fr), eslfile);
		p.parse();
		new ESLParser2(p).process();
		SFDL esl = p.esl;
		//		edl.dump();
		PrintStream os;
		if (cfile.equals("-")) {
			os = System.out;
		} else {
			os = new PrintStream(new FileOutputStream(cfile));
		}
		//EDLCHeader ech = new EDLCHeader(edl, System.out);
		ESLCFile ecf = new ESLCFile(esl, os);
		ecf.emit();
		os.close();

	}

}
