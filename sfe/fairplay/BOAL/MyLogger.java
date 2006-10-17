package fairplay.BOAL;

final public class MyLogger {
    public void debug(String str) {
	//System.err.println("DEBUG: " + str);
    }
    
    public void info(String str) {
	//System.err.println("INFO: " + str);
    }

    public void warn(String str) {
	System.err.println("WARN: " + str);
    }

    private static boolean firsterror;

    public void error(String str) {
	System.err.println("ERROR: " + str);
	if (!firsterror) {
	    Thread.dumpStack();
	    firsterror = true;
	}
    }
}
