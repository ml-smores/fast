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


/**
 * This class holds an Observation described by a vector of reals.
 */
public class ObservationVector extends Observation
implements Cloneable, CentroidFactory<ObservationVector>
{	
	final double[] value;
	
	
	/**
	 * An observation whose components are 0.
	 *
	 * @param dimension The dimension of the resulting vector.
	 */
	public ObservationVector(int dimension)
	{
		if (dimension <= 0)
			throw new IllegalArgumentException("Dimension must be strictly " +
					"positive");
		
		this.value = new double[dimension];
	}
	
	
	/**
	 * An observation that can be described by a vector of reals.
	 *
	 * @param value The value of this observation.  This array is copied.
	 */
	public ObservationVector(double[] value)
	{
		this(value.length);
		
		for (int i = 0 ; i < value.length; i++)
			this.value[i] = value[i];
	}
	
	
	/**
	 * Returns the dimension of this vector.
	 */
	public int dimension()
	{
		return value.length;
	}
	
	
	/**
	 * Returns the values composing this observation.
	 *
	 * @return The values of this observation. The array is copied.
	 */
	public double[] values()
	{
		return value.clone();
	}
	
	
	/**
	 * Returns one of the values composing the observation.
	 *
	 * @param i The dimension of interest (0 &le; i &lt; dimension).
	 * @return The value of the (i+1)-th dimension of this observation.
	 */
	public double value(int i)
	{
		return value[i];
	}
	
	
	/**
	 * Returns the centroid matching this observation.
	 *
	 * @return The corresponding observation.
	 */
	public Centroid<ObservationVector> factor()
	{
		return new CentroidObservationVector(this);
	}
	
	
	/**
	 * Returns a new observation that is the sum of this observation
	 * and another one.
	 *
	 * @param o The observation to sum with this one.
	 * @return An {@link ObservationVector ObservationVector} which is the
	 *         sum of this observation and <code>o</code>.
	 */
	public ObservationVector plus(ObservationVector o)
	{
		if (dimension() != o.dimension())
			throw new IllegalArgumentException();
		
		ObservationVector s = new ObservationVector(dimension());
		for (int i = 0; i < dimension(); i++)
			s.value[i] = value[i] + o.value[i];
		
		return s;
	}
	
	
	/**
	 * Returns a new observation that is the product of this observation
	 * by a scalar.
	 *
	 * @param c A scalar value.
	 * @return An {@link ObservationVector ObservationVector} which is the
	 *         product of this observation and <code>c</code>.
	 */
	public ObservationVector times(double c)
	{
		ObservationVector p = (ObservationVector) clone();;
		
		for (int i = 0; i < dimension(); i++)
			p.value[i] *= c;
		
		return p;
	}
	
	
	/**
	 * Returns a new observation that is the difference between this observation
	 * and another one.
	 *
	 * @param o The observation to subtract from this one.
	 * @return An {@link ObservationVector ObservationVector} which is the
	 *         difference between this observation and <code>o</code>.
	 */
	public ObservationVector minus(ObservationVector o)
	{
		if (dimension() != o.dimension())
			throw new IllegalArgumentException();
		
		ObservationVector d = new ObservationVector(dimension());
		for (int i = 0; i < dimension(); i++)
			d.value[i] = value[i] - o.value[i];
		
		return d;
	}
	
	
	public String toString(NumberFormat numberFormat)
	{
		String s = "[";
		
		for (int i = 0; i < value.length; i++)
			s += " " + numberFormat.format(value[i]);
		
		return s + " ]";
	}
	
	
	public ObservationVector clone()
	{
		return new ObservationVector(value);
	}
}
