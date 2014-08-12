package be.ac.ulg.montefiore.run.jahmm;

import java.text.NumberFormat;


/**
 * This class implements observations whose values are taken out of a finite
 * set implemented as an enumeration.
 */
public class ObservationDiscrete<E extends Enum<E>>
extends Observation
{
	/**
	 * This observation value.
	 */
	public final E value;
	
	
	public ObservationDiscrete(E value)
	{
		this.value = value;
	}
	
	
	public String toString()
	{
		return value.toString();
	}
	
	
	public String toString(NumberFormat nf)
	{
		return toString();
	}
}
