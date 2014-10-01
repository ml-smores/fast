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

package fast.hmmfeatures;

import java.io.IOException;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import fast.common.Bijection;
import fast.data.DataPoint;

/**
 * @author hy
 * @date 10/06
 * 
 *       This class implements a distribution defined by weights of features.
 *       Each object of OpdfContextAwareLogisticRegression corresponds to one
 *       hidden state
 */
public class OpdfContextAwareLogisticRegression implements
		OpdfContextAware<DataPoint> {
	private static final long serialVersionUID = 1L;

	public static final double PROBABILITY_MIN_VALUE = 1.0E-6;
	public static final double PROBABILITY_MAX_VALUE = (1.0 - PROBABILITY_MIN_VALUE);

	public Opts opts;
	// for outputing smallLL
	// private boolean verbose = true;
	// protected OpdfInteger distribution;
	// private boolean parameterizedEmit;
	// this is for the origianl emission probs.
	public double[] probabilities;

	public double[] featureWeights;
	public Bijection featureMapping;
	// for observation
	public List<Integer> classValues;
	public Bijection classMapping;

	// this is the class/label which p(class1) = 1/(1+exp(-logit))
	// ant it's posterior index, by hy's assignment, now it corresponds to
	// "correct"
	// public int obsClass1 = 1;
	// public String obsClass1Name = "correct";
	// public int hiddenState1 = 1;
	// public double bias;
	// public double guessBound;
	// public double slipBound;

	public OpdfContextAwareLogisticRegression(
			OpdfContextAwareLogisticRegression opdf) {
		this.classValues = new ArrayList<Integer>(opdf.classMapping.values());
		this.classMapping = new Bijection(opdf.classMapping);
		this.opts = opdf.opts;
		this.featureWeights = new double[opdf.featureWeights.length];
		for (int i = 0; i < featureWeights.length; i++)
			this.featureWeights[i] = opdf.featureWeights[i];
		this.featureMapping = new Bijection(opdf.featureMapping);
		this.probabilities = new double[opdf.probabilities.length];
		for (int i = 0; i < probabilities.length; i++)
			this.probabilities[i] = opdf.probabilities[i];
	}

	/**
	 * @author hy
	 * @date 10/06/13
	 * 
	 *       Builds a new probability distribution parameterized by weights over
	 *       specified features (currently for logistic regression)
	 * 
	 * @param mapping
	 *          An {@link Enum Enum} class representing the set of
	 *          values.(outcome)
	 * @param weights
	 *          Array holding one weight for each feature value (<i>i.e.</i> such
	 *          that <code>weights[i]</code> is the weight of a feature correspond
	 *          to a FeatureHMM instance's
	 *          <code>processedFeatures[j].get(i)</code>, <code>j</code> is the
	 *          index of <code>Bijection[] processedFeatures</code> corresponding
	 *          to one logistic regression).
	 * 
	 */
	public OpdfContextAwareLogisticRegression(Bijection classMapping_,
			double[] featureWeights_, Bijection featureMapping_, Opts opts_) {
		if (classMapping_ == null || featureWeights_ == null
				|| featureWeights_.length == 0 || featureMapping_ == null
				|| featureMapping_.getSize() != featureWeights_.length)
			throw new IllegalArgumentException();
		opts = opts_;
		if (!opts.parameterizedEmit) {
			System.out.println("Error: parameterizedEmit not true!");
			System.exit(1);
		}
		if (!opts.obsClass1Name.equals(classMapping_.get(opts.obsClass1))) {
			System.out.println("Error: obsClass1Name mismatch");
			System.exit(1);
		}
		classValues = new ArrayList<Integer>(classMapping_.values());
		classMapping = classMapping_;
		featureWeights = featureWeights_;
		featureMapping = featureMapping_;
		probabilities = new double[classMapping_.getSize()];

	}

	/**
	 * Builds a new probability distribution which operates on integer values.
	 * 
	 * @param mapping
	 *          An {@link Enum Enum} class representing the set of values.
	 * @param probabilities
	 *          Array holding one probability for each possible value (<i>i.e.</i>
	 *          such that <code>probabilities[i]</code> is the probability of the
	 *          observation <code>i</code>th element of <code>values</code>.
	 */
	public OpdfContextAwareLogisticRegression(Bijection mapping,
			double[] probabilities, Opts opts) {

		this.opts = opts;
		if (opts.parameterizedEmit) {
			System.out.println("Error: parameterizedEmit should be false!");
			System.exit(1);
		}
		// double bias, double guessBound, double slipBound,
		// int hiddenState1, int obsClass1, String obsClass1Name, boolean verbose) {
		classValues = new ArrayList<Integer>(mapping.values());

		if (probabilities.length == 0 || classValues.size() != probabilities.length)
			throw new IllegalArgumentException();

		this.probabilities = new double[probabilities.length];

		for (int i = 0; i < probabilities.length; i++)
			if ((this.probabilities[i] = probabilities[i]) < 0.)
				throw new IllegalArgumentException();

		// this.obsClass1 = obsClass1;
		// this.obsClass1Name = obsClass1Name;
		if (!opts.obsClass1Name.equals(mapping.get(opts.obsClass1))) {
			System.out.println("Error: obsClass1Name mismatch");
			System.exit(1);
		}
		// this.hiddenState1 = hiddenState1;

		classMapping = mapping;
		featureMapping = null;
		featureWeights = null;
		// this.bias = bias;
		// this.parameterizedEmit = false;
		// this.guessBound = guessBound;
		// this.slipBound = slipBound;
		// this.verbose = verbose;
	}

	public double probability(double[] featureValues, int classIndex) {
		double finalProb = 0.0;
		if (!opts.parameterizedEmit || opts.oneBiasFeature) {
			if (classIndex > probabilities.length - 1)
				throw new IllegalArgumentException("Wrong observation value");
			double prob = probabilities[classIndex];
			finalProb = Math.max(Math.min(prob, PROBABILITY_MAX_VALUE),
					PROBABILITY_MIN_VALUE);
			return finalProb;
		}

		if (opts.parameterizedEmit
				&& (featureValues == null && featureWeights == null)) {
			throw new IllegalArgumentException("no feature values");
		}

		if (opts.parameterizedEmit
				&& (featureValues.length != featureWeights.length)) {
			throw new IllegalArgumentException(
					"featureValues.length != featureWeights.length");
		}

		double logit = 0.0;
		for (int i = 0; i < featureWeights.length; i++) {
			// System.out.println("feature weight:" + featureWeights[i] + ",value:"
			// + featureValues[i]);
			logit += featureWeights[i] * featureValues[i];
		}
		// printArray(featureValues, "featureValue:");

		double prob = Math
				.max(Math.min(1.0 / (1.0 + Math.exp((-1.0) * logit)),
						PROBABILITY_MAX_VALUE), PROBABILITY_MIN_VALUE);

		finalProb = classIndex == opts.obsClass1 ? prob : (1.0 - prob);
		// if (opts.verbose)
		// System.out.println("Opdf prob given feature:\t" + finalProb);
		return finalProb;
	}

	@Override
	/**
	 * This is for two LR, or for HMMs
	 * @param receives gamma (it is flat meanning that all sequences are concatenated into one sequence)
	 */
	public void fit(Collection<? extends DataPoint> observations, double[] gamma,
			int hiddenStateIndex) {
		if (opts.parameterizedEmit && opts.oneLogisticRegression) {
			System.out
					.println("ERROR: here is for fitting two LR, but you configure to fit one LR!");
			throw new IllegalArgumentException();
		}
		if (observations.isEmpty() || observations.size() != gamma.length) {
			System.out.println("Error: observations.size:" + observations.size()
					+ ",gamma length:" + gamma.length);
			throw new IllegalArgumentException();
		}
		if (opts.parameterizedEmit && featureWeights == null) {
			System.out
					.println("ERROR: opts.parameterizedEmit && featureWeights==null!");
			System.exit(1);
		}
		if (!opts.parameterizedEmit
				&& featureWeights != null
				&& !opts.hmmsForcedToNonParmTrainDueToLBFGSException
						.contains(opts.currentKc)) {
			System.out
					.println("ERROR: !opts.parameterizedEmit && featureWeights != null && currents skill is not forced to use non-parametric way to train!");
			System.exit(1);
		}
		if (opts.useClassWeightToTrainParamerizedEmit
				&& opts.useInstanceWeightToTrainParamterezdEmit) {
			System.out
					.println("ERROR: opts.useClassWeightToTrainParamerizedEmit && opts.useInstanceWeightToTrainParamterezdEmit");
			System.exit(1);
		}

		if (opts.verbose) {
			System.out.println("Gamma:");
			for (int k = 0; k < gamma.length; k++)
				System.out.print("\t" + gamma[k] + "\t");
			System.out.print("\n");
			System.out.print("Gamma/sum(Gamma):\t");
			normalizedBySum(gamma);
		}

		try {
			if (!opts.parameterizedEmit) {
				fitNonParameterizedEmit(observations, gamma);
			}
			else {
				if (opts.useClassWeightToTrainParamerizedEmit) {
					fitParameterizedEmitByClassWeights(observations, gamma,
							opts.nbObsStates, hiddenStateIndex);
				}
				else {
					fitParameterizedEmitByInstanceWeights(observations, gamma,
							hiddenStateIndex);
				}
			}
		}
		catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	// This is for fitting 1LR for both hidden states, gammas is for each
	// hiddenState
	public void fit(Collection<? extends DataPoint> observations,
			double[][] gammas) {
		if (!opts.parameterizedEmit
				&& !opts.hmmsForcedToNonParmTrainDueToLBFGSException
						.contains(opts.currentKc)) {
			System.out.println("ERROR: !opts.parameterizedEmit && fitting one LR!");
			System.exit(1);
		}
		if (opts.parameterizedEmit && !opts.oneLogisticRegression) {
			System.out
					.println("ERROR: here is for fitting ONE LR, but you configure to fit TWO LR!");
			throw new IllegalArgumentException();
		}
		if (gammas.length != opts.nbHiddenStates) {
			System.out.println("Error: gammas.length!=opts.nbHiddenStates!");
			throw new IllegalArgumentException();
		}

		if (observations.isEmpty() || observations.size() != gammas[0].length) {
			System.out.println("Error: observations.size:" + observations.size()
					+ ",gamma length:" + gammas[0].length);
			throw new IllegalArgumentException();
		}

		if (opts.parameterizedEmit && featureWeights == null) {
			System.out
					.println("ERROR: opts.parameterizedEmit && featureWeights==null!");
			System.exit(1);
		}
		if (!opts.parameterizedEmit
				&& featureWeights != null
				&& !opts.hmmsForcedToNonParmTrainDueToLBFGSException
						.contains(opts.currentKc)) {
			System.out
					.println("ERROR: !opts.parameterizedEmit && featureWeights != null && currents skill is not forced to use non-parametric way to train!");
			System.exit(1);
		}
		if (opts.useClassWeightToTrainParamerizedEmit
				&& opts.useInstanceWeightToTrainParamterezdEmit) {
			System.out
					.println("ERROR: opts.useClassWeightToTrainParamerizedEmit && opts.useInstanceWeightToTrainParamterezdEmit");
			System.exit(1);
		}

		if (opts.verbose) {
			// if (opts.currentKc.equals("Objects") || opts.currentKc.equals("Arrays")
			// || opts.currentKc.equals("Classes")
			// || opts.currentKc.equals("Wrapper_Classes")) {
			for (int state = 0; state < opts.nbHiddenStates; state++) {
				System.out.println("hiddenState=" + state + " Gammas:\t");
				// System.out.println("Gammas:");
				for (int k = 0; k < gammas[state].length; k++)
					System.out.print("\t" + gammas[state][k]);
				System.out.print("\n");
				// System.out.print("Gamma/sum(Gamma):\n");
				// normalizedBySum(gammas[state]);
			}
		}

		try {
			if (opts.useClassWeightToTrainParamerizedEmit) {
				// fitParameterizedEmitByClassWeights(observations, gamma,
				// opts.nbObsStates, hiddenStateIndex);
				System.out.println("ERROR: Not availabel configuration!");
				System.exit(1);
			}
			else {
				fitParameterizedEmitByInstanceWeights(observations, gammas);
			}
		}
		catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	// TWO LR
	public void fitParameterizedEmitByClassWeights(
			Collection<? extends DataPoint> observations, double[] gamma,
			int nbObsStates, int hiddenStateIndex) throws IOException {
		double[] instanceWeights = new double[observations.size()];
		double[] classWeights = new double[nbObsStates];

		if (opts.useGammaAsInstanceWeight) {
			instanceWeights = gamma;
			if (opts.verbose)
				System.out.println("Class Weights using Instance Weights=Gamma!");
		}
		else {
			instanceWeights = normalizedBySum(gamma);
			if (opts.verbose)
				System.out
						.println("Class Weights using Instance Weights=Gamma/sum(Gamma)!");
		}
		classWeights = computeObsClassWeights(observations, instanceWeights,
				opts.nbObsStates);

		opts.currentIterHiddenStatesExpectedCount[hiddenStateIndex] = instanceWeights;
		// boolean smallExpectedCount = false;
		// for (int i = 0; i < instanceWeights.length; i++) {
		// if (instanceWeights[i] < EXPECTED_COUNT_SMALL_VALUE)
		// smallExpectedCount = true;
		// }
		// if (smallExpectedCount) {
		// String outStr = "KCName=" + opts.currentKc + "\titer="
		// + opts.currentBaumWelchIteration + "\thiddenState="
		// + hiddenStateIndex + "\tinstanceWeights:\t";
		// for (int i = 0; i < instanceWeights.length; i++) {
		// outStr += instanceWeights[i] + "\t";
		// }
		// opts.expectedCountLogWriter.write(outStr + "\n");
		// opts.expectedCountLogWriter.flush();
		// }

		// call LR
		int[] outcomes = new int[observations.size()];
		double[][] featureValues = new double[observations.size()][];
		int i = 0;
		for (DataPoint o : observations) {
			outcomes[i] = o.getOutcome();
			featureValues[i] = new double[o.getFeatures(hiddenStateIndex).length];
			featureValues[i] = o.getFeatures(hiddenStateIndex);
			i++;
		}

		opts.currentIterHiddenStatesSmallLLs[hiddenStateIndex][0] = calculateExpectedLogLikelihood(
				instanceWeights, featureValues, outcomes);

		// weights is for per instance, after training, update featureWeights;
		LogisticRegression LR = new LogisticRegression(featureValues, outcomes,
				classWeights, featureMapping, classMapping, opts, this);
		featureWeights = LR.train();
		if (opts.verbose) {
			if (featureWeights.length == 1) {
				System.out.println("Probability by LR:");
				double prob = Math.max(
						Math.min(1.0 / (1.0 + Math.exp(-1.0 * featureWeights[0])), 1.0),
						0.0);
				System.out.println("\t" + (1 - prob) + "\n\t" + prob);
			}
		}

		opts.currentIterHiddenStatesSmallLLs[hiddenStateIndex][1] = calculateExpectedLogLikelihood(
				instanceWeights, featureValues, outcomes);
	}

	// for 1LR, here expand the observations
	public void fitParameterizedEmitByInstanceWeights(
			Collection<? extends DataPoint> observations, double[][] gammas)
			throws IOException {
		// TODO: check gammas length with nbHiddenStates
		int newObsLength = opts.nbHiddenStates * observations.size();
		// int newFeatureVectorLength = -1;
		double[] finalInstanceWeights = new double[newObsLength];
		int[] finalOutcomes = new int[newObsLength];
		double[][] finalFeatureValues = new double[newObsLength][];
		// double[] currentDPFeatureValues = null;
		int nbRoundTo0 = 0;

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

			int currentOutcome = currentDP.getOutcome();
			finalOutcomes[index] = currentOutcome;
			if (opts.useGammaAsInstanceWeight) {
				double currentInstanceWeight = gammas[currentHiddenState][currentIndexInGamma];
				finalInstanceWeights[index] = currentInstanceWeight;
				if (opts.INSTANCE_WEIGHT_ROUNDING_THRESHOLD > 0) {
					// instance weight <= threshold ->0;
					// >=(1-threshold) -> 1, if threshold<0 then we don't round at all;
					// in liblinear, i already added a removeNonpositiveWeights() function
					if (currentInstanceWeight <= opts.INSTANCE_WEIGHT_ROUNDING_THRESHOLD) {
						currentInstanceWeight = 0;
						nbRoundTo0++;
						// System.out
						// .println("WARN: currentInstanceWeight <= opts.INSTANCE_WEIGHT_ROUNDING_THRESHOLD");
					}
					else if (currentInstanceWeight >= (1 - opts.INSTANCE_WEIGHT_ROUNDING_THRESHOLD)) {
						currentInstanceWeight = 1;
						// System.out
						// .println("WARN: currentInstanceWeight >= (1 - opts.INSTANCE_WEIGHT_ROUNDING_THRESHOLD)!");
					}
					finalInstanceWeights[index] = currentInstanceWeight;
				}
			}
			else {
				System.out.println("ERROR: no configuration for it yet!");
				System.exit(1);
			}

			double[] currentFeatureValues = currentDP.getFeatures(currentHiddenState);
			// double[] currentFinalFeatureValues = expandFeatureVectorForHidden1(
			// currentFeatureValues, currentHiddenState);
			finalFeatureValues[index] = currentFeatureValues;
			// // TODO: add bias
			// int noBiasNewFeatureVectorLength = currentFeatureValues.length
			// * opts.nbHiddenStates;
			// newFeatureVectorLength = noBiasNewFeatureVectorLength
			// + (opts.bias > 0 ? 1 : 0);
			//
			// double[] currentFinalFeatureValues = new
			// double[newFeatureVectorLength];
			// int featureIndex = 0;
			// if (currentHiddenState == 0) {
			// for (featureIndex = 0; featureIndex < currentFeatureValues.length;
			// featureIndex++) {
			// currentFinalFeatureValues[featureIndex] =
			// currentFeatureValues[featureIndex];
			// }
			// for (; featureIndex < noBiasNewFeatureVectorLength; featureIndex++) {
			// currentFinalFeatureValues[featureIndex] = 0;
			// }
			// }
			// else {
			// for (featureIndex = 0; featureIndex < currentFeatureValues.length;
			// featureIndex++) {
			// currentFinalFeatureValues[featureIndex] = 0.0;
			// }
			// for (; featureIndex < noBiasNewFeatureVectorLength; featureIndex++) {
			// int featureIndex_ = featureIndex - currentFeatureValues.length;
			// currentFinalFeatureValues[featureIndex] =
			// currentFeatureValues[featureIndex_];
			// }
			// }
			// if (opts.bias > 0)
			// currentFinalFeatureValues[featureIndex] = 1;
			// finalFeatureValues[index] = currentFinalFeatureValues;
			//
		}

		if (opts.INSTANCE_WEIGHT_ROUNDING_THRESHOLD > 0) {
			if (opts.verbose)
				System.out.println("nbRoundTo0=" + nbRoundTo0);
			if (nbRoundTo0 > opts.maxNbInsWeightRoundTo0PerIterPerHmm)
				opts.maxNbInsWeightRoundTo0PerIterPerHmm = nbRoundTo0;
			nbRoundTo0 = 0;
		}

		if (opts.verbose) {
			String info = "finalOutcomes:";
			// printArray(finalOutcomes, info);
			// info = "finalInstanceWeights:";
			// printArray(finalInstanceWeights, info);
			info = "finalFeatureValues:";
			printArray(finalFeatureValues, featureMapping, info);
		}

		if (opts.writeExpectedCountLog)
			opts.currentIterHiddenStatesExpectedCount_ = finalInstanceWeights;

		if (opts.writeLlLog) {
			// BEFORE
			opts.currentIterHiddenStatesSmallLLs_[0] = calculateExpectedLogLikelihood(
					finalInstanceWeights, finalFeatureValues, finalOutcomes);
		}

		// weights is for per instance, after training, update featureWeights;
		LogisticRegression LR = new LogisticRegression(finalFeatureValues,
				finalOutcomes, finalInstanceWeights, featureMapping, classMapping,
				opts, this);
		featureWeights = LR.train();
		if (opts.verbose) {
			if (featureWeights.length == 1) {
				System.out.println("Probability by LR:");
				double prob = Math.max(
						Math.min(1.0 / (1.0 + Math.exp(-1.0 * featureWeights[0])), 1.0),
						0.0);
				System.out.println("\t" + (1 - prob) + "\n\t" + prob);
			}
		}

		if (opts.writeLlLog) {
			// AFTER
			opts.currentIterHiddenStatesSmallLLs_[1] = calculateExpectedLogLikelihood(
					finalInstanceWeights, finalFeatureValues, finalOutcomes);
		}
	}

	// TWO LR
	public void fitParameterizedEmitByInstanceWeights(
			Collection<? extends DataPoint> observations, double[] gamma,
			int hiddenStateIndex) throws IOException {
		double[] instanceWeights = new double[gamma.length];
		if (opts.useGammaAsInstanceWeight) {
			instanceWeights = gamma;
			if (opts.verbose)
				System.out.println("Instance Weights using Gamma!");
		}
		else {
			instanceWeights = normalizedBySum(gamma);
			if (opts.verbose)
				System.out.println("Instance Weights using Gamma/sum(Gamma)!");
		}
		if (opts.verbose) {
			computeObsClassWeights(observations, instanceWeights, opts.nbObsStates);
		}
		opts.currentIterHiddenStatesExpectedCount[hiddenStateIndex] = instanceWeights;

		int[] outcomes = new int[observations.size()];
		double[][] featureValues = new double[observations.size()][];
		int i = 0;
		for (DataPoint o : observations) {
			outcomes[i] = o.getOutcome();
			featureValues[i] = new double[o.getFeatures(hiddenStateIndex).length];
			featureValues[i] = o.getFeatures(hiddenStateIndex);
			i++;
		}

		opts.currentIterHiddenStatesSmallLLs[hiddenStateIndex][0] = calculateExpectedLogLikelihood(
				instanceWeights, featureValues, outcomes);

		// weights is for per instance, after training, update featureWeights;
		LogisticRegression LR = new LogisticRegression(featureValues, outcomes,
				instanceWeights, featureMapping, classMapping, opts, this);
		featureWeights = LR.train();
		if (opts.verbose) {
			if (featureWeights.length == 1) {
				System.out.println("Probability by LR:");
				double prob = Math.max(
						Math.min(1.0 / (1.0 + Math.exp(-1.0 * featureWeights[0])), 1.0),
						0.0);
				System.out.println("\t" + (1 - prob) + "\n\t" + prob);
			}
		}

		opts.currentIterHiddenStatesSmallLLs[hiddenStateIndex][1] = calculateExpectedLogLikelihood(
				instanceWeights, featureValues, outcomes);

	}

	public void fitNonParameterizedEmit(
			Collection<? extends DataPoint> observations, double[] gamma) {
		// if (!opts.parameterizedEmit) {
		double[] weights = new double[gamma.length];
		Arrays.fill(probabilities, 0.);
		weights = normalizedBySum(gamma);
		if (opts.verbose)
			System.out.print("NonParamererizedEmit by Gamma/sum(Gamma):\t");
		probabilities = computeObsClassWeights(observations, weights,
				opts.nbObsStates);

		// add boundary
		// if (hiddenStateIndex == opts.hiddenState1) {// K
		// probabilities[1 - opts.obsClass1] *= opts.slipBound;// IC
		// probabilities[opts.obsClass1] = 1.0 - probabilities[1 -
		// opts.obsClass1];
		// }
		// else {// NK
		// probabilities[opts.obsClass1] *= opts.guessBound;// C
		// probabilities[1 - opts.obsClass1] = 1.0 -
		// probabilities[opts.obsClass1];
		// }
		// }
	}

	// FOR TWO LR
	// public double calculateExpectedLogLikelihood(double[] instanceWeights,
	// double[][] featureValues, int[] outcomes, int hiddenStateIndex) {
	// if (instanceWeights == null || featureWeights == null
	// || featureValues == null || outcomes == null) {
	// System.out
	// .println("ERROR: instanceWeights==null || featureWeights==null || featureValues==null||outcomes==null!");
	// System.exit(1);
	// }
	// if (instanceWeights.length != featureValues.length
	// || featureValues.length != outcomes.length) {
	// System.out
	// .println("ERROR: instanceWeights.length!=featureWeights.length || featureWeights.length != featureValues.length || featureValues.length != outcomes.length!");
	// System.exit(1);
	// }
	// double smallLL = 0.0;
	// int nbDatapoints = instanceWeights.length;
	// for (int i = 0; i < nbDatapoints; i++) {
	// double expectedCount = instanceWeights[i];
	// double[] features = featureValues[i];
	// if (features.length != featureWeights.length) {
	// System.out.println("ERROR: features.length != featureWeights.length!");
	// System.exit(1);
	// }
	// int outcome = outcomes[i];
	// double thita = probability(features, outcome);
	// smallLL += expectedCount * Math.log(thita);
	// }
	// if (opts.verbose) {
	// if (opts.oneLogisticRegression)
	// System.out.println("hiddenStates:" + "\tsmallLL: " + smallLL);
	// else
	// System.out.println("hiddenState=" + hiddenStateIndex + "\tsmallLL: "
	// + smallLL);
	// }
	// return smallLL;
	// }

	// for one LR
	public double calculateExpectedLogLikelihood(double[] instanceWeights,
			double[][] featureValues, int[] outcomes) {
		if (opts.generateLRInputs)
			return 0.0;
		if (!opts.oneLogisticRegression) {
			System.out
					.println("WARNING: calculateExpectedLogLikelihood but !opts.oneLogisticRegression!");
			// System.exit(1);
		}
		if (instanceWeights == null || featureWeights == null
				|| featureValues == null || outcomes == null) {
			System.out
					.println("ERROR: instanceWeights==null || featureWeights==null || featureValues==null||outcomes==null!");
			System.exit(1);
		}
		if (instanceWeights.length != featureValues.length
				|| featureValues.length != outcomes.length) {
			System.out
					.println("ERROR: instanceWeights.length!=featureWeights.length || featureWeights.length != featureValues.length || featureValues.length != outcomes.length!");
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
			int outcome = outcomes[i];
			// TODO: no bias yet
			// int hiddenStateIndex = (i >= nbDatapoints / 2) ? 1 : 0;
			double thita = probability(features, outcome);
			smallLL += expectedCount * Math.log(thita);
		}
		if (opts.verbose) {
			System.out.println("hiddenStates:" + "\tsmallLL: " + smallLL);
		}
		return smallLL;
	}

	public double[] normalizedBySum(double[] values) {
		double[] normedValues = new double[values.length];
		double sum = 0.0;
		for (int k = 0; k < values.length; k++) {
			sum += values[k];
		}
		for (int k = 0; k < values.length; k++) {
			normedValues[k] = values[k] / sum;
		}
		if (opts.verbose) {
			System.out.println("normalized by sum:");
			for (int k = 0; k < normedValues.length; k++)
				System.out.print("\t" + normedValues[k] + "\t");
			System.out.print("\n");
		}
		return normedValues;
	}

	public double[] computeObsClassWeights(
			Collection<? extends DataPoint> observations, double[] weights,
			int nbObsStates) {
		double[] classWeights = new double[nbObsStates];
		int i = 0;
		for (DataPoint o : observations)
			classWeights[o.getOutcome()] += weights[i++];
		// for (int p = 0; p < probs.length; p++) {
		// probs[p] = Math.max(Math.min(probs[p], 1.0), 0.0);
		// }
		if (opts.verbose) {
			System.out.println("Class Weights:");
			for (int j = 0; j < nbObsStates; j++) {
				System.out.print("\t" + classWeights[j] + "\t");
			}
			System.out.print("\n");
			System.out.println("ratio:" + classWeights[0] / classWeights[1]);
		}
		return classWeights;
	}

	public void printArray(double[] array, String info) {
		System.out.println(info);
		String outStr = "";
		for (int i = 0; i < array.length; i++)
			outStr += array[i] + "\t";
		System.out.println(outStr);
	}

	public void printArray(int[] array, String info) {
		System.out.println(info);
		String outStr = "";
		for (int i = 0; i < array.length; i++)
			outStr += array[i] + "\t";
		System.out.println(outStr);
	}

	public void printArray(double[][] array, String info) {
		System.out.println(info);
		String outStr = "";
		System.out.println("1st dim length=" + array.length + ", 2nd dim length="
				+ array[0].length);
		for (int i = 0; i < array.length; i++) {
			for (int j = 0; j < array[i].length; j++) {
				outStr += "\t" + array[i][j];
			}
			System.out.println(outStr);
			outStr = "";
		}
	}

	public void printArray(double[][] array, Bijection featureMapping, String info) {
		System.out.println(info);
		String outStr = "";
		String header = "";
		System.out.println("1st dim length=" + array.length + ", 2nd dim length="
				+ array[0].length);
		for (int j = 0; j < featureMapping.getSize(); j++) {
			header += "\t" + featureMapping.get(j);
		}
		System.out.println(header);
		for (int i = 0; i < array.length; i++) {
			for (int j = 0; j < array[i].length; j++) {
				outStr += "\t" + array[i][j];
			}
			System.out.println(outStr);
			outStr = "";
		}
	}

	@Override
	public OpdfContextAwareLogisticRegression clone() {
		try {
			OpdfContextAwareLogisticRegression opdfDiscrete = (OpdfContextAwareLogisticRegression) super
					.clone();
			// opdfDiscrete.distribution = distribution.clone();
			return opdfDiscrete;
		}
		catch (CloneNotSupportedException e) {
			throw new InternalError();
		}
	}

	@Override
	public String toString() {
		return toString(NumberFormat.getInstance());
	}

	@Override
	public String toString(NumberFormat numberFormat) {
		String s = "";// "<";

		if (featureWeights == null) {
			for (int i = 0; i < classMapping.keys().size(); i++) {

				String k = classMapping.get(i);
				s += k + " " + opts.formatter.format(probabilities[i])// numberFormat.format(probabilities[i])
						+ ((i != classValues.size() - 1) ? ", " : "");
			}
			// s += ">";
		}
		else {
			for (int i = 0; i < featureWeights.length; i++) {
				// s += numberFormat.format(featureWeights[i])
				// + ((i != values.size() - 1) ? ", " : "");
				s += opts.formatter.format(featureWeights[i])
						+ ((i != featureWeights.length - 1) ? "\t" : "");
			}
			// s += ">";
		}

		return s;
	}

	// public double[] expandFeatureVectorForHidden1(double[]
	// originalFeatureVector,
	// int hiddenStateIndex) {
	//
	// if (!opts.oneLogisticRegression || opts.oneBiasFeature) {
	// System.out
	// .println("ERROR: !opts.oneLogisticRegression || opts.oneBiasFeature");
	// System.exit(1);
	// }
	//
	// if (opts.bias < 0
	// && featureWeights.length != originalFeatureVector.length * 2) {
	// System.out
	// .println("ERROR: opts.bias < 0 && featureWeights.length != originalFeatureVector.length * 2)");
	// System.exit(1);
	// }
	// if (opts.bias > 0
	// && featureWeights.length != originalFeatureVector.length * 2 + 1) {
	// System.out
	// .println("ERROR: opts.bias > 0 && featureWeights.length != originalFeatureVector.length * 2 + 1");
	// throw new IllegalArgumentException();
	// }
	// double[] newFeatureVector = null;
	// int noBiasFeatureVectorLength = originalFeatureVector.length * 2;
	// int newFeatureVectorLength = noBiasFeatureVectorLength
	// + (opts.bias > 0 ? 1 : 0);
	// newFeatureVector = new double[newFeatureVectorLength];
	// for (int i = 0; i < noBiasFeatureVectorLength; i++) {
	// if (hiddenStateIndex == 0) {
	// if (i < originalFeatureVector.length)
	// newFeatureVector[i] = originalFeatureVector[i];
	// else
	// newFeatureVector[i] = 0;
	// }
	// else {
	// if (i >= originalFeatureVector.length)
	// newFeatureVector[i] = originalFeatureVector[i
	// - originalFeatureVector.length];
	// else
	// newFeatureVector[i] = 0;
	// }
	// }
	// if (opts.bias > 0)
	// newFeatureVector[newFeatureVectorLength - 1] = 1;
	//
	// return newFeatureVector;
	// }
}
