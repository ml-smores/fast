package fast.evaluation;

import java.text.DecimalFormat;
import java.util.LinkedHashMap;
//import edu.berkeley.nlp.classify.Feature;
import fast.common.Bijection;

public class TrainSummary {
	
	//TODO: Change to final iteration number
	public int nbLLError;
	public double maxLLDecrease, maxLLDecreaseRatio, trainLL;
	public int nbParameterizingFailed;
	public int nbStopByEMIteration;
	/* 
	 * Bijection allFeatures includes init, tran, emit;
	 * Each Bijection contains original feature name, and feature name with "_hidden1" surfix;
	 * Later when output to parameters file, the code will transfer the name using "init", "guess", "slip", "learn" (etc.).
	 */
	public Bijection allFeatures = new Bijection();
	public Bijection initFeatures = new Bijection();
	public Bijection tranFeatures = new Bijection();
	public Bijection emitFeatures = new Bijection(); 
	public LinkedHashMap<String, Double> parameters = new LinkedHashMap<String, Double>();

	
	public void update(double trainLL, int nbStopByEMIteration, int nbLLError, double maxLLDecrease, double maxLLDecreaseRatio, 
			int nbParameterizingFailed){
		this.trainLL = trainLL;
		this.nbLLError = nbLLError;
		this.maxLLDecrease = maxLLDecrease;
		this.maxLLDecreaseRatio = maxLLDecreaseRatio;
		this.nbParameterizingFailed = nbParameterizingFailed;
		this.nbStopByEMIteration = nbStopByEMIteration;
	}
	
	public String getHeader(String delimiter){
		return ("trainLL" + delimiter + "nbStopByEMIteration" + delimiter + "nbLLError" + delimiter + "maxLLDecrease" + delimiter + "maxLLDecreaseRatio" 
					+ delimiter +  "nbParameterizingFailed");
	}
	
	public String getEvaluationStr(String delimiter, DecimalFormat formatter){
		return  (formatter.format(trainLL) + delimiter + formatter.format(nbStopByEMIteration) + delimiter + formatter.format(nbLLError)
				+ delimiter + formatter.format(maxLLDecrease) + delimiter + formatter.format(maxLLDecreaseRatio)
				+ delimiter + nbParameterizingFailed);
	}
	
//	public void update(TrainSummary trainSummary){
//		this.nbLLError = trainSummary.nbLLError;
//		this.maxLLDecrease = trainSummary.maxLLDecrease;
//		this.maxLLDecreaseRatio = trainSummary.maxLLDecreaseRatio;
//		this.parameterizingSucceeded = trainSummary.parameterizingSucceeded;
//		this.stopByEMIteration = trainSummary.stopByEMIteration;
//		this.trainLL = trainSummary.trainLL;
//		this.allFeatures = trainSummary.allFeatures;
//		this.initFeatures = trainSummary.initFeatures;
//		this.tranFeatures = trainSummary.tranFeatures;
//		this.emitFeatures = trainSummary.emitFeatures;
//		this.parameters = trainSummary.parameters;
//	}
	
//	public double getTrainLL(){
//		return trainLL;
//	}
//	
//	public int getNbLLError(){
//		return nbLLError;
//	}
//
//	public double getMaxLLDecrease(){
//		return maxLLDecrease;
//	}
//	
//	public double getMaxLLDecreaseRatio(){
//		return maxLLDecreaseRatio;
//	}
//	
//	public boolean getParameterizingSucceeded(){
//		return parameterizingSucceeded;
//	}
//	
//	public int getStopByEMIteration(){
//		return stopByEMIteration;
//	}

}
