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

/*
 * This is built based on:
 *  jahmm package - v0.6.1 
 *  Copyright (c) 2004-2006, Jean-Marc Francois.
 *  
 */

package fast.featurehmm;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.List;
import fast.data.DataPoint;


/**
 * An implementation of the Baum-Welch learning algorithm. This algorithm finds
 * a HMM that models a set of observation sequences.
 */
public class BaumWelchLearner {
	
  //@Option(gloss = "INIT_LL is used to determine initial LL to compute the first LL change.")
	private final double INIT_LL = -1.0E10;
	private final double EM_TOLERANCE;
	private final double ACCETABLE_LL_DECREASE;
	private final double EPS;
	private final int EM_MAX_ITERS;
	private final boolean parameterizing, parameterizedInit,  parameterizedTran,  parameterizedEmit;
	private final int nbHiddenStates;

	private double allGamma[][][];
	private double aijNum[][];
	private double aijDen[];
	private double allXi[][][][]; 	// #sequences-1, #times,#hiddenStates, #hiddenStates
	private int nb_xi_datapoints;
	private double allFirstDpGamma[][]; //// #sequences, #hiddenStates
	private double ll; //  //current iteration's all data ll; belongs to an object (not class)
	private double previousLL;
	private boolean convergeByAbs = false;
	private boolean verbose = false;

	private int nbParameterizingFailed = 0;//only 1/0
	private int nbStopByEMIteration = 0; //only 1/0
	private double maxLLDecrease = 0.0;
	private double maxLLDecreaseRatio = 0.0;
	private int nbLLError = 0;
	//public boolean cgReachesTrustRegionBoundary = false;

	
	public BaumWelchLearner(boolean parameterizing, boolean parameterizedInit, boolean parameterizedTran, boolean parameterizedEmit,
													int nbHiddenStates,
													double EM_TOLERANCE, double EPS, double ACCETABLE_LL_DECREASE, int EM_MAX_ITERS){//, Opts opts) {//int restartId, String kcName, 
		this.parameterizing = parameterizing;
		this.parameterizedInit = parameterizedInit;
		this.parameterizedTran = parameterizedTran;
		this.parameterizedEmit = parameterizedEmit;
		this.nbHiddenStates = nbHiddenStates;
		this.EM_TOLERANCE = EM_TOLERANCE;
		this.EPS = EPS;
		this.ACCETABLE_LL_DECREASE = ACCETABLE_LL_DECREASE;
		this.EM_MAX_ITERS = EM_MAX_ITERS;
		
		this.previousLL = INIT_LL;//opts.INIT_LL;
		this.ll = INIT_LL;
	}
	
	private void reset() {
		nbParameterizingFailed = 0;
		nbStopByEMIteration = 0;
		maxLLDecrease = 0;
		maxLLDecreaseRatio = 0;
		nbLLError = 0;
		previousLL = INIT_LL;
		ll = INIT_LL;
		//opts.maxNbInsWeightRoundTo0PerIterPerHmm = 0;
		//cgReachesTrustRegionBoundary = false;
	}

	/**
	 * Does a fixed number of iterations (see {@link #getNbIterations}) of the
	 * Baum-Welch algorithm.
	 * 
	 * @param initialHmm
	 *          An initial estimation of the expected  This estimate is
	 *          critical as the Baum-Welch algorithm only find local minima of its
	 *          likelihood function.
	 * @param sequences
	 *          The observation sequences on which the learning is based. Each
	 *          sequence must have a length higher or equal to 2.
	 * @return The HMM that best matches the set of observation sequences given
	 *         (according to the Baum-Welch algorithm).
	 * @throws IOException
	 */
	public FeatureHMM learn(FeatureHMM initialHmm, List<? extends List<DataPoint>> sequences)
			throws IOException {
		FeatureHMM hmm = initialHmm;
		
		if (initialHmm.getNbHiddenStates() != nbHiddenStates){
			System.out.println("Error: initialHmm.getnbHiddenStates != nbHiddenStates");
			System.exit(-1);
		}
		
		reset();
		boolean retrainUsingNonParam = false;
		int i = 0;
		for (; i < EM_MAX_ITERS; i++) {
			if (verbose) 
				System.out.println("HMM:\n" + hmm);
			if (!retrainUsingNonParam && parameterizing && nbParameterizingFailed == 1) {
				System.out.println("WARNING: Restart training non-parameterized HMMs");
				//TODO: consider differentiate three cases
				//parameterizing = false;
				retrainUsingNonParam = true;
				reset();
				i = 0;
			}

			EStep(hmm, sequences);
			if (converged(previousLL, ll, EM_TOLERANCE, convergeByAbs, EPS)) {
				String str = "#iter=" + i + "(max=" + EM_MAX_ITERS + ")" + ", LL=" + ll + ", LL diff(valueChange/valueAvg) < " + EM_TOLERANCE + "\n";
				System.out.println(str);
				break;
			}
			
			FeatureHMM nhmm = MStep(hmm, sequences);
			hmm = nhmm;
			getLLSummary();
		}
		if (i >= EM_MAX_ITERS)
			nbStopByEMIteration = 1;
		if (verbose) 
			System.out.println("FINAL HMM:\n" + hmm);
		
		return hmm;
	}
	
	
	private void getLLSummary(){
			//ACCETABLE_LL_DECREASE > 0, ll should be bigger than previousLL
			double diff = previousLL - ll;
			if (previousLL != INIT_LL && (diff > ACCETABLE_LL_DECREASE)) {
				nbLLError++;
				double valueAverage = Math.abs(previousLL + ll + EPS) / 2.0;
				double diffRatio = diff / valueAverage;
				if (diffRatio >  maxLLDecreaseRatio)
					maxLLDecreaseRatio = diffRatio;
				if (diff > maxLLDecrease)
					maxLLDecrease = diff;
			}
			if (ll > 0){
				System.out.println("ERROR: trainset log likelihood > 0!");
				nbLLError++;
				ll = 0;
			}

	}
	
	private void EStep(FeatureHMM hmm, List<? extends List<DataPoint>> sequences) {
		/* gamma and xi arrays are those defined by Rabiner and Juang */
		/* allGamma[n] = gamma array associated to observation sequence n */
		allGamma = new double[sequences.size()][][];
		allFirstDpGamma = new double[sequences.size()][];
		// allXi: p(qt_i, qt+1_j|Dd); seq_id, id_within_seq(one less than
		// observation
		// legnth), t_i, t+1_j
		allXi = new double[sequences.size()][][][];
		nb_xi_datapoints = 0;
		/*
		 * a[i][j] = aijNum[i][j] / aijDen[i] aijDen[i] = expected number of
		 * transitions from state i aijNum[i][j] = expected number of transitions
		 * from state i to j
		 */
		aijNum = new double[nbHiddenStates][nbHiddenStates];
		aijDen = new double[nbHiddenStates];

		Arrays.fill(aijDen, 0.);
		for (int i = 0; i < nbHiddenStates; i++)
			Arrays.fill(aijNum[i], 0.);

		int seqIndex = 0;// q
		double ll = 0;
		if (verbose)
			System.out.println("#Sequences=" + sequences.size());

		for (List<DataPoint> obsSeq : sequences) {
			// if the Object who calls this iterate() method is superclass
			// BaumWelchScaledLearner, then use superclass's
			// generateForwardBackwardCalculator;
			// otherwise use subclass BaumWelchLearner's
			// generateForwardBackwardCalculator
			ForwardBackwardCalculator fbc = generateForwardBackwardCalculator(obsSeq,
					hmm);

//			if (moreVerbose)
//				System.out.println("seqIndex=" + seqIndex + "\tprobability="
//						+ fbc.probability() + "\tLL=" + Math.log(fbc.probability()));
			
			//natural logarithm 
			ll += Math.log(fbc.probability());

			double xi[][][] = null;
			double gamma[][] = null;
			if (obsSeq.size() > 1) {
				// xi[][][] = new double[sequence.size() - 1][nbHiddenStates][hmm
				// .nbHiddenStates];
				/*
				 * allXi: p(qt_i, qt+1_j|Dd); seq_id, id_within_seq(one less than
				 * observation length), t_i, t+1_j
				 * 
				 * xi: id_within_seq(one less than observation length), t_i, t+1_j
				 */
				xi = allXi[seqIndex] = estimateXi(obsSeq, fbc, hmm);
				// gamma = new double[xi.length + 1][xi[0].length];, gamma has two
				// hidden states
				gamma = allGamma[seqIndex] = estimateGamma(xi, fbc);
				// xi = allXi[g++] = estimateXi(obsSeq, fbc, hmm);
				// gamma = allGamma[g++] = estimateGamma(xi, fbc);
				nb_xi_datapoints += allXi[seqIndex].length;

			}
			else {
				// TODO: hy commented exit(1) on 12/17/2014
				// System.out.println("WARNING: obsSeq.size <=1!");
				// System.exit(1);
				gamma = allGamma[seqIndex] = estimateGamma(obsSeq, fbc, hmm);
				// gamma = allGamma[g++] = estimateGamma(obsSeq, fbc, hmm);
			}
			allFirstDpGamma[seqIndex] = allGamma[seqIndex][0];
			seqIndex++;


			// if obsSeq.size <=1, this part will be skipped
			for (int i = 0; i < nbHiddenStates; i++)
				for (int t = 0; t < obsSeq.size() - 1; t++) {
					aijDen[i] += gamma[t][i];

					for (int j = 0; j < nbHiddenStates; j++)
						aijNum[i][j] += xi[t][i][j];// todo
				}
		}
//		if (verbose) {
//			// if (opts.currentKc.equals("Loops"))
//			System.out.println("KC=" + kcName + "\t"
//					+ opts.currentBaumWelchIteration + "th LL:\t" + ll + "\t");
//		}
		this.previousLL = this.ll;
		this.ll = ll;
	}

	private FeatureHMM MStep(FeatureHMM hmm, List<? extends List<DataPoint>> sequences) {
			FeatureHMM nhmm;//nhmm is for M-step to set new value
			try {
				nhmm = hmm.clone();
			}
			catch (CloneNotSupportedException e) {
				throw new InternalError();
			}
	// /* aij computation */
			// for (int i = 0; i < nbHiddenStates; i++) {
			// if (aijDen[i] == 0.) // State i is not reachable
			// for (int j = 0; j < nbHiddenStates; j++){
			// nsetAij(i, j, getAij(i, j));
			// }
			// else
			// for (int j = 0; j < nbHiddenStates; j++)
			// nsetAij(i, j, aijNum[i][j] / aijDen[i]);
			// }
			//
			// /* pi computation */
			// for (int i = 0; i < nbHiddenStates; i++)
			// nsetPi(i, 0.);
			// // o is per sequence
			// for (int o = 0; o < sequences.size(); o++)
			// for (int i = 0; i < nbHiddenStates; i++)
			// nsetPi(i, ngetPi(i) + allGamma[o][0][i] / sequences.size());

			List<DataPoint> observations = flat(sequences);
			List<DataPoint> observations_xi = new ArrayList<DataPoint>();
			ArrayList<DataPoint> firstDpObservations = new ArrayList<DataPoint>();
			/* pdfs computation */
			double[][] flatGammaForStates = null;
			double[][] flatFirstDpGammaForStates = null;
			double[][][] flatXiForStates = null;

			if (parameterizing && nbParameterizingFailed == 0) {
				flatGammaForStates = new double[nbHiddenStates][observations.size()];
				flatFirstDpGammaForStates = new double[nbHiddenStates][sequences
						.size()];
				flatXiForStates = new double[nbHiddenStates][nbHiddenStates][observations
						.size() - 1];
			}

			//opts.currentIterfeatureWeights_ = "";
			for (int i = 0; i < nbHiddenStates; i++) {
				double[] flatGammaForStateI = new double[observations.size()];
				double[] flatFirstDpGammaForStateI = new double[sequences.size()];
				double[][] flatXiForStateI = new double[nbHiddenStates][nb_xi_datapoints];
				// TODO: hy commented on 12/17/2014 because for every sequence xi is one
				// observation less
				// double[][] flatXiForStateI = new
				// double[opts.nbHiddenStates][observations
				// .size() - 1];
				double[] ajNum = aijNum[i];
				double ajDen = aijDen[i];
				double[] aj = new double[nbHiddenStates];
				if (!parameterizedTran || !parameterizing || (parameterizing && nbParameterizingFailed == 1))
					for (int j = 0; j < nbHiddenStates; j++)
						// TODO: following original code, if state i is not reachable, using
						// original hmm's aij
						aj[j] = hmm.getTransitionij(i, j, null);

				// o is per sequence
				int k = 0;// counter of all dps in xi
				int k_ = 0; // counter of all dps
				for (int o = 0; o < sequences.size(); o++) {
					List<DataPoint> aObsSeq = sequences.get(o);
					for (int t = 0; t < aObsSeq.size(); t++, k_++) {
						// System.out.println("k:" + k);
						flatGammaForStateI[k_] = allGamma[o][t][i];
						if (t < aObsSeq.size() - 1) {// if aObsSeq.size()=1, it will not enter
							for (int j = 0; j < nbHiddenStates; j++)
								// allXi: seq_id, id_within_seq, t_i, t+1_j
								flatXiForStateI[j][k] = allXi[o][t][i][j];
							if (i == 0)
								observations_xi.add(aObsSeq.get(t));
							k++;
						}
					}
					flatFirstDpGammaForStateI[o] = allFirstDpGamma[o][i];
					if (i == 0)
						firstDpObservations.add(aObsSeq.get(0));
				}

				if (parameterizing && nbParameterizingFailed == 0) {
					flatGammaForStates[i] = flatGammaForStateI;
					flatFirstDpGammaForStates[i] = flatFirstDpGammaForStateI;
					// flatXiForStates: 1st: t step hiddenstates; 2nd: t+1 step
					// hiddenstates; 3rd: all datapoints
					flatXiForStates[i] = flatXiForStateI;
				}
//				if (opts.parameterizing && !opts.oneLogisticRegression) {
//					System.out
//							.println("WARNING: no supporting for twoLogisticRegression now!");
//					System.exit(1);
//				}
				// if (!opts.parameterizing) {
				// PdfFeatureAwareLogisticRegression pdf = ngetInitialPdf(i);
				// pdf.fit(firstDpObservations, flatFirstDpGammaForStateI, i, "init",
				// false);
				// pdf = ngetTransitionPdf(i);
				// pdf.fit(observations, ajNum, ajDen, aj, "trans", false);
				// pdf = ngetEmissionPdf(i);
				// pdf.fit(observations, flatGammaForStateI, i, "emi", false);
				// }
				// else if (!(opts.parameterizedInit && opts.parameterizedEmit &&
				// opts.parameterizedTrans)) {
				if (!parameterizedInit || !parameterizing || (parameterizing && nbParameterizingFailed == 1)) {
					PdfFeatureAwareLogisticRegression pdf = nhmm.getInitialPdf(i);
					pdf.fitNonParameterizedProb(firstDpObservations, flatFirstDpGammaForStateI, "init");
					//pdf.fit(firstDpObservations, flatFirstDpGammaForStateI, "init", i);
				}
				if (!parameterizedTran|| !parameterizing || (parameterizing && nbParameterizingFailed == 1)) {
					PdfFeatureAwareLogisticRegression pdf = nhmm.getTransitionPdf(i);
					pdf.fitNonParameterizedProb(observations, ajNum, ajDen, aj, "trans");
					//pdf.fit(observations, ajNum, ajDen, aj, "trans");
				}

				if (!parameterizedEmit|| !parameterizing || (parameterizing && nbParameterizingFailed == 1)) {
					PdfFeatureAwareLogisticRegression pdf = nhmm.getEmissionPdf(i);
					pdf.fitNonParameterizedProb(observations, flatGammaForStateI, "emit");
					//pdf.fit(observations, flatGammaForStateI, "emit", i);
				}
			}// after get flatGammaForStates
				// }

			if (parameterizing && nbParameterizingFailed == 0) {
				if (parameterizedInit) {
					PdfFeatureAwareLogisticRegression initPdf = nhmm.getInitialPdf(0);
					initPdf.fitParameterizedProbByInstanceWeights(firstDpObservations, flatFirstDpGammaForStates, "init");
					nbParameterizingFailed = initPdf.getParameterizingResult();
					//initPdf.fit(firstDpObservations, flatFirstDpGammaForStates, "init");
					PdfFeatureAwareLogisticRegression initPdf1 = nhmm.getInitialPdf(1);
					initPdf1.setFeatureWeights(initPdf.getFeatureWeights());
//					if (opts.writeFeatureWeightsLog)
//						opts.currentIterfeatureWeights_ += "\tinitPdf\t" + initPdf + "";
				}
				if (parameterizedTran) {
					PdfFeatureAwareLogisticRegression transPdf = nhmm.getTransitionPdf(0);
					transPdf.fitParameterizedTransByInstanceWeights(observations_xi, flatXiForStates);
					nbParameterizingFailed = transPdf.getParameterizingResult();
					// hy changed from observations to observations_xi
					//transPdf.fit(observations_xi, flatXiForStates, "trans");
					PdfFeatureAwareLogisticRegression transPdf1 = nhmm.getTransitionPdf(1);
					transPdf1.setFeatureWeights(transPdf.getFeatureWeights());
//					if (opts.writeFeatureWeightsLog)
//						opts.currentIterfeatureWeights_ += "\ttransPdf\t" + transPdf + "";
				}
				if (parameterizedEmit) {
					PdfFeatureAwareLogisticRegression emitPdf = nhmm.getEmissionPdf(0);
					emitPdf.fitParameterizedProbByInstanceWeights(observations, flatGammaForStates, "emit");
					nbParameterizingFailed = emitPdf.getParameterizingResult();
					//emitPdf.fit(observations, flatGammaForStates, "emit");
					// opdf1 is not fitted even I didn't use new
					// ArrayList<OpdfContextAware...> when initializating HMM
					// TODO: should make iteration
					PdfFeatureAwareLogisticRegression emitPdf1 = nhmm.getEmissionPdf(1);
					emitPdf1.setFeatureWeights(emitPdf.getFeatureWeights());
//					if (opts.writeFeatureWeightsLog)
//						opts.currentIterfeatureWeights_ += "\temitPdf\t" + emitPdf + "";
				}
			}
			// System.out.println(nhmm);
			// print(flatGammaForStates, "flatGamma");
			return nhmm;

	}



	/* Use original LBFGS's convergence judgement function*/
	private boolean converged(double value, double nextValue, double tolerance, boolean convergeByAbs, double EPS) {
		if (value == nextValue)
			return true;
		double valueChange = Math.abs(nextValue - value);
		if (convergeByAbs)
			if (valueChange < tolerance)
				return true;
			else
				return false;
		double valueAverage = Math.abs(nextValue + value + EPS) / 2.0;
		//System.out.println(valueChange + "," + valueChange / valueAverage);
		if (valueChange / valueAverage < tolerance)
			return true;
		return false;
	}
	

	

	//"protected" original
	private ForwardBackwardCalculator generateForwardBackwardCalculator(
			List<DataPoint> sequence, FeatureHMM hmm) {
		// hy:
		// System.out.println("ForwardBackwardCalculator...");
		return new ForwardBackwardCalculator(sequence, hmm,
				EnumSet.allOf(ForwardBackwardCalculator.Computation.class));
	}

	//"protected" original
 //xi: p(qt_i, qt+1_j|Dd)
	private double[][][] estimateXi(List<DataPoint> sequence,
			ForwardBackwardCalculator fbc, FeatureHMM hmm) {
		if (verbose)
			System.out.println("Non Scaled!");
		// if (sequence.size() <= 1)
		// throw new IllegalArgumentException("Observation sequence too " +
		// "short");

		double xi[][][] = new double[sequence.size() - 1][hmm.getNbHiddenStates()][hmm.getNbHiddenStates()];
		double probability = fbc.probability();

		Iterator<DataPoint> seqIterator = sequence.iterator();
		seqIterator.next();
		// DataPoint currentO = seqIterator.next();

		for (int t = 0; t < sequence.size() - 1; t++) {
			DataPoint nextO = seqIterator.next();

			for (int i = 0; i < hmm.getNbHiddenStates(); i++) {
				// double[] currentOFeatureValues = currentO.getFeatures(i);
				for (int j = 0; j < hmm.getNbHiddenStates(); j++) {
					// double[] nextOFeatureValues = nextO.getFeatures(j);
					// TODO: hy changed nextO.getFeatures(j) to nextO.getFeatures(i) on
					// 12/17/2014
					xi[t][i][j] = fbc.alphaElement(t, i)
							* hmm.getTransitionij(i, j, nextO.getFeatures(i, 1))
							* hmm.getEmissionjk(j, nextO.getOutcome(), nextO.getFeatures(j, 2))
							* fbc.betaElement(t + 1, j) / probability;
				}
			}
			// currentO = nextO;
		}

		return xi;
	}

	/**
	 * @author hy
	 * @date 11/16/13 When current sequence's length is 1, use alpha, beta, P(Dd)
	 *       instead of xi to estimate gamma
	 */
	//"protected" original
	private double[][] estimateGamma(List<DataPoint> sequence,
			ForwardBackwardCalculator fbc, FeatureHMM hmm) {
		if (verbose)
			System.out.println("Non Scaled!");
		double[][] gamma = new double[sequence.size()][hmm.getNbHiddenStates()];

		for (int t = 0; t < sequence.size(); t++)
			Arrays.fill(gamma[t], 0.);

		double probability = fbc.probability();

		for (int t = 0; t < sequence.size(); t++) {
			for (int i = 0; i < hmm.getNbHiddenStates(); i++) {
				gamma[t][i] = fbc.alphaElement(t, i) * fbc.betaElement(t, i)
						/ probability;
			}
		}

		return gamma;
	}

	/**
	 * gamma[][] could be computed directly using the alpha and beta arrays, but
	 * this (slower) method is preferred because it doesn't change if the xi array
	 * has been scaled (and should be changed with the scaled alpha and beta
	 * arrays).
	 * 
	 * "protected" original
	 */
	private double[][] estimateGamma(double[][][] xi,
			ForwardBackwardCalculator fbc) {
		double[][] gamma = new double[xi.length + 1][xi[0].length];

		// hy: it is wrong to put the last t of gamma to be 0
		for (int t = 0; t < xi.length + 1; t++)
			Arrays.fill(gamma[t], 0.);

		for (int t = 0; t < xi.length; t++)
			for (int i = 0; i < xi[0].length; i++)
				for (int j = 0; j < xi[0].length; j++)
					gamma[t][i] += xi[t][i][j];

		for (int j = 0; j < xi[0].length; j++)
			for (int i = 0; i < xi[0].length; i++)
				gamma[xi.length][j] += xi[xi.length - 1][i][j];

		return gamma;
	}

//	/**
//	 * Returns the number of iterations performed by the {@link #learn} method.
//	 * 
//	 * @return The number of iterations performed.
//	 */
//	public int getNbIterations() {
//		return nbIterations;
//	}

//	/**
//	 * Sets the number of iterations performed by the {@link #learn} method.
//	 * 
//	 * @param nb
//	 *          The (positive) number of iterations to perform.
//	 */
//	public void setNbIterations(int nb) {
//		if (nb < 0)
//			throw new IllegalArgumentException("Positive number expected");
//
//		nbIterations = nb;
//	}
	
	private static <T> List<T> flat(List<? extends List<? extends T>> lists) {
	List<T> v = new ArrayList<T>();
	
	for (List<? extends T> list : lists)
		v.addAll(list);
	
	return v;
	}
	
	public double getTrainLL(){
		return ll;
	}
	
	public int getNbLLError(){
		return nbLLError;
	}

	public double getMaxLLDecrease(){
		return maxLLDecrease;
	}
	
	public double getMaxLLDecreaseRatio(){
		return maxLLDecreaseRatio;
	}
	
	public int getNbParameterizingFailed(){
		return nbParameterizingFailed;
	}
	
	public int getNbStopByEMIteration(){
		return nbStopByEMIteration;
	}
	
//	public TrainSummary getTrainSummary(){
//		TrainSummary trainSummary = new TrainSummary();
//		trainSummary.update(nbLLError, maxLLDecrease, maxLLDecreaseRatio, parameterizingSucceeded, stopByEMIteration, ll);
//		return trainSummary;
//	}

}
