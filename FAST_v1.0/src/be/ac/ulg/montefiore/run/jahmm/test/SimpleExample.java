/* jahmm package - v0.6.1 */

package be.ac.ulg.montefiore.run.jahmm.test;


/*
 * SimpleExample.java: A simple example file for the jahmm package.
 *
 * Written by Jean-Marc Francois <francois-jahmm@run.montefiore.ulg.ac.be>
 *
 * The content of this file is public-domain.
 *
 * Compile with the following command:
 *      javac SimpleExample.java
 * And run with:
 *      java SimpleExample
 *
 * This file (or a newer version) can be found at this URL:
 *      http://www.run.montefiore.ulg.ac.be/~francois/software/jahmm/example/
 *
 *
 * Changelog:
 * 2004-03-01: Creation. (JMF)
 * 2004-04-27: Adapted to Jahmm 0.2.4. (JMF)
 * 2005-01-31: Minor adaption for release 0.3.0. (JMF)
 * 2005-11-25: Adapted to Jahmm 0.5.0. (JMF)
 * 2006-01-11: Now prints the initial/learnt HMMs. (JMF)
 * 2006-02-05: Small modification to avoid 'unchecked casting'. (JMF)
 * 2006-02-05: Renamed, adapted to v0.6.0. (JMF)
 */

import java.util.*;

import be.ac.ulg.montefiore.run.jahmm.*;
import be.ac.ulg.montefiore.run.jahmm.draw.GenericHmmDrawerDot;
import be.ac.ulg.montefiore.run.jahmm.learn.BaumWelchLearner;
import be.ac.ulg.montefiore.run.jahmm.toolbox.KullbackLeiblerDistanceCalculator;
import be.ac.ulg.montefiore.run.jahmm.toolbox.MarkovGenerator;


/**
 * This class demonstrates how to build a HMM with known parameters, how to
 * generate a sequence of observations given a HMM, how to learn the parameters
 * of a HMM given observation sequences, how to compute the probability of an
 * observation sequence given a HMM and how to convert a HMM to a Postscript
 * drawing.
 * <p>
 * The example used is that of a wireless computer network that can experience
 * jamming.  When the wireless medium is (resp. is not) jammed, a lot (resp.
 * few) packets are lost.  Thus, the HMMs built here have two states
 * (jammed/not jammed).
 */
public class SimpleExample
{	
	/* Possible packet reception status */
	
	public enum Packet {
		OK, LOSS;
		
		public ObservationDiscrete<Packet> observation() {
			return new ObservationDiscrete<Packet>(this);
		}
	};
	
	
	static public void main(String[] argv) 
	throws java.io.IOException
	{	
		/* Build a HMM and generate observation sequences using this HMM */
		
		Hmm<ObservationDiscrete<Packet>> hmm = buildHmm();
		
		List<List<ObservationDiscrete<Packet>>> sequences;
		sequences = generateSequences(hmm);
		
		/* Baum-Welch learning */
		
		BaumWelchLearner bwl = new BaumWelchLearner();
		
		Hmm<ObservationDiscrete<Packet>> learntHmm = buildInitHmm();
		
		// This object measures the distance between two HMMs
		KullbackLeiblerDistanceCalculator klc = 
			new KullbackLeiblerDistanceCalculator();
		
		// Incrementally improve the solution
		for (int i = 0; i < 10; i++) {
			System.out.println("Distance at iteration " + i + ": " +
					klc.distance(learntHmm, hmm));
			learntHmm = bwl.iterate(learntHmm, sequences);
		}
		
		System.out.println("Resulting HMM:\n" + learntHmm);
		
		/* Computing the probability of a sequence */
		
		ObservationDiscrete<Packet> packetOk = Packet.OK.observation();
		ObservationDiscrete<Packet> packetLoss = Packet.LOSS.observation();
		
		List<ObservationDiscrete<Packet>> testSequence = 
			new ArrayList<ObservationDiscrete<Packet>>(); 
		testSequence.add(packetOk);
		testSequence.add(packetOk);
		testSequence.add(packetLoss);
		
		System.out.println("Sequence probability: " +
				learntHmm.probability(testSequence));
		
		/* Write the final result to a 'dot' (graphviz) file. */
		
		(new GenericHmmDrawerDot()).write(learntHmm, "learntHmm.dot");
	}
	
	
	/* The HMM this example is based on */
	
	static Hmm<ObservationDiscrete<Packet>> buildHmm()
	{	
		Hmm<ObservationDiscrete<Packet>> hmm = 
			new Hmm<ObservationDiscrete<Packet>>(2,
					new OpdfDiscreteFactory<Packet>(Packet.class));
		
		hmm.setPi(0, 0.95);
		hmm.setPi(1, 0.05);
		
		hmm.setOpdf(0, new OpdfDiscrete<Packet>(Packet.class, 
				new double[] { 0.95, 0.05 }));
		hmm.setOpdf(1, new OpdfDiscrete<Packet>(Packet.class,
				new double[] { 0.20, 0.80 }));
		
		hmm.setAij(0, 1, 0.05);
		hmm.setAij(0, 0, 0.95);
		hmm.setAij(1, 0, 0.10);
		hmm.setAij(1, 1, 0.90);
		
		return hmm;
	}
	
	
	/* Initial guess for the Baum-Welch algorithm */
	
	static Hmm<ObservationDiscrete<Packet>> buildInitHmm()
	{	
		Hmm<ObservationDiscrete<Packet>> hmm = 
			new Hmm<ObservationDiscrete<Packet>>(2,
					new OpdfDiscreteFactory<Packet>(Packet.class));
		
		hmm.setPi(0, 0.50);
		hmm.setPi(1, 0.50);
		
		hmm.setOpdf(0, new OpdfDiscrete<Packet>(Packet.class,
				new double[] { 0.8, 0.2 }));
		hmm.setOpdf(1, new OpdfDiscrete<Packet>(Packet.class,
				new double[] { 0.1, 0.9 }));
		
		hmm.setAij(0, 1, 0.2);
		hmm.setAij(0, 0, 0.8);
		hmm.setAij(1, 0, 0.2);
		hmm.setAij(1, 1, 0.8);
		
		return hmm;
	}
	
	
	/* Generate several observation sequences using a HMM */
	
	static <O extends Observation> List<List<O>> 
	generateSequences(Hmm<O> hmm)
	{
		MarkovGenerator<O> mg = new MarkovGenerator<O>(hmm);
		
		List<List<O>> sequences = new ArrayList<List<O>>();
		for (int i = 0; i < 200; i++)
			sequences.add(mg.observationSequence(100));

		return sequences;
	}
}
