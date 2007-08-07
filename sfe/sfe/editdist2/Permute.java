package sfe.editdist2;

import java.util.Random;

public class Permute {
	public static void permute(Object[] array, Random rand) {
		for (int i=0; i<array.length; ++i) {
			int j = rand.nextInt(array.length);
			if (j!=i) {
				Object tmp = array[i];
				array[i] = array[j];
				array[j] = tmp;
			}
		}
	}
	
	// convenience: permute multiple arrays
	public static void permuteSeveral(Object[][] array, Random rand) {
		for (int i=0; i<array[0].length; ++i) {
			int j = rand.nextInt(array[0].length);
			if (j!=i) {
				for (int k=0; k<array.length; ++k) {
					Object tmp = array[k][i];
					array[k][i] = array[k][j];
					array[k][j] = tmp;
				}
			}
		}
	}
}
