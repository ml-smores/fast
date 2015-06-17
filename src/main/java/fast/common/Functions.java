package fast.common;

import java.util.ArrayList;

public class Functions {

	public static double logistic(double value){
		return (1.0 / (1.0 + Math.exp(-1.0 * value)));
	}
	
	public static Double logistic(ArrayList<Double> coefficients, ArrayList<Double> values){
		if (coefficients == null || values == null)
			return null;
		if (coefficients.size() != values.size())
			return null;
		double logit = 0;
		for (int i = 0; i < coefficients.size(); i++)
			logit += coefficients.get(i) * values.get(i);
		return (1.0 / (1.0 + Math.exp(-1.0 * logit)));//1.0 / (1.0 + Math.exp((-1.0) * logit)
	}
	
	public static Double euclidean_distance(ArrayList<Double> vector1, ArrayList<Double> vector2){
		if (vector1 == null || vector2 == null)
			return null;
		if (vector1.size() != vector2.size())
			return null;
		double distance = 0;
		for (int i = 0; i < vector1.size(); i++)
			distance += Math.pow((vector1.get(i) - vector2.get(i)), 2);
		return (Math.sqrt(distance));
	}

}

