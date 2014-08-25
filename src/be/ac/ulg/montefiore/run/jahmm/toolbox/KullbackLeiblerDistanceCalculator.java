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

package be.ac.ulg.montefiore.run.jahmm.toolbox;

import java.util.List;

import be.ac.ulg.montefiore.run.jahmm.*;


/**
 * Computes the distance between HMMs.
 * <p>
 * The distance metric is similar to the Kullback-Leibler distance defined
 * on distributions.  More information can be found in
 * <i>A Probabilistic Distance Measure For HMMs</i> by
 * <i>Rabiner</i> and <i>Juang</i> (AT&T Technical Journal, vol. 64, Feb. 1985,
 * pages 391-408).
 * <p>
 * This distance measure is not symetric: <code>distance(hmm1, hmm2)</code>
 * is not necessary equal to <code>distance(hmm2, hmm1)</code>.  To get a
 * symetric distance definition, compute
 * <code>(distance(hmm1, hmm2) + distance(hmm2, hmm1)) / 2</code>.
 */
public class KullbackLeiblerDistanceCalculator
{	
	private int sequencesLength = 1000;
	private int nbSequences = 10;
	
	
	/**
	 * Computes the Kullback-Leibler distance between two HMMs.
	 *
	 * @param hmm1 The first HMM against which the distance is computed.
	 *             The distance is mesured with regard to this HMM (this must
	 *             be defined since the Kullback-Leibler distance is not
	 *             symetric).
	 * @param hmm2 The second HMM against which the distance is computed.
	 * @return The distance between <code>hmm1</code> and <code>hmm2</code> with
	 *      regard to <code>hmm1</code>
	 */
	public <O extends Observation> double 
	distance(Hmm<O> hmm1, Hmm<? super O> hmm2)
	{			
		double distance = 0.;
		
		for (int i = 0; i < nbSequences; i++) {
			
			List<O> oseq = new MarkovGenerator<O>(hmm1).
			observationSequence(sequencesLength);
			
			distance += (new ForwardBackwardScaledCalculator(oseq, hmm1).
					lnProbability() -
					new ForwardBackwardScaledCalculator(oseq, hmm2).
					lnProbability()) / sequencesLength;
		}
		
		return distance / nbSequences;
	}


	/**
	 * Returns the number of sequences generated to estimate a distance.
	 * 
	 * @return The number of generated sequences.
	 */
	public int getNbSequences()
	{
		return nbSequences;
	}


	/**
	 * Sets the number of sequences generated to estimate a distance.
	 * 
	 * @param nb The number of generated sequences.
	 */
	public void setNbSequences(int nb)
	{
		this.nbSequences = nb;
	}
	
	
	/**
	 * Returns the length of sequences generated to estimate a distance.
	 * 
	 * @return The sequences length.
	 */
	public int getSequencesLength()
	{
		return sequencesLength;
	}


	/**
	 * Sets the length of sequences generated to estimate a distance.
	 * 
	 * @param length The sequences length.
	 */
	public void setSequencesLength(int length)
	{
		this.sequencesLength = length;
	}
}
