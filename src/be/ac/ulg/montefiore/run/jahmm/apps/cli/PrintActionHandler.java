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


/**
 * Creates a Hmm and writes it to file.
 */
class PrintActionHandler extends ActionHandler
{
	@SuppressWarnings({"unchecked"}) // We use a generic reader 
	public void act()
	throws FileFormatException, IOException, FileNotFoundException,
	AbnormalTerminationException
	{
		EnumSet<Arguments> args = EnumSet.of(Arguments.IN_HMM);
		CommandLineArguments.checkArgs(args);
		
		InputStream in = Arguments.IN_HMM.getAsInputStream();
		OpdfReader opdfReader = new OpdfGenericReader();
		Hmm<?> hmm = HmmReader.read(new InputStreamReader(in), opdfReader);
		
		System.out.println(hmm);
	}
}
