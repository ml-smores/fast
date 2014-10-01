package fast.hmm;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.List;
import be.ac.ulg.montefiore.run.jahmm.ForwardBackwardCalculator;
import be.ac.ulg.montefiore.run.jahmm.ForwardBackwardCalculator.Computation;
import be.ac.ulg.montefiore.run.jahmm.Hmm;
import be.ac.ulg.montefiore.run.jahmm.Observation;
import fast.common.Matrix;

public class ForwardBackward

{
	double[][] alpha, beta;

	/*
	 * Warning, the semantic of the alpha and beta elements are changed; in this
	 * class, they have their value scaled.
	 */
	// Scaling factors
	protected double[] ctFactors; // JPG
	private Double probability, lnProbability;

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
	public <O extends Observation> ForwardBackward(List<? extends O> oseq,
			Hmm<O> hmm, EnumSet<Computation> flags) {
		if (oseq.isEmpty())
			throw new IllegalArgumentException();

		ctFactors = new double[oseq.size()];
		Arrays.fill(ctFactors, 0.);

		computeAlpha(hmm, oseq);

		if (flags.contains(Computation.BETA))
			computeBeta(hmm, oseq);

		// JPG: computeProbability(oseq, hmm, flags);
	}

	/**
	 * Computes the probability of occurence of an observation sequence given a
	 * Hidden Markov Model. This computation computes the scaled
	 * <code>alpha</code> array as a side effect.
	 * 
	 * @see #ForwardBackwardScaledCalculator(List, Hmm, EnumSet)
	 */
	public <O extends Observation> ForwardBackward(List<? extends O> oseq,
			Hmm<O> hmm) {
		this(oseq, hmm, EnumSet.of(Computation.ALPHA));
	}

	/* Computes the content of the scaled alpha array */
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

	public <O extends Observation> void computeProbability(List<O> oseq,
			Hmm<? super O> hmm, EnumSet<Computation> flags) {
		lnProbability = 0.;

		for (int t = 0; t < oseq.size(); t++)
			lnProbability += Math.log(ctFactors[t]);

		probability = Math.exp(lnProbability);
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

	// *************************************************************************************
	/*
	 * Added by JPG. See
	 * http://xenia.media.mit.edu/~rahimi/rabiner/rabiner-errata/
	 * rabiner-errata.html
	 */
	/* Computes alpha[t][j] (t > 0) */

	/* Normalize alpha[t] and put the normalization factor in ctFactors[t] */
	protected static void scale(double[] ctFactors, double[][] array, int t) {
		ctFactors[t] = scale(array[t]);
		/*
		 * double[] table = array[t]; double sum = 0.;
		 * 
		 * for (int i = 0; i < table.length; i++) sum += table[i];
		 * 
		 * ctFactors[t] = sum; for (int i = 0; i < table.length; i++) table[i] /=
		 * sum;
		 */
	}

	protected static double scale(double[] table)// , int t)
	{
		// double[] table = array[t];

		double sum = 0.;

		for (int i = 0; i < table.length; i++)
			sum += table[i];

		for (int i = 0; i < table.length; i++)
			table[i] /= sum;

		return sum;
	}

	protected static <O extends Observation> double computeAlphaStep(
			double alpha[][], Hmm<O> hmm, O o, int t, int j) {
		double sum = 0.;

		for (int i = 0; i < hmm.nbStates(); i++)
			sum += alpha[t - 1][i] * hmm.getAij(i, j);

		return sum * hmm.getOpdf(j).probability(o);
	}

	/* Computes alpha[t][j] (t > 0) */
	protected <O extends Observation> void computeAlphaStep(Hmm<? super O> hmm,
			O o, int t, int j) {
		alpha[t][j] = computeAlphaStep(this.alpha, hmm, o, t, j);
	}

	protected static <O extends Observation> double alphaInit(Hmm<? super O> hmm,
			O o, int i) {
		return hmm.getPi(i) * hmm.getOpdf(i).probability(o);
	}

	/* Computes alpha[0][i] */
	protected <O extends Observation> void computeAlphaInit(Hmm<O> hmm, O o, int i) {
		alpha[0][i] = alphaInit(hmm, o, i);
	}

	/* Computes beta[t][i] (t < obs. seq.le length - 1) */
	protected <O extends Observation> void computeBetaStep(Hmm<O> hmm, O o,
			int t, int i) {
		beta[t][i] = computeBetaStep(this.beta, hmm, o, t, i);
	}

	protected static <O extends Observation> double computeBetaStep(
			double[][] beta, Hmm<? super O> hmm, O o, int t, int i) {
		double sum = 0.;

		for (int j = 0; j < hmm.nbStates(); j++)
			sum += beta[t + 1][j] * hmm.getAij(i, j) * hmm.getOpdf(j).probability(o);

		return sum;
	}

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

	protected ForwardBackward() {

	}

	public double[] getCtFactors() {
		return ctFactors;
	}

	public double[][] getAlpha() {
		return alpha;
	}

	public double[][] getBeta() {
		return beta;
	}

}
