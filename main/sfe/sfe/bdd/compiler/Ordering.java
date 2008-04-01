package sfe.bdd.compiler;

public class Ordering {
	public static int[] pattern(int max, int[] pat) {
		int per = pat.length;
		int[] ret = new int[max+1];
		
		int pos = 0;
		boolean progress = true;
		while (pos <= max && progress) {
			progress = false;
			int lbase = findFirst(ret, pos);
			int hbase = findLast(ret, max, pos);
			for (int i=0; i<pat.length; ++i) {
				if (pos > max) break;
				if (pat[i]>=0 && lbase + pat[i] <= max &&
						!find(ret, lbase + pat[i], pos)) {
					ret[pos++] = lbase + pat[i];
					progress = true;
				}
				if (pat[i]<0 && 1 + hbase + pat[i] <= max &&
						!find(ret, 1 + hbase + pat[i], pos)) {
					ret[pos++] = 1 + hbase + pat[i];
					progress = true;
				}
			}
		}
		while (pos <= max) {
			int n = findFirst(ret, pos);
			ret[pos++] = n;
		}
		return ret;
	}
	
	static int findFirst(int[] ret, int len) {
		boolean[] mark = new boolean[len];
		for (int i=0; i<len; ++i) {
			if (ret[i]<len)
				mark[ret[i]] = true;
		}
		for (int i=0; i<len; ++i) {
			if (!mark[i])
				return i;
		}
		return len;
	}
	
	static int findLast(int[] ret, int max, int len) {
		boolean[] mark = new boolean[len];
		for (int i=0; i<len; ++i) {
			if (max-ret[i]<len)
				mark[max-ret[i]] = true;
		}
		for (int i=0; i<len; ++i) {
			if (!mark[i])
				return max-i;
		}
		return max-len;
	}
	
	static boolean find(int[] ret, int num, int len) {
		for (int i=0; i<len; ++i) {
			if (ret[i] == num)
				return true;
		}
		return false;
	}
	
	
	public static void main(String[] args) {
		int max = Integer.parseInt(args[0]);
		int[] pat = new int[args.length - 1];
		for (int i=1; i<args.length; ++i) {
			pat[i-1] = Integer.parseInt(args[i]);
		}
		int[] ord = pattern(max, pat);
		for (int i=0; i<ord.length; ++i) {
			System.out.print(ord[i] + " ");
		}
		System.out.println();
	}
}
