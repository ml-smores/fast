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

import java.util.*;
import java.io.*;

import be.ac.ulg.montefiore.run.jahmm.*;


/**
 * This class can write a set of observation sequences to a
 * {@link java.io.Writer Writer}.
 * <p>
 * The sequences written using this file can be read using the
 * {@link ObservationSequencesReader ObservationSequencesReader} class.
 */
public class ObservationSequencesWriter
{
    /**
     * Writes a set of sequences to file.
     *
     * @param writer The writer to write to. It should use the "US-ASCII"
     *               character set.
     * @param ow The observation writer used to generate the observations. 
     * @param sequences The set of observation sequences.
     */
	static public <O extends Observation> void 
	write(Writer writer, ObservationWriter<? super O> ow,
			List<? extends List<O>> sequences)
	throws IOException
	{
		for (List<O> s : sequences)
			write(s, ow, writer);
	}
	
	
	/* 
	 * Writes the sequence 'sequence' to the writer 'writer' using the
	 * observation writer 'ow'.
	 */
	static <O extends Observation> void
	write(List<O> sequence, ObservationWriter<? super O> ow, Writer writer) 
	throws IOException
	{
		for (O o : sequence) 
			ow.write(o, writer);
		
		writer.write("\n");
    }
}
