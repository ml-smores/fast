package hmm;


import java.util.List;

import be.ac.ulg.montefiore.run.jahmm.Hmm;
import be.ac.ulg.montefiore.run.jahmm.Observation;

import common.Matrix;


public class OnlineForwardBackward		
{
	public static class Response 
	{
		public CalculatedForwardBackward current, previous;
		public Response(int timesteps)
		{
			current  = new CalculatedForwardBackward(timesteps);
			previous = new CalculatedForwardBackward(timesteps);
		}
		
	}
	private static class CalculatedForwardBackward extends ForwardBackward
	{
		double p[][];
		public CalculatedForwardBackward(int timesteps, int states)
		{
			alpha     = new double[timesteps][states]; 
			beta      = new double[timesteps][states]; 
			p         = new double[timesteps][states]; 
			ctFactors = new double[timesteps];
		}
		public CalculatedForwardBackward(int timesteps)
		{
			alpha     = new double[timesteps][]; 
			beta      = new double[timesteps][]; 
			p         = new double[timesteps][]; 
			ctFactors = new double[timesteps];
		}

		
		@Override
		public double[][] getStateProbabilities()
		{
			return p;
		}
	}


	public static  <O extends Observation> 
	Response calculate(ForwardBackward fwb,  Hmm<O> hmm, List<O> predicted, List<O> real)
	{
		// Create a copy of alpha, beta, ctFactors:
		Response r = new Response(real.size());
		
		for(int t = 0; t < real.size(); t++) 
		{
			List<O> ys = real.subList(0, t);
			O yhat     = predicted.get(t);
			CalculatedForwardBackward ts = onlineUpdate(ys, yhat, hmm, fwb.getAlpha(), fwb.getCtFactors());
			r.current.alpha[t] = ts.alpha[1];
			r.current.beta[t]  = ts.beta[1];
			r.current.p[t]     = ts.p[1];

			r.previous.alpha[t] = ts.alpha[0];
			r.previous.beta[t]  = ts.beta[0];
			r.previous.p[t]     = ts.p[0];
			
			assert(r.current.p[t].length == hmm.nbStates());
		}
		return r;
		
	}
	
	private static <O extends Observation>
	CalculatedForwardBackward onlineUpdate(List<O> oseq, O yhat, Hmm<O> hmm, double alphaInit[][],
			double[] ctFactorsInit)
	{
		CalculatedForwardBackward ts = new CalculatedForwardBackward(2, hmm.nbStates()); // just 2 time steps
		
		//Calculate forward:
		if (oseq.size() == 0)
			for(int i = 0; i < hmm.nbStates(); i++)
			{
				ts.alpha[0][i] = -1;
				ts.alpha[1][i] = ForwardBackward.alphaInit(hmm, yhat, i);
			}
		else
			for(int i = 0; i < hmm.nbStates(); i++)
			{
				ts.alpha[0][i] = alphaInit[oseq.size()-1][i];
				ts.alpha[1][i] = ForwardBackward.computeAlphaStep(alphaInit,  hmm, yhat, oseq.size(), i);
			}

		//Calculate scaling factor
		if (oseq.size() == 0)
			ts.ctFactors[0] = ctFactorsInit[oseq.size()-1];
		else
			ts.ctFactors[0] = -1;
		ts.ctFactors[1] = ForwardBackward.scale(ts.alpha[1]);
		
		// Calculate backward:

		double beta[][]  = new double[2][hmm.nbStates()]; // just 2 time steps
		for (int i = 0; i < hmm.nbStates(); i++)
			beta[1][i] = 1. / ts.ctFactors[1];		
		
		for (int i = 0; i < hmm.nbStates(); i++)
			if (oseq.size() > 0)
				beta[0][i] = ForwardBackward.computeBetaStep(beta, hmm, oseq.get(oseq.size()-1), 0, i);
			else
				beta[0][i] = -1;
		
		// Calculate forward-backward:
		ts.p[1] = Matrix.dotmult(ts.alpha[1], ts.beta[1], ts.ctFactors[1]);
		Matrix.assertProbability(ts.p[1]);
		
		if (oseq.size() == 0)
			ts.p[0] = new double[] {-1, -1};
		else
		{
			ts.p[0] = Matrix.dotmult(ts.alpha[0], ts.beta[0], ts.ctFactors[0]);
			Matrix.assertProbability(ts.p[0]);
		}
		
		return ts; 
	}


}
