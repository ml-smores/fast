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
import java.util.*;

import be.ac.ulg.montefiore.run.jahmm.Observation;


/**
 * This class can read observations sequences from file.
 * <p>
 * The file format has been chosen to be very simple:
 * <ul>
 * <li> a line per observation sequence, in pure 7 bits ASCII;</li>
 * <li> empty (white) lines, space and tab characters are not significant;</li>
 * <li> each observation is followed by a semi-colon
 *      (<i>i.e.</i> the line ends with a semi-colon);</li>
 * <li> The '#' character introduce a comment; the rest of the line is
 *      skipped; </li>
 * <li> A newline can be escaped using the '\' character; this character can't
 *      be used in any other context;</li>
 * <li> the format of each observation is defined by the corresponding
 *      IO class.</li>
 * </ul>
 * <p>
 * Those rules must be followed by {@link ObservationReader ObservationReader} 
 * subclasses.
 */
public class ObservationSequencesReader
{	
	/**
	 * Reads observation sequences file.  Such a file holds a set of observation
	 * sequences.
	 *
	 * @param or An observation reader.
	 * @param reader Holds the character stream reader the sequences are read 
	 *               from.
	 * @return A {@link java.util.Vector Vector} of 
	 *         {@link java.util.Vector Vector}s of
	 *         {@link be.ac.ulg.montefiore.run.jahmm.Observation Observation}s.
	 */
	static public <O extends Observation> List<List<O>>
	readSequences(ObservationReader<O> or, Reader reader)
	throws IOException, FileFormatException
	{
		List<List<O>> sequences = new ArrayList<List<O>>();
		StreamTokenizer st = new StreamTokenizer(reader);
		
		initSyntaxTable(st);
		
		for (st.nextToken(); st.ttype != StreamTokenizer.TT_EOF; 
		st.nextToken()) {
			st.pushBack();
			List<O> sequence = new ArrayList<O>(readSequence(or, st));
			
			if (sequence == null)
				break;
			
			sequences.add(sequence);
		}
		
		return sequences;
	}
	
	
	/* Initialize the syntax table of a stream tokenizer */
	static void initSyntaxTable(StreamTokenizer st)
	{
		st.resetSyntax();
		st.parseNumbers();
		st.whitespaceChars(0, (int) ' ');
		st.eolIsSignificant(true);
		st.commentChar((int) '#');
	}
	
	
	/**
	 * Reads an observation sequence out of a file {@link java.io.Reader
	 * Reader}. 
	 *
	 * @param oir An observation reader.
	 * @param reader Holds the character reader the sequences are read from.
	 * @return An observation sequence read from <code>st</code> or null if the
	 *         end of the file is reached before any sequence is found.
	 */
	static public <O extends Observation> List<O> 
	readSequence(ObservationReader<O> oir, Reader reader) 
	throws IOException, FileFormatException
	{	
		StreamTokenizer st = new StreamTokenizer(reader);
		initSyntaxTable(st);
		
		return readSequence(oir, st);
	}
	
	
	/*
	 * Reads an observation sequence out of a {@link java.io.StreamTokenizer
	 * StreamTokenizer}.  Empty lines or comments can appear before the
	 * sequence itself. <code>st</code>'s syntax table must be properly
	 * initialized.
	 */
	static <O extends Observation> List<O>
	readSequence(ObservationReader<O> oir, StreamTokenizer st) 
	throws IOException, FileFormatException
	{	
		for (st.nextToken(); st.ttype == StreamTokenizer.TT_EOL;
		st.nextToken());
		if (st.ttype == StreamTokenizer.TT_EOF)
			return null;
		
		List<O> sequence = new ArrayList<O>();
		
		do {
			st.pushBack();
			sequence.add(oir.read(st));
			
			if (st.nextToken() == '\\') { /* New lines can be escaped by '\' */
				if (st.nextToken() != StreamTokenizer.TT_EOL)
					throw new FileFormatException("'\' token is not followed " +
					"by a new line");
				st.nextToken();
			}
		} while (st.ttype != StreamTokenizer.TT_EOL &&
				st.ttype != StreamTokenizer.TT_EOF);
		
		if (st.ttype == StreamTokenizer.TT_EOF)
			throw new FileFormatException("Unexpected token: EOF"); 
		
		return sequence;
	}
}
