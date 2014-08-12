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
import java.util.List;

import be.ac.ulg.montefiore.run.jahmm.*;
import be.ac.ulg.montefiore.run.jahmm.apps.cli.CommandLineArguments.Arguments;
import be.ac.ulg.montefiore.run.jahmm.io.*;
import be.ac.ulg.montefiore.run.jahmm.learn.BaumWelchLearner;
import be.ac.ulg.montefiore.run.jahmm.learn.BaumWelchScaledLearner;


/**
 * Applies the Baum-Welch learning algorithm.
 */
class BWActionHandler
extends ActionHandler
{
	public void act()
	throws FileNotFoundException, IOException, FileFormatException, 
	AbnormalTerminationException
	{
		EnumSet<Arguments> args = EnumSet.of(
				Arguments.OPDF,
				Arguments.OUT_HMM,
				Arguments.IN_HMM,
				Arguments.IN_SEQ,
				Arguments.NB_ITERATIONS);
		CommandLineArguments.checkArgs(args);
		
		int nbIterations = Arguments.NB_ITERATIONS.getAsInt();
		OutputStream outStream = Arguments.OUT_HMM.getAsOutputStream();
		Writer hmmWriter = new OutputStreamWriter(outStream);
		InputStream hmmStream = Arguments.IN_HMM.getAsInputStream();
		InputStream seqStream = Arguments.IN_SEQ.getAsInputStream();
		Reader hmmReader = new InputStreamReader(hmmStream);
		Reader seqReader = new InputStreamReader(seqStream);
		
		learn(Types.relatedObjs(), hmmReader, seqReader, hmmWriter,
				nbIterations);
		
		hmmWriter.flush();
	}
	
	
	private <O extends Observation & CentroidFactory<O>> void
	learn(RelatedObjs<O> relatedObjs, Reader hmmFileReader,
			Reader seqFileReader, Writer hmmFileWriter,
			int nbIterations)
	throws IOException, FileFormatException
	{
		List<List<O>> seqs = relatedObjs.readSequences(seqFileReader);
		OpdfReader<? extends Opdf<O>> opdfReader = relatedObjs.opdfReader();
		OpdfWriter<? extends Opdf<O>> opdfWriter = relatedObjs.opdfWriter();
		
		Hmm<O> initHmm = HmmReader.read(hmmFileReader, opdfReader);
		BaumWelchLearner bw = new BaumWelchScaledLearner();
		bw.setNbIterations(nbIterations);
		Hmm<O> hmm = bw.learn(initHmm, seqs);
		HmmWriter.write(hmmFileWriter, opdfWriter, hmm);
	}
}
