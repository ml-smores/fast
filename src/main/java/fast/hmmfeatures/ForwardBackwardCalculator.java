/* 
 * Use Hmm in hmmfeatures instead of from jahmm
 * 
 * jahmm package - v0.6.1 */

/*
 *  Copyright (c) 2004-2006, Jean-Marc Francois.
 *
 *  This file is part of Jahmm.
 *  Jahmm is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation; either version 2 of the License, or
 *  (at your option) any later version.
 *
 *  Jahmm is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with Jahmm; if not, write to the Free Software
 *  Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA

 */

package fast.hmmfeatures;

import java.util.EnumSet;
import java.util.Iterator;
import java.util.List;

import fast.data.DataPoint;

/**
 * This class can be used to compute the probability of a given observations
 * sequence for a given HMM. Once the probability has been computed, the object
 * holds various information such as the <i>alpha</i> (and possibly <i>beta</i>)
 * array, as described in <i>Rabiner</i> and <i>Juang</i>.
 * <p>
 * Computing the <i>beta</i> array requires a O(1) access time to the
 * observation sequence to get a theoretically optimal performance.
 */
public class ForwardBackwardCalculator {
	/**
	 * Flags used to explain how the observation sequence probability should be
	 * computed (either forward, using the alpha array, or backward, using the
	 * beta array).
	 */
	public static enum Computation {
		ALPHA, BETA
	};

	/*
	 * alpha[t][i] = P(O(1), O(2),..., O(t+1), i(t+1) = i+1 | hmm), that is the
	 * probability of the beginning of the state sequence (up to time t+1) with
	 * the (t+1)th state being i+1.
	 */
	protected double[][] alpha = null;
	protected double[][] beta = null;
	protected double probability;
	private boolean verbose = false;

	protected ForwardBackwardCalculator() {
	};

	/**
	 * Computes the probability of occurrence of an observation sequence given a
	 * Hidden Markov Model.
	 * 
	 * @param hmm
	 *          A Hidden Markov Model;
	 * @param oseq
	 *          An observation sequence.
	 * @param flags
	 *          How the computation should be done. See the {@link Computation
	 *          Computation} enum.
	 */
	public ForwardBackwardCalculator(List<DataPoint> oseq, Hmm hmm,
			EnumSet<Computation> flags) {
		if (verbose)
			System.out.println("Non Scaled!");
		// System.out.println("ForwardBackwardCalculator...");
		if (oseq.isEmpty())
			throw new IllegalArgumentException("Invalid empty sequence");

		if (flags.contains(Computation.ALPHA))
			computeAlpha(hmm, oseq);// hy
		if (verbose) {
			System.out.println("alpha:");
			for (int t = 0; t < alpha.length; t++) {
				System.out.print("\ttime=" + t);
				for (int state = 0; state < alpha[t].length; state++) {
					System.out.print("\tstate" + state + "=\t" + alpha[t][state]);
				}
				System.out.print("\n");
			}
			System.out.print("\n");
		}

		if (flags.contains(Computation.BETA))
			computeBeta(hmm, oseq);
		if (verbose) {
			System.out.println("beta:");
			for (int t = 0; t < beta.length; t++) {
				System.out.print("\ttime=" + t);
				for (int state = 0; state < beta[t].length; state++) {
					System.out.print("\tstate" + state + "=\t" + beta[t][state]);
				}
				System.out.print("\n");
			}
			System.out.print("\n");
		}
		computeProbability(oseq, hmm, flags);
	}

	/**
	 * Computes the probability of occurence of an observation sequence given a
	 * Hidden Markov Model. This computation computes the <code>alpha</code> array
	 * as a side effect.
	 * 
	 * @see #ForwardBackwardCalculator(List, Hmm, EnumSet)
	 */
	public ForwardBackwardCalculator(List<DataPoint> oseq, Hmm hmm) {
		this(oseq, hmm, EnumSet.of(Computation.ALPHA));
	}

	/* Computes the content of the alpha array */
	protected void computeAlpha(Hmm hmm, List<DataPoint> oseq) {
		alpha = new double[oseq.size()][hmm.nbStates()];

		for (int i = 0; i < hmm.nbStates(); i++)
			computeAlphaInit(hmm, oseq.get(0), i);

		Iterator<DataPoint> seqIterator = oseq.iterator();
		if (seqIterator.hasNext())
			seqIterator.next();

		for (int t = 1; t < oseq.size(); t++) {
			DataPoint observation = seqIterator.next();
			for (int i = 0; i < hmm.nbStates(); i++)
				computeAlphaStep(hmm, observation, t, i);// hy: pass observation's
			// features
		}
	}

	/* Computes alpha[0][i] */
	protected void computeAlphaInit(Hmm hmm, DataPoint o, int i) {
		// hy: i is the hidden state
		alpha[0][i] = hmm.getPi(i)
				* hmm.getOpdf(i).probability(o.getFeatures(i), o.getOutcome());
	}

	/* Computes alpha[t][j] (t > 0) */
	protected void computeAlphaStep(Hmm hmm, DataPoint o, int t, int j) {
		double sum = 0.;

		for (int i = 0; i < hmm.nbStates(); i++)
			sum += alpha[t - 1][i] * hmm.getAij(i, j);

		if (verbose)
			System.out.println("computeAlphaStep:\n" + "\tt=" + t + "\tj=" + j
					+ "\n\talpha[" + (t - 1) + "][0]=" + alpha[t - 1][0] + "\talpha["
					+ (t - 1) + "][1]=" + alpha[t - 1][1] + "\tA0" + j + "="
					+ hmm.getAij(0, j) + "\tA1" + j + "=" + hmm.getAij(1, j) + "\n\tsum="
					+ sum);
		alpha[t][j] = sum
				* hmm.getOpdf(j).probability(o.getFeatures(j), o.getOutcome());
		if (verbose)
			System.out.println("\tbjk="
					+ hmm.getOpdf(j).probability(o.getFeatures(j), o.getOutcome())
					+ "\talpha[" + t + "][" + j + "]=" + alpha[t][j]);

		// hy:pass j(hidden state),o(observation),t(time) or featureValues
	}

	/*
	 * Computes the content of the beta array. Needs a O(1) access time to the
	 * elements of oseq to get a theoretically optimal algorithm.
	 */
	protected void computeBeta(Hmm hmm, List<DataPoint> oseq) {
		beta = new double[oseq.size()][hmm.nbStates()];

		for (int i = 0; i < hmm.nbStates(); i++)
			beta[oseq.size() - 1][i] = 1.;

		for (int t = oseq.size() - 2; t >= 0; t--)
			for (int i = 0; i < hmm.nbStates(); i++)
				computeBetaStep(hmm, oseq.get(t + 1), t, i);
	}

	/* Computes beta[t][i] (t < obs. seq.le length - 1) */
	protected void computeBetaStep(Hmm hmm, DataPoint o, int t, int i) {
		double sum = 0.;

		for (int j = 0; j < hmm.nbStates(); j++)
			sum += beta[t + 1][j] * hmm.getAij(i, j)
					* hmm.getOpdf(j).probability(o.getFeatures(j), o.getOutcome());

		beta[t][i] = sum;
	}

	/**
	 * Returns an element of the <i>alpha</i> array.
	 * 
	 * @param t
	 *          The temporal argument of the array (positive but strictly smaller
	 *          than the length of the sequence that helped generating the array).
	 * @param i
	 *          A state index of the HMM that helped generating the array.
	 * @throws {@link UnsupportedOperationException UnsupportedOperationException}
	 *         if alpha array has not been computed.
	 * @return The <i>alpha</i> array (t, i) element.
	 */
	public double alphaElement(int t, int i) {
		if (alpha == null)
			throw new UnsupportedOperationException("Alpha array has not "
					+ "been computed");

		return alpha[t][i];
	}

	/**
	 * Returns an element of the <i>beta</i> array.
	 * 
	 * @param t
	 *          The temporal argument of the array (positive but smaller than the
	 *          length of the sequence that helped generating the array).
	 * @param i
	 *          A state index of the HMM that helped generating the array.
	 * @throws {@link UnsupportedOperationException UnsupportedOperationException}
	 *         if beta array has not been computed.
	 * @return The <i>beta</i> beta (t, i) element.
	 */
	public double betaElement(int t, int i) {
		if (beta == null)
			throw new UnsupportedOperationException("Beta array has not "
					+ "been computed");

		return beta[t][i];
	}

	// compute Pr(Dd)
	private void computeProbability(List<DataPoint> oseq, Hmm hmm,
			EnumSet<Computation> flags) {
		// System.out.println("computeProbability...");

		probability = 0.;

		if (flags.contains(Computation.ALPHA)) {
			// System.out.println("computeProbability:using alpha...");
			for (int i = 0; i < hmm.nbStates(); i++)
				probability += alpha[oseq.size() - 1][i];
		}
		else {
			// System.out.println("computeProbability:using beta...");

			for (int i = 0; i < hmm.nbStates(); i++) {
				probability += hmm.getPi(i)
						* hmm.getOpdf(i).probability(oseq.get(0).getFeatures(i),
								oseq.get(0).getOutcome()) * beta[0][i];
			}
		}
	}

	/**
	 * Return the probability of the sequence that generated this object. For long
	 * sequences, this probability might be very small, or even meaningless
	 * because of underflows.
	 * 
	 * @return The probability of the sequence of interest.
	 */
	public double probability() {
		return probability;
	}
}
