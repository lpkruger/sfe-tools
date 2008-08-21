package count;

//import flanagan.integration.*;

public class Harmonic {
	public static final double EULER_CONSTANT_GAMMA = 0.5772156649015627;
	static double[] values = new double[10000];
	static {
		double sum=0;
		for (int i=1; i<10000; ++i) {
			sum += 1.0/i;
			values[i]=sum;
			//System.out.println(sum + "  "+ln+"  "+(sum-ln));
		}
	}
	
	public static double value(int i) {
		if (i<values.length) {
			return values[i];
		}
		return Math.log(i)+EULER_CONSTANT_GAMMA;
	}
	
	public static double valuebz(int b, int z) {
		//if (z==0)
		//	return Double.NaN;
		return value(b)-value(z);
	}
	
	public static double valuebz_bad(int b, int z) {
		return Math.log(b/(double)z);
	}
	
	
/*
	static class IntFn implements IntegralFunction {
		double n;
		public double function(double x) {
			return (1.0-Math.pow(x, n))/(1.0-x);
		}
		IntFn(double n) {
			this.n = n;
		}
	}
	
	public static double valueigl(double n) {
		return Integration.trapezium(new IntFn(n), 0, 1, 10000);
	}
	public static void main(String[] args) {
		for (int i=1; i<20; ++i) {
			System.out.println(valueigl(i+0.5));
		}
	}
	*/
}
