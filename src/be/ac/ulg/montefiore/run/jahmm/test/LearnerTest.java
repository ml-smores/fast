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
import be.ac.ulg.montefiore.run.jahmm.learn.*;
import be.ac.ulg.montefiore.run.jahmm.toolbox.KullbackLeiblerDistanceCalculator;
import be.ac.ulg.montefiore.run.jahmm.toolbox.MarkovGenerator;


public class LearnerTest
extends TestCase
{	
	final static private double DELTA = 5.E-3;
	
	private Hmm<ObservationInteger> hmm;
	private List<List<ObservationInteger>> sequences;
	private KullbackLeiblerDistanceCalculator klc;
	
	
	protected void setUp() 
	{ 
		hmm = new Hmm<ObservationInteger>(3, new OpdfIntegerFactory(10));
		hmm.getOpdf(0).fit(new ObservationInteger(1), new ObservationInteger(2));
		
		MarkovGenerator<ObservationInteger> mg =
			new MarkovGenerator<ObservationInteger>(hmm);
		
		sequences = new ArrayList<List<ObservationInteger>>();		
		for (int i = 0; i < 100; i++)
			sequences.add(mg.observationSequence(100));
		
		klc = new KullbackLeiblerDistanceCalculator();
	}
	
	
	public void testBaumWelch()
	{
		/* Model sequences using BW algorithm */
		
		BaumWelchLearner bwl = new BaumWelchLearner();

		Hmm<ObservationInteger> bwHmm = bwl.learn(hmm, sequences);
		
		assertEquals(0., klc.distance(bwHmm, hmm), DELTA);
		
		/* Model sequences using the scaled BW algorithm */
		
		BaumWelchScaledLearner bwsl = new BaumWelchScaledLearner();
		bwHmm = bwsl.learn(hmm, sequences);

		assertEquals(0., klc.distance(bwHmm, hmm), DELTA);
	}
	
	
	public void testKMeans()
	{
		KMeansLearner<ObservationInteger> kml =
			new KMeansLearner<ObservationInteger>(5,
					new OpdfIntegerFactory(10), sequences);
		assertEquals(0., klc.distance(kml.learn(), hmm), DELTA);
	}
}
