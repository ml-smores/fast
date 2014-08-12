package hmmfeatures;

import util.LBFGSMinimizer;
import edu.berkeley.nlp.math.CachingDifferentiableFunction;
import fig.basic.Pair;

public class LBFGS {

	public Opts opts;
	private double[][] featureValues;
	public double[] featureWeights;
	private double[] expectedCounts;
	private int[] outcomes;
	private int numFeatures;
	private OpdfContextAwareLogisticRegression opdf;

	public LBFGS(double[][] featureValues_, double[] initialFeatureWeights_,
			double[] expectedCounts_, int[] outcomes_,
			OpdfContextAwareLogisticRegression opdf_, Opts opts_) {
		this.featureValues = featureValues_;
		this.featureWeights = initialFeatureWeights_;
		this.expectedCounts = expectedCounts_;
		this.outcomes = outcomes_;
		this.opdf = new OpdfContextAwareLogisticRegression(opdf_);
		this.opts = opts_;
		this.numFeatures = initialFeatureWeights_.length;
	}

	public double[] run() {
		NegativeRegularizedExpectedLogLikelihood negativeLikelihood = new NegativeRegularizedExpectedLogLikelihood();
		LBFGSMinimizer minimizer = new LBFGSMinimizer(opts);
		minimizer.setMaxIterations(opts.LBFGS_MAX_ITERS);
		// featureWeights is deep copied into guess -> valueAt, deravativeAt ->
		// calculate -> setWeights set featuWeights as the new one.
		// here should start from an inital weights (set before when initializing
		// feaatureHMM)
		minimizer.minimize(negativeLikelihood, featureWeights,
				opts.LBFGS_TOLERANCE, opts.LBFGS_PRINT_MINIMIZER);
		return featureWeights;
	}

	public void setWeights(double[] featureWeights_) {
		this.featureWeights = featureWeights_;
	}

	// for getting new thita (emitProb and transProb) by new weights and origainl
	// featureValues
	public void computePotentials(double[] featureWeights_) {
		opdf.featureWeights = featureWeights_;
	}

	// hy:computes one LL consisting of transition and emission
	private class NegativeRegularizedExpectedLogLikelihood extends
			CachingDifferentiableFunction {

		// Pair.makePair(negativeRegularizedExpectedLogLikelihood, gradient), both
		// of them are updated in every iteration
		protected Pair<Double, double[]> calculate(double[] x) {
			// print(x, "featureWeights");
			setWeights(x);
			computePotentials(x);

			// hy: just get the small ll as the paper shows
			double negativeRegularizedExpectedLogLikelihood = 0.0;
			if (opts.oneLogisticRegression)
				negativeRegularizedExpectedLogLikelihood = -(opdf
						.calculateExpectedLogLikelihood(expectedCounts, featureValues,
								outcomes) - calculateRegularizer());
			else {
				System.out.println("ERROR: current configuration not defined!");
				System.exit(1);
			}

			/**
			 * double smallLL = 0.0; int nbDatapoints = instanceWeights.length; for
			 * (int i = 0; i < nbDatapoints; i++) { double expectedCount =
			 * instanceWeights[i]; double[] features = featureValues[i]; if
			 * (features.length != featureWeights.length) {
			 * System.out.println("ERROR: features.length != featureWeights.length!");
			 * System.exit(1); } int outcome = outcomes[i]; // TODO: no bias yet int
			 * hiddenStateIndex = (i >= nbDatapoints / 2) ? 1 : 0; double thita =
			 * probability(features, outcome, hiddenStateIndex); smallLL +=
			 * expectedCount * Math.log(thita); }
			 **/

			// Calculate gradient
			double[] gradient = new double[featureWeights.length];
			// Gradient of emit weights (hy: doesn't have transition part ;-)
			int nbDatapoints = expectedCounts.length;
			if (opts.forceSetInstanceWeightForLBFGS > 0) {
				for (int e = 0; e < expectedCounts.length; e++) {
					expectedCounts[e] = opts.forceSetInstanceWeightForLBFGS;
				}
			}
			// print(expectedCounts, "expected counts:");
			for (int i = 0; i < nbDatapoints; i++) {
				// System.out.println("dp id=" + i);
				double expectedCount = expectedCounts[i];
				double[] features = featureValues[i];
				if (features.length != featureWeights.length) {
					System.out.println("ERROR: features.length !=featureWeights.length");
					System.exit(1);
				}
				int outcome = outcomes[i];
				// int hiddenStateIndex = (i >= nbDatapoints / 2) ? 1 : 0;

				// TODO: no bias yet
				for (int featureIndex = 0; featureIndex < features.length; featureIndex++) {
					if (outcome == 1) {
						gradient[featureIndex] -= expectedCount * features[featureIndex]
								* (1 - opdf.probability(features, outcome));
					}
					else {
						gradient[featureIndex] -= expectedCount * features[featureIndex]
								* (-1.0) * opdf.probability(features, 1);
					}

				}
			}

			/**
			 * for (int s = 0; s < opts.nbHiddenStates; ++s) { for (int i = 0; i <
			 * numObservations; ++i) { for (int f = 0; f <
			 * activeEmitFeatures[s][i].size(); ++f) { Pair<Integer, Double> feat =
			 * activeEmitFeatures[s][i].get(f); // sum_dct(e_dct) * f(dct)
			 * gradient[feat.getFirst()] -= expectedEmitCounts[s][i] feat.getSecond();
			 * // sum_dct(e_dct)*sum_d'(thita_d'ct*f(d'ct)) // guess:
			 * expectedLabelCounts[s] = sum_i(expectedEmitCounts[s][i])
			 * gradient[feat.getFirst()] -= -expectedLabelCounts[s] * emitProbs[s][i]
			 * * feat.getSecond(); }
			 * 
			 * } }
			 **/

			// print(gradient, "Gradient");

			// Add gradient of regularizer
			for (int f = 0; f < numFeatures; ++f) {
				gradient[f] += 2.0 * opts.regularizationWeightsForLBFGS[f]
						* (featureWeights[f] - opts.regularizationBiasesForLBFGS[f]);
			}
			// print(gradient, "RegGradient");
			// print(x, "featureWeights");
			// System.out.println("negativeRegularizedExpectedLogLikelihood:\t"
			// + negativeRegularizedExpectedLogLikelihood);

			return Pair.makePair(negativeRegularizedExpectedLogLikelihood, gradient);
		}

		public int dimension() {
			return numFeatures;
		}

	}

	public void print(double[] temp, String info) {
		System.out.print(info + ":\t");
		for (int i = 0; i < temp.length; i++)
			System.out.print(temp[i] + "\t");
		System.out.println();

	}

	public double calculateRegularizer() {
		double result = 0.0;
		for (int f = 0; f < numFeatures; ++f) {
			result += opts.regularizationWeightsForLBFGS[f]
					* (featureWeights[f] - opts.regularizationBiasesForLBFGS[f])
					* (featureWeights[f] - opts.regularizationBiasesForLBFGS[f]);
		}
		return result;
	}

}
