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

package be.ac.ulg.montefiore.run.jahmm;

import java.io.Serializable;
import java.text.NumberFormat;
import java.util.Collection;

/**
 * Objects implementing this interface represent an observation probability
 * (distribution) function.
 * <p>
 * An <code>Opdf</code> can represent a probability function (if the
 * observations can take discrete values) or a probability distribution (if the
 * observations are continuous).
 */
public interface Opdf<O extends Observation> extends Cloneable, Serializable {

	/**
	 * Returns the probability (density) of an observation given a distribution.
	 * 
	 * @param o
	 *          An observation.
	 * @return The probability (density, if <code>o</code> takes continuous
	 *         values) of <code>o</code> for this function.
	 */
	public double probability(O o);

	/**
	 * Generates a (pseudo) random observation according to this distribution.
	 * 
	 * @return An observation.
	 */
	public O generate();

	/**
	 * Fits this observation probability (distribution) function to a (non empty)
	 * set of observations. The meaning to give to <i>fits</i> should be <i>has
	 * the maximum likelihood</i> if possible.
	 * 
	 * @param oa
	 *          An array of observations compatible with this function.
	 */
	public void fit(O... oa);

	/**
	 * Fits this observation probability (distribution) function to a (non empty)
	 * set of observations. The meaning to give to <i>fits</i> should be <i>has
	 * the maximum likelihood</i> if possible.
	 * 
	 * @param co
	 *          A set of observations compatible with this function.
	 */
	public void fit(Collection<? extends O> co);

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
	void fit(O[] o, double[] weights);// hy: may be a good place to plug logistic
																		// regression in!

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
	void fit(Collection<? extends O> co, double[] weights);

	/**
	 * Returns a {@link java.lang.String String} describing this distribution.
	 * 
	 * @param numberFormat
	 *          A formatter used to convert the numbers (<i>e.g.</i>
	 *          probabilities) to strings.
	 * @return A {@link java.lang.String String} describing this distribution.
	 */
	public String toString(NumberFormat numberFormat);

	public Opdf<O> clone();
}
