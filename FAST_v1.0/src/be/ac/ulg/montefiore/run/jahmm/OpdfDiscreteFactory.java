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


/**
 * This class can build <code>OpdfInteger</code> observation probability
 * distribution functions.
 */
public class OpdfDiscreteFactory<E extends Enum<E>>
implements OpdfFactory<OpdfDiscrete<E>>
{	
	final protected Class<E> valuesClass;
	
	
	/**
	 * Creates a factory for {@link OpdfDiscrete OpdfDiscrete} objects.
	 * 
	 * @param valuesClass The class representing the set of values over which
	 *      the generated observation distributions operate.
	 */
	public OpdfDiscreteFactory(Class<E> valuesClass)
	{
		this.valuesClass = valuesClass;
	}
	
	
	public OpdfDiscrete<E> factor()
	{
		return new OpdfDiscrete<E>(valuesClass);
	}
}
