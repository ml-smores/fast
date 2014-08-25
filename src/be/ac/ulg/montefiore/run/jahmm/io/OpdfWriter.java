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

import java.io.*;
import java.text.DecimalFormat;

import be.ac.ulg.montefiore.run.jahmm.Opdf;


/**
 * Writes an observation distribution textual description.
 */
public abstract class OpdfWriter<O extends Opdf<?>>
{
	/**
	 * Writes a textual description of a given
	 * {@link be.ac.ulg.montefiore.run.jahmm.Opdf Opdf} compatible
	 * with the corresponding {@link OpdfReader}.
	 *
	 * @param writer The writer where the description is output.
	 * @param opdf An observation distribution.
	 */
	public abstract void write(Writer writer, O opdf)
	throws IOException;
	
	
	/**
	 * Writes a sequence of numbers.  This method is compatible with 
	 * {@link OpdfReader#read(StreamTokenizer, int)}.
	 * 
	 * @param writer Where to read the sequence to.
	 * @param array The array to write.
	 */
	protected void write(Writer writer, double[] array)
	throws IOException
	{
		DecimalFormat formatter = new DecimalFormat();
		
		writer.write("[");
		
		for (int i = 0; i < array.length; i++)
			writer.write(" " + formatter.format(array[i]));
		
		writer.write(" ]");
	}
}
