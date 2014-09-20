package fast.data;

import fast.hmmfeatures.Opts;

import java.text.NumberFormat;

import be.ac.ulg.montefiore.run.jahmm.Observation;

public class DataPoint extends Observation {

	private final int student, problem, step;
	// hy: expandedFeatures[0] will be null without initialization
	private double[][] expandedFeatures = new double[Opts.nbObsStates][];
	private double[] features = null;

	private int fold = -1;

	private Double llAprox = -1., llExact = -1.;
	private Integer groundTruth, outcome;

	@Override
	public String toString(NumberFormat numberFormat) {
		return numberFormat.format(outcome);
	}

	public DataPoint(int aOutcome) {
		this.student = -1;
		this.problem = -1;
		this.step = -1;
		this.fold = -1;

		this.outcome = aOutcome;
	}

	public DataPoint(int aStudent, int aProb, int aStep, int aFold, int aOutcome) {
		this.student = aStudent;
		this.problem = aProb;
		this.step = aStep;
		this.fold = aFold;

		this.outcome = aOutcome;

	}

	// hy:
	public DataPoint(int aStudent, int aProb, int aStep, int aFold, int aOutcome,
			double[][] aFeatures) {
		this.student = aStudent;
		this.problem = aProb;
		this.step = aStep;
		this.fold = aFold;
		this.expandedFeatures = aFeatures;
		this.outcome = aOutcome;
	}

	public DataPoint(int aStudent, int aProb, int aStep, int aFold, int aOutcome,
			double[] aFeatures) {
		this.student = aStudent;
		this.problem = aProb;
		this.step = aStep;
		this.fold = aFold;
		this.features = aFeatures;
		this.outcome = aOutcome;
	}

	public DataPoint(int studentId, int problemId, int stepId, int groundTruth,
			double llAprox, double llExact) {
		this.student = studentId;
		this.problem = problemId;
		this.step = stepId;

		this.groundTruth = groundTruth;
		this.llAprox = llAprox;
		this.llExact = llExact;
	}

	public void setExpandedFeatures(double[][] expandedFeatures) {
		this.expandedFeatures = expandedFeatures;
	}

	public void setFeatures(double[] features) {
		this.features = features;
	}

	public int getStudent() {
		return student;
	}

	public int getProblem() {
		return problem;
	}

	public int getStep() {
		return step;
	}

	public int getFold() {
		return fold;
	}

	public int getOutcome() {
		return outcome;
	}

	public int getGroundTruth() {
		return groundTruth;
	}

	public double getLLAprox() {
		return llAprox;
	}

	public double getLLExact() {
		return llExact;
	}

	// hy:
	public double[] getFeatures(int hiddenStateIndex) {
		if (Opts.oneLogisticRegression)
			return expandedFeatures[hiddenStateIndex];// will return null if 2nd
																								// dimension is not initialized
		else
			return features;

	}

	// public double[] getFeatures() {
	// return features;
	// }

}
