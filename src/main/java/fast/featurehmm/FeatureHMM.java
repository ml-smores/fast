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

import java.io.Serializable;
//import java.text.NumberFormat;
import java.util.ArrayList;
//import java.util.HashSet;
//import java.util.List;
//import be.ac.ulg.montefiore.run.jahmm.Observation;
import fast.data.DataPoint;


public class FeatureHMM implements Serializable, Cloneable {
	private static final long serialVersionUID = 2L;

	private final boolean parameterizedInit,  parameterizedTran,  parameterizedEmit;
	private final boolean allowForget;
  private final int nbHiddenStates;
	private final ArrayList<PdfFeatureAwareLogisticRegression> initialPdfs;// = new ArrayList<PdfFeatureAwareLogisticRegression>();
	private final ArrayList<PdfFeatureAwareLogisticRegression> transitionPdfs;// = new ArrayList<PdfFeatureAwareLogisticRegression>();
	private final ArrayList<PdfFeatureAwareLogisticRegression> emissionPdfs;// = new ArrayList<PdfFeatureAwareLogisticRegression>();
//	private final PdfFeatureAwareLogisticRegression initialPdf;// = new ArrayList<PdfFeatureAwareLogisticRegression>();
//	private final PdfFeatureAwareLogisticRegression transitionPdf;// = new ArrayList<PdfFeatureAwareLogisticRegression>();
//	private final PdfFeatureAwareLogisticRegression emissionPdf;// = new ArrayList<PdfFeatureAwareLogisticRegression>();
	

	public FeatureHMM(ArrayList<PdfFeatureAwareLogisticRegression> initPdfs,
											ArrayList<PdfFeatureAwareLogisticRegression> transPdfs,
											ArrayList<PdfFeatureAwareLogisticRegression> emitPdfs//, 
											//boolean parameterizing, boolean parameterizedInit, boolean parameterizedTran, boolean parameterizedEmit, 
											//boolean allowForget,
											){
			//int restartId, String kcName, String modelName) {
		if (initPdfs.size() == 0 || initPdfs.size() != transPdfs.size() || emitPdfs.size() != transPdfs.size())
						throw new IllegalArgumentException("ERROR: Wrong initial parameters for HMM (constructor)!");
		this.initialPdfs = initPdfs;
		this.transitionPdfs = transPdfs;
		this.emissionPdfs = emitPdfs;
		this.parameterizedInit = initPdfs.get(0).getParameterizedInit();
		this.parameterizedTran = transPdfs.get(0).getParameterizedTran();
		this.parameterizedEmit = emitPdfs.get(0).getParameterizedEmit();
//		if (this.parameterizedInit || this.parameterizedTran || this.parameterizedEmit)
//			this.parameterizing = true;
//		else
//			this.parameterizing = false;
		this.allowForget = transPdfs.get(0).getAllowForget();
		this.nbHiddenStates = initPdfs.size();
//		this.restartId = restartId;
//		this.kcName = kcName;
//		this.modelName = modelName;
	}

	/* The same for i=0 or i=1, because the code expanded features for both hiddenStates to train one logistic regression. */
	public PdfFeatureAwareLogisticRegression getInitialPdf(int i) {
		return initialPdfs.get(i);
	}

	public PdfFeatureAwareLogisticRegression getTransitionPdf(int i) {
		return transitionPdfs.get(i);
	}

	public PdfFeatureAwareLogisticRegression getEmissionPdf(int i) {
		return emissionPdfs.get(i);
	}

	public double getInitiali(int i, double[] featureValues) {
		if (!parameterizedInit)
			return initialPdfs.get(i).probability(null, i, "init");
		else
			return initialPdfs.get(i).probability(featureValues, i, "init");

	}

	/** The probability associated to the transition going from <code>i</code> to / state <code>j</code>. */
	public double getTransitionij(int i, int j, double[] featureValues) {
		if (!parameterizedTran)
			return transitionPdfs.get(i).probability(null, j, "trans");
		else{
			if (!allowForget && i == 1){
				return (j == 0 ? 0.0:1.0);
			}
			else
				return transitionPdfs.get(i).probability(featureValues, j, "trans");
		}
	}

	public double getEmissionjk(int j, int k, double[] featureValues) {
		if (!parameterizedEmit)
			return emissionPdfs.get(j).probability(null, k, "emit");
		else{
			double prob = emissionPdfs.get(j).probability(featureValues, k, "emit");
			return prob;
		}
	}

	public void setInit(PdfFeatureAwareLogisticRegression pdf, int hiddenStateIndex) {
		initialPdfs.set(hiddenStateIndex, pdf);
	}

	public void setTransition(PdfFeatureAwareLogisticRegression pdf, int hiddenStateIndex) {
		transitionPdfs.set(hiddenStateIndex, pdf);
	}

	public void setEmission(PdfFeatureAwareLogisticRegression pdf, int hiddenStateIndex) {
		emissionPdfs.set(hiddenStateIndex, pdf);
	}

//	public String toString(NumberFormat nf) {
//		String s = "Hmm with " + nbHiddenStates + " state(s)\n";
//
//		for (int i = 0; i < nbHiddenStates; i++) {
//			s += "\nState " + i + "\n";
//			s += "  Initial:\n"
//					+ getHmmString(initialPdfs.get(i), (parameterizedInit ? true
//							: false)) + "\n";
//			s += "  Transition:\n"
//					+ getHmmString(transitionPdfs.get(i), (parameterizedTran ? true
//							: false)) + "\n";
//			s += "  Emission:\n"
//					+ getHmmString(emissionPdfs.get(i), (parameterizedEmit ? true
//							: false)) + "\n";
//		}
//		return s;
//	}

//	public String getHmmString(PdfFeatureAwareLogisticRegression pdf,
//			boolean parameterized) {
//		String s = "";
//		//TODO: differentiate between allowing forget vs not
//		if (parameterized) {
//			s += "\tparameterized:";
//			double[] w = pdf.featureWeights;
//			if (w.length == 1) {
//				s += "\tprobabilities:\t" + 1 / (1 + Math.exp(-w[0]));
//			}
//			s += "\tweights:";
//			for (int k = 0; k < w.length; k++)
//				s += "\t" + w[k];
//		}
//		else {
//			double[] p = pdf.probabilities;
//			if (p != null) {
//				for (int k = 0; k < p.length; k++)
//					s += "\t" + pdf.classMapping.get(k) + "\t" + p[k];
//			}
//			else
//				s += "\t" + pdf.probability;
//
//		}
//		return s;
//	}
//
//	public String toString() {
//		return toString(NumberFormat.getInstance());
//	}

	public FeatureHMM clone() throws CloneNotSupportedException {
		FeatureHMM Hmm = new FeatureHMM(initialPdfs, transitionPdfs, emissionPdfs);
		for (int i = 0; i < Hmm.nbHiddenStates; i++) {
			Hmm.initialPdfs.set(i, initialPdfs.get(i).clone()); //Can be shallow copy
			Hmm.transitionPdfs.set(i, transitionPdfs.get(i).clone());
			Hmm.emissionPdfs.set(i, emissionPdfs.get(i).clone());
		}
		return Hmm;
	}

	public int getNbHiddenStates() {
		// return pi.length;
		return nbHiddenStates;//initialPdfs.size();
	}
	
	public static int getKnownState(FeatureHMM hmm, DataPoint dp, boolean useEmissionToJudgeHiddenStates, boolean allowForget) {
		if (!allowForget)
			return 1;
		int knownState = -1;
		if (useEmissionToJudgeHiddenStates){//this requires guess+slip<1; if not, this way of judgement is questionable
			double hidden0obs0 = hmm.getEmissionjk(0, 0, dp.getFeatures(0, 2));
			double hidden1obs0 = hmm.getEmissionjk(1, 0, dp.getFeatures(1, 2));
			if (hidden0obs0 > hidden1obs0) {
				knownState = 1;//known
			}
			else {
				knownState = 0;
			}
		}
		else{//this requires guess+slip<1; if not, this way of judgement is questionable
			double a01 = hmm.getTransitionij(0, 1, dp.getFeatures(0, 1));
			double a10 = hmm.getTransitionij(1, 0, dp.getFeatures(1, 1));
			if (a01 < a10) {
				knownState = 0;// know
			}
			else {
				knownState= 1;// know
			}
		}
		return knownState;
	}
	
	public static double checkDegeneracy(FeatureHMM hmm, DataPoint dp, int knownState){
		double guess = hmm.getEmissionjk(1 - knownState, 1, dp.getFeatures(1 - knownState, 2));
		double slip =  hmm.getEmissionjk(knownState, 0, dp.getFeatures(knownState, 2));
		return (guess + slip);
	}
}

