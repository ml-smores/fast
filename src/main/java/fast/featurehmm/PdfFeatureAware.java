/**
 * FAST v1.0       08/12/2014
 * 
 * This code is only for research purpose not commercial purpose.
 * It is originally developed for research purpose and is still under improvement. 
 * Please email to us if you want to keep in touch with the latest release.
	 We sincerely welcome you to contact Yun Huang (huangyun.ai@gmail.com), or Jose P.Gonzalez-Brenes (josepablog@gmail.com) for problems in the code or cooperation.
 * We thank Taylor Berg-Kirkpatrick (tberg@cs.berkeley.edu) and Jean-Marc Francois (jahmm) for part of their code that FAST is developed based on.
 *
 */

/* 
 * This is built based on:
 * 
 *   jaHMM package - v0.6.1 
 *  Copyright (c) 2004-2006, Jean-Marc Francois.
 */

package fast.featurehmm;

import java.io.Serializable;
//import java.text.NumberFormat;
import be.ac.ulg.montefiore.run.jahmm.Observation;

/**
 * Objects implementing this interface represent a probability (distribution)
 * function which can be used to paramterize init, transition, or emission
 * probabilities.
 * <p>
 * An <code>PdfContextAware</code> can represent a probability function (if the
 * nodes can take discrete values) or a probability distribution (if the nodes
 * are continuous).
 */
public interface PdfFeatureAware<O extends Observation> extends Cloneable,
		Serializable {

	/**
	 * @author hy
	 * @date 10/06/13
	 * 
	 *       Returns the probability (density) of init/transition/emission given
	 *       the corresponding contextual features, and the index (to determine
	 *       the form of logistic regression), based on the distribution defined
	 *       by a set of weights. (index=observationIndex, or hiddenStateIndex)
	 * 
	 * @return The probability (density, if <code>o</code> takes continuous
	 *         values) of <code>o</code> for this function.
	 */
	public double probability(double[] featureValues, int index, String type);

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
	// void fit(Collection<? extends O> co, double[] weights, int
	// hiddenStateIndex,
	// String type);

	// void fit(Collection<? extends O> co, double[][] weights, String type);

	/**
	 * Returns a {@link java.lang.String String} describing this distribution.
	 * 
	 * @param numberFormat
	 *          A formatter used to convert the numbers (<i>e.g.</i>
	 *          probabilities) to strings.
	 * @return A {@link java.lang.String String} describing this distribution.
	 */

	//public String toString(NumberFormat numberFormat);

	public PdfFeatureAware<O> clone();
}
