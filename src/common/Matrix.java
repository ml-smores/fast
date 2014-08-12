package common;

import java.util.Arrays;

public final class Matrix
{
	
	public static final double EPS = 0.0000001;

	public static double max(double array[])
	{
		double max = Double.MIN_VALUE;
		for (double a : array)
			if (a > max)
				max = a;
		return max;
	}
	public static int argmax(double[] array)
	{
		int    index  = -1;
		double best   = Double.MIN_VALUE;
		for (int i = 0; i < array.length; i++)
		{
			//System.out.print( array[i]);
			if (array[i] > best)
			{
				best = array[i];
				index = i;
				
				//System.out.println("*");
			}
			/*else
				System.out.println("");*/
		}
		return index;
	}
	public static double[] add(double[] a, double[] b) 
	{
		if( a.length != b.length )
			throw new ArithmeticException("Attempting to add arrays of different lengths (" +  a.length + "," + b.length +  ")");		
		double c[] = new double[a.length];
		for (int i = 0; i < a.length; i++) 
		{
			c[i] = a[i] +  b[i];
		}
		return c;
	}
	
	public static double[] add(double[] a, int[] b) 
	{
		if( a.length != b.length )
			throw new ArithmeticException("Attempting to add arrays of different lengths (" +  a.length + "," + b.length +  ")");
		double c[] = new double[a.length];
		for (int i = 0; i < a.length; i++) 
		{
			c[i] = a[i] +  b[i];
		}
		return c;
	}
	
	public static double[] mult(double[] a, double b)
	{
		final double  ans[] = new double[a.length];
		for (int i = 0; i < a.length; i++) 
		{
			ans[i] = a[i] * b;
		}
		return ans;
	}
		
	public static double[] dotmult(double[] a, double[] b, double c) 
	{
		if( a.length != b.length )
			throw new ArithmeticException("Attempting to multiply arrays of different lengths (" +  a.length + "," + b.length +  ")");

		final double  ans[] = new double[a.length];
		for (int i = 0; i < a.length; i++) 
		{
			ans[i] = a[i] * b[i] * c;
		}
		return ans;
	}


	
	public static double[] dotmult(double[] a, double[] b) 
	{
		if( a.length != b.length )
			throw new ArithmeticException("Attempting to multiply arrays of different lengths");

		final double  ans[] = new double[a.length];
		for (int i = 0; i < a.length; i++) 
			ans[i] = a[i] * b[i];
		return ans;
	}


	public static double[] add(int[] a, double b) 
	{
		double c[] = new double[a.length];
		for (int i = 0; i < a.length; i++) 
		{
			c[i] = a[i] +  b;
		}
		return c;
	}

	
	
	public static double[] add(double[] a, double b) 
	{
		double c[] = new double[a.length];
		for (int i = 0; i < a.length; i++) 
		{
			c[i] = a[i] +  b;
		}
		return c;
	}

	public static double sum(double[] vector)
	{
		double ans = 0;
		for (double e: vector)
			ans += e;
		return ans;
	}

	public static double sum(double[][] q, int obs, int dim)
	{
		double ans = 0;
		assert dim == 2: "Parameter value not implemented";

		for(int i = 0; i < q.length; i++)
			ans += q[i][obs];

		return ans;
	}

	

	public static double[] toDouble(String[] split)
	{
		double[] ans = new double[split.length];
		for(int i = 0; i < ans.length; i++)
			ans[i] = Double.valueOf(split[i]);
		return ans;
	}
	
	public static void assertProbability(double[] p )
	{
		final double sum = Math.abs((Matrix.sum(p) - 1));
		assert  sum > (-1*EPS) & sum < EPS : "Not a probability: " + Matrix.sum(p) + " " + Arrays.toString(p);
	}

	public static int[] addInt(int[] a, int[] b)
	{
		if( a.length != b.length )
			throw new ArithmeticException("Attempting to multiply arrays of different lengths");

		final int  ans[] = new int[a.length];
		for (int i = 0; i < a.length; i++) 
			ans[i] = a[i] + b[i];

		return ans;
	
	}

	public static double[] div(double[] p, double pNorm)
	{
		final double  ans[] = new double[p.length];
		for (int i = 0; i < p.length; i++) 
		{
			ans[i] = p[i] / pNorm;
		}
		return ans;
	}	
}
