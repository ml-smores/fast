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
 * The centroid (basically, the mean) of a cluster.  Used by the k-means
 * algorithm.
 */
public interface Centroid<O>
{    
    /**
     * Reevalue the value of this centroid, knowing that it currently is the
     * centroid of the observations composing <code>v</code> and that we
     * want it to be the centroid of <code>v</code> plus the value of the
     * observation <code>e</code>.
     *
     * @param e An observation that must be involved in the computation
     *          of the new value of this centroid.
     * @param v The set of observations that gave the current value of this 
     *          centroid.
     */
    public void reevaluateAdd(O e, List<? extends O> v);
    
    
    /**
     * Reevalue the value of this centroid, knowing that it currently is the
     * centroid of the observations composing <code>v</code> and that we want 
     * it to be the centroid of <code>v</code> minus the value of the
     * observation <code>e</code>.
     *
     * @param e An observation that must not be involved in the computation
     *          of the new value of this centroid.
     * @param v The set of observations (which holds <code>o</code>) that gave
     *          the current value of this centroid.
     */
    public void reevaluateRemove(O e, List<? extends O> v);
    
    
    /**
     * Returns the distance from this centroid to a given element.
     *
     * @param e The element.
     * @return The distance to the centroid.
     */
    public double distance(O e);
}
