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

package fast.experimenter;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;

import fast.data.DataPoint;
import fast.data.StudentList;
import fast.hmmfeatures.FeatureHMM;

public class Predict {

	public Opts opts;
	public boolean verbose = false;
	public boolean parameterizedEmit = false;
	public double cutoff = 0.5; // >=cutoff -> class1
	// public ArrayList<Double> probs = new ArrayList<Double>();
	// public ArrayList<Integer> labels = new ArrayList<Integer>();
	// public ArrayList<Integer> actualLabels = new ArrayList<Integer>();
	public int obsClass1 = 1;
	public int obsClass0 = 0;
	public String obsClass1Name = "correct";
	public String obsClass0Name = "incorrect";
	// TODO: need to infer K/NK, C/IC so that it can be made generic?
	// TODO: currently only two states cases
	public int hiddenState1 = 1;// assuming it's "K"
	public int hiddenState0 = 0;
	// public double bias = -1.0;
	public String predictionFile = "";
	public HashSet<String> skillsToCheck = new HashSet<String>();
	public ArrayList<double[]> features = null;
	public ArrayList<ArrayList<Double>> studentListProbs = new ArrayList<ArrayList<Double>>();
	public ArrayList<ArrayList<Integer>> studentListLabels = new ArrayList<ArrayList<Integer>>();
	public ArrayList<ArrayList<Integer>> studentListActualLabels = new ArrayList<ArrayList<Integer>>();
	public ArrayList<Integer> studentIndexList = new ArrayList<Integer>();
	public BufferedWriter predWriter = null;
	public BufferedWriter trainPredWriter = null;

	public void main(String[] args) {
	}

	public Predict(Opts opts) {
		this.opts = opts;
		this.parameterizedEmit = opts.parameterizedEmit;
		this.verbose = opts.verbose;
		this.predictionFile = opts.predictionFile;
		this.hiddenState1 = opts.hiddenState1;
		this.hiddenState0 = 1 - hiddenState1;
		this.obsClass1 = opts.obsClass1;
		this.obsClass0 = 1 - obsClass1;
		this.obsClass1Name = opts.obsClass1Name;
		this.obsClass0Name = obsClass1Name.equals("correct") ? "incorrect"
				: "correct";
		this.skillsToCheck = opts.skillsToCheck;

	}

	public int doPredict(FeatureHMM hmm, StudentList testSequences,
			ArrayList<Double> probs, ArrayList<Integer> labels,
			ArrayList<Integer> actualLabels, ArrayList<Integer> trainTestIndicator,
			int lineID, String KCName, ArrayList<double[]> features) {
		this.features = features;
		return doPredict(hmm, testSequences, probs, labels, actualLabels,
				trainTestIndicator, lineID, KCName);
	}

	public void doPredict(FeatureHMM hmm, StudentList testSequences,
			ArrayList<ArrayList<Double>> studentListProbs,
			ArrayList<ArrayList<Integer>> studentListLabels,
			ArrayList<ArrayList<Integer>> studentListActualLabels,
			ArrayList<Integer> studentIndexList, String KCName) {
		// this.features = features;
		this.studentListProbs = studentListProbs;
		this.studentListLabels = studentListLabels;
		this.studentListActualLabels = studentListActualLabels;
		this.studentIndexList = studentIndexList;
		ArrayList<Integer> trainTestIndicator = null;
		ArrayList<Double> probs = new ArrayList<Double>();
		ArrayList<Integer> labels = new ArrayList<Integer>();
		ArrayList<Integer> actualLabels = new ArrayList<Integer>();
		int lineID = 1;
		doPredict(hmm, testSequences, probs, labels, actualLabels,
				trainTestIndicator, lineID, KCName);
	}

	public void doPredictAndWritePredFile(FeatureHMM hmm, StudentList testSequences,
			ArrayList<Double> probs, ArrayList<Integer> labels,
			ArrayList<Integer> actualLabels, ArrayList<Integer> trainTestIndicators,
			int lineID, String KCName, BufferedWriter predWriter,
			BufferedWriter trainPredWriter) throws IOException {
		this.predWriter = predWriter;
		this.trainPredWriter = trainPredWriter;
		doPredict(hmm, testSequences, probs, labels, actualLabels,
				trainTestIndicators, 2, KCName);
		// this.predWriter.close();
	}

	// receiving one hmm
	public int doPredict(FeatureHMM hmm, StudentList testSequences,
			ArrayList<Double> probs, ArrayList<Integer> labels,
			ArrayList<Integer> actualLabels, ArrayList<Integer> trainTestIndicator,
			int lineID, String KCName) {

		double[][] emitFeatureWeights = new double[hmm.nbStates()][];
		int[] emitClass1 = new int[hmm.nbStates()];
		int[] emitClass0 = new int[hmm.nbStates()];
		String[] emitClass1String = new String[hmm.nbStates()];
		String[] emitClass0String = new String[hmm.nbStates()];

		for (int s = 0; s < hmm.nbStates(); s++) {
			if (parameterizedEmit)
				emitFeatureWeights[s] = hmm.getOpdf(s).featureWeights;
			// should be indexed by 1
			// TODO: currently only 2 classes case
			emitClass1[s] = hmm.getOpdf(s).opts.obsClass1;
			emitClass0[s] = Math.abs(1 - emitClass1[s]);
			// should be "correct" by hy's assignment
			emitClass1String[s] = hmm.getOpdf(s).opts.obsClass1Name;
			emitClass0String[s] = (emitClass1String[s].equals(obsClass1Name) ? obsClass0Name
					: obsClass1Name);
			// TODO
			if (!emitClass1String[s].equals(obsClass1Name)
					|| emitClass1[s] != obsClass1) {
				System.out.println("Error: please map " + emitClass1String[s] + "/"
						+ emitClass1[s] + " into " + obsClass1Name + "/" + obsClass1 + "!");
				System.exit(1);
			}
		}

		// print HMM's probabilities
		if (verbose)
			System.out.println(hmm);
		// print(hmm);

		boolean hiddenStateChecked = false;
		double priorProbState1_n = 0.0;
		double nbTrainDataPointsInTestInOneHmm = 0;
		for (int i = 0; i < testSequences.size(); i++) {
			// before checking hiddenstate, may be wrong;
			priorProbState1_n = hmm.getPi(hiddenState1);
			ArrayList<Double> aStudentProbs = new ArrayList<Double>();
			ArrayList<Integer> aStudentLabels = new ArrayList<Integer>();
			ArrayList<Integer> aStudentActualLabels = new ArrayList<Integer>();
			for (int j = 0; j < testSequences.get(i).size(); j++) {
				DataPoint dp = testSequences.get(i).get(j);

				int fold = dp.getFold();
				if (trainTestIndicator != null)
					trainTestIndicator.add(fold);
				if (fold == -1)
					nbTrainDataPointsInTestInOneHmm++;
				// if (fold != -1) {
				// for 2LR
				if (opts.generateLRInputs) {
					double[] afeatures = dp.getFeatures(0);
					features.add(afeatures);
				}
				// }
				// get outcome and features
				int outcome = dp.getOutcome();
				actualLabels.add(outcome);
				aStudentActualLabels.add(outcome);
				// double[] featureValues = dp.getFeatures();

				// decide hidden states, each hmm should be redecide
				// TODO: it is just decided by the first point, what if later points
				// have inverse trends?
				if (!hiddenStateChecked) {
					if (opts.useEmissionToJudgeHiddenStates) {
						double hidden0obs1 = hmm.getOpdf(0).probability(dp.getFeatures(0),
								obsClass1);
						double hidden1obs1 = hmm.getOpdf(1).probability(dp.getFeatures(1),
								obsClass1);
						if (hidden0obs1 > hidden1obs1) {
							hiddenState1 = 0;
							hiddenState0 = 1;
						}
						else {
							hiddenState1 = 1;
							hiddenState0 = 0;
						}
						if (verbose)
							System.out.println("hidden0obs1=" + hidden0obs1
									+ "\thidden1obs1=" + hidden1obs1 + "\thiddenState1="
									+ hiddenState1 + "\thiddenState0=" + hiddenState0 + "\n");
					}
					else {
						double a01 = hmm.getAij(0, 1);
						double a10 = hmm.getAij(1, 0);
						if (a01 < a10) {
							hiddenState1 = 0;// know
							hiddenState0 = 1;// notknow
						}
						else {
							hiddenState1 = 1;// know
							hiddenState0 = 0;// unknow
						}
						if (verbose)
							System.out.println("a01=" + a01 + "\ta10=" + a10
									+ "\thiddenState1=" + hiddenState1 + "\thiddenState0="
									+ hiddenState0 + "\n");
					}
					hiddenStateChecked = true;
				}
				if (hiddenStateChecked && j == 0)
					priorProbState1_n = hmm.getPi(hiddenState1); // assuming it's P(K)

				// INFERENCE
				// assuming it's slip
				// System.out.println("hidden" + hiddenState1);
				double probState1Class0 = Math.min(hmm.getOpdf(hiddenState1)
						.probability(dp.getFeatures(hiddenState1), obsClass0), 1.0);
				// assuming it's guess
				// System.out.println("hidden" + hiddenState0);
				double probState0Class1 = Math.min(hmm.getOpdf(hiddenState0)
						.probability(dp.getFeatures(hiddenState0), obsClass1), 1.0);
				double probClass1_n = Math.min(priorProbState1_n
						* (1 - probState1Class0) + (1 - priorProbState1_n)
						* probState0Class1, 1.0);// should be P(C)

				int predLabel = (probClass1_n > cutoff) ? 1 : 0;
				probs.add(probClass1_n);
				labels.add(predLabel);
				aStudentProbs.add(probClass1_n);
				aStudentLabels.add(predLabel);

				try {
					String s = outcome + "," + predLabel + ","
							+ opts.formatter2.format(probClass1_n);
					if (predWriter != null && fold != -1)
						predWriter.write(s + "\n");
					else if (trainPredWriter != null && fold == -1)
						trainPredWriter.write(s + "\n");
				}
				catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				// UPDATE
				double posteriorProbState1_n;
				if (outcome == obsClass1)
					posteriorProbState1_n = Math.min(priorProbState1_n
							* (1 - probState1Class0)
							/ (probClass1_n == 0.0 ? Double.MIN_VALUE : probClass1_n), 1.0);
				else
					posteriorProbState1_n = Math.min(priorProbState1_n
							* probState1Class0
							/ ((1 - probClass1_n) == 0.0 ? Double.MIN_VALUE
									: (1 - probClass1_n)), 1.0);
				double probState1State0 = Math.min(
						hmm.getAij(hiddenState1, hiddenState0), 1.0);// assuming
				// forget
				double probState0State1 = Math.min(
						hmm.getAij(hiddenState0, hiddenState1), 1.0);// assuming
				// learn
				double priorProbState1_n1 = Math.min(posteriorProbState1_n
						* (1 - probState1State0) + (1 - posteriorProbState1_n)
						* probState0State1, 1.0);

				if (verbose) {
					if (parameterizedEmit) {
						System.out.print("Line:\t" + lineID + "\thidden0 features:\t");
						for (int k = 0; k < dp.getFeatures(hiddenState0).length; k++)
							System.out.print(dp.getFeatures(hiddenState0)[k] + "\t");
						System.out.print("\tseqID:\t" + j);
						System.out.print("Line:\t" + lineID + "\thidden1 features:\t");
						for (int k = 0; k < dp.getFeatures(hiddenState1).length; k++)
							System.out.print(dp.getFeatures(hiddenState1)[k] + "\t");
						System.out.print("\tseqID:\t" + j);
					}
					else {
						System.out.print("Line:\t" + lineID + "\tseqID:\t" + j);
					}
					System.out.println("\toutcome:\t" + dp.getOutcome()
							+ "\tP(Correct):\t" + probClass1_n + "\tPprior('K'):\t"
							+ priorProbState1_n + "\tP('slip'):\t" + probState1Class0
							+ "\tP('guess'):\t" + probState0Class1 + "\tP('learn'):\t"
							+ probState0State1 + "\tP('forget'):\t" + probState1State0
							+ "\tP(Correct):\t" + probClass1_n + "\tPpost('K'):\t"
							+ posteriorProbState1_n + "\tPprior('K')_next:\t"
							+ priorProbState1_n1);
				}
				if (probClass1_n == 0.0 || probClass1_n == 1.0) {
					System.out.println("\tWarning: probClass1_n=" + probClass1_n);
				}
				lineID++;
				priorProbState1_n = priorProbState1_n1;
			}
			if (verbose)
				System.out.println("");
			studentListProbs.add(aStudentProbs);
			studentListLabels.add(aStudentLabels);
			studentListActualLabels.add(aStudentActualLabels);
		}
		if (skillsToCheck.contains(KCName)) {
			try {
				if (opts.writeMainLog) {
					opts.mainLogWriter.write("\nKC=" + KCName
							+ "\tnbTrainDataPointsInTestInOneHmm="
							+ nbTrainDataPointsInTestInOneHmm + "\n");
					opts.mainLogWriter.flush();
				}
			}
			catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		if (verbose)
			System.out.println("\n");
		return lineID;
	}
	// public void print(Hmm hmm) {
	// double priorProbState1 = hmm.getPi(hiddenState1); // assuming it's P(K)
	// double probState0State1 = hmm.getAij(hiddenState0, hiddenState1);//
	// assuming
	// // learn
	// double probState1State0 = hmm.getAij(hiddenState1, hiddenState0);//
	// assuming
	// // forget
	//
	// if (parameterizedEmit) {
	// // assuming it's guess, p(X|NK)
	// double[] probState0_weights = hmm.getOpdf(hiddenState0).featureWeights;
	// String probState0_w = "";
	// for (int i = 0; i < probState0_weights.length; i++)
	// probState0_w += probState0_weights[i] + "\t";
	// // assuming it's slip, p(X|K)
	// double[] probState1_weights = hmm.getOpdf(hiddenState1).featureWeights;
	// String probState1_w = "";
	// for (int i = 0; i < probState1_weights.length; i++)
	// probState1_w += probState1_weights[i] + "\t";
	// if (verbose) {
	// System.out.println("\npInit:\t" + priorProbState1 + "\npLearn:\t"
	// + probState0State1 + "\npForget:\t" + probState1State0
	// + "\np(C|K):\t" + probState1_w + "\np(C|NK):\t" + probState0_w
	// + "\n");
	// }
	// }
	// else {
	// double[] probState0_para = hmm.getOpdf(hiddenState0).probabilities;
	// String probState0_p = "";
	// for (int i = 0; i < probState0_para.length; i++)
	// probState0_p += probState0_para[i] + "\t";
	// // assuming it's slip, p(X|K)
	// double[] probState1_para = hmm.getOpdf(hiddenState1).probabilities;
	// String probState1_p = "";
	// for (int i = 0; i < probState1_para.length; i++)
	// probState1_p += probState1_para[i] + "\t";
	// if (verbose) {
	// System.out.println("\npInit:\t" + priorProbState1 + "\npLearn:\t"
	// + probState0State1 + "\npForget:\t" + probState1State0
	// + "\np(C|K):\t" + probState1_p + "\np(C|NK):\t" + probState0_p
	// + "\n");
	// }
	// }

	// }
}
