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

//import java.io.BufferedWriter;
//import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Random;

import fast.common.Bijection;
import fast.common.RandomSampler;
import fast.data.CVStudent;
import fast.data.DataPoint;
import fast.data.StudentList;
//import fast.evaluation.EvaluationGeneral;
import fast.hmmfeatures.BaumWelchLearner;
import fast.hmmfeatures.BaumWelchScaledLearner;
import fast.hmmfeatures.FeatureHMM;
import fast.hmmfeatures.OpdfContextAwareLogisticRegression;

public class Train {

	public Opts opts;
	public String skillName = "";

//	public static void main(String[] args) throws IOException {
//		// [TODO] create another class for dealing with student modeling(multiple
//		// HMMs), featureHMM
//		// is for running one HMM
//		Opts opts = new Opts();
//		Experimenter featureHMM = new Experimenter(opts);
//		featureHMM.print(opts);
//		StudentList sequences = StudentList.loadData(opts.trainFile, opts);
//		featureHMM.print(sequences);
//		BufferedWriter writer = new BufferedWriter(new FileWriter(opts.mainLogFile));
//		FeatureHMM hmm = featureHMM.doTrain(sequences, "/");
//		// prediction
//		StudentList testSequences = StudentList.loadData(opts.testFile, opts);
//		// StudentList = LinkedList<Vector<DataPoint>>
//		ArrayList<Double> probs = new ArrayList<Double>();
//		ArrayList<Integer> labels = new ArrayList<Integer>();
//		ArrayList<Integer> actualLabels = new ArrayList<Integer>();
//		ArrayList<Integer> trainTestIndicator = new ArrayList<Integer>();
//
//		int lineID = 0;
//		Predict predict = new Predict(opts);
//		lineID = predict.doPredict(hmm, testSequences, probs, labels, actualLabels,
//				trainTestIndicator, lineID, featureHMM.skillName);
//		Evaluation evaluation = new Evaluation(opts);
//		evaluation.doEvaluationAndWritePred(probs, labels, actualLabels,
//				trainTestIndicator);
//		writer.close();
//	}

	public Train(Opts opts) throws IOException {
		this.opts = opts;
		// System.out.println("bias=" + opts.bias);
	}

	public void print(Opts opts) {
		System.out.println("trainfile:" + opts.trainFile);
		System.out.println("testfile:" + opts.testFile);
	}

	public void print(StudentList aHmmSeqs) throws IOException {
		String studentsStr = "";
		String outcomeStr = "";
		Bijection oriStudents = aHmmSeqs.getOriStudents();
		// Bijection finalStudents = aHmmSeqs.getFinalStudents();
		Bijection problems = aHmmSeqs.getProblems();
		// String learningCurveStr = "";
		String datapointStr2 = "";

		if (opts.writeForLearningCurve)
			opts.learningCurveWriter.write(opts.currentKc + "\n");
		for (int i = 0; i < aHmmSeqs.size(); i++) {
			if (opts.verbose)
				System.out.print("Stu:" + aHmmSeqs.getFinalStudents().get(i) + ",");
			if (opts.writeForLearningCurve) {
				opts.learningCurveWriter.write(oriStudents.get(aHmmSeqs.get(i).get(0)
						.getStudent())
						+ "\t");
			}
			for (int j = 0; j < aHmmSeqs.get(i).size(); j++) {
				DataPoint currentDataPoint = aHmmSeqs.get(i).get(j);
				String oriStudent = oriStudents.get(currentDataPoint.getStudent());
				// if (oriStudent.equals("3086"))
				// System.out.println();
				int outcome = currentDataPoint.getOutcome();
				String item = problems.get(currentDataPoint.getProblem());
				studentsStr += oriStudent + "\t";
				outcomeStr += outcome + "\t";
				datapointStr2 += item + "\t" + (j + 1) + "\t" + opts.currentKc + "\n";
				// System.out.print(oriStudent + ",");
				if (opts.verbose)
					System.out.print(outcome + "\t");
				if (opts.writeForLearningCurve) {
					opts.learningCurveWriter.write(outcome + "\t");
					opts.learningCurveWriter.flush();
				}
			}
			if (opts.verbose)
				System.out.println("");
			if (opts.writeForLearningCurve) {
				opts.learningCurveWriter.write("\n");
				opts.learningCurveWriter.flush();
			}
			if (opts.writeDatapointsLog2) {
				opts.datapointsLogWriter2.write(datapointStr2);
				opts.datapointsLogWriter2.flush();
				datapointStr2 = "";
			}
		}

		try {
			String logFileStudentStr = "";
			String logFileOutcomeStr = "";
			// KC \t iter \t LL: \t LLVALUE \t message \t real info \t...
			if (opts.oneLogisticRegression) {
				logFileStudentStr = "KC=" + opts.currentKc
						+ "\t~~~~~~\t~~~~~~\t~~~~~~\t~~~~~~\tstudent\t" + studentsStr
						+ studentsStr + "\n";
				logFileOutcomeStr = "KC=" + opts.currentKc
						+ "\t~~~~~~\t~~~~~~\t~~~~~~\t~~~~~~\toutcome\t" + outcomeStr
						+ outcomeStr + "\n";
			}
			else {
				logFileStudentStr = "KC=" + opts.currentKc
						+ "\t~~~~~~\t~~~~~~\t~~~~~~\t~~~~~~\tstudent\t" + studentsStr
						+ "student\t" + studentsStr + "\n";
				logFileOutcomeStr = "KC=" + opts.currentKc
						+ "\t~~~~~~\t~~~~~~\t~~~~~~\t~~~~~~\toutcome\t" + outcomeStr
						+ "outcome\t" + outcomeStr + "\n";
			}
			if (opts.writeDatapointsLog) {
				opts.datapointsLogWriter.write(logFileStudentStr);
				opts.datapointsLogWriter.write(logFileOutcomeStr);
				opts.datapointsLogWriter.flush();
			}
			if (opts.writeExpectedCountLog) {
				opts.expectedCountLogWriter.write(logFileStudentStr);
				opts.expectedCountLogWriter.write(logFileOutcomeStr);
				opts.expectedCountLogWriter.flush();
			}
			if (opts.writeGammaLog) {
				opts.gammaWriter.write(logFileStudentStr);
				opts.gammaWriter.write(logFileOutcomeStr);
				opts.gammaWriter.flush();
			}

		}
		catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	// hy:
	public FeatureHMM doTrain(StudentList sequences, String skillName)
			throws IOException {
		this.skillName = skillName;
		// TODO: why here?
		opts.writeForTheBestAucOnDev = true;// to control write logs
		// 1.read data including features
		Bijection finalFeatures = sequences.getFeatures();// finalFeatures, either
		if ((opts.parameterizedEmit || opts.oneBiasFeature)
				&& finalFeatures == null) {
			System.out
					.println("ERROR: (opts.parameterizedEmit || opts.oneBiasFeature) && finalFeatures == null!");
			System.exit(1);
		}

		// 2. train Hmm
		FeatureHMM hmm_ = null;
		// can be deleted, just to make sure
		opts.resetRandom(opts.wholeProcessRunId);
		RandomSampler rs = new RandomSampler(new Random(
				opts.nonFeatureParasRands[opts.currentKCIndex][opts.randomRestartId]
						.nextInt()));
		FeatureHMM hmm = getRandomFeatureHMM(rs, opts.initDirichlet,
				sequences.getOutcomes(), opts.nbHiddenStates, finalFeatures);// ,
		if (sequences.size() == 0)
			return hmm;
		if (opts.baumWelchScaledLearner) {
			BaumWelchScaledLearner bwsl = new BaumWelchScaledLearner(skillName, opts);
			bwsl.setNbIterations(opts.EM_MAX_ITERS);
			hmm_ = bwsl.learn(hmm, sequences);
		}
		else {
			BaumWelchLearner bwsl = new BaumWelchLearner(skillName, opts);
			bwsl.setNbIterations(opts.EM_MAX_ITERS);
			hmm_ = bwsl.learn(hmm, sequences);
		}

		return hmm_;
	}

//	public FeatureHMM doTrain(StudentList trainSequences, StudentList devSequences,
//			String skillName) throws IOException {
//		// if (!(opts.tuneL2 || opts.randomRestartPerHmmTimes > 1)
//		// && !(opts.combineTrainAndDev || opts.getAucOnDevPerKc)) {
//		if (!opts.useDev) {
//			System.out
//					.println("ERROR:!opts.useDev but now we are tuning or combining train and dev set or getting auc on dev set per kc!");
//			System.exit(1);
//		}
//		this.skillName = skillName;
//		// 1.read data including features
//		Bijection finalFeatures = trainSequences.getFeatures();// finalFeatures,
//																														// either
//		if ((opts.parameterizedEmit || opts.oneBiasFeature)
//				&& finalFeatures == null) {
//			System.out
//					.println("ERROR: (opts.parameterizedEmit || opts.oneBiasFeature) && finalFeatures == null!");
//			System.exit(1);
//		}
//
//		FeatureHMM hmm_ = null;
//		if (!(opts.tuneL2 || opts.randomRestartPerHmmTimes > 1)
//				&& (opts.combineTrainAndDev || opts.getAucOnDevPerKc)) {
//			if (opts.getAucOnDevPerKc) {// always before combining train and test
//				opts.writeForTheBestAucOnDev = true;// to control write logs
//				// can be deleted, just to make sure
//				opts.resetRandom(opts.wholeProcessRunId);
//				RandomSampler rs = new RandomSampler(
//						new Random(
//								opts.nonFeatureParasRands[opts.currentKCIndex][opts.randomRestartId]
//										.nextInt()));
//				FeatureHMM hmm = getRandomFeatureHMM(rs, opts.initDirichlet,
//						trainSequences.getOutcomes(), opts.nbHiddenStates, finalFeatures);// ,
//				if (trainSequences.size() == 0)
//					return hmm;
//				if (opts.baumWelchScaledLearner) {
//					BaumWelchScaledLearner bwsl = new BaumWelchScaledLearner(skillName,
//							opts);
//					bwsl.setNbIterations(opts.EM_MAX_ITERS);
//					hmm_ = bwsl.learn(hmm, trainSequences);
//				}
//				else {
//					BaumWelchLearner bwsl = new BaumWelchLearner(skillName, opts);
//					bwsl.setNbIterations(opts.EM_MAX_ITERS);
//					hmm_ = bwsl.learn(hmm, trainSequences);
//				}
//				// 3. pred on devSequences
//				Predict pred = new Predict(opts);
//				ArrayList<Double> probs = new ArrayList<Double>();
//				ArrayList<Integer> labels = new ArrayList<Integer>();
//				ArrayList<Integer> actualLabels = new ArrayList<Integer>();
//				ArrayList<Integer> trainTestIndicators = new ArrayList<Integer>();
//				ArrayList<String> students = new ArrayList<String>();
//				ArrayList<String> kcs = new ArrayList<String>();
//
//				BufferedWriter predWriter = new BufferedWriter(new FileWriter(
//						opts.predictionFile));
//				predWriter.write("actualLabel,predLabel,predProb\n");
//
//				BufferedWriter trainPredWriter = null;
//				if (opts.writeTrainPredFile) {
//					trainPredWriter = new BufferedWriter(new FileWriter(
//							opts.predictionFile + ".train"));
//					trainPredWriter.write("actualLabel,predLabel, predProb\n");
//				}
//
//				pred.doPredictAndWritePredFile(hmm_, devSequences, probs, labels,
//						actualLabels, trainTestIndicators, students, kcs, predWriter, null);// 2, skillName, 
//				predWriter.close();
//				EvaluationGeneral allFoldRunsEval = new EvaluationGeneral();
//				opts.allModelComparisonOutDir = opts.outDir;
//				String curModelName = opts.currentKc + "-reg"
//						+ opts.LBFGS_REGULARIZATION_WEIGHT;
//				double auc = -1.0;
//				auc = allFoldRunsEval.evaluateOnMultiFiles(curModelName, opts.numRuns,
//						opts.numFolds, opts.allModelComparisonOutDir, opts.outDir,
//						opts.outDir + curModelName + ".eval", opts.predSuffix, "");
//				System.out.println("\tauc=" + auc + "\n");
//				opts.bestAucOnDevAllKcsSum += auc;
//			}
//			if (opts.combineTrainAndDev) {
//				opts.writeForTheBestAucOnDev = true;// to control write logs
//				opts.resetRandom(opts.wholeProcessRunId);
//				RandomSampler rs = new RandomSampler(
//						new Random(
//								opts.nonFeatureParasRands[opts.currentKCIndex][opts.randomRestartId]
//										.nextInt()));
//				// combine train and dev
//				combineTrainAndDevSet(trainSequences,
//						trainSequences.getFinalStudents(), trainSequences.getOriStudents(),
//						devSequences, devSequences.getFinalStudents(),
//						devSequences.getOriStudents());
//				FeatureHMM hmm = getRandomFeatureHMM(rs, opts.initDirichlet,
//						trainSequences.getOutcomes(), opts.nbHiddenStates, finalFeatures);//
//				if (trainSequences.size() == 0)
//					return hmm;
//				if (opts.baumWelchScaledLearner) {
//					BaumWelchScaledLearner bwsl = new BaumWelchScaledLearner(skillName,
//							opts);
//					bwsl.setNbIterations(opts.EM_MAX_ITERS);
//					hmm_ = bwsl.learn(hmm, trainSequences);
//				}
//				else {
//					BaumWelchLearner bwsl = new BaumWelchLearner(skillName, opts);
//					bwsl.setNbIterations(opts.EM_MAX_ITERS);
//					hmm_ = bwsl.learn(hmm, trainSequences);
//				}
//			}
//			return hmm_;
//		}
//
//		FeatureHMM bestHmm = null;
//		double bestAuc = -1.0;
//		double bestRegWeight = -1.0;
//		int bestRandomRestartId = -1;
//		double auc = -1.0;
//		ArrayList<Double> regularizationRange = new ArrayList<Double>();
//		if (opts.tuneL2)
//			for (int i = 0; i < opts.LBFGS_REGULARIZATION_WEIGHT_RANGE.length; i++)
//				regularizationRange.add(opts.LBFGS_REGULARIZATION_WEIGHT_RANGE[i]);
//		else
//			regularizationRange.add(opts.LBFGS_REGULARIZATION_WEIGHT);
//		if (regularizationRange.size() == 1 && opts.randomRestartPerHmmTimes == 1)
//			opts.writeForTheBestAucOnDev = true;// to control write logs
//		else
//			opts.writeForTheBestAucOnDev = false;
//		for (opts.randomRestartId = 0; opts.randomRestartId < opts.randomRestartPerHmmTimes; opts.randomRestartId++) {
//			for (int i = 0; i < regularizationRange.size(); i++) {
//				opts.LBFGS_REGULARIZATION_WEIGHT = regularizationRange.get(i);// opts.LBFGS_REGULARIZATION_WEIGHT_RANGE[i];
//				// 2. train one Hmm
//				opts.resetRandom(opts.wholeProcessRunId);
//				RandomSampler rs = new RandomSampler(
//						new Random(
//								opts.nonFeatureParasRands[opts.currentKCIndex][opts.randomRestartId]
//										.nextInt()));
//				FeatureHMM hmm = getRandomFeatureHMM(rs, opts.initDirichlet,
//						trainSequences.getOutcomes(), opts.nbHiddenStates, finalFeatures);//
//				if (trainSequences.size() == 0)
//					return hmm;
//				if (opts.baumWelchScaledLearner) {
//					BaumWelchScaledLearner bwsl = new BaumWelchScaledLearner(skillName,
//							opts);
//					bwsl.setNbIterations(opts.EM_MAX_ITERS);
//					hmm_ = bwsl.learn(hmm, trainSequences);
//				}
//				else {
//					BaumWelchLearner bwsl = new BaumWelchLearner(skillName, opts);
//					bwsl.setNbIterations(opts.EM_MAX_ITERS);
//					hmm_ = bwsl.learn(hmm, trainSequences);
//				}
//				// 3. pred on devSequences
//				Predict pred = new Predict(opts);
//				ArrayList<Double> probs = new ArrayList<Double>();
//				ArrayList<Integer> labels = new ArrayList<Integer>();
//				ArrayList<Integer> actualLabels = new ArrayList<Integer>();
//				ArrayList<Integer> trainTestIndicators = new ArrayList<Integer>();
//				ArrayList<String> students = new ArrayList<String>();
//				ArrayList<String> kcs = new ArrayList<String>();
//
//				
//				BufferedWriter predWriter = new BufferedWriter(new FileWriter(
//						opts.predictionFile));
//				predWriter.write("actualLabel,predLabel, predProb\n");
//				pred.doPredictAndWritePredFile(hmm_, devSequences, probs, labels,
//						actualLabels, trainTestIndicators, students, kcs, predWriter, null);
//				predWriter.close();
//				EvaluationGeneral allFoldRunsEval = new EvaluationGeneral();
//				opts.allModelComparisonOutDir = opts.outDir;
//				String curModelName = opts.currentKc + "-reg"
//						+ opts.LBFGS_REGULARIZATION_WEIGHT;
//				auc = allFoldRunsEval.evaluateOnMultiFiles(curModelName, opts.numRuns,
//						opts.numFolds, opts.allModelComparisonOutDir, opts.outDir,
//						opts.outDir + curModelName + ".eval", opts.predSuffix, "");
//				// System.out.println("\tauc=" + auc);
//				if (opts.tuneL2) {
//					opts.bestRegWeightWriter.write(opts.currentKc + "\t"
//							+ "curRegWeight\t" + opts.LBFGS_REGULARIZATION_WEIGHT
//							+ "\tcurAuc\t" + auc + "\n");
//					opts.bestRegWeightWriter.flush();
//				}
//				if (auc > bestAuc) {
//					bestAuc = auc;
//					bestRegWeight = opts.LBFGS_REGULARIZATION_WEIGHT;
//					bestRandomRestartId = opts.randomRestartId;
//					bestHmm = hmm_;
//				}
//				// System.out.println("\n");
//			}
//			String str = opts.currentKc;
//			if (opts.tuneL2)
//				str += "\tbestAuc\t" + bestAuc + "\tbestRegWeight\t" + bestRegWeight;
//			else
//				str += "\tbestAucSoFar\t" + bestAuc + "\tregWeight\t" + bestRegWeight;
//			str += "\tUptill RandomRestartId\t" + opts.randomRestartId;
//			System.out.println("\n\n" + str);
//		}
//		String str = opts.currentKc;
//		if (opts.tuneL2)
//			str += "\tbestAuc\t" + bestAuc + "\tbestRegWeight\t" + bestRegWeight;
//		else
//			str += "\tbestAuc\t" + bestAuc + "\tbestRandomRestartId\t"
//					+ bestRandomRestartId + "\tregWeight\t" + bestRegWeight;
//		str += "\tOn all random restarts (#=" + opts.randomRestartPerHmmTimes + ")";
//		System.out.println(str + "\n\n");
//		// 4. log the best Hmm's paramers, combine train and dev to retrain
//		if (opts.tuneL2) {
//			opts.bestRegWeightWriter.write(str);
//			opts.bestRegWeightWriter.flush();
//		}
//
//		if (opts.getAucOnDevPerKc)
//			opts.bestAucOnDevAllKcsSum += bestAuc;
//
//		if (opts.combineTrainAndDev) {
//			opts.writeForTheBestAucOnDev = true;// to control write logs
//			opts.LBFGS_REGULARIZATION_WEIGHT = bestRegWeight;
//			opts.randomRestartId = bestRandomRestartId;
//			opts.resetRandom(opts.wholeProcessRunId);
//			RandomSampler rs = new RandomSampler(new Random(
//					opts.nonFeatureParasRands[opts.currentKCIndex][bestRandomRestartId]
//							.nextInt()));
//			// combine train and dev
//			combineTrainAndDevSet(trainSequences, trainSequences.getFinalStudents(),
//					trainSequences.getOriStudents(), devSequences,
//					devSequences.getFinalStudents(), devSequences.getOriStudents());
//			FeatureHMM hmm = getRandomFeatureHMM(rs, opts.initDirichlet,
//					trainSequences.getOutcomes(), opts.nbHiddenStates, finalFeatures);//
//			if (trainSequences.size() == 0)
//				return hmm;
//			if (opts.baumWelchScaledLearner) {
//				BaumWelchScaledLearner bwsl = new BaumWelchScaledLearner(skillName,
//						opts);
//				bwsl.setNbIterations(opts.EM_MAX_ITERS);
//				bestHmm = bwsl.learn(hmm, trainSequences);
//			}
//			else {
//				BaumWelchLearner bwsl = new BaumWelchLearner(skillName, opts);
//				bwsl.setNbIterations(opts.EM_MAX_ITERS);
//				bestHmm = bwsl.learn(hmm, trainSequences);
//			}
//		}
//
//		if (!opts.writeForTheBestAucOnDev
//				&& (opts.writeDatapointsLog || opts.writeDeltaGamma
//						|| opts.writeDeltaPCorrectOnTrain || opts.writeExpectedCountLog
//						|| opts.writeFinalFeatureWeights || opts.writeFeatureWeightsLog
//						|| opts.writeGammaLog || opts.writeGuessSlipProbLog
//						|| opts.writeGuessSlipProbLog2 || opts.writeInitLearnForgetProbLog
//						|| opts.writeLlLog || opts.writeMainLog || opts.writePerKcAucVsAvgDeltaGamma)) {
//			// to control write logs
//			opts.writeForTheBestAucOnDev = true;// to control write logs
//			opts.LBFGS_REGULARIZATION_WEIGHT = bestRegWeight;
//			opts.randomRestartId = bestRandomRestartId;
//			opts.resetRandom(opts.wholeProcessRunId);
//			RandomSampler rs = new RandomSampler(new Random(
//					opts.nonFeatureParasRands[opts.currentKCIndex][bestRandomRestartId]
//							.nextInt()));
//			FeatureHMM hmm = getRandomFeatureHMM(rs, opts.initDirichlet,
//					trainSequences.getOutcomes(), opts.nbHiddenStates, finalFeatures);// ,
//			if (trainSequences.size() == 0)
//				return hmm;
//			if (opts.baumWelchScaledLearner) {
//				BaumWelchScaledLearner bwsl = new BaumWelchScaledLearner(skillName,
//						opts);
//				bwsl.setNbIterations(opts.EM_MAX_ITERS);
//				bestHmm = bwsl.learn(hmm, trainSequences);
//			}
//			else {
//				BaumWelchLearner bwsl = new BaumWelchLearner(skillName, opts);
//				bwsl.setNbIterations(opts.EM_MAX_ITERS);
//				bestHmm = bwsl.learn(hmm, trainSequences);
//			}
//		}
//		return bestHmm;
//	}

	public void combineTrainAndDevSet(StudentList trainSequences,
			Bijection finalTrainStudents, Bijection oriTrainStudents,
			StudentList devSequences, Bijection finalDevStudents,
			Bijection oriDevStudents) {
		int nbDatapointsOnDev = 0;
		for (int devStuIndex = 0; devStuIndex < devSequences.size(); devStuIndex++) {
			// if (devStuIndex == 11)
			// System.out.println(devStuIndex);
			int aDevStudent = Integer.parseInt(finalDevStudents.get(devStuIndex));
			// System.out.println("aDevStudent=" + aDevStudent);
			int aTrainStudentIndex = finalTrainStudents.get(""
					+ oriTrainStudents.get(oriDevStudents.get(aDevStudent)));
			CVStudent aTrainStudentRecords = trainSequences.get(aTrainStudentIndex);
			CVStudent aDevStudentRecords = devSequences.get(devStuIndex);
			// System.out.println(" aTrainStudentRecords.size()="
			// + aTrainStudentRecords.size());
			for (int dpIndex = 0; dpIndex < aDevStudentRecords.size(); dpIndex++) {
				DataPoint aDevStudentDp = aDevStudentRecords.get(dpIndex);
				int fold = aDevStudentDp.getFold();
				if (fold != -1) {
					aTrainStudentRecords.add(aDevStudentDp);
					nbDatapointsOnDev += 1;
				}
				// aTrainStudentRecords.addAll(aDevStudentRecords);
			}
			// System.out.println(" aTrainStudentRecords.size()="
			// + aTrainStudentRecords.size());
			// change foldID->1
			// System.out.println();
		}
		System.out.println("#attempts(records) on dev=" + nbDatapointsOnDev);
		opts.nbDataPointsInTrainPerHmm += nbDatapointsOnDev;
		System.out.println("#attempts(records) on train+dev="
				+ opts.nbDataPointsInTrainPerHmm);
		System.out.println("#students(seqs) on train+dev=" + trainSequences.size());
		String str = "";
		if (trainSequences.getFeatures() != null)
			str = "#finalFeatures on train+dev="
					+ trainSequences.getFeatures().getSize();
		else
			str = "#finalFeatures on train+dev=0";
		System.out.println(str + "\n");
	}

	// public Bijection getNewEmitFeatures(Bijection inputEmitFeatures) {
	// newEmitFeatures = new Bijection();
	// // taylor's code by default use basic indicator features;
	// if (opts.useBasicIndicatorFeatres) {
	// for (int h = 0; h < opts.nbHiddenStates; h++)
	// for (int o = 0; o < opts.nbObsStates; o++) {
	// String featureName = String.format("%s|%d", outCome.get(o), h);
	// newEmitFeatures.put(featureName);
	// }
	// }
	//
	// if (opts.useStandardFeatures) {
	// for (int f = 0; f < inputEmitFeatures.getSize(); f++) {
	// String inputFeatureName = inputEmitFeatures.get(0);
	// for (int h = 0; h < opts.nbHiddenStates; h++)
	// for (int o = 0; o < opts.nbObsStates; o++) {
	// String featureName = String.format("%s|%s|%d", inputFeatureName,
	// outCome.get(o), h);
	// newEmitFeatures.put(featureName);
	// }
	// }
	// }
	//
	// if (opts.bias > 0) {
	// // TODO: use bias feature
	// }
	// return newEmitFeatures;
	// }

	// hy: Delete original code's static, because i think one featureHMM object
	// still can have multiple HMM instances due to different randomization
	public FeatureHMM getRandomFeatureHMM(RandomSampler rs, double dirichlet,
			Bijection outcomes, final int states, Bijection finalFeatures)
			throws IOException {// , double[][]
		// System.out.println("getRandomFeatureHMM...");
		// initialEmitFeatureWeights) {

		double paramsStates[] = new double[states];
		// double paramsEmissions[] = new double[outcomes.getSize()];
		ArrayList<OpdfContextAwareLogisticRegression> emissions = new ArrayList<OpdfContextAwareLogisticRegression>();

		Arrays.fill(paramsStates, dirichlet);
		// Arrays.fill(paramsEmissions, dirichlet);

		double[] k0 = rs.randDir(paramsStates);
		// double[] k0 = { 0.5, 0.5 };
		double[][] transitions = new double[states][states];
		// List<OpdfDataPoint> emissions = new LinkedList<OpdfDataPoint>();

		assert transitions.length == k0.length : "Number of states don't match";

		// hy*
		if (opts.allowForget) {
			for (int i = 0; i < states; i++)
				transitions[i] = rs.randDir(paramsStates);
		}
		else {
			transitions[1 - opts.hiddenState1] = rs.randDir(paramsStates);
			// transitions[0][0] = 0.5;
			// transitions[0][1] = 0.5;
			transitions[opts.hiddenState1][opts.hiddenState0] = 0.0;
			transitions[opts.hiddenState1][opts.hiddenState1] = 1.0;
		}

		// hy{
		if (opts.parameterizedEmit) {
			String outStr = "";
			// TODO: later considers opts.oneLogisticRegression condition
			// if (opts.sameInitForFeatureAndNonfeature &&
			// !opts.oneLogisticRegression) {
			// if (opts.bias > 0)
			// finalEmitFeatures.put("bias");
			// finalEmitFeatures = inputEmitFeatures;
			// int featureDimension = finalEmitFeatures.getSize();
			// double[][] initialEmitFeatureWeights = initialEmitFeatureWeights = new
			// double[opts.nbHiddenStates][featureDimension];
			// if (dirichlet == 1 && (featureDimension == 1 || opts.bias > 0)) {
			// if (featureDimension == 1) {
			// initialEmitFeatureWeights[0][0] = -4.134499025;
			// initialEmitFeatureWeights[1][0] = -0.754545583;
			// }
			// else {
			// initialEmitFeatureWeights[0][featureDimension - 1] = -4.134499025;
			// initialEmitFeatureWeights[1][featureDimension - 1] = -0.754545583;
			// }
			// }
			// else {
			// for (int s = 0; s < opts.nbHiddenStates; s++) {
			// if (opts.verbose)
			// System.out.println("\ninitial weight bounds:\tlow:\t"
			// + opts.initialWeightsBounds[s][0] + "high:\t"
			// + opts.initialWeightsBounds[s][1]);
			// initialEmitFeatureWeights[s] = uniformRandomWeights(
			// featureDimension, opts.initialWeightsBounds[s][0],
			// opts.initialWeightsBounds[s][1],
			// rands[opts.initWeightRandSeedIndex]);
			// }
			// }
			// }
			// else {
			// double[] paramsFeatures = new double[featureDimension];
			// Arrays.fill(paramsFeatures, dirichlet);
			// TODO: not to explicitly write 0,1
			if (opts.oneLogisticRegression) {
				int featureDimension = finalFeatures.getSize();
				double[] initialEmitFeatureWeights = new double[featureDimension];
				initialEmitFeatureWeights = uniformRandomWeights(featureDimension,
						opts.initialWeightsBounds[0][0], opts.initialWeightsBounds[0][1],
						opts.featureWeightsRands[opts.currentKCIndex][opts.randomRestartId]);
				if (opts.writeFeatureWeightsLog) {
					outStr = "KC=" + opts.currentKc
							+ "\t~~~\t~~~\t~~~\t~~~\thiddenStatesFeatureWeights:\t";
					for (int i = 0; i < finalFeatures.getSize(); i++) {
						String name = finalFeatures.get(i);
						outStr += name + "\t";
					}
					outStr += "\nKC=" + opts.currentKc + "\tBEFORE iter=0"
							+ "\t~~~\t~~~\t~~~\thiddenStatesFeatureWeights:\t";
					for (int i = 0; i < initialEmitFeatureWeights.length; i++) {
						outStr += initialEmitFeatureWeights[i] + "\t";
					}
				}
				OpdfContextAwareLogisticRegression emission = new OpdfContextAwareLogisticRegression(
						outcomes, initialEmitFeatureWeights, finalFeatures, opts);
				// CAUTION: even put the same emission object, later when initializing
				// hmm, actually it turns into two objects
				for (int i = 0; i < states; i++) {
					emissions.add(emission);
				}
			}
			else {// two logistic regression
				int featureDimension = finalFeatures.getSize();
				double[][] initialEmitFeatureWeights = new double[opts.nbHiddenStates][featureDimension];

				for (int s = 0; s < opts.nbHiddenStates; s++) {
					if (opts.verbose)
						System.out.println("\ninitial weight bounds:\tlow:\t"
								+ opts.initialWeightsBounds[s][0] + "high:\t"
								+ opts.initialWeightsBounds[s][1]);
					initialEmitFeatureWeights[s] = uniformRandomWeights(
							featureDimension,
							opts.initialWeightsBounds[s][0],
							opts.initialWeightsBounds[s][1],
							opts.featureWeightsRands[opts.currentKCIndex][opts.randomRestartId]);
					// initialEmitFeatureWeights[s] = rs.randDir(paramsFeatures);
				}
				if (opts.writeFeatureWeightsLog) {
					outStr = "KC=" + opts.currentKc + "\t~~~\t~~~\t~~~\t~~~\t";
					for (int h = 0; h < opts.nbHiddenStates; h++) {
						outStr += "hiddenState" + h + "FeatureWeights:\t";
						for (int i = 0; i < finalFeatures.getSize(); i++) {
							String name = finalFeatures.get(i);
							outStr += name + "\t";
						}
					}
					outStr += "\nKC=" + opts.currentKc
							+ "\tBEFORE iter=0\t~~~\t~~~\t~~~\t";
					for (int h = 0; h < opts.nbHiddenStates; h++) {
						outStr += "hiddenState" + h + "FeatureWeights:\t";
						for (int i = 0; i < initialEmitFeatureWeights[h].length; i++) {
							outStr += initialEmitFeatureWeights[h][i] + "\t";
						}
					}
				}
				for (int i = 0; i < states; i++) {
					// instead storing prob for each observation value, store just a set
					// of
					// weights for each emission logistic regression
					OpdfContextAwareLogisticRegression emission = new OpdfContextAwareLogisticRegression(
							outcomes, initialEmitFeatureWeights[i], finalFeatures, opts);
					// opts.bias, opts.guessBound, opts.slipBound, opts.hiddenState1,
					// opts.obsClass1, opts.obsClass1Name, opts.verbose);
					emissions.add(emission);
				}
			}

			if (opts.writeFeatureWeightsLog) {
				opts.featureWeightsLogWriter.write(outStr + "\n");
				opts.featureWeightsLogWriter.flush();
			}
			if (opts.LBFGS) {
				opts.regularizationWeightsForLBFGS = new double[finalFeatures.getSize()];
				opts.regularizationBiasesForLBFGS = new double[finalFeatures.getSize()];
				for (int f = 0; f < finalFeatures.getSize(); f++) {
					opts.regularizationWeightsForLBFGS[f] = opts.LBFGS_REGULARIZATION_WEIGHT;
					opts.regularizationBiasesForLBFGS[f] = opts.LBFGS_REGULARIZATION_BIAS;
					// }
				}
			}
		}
		else {// non paramtererized
			double paramsEmissions[] = new double[outcomes.getSize()];
			Arrays.fill(paramsEmissions, dirichlet);

			for (int i = 0; i < states; i++) {
				double rand[] = rs.randDir(paramsEmissions);
				// if (i == opts.hiddenState1) {// K
				// rand[1 - opts.obsClass1] *= opts.slipBound;// IC
				// rand[opts.obsClass1] = 1.0 - rand[1 - opts.obsClass1];
				// }
				// else {// NK
				// rand[opts.obsClass1] *= opts.guessBound;// C
				// rand[1 - opts.obsClass1] = 1.0 - rand[opts.obsClass1];
				// }
				OpdfContextAwareLogisticRegression emission = new OpdfContextAwareLogisticRegression(
						outcomes, rand, opts);
				// opts.bias, opts.guessBound, opts.slipBound,
				// opts.hiddenState1, opts.obsClass1, opts.obsClass1Name, opts.verbose);
				emissions.add(emission);
			}
		}

		FeatureHMM hmm = new FeatureHMM(k0, transitions, emissions, opts.parameterizedEmit);
		if (opts.verbose) {
			System.out.println("\ninitial outcomes:\t" + outcomes);
			System.out.println("initial emissions:\t" + emissions);
			String str = "\ninitial randomHMM:\t" + hmm;
			System.out.println(str);
		}
		if (opts.skillsToCheck.contains(skillName)) {
			if (opts.writeMainLog) {
				opts.mainLogWriter.write("KC=" + skillName + "\tINIT\t" + hmm + "\n");
				opts.mainLogWriter.flush();
			}
		}
		return hmm;
	}

	public static double[] uniformRandomWeights(int dim, double lower,
			double upper, Random rand) {
		double range = upper - lower;
		double[] weights = new double[dim];
		for (int i = 0; i < dim; ++i) {
			double randVal = rand.nextDouble();
			weights[i] = lower + (range * randVal);
		}
		return weights;
	}

	// static Options getOption() {
	// Options options = new Options();
	//
	// addOption(options, "data", "file", String.class,
	// "use given file for training and testing");
	// addOption(options, "fold", "int", Number.class,
	// "The fold that is used in the testing set");
	//
	// return options;
	// }

	// static void addOption(Options options, String option, String argname,
	// Object type, String description) {
	// @SuppressWarnings("static-access")
	// Option opt = OptionBuilder.withArgName(argname).hasArg().withType(type)
	// .withDescription(description).create(option);
	// options.addOption(opt);
	//
	// }

	// public static Object getOptionValue(CommandLine line, Options opts,
	// String key, Object def) throws ParseException {
	// assert opts.hasOption(key) :
	// "Trying to retrieve option that doesn't exist: "
	// + key;
	// boolean d = false;
	// Object ans;
	// if (line.hasOption(key)) {
	// ans = line.getParsedOptionValue(key);
	// if (ans instanceof Long)
	// ans = ((Long) ans).intValue();
	// }
	// else {
	// d = true;
	// ans = def;
	// }
	// // logger.debug(key + ":\t" + ans + (d ? " [default]" : ""));
	// System.out.println(key + ":\t" + ans + (d ? " [default]" : ""));
	// return ans;
	//
	// }

}
