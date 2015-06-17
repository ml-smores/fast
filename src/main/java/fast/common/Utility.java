package fast.common;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Random;

public class Utility {
	
	public static void swap(ArrayList<String> dataStrs, int i, int j) {
		String temp = dataStrs.get(i);
		dataStrs.set(i, dataStrs.get(j));
		dataStrs.set(j, temp);
	}
	

	public static void printArray(double[] array, String info) {
		System.out.println(info);
		String outStr = "";
		for (int i = 0; i < array.length; i++)
			outStr += array[i] + "\t";
		System.out.println(outStr);
	}


	public static double[] normalizedBySum(double[] values) {
		double[] normedValues = new double[values.length];
		double sum = 0.0;
		for (int k = 0; k < values.length; k++) {
			sum += values[k];
		}
		for (int k = 0; k < values.length; k++) {
			normedValues[k] = values[k] / sum;
		}
		return normedValues;
	}
	
	public static double[] uniformRandomArray(int dim, double lower,
			double upper, Random rand) {
		double range = upper - lower;
		double[] weights = new double[dim];
		for (int i = 0; i < dim; ++i) {
			double randVal = rand.nextDouble();
			weights[i] = lower + (range * randVal);
		}
		return weights;
	}
	
	public static double[] uniformRandomArraySumToOne(int dim, double lower,
			double upper, Random rand) {
		double range = upper - lower;
		double[] weights = new double[dim];
		double sum = 0.0;
		int i = 0;
		for (; i < dim-1; ++i) {
			double randVal = rand.nextDouble();
			weights[i] = lower + (range * randVal);
			sum += weights[i];
		}
		weights[i] = 1 - sum;
		return weights;
	}
	
	public static double[] intToDoubleArray(int[] labels) {
		double[] targets = new double[labels.length];
		for (int i = 0; i < labels.length; i++) {
			targets[i] = labels[i];
		}
		return targets;
	}
	
	public void printArray(double[] oneArray) {
		for (int i = 0; i < oneArray.length; i++)
			System.out.print(oneArray[i] + "\t");
		System.out.println();
	}
	
	public static String doubleArrayListToString(ArrayList<Double> oneList, DecimalFormat formatter, String delimiter){
		String str = "";
		for (double value : oneList)
			str += getValidString(value, formatter) + delimiter;
		return str;
	}
	
	public static void arrayListToArray(ArrayList<Double> aList, double[] a) {
		for (int i = 0; i < aList.size(); i++)
			a[i] = aList.get(i);
	}
	
	public static String[] linkedHashMapToStrings(LinkedHashMap<String, Double> aMap, String delimiter){
		String[] strs = {"", ""};
		for (Map.Entry<String, Double> entry : aMap.entrySet()) {
	    String key = entry.getKey();
	    Double value = entry.getValue();
	    strs[0] += key + delimiter;
	    strs[1] += value + delimiter;
		}
		return strs;
	}
	
	public static String getValidString(Double value, DecimalFormat formatter){
		String str = "";
		if (value == null || Double.isNaN(value))
			str = "NaN";
		else
			str = formatter.format(value) + "";
		return str;
	}


}


