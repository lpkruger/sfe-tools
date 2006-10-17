package sfe.editdist;

public class Test {
	public static void main(String[] args) {
		String s1 = args[0];
		String s2 = args[1];
		
		System.out.println("Dist: " + dist(s1, s2));
	}
	
	public static int dist(String s1, String s2) {
		int[][] matrix = new int[s1.length()+1][s2.length()+1];
		
		for (int i=0; i<s1.length(); ++i) {
			matrix[i][0] = i;
		}
		for (int j=0; j<s2.length(); ++j) {
			matrix[0][j] = j;
		}
		for (int i=1; i<=s1.length(); ++i) {
			for (int j=1; j<=s2.length(); ++j) {
				int xy = matrix[i-1][j-1] + (s1.charAt(i-1) == s2.charAt(j-1) ? 0 : 1);
				int xx = matrix[i][j-1] + 1;
				int yy = matrix[i-1][j] + 1;
				matrix[i][j] = Math.min(xy, Math.min(xx, yy));
			}
		}
		
		return matrix[s1.length()][s2.length()];
	}
}
