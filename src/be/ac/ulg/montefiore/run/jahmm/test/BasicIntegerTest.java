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

package be.ac.ulg.montefiore.run.jahmm.test;

import java.util.ArrayList;
import java.util.List;

import junit.framework.TestCase;
import be.ac.ulg.montefiore.run.jahmm.*;


public class BasicIntegerTest 
extends TestCase
{
	final static private double DELTA = 1.E-10;
	
	private Hmm<ObservationInteger> hmm;
	private List<ObservationInteger> sequence;
	private List<ObservationInteger> randomSequence;
	
	
	protected void setUp()
	{ 
		hmm = new Hmm<ObservationInteger>(5, new OpdfIntegerFactory(10));
		hmm.setOpdf(1, new OpdfInteger(6));
		
		sequence = new ArrayList<ObservationInteger>();
		for (int i = 0; i < 5; i++)
			sequence.add(new ObservationInteger(i));
		
		randomSequence = new ArrayList<ObservationInteger>();
		for (int i = 0; i < 30000; i++)
			randomSequence.
			add(new ObservationInteger((int) (Math.random()*10.)));
	}
	
	
	public void testForwardBackward()
	{	
		ForwardBackwardCalculator fbc = 
			new ForwardBackwardCalculator(sequence, hmm);
		
		assertEquals(1.8697705349794245E-5, fbc.probability(), DELTA);
		
		ForwardBackwardScaledCalculator fbsc =
			new ForwardBackwardScaledCalculator(sequence, hmm);
		
		assertEquals(1.8697705349794245E-5, fbsc.probability(), DELTA);
	}
	
	
	public void testViterbi()
	{	
		ViterbiCalculator vc = new ViterbiCalculator(sequence, hmm);
		
		assertEquals(4.1152263374485705E-8, 
				Math.exp(vc.lnProbability()), DELTA);
	}
	
	
	public void testKMeansCalculator()
	{	
		int nbClusters = 20;
		
		KMeansCalculator<ObservationInteger> kmc = new
		KMeansCalculator<ObservationInteger>(nbClusters, randomSequence);
		
		assertEquals("KMeans did not produce expected number of clusters",
				nbClusters, kmc.nbClusters());
	}
}
