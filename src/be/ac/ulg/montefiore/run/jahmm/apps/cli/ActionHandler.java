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


import java.io.FileNotFoundException;
import java.io.IOException;

import be.ac.ulg.montefiore.run.jahmm.io.FileFormatException;

abstract class ActionHandler
{
	public static enum Actions {
		HELP("-help", HelpActionHandler.class),
		PRINT("print", PrintActionHandler.class),
		CREATE("create", CreateActionHandler.class),
		BW("learn-bw", BWActionHandler.class),
		KMEANS("learn-kmeans", KMeansActionHandler.class),
		GENERATE("generate", GenerateActionHandler.class),
		KL("distance-kl", KLActionHandler.class);
		
		private String argument;
		private Class<? extends ActionHandler> handler;
		
		Actions(String argument, Class<? extends ActionHandler> handler) {
			this.argument = argument;
			this.handler = handler;
		}
		
		public String toString() {
			return argument;
		}
		
		public Class<? extends ActionHandler> handler() {
			return handler;
		}
	};

	
	public void parseArguments(String args[])
	throws WrongArgumentsException
	{
		CommandLineArguments.parse(args);
	}

	
	abstract public void act()
	throws FileNotFoundException, IOException, FileFormatException,
	AbnormalTerminationException;
}
