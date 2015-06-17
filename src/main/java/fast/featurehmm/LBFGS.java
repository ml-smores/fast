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

package fast.featurehmm;

import edu.berkeley.nlp.math.CachingDifferentiableFunction;
import edu.berkeley.nlp.math.LBFGSMinimizer;
import edu.berkeley.nlp.util.Logger;
import edu.berkeley.nlp.util.Pair;

public class LBFGS {

	private final double tol;
	private final int max_iters;
	private final double[] regularizationWeights;
	private final double[] regularizationBiases;
	private final double[][] featureValues;
	private final double[] expectedCounts;
	private final int[] classes;
	private final int nbFeatures;
	private final String type;

	private double[] featureWeights;
	private PdfFeatureAwareLogisticRegression pdf;
	private int nbParameterizingFailed = 0;//must set 0
	private boolean verbose;

	//in Logistic regression, it already creates new memory space
	public LBFGS(double[] initialFeatureWeights, PdfFeatureAwareLogisticRegression pdf, 
								double[] expectedCounts, 
								double[][] featureValues, int[] classes,
								String type,
								double[] regularizationWeights, double[] regularizationBiases,
								int max_iters, double tolerance) {
		this.featureWeights = initialFeatureWeights;
		this.pdf = pdf;

		this.expectedCounts = expectedCounts;
		this.featureValues = featureValues;
		this.classes = classes;
		this.nbFeatures = initialFeatureWeights.length;
		this.type = type; // trans or emit
		this.regularizationWeights = regularizationWeights;
		this.regularizationBiases = regularizationBiases;
		this.max_iters = max_iters;
		this.tol = tolerance;
	}

	
	public double[] run() {
		NegativeRegularizedExpectedLogLikelihood negativeLikelihood = new NegativeRegularizedExpectedLogLikelihood();

		LBFGSMinimizer minimizer = new LBFGSMinimizer();
		minimizer.setMaxIterations(max_iters);
		minimizer.setVerbose(verbose);

		try {
			minimizer.minimize(negativeLikelihood, featureWeights, tol);
		}
		catch (RuntimeException ex) {
			nbParameterizingFailed = 1;
			Logger.err("RuntimeException probably caused by [LBFGSMinimizer.implicitMultiply]: Curvature problem. parameterizingSucceeded=false.");
		}

		return featureWeights;
	}

	public void setFeatureWeights(double[] featureWeights) {
		this.featureWeights = featureWeights;
	}
	
	public double[] getFeatureWeights(){
		return featureWeights;
	}

	// for getting new theta (emitProb and transProb) by new weights and original
	// featureValues
	private void computePotentials(double[] featureWeights) {
		pdf.setFeatureWeights(featureWeights);
	}

	// hy:computes one LL consisting of transition and emission
	private class NegativeRegularizedExpectedLogLikelihood extends
			CachingDifferentiableFunction {

		// Pair.makePair(negativeRegularizedExpectedLogLikelihood, gradient), both
		// of them are updated in every iteration
		protected Pair<Double, double[]> calculate(double[] x) {
			// print(x, "featureWeights");
			setFeatureWeights(x);
			computePotentials(x);

			// hy: just get the small ll as the paper shows
			double negativeRegularizedExpectedLogLikelihood = 0.0;

			// JPG removed this:
			// if (opts.oneLogisticRegression)
			// negativeRegularizedExpectedLogLikelihood = -(pdf
			// .calculateExpectedLogLikelihood(expectedCounts, featureValues,
			// outcomes) - calculateRegularizer());
			negativeRegularizedExpectedLogLikelihood = -(pdf.calculateExpectedLogLikelihood(expectedCounts, featureValues,
							classes, type) - calculateRegularizer());

			// Calculate gradient
			double[] gradient = new double[featureWeights.length];
			// Gradient of emit weights (hy: doesn't have transition part ;-)
			int nbDatapoints = expectedCounts.length;

			/*
			 * JPG COMMENTED THIS: if (opts.forceSetInstanceWeightForLBFGS > 0) { for
			 * (int e = 0; e < expectedCounts.length; e++) { expectedCounts[e] =
			 * opts.forceSetInstanceWeightForLBFGS; } }
			 */

			// print(expectedCounts, "expected counts:");
			for (int i = 0; i < nbDatapoints; i++) {
				// System.out.println("dp id=" + i);
				double expectedCount = expectedCounts[i];
				double[] features = featureValues[i];
				if (features.length != featureWeights.length) {
					System.out.println("ERROR: features.length !=featureWeights.length");
					System.exit(1);
				}
				int curClass = classes[i];
				// int hiddenStateIndex = (i >= nbDatapoints / 2) ? 1 : 0;

				// TODO: no bias yet
				for (int featureIndex = 0; featureIndex < features.length; featureIndex++) {
					if (curClass == 1) {
						gradient[featureIndex] -= expectedCount * features[featureIndex]
								* (1 - pdf.probability(features, curClass, type));
					}
					else {
						gradient[featureIndex] -= expectedCount * features[featureIndex]
								* (-1.0) * pdf.probability(features, 1, type);
					}

					// for (int featureIndex = 0; featureIndex < features.length;
					// featureIndex++) {
					// if (outcome == 1) {
					// gradient[featureIndex] -= expectedCount * features[featureIndex]
					// * (1 - pdf.probability(features, outcome));
					// }
					// else {
					// gradient[featureIndex] -= expectedCount * features[featureIndex]
					// * (-1.0) * pdf.probability(features, 1);
					// }

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
			for (int f = 0; f < nbFeatures; ++f) {
				gradient[f] += 2.0 * regularizationWeights[f]
						* (featureWeights[f] - regularizationBiases[f]);
			}
			// print(gradient, "RegGradient");
			// print(x, "featureWeights");
			// System.out.println("negativeRegularizedExpectedLogLikelihood:\t"
			// + negativeRegularizedExpectedLogLikelihood);

			return Pair.makePair(negativeRegularizedExpectedLogLikelihood, gradient);
		}

		public int dimension() {
			return nbFeatures;
		}

	}

//	public void print(double[] temp, String info) {
//		System.out.print(info + ":\t");
//		for (int i = 0; i < temp.length; i++)
//			System.out.print(temp[i] + "\t");
//		System.out.println();
//
//	}
	
	public int getParameterizingResult(){
		return nbParameterizingFailed;
	}

	//TODO: for those features that never get activated, shouldn't be included in regularization
	private double calculateRegularizer() {
		double result = 0.0;
		for (int f = 0; f < nbFeatures; ++f) {
			result += regularizationWeights[f]
					* (featureWeights[f] - regularizationBiases[f])
					* (featureWeights[f] - regularizationBiases[f]);
		}
		return result;
	}

}
