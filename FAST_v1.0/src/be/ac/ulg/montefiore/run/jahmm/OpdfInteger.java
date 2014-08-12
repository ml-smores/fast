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

import java.text.NumberFormat;
import java.util.Arrays;
import java.util.Collection;

/**
 * This class represents a distribution of a finite number of positive integer
 * observations.
 */
public class OpdfInteger implements Opdf<ObservationInteger> {
	private double[] probabilities;

	/**
	 * Builds a new probability distribution which operates on integer values. The
	 * probabilities are initialized so that the distribution is uniformly
	 * distributed.
	 * 
	 * @param nbEntries
	 *          The number of values to which to associate probabilities.
	 *          Observations handled by this distribution have to be higher or
	 *          equal than 0 and strictly smaller than <code>nbEntries</code>.
	 */
	public OpdfInteger(int nbEntries) {
		if (nbEntries <= 0)
			throw new IllegalArgumentException("Argument must be strictly "
					+ "positive");

		probabilities = new double[nbEntries];

		for (int i = 0; i < nbEntries; i++)
			probabilities[i] = 1. / ((double) nbEntries);
	}

	/**
	 * Builds a new probability distribution which operates on integer values.
	 * 
	 * @param probabilities
	 *          Array holding one probability for each possible argument value
	 *          (<i>i.e.</i> such that <code>probabilities[i]</code> is the
	 *          probability of the observation <code>i</code>.
	 */
	public OpdfInteger(double[] probabilities) {
		if (probabilities.length == 0)
			throw new IllegalArgumentException("Invalid empty array");

		this.probabilities = new double[probabilities.length];

		for (int i = 0; i < probabilities.length; i++)
			if ((this.probabilities[i] = probabilities[i]) < 0.)
				throw new IllegalArgumentException();
	}

	/**
	 * Returns how many integers are associated to probabilities by this
	 * distribution.
	 * 
	 * @return The number of integers are associated to probabilities.
	 */
	public int nbEntries() {
		return probabilities.length;
	}

	public double probability(ObservationInteger o) {
		if (o.value > probabilities.length - 1)
			throw new IllegalArgumentException("Wrong observation value");

		return probabilities[o.value];
	}

	public ObservationInteger generate() {
		double rand = Math.random();

		for (int i = 0; i < probabilities.length - 1; i++)
			if ((rand -= probabilities[i]) < 0.)
				return new ObservationInteger(i);

		return new ObservationInteger(probabilities.length - 1);
	}

	public void fit(ObservationInteger... oa) {
		fit(Arrays.asList(oa));
	}

	public void fit(Collection<? extends ObservationInteger> co) {
		if (co.isEmpty())
			throw new IllegalArgumentException("Empty observation set");

		for (int i = 0; i < probabilities.length; i++)
			probabilities[i] = 0.;

		for (ObservationInteger o : co)
			probabilities[o.value]++;

		for (int i = 0; i < probabilities.length; i++)
			probabilities[i] /= co.size();
	}

	public void fit(ObservationInteger[] o, double[] weights) {
		fit(Arrays.asList(o), weights);
	}

	public void fit(Collection<? extends ObservationInteger> co, double[] weights) {
		if (co.isEmpty() || co.size() != weights.length)
			throw new IllegalArgumentException();

		Arrays.fill(probabilities, 0.);

		int i = 0;
		// hy: o.value is the vk, the specific observation value, here double[]
		// probabilities is for emission probabilities bik in M-step
		for (ObservationInteger o : co)
			probabilities[o.value] += weights[i++];
	}

	public OpdfInteger clone() {
		try {
			return (OpdfInteger) super.clone();
		}
		catch (CloneNotSupportedException e) {
			throw new AssertionError(e);
		}
	}

	public String toString() {
		return toString(NumberFormat.getInstance());
	}

	public String toString(NumberFormat numberFormat) {
		String s = "Integer distribution --- ";

		for (int i = 0; i < nbEntries();) {
			ObservationInteger oi = new ObservationInteger(i);

			s += numberFormat.format(probability(oi))
					+ ((++i < nbEntries()) ? " " : "");
		}

		return s;
	}

	private static final long serialVersionUID = 1L;
}
