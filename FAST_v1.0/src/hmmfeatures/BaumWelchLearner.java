/*
 * @author hy
 * @date 10/06/13
 * 
 * Use Hmm class in hmmfeatures package instead of that in jahmm package
 * 
 * This is built based on:
 *  jahmm package - v0.6.1 
 *  Copyright (c) 2004-2006, Jean-Marc Francois.
 *  
 */

package hmmfeatures;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.List;
import java.util.TreeSet;
import common.Bijection;
import data.DataPoint;
import data.StudentList;
//import be.ac.ulg.montefiore.run.jahmm.ForwardBackwardCalculator;
//import be.ac.ulg.montefiore.run.jahmm.Hmm;

/**
 * An implementation of the Baum-Welch learning algorithm. This algorithm finds
 * a HMM that models a set of observation sequences.
 */
public class BaumWelchLearner {

	// current iteration's all data ll; belongs to an object
	// (not class)
	private double ll;
	private double previousLL;
	public double allGamma[][][];
	private double aijNum[][];
	private double aijDen[];

	/**
	 * Number of iterations performed by the {@link #learn} method.
	 */
	private int nbIterations = 0;
	private static Opts opts;
	// private static Logger logger = Logger.getLogger("");
	private boolean verbose = false;
	private boolean moreVerbose = false;

	/**
	 * Initializes a Baum-Welch instance.
	 * 
	 * @throws IOException
	 */
	public BaumWelchLearner(String KCName, Opts opts) {
		this.opts = opts;
		this.verbose = opts.verbose;
		opts.maxLLDecreaseValuePerIterPerHmm = 0;
		opts.maxNbInsWeightRoundTo0PerIterPerHmm = 0;
		opts.nbLlError = 0;
		this.previousLL = opts.INIT_LL;
		opts.cgReachesTrustRegionBoundary = false;
	}

	/**
	 * Does a fixed number of iterations (see {@link #getNbIterations}) of the
	 * Baum-Welch algorithm.
	 * 
	 * @param initialHmm
	 *          An initial estimation of the expected HMM. This estimate is
	 *          critical as the Baum-Welch algorithm only find local minima of its
	 *          likelihood function.
	 * @param sequences
	 *          The observation sequences on which the learning is based. Each
	 *          sequence must have a length higher or equal to 2.
	 * @return The HMM that best matches the set of observation sequences given
	 *         (according to the Baum-Welch algorithm).
	 * @throws IOException
	 */
	public Hmm learn(Hmm initialHmm, List<? extends List<DataPoint>> sequences)
			throws IOException {
		Hmm hmm = initialHmm;

		boolean retrainUsingNonParam = false;
		for (int i = 0; i < nbIterations; i++) {
			if (!retrainUsingNonParam && !opts.modelName.contains("KT")
					&& opts.parameterizedEmit == false) {
				System.out.println("Restart training non-parameterized HMMs");
				opts.hmmsForcedToNonParmTrainDueToLBFGSException.add(opts.currentKc);
				retrainUsingNonParam = true;
				reconfigure();
				i = 0;
			}
			opts.currentBaumWelchIteration = i;
			// if (verbose)
			// System.out.println("\n*** BaumWelchLearner iter=" + i + " ***");
			// reassignRealHiddenState(hmm, sequences);
			if (opts.verbose)
				System.out.println(hmm);
			if (i == 0) {
				String info = "KC=" + opts.currentKc + "\tBEFORE iter="
						+ opts.currentBaumWelchIteration + "\t~~~~~~\t~~~~~~\t~~~\t";
				// if (opts.writeForTheBestAucOnDev)
				writeHmm(hmm, sequences, info);
			}
			// hmm = iterate(hmm, sequences);// hy
			Hmm nhmm;
			try {
				nhmm = hmm.clone();
			}
			catch (CloneNotSupportedException e) {
				throw new InternalError();
			}

			EStep(hmm, sequences);

			if (converged(previousLL, ll, opts.EM_TOLERANCE)) {
				// logger.info("KC=" + opts.currentKc + "\titer="
				// + opts.currentBaumWelchIteration + "\tLL diff < "
				// + opts.EM_TOLERANCE);
				String str = "KC=" + opts.currentKc + "\titer="
						+ opts.currentBaumWelchIteration + "\tLL:\t" + ll
						+ "\tLL diff(valueChange/valueAvg) < " + opts.EM_TOLERANCE;
				System.out.println(str);
				if (opts.writeForTheBestAucOnDev) {
					if (opts.writeMainLog) {
						opts.mainLogWriter.write(str + "\n");
						opts.mainLogWriter.flush();
					}
				}
				// if (opts.writeForTheBestAucOnDev)
				storeAndWriteInfoPerHmmPerIter(hmm, sequences, ll);
				break;
			}
			nhmm = MStep(hmm, nhmm, sequences);

			// if (opts.writeForTheBestAucOnDev)
			storeAndWriteInfoPerHmmPerIter(nhmm, sequences, ll);
			opts.cgReachesTrustRegionBoundary = false;
			// should check if nhmm is ok, then assign it to hmm
			hmm = nhmm;
		}
		// reassignRealHiddenState(hmm, sequences);
		// if (opts.writeForTheBestAucOnDev)
		storeAndWriteSummaryPerHmm(hmm, sequences);
		reconfigure();
		return hmm;
	}

	/**
	 * Performs one iteration of the Baum-Welch algorithm. In one iteration, a new
	 * HMM is computed using a previously estimated HMM.
	 * 
	 * @param hmm
	 *          A previously estimated HMM.
	 * @param sequences
	 *          The observation sequences on which the learning is based. Each
	 *          sequence must have a length higher or equal to 2.
	 * @return A new, updated HMM.
	 * @throws IOException
	 */
	public Hmm iterate(Hmm hmm, List<? extends List<DataPoint>> sequences)
			throws IOException {
		Hmm nhmm;
		try {
			nhmm = hmm.clone();
		}
		catch (CloneNotSupportedException e) {
			throw new InternalError();
		}

		EStep(hmm, sequences);
		nhmm = MStep(hmm, nhmm, sequences);

		return nhmm;
	}

	public void EStep(Hmm hmm, List<? extends List<DataPoint>> sequences) {
		/* gamma and xi arrays are those defined by Rabiner and Juang */
		/* allGamma[n] = gamma array associated to observation sequence n */
		allGamma = new double[sequences.size()][][];

		/*
		 * a[i][j] = aijNum[i][j] / aijDen[i] aijDen[i] = expected number of
		 * transitions from state i aijNum[i][j] = expected number of transitions
		 * from state i to j
		 */
		aijNum = new double[hmm.nbStates()][hmm.nbStates()];
		aijDen = new double[hmm.nbStates()];

		Arrays.fill(aijDen, 0.);
		for (int i = 0; i < hmm.nbStates(); i++)
			Arrays.fill(aijNum[i], 0.);

		int g = 0;
		double ll = 0;
		int seqID = 0;
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
			if (moreVerbose)
				System.out.println("seqIndex=" + seqID + "\tprobability="
						+ fbc.probability() + "\tLL=" + Math.log(fbc.probability()));
			seqID = seqID + 1;

			ll += Math.log(fbc.probability());

			double xi[][][] = null;
			double gamma[][] = null;
			if (obsSeq.size() > 1) {
				xi = estimateXi(obsSeq, fbc, hmm);
				gamma = allGamma[g++] = estimateGamma(xi, fbc);
			}
			else {
				gamma = allGamma[g++] = estimateGamma(obsSeq, fbc, hmm);
				// System.exit(1);
			}

			// if obsSeq.size <=1, this part will be skipped
			for (int i = 0; i < hmm.nbStates(); i++)
				for (int t = 0; t < obsSeq.size() - 1; t++) {
					aijDen[i] += gamma[t][i];

					for (int j = 0; j < hmm.nbStates(); j++)
						aijNum[i][j] += xi[t][i][j];// todo
				}
		}
		if (verbose) {
			// if (opts.currentKc.equals("Loops"))
			System.out.println("KC=" + opts.currentKc + "\t"
					+ opts.currentBaumWelchIteration + "th LL:\t" + ll + "\t");
		}
		this.ll = ll;
	}

	public Hmm MStep(Hmm hmm, Hmm nhmm, List<? extends List<DataPoint>> sequences) {

		for (int i = 0; i < hmm.nbStates(); i++) {
			if (aijDen[i] == 0.) // State i is not reachable
				for (int j = 0; j < hmm.nbStates(); j++)
					nhmm.setAij(i, j, hmm.getAij(i, j));
			else
				for (int j = 0; j < hmm.nbStates(); j++)
					nhmm.setAij(i, j, aijNum[i][j] / aijDen[i]);
		}

		/* pi computation */
		for (int i = 0; i < hmm.nbStates(); i++)
			nhmm.setPi(i, 0.);

		// o is per sequence
		for (int o = 0; o < sequences.size(); o++)
			for (int i = 0; i < hmm.nbStates(); i++)
				nhmm.setPi(i, nhmm.getPi(i) + allGamma[o][0][i] / sequences.size());

		/* pdfs computation */
		double[][] flatGammaForStates = null;
		List<DataPoint> observations = KMeansLearner.flat(sequences);

		if (opts.oneLogisticRegression)
			flatGammaForStates = new double[opts.nbHiddenStates][];

		for (int i = 0; i < hmm.nbStates(); i++) {
			double[] weights = new double[observations.size()];
			double sum = 0.;
			int j = 0;

			// o is per sequence
			for (int o = 0; o < sequences.size(); o++) {
				List<DataPoint> aObsSeq = sequences.get(o);
				for (int t = 0; t < aObsSeq.size(); t++, j++)
					sum += weights[j] = allGamma[o][t][i];
			}

			double[] flatGammaForStateI = Arrays.copyOf(weights, weights.length);
			if (opts.oneLogisticRegression)
				flatGammaForStates[i] = flatGammaForStateI;

			if (!opts.parameterizedEmit
					|| (opts.parameterizedEmit && !opts.oneLogisticRegression && !opts.generateLRInputs)
					|| (opts.generateLRInputs && i != 0)) {
				OpdfContextAware<DataPoint> opdf = nhmm.getOpdf(i);
				if (verbose) {
					System.out.println("\nBefore fitting (state " + i + "):");
					System.out.println(opdf);
				}

				// 2.use gamma(i,t), in each t, different states should sum up to one
				opdf.fit(observations, flatGammaForStateI, i);

				if (verbose) {
					System.out.println("After fitting (state " + i + "):");
					System.out.println(opdf);
				}
				opts.currentIterfeatureWeights[i] = opdf + "";
			}
		}// after get flatGammaForStates

		if (opts.oneLogisticRegression) {
			OpdfContextAwareLogisticRegression opdf = nhmm.getOpdf(0);
			opdf.fit(observations, flatGammaForStates);
			// opdf1 is not fitted even I didn't use new
			// ArrayList<OpdfContextAware...> when initializating HMM
			// TODO: should make iteration
			OpdfContextAwareLogisticRegression opdf1 = nhmm.getOpdf(1);
			opdf1.featureWeights = opdf.featureWeights;
			// System.out.println();
			opts.currentIterfeatureWeights_ = opdf + "";
		}
		return nhmm;

	}

	public void reassignRealHiddenState(Hmm hmm,
			List<? extends List<DataPoint>> sequences) {
		double p0 = hmm.getPi(0);
		double a00 = hmm.getAij(0, 0);
		double a01 = hmm.getAij(0, 1);
		// double Opdf00 =
		// double Opdf01 =
		double p1 = hmm.getPi(1);
		double a10 = hmm.getAij(1, 0);
		double a11 = hmm.getAij(1, 1);

		DataPoint dp = sequences.get(0).get(0);
		if (opts.useEmissionToJudgeHiddenStates) {
			double hidden0obs1 = hmm.getOpdf(0).probability(dp.getFeatures(0),
					opts.obsClass1);
			double hidden1obs1 = hmm.getOpdf(1).probability(dp.getFeatures(1),
					opts.obsClass1);
			if (hidden0obs1 > hidden1obs1) {
				opts.hiddenState1 = 0;// real know
				opts.hiddenState0 = 1;// real notknow
			}
			else {
				opts.hiddenState1 = 1;// know
				opts.hiddenState0 = 0;// unknow
			}
		}
		else {
			if (a01 < a10) {
				opts.hiddenState1 = 0;// know
				opts.hiddenState0 = 1;// notknow
			}
			else {
				opts.hiddenState1 = 1;// know
				opts.hiddenState0 = 0;// unknow
			}
		}
	}

	protected boolean converged(double value, double nextValue, double tolerance) {
		if (value == nextValue)
			return true;
		double valueChange = Math.abs(nextValue - value);
		double valueAverage = Math.abs(nextValue + value + opts.EPS) / 2.0;
		if (valueChange / valueAverage < tolerance)
			return true;
		return false;
	}

	public void reconfigure() {
		opts.maxLLDecreaseValuePerIterPerHmm = 0;
		opts.maxLLDecreaseRatioValuePerIterPerHmm = 0;
		opts.maxNbInsWeightRoundTo0PerIterPerHmm = 0;
		opts.nbLlError = 0;
		previousLL = opts.INIT_LL;
		ll = 0.0;
		opts.cgReachesTrustRegionBoundary = false;
	}

	public void writeHmm(Hmm hmm, List<? extends List<DataPoint>> sequences,
			String info) throws IOException {

		// currently, the code doesn't paramterized init, transition, so we can get
		// it directly
		double p0 = hmm.getPi(opts.hiddenState0);
		double a00 = hmm.getAij(opts.hiddenState0, opts.hiddenState0);
		double a01 = hmm.getAij(opts.hiddenState0, opts.hiddenState1);
		// double Opdf00 =
		// double Opdf01 =
		double p1 = hmm.getPi(opts.hiddenState1);
		double a10 = hmm.getAij(opts.hiddenState1, opts.hiddenState0);
		double a11 = hmm.getAij(opts.hiddenState1, opts.hiddenState1);

		if (opts.writeFinalHmmParameters && info.equals("FINAL")) {
			if (!opts.parameterizedEmit) {
				double b00 = hmm.getOpdf(opts.hiddenState0).probabilities[0];
				double b01 = hmm.getOpdf(opts.hiddenState0).probabilities[1];
				double b10 = hmm.getOpdf(opts.hiddenState1).probabilities[0];
				double b11 = hmm.getOpdf(opts.hiddenState1).probabilities[1];
				// ("hmmId\tpi0\ta00\ta01\tb00\tb01\tpi1\ta10\ta11\tb10\tb11\n");
				String outStr = opts.currentKc + "\t" + p0 + "\t" + a00 + "\t" + a01
						+ "\t" + b00 + "\t" + b01 + "\t" + +p1 + "\t" + a10 + "\t" + a11
						+ "\t" + b10 + "\t" + b11 + "\n";
				opts.finalHmmParametersWriter.write(outStr);
				opts.finalHmmParametersWriter.flush();
			}
			else {
				String outStr = opts.currentKc + "\t" + p0 + "\t" + a00 + "\t" + a01
						+ "\t" + "~" + "\t" + "~" + "\t" + +p1 + "\t" + a10 + "\t" + a11
						+ "\t" + "~" + "\t" + "~" + "\n";
				opts.finalHmmParametersWriter.write(outStr);
				opts.finalHmmParametersWriter.flush();
			}

		}

		// KC \t iter \t LL: \t LLVALUE \t message \t hidden0 \t realInfo \t
		// hidden1 \t realnfo..
		if (opts.writeInitLearnForgetProbLog) {
			opts.initLearnForgetProbLogWriter.write(info + "unknown\t"
					+ opts.formatter.format(p0) + "\t" + opts.formatter.format(a00)
					+ "\t" + opts.formatter.format(a01) + "\tknown\t"
					+ opts.formatter.format(p1) + "\t" + opts.formatter.format(a10)
					+ "\t" + opts.formatter.format(a11) + "\n");
			opts.initLearnForgetProbLogWriter.flush();
		}
		// hy: print out guess and slip
		if (opts.writeGuessSlipProbLog || opts.writeGuessSlipProbLog2) {
			String basicStr = info;
			String outStr = "";// differnet hiddenStates in separate lines
			String outStr2 = basicStr; // in one line
			String outStr3 = "";
			if (opts.parameterizedEmit) {
				int[] realHiddenStates = { opts.hiddenState0, opts.hiddenState1 };
				for (int index = 0; index < realHiddenStates.length; index++) {
					outStr += basicStr;
					outStr3 += basicStr;
					int hiddenState = realHiddenStates[index];
					for (int obsState = opts.nbObsStates - 1; obsState >= 0; obsState--) {
						outStr += "B" + hiddenState + obsState + "\t";
						if (index == 0)
							outStr3 += "B" + hiddenState + obsState + "-B"
									+ (1 - hiddenState) + obsState + "\t";
						outStr2 += "B" + hiddenState + obsState + "\t";
						for (List<DataPoint> obsSeq : sequences) {
							for (DataPoint o : obsSeq) {
								// hy: i is the hidden state
								// alpha[0][i] = hmm.getPi(i)
								// * hmm.getOpdf(i).probability(o.getFeatures(), o.getOutcome(),
								// i);
								double bij = hmm.getOpdf(hiddenState).probability(
										o.getFeatures(hiddenState), obsState);
								double bi_j = hmm.getOpdf(1 - hiddenState).probability(
										o.getFeatures(1 - hiddenState), obsState);
								outStr += opts.formatter.format(bij) + "\t";
								outStr3 += (bij - bi_j) + "\t";
								outStr2 += opts.formatter.format(bij) + "\t";
							}
						}
					}
					outStr += (hiddenState + 1) < opts.nbHiddenStates ? "\n" : "";
					outStr3 += (hiddenState + 1) < opts.nbHiddenStates ? "\n" : "";
				}
			}
			else {
				double b00 = hmm.getOpdf(opts.hiddenState0).probabilities[0];
				double b01 = hmm.getOpdf(opts.hiddenState0).probabilities[1];
				double b10 = hmm.getOpdf(opts.hiddenState1).probabilities[0];
				double b11 = hmm.getOpdf(opts.hiddenState1).probabilities[1];
				outStr = basicStr + "B00\t" + opts.formatter.format(b00)
						+ "\tB01(guess)\t" + opts.formatter.format(b01) + "\n" + basicStr
						+ "B10(slip)\t" + opts.formatter.format(b10) + "\tB11\t"
						+ opts.formatter.format(b11);
				outStr2 = basicStr + "B00\t" + opts.formatter.format(b00)
						+ "\tB01(guess)\t" + opts.formatter.format(b01) + "\tB10(slip)\t"
						+ opts.formatter.format(b10) + "\tB11\t"
						+ opts.formatter.format(b11);
			}
			if (opts.writeGuessSlipProbLog) {
				opts.guessSlipProbLogWriter.write(outStr + "\n");
				opts.guessSlipProbLogWriter.write(outStr3 + "\n");
				opts.guessSlipProbLogWriter.flush();
			}
			if (opts.writeGuessSlipProbLog2) {
				opts.guessSlipProbLogWriter2.write(outStr2 + "\n");
				opts.guessSlipProbLogWriter2.flush();
			}
		}
	}

	public void storeAndWriteSummaryPerHmm(Hmm hmm,
			List<? extends List<DataPoint>> sequences) throws IOException {
		String llErrorStr = "KC=" + opts.currentKc + "\t#LLError:\t"
				+ opts.nbLlError;
		// logger.error(llErrorStr);
		System.out.println(llErrorStr);
		String maxllDecreasePerHmmStr = "KC=" + opts.currentKc
				+ "\tmaxLLDecreaseValuePerIterPerHmm:\t"
				+ opts.maxLLDecreaseValuePerIterPerHmm;
		// logger.error(maxllDecreasePerHmmStr);
		System.out.println(maxllDecreasePerHmmStr);
		opts.maxLLDecreaseValuePerIter = (opts.maxLLDecreaseValuePerIterPerHmm > opts.maxLLDecreaseValuePerIter) ? opts.maxLLDecreaseValuePerIterPerHmm
				: opts.maxLLDecreaseValuePerIter;
		String maxllDecreaseStr = "Up till now all KCs:"
				+ "\tmaxLLDecreaseValuePerIter:\t" + opts.maxLLDecreaseValuePerIter;
		// logger.error(maxllDecreaseStr);
		System.out.println(maxllDecreaseStr);
		String maxllDecreaseRatioPerHmmStr = "KC=" + opts.currentKc
				+ "\tmaxLLDecreaseRatioValuePerIterPerHmm:\t"
				+ opts.maxLLDecreaseRatioValuePerIterPerHmm;
		// logger.error(maxllDecreaseRatioPerHmmStr);
		System.out.println(maxllDecreaseRatioPerHmmStr);
		opts.maxLLDecreaseRatioValuePerIter = (opts.maxLLDecreaseRatioValuePerIterPerHmm > opts.maxLLDecreaseRatioValuePerIter) ? opts.maxLLDecreaseRatioValuePerIterPerHmm
				: opts.maxLLDecreaseRatioValuePerIter;
		String maxllDecreaseRatioStr = "Up till now all KCs:"
				+ "\tmaxLLDecreaseRatioValuePerIter:\t"
				+ opts.maxLLDecreaseRatioValuePerIter;
		// logger.error(maxllDecreaseRatioStr);
		System.out.println(maxllDecreaseRatioStr);

		if (opts.writeMainLog) {
			if (opts.nbLlError > 0)
				opts.mainLogWriter.write(llErrorStr + "\n");
			if (opts.maxLLDecreaseValuePerIterPerHmm > 0)
				opts.mainLogWriter.write(maxllDecreasePerHmmStr + "\n");
			if (opts.maxLLDecreaseValuePerIter > 0)
				opts.mainLogWriter.write(maxllDecreaseStr + "\n");
			if (opts.maxLLDecreaseRatioValuePerIterPerHmm > 0)
				opts.mainLogWriter.write(maxllDecreaseRatioPerHmmStr + "\n");
			if (opts.maxLLDecreaseRatioValuePerIter > 0)
				opts.mainLogWriter.write(maxllDecreaseRatioStr + "\n");
		}

		if (opts.INSTANCE_WEIGHT_ROUNDING_THRESHOLD > 0) {
			String maxRound0PerHmmStr = "KC=" + opts.currentKc
					+ "\tmax#InstanceWeightRoundTo0PerHmm:\t"
					+ opts.maxNbInsWeightRoundTo0PerIterPerHmm;
			// logger.error(maxRound0PerHmmStr);
			System.out.println(maxRound0PerHmmStr);
			opts.maxNbInsWeightRoundTo0PerIter = (opts.maxNbInsWeightRoundTo0PerIterPerHmm > opts.maxNbInsWeightRoundTo0PerIter) ? opts.maxNbInsWeightRoundTo0PerIterPerHmm
					: opts.maxNbInsWeightRoundTo0PerIter;
			String maxRound0Str = "Up till now all KCs"
					+ "\tmax#InstanceWeightRoundTo0:\t"
					+ opts.maxNbInsWeightRoundTo0PerIter;
			// logger.error(maxRound0Str);
			System.out.println(maxRound0Str);

			if (opts.writeMainLog) {
				if (opts.maxNbInsWeightRoundTo0PerIterPerHmm > 0)
					opts.mainLogWriter.write(maxRound0PerHmmStr + "\n");
				if (opts.maxNbInsWeightRoundTo0PerIter > 0)
					opts.mainLogWriter.write(maxRound0Str + "\n");
			}
		}

		if (opts.writeFinalHmmParameters)
			writeHmm(hmm, sequences, "FINAL");

		if (opts.writeMainLog) {
			opts.mainLogWriter.write("\n\n");
			opts.mainLogWriter.flush();
		}

		if (opts.writeDeltaPCorrectOnTrain) {
			writeDeltaPCorrect(hmm, sequences);
		}
		if (opts.writeFinalFeatureWeights && opts.parameterizedEmit) {
			writeFinalFeatureWeights(hmm, sequences);
		}

		if (opts.writeDeltaGamma) {
			writeDeltaGamma(sequences);
		}

	}

	public void writeDeltaGamma(List<? extends List<DataPoint>> sequences)
			throws IOException {
		String header = opts.currentKc + " students\t";
		String firstGammaStr = opts.currentKc + " first gamma(known)\t";
		String deltaGammaStr = opts.currentKc + " delta gamma(known)\t";
		String lastGammaStr = opts.currentKc + "h last gamma(known)\t";
		StudentList stuListSequences = (StudentList) sequences;
		Bijection finalStudents = stuListSequences.getFinalStudents();
		Bijection oriStudents = stuListSequences.getOriStudents();
		double accumulatedDeltaGamma = 0.0;
		for (int stuSeqIndex = 0; stuSeqIndex < sequences.size(); stuSeqIndex++) {
			header += oriStudents
					.get(Integer.parseInt(finalStudents.get(stuSeqIndex)))
					+ "\t";
			int curSeqLength = sequences.get(stuSeqIndex).size();
			// allGamma[o][0][i]: stuSeqindex, timeIndex, hiddenState
			// double firstRealHiddenState0Gamma =
			// allGamma[stuSeqIndex][0][opts.hiddenState0];
			double firstRealHiddenState1Gamma = allGamma[stuSeqIndex][0][opts.hiddenState1];
			// double lastRealHiddenState0Gamma = allGamma[stuSeqIndex][curSeqLength -
			// 1][opts.hiddenState0];
			double lastRealHiddenState1Gamma = allGamma[stuSeqIndex][curSeqLength - 1][opts.hiddenState1];

			firstGammaStr += firstRealHiddenState1Gamma + "\t";
			deltaGammaStr += (lastRealHiddenState1Gamma - firstRealHiddenState1Gamma)
					+ "\t";
			accumulatedDeltaGamma += lastRealHiddenState1Gamma
					- firstRealHiddenState1Gamma;
			lastGammaStr += lastRealHiddenState1Gamma + "\t";
			if (opts.writeEachDeltaGamma) {
				opts.eachDeltaGammaWriter
						.write((lastRealHiddenState1Gamma - firstRealHiddenState1Gamma)
								+ "\n");
				opts.eachDeltaGammaWriter.flush();
			}
		}

		// if (opts.currentKc.equals("Objects"))
		// System.out.println();
		double avgDeltaGamma = accumulatedDeltaGamma / sequences.size();
		if (!opts.writeEachDeltaGamma)
			opts.deltaGammaWriter.write(header + "\n");
		if (!opts.writeOnlyDeltaGamma && !opts.writeEachDeltaGamma)
			opts.deltaGammaWriter.write(firstGammaStr + "\n");
		if (!opts.writeEachDeltaGamma)
			opts.deltaGammaWriter.write(deltaGammaStr + "avg\t" + avgDeltaGamma
					+ "\n");
		if (!opts.writeOnlyDeltaGamma && !opts.writeEachDeltaGamma)
			opts.deltaGammaWriter.write(lastGammaStr + "\n");
		if (!opts.writeEachDeltaGamma)
			opts.deltaGammaWriter.flush();
		opts.kcAvgDeltaGammaMap.put(opts.currentKc, avgDeltaGamma);
	}

	public void writeDeltaPCorrect(Hmm hmm,
			List<? extends List<DataPoint>> sequences) throws IOException {
		// ArrayList<Integer> trainTestIndicators = new ArrayList<Integer>();
		// ArrayList<double[]> features = new ArrayList<double[]>();
		// if (
		TreeSet<Integer> turnOffFeatureIndexes = new TreeSet<Integer>();
		Bijection featureMapping = null;
		if (opts.oneLogisticRegression && opts.parameterizedEmit) {
			featureMapping = hmm.getOpdf(0).featureMapping;
		}
		else {
			System.out
					.println("WARNING: no configuration for opts.writeDeltaPCorrectOnTrain && opts.parameterizedEmit!");
			// System.exit(1);
		}
		if (opts.turnOffItemFeaturesWhenWritingDeltaPCorrect
				&& opts.parameterizedEmit) {
			for (int i = 0; i < featureMapping.getSize(); i++) {
				String featureName = featureMapping.get(i);
				if (featureName.contains("*features_")) {
					// featureName = featureName.replace("*features_", "");
					turnOffFeatureIndexes.add(i);
				}
			}
			for (int stuSeqIndex = 0; stuSeqIndex < sequences.size(); stuSeqIndex++) {
				for (int timeIndex = 0; timeIndex < sequences.get(stuSeqIndex).size(); timeIndex++) {
					DataPoint dp = sequences.get(stuSeqIndex).get(timeIndex);
					for (int hiddenStateIndex = 0; hiddenStateIndex < opts.nbHiddenStates; hiddenStateIndex++) {
						double[] features = dp.getFeatures(hiddenStateIndex);
						for (int featureIndex = 0; featureIndex < features.length; featureIndex++) {
							if (turnOffFeatureIndexes.contains(featureIndex))
								features[featureIndex] = 0.0;
						}
					}
				}
			}
			// just to print out
			// for (int stuSeqIndex = 0; stuSeqIndex < sequences.size();
			// stuSeqIndex++) {
			// for (int timeIndex = 0; timeIndex <
			// sequences.get(stuSeqIndex).size(); timeIndex++) {
			// DataPoint dp = sequences.get(stuSeqIndex).get(timeIndex);
			// // if (opts.oneLogisticRegression) {
			// // }
			// // else {
			// for (int hiddenStateIndex = 0; hiddenStateIndex <
			// opts.nbHiddenStates; hiddenStateIndex++) {
			// double[] features = dp.getFeatures(hiddenStateIndex);
			// printArray(features, "after turning off (hidden"
			// + hiddenStateIndex + ")");
			// }
			// }
			// }
		}

		String header = opts.currentKc + " students\t";
		String firstPCorrectStr = opts.currentKc + " first PCorrect\t";
		String deltaPCorrectStr = opts.currentKc + " delta PCorrect\t";
		String lastPCorrectStr = opts.currentKc + " last PCorrect\t";
		StudentList stuListSequences = (StudentList) sequences;
		Bijection finalStudents = stuListSequences.getFinalStudents();
		Bijection oriStudents = stuListSequences.getOriStudents();

		if (opts.pCorrectOnTrainUsingGamma) {
			for (int stuSeqIndex = 0; stuSeqIndex < sequences.size(); stuSeqIndex++) {
				header += oriStudents.get(finalStudents.get(stuSeqIndex)) + "\t";
				int curSeqLength = sequences.get(stuSeqIndex).size();
				// allGamma[o][0][i]: stuSeqindex, timeIndex, hiddenState
				double firstRealHiddenState0Gamma = allGamma[stuSeqIndex][0][opts.hiddenState0];
				double firstRealHiddenState1Gamma = allGamma[stuSeqIndex][0][opts.hiddenState1];
				double lastRealHiddenState0Gamma = allGamma[stuSeqIndex][curSeqLength - 1][opts.hiddenState0];
				double lastRealHiddenState1Gamma = allGamma[stuSeqIndex][curSeqLength - 1][opts.hiddenState1];

				DataPoint firstAttDp = sequences.get(stuSeqIndex).get(0);
				DataPoint lastAttDp = sequences.get(stuSeqIndex).get(curSeqLength - 1);

				double firstRealSlip = hmm.getOpdf(opts.hiddenState1).probability(
						firstAttDp.getFeatures(opts.hiddenState1), 1 - opts.obsClass1);
				double firstRealGuess = hmm.getOpdf(opts.hiddenState0).probability(
						firstAttDp.getFeatures(opts.hiddenState0), opts.obsClass1);
				double lastRealSlip = hmm.getOpdf(opts.hiddenState1).probability(
						lastAttDp.getFeatures(opts.hiddenState1), 1 - opts.obsClass1);
				double lastRealGuess = hmm.getOpdf(opts.hiddenState0).probability(
						lastAttDp.getFeatures(opts.hiddenState0), opts.obsClass1);

				double firstAttPcorrect = firstRealHiddenState1Gamma
						* (1 - firstRealSlip) + firstRealHiddenState0Gamma * firstRealGuess;
				double lastAttPcorrect = lastRealHiddenState1Gamma * (1 - lastRealSlip)
						+ lastRealHiddenState0Gamma * lastRealGuess;
				double deltaPcorrect = lastAttPcorrect - firstAttPcorrect;
				firstPCorrectStr += firstAttPcorrect + "\t";
				deltaPCorrectStr += deltaPcorrect + "\t";
				lastPCorrectStr += lastAttPcorrect + "\t";
			}

		}
		else {
			Predict pred = new Predict(opts);

			ArrayList<ArrayList<Double>> probs = new ArrayList<ArrayList<Double>>();
			ArrayList<ArrayList<Integer>> labels = new ArrayList<ArrayList<Integer>>();
			ArrayList<ArrayList<Integer>> actualLabels = new ArrayList<ArrayList<Integer>>();
			ArrayList<Integer> studentIndexList = new ArrayList<Integer>();
			pred.doPredict(hmm, (StudentList) sequences, probs, labels, actualLabels,
					studentIndexList, opts.currentKc);
			if (probs.size() != labels.size() || labels.size() != actualLabels.size()) {
				System.out
						.println("ERROR:probs.size() != labels.size() ||labels.size() !=actualLabels.size()!");
				System.exit(1);
			}

			for (int stuSeqIndex = 0; stuSeqIndex < probs.size(); stuSeqIndex++) {
				header += oriStudents.get(finalStudents.get(stuSeqIndex)) + "\t";
				ArrayList<Double> aStudentProbs = probs.get(stuSeqIndex);
				ArrayList<Integer> aStudentLabels = labels.get(stuSeqIndex);
				ArrayList<Integer> aStudentActualLabels = actualLabels.get(stuSeqIndex);
				// int realStuIndex = studentIndexList.get(stuSeqIndex);
				if (aStudentProbs.size() != aStudentLabels.size()
						|| aStudentLabels.size() != aStudentActualLabels.size()) {
					System.out
							.println("ERROR:probs.size() != labels.size() ||labels.size() !=actualLabels.size()!");
					System.exit(1);
				}
				double firstPCorrect = aStudentProbs.get(0);
				double lastPCorrect = aStudentProbs.get(aStudentProbs.size() - 1);
				double deltaPCorrect = lastPCorrect - firstPCorrect;

				firstPCorrectStr += firstPCorrect + "\t";
				deltaPCorrectStr += deltaPCorrect + "\t";
				lastPCorrectStr += lastPCorrect + "\t";
			}
		}
		opts.deltaPCorrectWriter.write(header + "\n" + firstPCorrectStr + "\n"
				+ deltaPCorrectStr + "\n" + lastPCorrectStr + "\n");
		opts.deltaPCorrectWriter.flush();
	}

	public void writeFinalFeatureWeights(Hmm hmm,
			List<? extends List<DataPoint>> sequences) throws IOException {

		Bijection featureMapping = null;
		double[] featureWeights = null;
		if (opts.oneLogisticRegression) {
			featureMapping = hmm.getOpdf(0).featureMapping;
			featureWeights = hmm.getOpdf(0).featureWeights;
		}
		else {
			System.out
					.println("WARNING: no configuration for opts.writeFinalFeatureWeightsLog && opts.parameterizedEmit && opts.oneLogisticRegression!");
			System.exit(1);
		}
		if (featureMapping.getSize() != featureWeights.length) {
			System.out
					.println("ERROR: featureMapping.getSize() !=featureWeights.length!");
			System.exit(1);
		}
		StudentList stuListSequences = (StudentList) sequences;
		// TODO: assuming input data is ordered by student already, so that
		// stuSeqIndex could be the index in the Bijection
		Bijection finalStudents = null;
		Bijection oriStudents = null;
		double[][] studentFeatures = null;
		// TODO: consider removing this judging
		if (opts.modelName.contains("studummy"))
			finalStudents = stuListSequences.getFinalStudents();
		ArrayList<Double> realHiddenState0Features = new ArrayList<Double>();
		ArrayList<Double> realHiddenState1Features = new ArrayList<Double>();
		String header = opts.currentKc + "\t";
		String realHiddenState0Str = "";
		String realHiddenState1Str = "";
		String valueStr = "";
		String diffStr = "";
		// realHiddenState
		// TODO: consider removing this judging
		if (opts.modelName.contains("studummy")) {
			studentFeatures = new double[finalStudents.getSize()][opts.nbHiddenStates];
		}

		for (int i = 0; i < featureMapping.getSize(); i++) {
			String featureName = featureMapping.get(i);
			double featureCoefficient = featureWeights[i];
			header += featureName + "\t";
			valueStr += featureCoefficient + "\t";

			if (featureName.startsWith("*")) {
				realHiddenState0Str += featureCoefficient + "\t";
				realHiddenState1Str += featureCoefficient + "\t";
				realHiddenState0Features.add(featureCoefficient);
				realHiddenState1Features.add(featureCoefficient);
				// studentFeatures[studentIndex][0] = featureCoefficient;
				// studentFeatures[studentIndex][1] = featureCoefficient;
			}
			else {
				String studentName = "";
				Integer studentIndex = null;
				if (opts.modelName.contains("studummy")) {
					studentName = featureName.replace("features_", "");
					studentName = studentName.replace("_hidden1", "");
					if (!studentName.contains("bias") && !studentName.contains("j"))
						studentIndex = finalStudents.get(oriStudents.get(studentName) + "");
				}
				if (featureName.contains("_hidden1")) {
					if (opts.hiddenState1 == 1) {// featureCoefficient corresponds to
						// realHidden1
						realHiddenState1Str += featureCoefficient + "\t";
						realHiddenState1Features.add(featureCoefficient);
						// now only put student ability as student features
						if (studentIndex != null)
							studentFeatures[studentIndex][opts.hiddenState1] = featureCoefficient;
					}
					else {// featureCoefficient corresponds to realHidden0
						realHiddenState0Str += featureCoefficient + "\t";
						realHiddenState0Features.add(featureCoefficient);
						if (studentIndex != null)
							studentFeatures[studentIndex][opts.hiddenState0] = featureCoefficient;
					}
				}
				else {
					if (opts.hiddenState0 == 0) {// featureCoefficient corresponds to
						// realHidden0
						realHiddenState0Str += featureCoefficient + "\t";
						realHiddenState0Features.add(featureCoefficient);
						if (studentIndex != null)
							studentFeatures[studentIndex][opts.hiddenState0] = featureCoefficient;
					}
					else {// featureCoefficient corresponds to realHidden1
						realHiddenState1Str += featureCoefficient + "\t";
						realHiddenState1Features.add(featureCoefficient);
						if (studentIndex != null)
							studentFeatures[studentIndex][opts.hiddenState1] = featureCoefficient;
					}
				}
			}
		}
		if (realHiddenState0Features.size() != realHiddenState1Features.size()) {
			System.out
					.println("ERROR: realHiddenState0Features.size() != realHiddenState1Features.size()!");
			System.exit(1);
		}
		for (int i = 0; i < realHiddenState0Features.size(); i++) {
			diffStr += (realHiddenState1Features.get(i) - realHiddenState0Features
					.get(i)) + "\t";
		}
		if (opts.coefficientWeightedByGamma) {
			if (!opts.modelName.contains("studummy")) {
				System.out
						.println("ERROR: I am not going to weight the coefficient by gamma unless are using duplicated studummies");
				System.exit(1);
			}

			String firstAttStr = opts.currentKc + " firstAtt\t";
			String deltaStr = opts.currentKc + " delta\t";
			String lastAttStr = opts.currentKc + " lastAtt\t";

			for (int stuSeqIndex = 0; stuSeqIndex < sequences.size(); stuSeqIndex++) {
				int curSeqLength = sequences.get(stuSeqIndex).size();
				// allGamma[o][0][i]: stuSeqindex, timeIndex, hiddenState
				double firstRealHiddenState0Gamma = allGamma[stuSeqIndex][0][opts.hiddenState0];
				double lastRealHiddenState0Gamma = allGamma[stuSeqIndex][curSeqLength - 1][opts.hiddenState0];
				double firstRealHiddenState1Gamma = allGamma[stuSeqIndex][0][opts.hiddenState1];
				double lastRealHiddenState1Gamma = allGamma[stuSeqIndex][curSeqLength - 1][opts.hiddenState1];
				double studenFeatureRealHiddenState0 = 0.0;
				double studenFeatureRealHiddenState1 = 0.0;
				studenFeatureRealHiddenState0 = studentFeatures[stuSeqIndex][opts.hiddenState0];
				studenFeatureRealHiddenState1 = studentFeatures[stuSeqIndex][opts.hiddenState1];

				double firstAtt = studenFeatureRealHiddenState0
						* firstRealHiddenState0Gamma + studenFeatureRealHiddenState1
						* firstRealHiddenState1Gamma;
				double lastAtt = studenFeatureRealHiddenState0
						* lastRealHiddenState0Gamma + studenFeatureRealHiddenState1
						* lastRealHiddenState1Gamma;
				double delta = lastAtt - firstAtt;
				firstAttStr += firstAtt + "\t";
				deltaStr += delta + "\t";
				lastAttStr += lastAtt + "\t";
				// firstHiddenState0Gammas.add(firstHiddenState0Gamma);
				// firstHiddenState1Gammas.add(firstHiddenState1Gamma);
				// lastHiddenState0Gammas.add(lastHiddenState0Gamma);
				// lastHiddenState1Gammas.add(lastHiddenState1Gamma);
			}
			opts.finalFeatureWeightsWriter.write(header + "\n" + firstAttStr + "\n"
					+ deltaStr + "\n" + lastAttStr + "\n");
			opts.finalFeatureWeightsWriter.flush();
		}
		else {
			if (!opts.writeOnlyOriginalFinalFeatureWeights)
				opts.finalFeatureWeightsWriter.write(header + "\n" + "unknownState:\t"
						+ realHiddenState0Str + "\ndiff:\t" + diffStr + "\nknownState:\t"
						+ realHiddenState1Str + "\n");
			else
				opts.finalFeatureWeightsWriter.write(header + "\n" + opts.currentKc
						+ "\t" + valueStr + "\n");
			opts.finalFeatureWeightsWriter.flush();
		}
	}

	public void printArray(double[] array, String info) {
		System.out.println(info);
		String outStr = "";
		for (int i = 0; i < array.length; i++)
			outStr += array[i] + "\t";
		System.out.println(outStr);
	}

	// KC \t iter \t LL: \t LLVALUE \t message \t realnfo..
	public void storeAndWriteInfoPerHmmPerIter(Hmm hmm,
			List<? extends List<DataPoint>> sequences, double ll) throws IOException {
		reassignRealHiddenState(hmm, sequences);

		String basicOutStr = "KC=" + opts.currentKc + "\titer="
				+ opts.currentBaumWelchIteration + "\tLL:\t"
				+ opts.formatter.format(ll) + "\t";
		String errorMessage = "";
		String outStr = "";

		// LL errors
		double diff = previousLL - ll;
		if (previousLL != opts.INIT_LL
				&& ((diff >= opts.ACCETABLE_LL_DECREASE) || ll > 0)) {
			opts.nbLlError++;
			double valueAverage = Math.abs(previousLL + ll + opts.EPS) / 2.0;
			double diffRatio = diff / valueAverage;
			if (diffRatio > opts.maxLLDecreaseRatioValuePerIterPerHmm)
				opts.maxLLDecreaseRatioValuePerIterPerHmm = diffRatio;
			if (diff > opts.maxLLDecreaseValuePerIterPerHmm)
				opts.maxLLDecreaseValuePerIterPerHmm = diff;
			String llErrorMessage = ((diff >= opts.ACCETABLE_LL_DECREASE) ? ("LLD"
					+ opts.formatter.format(diff) + "("
					+ opts.formatter.format(diffRatio) + ")" + "," + (ll > 0 ? "LL>0,"
					: "")) : "LL>0,");
			errorMessage += llErrorMessage;
		}
		previousLL = ll;

		// cg reaches trust region boundary "error"
		if (opts.cgReachesTrustRegionBoundary) {
			errorMessage += "CGRTRB,";
		}

		// small expected count "error"
		if (opts.parameterizedEmit) {
			if (opts.writeExpectedCountLog) {
				if (opts.oneLogisticRegression) {
					for (int i = 0; i < opts.currentIterHiddenStatesExpectedCount_.length; i++) {
						double expectedCount = opts.currentIterHiddenStatesExpectedCount_[i];
						if (expectedCount < opts.EXPECTED_COUNT_SMALL_VALUE) {
							errorMessage += "EC<" + opts.EXPECTED_COUNT_SMALL_VALUE + ",";
							break;
						}
					}
				}
				else {
					boolean breakFlag = false;
					for (int hiddenState = 0; hiddenState < opts.nbHiddenStates
							&& !breakFlag; hiddenState++) {
						for (int i = 0; i < opts.currentIterHiddenStatesExpectedCount[hiddenState].length
								&& !breakFlag; i++) {
							double expectedCount = opts.currentIterHiddenStatesExpectedCount[hiddenState][i];
							if (expectedCount < opts.EXPECTED_COUNT_SMALL_VALUE) {
								errorMessage += "expectedCount<"
										+ opts.EXPECTED_COUNT_SMALL_VALUE + ",";
								breakFlag = true;
								break;
							}
						}
					}
				}
			}
		}

		// writing real info
		errorMessage += "\t";
		if (opts.verbose) {
			outStr = basicOutStr + errorMessage;
			System.out.println(outStr);
		}

		if (opts.writeInitLearnForgetProbLog || opts.writeGuessSlipProbLog
				|| opts.writeGuessSlipProbLog2)
			writeHmm(hmm, sequences, basicOutStr + errorMessage);

		if (opts.writeMainLog) {
			outStr = basicOutStr + errorMessage;
			opts.mainLogWriter.write(outStr + "\n");
			opts.mainLogWriter.flush();
		}

		if (opts.writeLlLog) {
			outStr = "";
			if (opts.oneLogisticRegression) {
				outStr += "smallLL:\t"
						+ opts.formatter.format(opts.currentIterHiddenStatesSmallLLs_[0])
						+ "\t"
						+ opts.formatter.format(opts.currentIterHiddenStatesSmallLLs_[1])
						+ "\t";
			}
			else {
				String perStateStr = "";
				double[] allHiddenStatesSmallLL = { 0.0, 0.0 };// new Double[2];
				for (int hiddenStateI = 0; hiddenStateI < opts.nbHiddenStates; hiddenStateI++) {
					double[] hiddenStateISmallLLs = opts.currentIterHiddenStatesSmallLLs[hiddenStateI];
					allHiddenStatesSmallLL[0] += hiddenStateISmallLLs[0];
					allHiddenStatesSmallLL[1] += hiddenStateISmallLLs[1];
					perStateStr += "hiddenState" + hiddenStateI + ":\t"
							+ opts.formatter.format(hiddenStateISmallLLs[0]) + "\t"
							+ opts.formatter.format(hiddenStateISmallLLs[1]) + "\t";
				}
				outStr += "smallLL:\t"
						+ opts.formatter.format(allHiddenStatesSmallLL[0]) + "\t"
						+ opts.formatter.format(allHiddenStatesSmallLL[1]) + "\t"
						+ perStateStr;
			}
			if (opts.verbose)
				System.out.print(outStr + "\n");
			outStr = basicOutStr + errorMessage + outStr;
			opts.llLogWriter.write(outStr + "\n");
			opts.llLogWriter.flush();
		}

		if (opts.writeExpectedCountLog) {
			outStr = "";
			if (opts.oneLogisticRegression) {
				outStr += "hiddenStatesExpectedCounts:\t";
				for (int i = 0; i < opts.currentIterHiddenStatesExpectedCount_.length; i++) {
					outStr += opts.formatter
							.format(opts.currentIterHiddenStatesExpectedCount_[i]) + "\t";
				}
			}
			else {
				for (int hiddenState = 0; hiddenState < opts.nbHiddenStates; hiddenState++) {
					String realHiddenState = (hiddenState == opts.hiddenState1) ? "known"
							: "unknown";
					outStr += realHiddenState + "ExpectedCounts:\t";
					for (int i = 0; i < opts.currentIterHiddenStatesExpectedCount[hiddenState].length; i++) {
						outStr += opts.formatter
								.format(opts.currentIterHiddenStatesExpectedCount[hiddenState][i])
								+ "\t";
					}
				}
			}
			if (opts.verbose)
				System.out.print(outStr + "\n");
			outStr = basicOutStr + errorMessage + outStr;
			opts.expectedCountLogWriter.write(outStr + "\n");
			opts.expectedCountLogWriter.flush();
		}

		if (opts.writeGammaLog) {
			for (int hiddenStateIndex = 0; hiddenStateIndex < opts.nbHiddenStates; hiddenStateIndex++) {
				String realHiddenState = (hiddenStateIndex == opts.hiddenState1) ? "known"
						: "unknown";
				outStr = basicOutStr + "gamma:\t" + realHiddenState + ":\t";
				for (int stuIndex = 0; stuIndex < allGamma.length; stuIndex++)
					for (int timeIndex = 0; timeIndex < allGamma[stuIndex].length; timeIndex++)
						outStr += allGamma[stuIndex][timeIndex][hiddenStateIndex] + "\t";
				opts.gammaWriter.write(outStr + "\n");
				opts.gammaWriter.flush();
			}
		}

		if (opts.writeFeatureWeightsLog) {
			outStr = "";
			if (opts.oneLogisticRegression) {
				outStr += "hiddenStatesFeatureWeights:\t"
						+ opts.currentIterfeatureWeights_ + "\t";
			}
			else {
				for (int hiddenState = 0; hiddenState < opts.nbHiddenStates; hiddenState++) {
					outStr += "hiddenState" + hiddenState + " featureWeights:\t"
							+ opts.currentIterfeatureWeights[hiddenState] + "\t";
				}
			}
			if (opts.verbose)
				System.out.print(outStr + "\n");
			outStr = basicOutStr + errorMessage + outStr;
			opts.featureWeightsLogWriter.write(outStr + "\n");
			opts.featureWeightsLogWriter.flush();
		}
	}

	protected ForwardBackwardCalculator generateForwardBackwardCalculator(
			List<DataPoint> sequence, Hmm hmm) {
		// hy:
		// System.out.println("ForwardBackwardCalculator...");
		return new ForwardBackwardCalculator(sequence, hmm,
				EnumSet.allOf(ForwardBackwardCalculator.Computation.class));
	}

	protected double[][][] estimateXi(List<DataPoint> sequence,
			ForwardBackwardCalculator fbc, Hmm hmm) {
		if (verbose)
			System.out.println("Non Scaled!");
		if (sequence.size() <= 1) {
			throw new IllegalArgumentException("Observation sequence too " + "short");
		}

		double xi[][][] = new double[sequence.size() - 1][hmm.nbStates()][hmm
				.nbStates()];
		double probability = fbc.probability();

		Iterator<DataPoint> seqIterator = sequence.iterator();
		seqIterator.next();

		for (int t = 0; t < sequence.size() - 1; t++) {
			DataPoint o = seqIterator.next();

			for (int i = 0; i < hmm.nbStates(); i++)
				for (int j = 0; j < hmm.nbStates(); j++)
					xi[t][i][j] = fbc.alphaElement(t, i) * hmm.getAij(i, j)
							* hmm.getOpdf(j).probability(o.getFeatures(j), o.getOutcome())
							* fbc.betaElement(t + 1, j) / probability;
		}

		return xi;
	}

	/**
	 * @author hy
	 * @date 11/16/13 When current sequence's length is 1, use alpha, beta, P(Dd)
	 *       instead of xi to estimate gamma
	 * @param fbc
	 * @return
	 */
	protected double[][] estimateGamma(List<DataPoint> sequence,
			ForwardBackwardCalculator fbc, Hmm hmm) {
		if (verbose)
			System.out.println("Non Scaled!");
		double[][] gamma = new double[sequence.size()][hmm.nbStates()];

		for (int t = 0; t < sequence.size(); t++)
			Arrays.fill(gamma[t], 0.);

		double probability = fbc.probability();

		for (int t = 0; t < sequence.size(); t++) {
			for (int i = 0; i < hmm.nbStates(); i++) {
				gamma[t][i] = fbc.alphaElement(t, i) * fbc.betaElement(t, i)
						/ probability;
			}
		}

		return gamma;
	}

	/*
	 * gamma[][] could be computed directly using the alpha and beta arrays, but
	 * this (slower) method is preferred because it doesn't change if the xi array
	 * has been scaled (and should be changed with the scaled alpha and beta
	 * arrays).
	 */
	protected double[][] estimateGamma(double[][][] xi,
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

	/**
	 * Returns the number of iterations performed by the {@link #learn} method.
	 * 
	 * @return The number of iterations performed.
	 */
	public int getNbIterations() {
		return nbIterations;
	}

	/**
	 * Sets the number of iterations performed by the {@link #learn} method.
	 * 
	 * @param nb
	 *          The (positive) number of iterations to perform.
	 */
	public void setNbIterations(int nb) {
		if (nb < 0)
			throw new IllegalArgumentException("Positive number expected");

		nbIterations = nb;
	}

}
