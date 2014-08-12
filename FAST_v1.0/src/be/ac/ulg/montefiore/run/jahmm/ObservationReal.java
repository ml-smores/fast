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

import java.text.*;


/**
 * This class implements observations made of a real value.
 */
public class ObservationReal extends Observation
implements CentroidFactory<ObservationReal>
{
	
	/**
	 * The observation's value.
	 */
	final public double value;
	
	
	/**
	 * An observation that can be described by a real.
	 *
	 * @param value The value of this observation.
	 */
	public ObservationReal(double value)
	{
		this.value = value;
	}
	
	
	/**
	 * Returns the centroid matching this observation.
	 *
	 * @return The corresponding observation.
	 */
	public Centroid<ObservationReal> factor()
	{
		return new CentroidObservationReal(this);
	}
	
	
	public String toString(NumberFormat numberFormat)
	{
		return numberFormat.format(value);
	}
}
