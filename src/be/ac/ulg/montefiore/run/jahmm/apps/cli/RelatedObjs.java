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

import java.io.IOException;
import java.io.Reader;
import java.util.List;

import be.ac.ulg.montefiore.run.jahmm.*;
import be.ac.ulg.montefiore.run.jahmm.io.*;
import be.ac.ulg.montefiore.run.jahmm.toolbox.MarkovGenerator;


/**
 * This class collects all the objects related to a specific observation
 * type.
 */
public interface RelatedObjs<O extends Observation & CentroidFactory<O>>
{
	public ObservationReader<O> observationReader();
	public ObservationWriter<O> observationWriter();
	public OpdfFactory<? extends Opdf<O>> opdfFactory();
	public OpdfReader<? extends Opdf<O>> opdfReader();
	public OpdfWriter<? extends Opdf<O>> opdfWriter();
	public List<List<O>> readSequences(Reader reader)
	throws FileFormatException, IOException;
	public MarkovGenerator<O> generator(Hmm<O> hmm);
}