/* jahmm package - v0.6.1 */

/*
 *  *  Copyright (c) 2004-2006, Jean-Marc Francois.
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

package be.ac.ulg.montefiore.run.jahmm.apps.cli;

import java.io.*;
import java.util.EnumSet;

import be.ac.ulg.montefiore.run.jahmm.*;
import be.ac.ulg.montefiore.run.jahmm.apps.cli.CommandLineArguments.Arguments;
import be.ac.ulg.montefiore.run.jahmm.io.*;
import be.ac.ulg.montefiore.run.jahmm.toolbox.KullbackLeiblerDistanceCalculator;

/**
 * This class implements an action that computes the Kullback-Leibler
 * distance between two HMMs.
 */
public class KLActionHandler extends ActionHandler
{
	public void act() throws FileNotFoundException, IOException,
	FileFormatException, AbnormalTerminationException
	{
		EnumSet<Arguments> args = EnumSet.of(
				Arguments.OPDF,
				Arguments.IN_HMM,
				Arguments.IN_KL_HMM);
		CommandLineArguments.checkArgs(args);
		
		InputStream st = Arguments.IN_KL_HMM.getAsInputStream();
		Reader reader1 = new InputStreamReader(st);
		st = Arguments.IN_HMM.getAsInputStream();
		Reader reader2 = new InputStreamReader(st);
		
		distance(Types.relatedObjs(), reader1, reader2);
	}
	
	
	private <O extends Observation & CentroidFactory<O>> void
	distance(RelatedObjs<O> relatedObjs, Reader reader1, Reader reader2)
	throws IOException, FileFormatException
	{
		Hmm<O> hmm1 = HmmReader.read(reader1, relatedObjs.opdfReader());
		Hmm<O> hmm2 = HmmReader.read(reader2, relatedObjs.opdfReader());
		
		KullbackLeiblerDistanceCalculator kl = 
			new KullbackLeiblerDistanceCalculator();
		System.out.println(kl.distance(hmm1, hmm2));
	}
}
