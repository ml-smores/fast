
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
import java.text.DecimalFormat;
//import java.io.FileWriter;
//import java.io.IOException;
//import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Date;
//import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
//import java.util.List;
//import java.util.TreeMap;
import fast.common.*;
import fast.data.DataPoint;
import fast.data.StudentList;
//import fast.common.Stats.ValueIndexSummary;
//import fast.data.DataPoint;
//import fast.data.StudentList;
import fast.evaluation.*;
//import fast.featurehmm.BaumWelchLearner;
//import fast.featurehmm.FeatureHMM;
//import fast.prediction.*;
import fast.featurehmm.FeatureHMM;

public class Logger {
	
	public Bijection metrics = new Bijection();
	public Bijection predictionMetrics = new Bijection();
	//public Bijection nonGOGONDMetricNames = new Bijection();
	public Date startDate;//curRestartStartDate;
	public BufferedWriter mainLogWriter = null;

	public OneRestartLogger curRestart = null; 
	public MultiRestartsLogger curMultiRestarts = null;
	public OneFileLogger curFile = null;
	public MultiFilesLogger curMultiFiles = null;

	
	public class OneRestartLogger{
		private String hmmName = "";
		private int restartId = 0;
		public TrainSummary trainSummary = new TrainSummary();
		public TestSummary testSummary = new TestSummary(); //Also contrain some train information
		
//		public ArrayList<Integer> testActualLabels = new ArrayList<Integer>();
//		public ArrayList<Integer> testPredLabels = new ArrayList<Integer>();
//		public ArrayList<Double> testPredProbs = new ArrayList<Double>();
//		//public Bijection outputParameterNames = new Bijection(); // in order to specify order of the features
//		public Mastery mastery = new Mastery();
//		public Degeneracy degeneracy = new Degeneracy();
		//public double trainTestTime;

		public OneRestartLogger(int restartId, String hmmName){
			this.restartId = restartId;
			this.hmmName = hmmName;
		}
		
		public String getHMMName(){
			return hmmName;
		}

		public void getTrainsetDegeneray(FeatureHMM hmm, StudentList curKcTrainStuList, boolean allowForget, boolean useEmissionToJudgeHiddenStates,
				String degeneracyJudgementInequality){
			testSummary.degeneracy.degeneracyJudgementInequality = degeneracyJudgementInequality;
			int knownState = FeatureHMM.getKnownState(hmm, curKcTrainStuList.get(0).get(0), useEmissionToJudgeHiddenStates, allowForget);
			int nbDp = 0;//, current_knownState = 0;
			double[] degenerateStatitics = new double[]{0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0};
			for (int i = 0; i < curKcTrainStuList.size(); i++) {//per student
			for (int j = 0; j < curKcTrainStuList.get(i).size(); j++) { //per datapoint
				DataPoint dp = curKcTrainStuList.get(i).get(j);
				double sumGuessSlip = FeatureHMM.checkDegeneracy(hmm, dp, knownState);
				if ((sumGuessSlip >= 1 && degeneracyJudgementInequality.equals("be")) || (sumGuessSlip > 1 && degeneracyJudgementInequality.equals("b")))
					degenerateStatitics[0] += 1;
				degenerateStatitics[4] += sumGuessSlip;
				if (sumGuessSlip < degenerateStatitics[8])
					degenerateStatitics[8] = sumGuessSlip;
				nbDp++;
			}
			degenerateStatitics[1] = nbDp;
			testSummary.degeneracy.degeneracyJudgementsAcrossDataPoints = degenerateStatitics;
			}	
		}
		
		//TODO: refine and combine
//		public void printTrainSummary() throws IOException{
//			String llErrorStr = "current KC:" + "\t#LLError:\t" + curRestart.trainSummary.nbLLError;
//			String maxllDecreasePerHmmStr = "current KC:" + "\tmaxLLDecreaseValuePerIter:\t" + formatter.format(curRestart.trainSummary.maxLLDecrease);
//			String maxllDecreaseRatioPerHmmStr = "current KC:" + "\tmaxLLDecreaseRatioValuePerIter:\t" + formatter.format(curRestart.trainSummary.maxLLDecreaseRatio);
//			printAndLog(llErrorStr);
//			printAndLog(maxllDecreasePerHmmStr);
//			printAndLog(maxllDecreaseRatioPerHmmStr);
//		}
		
	}
	
	
	//TODO: avoid multiple Restarts create multiple predLabel...
	//TODO: consider output to file and read the best model file, so that we can save memory;
	public class MultiRestartsLogger{
		
		private String hmmName;
		private ArrayList<OneRestartLogger> restartLoggers = new ArrayList<OneRestartLogger>();
		private int maxTrainLLRestartId = -1;
		private double maxTrainLL = -1.0E10;
		
		public MultiRestartsLogger(String hmmName){
			this.hmmName = hmmName;
		}
		
		public int getBestRestartId(){
			return maxTrainLLRestartId;
		}
		
		public OneRestartLogger getLogger(int i){
			return restartLoggers.get(i);
		}
		
		public void add(OneRestartLogger oneRestart){
			if (restartLoggers.size() == oneRestart.restartId){
				if (restartLoggers.size() == 0 || oneRestart.trainSummary.trainLL > maxTrainLL){
					maxTrainLL = oneRestart.trainSummary.trainLL;
					maxTrainLLRestartId = oneRestart.restartId;
				}
				restartLoggers.add(oneRestart);
			}
			else{
				System.out.println("ERROR: restartLoggers.size() != oneRestart.restartId!");
				System.exit(-1);
			}
		}
		
		public String getHMMName(){
			return hmmName;
		}
	}

	public class OneFileLogger{//all Hmms
		private int fileId;
		public Bijection trainAllHMMs = new Bijection();
		public HashSet<String> trainedHMMs = new HashSet<String>();
//		public double trainLL = 0.0;
//		public int trainNbLLError = 0;
//		public double trainMaxLLDecrease;
//		public double trainMaxLLDecreaseRatio;
//		public int trainNbStopByEMIteration = 0;
		//public HashSet<String> trainParameterizingFailed = new HashSet<String>();
		/* the index is corresponding to the original hmmId (even there are hmmIDs that are skipped, the code still add "fake" content) */
		private ArrayList<LinkedHashMap<String, Double>> trainedParameters = new ArrayList<LinkedHashMap<String, Double>>(); //per HMM
		//public Bijection outputParameterNamesForStats = new Bijection(); // in order to specify order of the features
		//public HashMap<String, ArrayList<Double>> featureToAllKCValues = new HashMap<String, ArrayList<Double>>();// feature to values (ordered by kc)
		//public HashMap<String, ArrayList<Double>> metricToAllKCMeanAcrossRestarts = new HashMap<String, ArrayList<Double>>();
		//public HashMap<String, ArrayList<Double>> metricToAllKCSdAcrossRestarts = new HashMap<String, ArrayList<Double>>();
		private ArrayList<Integer> testActualLabels = new ArrayList<Integer>();
		private ArrayList<Integer> testPredLabels = new ArrayList<Integer>();
		private ArrayList<Double> testPredProbs = new ArrayList<Double>();
		private ArrayList<Metrics> testEvals = new ArrayList<Metrics>(); //per HMM
		
		public OneFileLogger(int fileId){
			this.fileId = fileId;
		}
		
		public void add(int hmmId, OneRestartLogger curHmmLogger){
			trainedHMMs.add(curHmmLogger.hmmName);
//			trainLL += curHmmLogger.trainSummary.trainLL;
//			trainNbLLError += curHmmLogger.trainSummary.nbLLError;
//			trainMaxLLDecrease = (curHmmLogger.trainSummary.maxLLDecrease > trainMaxLLDecrease) ? curHmmLogger.trainSummary.maxLLDecrease : trainMaxLLDecrease;
//			trainMaxLLDecreaseRatio = (curHmmLogger.trainSummary.maxLLDecreaseRatio > trainMaxLLDecreaseRatio) ? curHmmLogger.trainSummary.maxLLDecreaseRatio: trainMaxLLDecreaseRatio;
//			
//			if (curHmmLogger.trainSummary.nbParameterizingFailed == 1)
//				trainParameterizingFailed.add(curHmmLogger.hmmName);
			
			if (trainedParameters.size() == hmmId)
				trainedParameters.add(curHmmLogger.trainSummary.parameters);
			else
				throw new RuntimeException("ERROR: trainedParameters.size() != hmmId");
			
			if (testEvals.size() == hmmId){
				testEvals.add(curHmmLogger.testSummary.eval);
				//testEvals.get(testEvals.size() - 1).
			}
			else
				throw new RuntimeException("ERROR: testEvals.size() == hmmId");	
			testActualLabels.addAll(curHmmLogger.testSummary.actualLabels);
			testPredLabels.addAll(curHmmLogger.testSummary.predLabels);
			testPredProbs.addAll(curHmmLogger.testSummary.predProbs);
			
		}
		
		public int getFileId(){
			return fileId;
		}
		
		
		public ArrayList<Metrics> getTestEval(){
			return testEvals;
		}
		
		public ArrayList<LinkedHashMap<String, Double>> getTrainedParameters(){
			return trainedParameters;
		}
		
		public ArrayList<Integer> getActualLabels(){
			return testActualLabels;
		}
	
		public ArrayList<Integer> getPredLabels(){
			return testPredLabels;
		}
	
		public ArrayList<Double> getPredProbs(){
			return testPredProbs;
		}
		
		
//		public void printTrainSummary(String delimiter) throws IOException{
//			String str =  ("trainLL" + delimiter + "nBstopByEMIteration" + delimiter + "nbLLError" + delimiter + "maxLLDecrease" + delimiter + "maxLLDecreaseRatio" 
//					+ delimiter +  "nbParameterizingFailed") + "\n";
//			str += (formatter.format(trainLL) + delimiter + formatter.format(trainNbStopByEMIteration) + delimiter + formatter.format(trainNbLLError)
//					+ delimiter + formatter.format(trainMaxLLDecrease) + delimiter + formatter.format(trainMaxLLDecreaseRatio)
//					+ delimiter + trainParameterizingFailed.size());
//			if (trainParameterizingFailed.size() > 0){
//				str += "\nParameterizingFailed: ";
//				for (String f : trainParameterizingFailed)
//					str += f + delimiter;
//			}
//			printAndLog(str + "\n");
//		}
	}
	
	public class MultiFilesLogger{
		public ArrayList<Metrics> testMeanAcrossKcsMetrics = new ArrayList<Metrics>(); //one element correspond to one file
		public ArrayList<Metrics> testOverallAcrossKcsMetrics = new ArrayList<Metrics>(); //one element correspond to one file
		private Bijection trainHMMs = new Bijection();//The set of all unique HMMs appeared in all files
		
		public void update(Bijection oneFileHMMs){
			for (int i = 0; i < oneFileHMMs.getSize(); i++)
				trainHMMs.put(oneFileHMMs.get(i));
		}
		
		public Bijection getTrainHMMNames(){
			return trainHMMs;
		}
		
		public int getNbHMMs(){
			return trainHMMs.getSize();
		}
	}
	
	public DecimalFormat formatter;
	public Logger(DecimalFormat formatter){
		this.formatter = formatter;
	}

	
	public void printAndLog(String str) throws IOException{
		System.out.println(str);
		//if (opts.writeMainLog){
		mainLogWriter.write(str + "\n");
		mainLogWriter.flush();
	//		}
	}
	
	
	//TODO: arrange
	// TODO: just use ArrayList (Bijection to store restartId)
	/* order: features from unknown to known, type + outputILGSNameSurfix from unknown to known */
//	public HashSet<String> newStudents = new HashSet<String>();
//	public HashSet<String> newItems = new HashSet<String>();
//	public HashSet<String> upTillNowNewStudents = new HashSet<String>();
//	public HashSet<String> upTillNowNewItems = new HashSet<String>();
	/*
	 * String: kc name int[]: knownState judged by first datapoint in train; # different judgements by latter datapoints in train; # datapoints in train; knownState judged by first datapoint in test; # different judgements by latter datapoints in test; # datapoints in test;
	 * public HashMap<String, int[]> knowStateJudgements = new HashMap<String, int[]>();
	 * String: kc name double[]: 
	 */

}
