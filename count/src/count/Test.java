package count;

import java.math.BigInteger;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

public class Test {

	static void printBits(byte[] h) {
		for (int i=0; i<h.length; ++i) {
			String b = Integer.toBinaryString(((int)h[i])&0xff);
			while (b.length()<8) {
				b = "0"+b;
			}
			System.out.print(b);
		}
		System.out.println();
	}
	
	public static void main(String[] args) {
		if (System.getProperty("SINGLE")!=null) {
			main1(args);
			return;
		}
		
		main2(args);
	}

	public static void main2(String[] args) {
		int trials = 10000;
		Counter[] bitmaps = new Counter[trials];
		for (int i=0; i<trials; ++i) {
			Counter bitmap = null;
			if (args[0].equals("virtual")) {  
				VirtualBitmap vb = new VirtualBitmap();
				vb.init(1024,1024);
				bitmap = vb;
			} else if (args[0].equals("segment")) {
				SegmentBitmap sb = new SegmentBitmap();
				sb.init(9, "");
				bitmap = sb;
			} else if (args[0].equals("segmentinc")) {
				SegmentBitmap sb = new SegmentBitmap();
				//sb.init(9, "");
				sb.init(6, "");
				sb.incremental = true;
				bitmap = sb;
			} else if (args[0].equals("layer")) {
				LayeredBitmap lb = new LayeredBitmap();
				lb.init();
				bitmap = lb;
			} else if (args[0].equals("layersegment")) {
				LayerSegmentBitmap lsb = new LayerSegmentBitmap();
				//lsb.k = 9;
				lsb.k = 6;
				lsb.init();
				bitmap = lsb;
			}
			
			bitmaps[i] = bitmap;
		}
		
		System.err.println("GO!");
		//int max = bitmaps[0].capacity();
		int max=1000000;
		for (int num=0; num<max; ++num) {
			double sum=0;
			double sumsq=0;
			int badtrials = 0;
			for (int rand=0; rand<trials; ++rand) {
				
				byte[] randb = BigInteger.valueOf(rand).toByteArray();
				key = new SecretKeySpec(randb, "HmacSHA1");
				
				//bitmap.clear();
				Counter bitmap = bitmaps[rand];
				
				
				//for (int i=0; i<num; ++i)
				if (num>0)
				{
					int i=num;
					byte[] h = hash(i);
					//printBits(h);
					bitmap.set(h);
					if (REPEAT_EPOCH && bitmap.isNewEpoch()) {
						//System.out.println("epoch change @" +i);
						for (i=0; i<num; ++i) {
							bitmap.set(hash(i));
						}
					}
				}
				
				if (doOutput(num)) {
					double est = bitmap.count();
					if (Double.isNaN(est)) {
						System.out.println("mistrial  n=" + num+"  rand="+rand);
						++badtrials;
					} else {
						sum += est;
						sumsq += est*est;
					}
					//System.out.println(num+" :  " + bitmap.count());
				}
			}
			
			//ystem.out.println(num);
			
			if (doOutput(num)) {
				double xbar = sum/(trials-badtrials);
				double x2bar = sumsq/(trials-badtrials);
				double sig = Math.sqrt(x2bar - xbar*xbar);
				System.out.println(num+" :  " + xbar + "   " + sig);
				System.out.flush();
			}
		}
	}
	
	static boolean doOutput(int n) {
		if (n<10)
			return true;
		if (n<100)
			return n%10==0;
		if (n<1000)
			return n%100==0;
		if (n<10000)
			return n%1000==0;
		if (n<100000)
			return n%10000==0;
		if (n<1000000)
			return n%100000==0;
		if (n<10000000)
			return n%1000000==0;
		return false;
	}
	private static final boolean REPEAT_EPOCH = System.getProperty("RE")!=null;
	
	public static void main1(String[] args) {
		for (int num=69; num<70; ++num) {
			//VirtualBitmap bitmap = new VirtualBitmap();
			//bitmap.init(64, 64);
			//SegmentBitmap bitmap = new SegmentBitmap();
		//	bitmap.init();
			//LayeredBitmap bitmap = new LayeredBitmap();
			//bitmap.init();
			LayerSegmentBitmap bitmap = new LayerSegmentBitmap();
			//bitmap.k = 6;
			bitmap.init();

			for (int i=0; i<num; ++i) {
				byte[] h = hash(i);
				//printBits(h);
				bitmap.set(h);
				if (bitmap.isNewEpoch()) {
					System.out.println("new epoch @ "+(i-1));
					if (REPEAT_EPOCH){
						i=-1;
					}
				}
			}
			double est = bitmap.count();
			bitmap.printme();
			System.out.println(num+" :  " + est);
		}
	}
	
	static byte[] hash(int n) {
		return hmac(n);
	}
	
	static byte[] sha(int n) {
		MessageDigest md = null;
		try {
			md = MessageDigest.getInstance("SHA-1");
		} catch (NoSuchAlgorithmException e) {
			
			e.printStackTrace();
		}
		//md.reset();
		byte[] dig = md.digest(BigInteger.valueOf(n).toByteArray());
		return dig;
	}
	
	static {
		//int rand = 940;
		//int rand = (int) (Math.random()*10000);
		int rand=0;
		System.err.println("random seed = " + rand);
		byte[] randb = BigInteger.valueOf(rand).toByteArray();
		key = new SecretKeySpec(randb, "HmacSHA1");
	}
	static SecretKeySpec key;
	
	static byte[] hmac(int n) {
		Mac mac = null;
		try {
			mac = Mac.getInstance("HmacSHA1");
			mac.init(key);
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		} catch (InvalidKeyException e) {
			e.printStackTrace();
		}
		
		byte[] dig = mac.doFinal(BigInteger.valueOf(n).toByteArray());
		return dig;
		
	}
	

}
