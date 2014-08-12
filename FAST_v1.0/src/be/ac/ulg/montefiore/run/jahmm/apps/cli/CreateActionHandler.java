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
import be.ac.ulg.montefiore.run.jahmm.io.HmmWriter;
import be.ac.ulg.montefiore.run.jahmm.io.OpdfWriter;


/**
 * Creates a Hmm and write it to file.
 */
class CreateActionHandler
extends ActionHandler
{	
	public void act()
	throws FileNotFoundException, IOException, AbnormalTerminationException
	{
		EnumSet<Arguments> args = EnumSet.of(
				Arguments.OPDF,
				Arguments.NB_STATES,
				Arguments.OUT_HMM);
		CommandLineArguments.checkArgs(args);
		
		int nbStates = Arguments.NB_STATES.getAsInt();
		OutputStream outStream = Arguments.OUT_HMM.getAsOutputStream();
		Writer outWriter = new OutputStreamWriter(outStream);
		
		write(outWriter, nbStates, Types.relatedObjs());
		
		outWriter.flush();
	}
	
	
	private <O extends Observation & CentroidFactory<O>> void
	write(Writer outWriter, int nbStates, RelatedObjs<O> relatedObjs)
	throws IOException
	{
		OpdfFactory<? extends Opdf<O>> opdfFactory = relatedObjs.opdfFactory();
		OpdfWriter<? extends Opdf<O>> opdfWriter = relatedObjs.opdfWriter();
		
		Hmm<O> hmm = new Hmm<O>(nbStates, opdfFactory);
		
		HmmWriter.write(outWriter, opdfWriter, hmm);
	}
}
