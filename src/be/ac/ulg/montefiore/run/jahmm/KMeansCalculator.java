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
 * This class can be used to divide a set of elements in clusters using
 * the k-means algorithm.
 * <p>
 * The algorithm used is just the plain old k-means algorithm as explained
 * in <i>Clustering and the Continuous k-Means Algorithm</i> (Vance Faber,
 * <i>Los Alamos Science</i> number 22).
 * <p>
 * In order to get the theoretical complexity, the list of elements to be
 * clustered must be accessible in O(1).
 */
public class KMeansCalculator<K extends CentroidFactory<? super K>>
{	
	private ArrayList<Cluster<K>> clusters;
	
	
	/**
	 * This class represents a cluster of elements.
	 */
	class Cluster<L extends CentroidFactory<? super L>>
	{
		private List<L> elements;
		private Centroid<? super L> centroid;
		
		
		/**
		 * Creates a new empty cluster.
		 */
		public Cluster()
		{
			elements = new ArrayList<L>();
			centroid = null;
		}
		
		
		/**
		 * Creates a new cluster composed of one element.
		 *
		 * @param element The element that compose the new cluster.
		 */
		public Cluster(L e)
		{
			elements = new ArrayList<L>();
			elements.add(e);
			centroid = e.factor();
		}
		
		
		/**
		 * Returns all the elements of this cluster.
		 *
		 * @return The elements of this cluster.
		 */
		public List<L> elements()
		{
			return elements;
		}
		
		
		public void add(L e)
		{
			if (centroid == null)
				centroid = e.factor();
			else
				centroid.reevaluateAdd(e, elements);
			
			elements.add(e);
		}
		
		
		public void remove(int i)
		{
			centroid.reevaluateRemove(elements.get(i), elements);
			elements.remove(i);
		}
		
		
		public Centroid<? super L> centroid()
		{
			return centroid;
		}
	}
	
	
	/**
	 * This class divides a set of elements in a given number of clusters.
	 *
	 * @param k The number of clusters to get.
	 * @param elements The elements to divide in clusters.
	 */
	public KMeansCalculator(int k, List<? extends K> elements)
	{
		if (k <= 0)
			throw new IllegalArgumentException("Illegal number of clusters");
		
		clusters = new ArrayList<Cluster<K>>(k);
		
		/* First, initialize clusters randomly */
		int clusterNb = 0;
		int elementNb = 0;
		
		elLoop:
			for (; elementNb < elements.size() && clusterNb < k &&
			elements.size() - elementNb > k - clusterNb; elementNb++) {
				K element = elements.get(elementNb);
				
				for (int i = 0; i < clusterNb; i++) {
					Cluster<K> cluster = clusters.get(i);
					
					if (cluster.centroid().distance(element) == 0.) {
						cluster.add(element);
						continue elLoop;
					}
				}
				
				clusters.add(new Cluster<K>(elements.get(elementNb)));
				clusterNb++;
			}
		
		for (; clusterNb < k && elementNb < elements.size(); 
		elementNb++, clusterNb++)
			clusters.add(new Cluster<K>(elements.get(elementNb)));
		
		for (; clusterNb < k; clusterNb++)
			clusters.add(new Cluster<K>());
		
		for (; elementNb < elements.size(); elementNb++) {
			K element = elements.get(elementNb);
			nearestCluster(element).add(element);
		}
		
		/* Then the k-means algorithm itself */
		boolean terminated;
		do {
			terminated = true;
			
			for (int i = 0; i < k; i++) {
				Cluster<K> thisCluster = clusters.get(i);
				List<K> thisElements = thisCluster.elements();
				
				for (int j = 0; j < thisElements.size(); j++) {
					K thisElement = thisElements.get(j);
					
					/* We don't move an element if it is the only one in
					 its cluster. */
					if (thisCluster.centroid().distance(thisElement) > 0.) {
						Cluster<K> nearestCluster = nearestCluster(thisElement);
						
						if (thisCluster != nearestCluster) {
							nearestCluster.add(thisElement);
							thisCluster.remove(j);
							terminated = false;
						}
					}
				}
			}
		} while (!terminated);
	}
	
	
	private Cluster<K> nearestCluster(K element)
	{
		double distance = Double.MAX_VALUE;
		Cluster<K> cluster = null;
		
		for (int i = 0; i < clusters.size(); i++) {
			double thisDistance = clusters.get(i).centroid().distance(element);
			
			if (distance > thisDistance) {
				distance = thisDistance;
				cluster = clusters.get(i);
			}
		}
		
		return cluster;
	}
	
	
	/**
	 * Returns the elements of one of the clusters.
	 *
	 * @param index The cluster index of the cluster your are interested in (the
	 *              first cluster has the index 0, while the last has the index
	 *              given by {@link #nbClusters nbClusters} <code>- 1</code>.
	 * @return A vector holding the elements of the requested cluster.
	 */
	public Collection<K> cluster(int index)
	{
		return clusters.get(index).elements();
	}
	
	
	/**
	 * Returns the number of clusters.
	 *
	 * @return The number of clusters in the cluster set computed by this class.
	 */
	public int nbClusters()
	{
		return clusters.size();
	}
}
