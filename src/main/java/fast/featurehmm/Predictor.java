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

package fast.featurehmm;

import java.io.BufferedWriter;
import java.io.FileWriter;
//import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
//import java.util.HashMap;
import fast.common.Bijection;
import fast.data.DataPoint;
import fast.data.StudentList;
import fast.evaluation.Degeneracy;
import fast.evaluation.Mastery;
import fast.evaluation.TestSummary;
//import fast.evaluation.TrainSummary;

public class Predictor {

	private final boolean useEmissionToJudgeHiddenStates;
	private final boolean allowForget;
	
	private Mastery masteryObj = null;
	private Degeneracy degeneracyObj = null;
	
	private double cutoff = 0.5; // >=cutoff -> class1
	private boolean verbose = false;
	
	//public HashMap<String, int[]> knowStateJudgements = null;
	private ArrayList<Integer> actualLabels = new ArrayList<Integer>();
	private ArrayList<Integer> predLabels  = new ArrayList<Integer>();
	private ArrayList<Double> predProbs  = new ArrayList<Double>();
	private ArrayList<Double> priorProbKnowns  = new ArrayList<Double>();
	private ArrayList<Double> posteriorProbKnowns  = new ArrayList<Double>();


	public Predictor(boolean useEmissionToJudgeHiddenStates, boolean allowForget){
		this(useEmissionToJudgeHiddenStates, allowForget,  null, null);
	}
	
	//TODO: simplify the parameters; consider knowStateJudgements
	public Predictor(boolean useEmissionToJudgeHiddenStates, boolean allowForget, Degeneracy degeneracyObj, Mastery masteryObj) {
		this.useEmissionToJudgeHiddenStates = useEmissionToJudgeHiddenStates;
		this.allowForget = allowForget;
		this.masteryObj = masteryObj;
		this.degeneracyObj = degeneracyObj;
	}

	
	public void test(FeatureHMM hmm, StudentList testSequences) throws IOException {
		actualLabels = new ArrayList<Integer>();
		predLabels  = new ArrayList<Integer>();
		predProbs  = new ArrayList<Double>();
		priorProbKnowns  = new ArrayList<Double>();
		posteriorProbKnowns  = new ArrayList<Double>();

		
		Bijection skills = testSequences.getExpertSkills();
//		Bijection stus = testSequences.getOriStudents();
//		Bijection problems = testSequences.getProblems();
//		Bijection steps = testSequences.getSteps();
		String firstDpKC = "";

		int unknownState = 0; // before judging
		int knownState = 1;// before judging
		boolean hiddenStateChecked = false;
		double priorProbState1_n = 0.0;
		double probClass1_n_1 = 0.0;
		int nbTrainInTest = 0;
		int nbTest = 0;
		int nbDecreasePKnonw = 0;
		int nbDecreasePCorrect = 0;
		
		for (int stuId = 0; stuId < testSequences.size(); stuId++) {// per student
			priorProbState1_n = hmm.getInitiali(knownState, testSequences.get(stuId).get(0).getFeatures(knownState, 0));
			for (int dpId = 0; dpId < testSequences.get(stuId).size(); dpId++) { // per datapoint
				DataPoint dp = testSequences.get(stuId).get(dpId);
				String kc = skills.get(dp.getSkill());
				if (firstDpKC.length() == 0)
					firstDpKC = kc;
				else if (!firstDpKC.equals(kc)) {
					System.out.println("ERROR: this datapoint should belong to " + firstDpKC);
					System.exit(-1);
				}
				//String stu = stus.get(dp.getStudent());
				int fold = dp.getFold();
				int outcome = dp.getOutcome();
				//String problem = problems.get(dp.getProblem());
				//String step = steps.get(dp.getStep());
				if (fold == -1)//need to consider in order to update till the test set datapoint
					nbTrainInTest++;
				else
					nbTest++;
				if (!hiddenStateChecked) {
					knownState = FeatureHMM.getKnownState(hmm, dp, useEmissionToJudgeHiddenStates, allowForget);
					unknownState = 1 - knownState;
					hiddenStateChecked = true;
				}
				
				if (hiddenStateChecked && dpId == 0)
					priorProbState1_n = hmm.getInitiali(knownState, testSequences.get(stuId).get(0).getFeatures(knownState, 0));// assuming it's P(K)
				
				// INFERENCE
				double probState1Class0 = Math.min(hmm.getEmissionjk(knownState, 0, dp.getFeatures(knownState, 2)), 1.0);
				double probState0Class1 = Math.min(hmm.getEmissionjk(unknownState, 1, dp.getFeatures(unknownState, 2)), 1.0);
				double probClass1_n = Math.min(priorProbState1_n * (1 - probState1Class0) + (1 - priorProbState1_n) * probState0Class1, 1.0);// should be P(C)

				int predLabel = (probClass1_n > cutoff) ? 1 : 0;

				// UPDATE
				/* This is the posterior knowledge probability. */
				double posteriorProbState1_n;
				if (outcome == 1)
					posteriorProbState1_n = Math.min(priorProbState1_n * (1 - probState1Class0) / (probClass1_n == 0.0 ? Double.MIN_VALUE : probClass1_n), 1.0);
				else
					posteriorProbState1_n = Math.min(priorProbState1_n * probState1Class0 / ((1 - probClass1_n) == 0.0 ? Double.MIN_VALUE : (1 - probClass1_n)), 1.0);
				double probState1State0 = Math.min(hmm.getTransitionij(knownState, unknownState, dp.getFeatures(knownState, 1)), 1.0);
				double probState0State1 = Math.min(hmm.getTransitionij(unknownState, knownState, dp.getFeatures(unknownState, 1)), 1.0);
				/* Correspond to original paper Formula (1). This is the prior for next datapoint */
				double priorProbState1_n1 = Math.min(posteriorProbState1_n * (1 - probState1State0) + (1 - posteriorProbState1_n) * probState0State1, 1.0); 

				if (verbose) 
					System.out.println("\toutcome:\t" + dp.getOutcome() + "\tP(Correct):\t" + probClass1_n + "\tPprior('K'):\t" + priorProbState1_n + "\tP('slip'):\t" + probState1Class0 + "\tP('guess'):\t" + probState0Class1 + "\tP('learn'):\t" + probState0State1 + "\tP('forget'):\t" + probState1State0 + "\tP(Correct):\t" + probClass1_n + "\tPpost('K'):\t" + posteriorProbState1_n + "\tPprior('K')_next:\t" + priorProbState1_n1);
				if (probClass1_n == 0.0 || probClass1_n == 1.0)
					System.out.println("WARNING: probClass1_n=" + probClass1_n);
				
				if (fold != -1){
					actualLabels.add(outcome);
					predLabels.add(predLabel);
					predProbs.add(probClass1_n);
					priorProbKnowns.add(priorProbState1_n);
					posteriorProbKnowns.add(posteriorProbState1_n);
//					
//					if (predWriter != null){
//						//before 20150616, it outputs priorProbState1_n and priorProbState1_n1
//						String s = outcome + "," + predLabel + "," + (probClass1_n) + "," + (priorProbState1_n) + "," + posteriorProbState1_n + "," + stu + "," + kc + "," + problem + "," + step;
//						predWriter.write(s + "\n");
//						predWriter.flush();
//					}
					if (degeneracyObj != null) {
						double[] degenerate_statitics = degeneracyObj.degeneracyJudgementsAcrossDataPoints;
						double sumGuessSlip = FeatureHMM.checkDegeneracy(hmm, dp, knownState);
						if ((sumGuessSlip >= 1 && degeneracyObj.degeneracyJudgementInequality.equals("be")) || (sumGuessSlip > 1 && degeneracyObj.degeneracyJudgementInequality.equals("b")))
							degenerate_statitics[2] += 1;
						degenerate_statitics[5] += sumGuessSlip;
						if (sumGuessSlip < degenerate_statitics[9])
							degenerate_statitics[9] = sumGuessSlip;
					}
					if (masteryObj != null){
						if (!masteryObj.studentsReachedMastery.contains(stuId + "")) {
							if (priorProbState1_n >= masteryObj.MASTERY_THRESHOLD) {
								masteryObj.studentsReachedMastery.put(stuId + "");
								masteryObj.nbPracToReachMastery.add(dpId * 1.0);
							}
							if (priorProbState1_n1 >= this.masteryObj.MASTERY_THRESHOLD) {
								masteryObj.studentsReachedMastery.put(stuId + "");
								masteryObj.nbPracToReachMastery.add(dpId + 1.0);
							}
						}
					}
					if (degeneracyObj != null) {
						if (priorProbState1_n1 < priorProbState1_n && dpId > 0)
							nbDecreasePKnonw++;
						if (probClass1_n < probClass1_n_1 && dpId > 0)
							nbDecreasePCorrect++;
					}
				}//fold = -1
				// for checking whether use first dp to judge is suitable
//			if (knowStateJudgements != null && allowForget && hiddenStateChecked ) {
//				int[] statitics = knowStateJudgements.get(kc);
//				statitics[3] = knownState;
//				int current_knownState = FeatureHMM.judgeHiddenStates(hmm, dp, this.useEmissionToJudgeHiddenStates);
//				if (knownState != current_knownState)
//					statitics[4] += 1;
//			}
				priorProbState1_n = priorProbState1_n1;
				probClass1_n_1 = probClass1_n;

			}// per student sequence
			if (verbose)
				System.out.println("");
		}
		if (verbose)
			System.out.println("nbTrainDataPointsInTest=" + nbTrainInTest);
//		if (predWriter != null)
//			predWriter.close();
		if (degeneracyObj != null) {
			degeneracyObj.degeneracyJudgementsAcrossDataPoints[3] = nbTest;
			degeneracyObj.degeneracyJudgementsAcrossDataPoints[6] = nbDecreasePKnonw / (1.0 * (nbTest - testSequences.size()));
			degeneracyObj.degeneracyJudgementsAcrossDataPoints[7] = nbDecreasePCorrect / (1.0 * (nbTest - testSequences.size()));
		}
		// for checking knownState judgement
		//		if (knowStateJudgements != null && allowForget) {
		//			int[] statitics = knowStateJudgements.get(aHmmKC);
		//			statitics[5] = nb_dp;
		//			knowStateJudgements.put(aHmmKC, statitics);
		//		}
	}
	
	public static void writeHMMPrediction(String predictionFile, StudentList testSequences, TestSummary testSummary,
																boolean append, boolean header) throws IOException{
		Bijection skills = testSequences.getExpertSkills();
		Bijection stus = testSequences.getOriStudents();
		Bijection problems = testSequences.getProblems();
		Bijection steps = testSequences.getSteps();
		int id = 0;
		
		BufferedWriter predWriter = null;
		if (append)
			 predWriter = new BufferedWriter(new FileWriter(predictionFile, true));
		else
			 predWriter = new BufferedWriter(new FileWriter(predictionFile));
		for (int stuId = 0; stuId < testSequences.size(); stuId++) {// per student
			for (int dpId = 0; dpId < testSequences.get(stuId).size(); dpId++) {
				
				DataPoint dp = testSequences.get(stuId).get(dpId);
				int fold = dp.getFold();
				if (fold == -1)
					continue;
				int outcome = dp.getOutcome();
				int actualLabel = testSummary.actualLabels.get(id);
				if (outcome != actualLabel){
					System.out.println("ERROR: outcome != actualLabel");
					System.exit(-1);
				}
				int predLabel = testSummary.predLabels.get(id);
				double predProb = testSummary.predProbs.get(id);
				double priorProbKnown = testSummary.priorProbKnowns.get(id);
				double posteriorProbKnown = testSummary.posteriorProbKnowns.get(id);
				String kc = skills.get(dp.getSkill());
				String problem = problems.get(dp.getProblem());
				String step = steps.get(dp.getStep());
				String stu = stus.get(dp.getStudent());
				
				//before 20150616, it outputs priorProbState1_n and priorProbState1_n1
				if (header){
					predWriter.write("actualLabel,predLabel,predProbCorrect,priorProbKnown,posteriorProbKnown,student,kc,problem,step\n");
					header = false;
				}
				String s = outcome + "," + predLabel + "," + predProb + "," + priorProbKnown + "," + posteriorProbKnown + "," + stu + "," + kc + "," + problem + "," + step;
				predWriter.write(s + "\n");
				predWriter.flush();
				
				id++;
			}
		}
		predWriter.close();
	}
	
	public ArrayList<Integer> getActualLabels(){
		return actualLabels;
	}
	
	public ArrayList<Integer> getPredLabels(){
		return predLabels;
	}
	
	public ArrayList<Double> getPredProbs(){
		return predProbs;
	}
	
	
	public ArrayList<Double> getPriorProbKnowns(){
		return priorProbKnowns;
	}
	
	public ArrayList<Double> getPosteriorProbKnowns(){
		return posteriorProbKnowns;
	}
	
//	public TestSummary getTestSummary(){
//		TestSummary testSummary = new TestSummary();
//		testSummary.update(actualLabels, predictLabels, predictProbs, priorProbKnowns, posteriorProbKnowns);
//		return testSummary;
//	}
}
