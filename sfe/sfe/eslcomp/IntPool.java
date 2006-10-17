/*
 * Created on Sep 25, 2003
 *
 * To change the template for this generated file go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
package sfe.eslcomp;

import java.util.*;

/**
 * @author lpkruger
 *
 * To change the template for this generated type comment go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
public class IntPool {
	private static HashMap<Integer, Integer> ints = new HashMap<Integer, Integer>();
	public static synchronized Integer create(int n) {
		Integer j = new Integer(n);
		Integer i = ints.get(j);
		if (i == null) {
			ints.put(j, j);
			return j;
		} else {
			return i;
		}
	}
}
