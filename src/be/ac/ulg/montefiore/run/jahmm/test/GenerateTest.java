package be.ac.ulg.montefiore.run.jahmm.test;

import java.io.IOException;

import junit.framework.TestCase;
import be.ac.ulg.montefiore.run.jahmm.*;
import be.ac.ulg.montefiore.run.jahmm.draw.GenericHmmDrawerDot;


public class GenerateTest 
extends TestCase
{	
	public final static String outputDir = "";
	
	private Hmm<ObservationInteger> hmm;

	
	protected void setUp()
	{
		hmm = new Hmm<ObservationInteger>(4, new OpdfIntegerFactory(2));
	}
	
	
	public void testDotGenerator()
	{	
		GenericHmmDrawerDot hmmDrawer = new GenericHmmDrawerDot();
		
		try {
			hmmDrawer.write(hmm, outputDir + "hmm-generate.dot");
		}
		catch (IOException e) {
			assertTrue("Writing file triggered an exception: " + e, false);
		}
	}
}
