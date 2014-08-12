package be.ac.ulg.montefiore.run.jahmm.io;

import java.io.IOException;
import java.io.StreamTokenizer;

import be.ac.ulg.montefiore.run.jahmm.Opdf;

public class OpdfGenericReader
extends OpdfReader<Opdf<?>>
{
	String keyword()
	{
		throw new AssertionError("Cannot call method");
	}
	
	
	public Opdf<?> read(StreamTokenizer st)
	throws IOException, FileFormatException
	{
		if (st.nextToken() != StreamTokenizer.TT_WORD)
			throw new FileFormatException("Keyword expected");
		
		for (OpdfReader r : new OpdfReader[] {
				new OpdfIntegerReader(),
				new OpdfGaussianReader(),
				new OpdfGaussianMixtureReader(),
				new OpdfMultiGaussianReader() })
			if (r.keyword().equals(st.sval)) {
				st.pushBack();
				return r.read(st);
			}
		
		throw new FileFormatException("Unknown distribution");
	}
}
