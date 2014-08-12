package be.ac.ulg.montefiore.run.jahmm;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;

/**
 * This class implements a distribution over a finite set of elements. This set
 * is implemented as an <code>enum</code>.
 */
public class OpdfDiscrete<E extends Enum<E>> implements
		Opdf<ObservationDiscrete<E>> {
	protected OpdfInteger distribution;
	protected final List<E> values;
	protected final EnumMap<E, ObservationInteger> toIntegerMap;

	/**
	 * Builds a new probability distribution which operates on a finite set of
	 * values. The probabilities are initialized so that the distribution is
	 * uniformly distributed.
	 * 
	 * @param valuesClass
	 *          An {@link Enum Enum} class representing the set of values.
	 */
	public OpdfDiscrete(Class<E> valuesClass) {
		values = new ArrayList<E>(EnumSet.allOf(valuesClass));

		if (values.isEmpty())
			throw new IllegalArgumentException();

		distribution = new OpdfInteger(values.size());
		toIntegerMap = createMap(valuesClass);
	}

	/**
	 * Builds a new probability distribution which operates on integer values.
	 * 
	 * @param valuesClass
	 *          An {@link Enum Enum} class representing the set of values.
	 * @param probabilities
	 *          Array holding one probability for each possible value (<i>i.e.</i>
	 *          such that <code>probabilities[i]</code> is the probability of the
	 *          observation <code>i</code>th element of <code>values</code>.
	 */
	public OpdfDiscrete(Class<E> valuesClass, double[] probabilities) {
		values = new ArrayList<E>(EnumSet.allOf(valuesClass));

		if (probabilities.length == 0 || values.size() != probabilities.length)
			throw new IllegalArgumentException();

		distribution = new OpdfInteger(probabilities);
		toIntegerMap = createMap(valuesClass);
	}

	private EnumMap<E, ObservationInteger> createMap(Class<E> valuesClass) {
		EnumMap<E, ObservationInteger> result = new EnumMap<E, ObservationInteger>(
				valuesClass);

		for (E value : values)
			result.put(value, new ObservationInteger(value.ordinal()));

		return result;
	}

	public double probability(ObservationDiscrete o) {
		return distribution.probability(toIntegerMap.get(o.value));
	}

	public ObservationDiscrete<E> generate() {
		return new ObservationDiscrete<E>(values.get(distribution.generate().value));
	}

	public void fit(ObservationDiscrete<E>... oa) {
		fit(Arrays.asList(oa));
	}

	public void fit(Collection<? extends ObservationDiscrete<E>> co) {
		List<ObservationInteger> dco = new ArrayList<ObservationInteger>();

		for (ObservationDiscrete<E> o : co)
			dco.add(toIntegerMap.get(o.value));

		distribution.fit(dco);
	}

	public void fit(ObservationDiscrete<E>[] o, double[] weights) {
		fit(Arrays.asList(o), weights);
	}

	public void fit(Collection<? extends ObservationDiscrete<E>> co,
			double[] weights) {
		List<ObservationInteger> dco = new ArrayList<ObservationInteger>();

		for (ObservationDiscrete<E> o : co)
			dco.add(toIntegerMap.get(o.value));

		distribution.fit(dco, weights);
	}

	@SuppressWarnings("unchecked")
	public OpdfDiscrete<E> clone() {
		try {
			OpdfDiscrete<E> opdfDiscrete = (OpdfDiscrete<E>) super.clone();
			opdfDiscrete.distribution = distribution.clone();
			return opdfDiscrete;
		}
		catch (CloneNotSupportedException e) {
			throw new InternalError();
		}
	}

	public String toString() {
		return toString(NumberFormat.getInstance());
	}

	public String toString(NumberFormat numberFormat) {
		String s = "Discrete distribution --- ";

		for (int i = 0; i < values.size();) {
			ObservationDiscrete o = new ObservationDiscrete<E>(values.get(i));

			s += o + " " + numberFormat.format(probability(o))
					+ ((++i < values.size()) ? ", " : "");
		}

		return s;
	}

	private static final long serialVersionUID = 1L;
}
