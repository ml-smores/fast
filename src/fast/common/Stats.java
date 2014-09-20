package fast.common;
import java.util.List;


public final class Stats
{

	public static double sum (List<Double> a)
	{	
	    double sum = 0;
		if (a.size() > 0) 
	        for (Double i : a) 
	            sum += i;
	    return sum;
	}

	public static double mean (List<Double> accuracy)
	{
	    double sum = sum(accuracy);
	    double mean = 0;
	    mean = sum / (accuracy.size() * 1.0);
	    return mean;
	}

	public static double sd (List<Double> a)
	{
	    double sum = 0;
	    double mean = mean(a);
	
	    for (Double i : a)
	        sum += Math.pow((i - mean), 2);
	    return Math.sqrt( sum / ( a.size() - 1.0 ) ); // sample
	}

}
