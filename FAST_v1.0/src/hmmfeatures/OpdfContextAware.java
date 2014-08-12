/* 
 * @author hy
 * @date 10/06/13
 * 
 * This class is built based on:
 * 
 * jahmm package - v0.6.1 
 *  Copyright (c) 2004-2006, Jean-Marc Francois.
 *
 */

package hmmfeatures;

import java.io.Serializable;
import java.text.NumberFormat;
import java.util.Collection;
import be.ac.ulg.montefiore.run.jahmm.Observation;

/**
 * Objects implementing this interface represent an observation probability
 * (distribution) function.
 * <p>
 * An <code>OpdfContextAware</code> can represent a probability function (if the
 * observations can take discrete values) or a probability distribution (if the
 * observations are continuous).
 */
public interface OpdfContextAware<O extends Observation> extends Cloneable,
		Serializable {

	/**
	 * @author hy
	 * @date 10/06/13
	 * 
	 *       Returns the probability (density) of an observation given the
	 *       contextual features correspond to this observation, and the
	 *       observation's index (to determine the form of logistic regression),
	 *       based on the distribution defined by a set of weights (currently use
	 *       logistic regression)
	 * 
	 * @param o
	 *          An observation.
	 * 
	 * @return The probability (density, if <code>o</code> takes continuous
	 *         values) of <code>o</code> for this function.
	 */
	public double probability(double[] featureValues, int observationIndex);

	// /**
	// * Generates a (pseudo) random observation according to this distribution.
	// *
	// * @return An observation.
	// */
	// public O generate();
	//
	// /**
	// * Fits this observation probability (distribution) function to a (non
	// empty)
	// * set of observations. The meaning to give to <i>fits</i> should be <i>has
	// * the maximum likelihood</i> if possible.
	// *
	// * @param oa
	// * An array of observations compatible with this function.
	// */
	// public void fit(O... oa);
	//
	// /**
	// * Fits this observation probability (distribution) function to a (non
	// empty)
	// * set of observations. The meaning to give to <i>fits</i> should be <i>has
	// * the maximum likelihood</i> if possible.
	// *
	// * @param co
	// * A set of observations compatible with this function.
	// */
	// public void fit(Collection<? extends O> co);

	/**
	 * Fits this observation probability (distribution) function to a weighted
	 * (non empty) set of observations. Equations (53) and (54) of Rabiner's <i>A
	 * Tutorial on Hidden Markov Models and Selected Applications in Speech
	 * Recognition</i> explain how the weights can be used.
	 * 
	 * @param o
	 *          An array of observations compatible with this factory.
	 * @param weights
	 *          The weight associated to each observation (such that
	 *          <code>weight.length == o.length</code> and the sum of all the
	 *          elements equals 1).
	 */
	// void fit(O[] o, double[] weights);// hy: may be a good place to plug
	// logistic
	// // regression in!

	/**
	 * Fits this observation probability (distribution) function to a weighted
	 * (non empty) set of observations. Equations (53) and (54) of Rabiner's <i>A
	 * Tutorial on Hidden Markov Models and Selected Applications in Speech
	 * Recognition</i> explain how the weights can be used.
	 * 
	 * @param co
	 *          A set of observations compatible with this factory.
	 * @param weights
	 *          The weight associated to each observation (such that
	 *          <code>weight.length == o.length</code> and the sum of all the
	 *          elements equals 1).
	 */
	void fit(Collection<? extends O> co, double[] weights, int hiddenStateIndex);

	void fit(Collection<? extends O> co, double[][] weights);

	/**
	 * Returns a {@link java.lang.String String} describing this distribution.
	 * 
	 * @param numberFormat
	 *          A formatter used to convert the numbers (<i>e.g.</i>
	 *          probabilities) to strings.
	 * @return A {@link java.lang.String String} describing this distribution.
	 */

	public String toString(NumberFormat numberFormat);

	public OpdfContextAware<O> clone();
}
