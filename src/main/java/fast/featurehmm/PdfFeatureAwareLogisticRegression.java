/**
 * FAST v1.0       08/12/2014
 * 
 * This code is only for research purpose not commercial purpose.
 * It is originally developed for research purpose and is still under improvement. 
 * Please email to us if you want to keep in touch with the latest release.
	 We sincerely welcome you to contact Yun Huang (huangyun.ai@gmail.com), or Jose P.Gonzalez-Brenes (josepablog@gmail.com) for problems in the code or cooperation.
 * We thank Taylor Berg-Kirkpatrick (tberg@cs.berkeley.edu) and Jean-Marc Francois (jahmm) for part of their code that FAST is developed based on.
 *
 */

/* 
 * This is built based on:
 * 
 *   jaHMM package - v0.6.1 
 *  Copyright (c) 2004-2006, Jean-Marc Francois.
 */

package fast.featurehmm;

//import java.text.NumberFormat;
//import java.text.NumberFormat;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import fast.common.*;
import fast.data.DataPoint;

/**
 * This class implements a distribution defined by weights of features. Each
 * object of PdfFeatureAwareLogisticRegression corresponds to one hidden state,
 * and can be for initial or transition or emission.
 */
public class PdfFeatureAwareLogisticRegression implements PdfFeatureAware<DataPoint> {
	private static final long serialVersionUID = 1L;
	private final boolean parameterizing, parameterizedInit,  parameterizedTran,  parameterizedEmit;
	private final boolean allowForget;
	private final int nbHiddenStates, nbObsStates;
	private final double[] regularizationWeights, regularizationBiases;
	private final double PROBABILITY_MIN_VALUE;
	private final double LBFGS_TOLERANCE;
	private final int LBFGS_MAX_ITERS;

	
	private double[] featureWeights;
	private double probability;// for initial
	private double[] probabilities;// for transition or emission
	private int nbParameterizingFailed;

//	private Bijection featureMapping;
//	private Bijection classMapping;
	

	
	public PdfFeatureAwareLogisticRegression(boolean parameterizing, boolean parameterizedInit, boolean parameterizedTran, boolean parameterizedEmit,
																					boolean allowForget,
																					double PROBABILITY_MIN_VALUE, int nbHiddenStates, int nbObsStates,
																					double[] regularizationWeights, double[] regularizationBiases,
																					double LBFGS_TOLERANCE, int LBFGS_MAX_ITERS){
		this.parameterizing = parameterizing;
		this.parameterizedInit = parameterizedInit;
		this.parameterizedTran = parameterizedTran;
		this.parameterizedEmit = parameterizedEmit;
		this.allowForget = allowForget;
		this.PROBABILITY_MIN_VALUE = PROBABILITY_MIN_VALUE;
		this.nbObsStates = nbObsStates;
		this.nbHiddenStates = nbHiddenStates;
		this.regularizationWeights = regularizationWeights;
		this.regularizationBiases = regularizationBiases;
		this.LBFGS_TOLERANCE = LBFGS_TOLERANCE;
		this.LBFGS_MAX_ITERS = LBFGS_MAX_ITERS;
		
//		if (parameterizing){
//			if (featureWeights == null || featureWeights.length == 0){
//				System.out.println("Error: parameterizing=true but no featureWeights!");
//				System.exit(1);
//			}
//		}
	}

	/** 1) initialize by another object (deep copy)*/
	public PdfFeatureAwareLogisticRegression(PdfFeatureAwareLogisticRegression pdf) {
		//this.classMapping = new Bijection(pdf.classMapping);
		if (pdf.featureWeights != null) {
			this.featureWeights = new double[pdf.featureWeights.length];
			for (int i = 0; i < featureWeights.length; i++)
				this.featureWeights[i] = pdf.featureWeights[i];
		}
		//this.featureMapping = new Bijection(pdf.featureMapping);
		if (pdf.probabilities != null) {
			this.probabilities = new double[pdf.probabilities.length];
			for (int i = 0; i < probabilities.length; i++)
				this.probabilities[i] = pdf.probabilities[i];
		}
		this.probability = pdf.probability;
		
		this.parameterizing = pdf.parameterizing;
		this.parameterizedInit = pdf.parameterizedInit;
		this.parameterizedTran = pdf.parameterizedTran;
		this.parameterizedEmit = pdf.parameterizedEmit;
		this.allowForget = pdf.allowForget;
		this.PROBABILITY_MIN_VALUE = pdf.PROBABILITY_MIN_VALUE;
		this.nbObsStates = pdf.nbObsStates;
		this.nbHiddenStates = pdf.nbHiddenStates;
		this.regularizationWeights = pdf.regularizationWeights;
		this.regularizationBiases = pdf.regularizationBiases;
		this.LBFGS_TOLERANCE = pdf.LBFGS_TOLERANCE;
		this.LBFGS_MAX_ITERS = pdf.LBFGS_MAX_ITERS;
	}

	/**
	 * 2) for initializing parameterized probabilities. 
	 *    Builds a new probability distribution parameterized by weights over specified features
	 */
	public void initialize(double[] featureWeights, int nbClasses){//, Bijection classMapping, Bijection featureMapping) {
		
		if (featureWeights == null || featureWeights.length == 0)
				//classMapping == null || featureMapping == null || featureMapping.getSize() != featureWeights.length)
			throw new IllegalArgumentException();
		//this.classMapping = classMapping;
		this.featureWeights = featureWeights;
		//this.featureMapping = featureMapping;
		this.probabilities = new double[nbClasses];
		this.probability = -1.0;
	}

	/** 3) for initializing nonparametric pdf for tran or emit */
	public void initialize(double[] probabilities) {//Bijection classMapping, 
		if (probabilities.length == 0)
			throw new IllegalArgumentException();
		this.probabilities = new double[probabilities.length];
		for (int i = 0; i < probabilities.length; i++)
			if ((this.probabilities[i] = probabilities[i]) < 0.)
				throw new IllegalArgumentException();
		this.probability = -1.0;
//		this.classMapping = classMapping;
//		featureMapping = null;
		featureWeights = null;
	}

	/** 4) for initializing nonparametric pdf for init */
	public void initialize(double probability) {//Bijection classMapping, 
		this.probability = probability;
		this.probabilities = null;
//		this.classMapping = classMapping;
//		this.featureMapping = null;
		this.featureWeights = null;
	}

	// classIndex can be either for hiddenstate or observed state
	public double probability(double[] featureValues, int classIndex, String type) {
		double finalProb = 0.0;
		if (featureValues == null) {
			double prob = -1.0;
			if (type.contains("init") && ((!parameterizedInit && parameterizing) || !parameterizing) )
				prob = probability;
			else if (type.contains("tran") && ((!parameterizedTran && parameterizing) || !parameterizing))
				prob = probabilities[classIndex];
			else if (type.contains("emi") && ((!parameterizedEmit && parameterizing) || !parameterizing))
				prob = probabilities[classIndex];
			if (prob != -1.0) {
				finalProb = Math.max(Math.min(prob, (1 - PROBABILITY_MIN_VALUE)), PROBABILITY_MIN_VALUE);
				return finalProb;
			}
			else {
				System.out.println("type:" + type + ", ERROR: prob=-1.0!");
				System.exit(1);
			}
		}

		if (parameterizing && (featureValues == null || featureWeights == null))
			throw new IllegalArgumentException("no feature values");

		double[] finalFeatureValues = featureValues;

		if (finalFeatureValues.length != featureWeights.length) {
			System.out.println("ERROR: finalFeatureValues.size() !=featureWeights.length");
			System.exit(1);
		}

		double logit = 0.0;
		for (int i = 0; i < featureWeights.length; i++) 
			logit += featureWeights[i] * finalFeatureValues[i];
		
		double prob = Math.max(Math.min(1.0 / (1.0 + Math.exp((-1.0) * logit)), (1 - PROBABILITY_MIN_VALUE)), PROBABILITY_MIN_VALUE);
		finalProb = (classIndex == 1 ? prob : (1.0 - prob));
		
		return finalProb;
	}

	/**
	 * for init or emission: if init: gamma=firstDpGamma; observations=firstDpObs
	 */
	public void fitNonParameterizedProb(Collection<? extends DataPoint> observations, double[] gamma, String type) {
		if (type.contains("init")) {
			double sum = 0.0;
			for (int k = 0; k < gamma.length; k++) {
				sum += gamma[k];
			}
			probability = sum / observations.size();
		}
		else {
			if (type.contains("emi") && observations.size() != gamma.length) {
				System.out.println("ERROR: observations.size() != gamma.length!");
				System.exit(1);
			}
			double[] weights = new double[gamma.length];
			Arrays.fill(probabilities, 0.);
			weights = Utility.normalizedBySum(gamma);

			probabilities = computeObsClassWeights(observations, weights, nbObsStates);
		}
	}

	/** for trans, correspond to one hiddenstate */
	public void fitNonParameterizedProb(Collection<? extends DataPoint> observations, double[] ajNum, double ajDen, double[] aj, String type) {
		if (!type.contains("trans")) {
			System.out.println("ERROR: you should fit transition here!");
			System.exit(1);
		}
		if (ajDen == 0.) // State i is not reachable
			for (int j = 0; j < nbHiddenStates; j++) {
				probabilities[j] = aj[j];
			}
		else
			for (int j = 0; j < nbHiddenStates; j++)
				probabilities[j] = ajNum[j] / ajDen;

	}

	/**
	 * Actual function to fit 1LR for init and emission, here expand the
	 * observations; expectedCounts can be firstDpGamma, or gamma; for init,
	 * observations are the first data point observations
	 */
	public void fitParameterizedProbByInstanceWeights(Collection<? extends DataPoint> observations, double[][] expectedCounts, String type) {
		// TODO: check gammas length with nbHiddenStates
		int newObsLength = nbHiddenStates * observations.size();
		// int newFeatureVectorLength = -1;
		double[] finalInstanceWeights = new double[newObsLength];
		// classes can be outcome or 2nd hiddenState
		int[] finalClasses = new int[newObsLength];
		double[][] finalFeatureValues = new double[newObsLength][];
		// double[] currentDPFeatureValues = null;
		//int nbRoundTo0 = 0;

		// int i = 0;
		Iterator<? extends DataPoint> iter = observations.iterator();
		for (int index = 0; index < newObsLength; index++) {
			if (!iter.hasNext())
				iter = observations.iterator();
			DataPoint currentDP = iter.next();
			// System.out.println("index:" + index);
			int currentHiddenState = (index >= observations.size()) ? 1 : 0;
			// if (currentHiddenState == 1)
			// System.out.println();
			int currentIndexInGamma = (index >= observations.size()) ? index
					- observations.size() : index;

			if (type.contains("emi"))
				finalClasses[index] = currentDP.getOutcome();
			else
				// TODO: think about it more... after doubling the observations, 1st half hiddenstate0, 2nd half hiddenstate1
				finalClasses[index] = currentHiddenState;

			double currentInstanceWeight = expectedCounts[currentHiddenState][currentIndexInGamma];
			finalInstanceWeights[index] = currentInstanceWeight;

			double[] currentFeatureValues = null;
			if (type.contains("init"))
				currentFeatureValues = currentDP.getFeatures(currentHiddenState, 0);
			else if (type.contains("emi"))
				currentFeatureValues = currentDP.getFeatures(currentHiddenState, 2);
			else {
				System.out.println("ERROR: type should be either init or emit!");
				System.exit(1);
			}
			if (featureWeights.length != currentFeatureValues.length){
					//|| currentFeatureValues.length != featureMapping.getSize()) {
				System.out.println("ERROR: featureWeights.length != currentFeatureValues.length");// || currentFeatureValues.length != featureMapping.getSize()");
				System.exit(1);
			}
			finalFeatureValues[index] = currentFeatureValues;
		}

		LogisticRegression LR = new LogisticRegression(this, finalInstanceWeights, finalFeatureValues, finalClasses, type, regularizationWeights, regularizationBiases, LBFGS_TOLERANCE, LBFGS_MAX_ITERS);
		featureWeights = LR.train();
		nbParameterizingFailed = LR.getParameterizingResult();
	}
	

	/**
	 * Actual function to fit 1LR for transition, here expand the observations to
	 * 4 times the original;
	 * 
	 * xi =expectedCounts=flatXiForStates = new
	 * double[opts.nbHiddenStates][opts.nbHiddenStates][observations.size()-1];
	 * 1st: t step hiddenstates; 2nd: t+1 step, hiddenstates; 3rd: all datapoints - 1
	 * 
	 * xi(expectedCounts) is the joint prob: p(qt_i, qt+1_j|Dd); t_i, t+1_j,
	 * datapoint_id it is one observation shorter because the last dp wouldn't
	 * have a next state to transition to
	 */
	public void fitParameterizedTransByInstanceWeights(Collection<? extends DataPoint> observations, double[][][] expectedCounts) {
		// 1st obs:p(nk|nk), 2nd obs:p(k|nk), 3rd obs:p(nk|k), 4th obs:p(k|k), 1=k
		// TOOD: hy changed on 12/17/2014, actual observations with every sequence
		// one obs less
		if (observations.size() != expectedCounts[0][0].length) {
			System.out
					.println("ERROR: observations.size() != expectedCounts[0][0].length");
			System.exit(1);
		}
		int activeObsLength = observations.size();
		int newObsLength = nbHiddenStates * activeObsLength * 2;
		// int newFeatureVectorLength = -1;
		double[] finalInstanceWeights = new double[newObsLength];
		int[] final2ndHiddenState = new int[newObsLength];// hiddenState
		double[][] finalFeatureValues = new double[newObsLength][];
		// double[] currentDPFeatureValues = null;
		//int nbRoundTo0 = 0;

		// int i = 0;
		Iterator<? extends DataPoint> iter = observations.iterator();
		for (int index = 0; index < newObsLength; index++) {
			if (!iter.hasNext())
				iter = observations.iterator();
			// if ((index + 1) % observations.size() == 0)
			// iter = observations.iterator();
			DataPoint currentDP = iter.next();
			// System.out.println("index:" + index);
			int currentHiddenState = (index >= activeObsLength * 2) ? 1 : 0;// first two blocks are 0, latter 2 blocks are 1
			// if (currentHiddenState == 1)
			// System.out.println();
			int currentIndexInXi = (index >= activeObsLength * 3) ? index
					- activeObsLength * 3 : (index >= activeObsLength * 2) ? index
					- activeObsLength * 2 : (index >= activeObsLength) ? index
					- activeObsLength : index;
			int current2ndHiddenState = 0;
			if (index >= activeObsLength * 3
					|| (index >= activeObsLength && index < activeObsLength * 2))// 1st, 3rd are 0 and the 2nd, 4th are 1
				current2ndHiddenState = 1;
			final2ndHiddenState[index] = current2ndHiddenState;

			//useXiAsInstanceWeight) {
			double currentInstanceWeight = expectedCounts[currentHiddenState][current2ndHiddenState][currentIndexInXi];
			finalInstanceWeights[index] = currentInstanceWeight;
			/* adjust the instance weight here if we want to pre-set some of the transition probabilities fixed while others still keep being parameterized. */
			if (!allowForget) {
				if (currentHiddenState == 1)
					finalInstanceWeights[index] = 0.0;
			}

			double[] currentFeatureValues = currentDP.getFeatures(currentHiddenState,
					1);
			finalFeatureValues[index] = currentFeatureValues;
		}

		LogisticRegression LR = new LogisticRegression(this, finalInstanceWeights, finalFeatureValues, final2ndHiddenState, "trans", regularizationWeights, regularizationBiases, LBFGS_TOLERANCE, LBFGS_MAX_ITERS);
		featureWeights = LR.train();
		nbParameterizingFailed = LR.getParameterizingResult();
	}

	// 1LR, classes can be outcomes or hiddenStates
	public double calculateExpectedLogLikelihood(double[] instanceWeights, double[][] featureValues, int[] classes, String type) {
		// if (!opts.oneLogisticRegression) {
		// System.out
		// .println("WARNING: calculateExpectedLogLikelihood but !opts.oneLogisticRegression!");
		// // System.exit(1);
		// }
		if (instanceWeights == null || featureWeights == null
				|| featureValues == null || classes == null) {
			System.out
					.println("ERROR: instanceWeights==null || featureWeights==null || featureValues==null||outcomes==null!");
			System.exit(1);
		}
		if (instanceWeights.length != featureValues.length
				|| featureValues.length != classes.length) {
			System.out
					.println("ERROR: instanceWeights.length!=featureWeights.length || featureWeights.length != featureValues.length || featureValues.length != classes.length!");
			System.exit(1);
		}
		double smallLL = 0.0;
		int nbDatapoints = instanceWeights.length;
		for (int i = 0; i < nbDatapoints; i++) {
			double expectedCount = instanceWeights[i];
			double[] features = featureValues[i];
			if (features.length != featureWeights.length) {
				System.out.println("ERROR: features.length != featureWeights.length!");
				System.exit(1);
			}
			int class_ = classes[i];
			// TODO: no bias yet
			// int hiddenStateIndex = (i >= nbDatapoints / 2) ? 1 : 0;
			double thita = probability(features, class_, type);
			smallLL += expectedCount * Math.log(thita);
		}
		return smallLL;
	}


	public double[] computeObsClassWeights(
			Collection<? extends DataPoint> observations, double[] weights,
			int nbObsStates) {
		double[] classWeights = new double[nbObsStates];
		int i = 0;
		for (DataPoint o : observations)
			classWeights[o.getOutcome()] += weights[i++];
		return classWeights;
	}

//	private void printArray(double[] array, String info) {
//		System.out.println(info);
//		String outStr = "";
//		for (int i = 0; i < array.length; i++)
//			outStr += array[i] + "\t";
//		System.out.println(outStr);
//	}
//
//	private void printArray(int[] array, String info) {
//		System.out.println(info);
//		String outStr = "";
//		for (int i = 0; i < array.length; i++)
//			outStr += array[i] + "\t";
//		System.out.println(outStr);
//	}
//
//	private void printArray(double[][] array, String info) {
//		System.out.println(info);
//		String outStr = "";
//		System.out.println("1st dim length=" + array.length + ", 2nd dim length="
//				+ array[0].length);
//		for (int i = 0; i < array.length; i++) {
//			for (int j = 0; j < array[i].length; j++) {
//				outStr += "\t" + array[i][j];
//			}
//			System.out.println(outStr);
//			outStr = "";
//		}
//	}
//
//	private void printArray(double[][] array, Bijection featureMapping, String info) {
//		System.out.println(info);
//		String outStr = "";
//		String header = "";
//		System.out.println("1st dim length=" + array.length + ", 2nd dim length="
//				+ array[0].length);
//		for (int j = 0; j < featureMapping.getSize(); j++) {
//			header += "\t" + featureMapping.get(j);
//		}
//		System.out.println(header);
//		for (int i = 0; i < array.length; i++) {
//			for (int j = 0; j < array[i].length; j++) {
//				outStr += "\t" + array[i][j];
//			}
//			System.out.println(outStr);
//			outStr = "";
//		}
//	}

	public int getParameterizingResult(){
		return nbParameterizingFailed;
	}
	
	public double[] getFeatureWeights(){
		return featureWeights;
	}
	
	public void setFeatureWeights(double[] featureWeights){
		this.featureWeights = featureWeights;
	}
	
	public boolean getParameterizing(){
		return parameterizing;
	}
	
	public boolean getParameterizedInit(){
		return parameterizedInit;
	}
	
	public boolean getParameterizedTran(){
		return parameterizedTran;
	}	
	
	public boolean getParameterizedEmit(){
		return parameterizedEmit;
	}
	
	public boolean getAllowForget(){
		return allowForget;
	}
	
	public int getNbHiddenStates(){
		return nbHiddenStates;
	}
	
	// @SuppressWarnings("unchecked")
	public PdfFeatureAwareLogisticRegression clone() {
		try {
			PdfFeatureAwareLogisticRegression pdf = (PdfFeatureAwareLogisticRegression) super .clone();//Can be shallow copy
			return pdf;
		}
		catch (CloneNotSupportedException e) {
			throw new InternalError();
		}
	}


//	@Override
//	private String toString(NumberFormat numberFormat) {
//		// TODO Auto-generated method stub
//		return null;
//	}

//	private String toString() {
//		return toString(NumberFormat.getInstance());
//	}

//	private String toString(NumberFormat numberFormat) {
//		String s = "";// "<";
//
//		if (featureWeights == null) {
//			for (int i = 0; i < classMapping.keys().size(); i++) {
//
//				String k = classMapping.get(i);
//				s += k + " " + longFormatter.format(probabilities[i])// numberFormat.format(probabilities[i])
//						+ ((i != classMapping.getSize() - 1) ? ", " : "");
//			}
//			// s += ">";
//		}
//		else {
//			for (int i = 0; i < featureWeights.length; i++) {
//				// s += numberFormat.format(featureWeights[i])
//				// + ((i != values.size() - 1) ? ", " : "");
//				s += longFormatter.format(featureWeights[i])
//						+ ((i != featureWeights.length - 1) ? "\t" : "");
//			}
//			// s += ">";
//		}
//
//		return s;
//	}
}
