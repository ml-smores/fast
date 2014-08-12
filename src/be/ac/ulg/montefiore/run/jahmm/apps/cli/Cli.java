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

import be.ac.ulg.montefiore.run.jahmm.io.FileFormatException;


/**
 * This class implements a command line interface for the Jahmm library.
 */
public class Cli
{
	/**
	 * The entry point of the CLI.
	 * 
	 * @param args Command line arguments.
	 */
	public static void main(String... args)
	throws IOException
	{
		try {
			System.exit(run(args));
		}
		catch (AbnormalTerminationException e) {
			System.err.println(e);
			System.exit(-1);
		}
	}
	
	
	static public int run(String... args)
	throws IOException, AbnormalTerminationException
	{
		// Allows this method to be called more than once
		CommandLineArguments.reset();
		
		ActionHandler.Actions action = CommandLineArguments.parseAction(args);
		if (action  == null)
			throw new WrongArgumentsException("Valid action required");
		
		ActionHandler actionHandler = null;
		
		try {
			actionHandler = action.handler().newInstance();
		} catch(Exception e) {
			throw new InternalError(e.toString());
		}
		
		actionHandler.parseArguments(args);
		
		try {
			actionHandler.act();
		} catch(FileNotFoundException e) {
			System.err.println(e);
			return -1;
		} catch(FileFormatException e) {
			System.err.println(e);
			return -1;
		}
		
		return 0;
	}
}			
