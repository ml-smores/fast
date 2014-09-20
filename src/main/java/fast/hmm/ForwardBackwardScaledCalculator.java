package fast.hmm;

import java.util.AbstractList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.List;

import be.ac.ulg.montefiore.run.jahmm.ForwardBackwardCalculator;
import be.ac.ulg.montefiore.run.jahmm.Hmm;
import be.ac.ulg.montefiore.run.jahmm.Observation;
import fast.common.Matrix;

public class ForwardBackwardScaledCalculator extends ForwardBackwardCalculator {
	/*
	 * Warning, the semantic of the alpha and beta elements are changed; in this
	 * class, they have their value scaled.
	 */
	// Scaling factors
	private double[] ctFactors;
	private double lnProbability;

	/**
	 * Computes the probability of occurence of an observation sequence given a
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
	public <O extends Observation> ForwardBackwardScaledCalculator(List<O> oseq,
			Hmm<O> hmm, EnumSet<Computation> flags) {
		if (oseq.isEmpty())
			throw new IllegalArgumentException();

		ctFactors = new double[oseq.size()];
		Arrays.fill(ctFactors, 0.);

		computeAlpha(hmm, oseq);

		if (flags.contains(Computation.BETA))
			computeBeta(hmm, oseq);

		computeProbability(oseq);
	}

	/**
	 * Computes the probability of occurence of an observation sequence given a
	 * Hidden Markov Model. This computation computes the scaled
	 * <code>alpha</code> array as a side effect.
	 * 
	 * @see #ForwardBackwardScaledCalculator(List, Hmm, EnumSet)
	 */
	public <O extends Observation> ForwardBackwardScaledCalculator(List<O> oseq,
			Hmm<O> hmm) {
		this(oseq, hmm, EnumSet.of(Computation.ALPHA));
	}

	/* Computes the content of the scaled alpha array */
	@Override
	protected <O extends Observation> void computeAlpha(Hmm<? super O> hmm,
			List<O> oseq) {
		alpha = new double[oseq.size()][hmm.nbStates()];

		for (int i = 0; i < hmm.nbStates(); i++)
			computeAlphaInit(hmm, oseq.get(0), i);
		scale(ctFactors, alpha, 0);

		Iterator<? extends O> seqIterator = oseq.iterator();
		if (seqIterator.hasNext())
			seqIterator.next();

		for (int t = 1; t < oseq.size(); t++) {
			O observation = seqIterator.next();

			for (int i = 0; i < hmm.nbStates(); i++)
				computeAlphaStep(hmm, observation, t, i);
			scale(ctFactors, alpha, t);
		}
	}

	/*
	 * Computes the content of the scaled beta array. The scaling factors are
	 * those computed for alpha.
	 */
	@Override
	protected <O extends Observation> void computeBeta(Hmm<? super O> hmm,
			List<O> oseq) {
		beta = new double[oseq.size()][hmm.nbStates()];

		for (int i = 0; i < hmm.nbStates(); i++)
			beta[oseq.size() - 1][i] = 1. / ctFactors[oseq.size() - 1];

		for (int t = oseq.size() - 2; t >= 0; t--)
			for (int i = 0; i < hmm.nbStates(); i++) {
				computeBetaStep(hmm, oseq.get(t + 1), t, i);
				beta[t][i] /= ctFactors[t];
			}
	}

	/* Normalize alpha[t] and put the normalization factor in ctFactors[t] */
	private void scale(double[] ctFactors, double[][] array, int t) {
		double[] table = array[t];
		double sum = 0.;

		for (int i = 0; i < table.length; i++)
			sum += table[i];

		ctFactors[t] = sum;
		for (int i = 0; i < table.length; i++)
			table[i] /= sum;
	}

	private <O extends Observation> void computeProbability(List<O> oseq) {
		lnProbability = 0.;

		// System.out.println(Arrays.deepToString(alpha ) + " " +
		// Arrays.deepToString(beta) + " " + Arrays.toString(ctFactors));
		for (int t = 0; t < oseq.size(); t++)
			lnProbability += Math.log(ctFactors[t]);

		probability = Math.exp(lnProbability);
	}

	// added by JPG:
	public static <O extends Observation> double getLL(Hmm<O> hmm,
			AbstractList<? extends AbstractList<O>> students) {
		double ll = 0;
		for (List<O> student : students) {
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
		return lnProbability;
	}

	/*
	 * Added by JPG. See
	 * http://xenia.media.mit.edu/~rahimi/rabiner/rabiner-errata/
	 * rabiner-errata.html
	 */
	public double[][] getStateProbabilities() {

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
