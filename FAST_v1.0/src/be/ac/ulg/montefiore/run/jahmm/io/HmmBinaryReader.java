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

import be.ac.ulg.montefiore.run.jahmm.Hmm;


/**
 * This class can read Hidden Markov Models from a byte stream.
 * <p>
 * The HMM objects are simply deserialized.  HMMs could thus be unreadable using
 * a different release of this library.
 */
public class HmmBinaryReader
{	
	/**
	 * Reads a HMM from a byte stream.
	 *
	 * @param stream Holds the byte stream the HMM is read from.
	 * @return The {@link be.ac.ulg.montefiore.run.jahmm.Hmm HMM} read.
	 */
	static public Hmm<?> read(InputStream stream)
	throws IOException
	{		
		ObjectInputStream ois = new ObjectInputStream(stream);
		
		try {
			return (Hmm) ois.readObject();
		}
		catch(ClassNotFoundException e) {
			throw new RuntimeException(e);
		}
	}
}
