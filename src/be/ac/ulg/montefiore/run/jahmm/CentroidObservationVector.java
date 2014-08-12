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

import java.util.*;


/**
 * This class represents the centroid of a set of {@link ObservationVector
 * ObservationVector}.
 */
public class CentroidObservationVector
implements Centroid<ObservationVector>
{	
	private ObservationVector value;
	
	
	/**
	 * Creates a new centroid that represents the mean value of a set of
	 * {@link ObservationVector ObservationVector}s.
	 *
	 * @param o The initial value of the new centroid.
	 */
	public CentroidObservationVector(ObservationVector o)
	{
		this.value = (ObservationVector) o.clone();
	} 
	
	
	public void reevaluateAdd(ObservationVector e,
			List<? extends ObservationVector> v)
	{
		double[] evalues = e.value;
		
		for (int i = 0; i < value.dimension(); i++)
			value.value[i] = 
				((value.value[i] * v.size()) + evalues[i]) / (v.size()+1);
	}
	
	
	public void reevaluateRemove(ObservationVector e, 
			List<? extends ObservationVector> v)
	{
		double[] evalues = e.value;
		
		for (int i = 0; i < value.dimension(); i++)
			value.value[i] = 
				((value.value[i] * v.size()) - evalues[i]) / (v.size()-1);
	}
	
	
	/**
	 * Returns the distance between this centroid and an element.  The
	 * distance metric is the euclidian distance.
	 *
	 * @param e The element, which must be an {@link ObservationVector
	 *          ObservationVector} with a dimension compatible with this
	 *          centroid.
	 * @return The distance between <code>element</code> and this centroid.
	 */
	public double distance(ObservationVector e)
	{
		ObservationVector diff = value.minus(e);
		double sum = 0.;
		
		for (int i = 0; i < diff.dimension(); i++)
			sum += diff.value[i] * diff.value[i];
		
		return Math.sqrt(sum);
	}
}
