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

package be.ac.ulg.montefiore.run.jahmm.io;

import java.io.IOException;
import java.io.Writer;
import java.text.DecimalFormat;

import be.ac.ulg.montefiore.run.jahmm.*;

/**
 * Writes a HMM to a text file compatible with {@link HmmReader}.
 */
public class HmmWriter
{
	/**
	 * Writes a HMM description.
	 * 
	 * @param writer The writer to write the HMM to.
	 * @param opdfWriter The writer used to convert the observation's
	 *        distributions of the HMMs.
	 * @param hmm The HMM to write.
	 */
	static public <O extends Observation> void 
	write(Writer writer, OpdfWriter<? extends Opdf<O>> opdfWriter, Hmm<O> hmm)
	throws IOException
	{
    	writer.write("Hmm v1.0\n\nNbStates " + hmm.nbStates() + "\n\n");
    	
    	for (int i = 0; i < hmm.nbStates(); i++)
    		writeState(writer, opdfWriter, hmm, i);
	}
    
    
	@SuppressWarnings("unchecked") // Cannot guarantee type safety
	static private <O extends Observation, D extends Opdf<O>> void 
	writeState(Writer writer, OpdfWriter<D> opdfWriter,
			Hmm<O> hmm, int stateNb)
    throws IOException
    {
		DecimalFormat formatter = new DecimalFormat();
		
    	writer.write("State\nPi " + formatter.format(hmm.getPi(stateNb)));
    	
    	writer.write("\nA ");
    	for (int i = 0; i < hmm.nbStates(); i++)
    		writer.write(formatter.format(hmm.getAij(stateNb, i)) + " ");
    	writer.write("\n");
    	
    	D opdf = (D) hmm.getOpdf(stateNb);
    	opdfWriter.write(writer, opdf);
    	writer.write("\n\n");
    }
}
