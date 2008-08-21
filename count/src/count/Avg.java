package count;

public class Avg {
	double num = 0;
	double den = 0;
	/*
	public void add(double x) {
		num += x;
		den += 1;
	}
	*/
	public void add(double x, double w) {
		num += x*w;
		den += w;
	}
	
	public double avg() {
		return num/den;
	}
	
	/*
	double num = 0;
	double den = 0;
	
	public void add(double x) {
		num += Math.log(x);
		den += 1;
	}
	
	public void add(double x, double w) {
		//System.out.println("avg :  " + x + "  " + w);
		num += w*Math.log(x);
		den += w;
	}
	
	public double avg() {
		return Math.exp(num/den);
	}
	*/
	
}
