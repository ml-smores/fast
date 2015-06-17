/**
 * FAST v1.0       08/12/2014
 * 
 * This code is only for research purpose not commercial purpose.
 * It is originally developed for research purpose and is still under improvement. 
 * Please email to us if you want to keep in touch with the latest release.
	 We sincerely welcome you to contact Yun Huang (huangyun.ai@gmail.com), or Jose P.Gonzalez-Brenes (josepablog@gmail.com) for problems in the code or cooperation.
 * We thank Taylor Berg-Kirkpatrick (tberg@cs.berkeley.edu) and Jean-Marc Francois (jahmm) for part of their codes that FAST is developed based on.
 *
 */

package fast.evaluation;

//import java.text.DecimalFormat;
import java.util.ArrayList;

public class PredictivePerformance {

	
//	private  String splitter = "[,\t]";
//	private  String delim = ",";
	// confusion matrix's "positive" corresponds to majorityName
	private  String label1Name = "correct";
	private  String label0Name = "incorrect";
	private  String majorityName = "correct";
	//private DecimalFormat formatter = null;
	//private  String minorityName = "incorrect";	
	
	public PredictivePerformance(){	
	}
	
	public PredictivePerformance(String majorityName, String minorityName, String label1Name, String label0Name){
		this.majorityName = majorityName;
		//this.minorityName = minorityName;
		this.label1Name = label1Name;
		this.label0Name = label0Name;
		if (!majorityName.equals(label1Name) && !majorityName.equals(label0Name))
			System.out.println("ERROR: majorityName string and actual label name mismatch!");
		if (!minorityName.equals(label1Name) && !minorityName.equals(label0Name))
			System.out.println("ERROR: minorityName string and actual label name mismatch!");
	}
	
//	public void setFormatter(DecimalFormat formatter){
//		this.formatter = formatter;
//	}

	public Metrics evaluateClassifier(ArrayList<Integer> actualLabels, ArrayList<Integer> predictLabels, ArrayList<Double> predictProbs, 
			String name){//, BufferedWriter writer, boolean writeHeader){
		Double nbObs = 0.0;
		Double majAUC = 0.0;
		Double LL = 0.0;
		Double accuracy = 0.0;
		Double rmse = 0.0;
		Double majFmeasure = 0.0,  minFmeasure = 0.0;
		Double majPrecision = 0.0, minPrecision = 0.0;
		Double majRecall = 0.0, minRecall = 0.0;
		Double TP = 0.0, TN = 0.0, FP = 0.0, FN = 0.0;
		
		int nbAccurate = 0;
		Double squaredError = 0.0;
		//double totalNbInstances = 0.0;

		if ((actualLabels.size() != predictLabels.size()) || (actualLabels.size() != predictLabels.size()) || (predictLabels.size() != predictProbs.size())) {
			System.out.println("ERROR: actualLabel, predictLabel and predictProbs size mismatch!");
			System.exit(1);
		}
		if (actualLabels.size() == 1) {
			System.out.println("WARNING: actualLabel size=1!");
			// System.exit(1);
		}

		for (int insId = 0; insId < actualLabels.size(); insId++) {
			Integer actualLabel = actualLabels.get(insId);
			Integer predictLabel = predictLabels.get(insId);
			Double predictProb = predictProbs.get(insId);
			//System.out.println(actualLabel + delim + predictLabel + delim + predictProb);
			
			nbObs++;

			if (predictProb > 1.0 || predictProb < 0.0) {
				System.out.println("Error:predictProb > 1.0 || predictProb < 0.0! insId=" + insId);
				System.exit(1);
			}

			majAUC = getAUC(actualLabels, predictProbs);
			if (Double.isNaN(majAUC))
				System.out.println("WARNING: AUC=" + majAUC);
			// minAUC = -1;
			// Not sure about the following method is correct (a little bit different
			// from weka...)
			// ArrayList<Integer> inverseActualLabels = new ArrayList<Integer>();
			// ArrayList<Double> inversePredictProbs = new ArrayList<Double>();
			// for (int ii = 0; ii < actualLabels.size(); ii++) {
			// int label = actualLabels.get(ii);
			// double prob = predictProbs.get(ii);
			// inverseActualLabels.add(1 - label);
			// inversePredictProbs.add(1.0 - prob);
			// }
			// minAUC[foldRunID] = getAUC(inverseActualLabels, inversePredictProbs);
			
			Double curLL = Double.NaN;
			if (actualLabel == 1){// (predictProb >= 0.5)
				if (predictProb > 0.0)
					curLL = Math.log10(predictProb);
			}
			else{
				if (predictProb < 1.0)
					curLL = Math.log10(1.0 - predictProb);
			}
			LL += curLL;
			
			if (predictLabel == actualLabel) {
				nbAccurate += 1;
				if ((predictLabel == 1 && label1Name.equals(majorityName)) || (predictLabel == 0 && label0Name.equals(majorityName)))
					 TP += 1;
				else
					 TN += 1;
			}
			else {
				if ((predictLabel == 1 && label1Name.equals(majorityName)) || (predictLabel == 0 && label0Name.equals(majorityName)))
					 FP += 1;
				else
				   FN += 1;	
			}
			
			squaredError += Math.pow(actualLabel - predictProb, 2);
			if (Double.isNaN(squaredError)) {
				System.out.println("Error: squaredError is NaN");
				System.exit(1);
			}
		}

		accuracy = (1.0 * nbAccurate) / nbObs;
		rmse = Math.sqrt(squaredError / nbObs);
		if (rmse == 0.0) 
			System.out.println("WARNING: RMSE=0");
		majPrecision = (TP + FP == 0.0) ? 0.0 : (1.0 * TP) / (TP + FP);
		minPrecision = (TN + FN == 0.0) ? 0.0 : (1.0 * TN) / (TN + FN);
		majRecall = (TP + FN == 0.0) ? 0.0 : (1.0 * TP) / (TP + FN);
		minRecall = (TN + FP == 0.0) ? 0.0 : (1.0 * TN) / (TN + FP);
		double denominator = majPrecision + majRecall;
		majFmeasure = (denominator == 0.0) ? 0.0 : (2 * majPrecision * majRecall) / (majPrecision + majRecall);
		denominator = minPrecision + minRecall;
		minFmeasure = (denominator == 0.0) ? 0.0 : (2 * minPrecision * minRecall) / (minPrecision + minRecall);
		double meanLLPerObs = LL/nbObs;
		
		Metrics eval = new Metrics(name);
		//Metrics.setFormatter(formatter);
		eval.setMetricValue("NbObs(test)", nbObs);
		eval.setMetricValue("AUC", majAUC);
		eval.setMetricValue("LogLikelihood_base10", LL);
		eval.setMetricValue("MeanLLPerObs",meanLLPerObs);
		eval.setMetricValue("Accuracy", accuracy);
		eval.setMetricValue("RMSE", rmse);
		eval.setMetricValue("MajFmeasure", majFmeasure);
		eval.setMetricValue("MinFmeasure", minFmeasure);
		eval.setMetricValue("MajPrecision", majPrecision);
		eval.setMetricValue("MinPrecision", minPrecision);
		eval.setMetricValue("MajRecall", majRecall);
		eval.setMetricValue("MinRecall", minRecall);
		eval.setMetricValue("TP", TP);
		eval.setMetricValue("TN", TN);
		eval.setMetricValue("FP", FP);
		eval.setMetricValue("FN", FN);
		
		return eval;
	}

	public static double getAUC(ArrayList<Integer> actualLabels, ArrayList<Double> predictProbs) {
		double[] actualLabelsArray = new double[actualLabels.size()];
		double[] predictProbsArray = new double[actualLabels.size()];
		for (int ii = 0; ii < actualLabels.size(); ii++) {
			actualLabelsArray[ii] = actualLabels.get(ii) * 1.0;
			predictProbsArray[ii] = predictProbs.get(ii);
		}
		Sample data = new Sample(actualLabelsArray);
		AUC aucCalculator = new AUC();
		double auc = aucCalculator.measure(predictProbsArray, data);		
		return auc;
	}

}
