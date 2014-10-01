package fast.hmm;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import be.ac.ulg.montefiore.run.jahmm.ObservationInteger;
import be.ac.ulg.montefiore.run.jahmm.Opdf;
import be.ac.ulg.montefiore.run.jahmm.OpdfInteger;
import fast.common.Bijection;
import fast.data.DataPoint;

/**
 * This class implements a distribution over a finite set of elements. This set
 * is implemented as an <code>enum</code>.
 */
public class OpdfDataPoint implements Opdf<DataPoint> {
	protected OpdfInteger distribution;
	protected final List<Integer> values;
	protected final Bijection mapping;

	/**
	 * Builds a new probability distribution which operates on a finite set of
	 * values. The probabilities are initialized so that the distribution is
	 * uniformaly distributed.
	 * 
	 * @param mapping
	 *          An {@link Enum Enum} class representing the set of values.
	 */
	public OpdfDataPoint(Bijection mapping) {
		values = new ArrayList<Integer>(mapping.values());

		if (values.isEmpty())
			throw new IllegalArgumentException();

		distribution = new OpdfInteger(values.size());
		this.mapping = mapping;
	}

	/**
	 * Builds a new probability distribution which operates on integer values.
	 * 
	 * @param mapping
	 *          An {@link Enum Enum} class representing the set of values.
	 * @param probabilities
	 *          Array holding one probability for each possible value (<i>i.e.</i>
	 *          such that <code>probabilities[i]</code> is the probability of the
	 *          observation <code>i</code>th element of <code>values</code>.
	 */
	public OpdfDataPoint(Bijection mapping, double[] probabilities) {
		values = new ArrayList<Integer>(mapping.values());

		if (probabilities.length == 0 || values.size() != probabilities.length)
			throw new IllegalArgumentException();

		distribution = new OpdfInteger(probabilities);
		this.mapping = mapping;
	}

	@Override
	public double probability(DataPoint o) {
		return distribution.probability(new ObservationInteger(o.getOutcome()));
	}

	@Override
	public DataPoint generate() {
		return new DataPoint(distribution.generate().value);
	}

	@Override
	public void fit(DataPoint... oa) {
		fit(Arrays.asList(oa));
	}

	@Override
	public void fit(Collection<? extends DataPoint> co) {
		List<ObservationInteger> dco = new ArrayList<ObservationInteger>();

		for (DataPoint o : co)
			dco.add(new ObservationInteger(o.getOutcome()));

		distribution.fit(dco);
	}

	@Override
	public void fit(DataPoint[] o, double[] weights) {
		fit(Arrays.asList(o), weights);
	}

	@Override
	public void fit(Collection<? extends DataPoint> co, double[] weights) {
		List<ObservationInteger> dco = new ArrayList<ObservationInteger>();

		for (DataPoint o : co)
			dco.add(new ObservationInteger(o.getOutcome()));

		distribution.fit(dco, weights);
	}

	@Override
	public OpdfDataPoint clone() {
		try {
			OpdfDataPoint opdfDiscrete = (OpdfDataPoint) super.clone();
			opdfDiscrete.distribution = distribution.clone();
			return opdfDiscrete;
		}
		catch (CloneNotSupportedException e) {
			throw new InternalError();
		}
	}

	@Override
	public String toString() {
		return toString(NumberFormat.getInstance());
	}

	@Override
	public String toString(NumberFormat numberFormat) {
		String s = "<";

		for (int i = 0; i < mapping.keys().size(); i++) {

			String k = mapping.get(i);
			s += k
					+ " "
					+ numberFormat.format(distribution
							.probability(new ObservationInteger(i)))
					+ ((i != values.size() - 1) ? ", " : "");
		}
		s += ">";

		return s;
	}

	private static final long serialVersionUID = 1L;

}
