/**
 * FAST v1.0       08/12/2014
 * 
 * This code is only for research purpose not commercial purpose.
 * It is originally developed for research purpose and is still under improvement. 
 * Please email to us if you want to keep in touch with the latest release.
	 We sincerely welcome you to contact Yun Huang (huangyun.ai@gmail.com), or Jose P.Gonzalez-Brenes (josepablog@gmail.com) for problems in the code or cooperation.
 * We thank Taylor Berg-Kirkpatrick (tberg@cs.berkeley.edu) and Jean-Marc Francois (jahmm) for part of their codes that FAST is developed based on.
 *
 */

package fast.data;

import java.text.NumberFormat;
import be.ac.ulg.montefiore.run.jahmm.Observation;

public class DataPoint extends Observation {

	private final int student, problem, step, skill;
	private int nbStates;
	/**
	 * expandedFeatures dimensions: 
	 * 1st: hiddenStates; (0 state with _hidden1 features deactivated)
	 * 2nd: type: 0-init,1-tran,2-emit;
	 * 3rd: featureValues corresponding to current hiddenState;
	 * 
	 * expandedFeatures[0] will be null without initialization.
	 */
	private double[][][] expandedFeatures = null;
	// private double[] features = null;
	// private boolean oneLogisticRegression = false;
	private int fold = -1;
	private Double llAprox = -1., llExact = -1.;
	private Integer groundTruth, outcome;

	@Override
	public String toString(NumberFormat numberFormat) {
		return numberFormat.format(outcome);
	}

	public void setNbStates(int nbStates) {
		this.nbStates = nbStates;
		expandedFeatures = new double[this.nbStates][3][];
	}

	public DataPoint(int aOutcome) {
		this.student = -1;
		this.problem = -1;
		this.step = -1;
		this.fold = -1;
		this.skill = -1;
		this.outcome = aOutcome;
	}

	public DataPoint(int aStudent, int aSkill, int aProb, int aStep, int aFold,
			int aOutcome) {
		this.student = aStudent;
		this.skill = aSkill;
		this.problem = aProb;
		this.step = aStep;
		this.fold = aFold;
		this.outcome = aOutcome;
	}

	// For 1LR
	public DataPoint(int aStudent, int aSkill, int aProb, int aStep, int aFold,
			int aOutcome, double[][][] aFeatures) {
		this.student = aStudent;
		this.problem = aProb;
		this.step = aStep;
		this.fold = aFold;
		this.skill = aSkill;
		this.expandedFeatures = aFeatures;
		this.outcome = aOutcome;
		// this.oneLogisticRegression = true;
	}

	// TODO: Reserved for 1 bias feature, check whether I could simplify
	// public DataPoint(int aStudent, int aProb, int aStep, int aFold, int
	// aOutcome,
	// double[] aFeatures) {
	// this.student = aStudent;
	// this.problem = aProb;
	// this.step = aStep;
	// this.fold = aFold;
	// this.features = aFeatures;
	// this.outcome = aOutcome;
	// this.oneLogisticRegression = false;
	// }

	public DataPoint(int studentId, int skillId, int problemId, int stepId,
			int groundTruth, double llAprox, double llExact) {
		this.student = studentId;
		this.skill = skillId;
		this.problem = problemId;
		this.step = stepId;

		this.groundTruth = groundTruth;
		this.llAprox = llAprox;
		this.llExact = llExact;
	}

	public void setExpandedFeatures(double[][][] expandedFeatures) {
		this.expandedFeatures = expandedFeatures;
	}

	// public void setFeatures(double[] features) {
	// this.features = features;
	// }

	public int getStudent() {
		return student;
	}

	public int getSkill() {
		return skill;
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

	/*
	 * type: 0-init, 1-tran, 2-emit
	 */
	public double[] getFeatures(int hiddenStateIndex, int type) {
		if (expandedFeatures == null)
			return null;
		// this will return null if 2nd dimension is not initialized
		return expandedFeatures[hiddenStateIndex][type];
		// else
		// return features;

	}

	// public double[] getFeatures() {
	// return features;
	// }

}
