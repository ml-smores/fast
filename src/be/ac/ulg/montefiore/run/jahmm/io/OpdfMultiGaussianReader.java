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
import java.io.StreamTokenizer;

import be.ac.ulg.montefiore.run.jahmm.OpdfMultiGaussian;

/**
 * This class implements a {@link OpdfMultiGaussian} reader.  The syntax of the
 * distribution description is the following.
 * <p>
 * The description always begins with the keyword <tt>MultiGaussianOPDF</tt>.
 * The next (resp. last) symbol is an opening (resp. closing) bracket.
 * Between the backets are two series of numbers between brackets and separated
 * by a space.
 * <p>
 * The first describes the distribution's mean vector; each number
 * is the corresponding vector element, from top to bottom.
 * <p>
 * The second describes the covariance matrix; it is given line by line, from
 * top to bottom. Each line is represented by the values of its elements, from
 * left to right, separated by a space and between brackets.  
 * <p>
 * For example, reading<br>
 * <tt>MultiGaussianOPDF [ [ 5. 5. ] [ [ 1.2 .3 ] [ .3 4. ] ] ]</tt>
 * returns a distribution equivalent to<br>
 * <code>new OpdfMultiGaussian(new double[] { 5., 5. },
 *       new double[][] { { 1.2, .3 }, { .3, 4. } })</code>.
 */
public class OpdfMultiGaussianReader
extends OpdfReader<OpdfMultiGaussian>
{
	String keyword()
	{
		return "MultiGaussianOPDF";
	}

	
	public OpdfMultiGaussian read(StreamTokenizer st)
	throws IOException, FileFormatException
	{
		HmmReader.readWords(st, keyword(), "[");
				
		double[] means = OpdfReader.read(st, -1);		
		double[][] covariance = new double[means.length][];
		
		HmmReader.readWords(st, "[");
		for (int l = 0; l < covariance.length; l++)
			covariance[l] = OpdfReader.read(st, means.length);
		HmmReader.readWords(st, "]");
		
		return new OpdfMultiGaussian(means, covariance);
	}
}
