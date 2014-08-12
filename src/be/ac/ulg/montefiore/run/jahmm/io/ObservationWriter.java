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

import be.ac.ulg.montefiore.run.jahmm.Observation;


/**
 * Writes an observation up to (and including) the semi-colon.
 * <p>
 * The syntax of each observation must be compatible with the corresponding
 * {@link ObservationReader ObservationReader}.
 */
public abstract class ObservationWriter<O extends Observation>
{	
	/**
	 * Writes an
	 * {@link be.ac.ulg.montefiore.run.jahmm.Observation Observation} (followed
	 * by a semi-colon) using a {@link java.io.Writer Writer}.
	 *
	 * @param observation The observation to write.
	 * @param writer The <code>writer</code> used to write the observations.
	 **/
	public abstract void write(O observation, Writer writer) 
	throws IOException;
}
