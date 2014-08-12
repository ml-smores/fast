package be.ac.ulg.montefiore.run.jahmm.test;

import java.io.*;

import junit.framework.TestCase;
import be.ac.ulg.montefiore.run.jahmm.apps.cli.AbnormalTerminationException;
import be.ac.ulg.montefiore.run.jahmm.apps.cli.Cli;


public class CliTest extends TestCase
{
	private InputStream origIn;
	private PrintStream origOut;
	private PrintStream origErr;
	
	
	protected void setUp()
	{
		origIn = System.in;
		origOut = System.out;
		origErr = System.err;
	}
	
	
	public void testCli()
	throws IOException
	{
		ByteArrayOutputStream err = new ByteArrayOutputStream();
		System.setErr(new PrintStream(err));
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		System.setOut(new PrintStream(out));
		
		try {
			help();
			flush(out, err);
			
			// Creates a HMM
			create();
			String hmm = new String(out.toByteArray());
			flush(out, err);
			
			// Prints the HMM created above
			ByteArrayInputStream in = new ByteArrayInputStream(hmm.getBytes());
			System.setIn(in);
			print();
			flush(out, err);
		}
		catch(AbnormalTerminationException e) {
			 throw new AssertionError("Unexpected exception: " + e);
		}
		
		System.setOut(origOut);
		System.setErr(origErr);
		System.setIn(origIn);
	}
	
	
	protected void flush(ByteArrayOutputStream out, ByteArrayOutputStream err)
	{
		assertEquals("Something has been written on the \"standard\"" +
				" error stream ('" + err.toString() + "')",
				err.toString().length(), 0);
		
		out.reset();
		err.reset();
	}
	
	
	protected void help()
	throws IOException, AbnormalTerminationException
	{
		Cli.run("-help");
	}
	
	
	protected void create()
	throws IOException, AbnormalTerminationException
	{
		Cli.run("create", "-opdf", "integer", "-r", "4", "-n", "3", "-o", "-");
	}
	
	
	protected void print()
	throws IOException, AbnormalTerminationException
	{
		Cli.run("print", "-i", "-");
	}
}
