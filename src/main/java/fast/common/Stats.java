package fast.common;
//import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;


public final class Stats
{
	
	public static class ValueIndexSummary{
		public ArrayList<Double> values;
		public ArrayList<Integer> indexes;
		
		public ValueIndexSummary(ArrayList<Double> values, ArrayList<Integer> indexes){
			this.values = values;
			this.indexes = indexes;
		}
		
		public String toString(){//NumberFormat nf) {
			String str = "";
			String delimiter = ",";
			if (values != null && indexes != null){
				for (int i = 0; i < values.size(); i++) {
					double value = values.get(i);
					int index = indexes.get(i);
					str += value +  "(" + index + ")" + delimiter;
				}
				str = str.substring(0, str.length() - delimiter.length());
			}
			return str;
		}
	}

	public static Double min (List<Double> a)
	{	
		if (a == null || a.size() == 0)
			return null;
	  Double min = null;
		if (a.size() > 0) 
	        for (int i = 0; i < a.size(); i++) {
	        	Double value = a.get(i);
	        	if (value == null)
	        		continue;
	        	if (i == 0 || min == null)
	        		min = value;
	        	else
	            min = (value < min)? value: min;
	        }
	    return min;
	}
	
	public static Double max (List<Double> a)
	{	
		if (a == null || a.size() == 0)
			return null;
	  Double max = null;
		if (a.size() > 0) 
	        for (int i = 0; i < a.size(); i++) {
	        	Double value = a.get(i);
	        	if (value == null)
	        		continue;
	        	if (i == 0 || max == null)
	        		max = value;
	        	else{
	            max = (value > max)? value: max;
	        	}
	        }
	    return max;
	}
	
	public static ValueIndexSummary max_with_index (List<Double> a)
	{	
		if (a == null || a.size() == 0)
			return null;
	   double max = 0.0;
	   int index = 0;
	   ArrayList<Double> maxes = new ArrayList<Double>();
	   ArrayList<Integer> indexes = new ArrayList<Integer>();
		if (a.size() > 0) 
	        for (int i = 0; i < a.size(); i++) {
	        	Double value = a.get(i);
	        	if (value == null)
	        		continue;
	        	if (i == 0){
	        		max = value;
	        		index = i;
        			maxes.add(max);
        			indexes.add(index);
	        	}
	        	else if (value > max){
	        			max = value;
	        			index = i;
	        			maxes = new ArrayList<Double>();
	        			indexes = new ArrayList<Integer>();
	        			maxes.add(max);
	        			indexes.add(index);
	        	}
	        	else if (value == max){
	        		maxes.add(value);
	        		indexes.add(i);
	        	}
	        }
		ValueIndexSummary maxObj = new ValueIndexSummary(maxes,indexes);
		return maxObj;
	}
	
	public static ValueIndexSummary min_with_index (List<Double> a)
	{	
		if (a == null || a.size() == 0)
			return null;
	   double min = 0.0;
	   int index = 0;
	   ArrayList<Double> mins = new ArrayList<Double>();
	   ArrayList<Integer> indexes = new ArrayList<Integer>();
		if (a.size() > 0) 
	        for (int i = 0; i < a.size(); i++) {
	        	Double value = a.get(i);
	        	if (value == null)
	        		continue;
	        	if (i == 0){
	        		min = value;
	        		index = i;
	        		mins.add(min);
        			indexes.add(index);
	        	}
	        	else if (value < min){
	        			min = value;
	        			index = i;
	        			mins = new ArrayList<Double>();
	        			indexes = new ArrayList<Integer>();
	        			mins.add(min);
	        			indexes.add(index);
	        	}
	        	else if (value == min){
	        		mins.add(value);
	        		indexes.add(i);
	        	}
	        }
		ValueIndexSummary minObj = new ValueIndexSummary(mins,indexes);
		return minObj;
	}
	
	public static Integer countLessThan (List<Double> a, int lessThanValue)
	{	
		if (a == null || a.size() == 0)
			return null;
	  Integer nb = null;
    for (int i = 0; i < a.size(); i++) {
    	Double value = a.get(i);
    	if (value == null)
    		continue;
    	if (value < lessThanValue){
    		if (nb == null)
    			nb = 0;
    		nb++;
    	}
    }
	   return nb;
	}
	
	public static Double sum (List<Double> a)
	{	
		if (a == null || a.size() < 1) 
			return null;
	  Double sum = null;
    for (Double i : a) 
    	 if (i != null){
    		 if (sum == null)
    			 sum = 0.0;
         sum += i;
    	 }
    return sum;
	}

	public static Double mean (List<Double> a)
	{
			if (a == null || a.size() < 1)
				return null;
	    Double sum = null;//sum(accuracy);
	    Double mean = null;
	    int nb = 0;
	    for (Double i : a) {
	    	 if (i != null && !Double.isNaN(i)){
	    		 if (sum == null)
	    			 sum = 0.0;
	         sum += i;
	         nb++;
	    	 }
	    }
	    if (sum != null && nb != 0)
	    	mean = sum / (nb * 1.0);
	    return mean;
	}

	/**
	* This formula is the corrected sample standard deviation, which is generally known simply as the "sample standard deviation", which is less biased than the uncorrected sample standard deviation (standard formula of variance taking square root)
	* sample sd: When only a sample of data from a population is available, the term standard deviation of the sample or sample standard deviation can refer to either the above-mentioned quantity as applied to those data or to a modified quantity that is a better estimate of the population standard deviation (the standard deviation of the entire population).
	* not se:The standard error of the mean (SEM) is the standard deviation of the sample-mean's estimate of a population mean. (It can also be viewed as the standard deviation of the error in the sample mean with respect to the true mean, since the sample mean is an unbiased estimator.) 
	*/
	public static Double sd (List<Double> a)
	{
		if (a == null || a.size() < 2)
			return null;
		Double sd = null;
    Double sum = null;
    Double mean = null;
    int nb = 0;
    for (Double i : a) {
   	 	if (i != null && !Double.isNaN(i)){
    		 if (sum == null)
    			 sum = 0.0;
         sum += i;
         nb++;
    	 }
    }
    if (sum != null && nb != 0)
    	mean = sum / (nb * 1.0);
    
    if (mean == null)
    	return null;
    
    sum = null;
    for (Double i : a)
   	 	if (i != null && !Double.isNaN(i)){
    		if (sum == null)
    			sum = 0.0;
        sum += Math.pow((i - mean), 2);
    	}
    if (sum != null && nb > 1)
    	sd = Math.sqrt( sum / ( nb - 1.0 ) ); // sample
    
	  return sd;
	}

}
