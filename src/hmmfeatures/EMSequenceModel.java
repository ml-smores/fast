package hmmfeatures;

import hmm.ForwardBackward;
import java.util.List;
import fig.basic.Pair;

public interface EMSequenceModel {

	public abstract void setActiveFeatures(
			List<Pair<Integer, Double>>[][] activeTransFeatures0,
			List<Pair<Integer, Double>>[][] activeEmitFeatures0, int numFeatures0,
			double[] regularizationWeights0, double[] regularizationBiases0);

	public abstract double[][] getTransPotentials();

	public abstract double[][] getEmitPotentials();

	public abstract double[] getWeights();

	public abstract int getNumFeatures();

	public abstract int getNumLabels();

	public abstract int getNumObservations();

	public abstract int getStartLabel();

	public abstract int getStopLabel();

	public abstract void setWeights(double[] weights0);

	public abstract void computePotentials();

	public abstract void EStep();

	public abstract void MStep();

	public abstract double calculateRegularizedLogMarginalLikelihood();

	public abstract double calculateRegularizedExpectedLogLikelihood();

	public abstract double calculateRegularizer();

	public abstract ForwardBackward getForwardBackward();

}