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

/**
 * Important Code Reminder:
 *   -- State index: When fitting HMM, index 0(1) is always corresponding to 0(1). 
 *   		-- For observed states, 1 always corresponds to "correct", because when reading input in DataPointList.java, the Bijection outcomes put "incorrect" first. Since the direct feature weights (without taking negative) in LR(LBFGS) predict Prob(Class=1), so using these feature weights we can get Prob("correct"). 
 *   		-- For hidden states, if allowForget=false, then 1 alway correpsond to "known". Similarly, direclty using feature weights in LR(LBFGS) predict Prob("known").
 *   -- Feature values and weights: In expanded feature names, "_hidden1" correspond to index 1 state.
 *   		-- For transition and emission features, the code expand feature by "feature_A", "feature_B",..., "feature_A_hidden1", "feature_B_hidden1",... 
 *   		-- For initial features, it doesn't differentiate hidden states. Both hidden states have the same feature vector length and values ("feature_A", "feature_B"...)
 *    	-- Currently in Pdf, only the hidden state=0 (i=0) maintains feature weights, because the code expanded features for both hiddenStates to train one logistic regression.
 *   		-- In output parameter files, "slip_XXX" means if you use this coefficient direclty into logistic function (1/(1+exp(-x)), you can get slip probability (without any negative tranformation). The same applies for other prefix.
 *   -- InstanceWeights:
 *   		-- For transition parameterization instanceWeights, the code expand the dataset by 4 times with 1st and 2nd -> prior time step hiddenState0, 3rd and 4th ->  prior time step hiddenState1;
 *   		-- For initial and emission parameterization instanceWeights, the code doubles the dataset with the first part corresponding to hiddenState0 and second part corresponding to hiddenState1.
 */


package fast.experimenter;

import java.io.BufferedReader;
import java.io.BufferedWriter;
//import java.io.File;
import java.io.FileInputStream;
//import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
//import java.text.DateFormat;
//import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
//import java.util.Map;
//import java.util.HashMap;
import java.util.LinkedHashMap;
import fast.common.Bijection;
import fast.common.Functions;
//import fast.common.Functions;
import fast.common.Stats;
import fast.common.Utility;
//import fast.common.Stats.ValueIndexSummary;
//import fast.data.DataPoint;
import fast.data.StudentList;
import fast.evaluation.*;
//import fast.experimenter.Logger.MultiFiles;
import fast.experimenter.Logger.OneFileLogger;
import fast.experimenter.Logger.OneRestartLogger;
//import fast.experimenter.Logger.OneRestartLogger;
//import fast.experimenter.Logger.*;
import fast.featurehmm.FeatureHMM;
import fast.featurehmm.Predictor;
import fast.featurehmm.Learner;
//import fast.evaluation.TestInfo;
import fig.exec.Execution;
//import fast.experiment.Log;

public class Runner implements Runnable {

	private Options opts;
	private Logger logger;
	
	public Runner() {
		opts = new Options();
		logger = new Logger(opts.formatter);
	}

	public static void main(String[] args) throws IOException {
		Runner runner = new Runner();
		// Opts opts = new Opts();
		if (args != null && args.length > 0) {
			if (args.length == 1)
				Execution.run(args, runner, runner.opts);
			else {
				System.out.println("ERROR: AUGUMENTS: ++input/your_configuration_file");
				System.exit(1);
			}
		}
		else if (args == null || args.length == 0) {
			runner.run();
		}
		else {
			System.out.println("ERROR: AUGUMENTS: ++input/your_configuration_file");
			System.exit(1);
		}
	}

	// This is the necessary function to call to start the whole program.
	public void run() {
		try {
			start();
			logger.curMultiFiles = logger.new MultiFilesLogger();
			for (int fileId = 0; fileId < opts.nbFiles; fileId++){
//      if (!opts.readOneHmmOneTime)
				runOneFileByReadingAllHMM(fileId); 
//			else
//				runOnebyOne(foldID, runID);
			}
			if (opts.nbFiles > 1){
				logger.curMultiFiles.update(logger.curFile.trainAllHMMs);
				writeAllFilesSummary();
			}
			close();
		}
		catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * By default, use runBatch to start the actual process
	 */
	public void runOneFileByReadingAllHMM(int fileId) throws IOException { //int foldId, int runId
		opts.configure(fileId, 0, "");
		logger.curFile =  logger.new OneFileLogger(fileId);

		Bijection trainKcs = new Bijection();
		ArrayList<StudentList> trainKcsStuLists  = getFormattedInputData(splitHMMs(opts.trainFile, trainKcs));
		logger.curFile.trainAllHMMs = trainKcs;
		
		Bijection testKcs = new Bijection();
		ArrayList<StudentList> testKcsStuLists = getFormattedInputData(splitHMMs(opts.testFile, testKcs));
		if (testKcsStuLists.size() > trainKcsStuLists.size() || testKcs.getSize() > trainKcs.getSize()) {
			System.out.println("The # of hmms(KCs) in testset is bigger than that in trainset! Please remove new KCs on testset!");
			System.exit(1);
		}
		opts.resetRandom(fileId, opts.nbRandomRestart, trainKcs.getSize());

		
		String fileProgress = "(" + Integer.toString(fileId + 1) + "/" + Integer.toString(opts.nbFiles) + ")";
		String str = "\n\n\n***** Starting new file: fileId=" + fileId + fileProgress + " *****\n#HMMs(train)=" + trainKcs.getSize() 
							+ "\n#HMMs(test)=" + testKcs.getSize() +"\n";
		logger.printAndLog(str);

		for (int hmmId = 0; hmmId < trainKcs.getSize(); hmmId++) {
			String hmmName = trainKcs.get(hmmId);
			if (!testKcs.contains(hmmName)){
				System.out.println("Testset doesn't conatin " + hmmName + "!");
				System.exit(1);
			}
			int curKcTestId = testKcs.get(hmmName);
			StudentList curKcTrainStuList = trainKcsStuLists.get(hmmId);
			StudentList curKcTestStuList = testKcsStuLists.get(curKcTestId);
			
			checkTrainTestFeatures(curKcTrainStuList.getAllFeatures(), curKcTestStuList.getAllFeatures());
			checkTrainTestFeatures(curKcTrainStuList.getInitFeatures(), curKcTestStuList.getInitFeatures());
			checkTrainTestFeatures(curKcTrainStuList.getTranFeatures(), curKcTestStuList.getTranFeatures());
			checkTrainTestFeatures(curKcTrainStuList.getEmitFeatures(), curKcTestStuList.getEmitFeatures());
			
			OneRestartLogger bestRestartLogger = null;
			if (opts.nbRandomRestart <= 1)
				bestRestartLogger = runOneHMMOneRestart(fileId, 0, hmmId, hmmName, curKcTrainStuList, curKcTestStuList, trainKcs);
			else
				bestRestartLogger = runOneHMMMultiRestarts(fileId, hmmId, hmmName, curKcTrainStuList, curKcTestStuList, trainKcs, opts.nbRandomRestart);
			
			logger.curFile.trainedHMMs.add(hmmName);
			int nbTrainedHmms = logger.curFile.trainedHMMs.size();
			writeOneHMMOneRestartSummary(curKcTestStuList, bestRestartLogger.testSummary, nbTrainedHmms);
		
			logger.curFile.add(hmmId, bestRestartLogger);
			
			//closeAndClearAllRestarts();
		}
		writeOneFileAllHMMSummary();//opts.allHmmsSummaryDir, opts.modelName);
		//TODO: release the memory space for logger.curFileLogger;
		//closeOneFileLogger();
	}

	
	// input should ordered by the hmm unit (e.g. skill)
	//public void runOnebyOne(int foldId, int runId) throws IOException {
		// String str = getConfigurationStr(foldId,runId);
		// System.out.println(str);
		// opts.writeLogFiles(str);
		// InputStream is = this.getClass().getClassLoader()
		// .getResourceAsStream(opts.trainFile);
		// if (is == null) {
		// is = new FileInputStream(opts.trainFile);
		// }
		// InputStreamReader isr = new InputStreamReader(is);
		// BufferedReader trainReader = new BufferedReader(isr);
		// BufferedReader testReader = new BufferedReader(
		// new FileReader(opts.testFile));
		// String line = "";
		// System.out.println("Loading train file: " + opts.trainFile + "\n"
		// + "Loading test file: " + opts.testFile);
		// String header = trainReader.readLine().trim();
		// String testHeader = testReader.readLine().trim();
		// if (!header.equals(testHeader)) {
		// System.out.println("trainHeader:" + header);
		// System.out.println("testHeader:" + testHeader);
		// System.out.println("ERROR: input files !header.equals(testHeader)");
		// System.exit(1);
		// }
		//
		// String[] headerColumns = header.split("\\s*[,\t]\\s*");// ("\\s*,\\t*\\s*");
		// int skillColumn = -1;
		// String reg = "KC.*";
		// String reg2 = "skill.*";
		// // assign KC column
		// if (!header.contains("KC") && !header.contains("skill")) {
		// System.out.println("Header doesn't contain KCs!");
		// if (header.contains("problem"))
		// reg = "problem";
		// else if (header.contains("step"))
		// reg = "step";
		// else {
		// System.out.println("Error: No problem or step!");
		// System.exit(1);
		// }
		// }
		// for (int i = 0; i < headerColumns.length; i++) {
		// if (headerColumns[i].matches("(?i)" + reg)
		// || headerColumns[i].matches("(?i)" + reg2))
		// skillColumn = i;
		// }
		// kc = 0;
		// String preSkill = "";
		// String curSkill = "";
		// String testLine = "";
		// String preSkillOnTest = "";
		// ArrayList<String> aHmmSequences = new ArrayList<String>();
		// ArrayList<String> aHmmSequencesOnTest = new ArrayList<String>();
		//
		// trainHmms = new ArrayList<FeatureHMM>();
		// while ((line = trainReader.readLine()) != null) {
		// String newLine = line.trim();
		// if (newLine.length() == 0)
		// continue;
		// // System.out.println(line);
		// String[] columns = line.split("\\s*[,\t]\\s*");// ("\\s*,\\t*\\s*");
		// if (columns.length <= skillColumn) {
		// System.out.println("Error: incorrect line!");
		// continue;
		// }
		// if (skillColumn == -1) {
		// System.out.println("Error: No KCs!");
		// System.exit(1);
		// }
		//
		// curSkill = columns[skillColumn].trim();
		// // one hmm
		// if (preSkill.equals("") || preSkill.equals(curSkill)) {
		// if (preSkill.equals(""))
		// aHmmSequences.add(header);
		// aHmmSequences.add(line);
		// preSkill = curSkill;
		// }
		// else {
		// // train one hmm using aHmmSequences
		// opts.currentKc = preSkill;
		// opts.currentKCId = kc;
		// opts.nowInTrain = true;
		// FeatureHMM trainHmm = trainOneHmm(wholeProcessRunId, foldId, runId, preSkill, kc,
		// aHmmSequences, null, null);
		// trainHmms.add(trainHmm);
		//
		// while ((testLine = testReader.readLine()) != null) {
		// String newLineOnTest = testLine.trim();
		// if (newLineOnTest.length() == 0)
		// continue;
		// columns = testLine.split("\\s*[,\t]\\s*");// ("\\s*,\\t*\\s*");
		// String curSkillOnTest = columns[skillColumn].trim();
		// if (!preSkillOnTest.equals("") && !preSkillOnTest.equals(preSkill)) {
		// System.out
		// .println("WARNING: !preSkillOnTest.equals(\"\") && !preSkillOnTest.equals(preSkill): preSkillOnTest="
		// + preSkillOnTest + "," + "preSkill" + preSkill);
		// System.exit(1);
		// }
		// if (!curSkillOnTest.equals(preSkill)
		// && aHmmSequencesOnTest.size() == 0) {
		// System.out
		// .println("WARNING: !curSkillOnTest.equals(preSkill) && aHmmSequencesOnTest.size() == 0: curSkillOnTest="
		// + curSkillOnTest + "," + "preSkill" + preSkill);
		// System.exit(1);
		//
		// }
		// if (preSkillOnTest.equals("")
		// || preSkillOnTest.equals(curSkillOnTest)) {
		// if (preSkillOnTest.equals(""))
		// aHmmSequencesOnTest.add(header);
		// aHmmSequencesOnTest.add(testLine);
		// preSkillOnTest = curSkillOnTest;
		// if (!preSkillOnTest.equals(preSkill)) {
		// System.out
		// .println("ERROR: !preSkillOnTest.equals(preSkill): preSkillOnTest="
		// + preSkillOnTest + ",preSkill=" + preSkill);
		// System.exit(1);
		// }
		// }
		// else {
		// if (!preSkillOnTest.equals(preSkill)) {
		// System.out
		// .println("ERROR: !preSkillOnTest.equals(preSkill): preSkillOnTest="
		// + preSkillOnTest + ",preSkill=" + preSkill);
		// System.exit(1);
		// }
		// opts.nowInTrain = false;
		// opts.newStudents = new HashSet<String>();
		// opts.newItems = new HashSet<String>();
		// predictOneHmmForOneByOneMode(wholeProcessRunId, foldId, runId, kc, preSkill, trainHmm,
		// aHmmSequencesOnTest);
		// aHmmSequencesOnTest.clear();
		// aHmmSequencesOnTest = new ArrayList<String>();
		// aHmmSequencesOnTest.add(header);
		// aHmmSequencesOnTest.add(testLine);
		// preSkillOnTest = curSkillOnTest;
		// EvaluationGeneral allFoldRunsEval = new EvaluationGeneral();
		// double overallAuc = allFoldRunsEval.evaluateOnMultiFiles(
		// (opts.nbRandomRestart > 1 ? currentrestartId + "_" : "") + opts.modelName, opts.nbRuns,
		// opts.nbFolds, opts.allModelComparisonOutDir, opts.outDir,
		// opts.evalFile, opts.predSuffix); // the first argument serves as a prefix to find files; while the last argument serves as a suffix to find files
		// opts.perProcessOverallAuc.put(currentrestartId, overallAuc);
		// break;
		// }
		// }// finish testing
		// aHmmSequences.clear();
		// aHmmSequences = new ArrayList<String>();
		// aHmmSequences.add(header);
		// aHmmSequences.add(line);
		// preSkill = curSkill;
		// kc++;
		// opts.nowInTrain = true;
		// }
		// }
		// trainReader.close();
		// if (aHmmSequencesOnTest != null & aHmmSequencesOnTest.size() > 0) {
		// // train one hmm using aHmmSequences
		// opts.currentKc = preSkill;
		// opts.currentKCId = kc;
		// FeatureHMM trainHmm = trainOneHmm(wholeProcessRunId, foldId, runId, curSkill, kc,
		// aHmmSequences, null, null);
		// trainHmms.add(trainHmm);
		// while ((testLine = testReader.readLine()) != null) {
		// String newLineOnTest = testLine.trim();
		// if (newLineOnTest.length() == 0)
		// continue;
		// String[] columns = testLine.split("\\s*[,\t]\\s*");// ("\\s*,\\t*\\s*");
		// String curSkillOnTest = columns[skillColumn].trim();
		// if (!curSkillOnTest.equals(preSkillOnTest)
		// || !curSkillOnTest.equals(curSkill)) {
		// System.out
		// .println("ERROR: !curSkillOnTest.equals(preSkillOnTest) || !curSkillOnTest.equals(curSkill): curSkillOnTest="
		// + curSkillOnTest
		// + ",preSkillOnTest="
		// + preSkillOnTest
		// + ",curSkill=" + curSkill);
		// System.exit(1);
		// }
		// aHmmSequencesOnTest.add(testLine);
		// }
		// predictOneHmmForOneByOneMode(wholeProcessRunId, foldId, runId, kc, preSkill, trainHmm,
		// aHmmSequencesOnTest);
		// EvaluationGeneral allFoldRunsEval = new EvaluationGeneral();
		// double overallAuc = allFoldRunsEval.evaluateOnMultiFiles(
		// (opts.nbRandomRestart > 1 ? currentrestartId + "_" : "") + opts.modelName, opts.nbRuns,
		// opts.nbFolds, opts.allModelComparisonOutDir, opts.outDir,
		// opts.evalFile, opts.predSuffix); // the first argument serves as a prefix to find files; while the last argument serves as a suffix to find files
		// opts.perProcessOverallAuc.put(currentrestartId, overallAuc);
		// aHmmSequencesOnTest.clear();
		// aHmmSequences.clear();
		// }
		// testReader.close();
	//}

	// TODO: The design of fileId is weird
	public OneRestartLogger runOneHMMMultiRestarts(int fileId, int hmmId, String hmmName, 
															StudentList curKcTrainStuList, StudentList curKcTestStuList, Bijection trainHMMs,
															int nbRandomRestart) throws IOException{
		  logger.curMultiRestarts = logger.new MultiRestartsLogger(hmmName);
			for (int restartId = 0; restartId < nbRandomRestart; restartId++){
				runOneHMMOneRestart(fileId, restartId, hmmId, hmmName, curKcTrainStuList, curKcTestStuList, trainHMMs);
				logger.curMultiRestarts.add(logger.curRestart);
				//logger.printAndLog("\n\nWARNING: The code currently doesn't output multiple restart results. We will implement later!");
			}
			int bestRestartId = logger.curMultiRestarts.getBestRestartId();
			logger.printAndLog("\n\n***** Choose restartId=" + bestRestartId + " *****\n\n");
			OneRestartLogger oneRestartLogger = logger.curMultiRestarts.getLogger(bestRestartId);
			return oneRestartLogger;
	}
	

	/** Actual function to do one restart train and test per KC */
	public OneRestartLogger runOneHMMOneRestart(int fileId, int restartId, int hmmId, String hmmName, 
										StudentList curKcTrainStuList, StudentList curKcTestStuList, Bijection trainHMMs) throws IOException {

		opts.configure(fileId, restartId, hmmName);
		logger.curRestart = logger.new OneRestartLogger(restartId, hmmName);
		logger.curRestart.trainSummary.allFeatures = curKcTrainStuList.getAllFeatures();
		logger.curRestart.trainSummary.initFeatures = curKcTrainStuList.getInitFeatures();
		logger.curRestart.trainSummary.tranFeatures = curKcTrainStuList.getTranFeatures();
		logger.curRestart.trainSummary.emitFeatures = curKcTrainStuList.getEmitFeatures();
		
		String fileProgress = "fileId=" + fileId + "(" + (fileId + 1) + "/" + opts.nbFiles + ")";
		String restartProgress = (opts.nbRandomRestart > 1) ? (", restartId=" + restartId + "(" + (restartId + 1) + "/" + opts.nbRandomRestart + ")"):"";
		String hmmProgress = ", kc=" + hmmName + "(" + (trainHMMs.get(hmmName) + 1) + "/" + trainHMMs.getSize() + ")";
//		for (String kc : logger.curFileLogger.trainedHmms)
//			System.out.println(kc);
		String progress = fileProgress + hmmProgress + restartProgress;
		
		String str = "\n\n***** Training: " + progress + " *****";
		logger.printAndLog(str);
		FeatureHMM trainHmm = trainOneHMMOneRestart(restartId, hmmName, hmmId, curKcTrainStuList);

		str = "\n***** Testing: " + progress + " *****";
		logger.printAndLog(str);
		testOneHMMOneRestart(hmmName, trainHmm, curKcTestStuList); //TestSummary testSummary = 

		str = "\n***** Evaluating: " + progress + " *****";
		logger.printAndLog(str);
		evaluateOneHMMOneRestart(trainHmm, curKcTrainStuList);
	
		return logger.curRestart;
	}
	
	public void writeOneHMMOneRestartSummary(StudentList curKcTestStuList, TestSummary testSummary, int nbHMMTrained) throws IOException{
		//int nbHMMTrained = logger.curFile.trainedHmms.size();
		boolean append = (nbHMMTrained <= 1) ? false : true;
		boolean printHeader = (nbHMMTrained <= 1) ? true : false;
		Predictor.writeHMMPrediction(opts.predictionFile, curKcTestStuList, testSummary, append, printHeader);
	}
	
	/** This function doesn't write to evaluation file, it just saves objects and do printAndLog. We only write to evaluation file when evaluating all HMMs */
	public void evaluateOneHMMOneRestart(FeatureHMM trainHMM, StudentList curKcTrainStuList) throws IOException{
		String hmmName = logger.curRestart.getHMMName();
		String header = "";
		String evalStr = "";
		String delimiter = ",";
		
		/** Predictive Performance (test summary) */
		PredictivePerformance evaluation = new PredictivePerformance();
		//evaluation.setFormatter(opts.formatter);
		Metrics eval = evaluation.evaluateClassifier(logger.curRestart.testSummary.actualLabels, 
																								logger.curRestart.testSummary.predLabels, 
																								logger.curRestart.testSummary.predProbs, hmmName);
		logger.predictionMetrics = new Bijection(eval.getMetricNames());

		// TODO:  keep on cleaning
//		Degeneracy deg_obj = new Degeneracy();
//		String outStr += "\ndegeneracyJudgements:\nkc" + delimiter + "%degeneracy_in_train" + delimiter + "(#deg/#train)" + delimiter + "%degeneracy_in_test" + delimiter + "(#deg/#test)\n";
//		// int nbDegKcs = 0;
//		double nbDegDps = 0.0;// overall KCs
//		double nbDps = 0.0; // overall KCs
//		// double pctDegDps = 0.0;
//		double guessPlusSlipAvgPerDP = 0.0;
//		// for (Map.Entry<String, double[]> kc_known_stat : degeneracyJudgements.entrySet()){
//		String kc = hmmName;// kc_known_stat.getKey();
//		double[] stat = logger.curRestartLogger.degeneracy.degeneracyJudgementsAcrossDataPoints;// kc_known_stat.getValue();
//		double train_deg_pct = (stat[0] / (1.0 * stat[1]));
//		double test_deg_pct = (stat[2] / (1.0 * stat[3]));
//		outStr += kc + delimiter + opts.shortFormatter.format(train_deg_pct) + delimiter + "(" + stat[0] + "/" + stat[1] + ")" + delimiter 
//							+ opts.shortFormatter.format(test_deg_pct) + delimiter + "(" + stat[2] + "/" + stat[3] + ")\n";
//		// if (train_deg_pct > 0 || test_deg_pct > 0)
//		// nbDegKcs += 1;
//		nbDegDps += stat[0] + stat[2];
//		nbDps += stat[1] + stat[3];
//		// pctDegDps += (stat[0] + stat[2]) / (1.0 * (stat[1] + stat[3]));
//		// sumGuessSlip += stat[4] + stat[5]; //overall KCs' datapoints
//		guessPlusSlipAvgPerDP += (stat[4] + stat[5]) / (1.0 * (stat[1] + stat[3]));
//		// }
//		outStr += // "Total_#deg_kcs(train+test)" + delimiter + nbDegKcs + "\n" +
//		"Total_%deg_dps(train+test)" + delimiter + (nbDegDps / (1.0 * nbDps)) + "\n";
//		// perProcessNbDegenerate.put(restartId, nbDegKcs);//overall
//		// perProcessPctDpDegenerate.put(restartId, nbDegDps / (1.0 * nbDps));//overall
//		// deg_obj.overallNbDegKcs = nbDegKcs;
//		// deg_obj.overallPctDegDps = nbDegDps / (1.0 * nbDps);
//		// deg_obj.avgPerKcPctDegDps = pctDegDps;
//		deg_obj.pctDegDps = nbDegDps / (1.0 * nbDps);
//		// deg_obj.avgPerDpGuessPlusSlip = sumGuessSlip / (1.0 * nbDps);
//		// deg_obj.avgPerKcGuessPlusSlipAvgPerDP = sumGuessSlip / (1.0 * nbKcTrained);
//		deg_obj.guessPlusSlipAvgPerDP = guessPlusSlipAvgPerDP;
//		deg_obj.pctDecProbKnown = stat[6];
//		deg_obj.pctDecProbCorrect = stat[7];
//		deg_obj.minGuessPlusSlipPerDpOnTrain = stat[8];
//		deg_obj.minGuessPlusSlipPerDpOnTest = stat[9];
//		// deg_obj.nbDegKcsBasedOnGuessPlusSlipFeatureOff = nbDegKcs;
//
//		HashMap<String, Double> featureToValue =	logger.curMultiRestartLogger.featureToValue.get(restartId);
//		// compute g+s
//		// if (oneProcessPerFeatureAllKcFeatureValues.get("guess").get(0) != -1.0 && oneProcessPerFeatureAllKcFeatureValues.get("slip").get(0) != -1.0)
//		// deg_obj.avgPerKcFeatureOffGuessPlusSlip = Stats.mean(oneProcessPerFeatureAllKcFeatureValues.get("guess")) + Stats.mean(oneProcessPerFeatureAllKcFeatureValues.get("slip"));
		
		LinkedHashMap<String, Double> parameters =	logger.curRestart.trainSummary.parameters;
		Degeneracy degObj = logger.curRestart.testSummary.degeneracy;
		
		if (parameters.get("guess") != -1.0  && parameters.get("slip") != -1.0)
			degObj.guessPlusSlipFeatureOff = parameters.get("guess") + parameters.get("slip");
		else {
//			ArrayList<String> guessFeatureNames = new ArrayList<String>();
//			ArrayList<String> slipFeatureNames = new ArrayList<String>();
//			for (Map.Entry<String, Double> parameter : parameters.entrySet()) {
//				String featureName = parameter.getKey();
//				if (featureName.contains("features") && !featureName.contains("bias"))
//					if (featureName.contains("guess"))
//						guessFeatureNames.add(featureName);
//					else if (featureName.contains("slip"))
//						slipFeatureNames.add(featureName);
//			}
			// guess off: bias
			double sumGuessOff = 0.0,  sumSlipOff = 0.0;
			if (parameters.containsKey("guess_bias")) {
				sumGuessOff += Functions.logistic(parameters.get("guess_bias"));
//				if (guessFeatureNames.size() == 1)
//					sumGuessOn += Functions.logistic(parameters.get("guess_bias") + parameters.get(guessFeatureNames.get(0)));
			}
			else
				System.out.println("WARNING: !parameters.contains(guess_bias)");
			if (parameters.containsKey("slip_bias")) {
				sumSlipOff += Functions.logistic(parameters.get("slip_bias"));
//				if (slipFeatureNames.size() == 1)
//					sumSlipOn += Functions.logistic(parameters.get("slip_bias") + parameters.get(slipFeatureNames.get(0)));
			}
			else
				System.out.println("WARNING: !parameters.contains(slip_bias)");
//			
//			sumGuessOff /= nbTrainedHmms; //nbHmmTrained;
//			sumSlipOff /= nbTrainedHmms;//nbHmmTrained;
//			if (guessFeatureNames.size() == 1)
//				sumGuessOn /=  nbTrainedHmms;//nbHmmTrained;
//			if (slipFeatureNames.size() == 1)
//				sumSlipOn /=  nbTrainedHmms;//nbHmmTrained;
//			// deg_obj.avgPerKcFeatureOffGuessPlusSlip = sumGuessOff + sumSlipOff;
//			// deg_obj.avgPerKcFeatureOnGuessPlusSlip = sumGuessOn + sumSlipOn;
			degObj.guessPlusSlipFeatureOff = sumGuessOff + sumSlipOff;
//			if (sumGuessOn != -1.0 && sumSlipOn != -1.0)
//				deg_obj.guessPlusSlipFeatureOn = sumGuessOn + sumSlipOn;
//		}
		}
		if ((degObj.guessPlusSlipFeatureOff >= 1 && degObj.degeneracyJudgementInequality.equals("be")) 
				|| (degObj.guessPlusSlipFeatureOff > 1 && degObj.degeneracyJudgementInequality.equals("b")))
			degObj.nbDegKcsBasedOnGuessPlusSlipFeatureOff = 1.0;
		else
			degObj.nbDegKcsBasedOnGuessPlusSlipFeatureOff = 0.0;
		
		eval.setMetricValue("G+S_featureOff(plausibility)", degObj.guessPlusSlipFeatureOff);
		eval.setMetricValue("G+S<1_featureOff(plausibility)", (1.0 - degObj.nbDegKcsBasedOnGuessPlusSlipFeatureOff));
		TrainSummary trainSummary = logger.curRestart.trainSummary;
		eval.setMetricValue("LL(train)", trainSummary.trainLL);
		eval.setMetricValue("NbStopByEMIteration(train)", (double) trainSummary.nbStopByEMIteration);
		eval.setMetricValue("NbLLError(train)", (double) trainSummary.nbLLError);
		eval.setMetricValue("MaxLLDecreaseRatio_diffByavg(train)", trainSummary.maxLLDecreaseRatio);
		eval.setMetricValue("NbParameterizingFailed(train)", (double) trainSummary.nbParameterizingFailed);
		
		header = eval.getHeader(delimiter);
		evalStr = eval.getEvaluationStr(delimiter);
		logger.printAndLog(header + "\n" + evalStr);

		logger.metrics = new Bijection(eval.getMetricNames());
		logger.curRestart.testSummary.eval = eval;	
		
//		header = logger.curRestart.trainSummary.getHeader(",");
//		evalStr = logger.curRestart.trainSummary.getEvaluationStr(",", opts.shortFormatter); 
//		logger.printAndLog(header + "\n" + evalStr + "\n");
	}
//
//		// perProcessDegeneracy.put(restartId, deg_obj);
//		logger.curRestartLogger.degeneracy = deg_obj;
//		//curHmmPerProcessDegeneracy.put(restartId, deg_obj);
//		// }
//		// }

//outStr = "";
//String delimiter = ",";
//// if (getAucOnDevPerKc)
//// System.out.println("\nbestAucOnDevAllKcsSum="
//// + bestAucOnDevAllKcsSum);
//Date endDate = new Date();
//double diffSec = ((endDate.getTime() - curRestartStartDate.getTime())) / (1.0 * 1000);
//// perProcessTrainTestTime.put(restartId, diffSec);
//// perProcessRunNbKcsStopByEMIteration.put(restartId, nbKcsStopByEMIteration);
//curHmmPerProcessTrainTestTime.put(restartId, diffSec);
//logger.urRestartLogger.put(restartId, curHmmStopByEMIteration);
// nbKcsStopByEMIteration = 0;

//
//		if (opts.computeStatForParameters) {
//			// HashMap<String, Double> perFeatureCoefficent = new HashMap<String, Double>();
//			String meanStr = "mean" + delimiter, minStr = "min" + delimiter, maxStr = "max" + delimiter, nbNegStr = "#neg" + delimiter, sdStr = "sd" + delimiter;
//			// TODO: correspond to BaumWelchLearner writeHmm();
//			if (opts.outputPerHMM) {
//				String[] types = { "init" + opts.outputILGSNameSurfix, "learn" + opts.outputILGSNameSurfix, "guess" + opts.outputILGSNameSurfix, 
//						"slip" + opts.outputILGSNameSurfix };
//				for (String type : types) {
//					String oriType = type.split("_")[0];
//					if (featureToValue.get(oriType) != -1.0)
//						featureToValue.put(type, featureToValue.get(oriType));
//					else {
//						double coefficient = 0.0, value = 0.0;
//						if (opts.outputILGSNameSurfix.equals("_feature_off")) {
//							if (featureToValue.containsKey(oriType + "_bias")) {
//								coefficient = featureToValue.get(oriType + "_bias");
//								value = Functions.logistic(coefficient);
//							}
//						}
//						featureToValue.put(type, value);
//					}
//				}
//			}
//			else {
//				for (int i = 0; i < curHmmOutputParameterNames.getSize(); i++) {
//					String name = curHmmOutputParameterNames.get(i);
//					double mean = 0.0, max = 0.0, min = 0.0, nbNeg = 0.0, sd = 0.0;
//					ArrayList<Double> stats = logger.curFileLogger.featureToAllKCValues.get(name);
//					// ArrayList<Double> stats = new ArrayList<Double>();
//					// stats.add(statistics);
//					if (name.contains("knownState") || (!opts.allowForget && name.contains("forget"))) {
//						mean = stats.get(0);
//						min = mean;
//						max = mean;
//						nbNeg = 0.0;
//						sd = 0.0;
//					}
//					else {
//						mean = Stats.mean(stats);
//						min = Stats.min(stats);
//						max = Stats.max(stats);
//						nbNeg = Stats.countLessThan(stats, 0);
//						sd = Stats.sd(stats);
//					}
//					if (i + 1 < curHmmOutputParameterNames.getSize()) {
//						meanStr += mean + delimiter;
//						minStr += min + delimiter;
//						maxStr += max + delimiter;
//						nbNegStr += nbNeg + delimiter;
//						sdStr += sd + delimiter;
//					}
//					else {
//						meanStr += mean;
//						minStr += min;
//						maxStr += max;
//						nbNegStr += nbNeg;
//						sdStr += sd;
//					}
//				}
////				finalParametersWriter.write(meanStr + "\n");
////				finalParametersWriter.write(maxStr + "\n");
////				finalParametersWriter.write(minStr + "\n");
////				finalParametersWriter.write(nbNegStr + "\n");
////				finalParametersWriter.write(sdStr + "\n");
//			}
//		}
//
////		if (parameterizingFailedHmms.size() > 0) {
////			outStr += "\nException Summary:\nhmmsForcedToNonParmTrainDueToLBFGSException:\t";
////			for (String hmmName : parameterizingFailedHmms) {
////				outStr += hmmName + delimiter;
////			}
////			outStr += "\n";
////		}
//		logger.printAndLog(outStr);
//		BufferedWriter evalFileWriter = new BufferedWriter(new FileWriter(opts.evalFile, true));
//		evalFileWriter.write(outStr);
//		evalFileWriter.close();
		
		//logger.closeAndClearOneRestart();
//		if (opts.writeFinalParameters)
//			finalParametersWriter.close();
//		
//		//TODO: use a class
//		curHmmPerRestartAuc = new TreeMap<Integer, Double>();
//		curHmmPerProcessDegeneracy = new TreeMap<Integer, Degeneracy>();
//		curHmmPerRestartLL = new TreeMap<Integer, Double>();
//		curHmmPerProcessTrainTestTime = new TreeMap<Integer, Double>();
//		curHmmPerProcessMastery = new TreeMap<Integer, Mastery>();
//		// curHmmCurProcessPerParameterValue = new HashMap<String, Double>();//feature to values (ordered by kc)
//		curHmmPerProcessPerParameterValue = new TreeMap<Integer, HashMap<String, Double>>();
//		curHmmPerProcessStopByEMIteration = new HashMap<Integer, Integer>();
//		curHmmCurProcessDegeneracyJudgementsAcrossDataPoints = new double[10];

	///** For runOnebyOne setting, this differs from predictOneHmm because it receives foldId, runId, kc, hmmName */
	// public void predictOneHmmForOneByOneMode(int wholeProcessRunId, int foldId, int runId, int kc,
	// String hmmName, FeatureHMM trainHmm, ArrayList<String> aHmmSequences)
	// throws IOException {
	// String str = "\n\n******** Testing: kc=" + kc + ", skill=" + hmmName
	// + ", wholeProcessRunId=" + wholeProcessRunId + ", foldID=" + foldId + ", runID=" + runId + " ************";
	// System.out.println(str);
	// if (opts.writeMainLog) {
	// opts.mainLogWriter.write(str + "\n");
	// opts.mainLogWriter.flush();
	// }
	// opts.nowInTrain = false;
	// ArrayList<Double> probs = new ArrayList<Double>();
	// ArrayList<Integer> labels = new ArrayList<Integer>();
	// ArrayList<Integer> actualLabels = new ArrayList<Integer>();
	// ArrayList<Double> pKnownPrior = new ArrayList<Double>();
	// ArrayList<Double> pKnownPosterior = new ArrayList<Double>();
	// //ArrayList<Integer> trainTestIndicators = new ArrayList<Integer>();
	// ArrayList<String> students = new ArrayList<String>();
	// ArrayList<String> kcs = new ArrayList<String>();
	// ArrayList<double[]> features = new ArrayList<double[]>();
	// predictOneHmm(trainHmm, hmmName, aHmmSequences, probs, labels, actualLabels, pKnownPrior, pKnownPosterior,
	// students, kcs, features); //trainTestIndicators,
	// }



	public String getConfigurationStr() throws IOException {
		String str = "\n***** Configuration *****" + "\nmodelName=" + opts.modelName

				+ "\ntrain=" + opts.trainFile + "\ntest=" + opts.testFile + "\nnbFiles=" + opts.nbFiles + "\tnbRandomRestarts=" + opts.nbRandomRestart// + "\ttestSingleFile=" + opts.testSingleFile 
				+ "\nallowForget="+(opts.allowForget?"true":"false")
				+ "\nparameterizing=" + opts.parameterizing + "\tparameterizingInit=" + opts.parameterizingInit + "\tparameterizingTran=" + opts.parameterizingTran + "\tparameterizingEmit=" + opts.parameterizingEmit + "\tforceUsingInputFeature=" + opts.forceUsingAllInputFeatures
				+ "\ninitialK0=" + opts.initialK0 + "\tinitialT=" + opts.initialT + "\tinitialG" + opts.initialG + "\tinitialS" + opts.initialS 
				+ "\tinitialFeatureWeightsBounds=" + (opts.initialFeatureWeightsBounds * -1.0) + "," + opts.initialFeatureWeightsBounds
				+ "\nEMTolerance=" + opts.EMTolerance + "\tEMMaxIters=" + opts.EMMaxIters + "\tuseBaumWelchScaledLearner=" + opts.useBaumWelchScaledLearner
				+ "\tACCEPTABLE_LL_DECREASE=" + opts.ACCEPTABLE_LL_DECREASE
				+ "\nLBFGSTolerance=" + opts.LBFGSTolerance + "\tLBFGSMaxIters=" + opts.LBFGSMaxIters
				+ "\nuseReg=" + opts.useReg + "\tLBFGSRegWeight=" + opts.LBFGSRegWeight + "\tLBFGSRegBias=" + opts.LBFGSRegBias
				+ "\nbias=" + opts.bias;// + "\tdifferentBias=" + opts.differentBias;
		logger.printAndLog(str);
		return str;
	}
	
	public FeatureHMM trainOneHMMOneRestart(int restartId, String hmmName, int hmmId, StudentList curKcStuList) throws IOException {
		//logger.nowInTrain = true;
		//logger.nowInTrain = true;
		String str = "#attempts(records)=" + (curKcStuList.getNbDataPoints()) + "\n#students(seqs)=" + curKcStuList.size() + "\n" + "#items(questions)=" 
								+ curKcStuList.getProblems().getSize() + "\n" + (((curKcStuList.getInitFeatures() != null) ? "#finalInitFeatures=" 
									+ curKcStuList.getInitFeatures().getSize() : "#finalInitFeatures=0") + ", " 
										+ ((curKcStuList.getTranFeatures() != null) ? "#finalTranFeatures=" 
											+ curKcStuList.getTranFeatures().getSize() : "#finalTranFeatures=0") + ", "
												+ ((curKcStuList.getEmitFeatures() != null) ? "#finalEmitFeatures=" + curKcStuList.getEmitFeatures().getSize() : "#finalEmitFeatures=0"));
		logger.printAndLog(str);
		
		Learner trainer = new Learner(opts.Nb_HIDDEN_STATES, opts.NB_OBS_STATES, 
																	opts.parameterizing, opts.parameterizingInit, opts.parameterizingTran, opts.parameterizingEmit,
																	opts.allowForget, 
																	opts.PROBABILITY_MIN_VALUE, opts.LBFGSRegWeight, opts.LBFGSRegBias,
																	opts.initialK0, opts.initialT, opts.initialG, opts.initialS,
																	opts.featureWeightsRands[restartId][hmmId], opts.initialFeatureWeightsBounds,
																	opts.nonFeatureParasRands[restartId][hmmId],
																	opts.useBaumWelchScaledLearner,
																	opts.EMTolerance, opts.EPS, opts.ACCEPTABLE_LL_DECREASE, opts.EMMaxIters,
																	opts.LBFGSTolerance, opts.LBFGSMaxIters);
		FeatureHMM hmm = trainer.train(curKcStuList);
		
		logger.curRestart.trainSummary.update(trainer.getTrainLL(), trainer.getNbStopByEMIteration(), trainer.getNbLLError(), trainer.getMaxLLDecrease(), trainer.getMaxLLDecreaseRatio(), trainer.getNbParameterizingFailed());
		//logger.curRestart.printTrainSummary();
		if (opts.getDataPointRelatedDegeneray)
			logger.curRestart.getTrainsetDegeneray(hmm, curKcStuList, opts.allowForget, opts.judgeHiddenByEmit, opts.degInequalityStr);
		//if (opts.writeParameters || opts.computeStatForParameters)
		getOneHMMParameters(hmm, FeatureHMM.getKnownState(hmm, curKcStuList.get(0).get(0), opts.judgeHiddenByEmit, opts.allowForget), 
												logger.curRestart.trainSummary.parameters);
		return hmm;
	}
	

	public ArrayList<ArrayList<String>> splitHMMs(String filename, Bijection kcs) throws IOException {

		ArrayList<ArrayList<String>> allKcArrayList = new ArrayList<ArrayList<String>>();
		InputStream is = this.getClass().getClassLoader().getResourceAsStream(filename);
		if (is == null) {
			is = new FileInputStream(filename);
		}
		InputStreamReader isr = new InputStreamReader(is);
		BufferedReader br = new BufferedReader(isr);
		String line = null;

		String header = br.readLine();
		String[] headerColumns = header.split("\\s*[,\t]\\s*");// ("\\s*,\\t*\\s*");
		int skillColumn = -1;//, stuColumn = -1;
		String reg = "KC.*";
		String reg2 = "skill.*";
		//String reg3 = "student.*";
		if (!header.contains("KC") && !header.contains("skill")) {
			System.out.println("Header doesn't contain KCs!");
			if (header.contains("problem"))
				reg = "problem";
			else if (header.contains("step"))
				reg = "step";
			else {
				System.out.println("Error: No problem or step!");
				System.exit(1);
			}
		}
		for (int i = 0; i < headerColumns.length; i++) {
			if (headerColumns[i].matches("(?i)" + reg) || headerColumns[i].matches("(?i)" + reg2))
				skillColumn = i;
//			if (headerColumns[i].matches("(?i)" + reg3))
//				stuColumn = i;
		}
		// ArrayList<ArrayList<String>> hmmsSequences = new
		// ArrayList<ArrayList<String>>();
		/* this is for input that not neccessarily order by skill, but records within a student should be ordered */
		while ((line = br.readLine()) != null) {
			String newLine = line.trim();
			if (newLine.length() == 0)
				continue;
			// System.out.println(line);
			String[] columns = line.split("\\s*[,\t]\\s*");// ("\\s*,\\t*\\s*");
			if (columns.length <= skillColumn) {
				System.out.println("Error: incorrect line!");
				continue;
			}
			if (skillColumn == -1) {
				System.out.println("Error: No KCs!");
				System.exit(1);
			}
//			if ((opts.stusToSkip != null && opts.stusToSkip.size() > 0 & stuColumn == -1)) {
//				System.out.println("Error: No Students!");
//				System.exit(1);
//			}

			String aSkill = columns[skillColumn].trim();
//			if (opts.skillsToSkip != null && opts.skillsToSkip.size() > 0 && opts.skillsToSkip.contains(aSkill)) {
//				continue;
//			}
//			if (opts.stusToSkip != null && opts.stusToSkip.size() > 0) {
//				String aStu = columns[stuColumn].trim();
//				if (opts.stusToSkip.contains(aStu)) {
//					// System.out.println(aStu);
//					continue;
//				}
//			}
			int skillID = kcs.put(aSkill);
			if (allKcArrayList.size() < skillID) {
				System.out.println("Error: skill size mismatch!");
				System.exit(1);
			}
			if (allKcArrayList.size() > skillID) {// existed skillID
				ArrayList<String> hmmSeqs = allKcArrayList.get(skillID);
				if (hmmSeqs == null) {
					hmmSeqs = new ArrayList<String>();
					hmmSeqs.add(header);
					hmmSeqs.add(line);
					// StudentList curStuList = StudentList.loadData(hmmSeqs, opts);
					// allKcStuLists.add(curStuList);
				}
				else
					hmmSeqs.add(line);
			}
			else {// a new skillID
				ArrayList<String> hmmSeqs = new ArrayList<String>();
				// TODO: now still pass header since used original code to setcolumns
				hmmSeqs.add(header);
				hmmSeqs.add(line);
				allKcArrayList.add(hmmSeqs);
				// StudentList curStuList = StudentList.loadData(hmmSeqs, opts);
				// allKcStuLists.add(curStuList);
			}
		}
		br.close();
		return allKcArrayList;

		// return hmmsSequences;
	}

	
	public ArrayList<StudentList> getFormattedInputData(ArrayList<ArrayList<String>> allKcArrayList){
		ArrayList<StudentList> allKcStuLists = new ArrayList<StudentList>();
		for (ArrayList<String> kcArrayList : allKcArrayList) {
			StudentList curStuList = new StudentList(kcArrayList, opts.parameterizing, opts.parameterizingInit, opts.parameterizingTran, opts.parameterizingEmit,
					opts.forceUsingAllInputFeatures, opts.bias, opts.Nb_HIDDEN_STATES);//opts.differentBias, 
			allKcStuLists.add(curStuList);
		}
		return allKcStuLists;
	}

	/** By default, here is the actual function to start predicting for one fold, one skill for all kinds of API */
	public void testOneHMMOneRestart(String hmmName, FeatureHMM curKcTrainHmm, StudentList curKcTestStuList) throws IOException {// ArrayList<Integer> trainTestIndicators,
		String str = "#attempts(records)=" + (curKcTestStuList.getNbDataPoints() - 1) + "\n" + "#students(sequences)=" + curKcTestStuList.size() + "\n" 
									+ "#items(questions)=" + curKcTestStuList.getProblems().getSize() + "\n" 
									+ (((curKcTestStuList.getInitFeatures() != null) ? "#finalInitFeatures=" 
									+ curKcTestStuList.getInitFeatures().getSize() : "#finalInitFeatures=0") + ", " 
									+ ((curKcTestStuList.getTranFeatures() != null) ? "#finalTranFeatures=" 
									+ curKcTestStuList.getTranFeatures().getSize() : "#finalTranFeatures=0") + ", " 
									+ ((curKcTestStuList.getEmitFeatures() != null) ? "#finalEmitFeatures=" 
									+ curKcTestStuList.getEmitFeatures().getSize() : "#finalEmitFeatures=0") + "\n");
		logger.printAndLog(str);
		

		Predictor tester = new Predictor(opts.judgeHiddenByEmit, opts.allowForget, 
							(opts.getDataPointRelatedDegeneray?logger.curRestart.testSummary.degeneracy:null), 
							(opts.getMastery?logger.curRestart.testSummary.mastery:null));
		tester.test(curKcTrainHmm, curKcTestStuList);// , lineID, hmmName); trainTestIndicators,
		logger.curRestart.testSummary.update(tester.getActualLabels(), tester.getPredLabels(), tester.getPredProbs(), tester.getPriorProbKnowns(), tester.getPosteriorProbKnowns());
		//return tester.getTestSummary();
	}


	public void checkTrainTestFeatures(Bijection trainFeatures, Bijection testFeatures) {
		if (trainFeatures.getSize() != testFeatures.getSize()) {
			System.out.println("ERROR:trainFeatures.getSize()=" + trainFeatures.getSize() + ",testFeatures.getSize()=" + testFeatures.getSize());
			System.exit(1);
		}
		for (int index = 0; index < trainFeatures.getSize(); index++) {
			String featureName = testFeatures.get(index);
			if (!featureName.equals(testFeatures.get(index))) {
				// TODO: 1) need to use Bijection trainFeatures' order to re-arrange
				// featureVector, only use train feature vector's length (meaning
				// thatextra features on test will be set 0 and trimed)
				System.out.println("ERROR:trainFeatures.get(index) != testFeatures.get(index)) ");
				System.exit(1);
			}
		}
	}

	
	/* Return: allParameters */
	public void getFormatedFeatureMapping(Bijection aTypeFeatures, double[] aTypeFeatureWeights, 
									LinkedHashMap<String, Double> allParameters, 
									String type, int knownState){//String[] featureStrings, 
		if (aTypeFeatures.getSize() != aTypeFeatureWeights.length) {
			System.out.println("ERROR: features.getSize() !=featureWeights.length!");
			System.exit(1);
		}
	
//		String knownFeatureNameString = "";
//		String unknownFeatureNameString = "";
//		String knownFeatureValueString = "";
//		String unknownFeatureValueString = "";
		ArrayList<String> knownFeatureNames = new ArrayList<String>();
		ArrayList<String> unknownFeatureNames = new ArrayList<String>();
		ArrayList<Double> knownFeatureValues = new ArrayList<Double>();
		ArrayList<Double> unknownFeatureValues = new ArrayList<Double>();
		
		for (int i = 0; i < aTypeFeatures.getSize(); i++) {
			String featureName = aTypeFeatures.get(i);
			double featureCoefficient = aTypeFeatureWeights[i];
			if (featureName.contains("*feature")) {
//				System.out.println("ERROR: Sorry current code doesn't support * in feature name yet!");
//				System.exit(-1);
				featureName = (featureName.startsWith(type)? featureName : type + "_" + featureName);
				knownFeatureNames.add(featureName);
				unknownFeatureNames.add(featureName);
				knownFeatureValues.add(featureCoefficient);
				unknownFeatureValues.add(featureCoefficient);
				//featurePrinted = true;
			}
			else {
				/* Current feature weights are for known state used to predict 1 class(state). */
				if ((featureName.contains("_hidden1") && knownState == 1) || (!featureName.contains("_hidden1") && knownState == 0)){
					featureName = featureName.replace("_hidden1", "");
					if (type.equals("init")){
						if (featureName.contains("_hidden1")){
							System.out.println("ERROR: init feature names shouldn't contain _hidden1 surfix");
							System.exit(-1);
						}
						if (knownState != 0){
							System.out.println("ERROR: knownState should be 0");
							System.exit(-1);
						}
						/* knownState=0. Direct feature coef is for predicting 1, which is unknown state. */
						featureName =  (featureName.startsWith(type)? featureName : type + "_" + featureName);
						featureCoefficient = (-1.0) * featureCoefficient;
						knownFeatureNames.add(featureName);
						knownFeatureValues.add(featureCoefficient);
						//featurePrinted = true;
					}
					if (type.equals("tran") && opts.allowForget){
							/* currently we only support tran_ prefix*/
							featureName = "forget_" + featureName;// + (featureName.startsWith(type)? featureName : type + "_" + featureName);
							knownFeatureNames.add(featureName);
							if (knownState == 1){
								featureCoefficient = (-1.0) * featureCoefficient;
								knownFeatureValues.add(featureCoefficient);
							}
							else
								knownFeatureValues.add(featureCoefficient);
							//featurePrinted = true;
					}
					if (type.equals("emit")){
						/* Currently doesn't need to specify emit prefix*/
						featureName = "slip_" + featureName;//(featureName.startsWith(type)? featureName : type + "_" + featureName);
						/* Direct feature coef are for known state used to predict 1 class, which is outcome=correct */
						featureCoefficient = (-1.0) * featureCoefficient;
						knownFeatureNames.add(featureName);
						knownFeatureValues.add(featureCoefficient);
						//featurePrinted = true;
					}
				}
				/* Current feature weights are for unknown state used to predict 1 class(state). */
				else if ((featureName.contains("_hidden1") && knownState == 0) || (!featureName.contains("_hidden1") && knownState == 1)) {
					featureName = featureName.replace("_hidden1", "");
					if (type.equals("init")){
						if (featureName.contains("_hidden1")){
							System.out.println("ERROR: init feature names shouldn't contain _hidden1 surfix");
							System.exit(-1);
						}
						if (knownState != 1){
							System.out.println("ERROR: knownState should be 1");
							System.exit(-1);
						}
						/* knownState=1. Direct feature coef is for predicting 1, which is known state. */
						featureName =  (featureName.startsWith(type)? featureName : type + "_" + featureName);
						knownFeatureNames.add(featureName);
						knownFeatureValues.add(featureCoefficient);
						//featurePrinted = true;
					}
					if (type.equals("tran")){
						/* currently we only support tran_ prefix */
						featureName = "learn_" + featureName;//+ (featureName.startsWith(type)? featureName : type + "_" + featureName);
						unknownFeatureNames.add(featureName);
						if (knownState == 0){
							/* Direct feature coef are for unknown state used to predict 1 class, which is unknown state. */
							featureCoefficient = (-1) * featureCoefficient;//use for unknown to known
							unknownFeatureValues.add(featureCoefficient);//Before: (-1 * featureCoefficient)
						}
						else /* Direct feature coef are for unknown state used to predict 1 class, which is known state. */
							unknownFeatureValues.add(featureCoefficient);
						//featurePrinted = true;
					}
					if (type.equals("emit")){
						/* currently we only support emit_ prefix */
						featureName = "guess_" + featureName;
						/* Direct feature coef are for unknown state used to predict 1 class, which is outcome=correct */
						unknownFeatureNames.add(featureName);
						unknownFeatureValues.add(featureCoefficient);
						//featurePrinted = true;
					}
				}
			}
		}
		if (type.equals("init")){
			for (int i = 0; i < knownFeatureNames.size(); i++){
				String knownFeatureName = knownFeatureNames.get(i);
				double knownFeatureValue = knownFeatureValues.get(i);
				allParameters.put(knownFeatureName, knownFeatureValue);
			}
//			featureStrings[0] += knownFeatureNameString;
//			featureStrings[1] += knownFeatureValueString;
		}
		if (type.equals("tran") || type.equals("emit") ){
			for (int i = 0; i < unknownFeatureNames.size(); i++){
				String unknownFeatureName = unknownFeatureNames.get(i);
				double unknownFeatureValue = unknownFeatureValues.get(i);
				allParameters.put(unknownFeatureName, unknownFeatureValue);
			}
			for (int i = 0; i < knownFeatureNames.size(); i++){
				String knownFeatureName = knownFeatureNames.get(i);
				double knownFeatureValue = knownFeatureValues.get(i);
				allParameters.put(knownFeatureName, knownFeatureValue);
			}	
//			featureStrings[0] += unknownFeatureNameString + knownFeatureNameString;
//			featureStrings[1] += unknownFeatureValueString + knownFeatureValueString;
		}
//		if (opts.computeStatForParameters){
//			String[] unknownNames = unknownFeatureNameString.split(",");
//			String[] knownNames = knownFeatureNameString.split(",");
//			String[] unknownValues = unknownFeatureValueString.split(",");
//			String[] knownValues = knownFeatureValueString.split(",");
//			for (int pos = 0; pos < unknownNames.length; pos++){
//				//System.out.println(unknownNames[pos] + "," + unknownValues[pos]);
//				if (unknownNames[pos] == "" || unknownNames[pos].length() == 0)
//					continue;
//				String featureName = unknownNames[pos];
//				double featureCoefficient = Double.parseDouble(unknownValues[pos]);
//				logger.curRestartLogger.outputParameterNames.put(featureName);
//				if (opts.outputFeatureCoefToEval)
//					logger.curFileLogger.outputParameterNamesForStats.put(featureName);
//				if (!opts.outputPerHMM){
//					ArrayList<Double> coefs = new ArrayList<Double>();
//					coefs.add(featureCoefficient);
//					logger.curFileLogger.featureToAllKCValues.put(featureName, coefs);
//				}
//				else
//					logger.curMultiRestartLogger.featureToValue.get(restartId).put(featureName, featureCoefficient);
////				
////				if (!featureNameAdded){
////					outputParameterNames.add(featureName);
////					outputParameterNamesForStatistics.add(featureName);
////					if (!outputPerKC){
////						ArrayList<Double> coefs = new ArrayList<Double>();
////						coefs.add(featureCoefficient);
////						oneProcessPerParameterAllKcValues.put(featureName, coefs);
////					}
////					else
////						curHmmPerProcessPerParameterValue.get(curProcessId).put(featureName, featureCoefficient);
////				}
////				else{
////					if (!outputPerKC)
////						oneProcessPerParameterAllKcValues.get(featureName).add(featureCoefficient);
////					else
////						curHmmPerProcessPerParameterValue.get(curProcessId).put(featureName, featureCoefficient);
////				}
//			}
//			for (int pos = 0; pos < knownNames.length; pos++){
//				//System.out.println(knownNames[pos] + "," + knownNames[pos]);
//				if (knownNames[pos] == "" || knownNames[pos].length() == 0)
//					continue;
//				String featureName = knownNames[pos];
//				double featureCoefficient = Double.parseDouble(knownValues[pos]);
//				logger.curRestartLogger.outputParameterNames.put(featureName);
//				//curHmmOutputParameterNames.put(featureName);
//				if (opts.outputFeatureCoefToEval)
//					logger.curFileLogger.outputParameterNamesForStats.put(featureName);
//					//allHmmsOutputParameterNamesForStatistics.put(featureName);
//				if (!opts.outputPerHMM){
//					ArrayList<Double> coefs = new ArrayList<Double>();
//					coefs.add(featureCoefficient);
//					logger.curFileLogger.featureToAllKCValues.put(featureName, coefs);
//					//oneProcessPerParameterAllKcValues.put(featureName, coefs);
//				}
//				else
//					//curHmmPerProcessPerParameterValue.get(restartId).put(featureName, featureCoefficient);
//					logger.curMultiRestartLogger.featureToValue.get(restartId).put(featureName, featureCoefficient);
//
////				if (nbHmmTrained == 1){
////					 outputParameterNames.add(featureName);
////					 outputParameterNamesForStatistics.add(featureName);
////					 if (!outputPerKC){
////							ArrayList<Double> coefs = new ArrayList<Double>();
////							coefs.add(featureCoefficient);
////							oneProcessPerParameterAllKcValues.put(featureName, coefs);
////					 }
////					 else
////						 curHmmPerProcessPerParameterValue.get(curProcessId).put(featureName, featureCoefficient);
////				}
////				else{
////					if (!outputPerKC)
////						oneProcessPerParameterAllKcValues.get(featureName).add(featureCoefficient);
////					else
////						curHmmPerProcessPerParameterValue.get(curProcessId).put(featureName, featureCoefficient);
////				}
//			}
//		}
	}
	
	public void start() throws IOException{
		opts.configure();
		logger.startDate = new Date();
//		if (opts.writeAllProcessSummary && opts.nbRandomRestart > 1) {// If restartId=-1, it is running only one random restart
//			//logger.allProcessSummaryWriter = new BufferedWriter(new FileWriter(opts.allProcessSummaryFile));
//			//logger.bestModelFileNameWriter = new BufferedWriter(new FileWriter(opts.bestModelFileNameFile, true));
//		}
	 //if (opts.writeMainLog)
		logger.mainLogWriter = new BufferedWriter(new FileWriter(opts.mainLogFile, true));
		getConfigurationStr();
	}
	

	
	public void writeOneFileAllHMMSummary() throws IOException {
		OneFileLogger curFileLogger = logger.curFile;
		Bijection metricNames = logger.metrics;
		String delimiter = ",";
		ArrayList<Metrics> allHmmMetrics = curFileLogger.getTestEval();
		if (allHmmMetrics == null || allHmmMetrics.size() < 1){
			System.out.println("Error: (allHmmMetrics == null || allHmmMetrics.size() < 1)");
			System.exit(1);
		}
		//int nbTrainedHmms = logger.curFileLogger.trainedHmms.size();
		BufferedWriter writer = new BufferedWriter(new FileWriter(opts.evalFile));
		
		String header = allHmmMetrics.get(0).getHeader(delimiter);
		String evalStr = "";
		for (Metrics eval : allHmmMetrics)
			evalStr += eval.getEvaluationStr(delimiter) + "\n";
		String outStr = header + "\n" + evalStr;
		//writer.write(header + "\n" + evalStr);
		
		//Bijection metricNames = logger.metricNames;
		int fileId = curFileLogger.getFileId();

		//Get Overall
		Metrics overallAcrossKcsEval = new Metrics(fileId + "(OverallAcrossKCs)");
		PredictivePerformance evaluation = new PredictivePerformance();
		Metrics eval = evaluation.evaluateClassifier(curFileLogger.getActualLabels(), 
				curFileLogger.getPredLabels(), 
				curFileLogger.getPredProbs(), "OverallAcrossKCs");
		String overallPerfStr = eval.getEvaluationStr(delimiter);
		overallAcrossKcsEval.copyMetrics(eval);
		
		//Get Mean
		Metrics meanAcrossKcsEval = new Metrics(fileId + "(MeanAcrossKCs)");
		String meanStr = "MeanAcrossKCs" + delimiter;
		String sdStr = "SDAcrossKCs" + delimiter;
		String overallStr = overallPerfStr;
		for (int j = 0; j < metricNames.getSize(); j++) {
			String metricName = metricNames.get(j);
			ArrayList<Double> metricValues = new ArrayList<Double>();
			for (int i = 0; i < allHmmMetrics.size(); i++ ){
				Metrics oneHmmEval = allHmmMetrics.get(i);
				double metricValue = oneHmmEval.getMetricValue(metricName);
				metricValues.add(metricValue);
			}
			Double mean = Stats.mean(metricValues);
			Double sd = Stats.sd(metricValues);
			meanStr += Utility.getValidString(mean, opts.formatter) + delimiter ;
			sdStr += Utility.getValidString(sd, opts.formatter) + delimiter;
			meanAcrossKcsEval.setMetricValue(metricName, mean);
			
			if (!logger.predictionMetrics.contains(metricName)){
				if (metricName.matches("(?i).*plausibility.*") || metricName.matches("(?i).*train.*")){
					Double value = -1.0;
					if (metricName.matches("(?i).*plausibility.*") || (metricName.matches("(?i).*nb.*")))
						 value = Stats.sum(metricValues);
					else if (metricName.matches("(?i).*max.*"))
						 value = Stats.max(metricValues);
					else
						 value = Stats.sum(metricValues);
					overallStr += Utility.getValidString(value, opts.formatter) + delimiter;
					overallAcrossKcsEval.setMetricValue(metricName, value);
				}
				else{
					System.out.println("ERROR: unknown metric name!");
					System.exit(-1);
				}
			}
		}
		logger.curMultiFiles.testMeanAcrossKcsMetrics.add(meanAcrossKcsEval);
		logger.curMultiFiles.testOverallAcrossKcsMetrics.add(overallAcrossKcsEval);
		outStr +=  meanStr + "\n" + sdStr + "\n" + overallStr + "\n";
		writer.write(outStr);
		writer.close();
		
		String fileProgress = "(" + Integer.toString(fileId + 1) + "/" + Integer.toString(opts.nbFiles) + ")";
		String str = "\n\n\n***** Evaluating: fileId=" + fileId + fileProgress + ", all KCs(" + curFileLogger.trainAllHMMs.getSize() + ")" 
										+ (opts.nbRandomRestart > 1 ? ", best restarts(" + opts.nbRandomRestart + ")" : "") + " *****\n"
				 + outStr;
		logger.printAndLog(str);
		//TODO: accumulate to multiFiles
		//logger.curFile.printTrainSummary(delimiter);
		
		
		ArrayList<LinkedHashMap<String, Double>> trainedParameters = curFileLogger.getTrainedParameters();
		if (trainedParameters == null || trainedParameters.size() < 1){
			System.out.println("Error: (trainedParameters == null || trainedParameters.size() < 1)");
			System.exit(1);
		}
		writer = new BufferedWriter(new FileWriter(opts.parametersFile));
		int i = 0;
		for (LinkedHashMap<String, Double> parameters : curFileLogger.getTrainedParameters()){
			String[] parameterStrs = Utility.linkedHashMapToStrings(parameters, delimiter);
			String hmmName = curFileLogger.trainAllHMMs.get(i);
			if (i == 0 || opts.parameterizing){
				writer.write("KC" + delimiter + parameterStrs[0] + "\n");
			}
			writer.write(hmmName + delimiter + parameterStrs[1] + "\n");
			i++;
		}
		writer.close();


//		else {
			// BufferedWriter allHmmsSummaryWriter = new BufferedWriter(new FileWriter(allHmmsSummaryDir + modelName + "_AllHMMs_Evaluation.csv"));
			// BufferedWriter allHmmsNamesWriter = new BufferedWriter(new FileWriter(allHmmsSummaryDir + hmmNameFileName));
			
//			String headerStr = "kc" + delimiter + "#restarts" + delimiter;
//			String hmmNameStr = "kc" + "\n";
//			boolean getHeader = false;
//			String str = "";
//			
//			if (logger.trainHmmNames != null 
//									&& logger.curFileLogger.metricToAllKCMeanAcrossRestarts != null 
//									&& logger.curFileLogger.metricToAllKCMeanAcrossRestarts.get("metric") != null 
//									&& (logger.trainHmmNames.getSize() != logger.curFileLogger.metricToAllKCMeanAcrossRestarts.get("auc").size())) {
//				System.out.println("Error: trainKcs.getSize() != perMetricMeanAcrossRestartsPerKcValues.get(metric).size()");
//				System.exit(-1);
//			}
//		
//			for (int i = 0; i < logger.trainHmmNames.getSize(); i++) {
//				hmmNameStr += logger.trainHmmNames.get(i) + "\n";
//				String curStr = logger.trainHmmNames.get(i) + delimiter + opts.nbRandomRestart + delimiter;
//	
//				// across all random restarts
//				for (int j = 0; j < logger.metricNames.getSize(); j++) {
//					String metric = logger.metricNames.get(j);
//					if (!getHeader)
//						headerStr += "MEAN_" + metric + "_acrossRestarts" + delimiter + "SD_" + metric + "_acrossRestarts" + delimiter;
//					curStr += logger.curFileLogger.metricToAllKCMeanAcrossRestarts.get(metric).get(i) + delimiter 
//							+ logger.curFileLogger.metricToAllKCMeanAcrossRestarts.get(metric).get(i) + delimiter;
//				}
//				getHeader = true;
//				curStr = curStr.substring(0, curStr.length() - 1) + "\n";
//				str += curStr;
//			}
//		
//			hmmNameStr = hmmNameStr.substring(0, hmmNameStr.length() - 1);
//			headerStr = headerStr.substring(0, headerStr.length() - 1) + "\n";
//			str = headerStr + str;
//			String meanStr = "mean" + delimiter + opts.nbRandomRestart + delimiter;
//			String sdStr = "sd" + delimiter + opts.nbRandomRestart + delimiter;
//	
//			// across all random restarts
//			for (int j = 0; j < logger.metricNames.getSize(); j++) {
//				String metric = logger.metricNames.get(j);
//				ArrayList<Double> metricMeanValues = logger.curFileLogger.metricToAllKCMeanAcrossRestarts.get(metric);
//				ArrayList<Double> metricSdValues = logger.curFileLogger.metricToAllKCSdAcrossRestarts.get(metric);
//				meanStr += Stats.mean(metricMeanValues) + delimiter + Stats.mean(metricSdValues) + delimiter;
//				sdStr += Stats.sd(metricMeanValues) + delimiter + Stats.sd(metricSdValues) + delimiter;
//			}
//			meanStr = meanStr.substring(0, meanStr.length() - 1) + "\n";
//			sdStr = sdStr.substring(0, sdStr.length() - 1) + "\n";
//			str = str + meanStr + sdStr;
			
			// allHmmsSummaryWriter.write(str);
			// allHmmsSummaryWriter.close();
			// allHmmsNamesWriter.write(hmmNameStr);
			// allHmmsNamesWriter.close();
//		}
	}
	
	public void getMetricsMeanSD(Bijection metricNames, ArrayList<Metrics> metricObjList, ArrayList<Double> means, ArrayList<Double> sds){
		for (int j = 0; j < metricNames.getSize(); j++) {
			String metricName = metricNames.get(j);
			ArrayList<Double> metricValues = new ArrayList<Double>();
			for (int i = 0; i < metricObjList.size(); i++ ){
				Metrics oneFileEval = metricObjList.get(i);
				double metricValue = oneFileEval.getMetricValue(metricName);
				metricValues.add(metricValue);
			}
			Double mean = Stats.mean(metricValues);
			Double sd = Stats.sd(metricValues);
			means.add(mean);
			sds.add(sd);
		}
	}


	
	public void writeAllFilesSummary() throws IOException {
		int nbHMMs = logger.curMultiFiles.getNbHMMs();
		String str = "\n\n\n***** Evaluating: all files(" + opts.nbFiles + "), all KCs(" + nbHMMs + ")"
							+ (opts.nbRandomRestart > 1 ? ", best restarts(" + opts.nbRandomRestart + ")" : "") + " *****";
		logger.printAndLog(str);

		String delimiter = ",";
		ArrayList<Metrics> meanAcrossKcs = logger.curMultiFiles.testMeanAcrossKcsMetrics;
		str =  meanAcrossKcs.get(0).getHeader(delimiter) + "\n";	
		BufferedWriter writer = new BufferedWriter(new FileWriter(opts.allFilesEvalFile));
		for (Metrics eval : meanAcrossKcs)
			str += eval.getEvaluationStr(delimiter) + "\n";
		
		ArrayList<Double> means = new ArrayList<Double>();
		ArrayList<Double> sds = new ArrayList<Double>();
		getMetricsMeanSD(logger.metrics, meanAcrossKcs, means, sds);
		String meanStr = "Mean(MeanAcrossKCs)" + delimiter +  Utility.doubleArrayListToString(means, opts.formatter, delimiter);
		String sdStr = "SD(MeanAcrossKCs)" + delimiter + Utility.doubleArrayListToString(sds, opts.formatter, delimiter);
		
		ArrayList<Metrics> overallAcrossKcs = logger.curMultiFiles.testOverallAcrossKcsMetrics;
		for (Metrics eval : overallAcrossKcs)
			str += eval.getEvaluationStr(delimiter) + "\n";
		means = new ArrayList<Double>();
		sds = new ArrayList<Double>();
		getMetricsMeanSD(logger.metrics, overallAcrossKcs, means, sds);
		String meanOverallStr = "Mean(OverallAcrossKCs)" + delimiter + Utility.doubleArrayListToString(means, opts.formatter, delimiter);
		String sdOverallStr = "SD(OverallAcrossKCs)" + delimiter + Utility.doubleArrayListToString(sds, opts.formatter, delimiter);
		
		str += meanStr + "\n" + sdStr + "\n" +  meanOverallStr + "\n" + sdOverallStr;

		writer.write(str + "\n");
		writer.close();
		logger.printAndLog(str);

	}
	

//	public int evaluateOneHMMAllRestarts() {
//		// TODO: now by default, use the one with the least model degeneracy
//		String delimiter = ",";
//		String hmmName = logger.curMultiRestartLogger.hmmName;
//		int fileId = logger.curFileLogger.fileId;
//		int nbHmmTrained = logger.curFileLogger.trainedHmms.size();
//		String hmmProgress = "(" + (logger.curFileLogger.trainedHmms.size() + 1) + "/" + logger.trainHmmNames.getSize() + ")";
//		String outStr = "\n***** Summary: HMM=" + hmmName + hmmProgress + ", fileId=" + fileId + " *****" + "\nmodelName=" + delimiter 
//							+ opts.modelName + "\ntrain=" + delimiter + opts.trainFile + "\ntest=" + delimiter + opts.testFile 
//							+ "\nparameterizaing=" + opts.parameterizing + "\tparameterizedInit=" + opts.parameterizedInit + "\tparameterizedTrans=" + opts.parameterizedTran + "\tparameterizedEmit=" + opts.parameterizedEmit + "\n";
//		HashMap<String, ArrayList<Double>> perParameterPerProcessValues = new HashMap<String, ArrayList<Double>>();
//		String featureNames = "";
//		for (int i = 0; i < logger.curFileLogger.outputParameterNamesForStats.getSize(); i++) {
//			String feature = logger.curFileLogger.outputParameterNamesForStats.get(i);
//			featureNames += feature + delimiter;
//			perParameterPerProcessValues.put(feature, new ArrayList<Double>());
//		}
//		String header = "process" + delimiter + "#kcs" + delimiter + "kc" + delimiter + "trainLL" + delimiter // + "overall_LL" + delimiter
//				+ "#deg_kcs_by_guess+slip_feature_off" + delimiter + "%deg_dp_traintest" + delimiter + "guess+slip_feature_off" + delimiter + "guess+slip_feature_on" + delimiter + "guess+slip_avg_dp_traintest" + delimiter + "min_guess+slip_across_dp_train" + delimiter + "min_guess+slip_test" + delimiter + "%decrease_pKnown_test" + delimiter + "%decrease_pCorrect_test" + delimiter// + "auc" + delimiter// "mean_AUC" + delimiter + "overall_AUC" + delimiter
//				+ "#kcs_stop_by_em_iteration" + delimiter + "running_time(s)" + delimiter + "#prac_to_mastery" + delimiter + "#stu_reached_mastery" + delimiter + "#stu" + delimiter + "%stu_reached_mastery" + delimiter + "#kcs_with_mastery" + delimiter + featureNames + "\n";
//
//		outStr = outStr + header;
//		int bestProcess = -1;
//		double bestValue = -1.0;
//		String contentOutStr = "";
//		String kc_names = hmmName;
//		// boolean get_kc_names = false;
//		ArrayList<Double> LLs = new ArrayList<Double>();
//		// ArrayList<Double> overallLLs = new ArrayList<Double>();
//		ArrayList<Double> nbDegKcs = new ArrayList<Double>();
//		ArrayList<Double> pctDegDps = new ArrayList<Double>();
//		ArrayList<Double> guessPlusSlipOff = new ArrayList<Double>();
//		ArrayList<Double> guessPlusSlipOn = new ArrayList<Double>();
//		ArrayList<Double> guessPlusSlipAvgDp = new ArrayList<Double>();
//		ArrayList<Double> minGuessSlipTrain = new ArrayList<Double>();
//		ArrayList<Double> minGuessSlipTest = new ArrayList<Double>();
//		ArrayList<Double> pctDecreasePKnown = new ArrayList<Double>();
//		ArrayList<Double> pctDecreasePCorrect = new ArrayList<Double>();
//		ArrayList<Double> aucs = new ArrayList<Double>();
//		// ArrayList<Double> overallAucs = new ArrayList<Double>();
//		ArrayList<Double> stopByEMIterations = new ArrayList<Double>();
//		ArrayList<Double> nbPracToMastery = new ArrayList<Double>();
//		ArrayList<Double> nbStudentReachedMastery = new ArrayList<Double>();
//		ArrayList<Double> nbStudents = new ArrayList<Double>();
//		ArrayList<Double> pctStudentReachedMastery = new ArrayList<Double>();
//		ArrayList<Double> nbKcsWithMastery = new ArrayList<Double>();
//		//ArrayList<Double> runningTime = new ArrayList<Double>();
//
//		// Bijection processIdMap = new Bijection();
//		for (int id = opts.startingRestartId; id < opts.nbRandomRestart + opts.startingRestartId; id++) {
//			// processIdMap.put(id+"");
//			String parameterValues = "";
//			for (int i = 0; i < logger.curFileLogger.outputParameterNamesForStats.getSize(); i++) {
//				String feature = logger.curFileLogger.outputParameterNamesForStats.get(i);
//				if (logger.curMultiRestartLogger.featureToValue.get(id).containsKey(feature)) {
//					double featureValue = logger.curMultiRestartLogger.featureToValue.get(id).get(feature);
//					perParameterPerProcessValues.get(feature).add(logger.curMultiRestartLogger.featureToValue.get(id).get(feature));
//					parameterValues += featureValue + delimiter;
//				}
//				else {
//					perParameterPerProcessValues.get(feature).add(null);
//					parameterValues += "null" + delimiter;
//				}
//			}
//			parameterValues = parameterValues.substring(0, parameterValues.length() - 1);
//			OneRestartLogger curLogger = logger.curMultiRestartLogger.restartToLogger.get(id);
//			double ll = curLogger.trainLL;
//			// double overall_ll = mean_ll;
//			double nb_deg_kcs = curLogger.degeneracy.nbDegKcsBasedOnGuessPlusSlipFeatureOff;
//			double pct_deg_dps = curLogger.degeneracy.pctDegDps;
//			double guess_plus_slip_off = curLogger.degeneracy.guessPlusSlipFeatureOff;
//			Double guess_plus_slip_on = curLogger.degeneracy.guessPlusSlipFeatureOn;
//			double guess_plus_slip_avg_dp = curLogger.degeneracy.guessPlusSlipAvgPerDP;
//			double min_guess_slip_train = curLogger.degeneracy.minGuessPlusSlipPerDpOnTrain;
//			double min_guess_slip_test = curLogger.degeneracy.minGuessPlusSlipPerDpOnTest;
//			double pct_decrease_pknown = curLogger.degeneracy.pctDecProbKnown;
//			double pct_decrease_pcorrect = curLogger.degeneracy.pctDecProbCorrect;
//			//double auc = curHmmPerRestartAuc.get(id);
//			// double overall_auc = curHmmPerProcessAuc.get(id);
//			double stopByEMIteration = (double) curLogger.trainStopByEMIteration;
//			Mastery mastery_obj = curLogger.mastery;
//			//double running_time = curLogger.trainTestTime;
//			Double sum_nb_prac_to_mastery = null;
//			Double avg_pct_stu_to_mastery = 0.0;
//			Double avg_nb_total_students = 0.0;
//			Double avg_nb_students_reached_mastery = 0.0;
//			Double nb_kcs_with_mastery = 0.0;
//			// for (Map.Entry<String, Mastery> obj : mastery_obj.entrySet()){//iterate through skills
//			// if (!get_kc_names)
//			// kc_names += obj.getKey() + "~";
//			// Mastery m = obj.getValue();
//			avg_nb_total_students = mastery_obj.nbTotalStudents; // for one skill on test set
//			if (mastery_obj.nbPracToReachMastery.size() > 0) {
//				nb_kcs_with_mastery = 1.0;
//				sum_nb_prac_to_mastery = Stats.mean(mastery_obj.nbPracToReachMastery);// across students for one skill;
//				avg_nb_students_reached_mastery = (double) mastery_obj.studentsReachedMastery.getSize(); // for one skill
//				avg_pct_stu_to_mastery = mastery_obj.studentsReachedMastery.getSize() / (1.0 * mastery_obj.nbTotalStudents); // for one skill
//				// nb_kcs_with_mastery += 1;
//			}
//			// }
//			// if (!get_kc_names){
//			// kc_names = kc_names.substring(0, kc_names.length() - 1);
//			// get_kc_names = true;
//			// }
//			// avg_nb_total_students /= nbHmmTrained;
//			// avg_nb_students_reached_mastery /= nbHmmTrained;
//			// avg_pct_stu_to_mastery /= nbHmmTrained;
//
//			double currentValue = -1.0;
//			// if (chooseBestAmongRandomStarts.equals("#deg_kcs"))
//			// currentValue = nb_deg_kcs;
//			// else
//			if (opts.chooseBestAmongRandomStarts.equals("ll"))
//				currentValue = ll;
//			else if (opts.chooseBestAmongRandomStarts.equals("g+s"))
//				currentValue = guess_plus_slip_off;
//			if (bestValue == -1) {
//				bestValue = currentValue;
//				bestProcess = id;
//			}
//			else if
//			// ((currentValue < bestValue
//			// && (chooseBestAmongRandomStarts.equals("#deg_kcs") || chooseBestAmongRandomStarts.equals("g+s"))) ||
//			(currentValue > bestValue && opts.chooseBestAmongRandomStarts.equals("ll")) {
//				bestValue = currentValue;
//				bestProcess = id;
//			}
//
//			LLs.add(ll);
//			// overallLLs.add(overall_ll);
//			nbDegKcs.add(nb_deg_kcs);
//			pctDegDps.add(pct_deg_dps);
//			guessPlusSlipOff.add(guess_plus_slip_off);
//			guessPlusSlipOn.add(guess_plus_slip_on);
//			guessPlusSlipAvgDp.add(guess_plus_slip_avg_dp);
//			minGuessSlipTrain.add(min_guess_slip_train);
//			minGuessSlipTest.add(min_guess_slip_test);
//			pctDecreasePKnown.add(pct_decrease_pknown);
//			pctDecreasePCorrect.add(pct_decrease_pcorrect);
//			//aucs.add(auc);
//			// overallAucs.add(overall_auc);
//			stopByEMIterations.add(stopByEMIteration);
//			nbPracToMastery.add(sum_nb_prac_to_mastery);// sum of skills (each skill mean across students) for one process
//			nbStudentReachedMastery.add(avg_nb_students_reached_mastery);// avg across skills for one process
//			nbStudents.add(avg_nb_total_students);// avg across skills for one process
//			pctStudentReachedMastery.add(avg_pct_stu_to_mastery);// avg across skills for one process
//			nbKcsWithMastery.add(nb_kcs_with_mastery);
//			//runningTime.add(running_time);
//
//			contentOutStr += id + delimiter + nbHmmTrained + delimiter + kc_names + delimiter + ll + delimiter // + overall_ll + delimiter
//					+ nb_deg_kcs + delimiter + pct_deg_dps + delimiter + guess_plus_slip_off + delimiter + guess_plus_slip_on + delimiter + guess_plus_slip_avg_dp + delimiter + min_guess_slip_train + delimiter + min_guess_slip_test + delimiter + pct_decrease_pknown + delimiter + pct_decrease_pcorrect + delimiter //+ auc + delimiter // + overall_auc + delimiter
//					+ stopByEMIteration + delimiter //+ running_time + delimiter 
//					+ sum_nb_prac_to_mastery + delimiter + avg_nb_students_reached_mastery + delimiter + avg_nb_total_students + delimiter + avg_pct_stu_to_mastery + delimiter + nb_kcs_with_mastery + delimiter + parameterValues + "\n";
//			// contentOutStr += id + delimiter + nbKcTrained + delimiter + kc_names + delimiter + mean_ll + delimiter + overall_ll + delimiter
//			// + nb_deg_kcs + delimiter + shortFormatter.format(pct_deg_dps) + delimiter + shortFormatter.format(guess_plus_slip_off) + delimiter + shortFormatter.format(guess_plus_slip_on) + delimiter+ shortFormatter.format(guess_plus_slip_avg_dp) + delimiter
//			// + shortFormatter.format(mean_auc) + delimiter + shortFormatter.format(overall_auc) + delimiter
//			// + nbKcsStopByEMIteration + delimiter + shortFormatter.format(running_time) + delimiter
//			// + shortFormatter.format(sum_nb_prac_to_mastery) + delimiter + shortFormatter.format(avg_nb_students_reached_mastery) + delimiter + shortFormatter.format(avg_nb_total_students) + delimiter + shortFormatter.format(avg_pct_stu_to_mastery) + delimiter + nb_kcs_with_mastery + "\n";
//		}
//		double globalOptimum = Stats.max(LLs);
//		double threshold = globalOptimum + globalOptimum * opts.GLOBAL_OPTIMA_DIST;
//		ValueIndexSummary SummaryObjSum = Stats.max_with_index(LLs);
//		int globalOptimalIndex = SummaryObjSum.indexes.get(0);// Integer.parseInt(processIdMap.get(SummaryObjSum.indexes.get(0)));
//		String[] types = { "init" + opts.outputILGSNameSurfix, "learn" + opts.outputILGSNameSurfix, "guess" + opts.outputILGSNameSurfix, "slip" + opts.outputILGSNameSurfix };
//		ArrayList<Double> globalOptimumILGSVector = new ArrayList<Double>();
//		for (String type : types)
//			globalOptimumILGSVector.add(perParameterPerProcessValues.get(type).get(globalOptimalIndex));
//
//		ArrayList<Double> distanceToGlobalOptimumWithinAll = new ArrayList<Double>();
//		for (int i = 0; i < LLs.size(); i++) {
//			ArrayList<Double> curILGSVector = new ArrayList<Double>();
//			for (String type : types)
//				curILGSVector.add(perParameterPerProcessValues.get(type).get(i));
//			distanceToGlobalOptimumWithinAll.add(Functions.euclidean_distance(curILGSVector, globalOptimumILGSVector));
//		}
//
//		HashMap<String, ArrayList<Double>> perMetricAcorssRestarts = new HashMap<String, ArrayList<Double>>();
//		// String[] metrics = {"ll", "#deg_kcs_by_guess+slip_feature_off", "%deg_dp_traintest", "guess+slip_feature_off", "guess+slip_feature_on", "guess+slip_avg_dp_traintest",
//		// "min_guess+slip_across_dp_train", "min_guess+slip_test", "%decrease_pKnown_test", "%decrease_pCorrect_test", "auc", "#kcs_stop_by_em_iteration", "running_time", "#prac_to_mastery", "%stu_reached_mastery", "#kcs_with_mastery" }; metricsPerRestart.put("ll", lLs);
//		Bijection metricNames = logger.metricNames;
//		Bijection nonGOGONDMetricNames = logger.nonGOGONDMetricNames;
//		metricNames.put("trainLL");
//		nonGOGONDMetricNames.put("trainLL");
//		perMetricAcorssRestarts.put("trainLL", LLs);
////		metricNames.put("auc");
////		nonGOGONDMetricNames.put("auc");
//	//	perMetricAcorssRestarts.put("auc", aucs);
//		metricNames.put("guess+slip_feature_off");
//		nonGOGONDMetricNames.put("guess+slip_feature_off");
//		perMetricAcorssRestarts.put("guess+slip_feature_off", guessPlusSlipOff);
//		metricNames.put("#deg_kcs_by_guess+slip_feature_off");
//		nonGOGONDMetricNames.put("#deg_kcs_by_guess+slip_feature_off");
//		perMetricAcorssRestarts.put("#deg_kcs_by_guess+slip_feature_off", nbDegKcs);
//		metricNames.put("%deg_dp_traintest");
//		nonGOGONDMetricNames.put("%deg_dp_traintest");
//		perMetricAcorssRestarts.put("%deg_dp_traintest", pctDegDps);
//		metricNames.put("%decrease_pKnown_test");
//		nonGOGONDMetricNames.put("%decrease_pKnown_test");
//		perMetricAcorssRestarts.put("%decrease_pKnown_test", pctDecreasePKnown);
//		metricNames.put("%decrease_pCorrect_test");
//		nonGOGONDMetricNames.put("%decrease_pCorrect_test");
//		perMetricAcorssRestarts.put("%decrease_pCorrect_test", pctDecreasePCorrect);
//		metricNames.put("min_guess+slip_across_dp_train");
//		nonGOGONDMetricNames.put("min_guess+slip_across_dp_train");
//		perMetricAcorssRestarts.put("min_guess+slip_across_dp_train", minGuessSlipTrain);
//		metricNames.put("min_guess+slip_test");
//		nonGOGONDMetricNames.put("min_guess+slip_test");
//		perMetricAcorssRestarts.put("min_guess+slip_test", minGuessSlipTest);
//		metricNames.put("guess+slip_feature_on");
//		nonGOGONDMetricNames.put("guess+slip_feature_on");
//		perMetricAcorssRestarts.put("guess+slip_feature_on", guessPlusSlipOn);
//		metricNames.put("guess+slip_avg_dp_traintest");
//		nonGOGONDMetricNames.put("guess+slip_avg_dp_traintest");
//		perMetricAcorssRestarts.put("guess+slip_avg_dp_traintest", guessPlusSlipAvgDp);
//		metricNames.put("#kcs_stop_by_em_iteration");
//		nonGOGONDMetricNames.put("#kcs_stop_by_em_iteration");
//		perMetricAcorssRestarts.put("#kcs_stop_by_em_iteration", stopByEMIterations);
////		metricNames.put("running_time(s)");
////		nonGOGONDMetricNames.put("running_time(s)");
////		perMetricAcorssRestarts.put("running_time(s)", runningTime);
//		metricNames.put("#prac_to_mastery");
//		nonGOGONDMetricNames.put("#prac_to_mastery");
//		perMetricAcorssRestarts.put("#prac_to_mastery", nbPracToMastery);
//		metricNames.put("%stu_reached_mastery");
//		nonGOGONDMetricNames.put("%stu_reached_mastery");
//		perMetricAcorssRestarts.put("%stu_reached_mastery", pctStudentReachedMastery);
//		metricNames.put("#kcs_with_mastery");
//		nonGOGONDMetricNames.put("#kcs_with_mastery");
//		perMetricAcorssRestarts.put("#kcs_with_mastery", nbKcsWithMastery);
//		for (int i = 0; i < logger.curFileLogger.outputParameterNamesForStats.getSize(); i++) {
//			String feature = logger.curFileLogger.outputParameterNamesForStats.get(i);
//			metricNames.put(feature);
//			nonGOGONDMetricNames.put(feature);
//		}
//		perMetricAcorssRestarts.putAll(perParameterPerProcessValues);
//		metricNames.put(opts.distToGlobalOptimumStr);
//		perMetricAcorssRestarts.put(opts.distToGlobalOptimumStr, distanceToGlobalOptimumWithinAll);
//
//		String meanFeatureStr = "";
//		String sdFeatureStr = "";
//		String sumFeatureStr = "";
//		for (int i = 0; i < logger.curFileLogger.outputParameterNamesForStats.getSize(); i++) {
//			String feature = logger.curFileLogger.outputParameterNamesForStats.get(i);
//			meanFeatureStr += Stats.mean(perParameterPerProcessValues.get(feature)) + delimiter;
//			sdFeatureStr += Stats.sd(perParameterPerProcessValues.get(feature)) + delimiter;
//			sumFeatureStr += Stats.sum(perParameterPerProcessValues.get(feature)) + delimiter;
//		}
//		meanFeatureStr = meanFeatureStr.substring(0, meanFeatureStr.length() - 1);
//		sdFeatureStr = sdFeatureStr.substring(0, sdFeatureStr.length() - 1);
//		sumFeatureStr = sumFeatureStr.substring(0, sumFeatureStr.length() - 1);
//
//		contentOutStr += "mean" + delimiter + nbHmmTrained + delimiter + kc_names + delimiter + Stats.mean(LLs) + delimiter // + Stats.mean(overallLLs) + delimiter
//				+ Stats.mean(nbDegKcs) + delimiter + Stats.mean(pctDegDps) + delimiter + Stats.mean(guessPlusSlipOff) + delimiter + Stats.mean(guessPlusSlipOn) + delimiter + Stats.mean(guessPlusSlipAvgDp) + delimiter + Stats.mean(minGuessSlipTrain) + delimiter + Stats.mean(minGuessSlipTest) + delimiter + Stats.mean(pctDecreasePKnown) + delimiter + Stats.mean(pctDecreasePCorrect) + delimiter + Stats.mean(aucs) + delimiter // + Stats.mean(overallAucs) + delimiter
//				+ Stats.mean(stopByEMIterations) + delimiter //+ Stats.mean(runningTime) 
//				+ delimiter + Stats.mean(nbPracToMastery) + delimiter + Stats.mean(nbStudentReachedMastery) + delimiter + Stats.mean(nbStudents) + delimiter + Stats.mean(pctStudentReachedMastery) + delimiter + Stats.mean(nbKcsWithMastery) + delimiter + meanFeatureStr + "\n";
//		contentOutStr += "sample sd" + delimiter + nbHmmTrained + delimiter + kc_names + delimiter + Stats.sd(LLs) + delimiter // + Stats.sd(overallLLs) + delimiter
//				+ Stats.sd(nbDegKcs) + delimiter + Stats.sd(pctDegDps) + delimiter + Stats.sd(guessPlusSlipOff) + delimiter + Stats.sd(guessPlusSlipOn) + delimiter + Stats.sd(guessPlusSlipAvgDp) + delimiter + Stats.sd(minGuessSlipTrain) + delimiter + Stats.sd(minGuessSlipTest) + delimiter + Stats.sd(pctDecreasePKnown) + delimiter + Stats.sd(pctDecreasePCorrect) + delimiter + Stats.sd(aucs) + delimiter // + Stats.sd(overallAucs) + delimiter
//				+ Stats.sd(stopByEMIterations) + delimiter //+ Stats.sd(runningTime) 
//				+ delimiter + Stats.sd(nbPracToMastery) + delimiter + Stats.sd(nbStudentReachedMastery) + delimiter + Stats.sd(nbStudents) + delimiter + Stats.sd(pctStudentReachedMastery) + delimiter + Stats.sd(nbKcsWithMastery) + delimiter + sdFeatureStr + "\n";
//		contentOutStr += "sum" + delimiter + nbHmmTrained + delimiter + kc_names + delimiter + Stats.sum(LLs) + delimiter// + Stats.sum(overallLLs) + delimiter
//				+ Stats.sum(nbDegKcs) + delimiter + Stats.sum(pctDegDps) + delimiter + Stats.sum(guessPlusSlipOff) + delimiter + Stats.sum(guessPlusSlipOn) + delimiter + Stats.sum(guessPlusSlipAvgDp) + delimiter + Stats.sum(minGuessSlipTrain) + delimiter + Stats.sum(minGuessSlipTest) + delimiter + Stats.sum(pctDecreasePKnown) + delimiter + Stats.sum(pctDecreasePCorrect) + delimiter + Stats.sum(aucs) + delimiter // + Stats.sum(overallAucs)
//				+ Stats.sum(stopByEMIterations) + delimiter //+ Stats.sum(runningTime)
//				+ delimiter + Stats.sum(nbPracToMastery) + delimiter + Stats.sum(nbStudentReachedMastery) + delimiter + Stats.sum(nbStudents) + delimiter + Stats.sum(pctStudentReachedMastery) + delimiter + Stats.sum(nbKcsWithMastery) + delimiter + sumFeatureStr + "\n";
//		contentOutStr += "best by (mean) ll" + delimiter + Stats.max_with_index(LLs) + "\n";
//		// contentOutStr += "best(by #degenerated Kcs)" + delimiter + Stats.min_with_index(nbDegKcs) + "\n";
//		// contentOutStr += "best(by %degenerated datapoints)" + delimiter + Stats.min_with_index(pctDegDps) + "\n";
//		contentOutStr += "best by (mean) auc" + delimiter + Stats.max_with_index(aucs) + "\n";
//		contentOutStr += "best by g+s_off" + delimiter + Stats.min_with_index(guessPlusSlipOff) + "\n";
//		if (guessPlusSlipOn.get(0) != null)
//			contentOutStr += "best by g+s_on" + delimiter + Stats.min_with_index(guessPlusSlipOn) + "\n";
//		contentOutStr += "best by g+s_avg_dp" + delimiter + Stats.min_with_index(guessPlusSlipAvgDp) + "\n";
//		contentOutStr += "best by #deg_kcs_by_guess+slip_feature_off" + delimiter + Stats.min_with_index(nbDegKcs) + "\n";
//		contentOutStr += "best by %deg_dp_traintest" + delimiter + Stats.min_with_index(pctDegDps) + "\n";
//		contentOutStr += "best by %decrease_pKnown_test" + delimiter + Stats.min_with_index(pctDecreasePKnown) + "\n";
//		contentOutStr += "best by %decrease_pCorrect_test" + delimiter + Stats.min_with_index(pctDecreasePCorrect) + "\n";
//
//		// String header = "process" + delimiter + "#kcs" + delimiter + "kc_names" + delimiter
//		// + "ll" + delimiter //+ "overall_LL" + delimiter
//		// + "#deg_kcs_by_guess+slip_feature_off" + delimiter + "%deg_dp_traintest" + delimiter + "guess+slip_feature_off" + delimiter + "guess+slip_feature_on" + delimiter + "guess+slip_avg_dp_traintest" + delimiter + "min_guess+slip_across_dp_train" + delimiter + "min_guess+slip_test" + delimiter +"%decrease_pKnown_test" + delimiter + "%decrease_pCorrect_test" + delimiter
//		// + "auc" + delimiter//"mean_AUC" + delimiter + "overall_AUC" + delimiter
//		// + "#kcs_stop_by_em_iteration" + delimiter + "running_time" + delimiter
//		// + "#prac_to_mastery" + delimiter + "#stu_reached_mastery" + delimiter + "#stu" + delimiter + "%stu_reached_mastery" + delimiter + "#kcs_with_mastery" + "\n";
//		//
//		HashMap<String, ArrayList<Double>> perMetricAcrossRestartsOnGlobalOptima = new HashMap<String, ArrayList<Double>>();
//		HashMap<String, ArrayList<Double>> perMetricAcrossRestartsOnGlobalOptimaAndNonDeg = new HashMap<String, ArrayList<Double>>();
//		// Bijection allRandomStartsMetricNames = new Bijection(metricNames);
//		for (int i = 0; i < nonGOGONDMetricNames.getSize(); i++) {
//			String metric = nonGOGONDMetricNames.get(i);
//			perMetricAcrossRestartsOnGlobalOptima.put("GO_" + metric, new ArrayList<Double>());
//			perMetricAcrossRestartsOnGlobalOptimaAndNonDeg.put("GOND_" + metric, new ArrayList<Double>());
//		}
//
//		ArrayList<Double> globalOptimaIndicator = new ArrayList<Double>();
//		ArrayList<Double> globalOptimaAndNonDegIndicator = new ArrayList<Double>();
//		ArrayList<Double> distanceWithinGlobalOptima = new ArrayList<Double>();
//		ArrayList<Double> distanceWithinGlobalOptimaAndNonDeg = new ArrayList<Double>();
//
//		for (int i = 0; i < LLs.size(); i++) {
//			if (LLs.get(i) < threshold) {
//				globalOptimaIndicator.add(0.0);
//				globalOptimaAndNonDegIndicator.add(0.0);
//				continue;
//			}
//
//			globalOptimaIndicator.add(1.0);
//			for (String metric : perMetricAcrossRestartsOnGlobalOptima.keySet()) {
//				String oriMetric = metric.replace("GO_", "");
//				perMetricAcrossRestartsOnGlobalOptima.get(metric).add(perMetricAcorssRestarts.get(oriMetric).get(i));
//			}
//			ArrayList<Double> curILGSVector = new ArrayList<Double>();
//			for (String type : types)
//				curILGSVector.add(perParameterPerProcessValues.get(type).get(i));
//			distanceWithinGlobalOptima.add(Functions.euclidean_distance(curILGSVector, globalOptimumILGSVector));
//
//			if ((opts.degeneracyJudgementInequality.equals("be") && guessPlusSlipOff.get(i) < 1) || (opts.degeneracyJudgementInequality.equals("b") && guessPlusSlipOff.get(i) <= 1)) {
//				globalOptimaAndNonDegIndicator.add(1.0);
//				for (String metric : perMetricAcrossRestartsOnGlobalOptimaAndNonDeg.keySet()) {
//					String oriMetric = metric.replace("GOND_", "");
//					perMetricAcrossRestartsOnGlobalOptimaAndNonDeg.get(metric).add(perMetricAcorssRestarts.get(oriMetric).get(i));
//				}
//				// System.out.println(curILGSVector);
//				// System.out.println(globalOptimumILGSVector);
//				distanceWithinGlobalOptimaAndNonDeg.add(Functions.euclidean_distance(curILGSVector, globalOptimumILGSVector));
//			}
//			else
//				globalOptimaAndNonDegIndicator.add(0.0);
//		}
//
//		metricNames.put("GO_" + opts.probGlobalOptimalStr);
//		perMetricAcorssRestarts.put("GO_" + opts.probGlobalOptimalStr, globalOptimaIndicator);
//		metricNames.put("GO_" + opts.distToGlobalOptimumStr);
//		perMetricAcorssRestarts.put("GO_" + opts.distToGlobalOptimumStr, distanceWithinGlobalOptima);
//		for (int i = 0; i < nonGOGONDMetricNames.getSize(); i++) {
//			String metric = "GO_" + nonGOGONDMetricNames.get(i);
//			metricNames.put(metric);
//			perMetricAcorssRestarts.put(metric, perMetricAcrossRestartsOnGlobalOptima.get(metric));
//		}
//
//		metricNames.put("GOND_" + opts.probGlobalOptimalAndNonDegStr);
//		perMetricAcorssRestarts.put("GOND_" + opts.probGlobalOptimalAndNonDegStr, globalOptimaAndNonDegIndicator);
//		metricNames.put("GOND_" + opts.distToGlobalOptimumNonDegStr);
//		perMetricAcorssRestarts.put("GOND_" + opts.distToGlobalOptimumNonDegStr, distanceWithinGlobalOptimaAndNonDeg);
//		for (int i = 0; i < nonGOGONDMetricNames.getSize(); i++) {
//			String metric = "GOND_" + nonGOGONDMetricNames.get(i);
//			metricNames.put(metric);
//			perMetricAcorssRestarts.put(metric, perMetricAcrossRestartsOnGlobalOptimaAndNonDeg.get(metric));
//		}
//
//		// if (!perMetricMeanAcrossRestartsPerKcValuesOnGlobalOptima.containsKey(metric)){
//		// perMetricMeanAcrossRestartsPerKcValuesOnGlobalOptima.put(metric, new ArrayList<Double>());
//		// perMetricMeanAcrossRestartsPerKcValuesOnGlobalOptimaAndNonDeg.put(metric, new ArrayList<Double>());
//		// }
//		// perMetricMeanAcrossRestartsPerKcValuesOnGlobalOptima.get(metric).add(Stats.mean(metricsPerRestartOnGlobalOptima.get(metric)));//prob
//		// perMetricMeanAcrossRestartsPerKcValuesOnGlobalOptimaAndNonDeg.get(metric).add(Stats.mean(metricsPerRestartOnGlobalOptimaAndNonDeg.get(metric)));//prob
//
//		for (String metric : metricNames.keys()) {
//			if (!logger.curFileLogger.metricToAllKCMeanAcrossRestarts.containsKey(metric)) {
//				logger.curFileLogger.metricToAllKCMeanAcrossRestarts.put(metric, new ArrayList<Double>());
//				logger.curFileLogger.metricToAllKCSdAcrossRestarts.put(metric, new ArrayList<Double>());
//			}
//			logger.curFileLogger.metricToAllKCMeanAcrossRestarts.get(metric).add(Stats.mean(perMetricAcorssRestarts.get(metric)));
//			logger.curFileLogger.metricToAllKCSdAcrossRestarts.get(metric).add(Stats.sd(perMetricAcorssRestarts.get(metric)));
//		}
//		// contentOutStr += "#stu" + delimiter + + ")" + delimiter + Stats.max_with_index(meanLLs) + "\n";
////		outStr = outStr + contentOutStr; // + "Choosing the best process by " + chooseBestAmongRandomStarts + ":\t" + bestProcess;
////		try {
////		  printAndLog(outStr);
////			if (opts.writeAllProcessSummary) {
////				if (opts.outputPerHMM) {
////					opts.allProcessSummaryFile = opts.outDir + "allprocess_" + opts.modelName + ".eval.csv";
////					allProcessSummaryWriter = new BufferedWriter(new FileWriter(opts.allProcessSummaryFile));
////				}
////				allProcessSummaryWriter.write(header + contentOutStr);
////				bestModelFileNameWriter.write(kc_names + "," + opts.outDir + "," + bestProcess + "_" + opts.modelName + "_finalParameters.csv" 
////													+ "," + opts.outDir + bestProcess + "_" + opts.modelName + "_finalParameters.csv" + "\n");
////			}
////		}
////		catch (IOException e) {
////			// TODO Auto-generated catch block
////			e.printStackTrace();
////		}
//		return bestProcess;
//	}
	
//	public void closeAndClearOneRestart() throws IOException {
//		// nbKcsStopByLBFGSIteration = 0;
//		if (opts.writeFinalParameters)
//			finalParametersWriter.close();
//		
//		//TODO: use a class
//		curHmmPerRestartAuc = new TreeMap<Integer, Double>();
//		curHmmPerProcessDegeneracy = new TreeMap<Integer, Degeneracy>();
//		curHmmPerRestartLL = new TreeMap<Integer, Double>();
//		curHmmPerProcessTrainTestTime = new TreeMap<Integer, Double>();
//		curHmmPerProcessMastery = new TreeMap<Integer, Mastery>();
//		// curHmmCurProcessPerParameterValue = new HashMap<String, Double>();//feature to values (ordered by kc)
//		curHmmPerProcessPerParameterValue = new TreeMap<Integer, HashMap<String, Double>>();
//		curHmmPerProcessStopByEMIteration = new HashMap<Integer, Integer>();
//		curHmmCurProcessDegeneracyJudgementsAcrossDataPoints = new double[10];
//	}
	
//	public void closeAndClearAllRestarts() throws IOException{
//		if (opts.writeAllProcessSummary) {
//			allProcessSummaryWriter.close();
//			bestModelFileNameWriter.close();
//		}
//	}
	
//	public void closeOneFileLogger(){
//		testsetActualLabels = new ArrayList<Integer>();
//		testsetPredictLabels = new ArrayList<Integer>();
//		testsetPredictProbs = new ArrayList<Double>();
//		allHmmMetrics = new ArrayList<Metrics>();
//	}
	
	public LinkedHashMap<String, Double> getOneHMMParameters(FeatureHMM trainHmm, int knownState, LinkedHashMap<String, Double> parameters){
		int unknownState = 1 - knownState;
		
		double p0=-1.0, p1=-1.0, a00=-1.0, a01=-1.0, a10=-1.0, a11=-1.0, b00=-1.0, b01=-1.0, b10=-1.0, b11=-1.0;
		if (!opts.parameterizingInit || !opts.parameterizing){
		 p0 = trainHmm.getInitiali(unknownState, null);
		 p1 = trainHmm.getInitiali(knownState, null);
		}
		if (!opts.parameterizingTran || !opts.parameterizing){
			a00 = trainHmm.getTransitionij(unknownState, unknownState, null);
			a01 = trainHmm.getTransitionij(unknownState, knownState, null);
			a10 = trainHmm.getTransitionij(knownState, unknownState, null);
			a11 = trainHmm.getTransitionij(knownState, knownState, null);
		}
		if (!opts.allowForget ){
			a10 = 0;
			a11 = 1;
		}
		if ( !opts.parameterizingEmit || !opts.parameterizing){
			b00 = trainHmm.getEmissionjk(unknownState, 0, null);
			b01 = trainHmm.getEmissionjk(unknownState, 1, null);
			b10 = trainHmm.getEmissionjk(knownState, 0, null);
			b11 = trainHmm.getEmissionjk(knownState, 1, null);
		}
		
		parameters.put("knownState", (double) knownState);
		parameters.put("1-init", p0);
		parameters.put("1-learn", a00);
		parameters.put("learn", a01);
		parameters.put("1-guess", b00);
		parameters.put("guess", b01);
		parameters.put("init", p1);
		parameters.put("forget", a10);
		parameters.put("1-forget", a11);
		parameters.put("slip", b10);
		parameters.put("1-slip", b11);	
		
		if (opts.parameterizing) {
			Bijection featureMapping = null;
			double[] featureWeights = null;
			//String[] featureStrings = {"",""};
			if (opts.parameterizingInit){
				featureMapping = logger.curRestart.trainSummary.initFeatures;//trainHmm.getInitialPdf(0).featureMapping;
	  		featureWeights = trainHmm.getInitialPdf(0).getFeatureWeights();
	  		getFormatedFeatureMapping(featureMapping, featureWeights, parameters, "init", knownState);//featureStrings,
			}
			if (opts.parameterizingTran){
				featureMapping = logger.curRestart.trainSummary.tranFeatures;//trainHmm.getTransitionPdf(0).featureMapping;
	  		featureWeights = trainHmm.getTransitionPdf(0).getFeatureWeights();
	  		getFormatedFeatureMapping(featureMapping, featureWeights, parameters, "tran", knownState);

			}
			if (opts.parameterizingEmit){
				featureMapping = logger.curRestart.trainSummary.emitFeatures;//trainHmm.getEmissionPdf(0).featureMapping;
	  		featureWeights = trainHmm.getEmissionPdf(0).getFeatureWeights();
	  		getFormatedFeatureMapping(featureMapping, featureWeights, parameters, "emit", knownState);
			}
//			featureStrings[0] = featureStrings[0].substring(0, featureStrings[0].length() - 1);
//			featureStrings[1] = featureStrings[1].substring(0, featureStrings[1].length() - 1);
		}
//		logger.curRestartLogger.parameters = parameters;
//		logger.curRestartLogger.parameterValues = parameterValues;
		return parameters;
	}

	public void close() throws IOException {
			Date endDate = new Date();
			int diffSec = (int) ((endDate.getTime() - logger.startDate.getTime()) / (1.0 * 1000));
			String str = "\n\n***** Summary for running time *****\n" 
										+ "End:\t" + (new Date()) + "\nStart:\t" + logger.startDate + "\nDuration(sec):\t" + diffSec;
			logger.printAndLog(str);
	}
}
