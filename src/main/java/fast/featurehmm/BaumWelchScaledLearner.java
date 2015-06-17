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

//import hmm.ForwardBackwardScaledCalculator;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.List;
import fast.data.DataPoint;
//import fast.experiment.Opts;

//import be.ac.ulg.montefiore.run.jahmm.ForwardBackwardCalculator;

/**
 * An implementation of the Baum-Welch learning algorithm. It uses a scaling
 * mechanism so as to avoid underflows.
 * <p>
 * For more information on the scaling procedure, read <i>Rabiner</i> and
 * <i>Juang</i>'s <i>Fundamentals of speech recognition</i> (Prentice Hall,
 * 1993).
 */
public class BaumWelchScaledLearner extends BaumWelchLearner {
	/**
	 * Initializes a Baum-Welch algorithm implementation.
	 */
	public BaumWelchScaledLearner(boolean parameterizing, boolean parameterizedInit, boolean parameterizedTran, boolean parameterizedEmit,
																int nbHiddenStates,
																double EM_TOLERANCE, double EPS, double ACCETABLE_LL_DECREASE, int EM_MAX_ITERS) {
		super(parameterizing, parameterizedInit, parameterizedTran, parameterizedEmit, nbHiddenStates, EM_TOLERANCE, EPS, ACCETABLE_LL_DECREASE, EM_MAX_ITERS);
	}

	protected ForwardBackwardCalculator generateForwardBackwardCalculator(
			List<DataPoint> sequence, FeatureHMM hmm) {
		return new ForwardBackwardScaledCalculator(sequence, hmm,
				EnumSet.allOf(ForwardBackwardCalculator.Computation.class));
	}

	/**
	 * Here, the xi (and, thus, gamma) values are not divided by the probability
	 * of the sequence because this probability might be too small and induce an
	 * underflow. xi[t][i][j] still can be interpreted as P[q_t = i and q_(t+1) =
	 * j | obsSeq, hmm] because we assume that the scaling factors are such that
	 * their product is equal to the inverse of the probability of the sequence.
	 * 
	 * hy: the scaling factors are ctFactors, of which each
	 * ctFactors[t]=P(O1...Ot)
	 */
	protected double[][][] estimateXi(List<DataPoint> sequence,
			ForwardBackwardCalculator fbc, FeatureHMM hmm) {
		// hy*
		if (sequence.size() <= 1)
			throw new IllegalArgumentException("Observation sequence too " + "short");
		// *hy

		double xi[][][] = new double[sequence.size() - 1][hmm.getNbHiddenStates()][hmm.getNbHiddenStates()];

		Iterator<DataPoint> seqIterator = sequence.iterator();
		seqIterator.next();
		// DataPoint currentO = seqIterator.next();

		for (int t = 0; t < sequence.size() - 1; t++) {
			DataPoint nextO = seqIterator.next();
			for (int i = 0; i < hmm.getNbHiddenStates(); i++) {
				// double[] currentOFeatureValues = currentO.getFeatures(i);
				for (int j = 0; j < hmm.getNbHiddenStates(); j++) {
					// double[] nextOFeatureValues = nextO.getFeatures(j);
					// TODO: hy changed nextO.getFeatures(j) to nextO.getFeatures(i)
					xi[t][i][j] = fbc.alphaElement(t, i)
							* hmm.getTransitionij(i, j, nextO.getFeatures(i, 1))
							* hmm.getEmissionjk(j, nextO.getOutcome(),
									nextO.getFeatures(j, 2)) * fbc.betaElement(t + 1, j);
				}
			}
			// currentO = nextO;
		}

		return xi;
	}

	// protected double[][][] estimateXi(List<DataPoint> sequence,
	// ForwardBackwardCalculator fbc, FeatureHMM hmm) {
	// // hy*
	// if (sequence.size() <= 1)
	// throw new IllegalArgumentException("Observation sequence too " + "short");
	// // *hy
	//
	// double xi[][][] = new double[sequence.size() - 1][hmm.nbStates()][hmm
	// .nbStates()];
	//
	// Iterator<DataPoint> seqIterator = sequence.iterator();
	// seqIterator.next();
	//
	// for (int t = 0; t < sequence.size() - 1; t++) {
	// DataPoint observation = seqIterator.next();
	//
	// for (int i = 0; i < hmm.nbStates(); i++)
	// for (int j = 0; j < hmm.nbStates(); j++)
	// xi[t][i][j] = fbc.alphaElement(t, i)
	// * hmm.getAij(i, j)
	// * hmm.getOpdf(j).probability(observation.getFeatures(j),
	// observation.getOutcome()) * fbc.betaElement(t + 1, j);
	// }
	//
	// return xi;
	// }

	/**
	 * @author hy
	 * @date 11/16/13 When current sequence's length is 1, use alpha, beta, P(Dd)
	 *       instead of xi to estimate gamma, to be consistent with the estimateXi
	 *       in BaumWelchScaledLearner, not sure whether divided by p(Dd) or
	 *       not...
	 * @param fbc
	 * @return
	 */
	protected double[][] estimateGamma(List<DataPoint> sequence,
			ForwardBackwardCalculator fbc, FeatureHMM hmm) {
		double[][] gamma = new double[sequence.size()][hmm.getNbHiddenStates()];

		for (int t = 0; t < sequence.size(); t++)
			Arrays.fill(gamma[t], 0.);

		// double probability = fbc.probability();

		for (int t = 0; t < sequence.size(); t++) {
			for (int i = 0; i < hmm.getNbHiddenStates(); i++) {
				gamma[t][i] = fbc.alphaElement(t, i) * fbc.betaElement(t, i)
						* fbc.probability();
			}
		}

		return gamma;
	}
}
