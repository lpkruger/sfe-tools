package sfe.js;

import org.mozilla.javascript.*;

import sfe.js.ProtocolComm.Party;
import sfe.js.ProtocolComm.Connector;
import sfe.js.ProtocolComm.Listener;

import java.io.*;

public class JS1 {

	static ContextFactory cfact = new ContextFactory();
	static ScriptableObject scope;
	void go(String[] args) throws IOException {
		
		Context cx = cfact.enterContext();
		cx.setLanguageVersion(Context.VERSION_1_7);
		//System.out.println("Using JS ver " + cx.getLanguageVersion());
		cx.setOptimizationLevel(-1);	// serialization fails without this
		
		//Scriptable scope = cx.initStandardObjects();
		//Scriptable scope = new ImporterTopLevel(cx);
		scope = new ImporterTopLevel(cx);
		scope.defineFunctionProperties(new String[] {"evalFile"}, JS1.class,
                ScriptableObject.DONTENUM);
		
		Party party=null;
		String partyName = "none";
		int argn=0;
		if(args[argn].equals("alice")) {
			party = new Connector(scope, "localhost", 2345);
			partyName=args[0];
			argn++;
		} else if (args[argn].equals("bob")) {
			party = new Listener(scope, 2345);
			partyName=args[0];
			argn++;
		} else {
			System.out.println("running in JS test mode");
		}
		
		String eval = "";
		for (int i=argn; i < args.length; i++) {
		    eval += args[i];
		}
		
		//Object wrappedOut = Context.javaToJS(System.out, scope);
		//ScriptableObject.putProperty(scope, "out", wrappedOut);

		Object wrappedParty = Context.javaToJS(party, scope);
		ScriptableObject.putProperty(scope, "commParty", wrappedParty);
		ScriptableObject.putProperty(scope, "commPartyName", partyName);
		
		try {
			BufferedReader r = new BufferedReader(new FileReader("init.js"));
			cx.evaluateReader(scope, r, "init.js", 1, null);
			r.close();
		} catch (IOException e) {
			e.printStackTrace();
			return;
		} catch (RhinoException err) {
			System.out.println(err.getMessage());
			return;
		}
		
		try {
			Object result = cx.evaluateString(scope, eval, "<cmd>", 1, null);
			if (result != Context.getUndefinedValue()) {
				System.out.println(Context.toString(result));
			}
		} catch (RhinoException err) {
			System.out.println(err.getMessage());
			return;
		}
	}
	public static void main(String[] args) throws IOException {
		
		new JS1().go(args);
	}
	
	public static Object evalFile(Context cx, Scriptable thisObj, Object[] args,
			Function funObj) throws Exception {

		String fname = Context.toString(args[0]);
		BufferedReader r = new BufferedReader(new FileReader(fname));
		Object ret = cx.evaluateReader(scope, r, fname, 1, null);
		r.close();
		return ret;
	}
}
