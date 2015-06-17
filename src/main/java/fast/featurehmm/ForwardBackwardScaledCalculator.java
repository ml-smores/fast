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
 *  scaling: this gives P(St+1=qj|O1..Ot+1) for scaled alpha, ctFactors[t] = P(O1..Ot) (for different t, O1~Ot is different)
 */

package fast.featurehmm;

import java.util.AbstractList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.List;
import be.ac.ulg.montefiore.run.jahmm.Observation;
import fast.common.Matrix;
import fast.data.DataPoint;
//import be.ac.ulg.montefiore.run.jahmm.ForwardBackwardCalculator;

//import be.ac.ulg.montefiore.run.jahmm.Hmm;

public class ForwardBackwardScaledCalculator extends ForwardBackwardCalculator {
	/*
	 * Warning, the semantic of the alpha and beta elements are changed; in this
	 * class, they have their value scaled.
	 */
	// Scaling factors
	private double[] ctFactors;
	private double lnProbability;
	private boolean verbose = false;

	/**
	 * Computes the probability of occurrence of an observation sequence given a
	 * Hidden Markov Model. The algorithms implemented use scaling to avoid
	 * underflows.
	 * 
	 * @param hmm
	 *          A Hidden Markov Model;
	 * @param oseq
	 *          An observations sequence.
	 * @param flags
	 *          How the computation should be done. See the
	 *          {@link ForwardBackwardCalculator.Computation}. The alpha array is
	 *          always computed.
	 */
	public ForwardBackwardScaledCalculator(List<DataPoint> oseq, FeatureHMM hmm,
			EnumSet<Computation> flags) {
		// System.out.println("ForwardBackwardScaledCalculator...");
		if (oseq.isEmpty())
			throw new IllegalArgumentException();

		ctFactors = new double[oseq.size()];
		Arrays.fill(ctFactors, 0.);

		computeAlpha(hmm, oseq);
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

		if (verbose) {
			System.out.println("scaling ctFactors:");
			for (int t = 0; t < ctFactors.length; t++)
				System.out.println("\ttime=" + t + "\t" + ctFactors[t]);
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

		computeProbability(oseq);
	}

	/**
	 * Computes the probability of occurence of an observation sequence given a
	 * Hidden Markov Model. This computation computes the scaled
	 * <code>alpha</code> array as a side effect.
	 * 
	 * @see #ForwardBackwardScaledCalculator(List, FeatureHMM, EnumSet)
	 */
	public ForwardBackwardScaledCalculator(List<DataPoint> oseq, FeatureHMM hmm) {
		this(oseq, hmm, EnumSet.of(Computation.ALPHA));
	}

	/* Computes the content of the scaled alpha array */
	@Override
	protected void computeAlpha(FeatureHMM hmm, List<DataPoint> oseq) {
		// System.out.println("ForwardBackwardScaledCalculator:computeAlpha...");
		alpha = new double[oseq.size()][hmm.getNbHiddenStates()];

		for (int i = 0; i < hmm.getNbHiddenStates(); i++)
			computeAlphaInit(hmm, oseq.get(0), i);
		scale(ctFactors, alpha, 0);

		Iterator<DataPoint> seqIterator = oseq.iterator();
		if (seqIterator.hasNext())
			seqIterator.next();

		for (int t = 1; t < oseq.size(); t++) {
			DataPoint observation = seqIterator.next();

			for (int i = 0; i < hmm.getNbHiddenStates(); i++)
				computeAlphaStep(hmm, observation, t, i);
			// scale the t-th alpha array by dividing [P(O1...Ot,St=qi) +
			// P(O1...Ot,St=qj], new alpha = P(St=qi|O1...Ot)
			scale(ctFactors, alpha, t);
		}
	}

	/*
	 * Computes the content of the scaled beta array. The scaling factors are
	 * those computed for alpha.
	 * 
	 * hy: new beta = P(Ot+1,...ON|St=qi)/P(O1..Ot)
	 */
	protected void computeBeta(FeatureHMM hmm, List<DataPoint> oseq) {
		// System.out.println("ForwardBackwardScaledCalculator:computeBeta...");
		beta = new double[oseq.size()][hmm.getNbHiddenStates()];

		for (int i = 0; i < hmm.getNbHiddenStates(); i++)
			beta[oseq.size() - 1][i] = 1. / ctFactors[oseq.size() - 1];

		for (int t = oseq.size() - 2; t >= 0; t--)
			for (int i = 0; i < hmm.getNbHiddenStates(); i++) {
				computeBetaStep(hmm, oseq.get(t + 1), t, i);
				beta[t][i] /= ctFactors[t];
			}
	}

	/* Normalize alpha[t] and put the normalization factor in ctFactors[t] */
	// hy: this gives P(St=qj|O1..Ot) for scaled alpha, ctFactors[t] =
	// P(O1..Ot)
	// hy: array= new double[oseq.size()][hmm.getNbHiddenStates()];
	private void scale(double[] ctFactors, double[][] array, int t) {
		// System.out.println("ForwardBackwardScaledCalculator:scale...");
		double[] table = array[t];
		double sum = 0.;

		// hy:i->nbStates
		for (int i = 0; i < table.length; i++)
			sum += table[i];

		ctFactors[t] = sum;
		for (int i = 0; i < table.length; i++)
			table[i] /= sum;
	}

	// TODO
	// hy* probability = P(O1)*P(O1,O2)*...*P(O1,O2..On) (not conditional
	// P(O2|O1)....?)
	private <O extends Observation> void computeProbability(List<O> oseq) {
		// System.out.println("ForwardBackwardScaledCalculator:computeProbability...");
		lnProbability = 0.;

		// System.out.println(Arrays.deepToString(alpha ) + " " +
		// Arrays.deepToString(beta) + " " + Arrays.toString(ctFactors));
		for (int t = 0; t < oseq.size(); t++)
			lnProbability += Math.log(ctFactors[t]);

		probability = Math.exp(lnProbability);
		if (verbose)
			System.out.println("probability:\t" + probability);
	}

	// added by JPG:
	public static double getLL(FeatureHMM hmm,
			AbstractList<? extends AbstractList<DataPoint>> students) {
		// System.out.println("ForwardBackwardScaledCalculator:getLL...");
		double ll = 0;
		for (List<DataPoint> student : students) {
			ForwardBackwardScaledCalculator fwbs = new ForwardBackwardScaledCalculator(
					student, hmm, EnumSet.of(Computation.ALPHA, Computation.BETA));
			fwbs.computeProbability(student);
			// System.out.println("~" + fwbs.lnProbability);
			ll += fwbs.lnProbability;
		}
		return ll;
	}

	/**
	 * Return the neperian logarithm of the probability of the sequence that
	 * generated this object.
	 * 
	 * @return The probability of the sequence of interest's neperian logarithm.
	 */
	public double lnProbability() {
		System.out.println("ForwardBackwardScaledCalculator:lnProbability...");
		return lnProbability;
	}

	/*
	 * Added by JPG. See
	 * http://xenia.media.mit.edu/~rahimi/rabiner/rabiner-errata/
	 * rabiner-errata.html
	 */
	public double[][] getStateProbabilities() {
		// System.out
		// .println("ForwardBackwardScaledCalculator:getStateProbabilities...");
		final double[][] p = new double[alpha.length][alpha[0].length];

		// double ct_1 = 1;
		for (int t = 0; t < alpha.length; t++) {
			// ct_1 = ctFactors[t] / ct_1;
			p[t] = Matrix.dotmult(alpha[t], beta[t], ctFactors[t]); // I thought it
																															// should be alpha
																															// * beta *ct_1

			Matrix.assertProbability(p[t]);
		}

		return p;

	}

}
