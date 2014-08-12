/* jahmm package - v0.6.1 */

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

package be.ac.ulg.montefiore.run.jahmm.learn;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.List;
import be.ac.ulg.montefiore.run.jahmm.ForwardBackwardCalculator;
import be.ac.ulg.montefiore.run.jahmm.Hmm;
import be.ac.ulg.montefiore.run.jahmm.Observation;
import be.ac.ulg.montefiore.run.jahmm.Opdf;

/**
 * An implementation of the Baum-Welch learning algorithm. This algorithm finds
 * a HMM that models a set of observation sequences.
 */
public class BaumWelchLearner {
	/**
	 * Number of iterations performed by the {@link #learn} method.
	 */
	private int nbIterations = 9;

	/**
	 * Initializes a Baum-Welch instance.
	 */
	public BaumWelchLearner() {
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
	 */
	public <O extends Observation> Hmm<O> iterate(Hmm<O> hmm,
			List<? extends List<? extends O>> sequences) {
		Hmm<O> nhmm;
		try {
			nhmm = hmm.clone();
		}
		catch (CloneNotSupportedException e) {
			throw new InternalError();
		}

		/* gamma and xi arrays are those defined by Rabiner and Juang */
		/* allGamma[n] = gamma array associated to observation sequence n */
		double allGamma[][][] = new double[sequences.size()][][];

		/*
		 * a[i][j] = aijNum[i][j] / aijDen[i] aijDen[i] = expected number of
		 * transitions from state i aijNum[i][j] = expected number of transitions
		 * from state i to j
		 */
		double aijNum[][] = new double[hmm.nbStates()][hmm.nbStates()];
		double aijDen[] = new double[hmm.nbStates()];

		Arrays.fill(aijDen, 0.);
		for (int i = 0; i < hmm.nbStates(); i++)
			Arrays.fill(aijNum[i], 0.);

		int g = 0;
		double ll = 0;
		for (List<? extends O> obsSeq : sequences) {
			// hy:
			ForwardBackwardCalculator fbc = generateForwardBackwardCalculator(obsSeq,
					hmm);

			ll += Math.log(fbc.probability());
			double xi[][][] = estimateXi(obsSeq, fbc, hmm);
			double gamma[][] = allGamma[g++] = estimateGamma(xi, fbc);

			for (int i = 0; i < hmm.nbStates(); i++)
				for (int t = 0; t < obsSeq.size() - 1; t++) {
					aijDen[i] += gamma[t][i];

					for (int j = 0; j < hmm.nbStates(); j++)
						aijNum[i][j] += xi[t][i][j];
				}
		}
		System.err.println(ll);

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

		for (int o = 0; o < sequences.size(); o++)
			for (int i = 0; i < hmm.nbStates(); i++)
				nhmm.setPi(i, nhmm.getPi(i) + allGamma[o][0][i] / sequences.size());

		/* pdfs computation */
		for (int i = 0; i < hmm.nbStates(); i++) {
			List<O> observations = KMeansLearner.flat(sequences);
			double[] weights = new double[observations.size()];
			double sum = 0.;
			int j = 0;

			int o = 0;
			for (List<? extends O> obsSeq : sequences) {
				for (int t = 0; t < obsSeq.size(); t++, j++)
					sum += weights[j] = allGamma[o][t][i];
				o++;
			}

			for (j--; j >= 0; j--)
				weights[j] /= sum;

			Opdf<O> opdf = nhmm.getOpdf(i);
			opdf.fit(observations, weights);
		}

		return nhmm;
	}

	protected <O extends Observation> ForwardBackwardCalculator generateForwardBackwardCalculator(
			List<? extends O> sequence, Hmm<O> hmm) {
		// hy:
		return new ForwardBackwardCalculator(sequence, hmm,
				EnumSet.allOf(ForwardBackwardCalculator.Computation.class));
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
	 */
	public <O extends Observation> Hmm<O> learn(Hmm<O> initialHmm,
			List<? extends List<? extends O>> sequences) {
		Hmm<O> hmm = initialHmm;

		for (int i = 0; i < nbIterations; i++)
			hmm = iterate(hmm, sequences);// hy

		return hmm;
	}

	protected <O extends Observation> double[][][] estimateXi(
			List<? extends O> sequence, ForwardBackwardCalculator fbc, Hmm<O> hmm) {
		if (sequence.size() <= 1)
			throw new IllegalArgumentException("Observation sequence too " + "short");

		double xi[][][] = new double[sequence.size() - 1][hmm.nbStates()][hmm
				.nbStates()];
		double probability = fbc.probability();

		Iterator<? extends O> seqIterator = sequence.iterator();
		seqIterator.next();

		for (int t = 0; t < sequence.size() - 1; t++) {
			O o = seqIterator.next();

			for (int i = 0; i < hmm.nbStates(); i++)
				for (int j = 0; j < hmm.nbStates(); j++)
					xi[t][i][j] = fbc.alphaElement(t, i) * hmm.getAij(i, j)
							* hmm.getOpdf(j).probability(o) * fbc.betaElement(t + 1, j)
							/ probability;
		}

		return xi;
	}

	/*
	 * gamma[][] could be computed directly using the alpha and beta arrays, but
	 * this (slower) method is prefered because it doesn't change if the xi array
	 * has been scaled (and should be changed with the scaled alpha and beta
	 * arrays).
	 */
	protected double[][] estimateGamma(double[][][] xi,
			ForwardBackwardCalculator fbc) {
		double[][] gamma = new double[xi.length + 1][xi[0].length];

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
