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
 * 
 *   jaHMM package - v0.6.1 
 *  Copyright (c) 2004-2006, Jean-Marc Francois.
 */

package fast.hmmfeatures;

import java.io.Serializable;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import be.ac.ulg.montefiore.run.jahmm.Observation;

/**
 * Main Hmm class; it implements an Hidden Markov Model. An Hmm is composed of:
 * <ul>
 * <li><i>states</i>: each state has a given probability of being initial
 * (<i>pi</i>) and an associated observation probability function (<i>opdf</i>).
 * Each state is associated to an index; the first state is numbered 0, the last
 * n-1 (where n is the number of states in the Hmm); this number is given as an
 * argument to the various functions to refer to the matching state.</li>
 * <li><i>transition probabilities</i>: that is, the probability of going from
 * state <i>i</i> to state <i>j</i> (<i>a<sub>i,j</sub></i>).</li>
 * </ul>
 * <p>
 * Important objects extensively used with HMMs are {@link Observation
 * Observation}s, observation sequences and set of observation sequences. An
 * observation sequence is simply a {@link List List} of {@link Observation
 * Observation}s (in the right order, the i-th element of the vector being the
 * i-th element of the sequence). A set of observation sequences is a
 * {@link java.util.List List} of such sequences.
 */

public class Hmm implements Serializable, Cloneable {
	private double pi[];
	private double a[][];
	private ArrayList<OpdfContextAwareLogisticRegression> opdfs;
	private boolean parameterizedEmit;

	// /**
	// * Creates a new Hmm. Each state has the same <i>pi</i> value and the
	// * transition probabilities are all equal.
	// *
	// * @param nbStates
	// * The (strictly positive) number of states of the Hmm.
	// * @param opdfFactory
	// * A pdf generator that is used to build the pdfs associated to each
	// * state.
	// */
	// public Hmm(int nbStates, OpdfFactory<? extends Opdf<O>> opdfFactory) {
	// if (nbStates <= 0)
	// throw new IllegalArgumentException("Number of states must be "
	// + "strictly positive");
	//
	// pi = new double[nbStates];
	// a = new double[nbStates][nbStates];
	// opdfs = new ArrayList<Opdf<O>>(nbStates);
	//
	// for (int i = 0; i < nbStates; i++) {
	// pi[i] = 1. / ((double) nbStates);
	// opdfs.add(opdfFactory.factor()); // hy:use the init weight to initialize
	// // it
	//
	// for (int j = 0; j < nbStates; j++)
	// a[i][j] = 1. / ((double) nbStates);
	// }
	// }

	/**
	 * @author hy
	 * @date 10/06/13
	 * 
	 *       Creates a new Hmm. All the Hmm parameters are given as arguments.
	 * 
	 * @param pi
	 *          The initial probability values. <code>pi[i]</code> is the initial
	 *          probability of state <code>i</code>. This array is copied.
	 * @param a
	 *          The state transition probability array. <code>a[i][j]</code> is
	 *          the probability of going from state <code>i</code> to state
	 *          <code>j</code>. This array is copied.
	 * @param opdfs
	 *          The observation distributions. <code>opdfs.get(i)</code> is the
	 *          observation distribution associated with state <code>i</code>. The
	 *          distributions are not copied.
	 */
	public Hmm(double[] pi, double[][] a,
			ArrayList<OpdfContextAwareLogisticRegression> opdfs,
			boolean parameterizedEmit) {
		if (a.length == 0 || pi.length != a.length || opdfs.size() != a.length)
			throw new IllegalArgumentException("Wrong parameter");

		this.pi = pi.clone();
		this.a = new double[a.length][];

		for (int i = 0; i < a.length; i++) {
			if (a[i].length != a.length)
				throw new IllegalArgumentException("'A' is not a square" + "matrix");
			this.a[i] = a[i].clone();
		}

		// CAUTION: Here actually it doesn't work, since still there are two objects
		// of OpdfContextAwareLogisticRegression
		// if (Opts.oneLogisticRegression) {
		// this.opdfs = opdfs;
		// }
		// else {
		// Here it allocates new space by (elementData = c.toArray())
		this.opdfs = new ArrayList<OpdfContextAwareLogisticRegression>(opdfs);
		// }
		this.parameterizedEmit = parameterizedEmit;
	}

	/**
	 * @author hy
	 * @date 10/06/13
	 * 
	 *       Change Opdf -> OpdfContextAware
	 * 
	 *       Creates a new Hmm. The parameters of the created Hmm set to
	 *       <code>null</code> specified and must be set using the appropriate
	 *       methods.
	 * 
	 * @param nbStates
	 *          The (strictly positive) number of states of the Hmm.
	 */
	protected Hmm(int nbStates) {
		if (nbStates <= 0)
			throw new IllegalArgumentException("Number of states must be "
					+ "positive");

		pi = new double[nbStates];
		a = new double[nbStates][nbStates];
		opdfs = new ArrayList<OpdfContextAwareLogisticRegression>(nbStates);

		for (int i = 0; i < nbStates; i++)
			opdfs.add(null);
	}

	/**
	 * Returns the number of states of this Hmm.
	 * 
	 * @return The number of states of this Hmm.
	 */
	public int nbStates() {
		return pi.length;
	}

	/**
	 * Returns the <i>pi</i> value associated with a given state.
	 * 
	 * @param stateNb
	 *          A state number such that
	 *          <code>0 &le; stateNb &lt; nbStates()</code>
	 * @return The <i>pi</i> value associated to <code>stateNb</code>.
	 */
	public double getPi(int stateNb) {
		return pi[stateNb];
	}

	/**
	 * Sets the <i>pi</i> value associated with a given state.
	 * 
	 * @param stateNb
	 *          A state number such that
	 *          <code>0 &le; stateNb &lt; nbStates()</code>.
	 * @param value
	 *          The <i>pi</i> value to associate to state number
	 *          <code>stateNb</code>
	 */
	public void setPi(int stateNb, double value) {
		pi[stateNb] = value;
	}

	/**
	 * Returns the opdf associated with a given state.
	 * 
	 * @param stateNb
	 *          A state number such that
	 *          <code>0 &le; stateNb &lt; nbStates()</code>.
	 * @return The opdf associated to state <code>stateNb</code>.
	 */
	public OpdfContextAwareLogisticRegression getOpdf(int stateNb) {
		return opdfs.get(stateNb);// hy:need to modify in Opdf<O> in the previous
		// code
	}

	// /**
	// * Sets the opdf associated with a given state.
	// *
	// * @param stateNb
	// * A state number such that
	// * <code>0 &le; stateNb &lt; nbStates()</code>.
	// * @param opdf
	// * An observation probability function.
	// */
	// public void setOpdf(int stateNb, Opdf<O> opdf) {
	// opdfs.set(stateNb, opdf);// hy:may need to modify here to update the bjk
	// }

	/**
	 * Returns the probability associated with the transition going from state
	 * <i>i</i> to state <i>j</i> (<i>a<sub>i,j</sub></i>).
	 * 
	 * @param i
	 *          The first state number such that
	 *          <code>0 &le; i &lt; nbStates()</code>.
	 * @param j
	 *          The second state number such that
	 *          <code>0 &le; j &lt; nbStates()</code>.
	 * @return The probability associated to the transition going from
	 *         <code>i</code> to state <code>j</code>.
	 */
	public double getAij(int i, int j) {
		return a[i][j];
	}

	/**
	 * Sets the probability associated to the transition going from state <i>i</i>
	 * to state <i>j</i> (<i>A<sub>i,j</sub></i>).
	 * 
	 * @param i
	 *          The first state number such that
	 *          <code>0 &le; i &lt; nbStates()</code>.
	 * @param j
	 *          The second state number such that
	 *          <code>0 &le; j &lt; nbStates()</code>.
	 * @param value
	 *          The value of <i>A<sub>i,j</sub></i>.
	 */
	public void setAij(int i, int j, double value) {
		a[i][j] = value;
	}

	// /**
	// * Returns an array containing the most likely state sequence matching an
	// * observation sequence given this Hmm. This sequence <code>I</code>
	// maximizes
	// * the probability of <code>P[I|O,Model]</code> where <code>O</code> is the
	// * observation sequence and <code>Model</code> this Hmm model.
	// *
	// * @param oseq
	// * A non-empty observation sequence.
	// * @return An array containing the most likely sequence of state numbers.
	// This
	// * array can be modified.
	// */
	// public int[] mostLikelyStateSequence(List<? extends O> oseq) {
	// return (new ViterbiCalculator(oseq, this)).stateSequence();
	// }

	// /**
	// * Returns the probability of an observation sequence given this Hmm.
	// *
	// * @param oseq
	// * A non-empty observation sequence.
	// * @return The probability of this sequence.
	// */
	// public double probability(List<? extends O> oseq) {
	// return (new ForwardBackwardCalculator(oseq, this)).probability();
	// }

	// /**
	// * Returns the neperian logarithm of observation sequence's probability
	// given
	// * this Hmm. A <i>scaling</i> procedure is used in order to avoid underflows
	// * when computing the probability of long sequences.
	// *
	// * @param oseq
	// * A non-empty observation sequence.
	// * @return The probability of this sequence.
	// */
	// public double lnProbability(List<? extends O> oseq) {
	// return (new ForwardBackwardScaledCalculator(oseq, this)).lnProbability();
	// }

	// /**
	// * Returns the probability of an observation sequence along a state sequence
	// * given this Hmm.
	// *
	// * @param oseq
	// * A non-empty observation sequence.
	// * @param sseq
	// * An array containing a sequence of state numbers. The length of
	// * this array must be equal to the length of <code>oseq</code>
	// * @return The probability P[oseq,sseq|H], where H is this Hmm.
	// */
	// public double probability(List<? extends O> oseq, int[] sseq) {
	// if (oseq.size() != sseq.length || oseq.isEmpty())
	// throw new IllegalArgumentException();
	//
	// double probability = getPi(sseq[0]);
	//
	// Iterator<? extends O> oseqIterator = oseq.iterator();
	//
	// for (int i = 0; i < sseq.length - 1; i++)
	// probability *= getOpdf(sseq[i]).probability(oseqIterator.next())
	// * getAij(sseq[i], sseq[i + 1]);
	//
	// return probability
	// * getOpdf(sseq[sseq.length - 1]).probability(oseq.get(sseq.length - 1));
	// }

	// /**
	// * Gives a description of this Hmm.
	// *
	// * @param nf
	// * A number formatter used to print numbers (e.g. Aij values).
	// * @return A textual description of this Hmm.
	// */
	// public String toString(NumberFormat nf) {
	// String s = "Hmm with " + nbStates() + " state(s)\n";
	//
	// for (int i = 0; i < nbStates(); i++) {
	// s += "\nState " + i + "\n";
	// s += "  Pi: " + getPi(i) + "\n";
	// s += "  Aij:";
	//
	// for (int j = 0; j < nbStates(); j++)
	// s += " " + nf.format(getAij(i, j));
	// s += "\n";
	//
	// s += "  Opdf: " + ((Opdf<O>) getOpdf(i)).toString(nf) + "\n";
	// }
	//
	// return s;
	// }

	/**
	 * Gives a description of this Hmm.
	 * 
	 * @param nf
	 *          A number formatter used to print numbers (e.g. Aij values).
	 * @return A textual description of this Hmm.
	 */
	public String toString(NumberFormat nf) {
		String s = "Hmm with " + nbStates() + " state(s)\n";

		for (int i = 0; i < nbStates(); i++) {
			s += "\nState " + i + "\n";
			s += "  Pi:\t" + getPi(i) + "\n";
			s += "  Aij:";

			for (int j = 0; j < nbStates(); j++)
				s += "\t" + getAij(i, j); // hy* nf.format(getAij(i, j));
			s += "\n";

			s += "  Opdf:";//
			if (parameterizedEmit) {
				s += "weights:";
				double[] w = getOpdf(i).featureWeights;
				for (int k = 0; k < w.length; k++)
					s += "\t" + w[k];
				if (w.length == 1) {
					s += "\tprobability:\t" + 1 / (1 + Math.exp(-w[0]));
				}
				s += "\n";
			}
			else {
				double[] p = getOpdf(i).probabilities;
				for (int k = 0; k < p.length; k++)
					s += "\t" + getOpdf(i).classMapping.get(k) + "\t" + p[k];
				s += "\n";
			}
		}
		return s;
	}

	/**
	 * Gives a description of this Hmm.
	 * 
	 * @return A textual description of this Hmm.
	 */
	public String toString() {
		return toString(NumberFormat.getInstance());
	}

	public Hmm clone() throws CloneNotSupportedException {
		Hmm Hmm = new Hmm(nbStates());

		Hmm.pi = pi.clone();
		Hmm.a = a.clone();
		// hy
		Hmm.parameterizedEmit = parameterizedEmit;

		for (int i = 0; i < a.length; i++)
			Hmm.a[i] = a[i].clone();

		for (int i = 0; i < Hmm.opdfs.size(); i++)
			Hmm.opdfs.set(i, opdfs.get(i).clone());

		return Hmm;
	}

	private static final long serialVersionUID = 2L;
}
