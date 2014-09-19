/**
 * FAST v1.0       08/12/2014
 * 
 * This code is originally developed for research purpose and is still under improvement. 
 * Please email to us if you want to keep in touch with the latest release.
	 We sincerely welcome you to contact Yun Huang (huangyun.ai@gmail.com), or Jose P.Gonzalez-Brenes (josepablog@gmail.com) for problems in the code or cooperation.
 * We thank Taylor Berg-Kirkpatrick (tberg@cs.berkeley.edu) and Jean-Marc Francois (jahmm) for part of their codes that FAST is developed based on.
 *
 */

package hmmfeatures;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import common.Bijection;
import data.StudentList;
import evaluation.EvaluationGeneral;
import fig.exec.Execution;

//import org.apache.log4j.Logger;

public class Run implements Runnable {

	// public static Logger logger = Logger.getLogger("");
	// public static BufferedWriter opts.mainLogWriter = null;
	static DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");;
	// static String trainingStartDate = "";
	// static String trainingEndDate = "";
	static int hmmId = 0;
	public static Opts opts;
	// order by Bijection
	public static ArrayList<Hmm> hmms = new ArrayList<Hmm>();
	public static HashMap<String, Bijection> skillToTrainFeatures = new HashMap<String, Bijection>();

	public Run() {
		opts = new Opts();
	}

	public static void closeRun() {
		opts.closeAndClear();
		hmms.clear();
	}

	public static void main(String[] args) throws IOException {
		Run runner = new Run();
		if (args != null && args.length > 0) {
			if (args.length == 1)
				Execution.run(args, runner, runner.opts);
			else if (args.length >= 4) {// #realArguments
				opts.basicModelName = args[0];
				opts.variant1ModelName = args[1];
				opts.variant2ModelName = args[2].equals("NULL") ? "" : args[2];
				opts.basicDir = args[3];
				if (args.length == 5)
					opts.generateLRInputs = args[4].equals("true") ? true : false;
				else {
					System.out
							.println("ERROR: AUGUMENTS: studentModel/basicModelName variant1ModelName variant2ModelName basicDir [generateLRInputs]");
					System.exit(1);
				}
				opts.configure();
				runner.run();
			}
		}
		else if (args == null || args.length == 0) {
			opts.configure();
			runner.run();
		}
		else {
			System.out
					.println("ERROR: AUGUMENTS: studentModel/basicModelName variant1ModelName [variant2ModelName] or give correct conf file!");
			System.exit(1);
		}

	}

	// This is the necessary function to call to start the whole program.
	public void run() {
		opts.reconfigure();
		try {
			opts.wholeProcessRunId = 0;
			for (; opts.wholeProcessRunId < opts.randomRestartWholeProcessTimes; opts.wholeProcessRunId++) {
				opts.resetRandom(opts.wholeProcessRunId);
				// System.out.println("\nMODEL NAME = " + opts.modelName);
				if (!opts.testSingleFile)
					for (int runID = 0; runID < opts.numRuns; runID++)
						for (int foldID = 0; foldID < opts.numFolds; foldID++) {
							opts.nowInTrain = true;
							if (opts.readOneHmmOneTime)
								runOnebyOne(foldID, runID);
							else
								runBatch(foldID, runID); // foldId, runId
						}
				else {
					opts.numFolds = 1;
					opts.numRuns = 1;
					opts.nowInTrain = true;
					if (opts.readOneHmmOneTime)
						runOnebyOne(0, 0);
					else
						runBatch(0, 0); // foldId, runId
				}
				EvaluationGeneral allFoldRunsEval = new EvaluationGeneral();
				allFoldRunsEval.evaluateOnMultiFiles(opts.modelName, opts.numRuns,
						opts.numFolds, opts.allModelComparisonOutDir, opts.outDir,
						opts.outDir + opts.modelName + ".eval", opts.predSurfix,
						opts.predPrefix);
				printAndWrite();
				closeRun();
			}
		}
		catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public void printAndWrite() throws IOException {
		if (opts.getAucOnDevPerKc)
			System.out.println("\nopts.bestAucOnDevAllKcsSum="
					+ opts.bestAucOnDevAllKcsSum);
		if (opts.writePerKcTestSetAUC) {
			String str = "\naverage auc On test across allKcs="
					+ (opts.aucOnTestAllKcsSum / opts.nbHmms * 1.0);
			System.out.println(str);
			String evalOutFile = opts.outDir + opts.modelName + ".eval";
			BufferedWriter writer = new BufferedWriter(new FileWriter(evalOutFile,
					true));
			writer.write(str);
			writer.close();
			evalOutFile = opts.outDir + "evaluation.log";
			writer = new BufferedWriter(new FileWriter(evalOutFile, true));
			writer.write(str);
			writer.close();
		}
		if (opts.writePerKcAucVsAvgDeltaGamma) {
			for (Map.Entry<String, Double> iter : opts.kcAvgDeltaGammaMap.entrySet()) {
				String kc = iter.getKey();
				double avgDeltaGamma = iter.getValue();
				double testAuc = opts.kcTestAucMap.get(kc);
				opts.perKcAucVsAvgDeltaGammaWriter.write(kc + "\t" + avgDeltaGamma
						+ "\t" + testAuc + "\n");
				opts.perKcAucVsAvgDeltaGammaWriter.flush();
			}
		}
		if (opts.writeMainLog) {
			if (opts.hmmsForcedToNonParmTrainDueToLBFGSException.size() > 0) {
				opts.mainLogWriter
						.write("hmmsForcedToNonParmTrainDueToLBFGSException:\t");
				for (String hmmName : opts.hmmsForcedToNonParmTrainDueToLBFGSException) {
					opts.mainLogWriter.write(hmmName + "\t");
				}
				opts.mainLogWriter.write("\n");
			}
			else {
				opts.mainLogWriter
						.write("Running Summary:\n\tNo hmmsForcedToNonParmTrainDueToLBFGSException!\n");
			}
		}
	}

	public void readDevSet(Bijection trainSkills, Bijection devSkills,
			ArrayList<ArrayList<String>> devHmmsSequences) throws IOException {
		// if (opts.tuneL2
		// || (!opts.tuneL2 && (opts.combineTrainAndDev || opts.getAucOnDevPerKc))
		// || opts.randomRestartPerHmmTimes > 1) {
		devSkills = new Bijection();
		devHmmsSequences = new ArrayList<ArrayList<String>>();
		splitForHmms(opts.devFile, devSkills, devHmmsSequences);
		if (trainSkills.getSize() != devSkills.getSize()) {
			// logger.error("trainSkills and testSkills #skills mismatch!");
			System.out.println("\ttrainSkills and devSkills #skills mismatch!");
			System.exit(1);
		}
		else {
			for (int i = 0; i < trainSkills.getSize(); i++) {
				if (!trainSkills.get(i).equals(devSkills.get(i))) {
					// logger.warn("trainSkills and testSkills order mismatch!");
					System.out.println("\ttrainSkills and devSkills order mismatch!");
					break;
				}
			}
		}
	}

	// input should ordered by the hmm unit (e.g. skill)
	public void runOnebyOne(int foldId, int runId) throws IOException {
		String str = getConfigurationStr(dateFormat.format(new Date()), foldId,
				runId);
		System.out.println(str);
		opts.writeLogFiles(str);
		InputStream is = this.getClass().getClassLoader()
				.getResourceAsStream(opts.trainFile);
		if (is == null) {
			is = new FileInputStream(opts.trainFile);
		}
		InputStreamReader isr = new InputStreamReader(is);
		BufferedReader trainReader = new BufferedReader(isr);
		BufferedReader testReader = new BufferedReader(
				new FileReader(opts.testFile));
		String line = "";
		System.out.println("Loading train file: " + opts.trainFile + "\n"
				+ "Loading test file: " + opts.testFile);
		int lineNumber = 0;
		String header = trainReader.readLine().trim();
		String testHeader = testReader.readLine().trim();
		if (!header.equals(testHeader)) {
			System.out.println("trainHeader:" + header);
			System.out.println("testHeader:" + testHeader);
			System.out.println("ERROR: input files !header.equals(testHeader)");
			System.exit(1);
		}

		String[] headerColumns = header.split("\\s*[,\t]\\s*");// ("\\s*,\\t*\\s*");
		int skillColumn = -1;
		String reg = "KC.*";
		String reg2 = "skill.*";
		// assign KC column
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
			if (headerColumns[i].matches("(?i)" + reg)
					|| headerColumns[i].matches("(?i)" + reg2))
				skillColumn = i;
		}
		hmmId = 0;
		String preSkill = "";
		String curSkill = "";
		String testLine = "";
		String preSkillOnTest = "";
		ArrayList<String> aHmmSequences = new ArrayList<String>();
		ArrayList<String> aHmmSequencesOnTest = new ArrayList<String>();

		hmms = new ArrayList<Hmm>();
		while ((line = trainReader.readLine()) != null) {
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

			curSkill = columns[skillColumn].trim();
			// one hmm
			if (preSkill.equals("") || preSkill.equals(curSkill)) {
				if (preSkill.equals(""))
					aHmmSequences.add(header);
				aHmmSequences.add(line);
				preSkill = curSkill;
			}
			else {
				// train one hmm using aHmmSequences
				opts.currentKc = preSkill;
				opts.currentKCIndex = hmmId;
				opts.nowInTrain = true;
				Hmm trainHmm = trainOneHmm(foldId, runId, preSkill, hmmId,
						aHmmSequences, null, null);
				hmms.add(trainHmm);

				int nbTest = 0;
				while ((testLine = testReader.readLine()) != null) {
					String newLineOnTest = testLine.trim();
					if (newLineOnTest.length() == 0)
						continue;
					columns = testLine.split("\\s*[,\t]\\s*");// ("\\s*,\\t*\\s*");
					String curSkillOnTest = columns[skillColumn].trim();
					if (!preSkillOnTest.equals("") && !preSkillOnTest.equals(preSkill)) {
						System.out
								.println("WARNING: !preSkillOnTest.equals(\"\") && !preSkillOnTest.equals(preSkill): preSkillOnTest="
										+ preSkillOnTest + "," + "preSkill" + preSkill);
						System.exit(1);
					}
					if (!curSkillOnTest.equals(preSkill)
							&& aHmmSequencesOnTest.size() == 0) {
						System.out
								.println("WARNING: !curSkillOnTest.equals(preSkill) && aHmmSequencesOnTest.size() == 0: curSkillOnTest="
										+ curSkillOnTest + "," + "preSkill" + preSkill);
						System.exit(1);

					}
					nbTest++;
					if (preSkillOnTest.equals("")
							|| preSkillOnTest.equals(curSkillOnTest)) {
						if (preSkillOnTest.equals(""))
							aHmmSequencesOnTest.add(header);
						aHmmSequencesOnTest.add(testLine);
						preSkillOnTest = curSkillOnTest;
						if (!preSkillOnTest.equals(preSkill)) {
							System.out
									.println("ERROR: !preSkillOnTest.equals(preSkill): preSkillOnTest="
											+ preSkillOnTest + ",preSkill=" + preSkill);
							System.exit(1);
						}
					}
					else {
						if (!preSkillOnTest.equals(preSkill)) {
							System.out
									.println("ERROR: !preSkillOnTest.equals(preSkill): preSkillOnTest="
											+ preSkillOnTest + ",preSkill=" + preSkill);
							System.exit(1);
						}
						opts.nowInTrain = false;
						opts.newStudents = new HashSet<String>();
						opts.newItems = new HashSet<String>();
						predictOneHmmOnTest(foldId, runId, hmmId, preSkill, trainHmm,
								aHmmSequencesOnTest);
						aHmmSequencesOnTest.clear();
						aHmmSequencesOnTest = new ArrayList<String>();
						aHmmSequencesOnTest.add(header);
						aHmmSequencesOnTest.add(testLine);
						preSkillOnTest = curSkillOnTest;
						break;
					}
				}// finish testing
				aHmmSequences.clear();
				aHmmSequences = new ArrayList<String>();
				aHmmSequences.add(header);
				aHmmSequences.add(line);
				preSkill = curSkill;
				hmmId++;
				opts.nowInTrain = true;
			}
		}
		trainReader.close();
		if (aHmmSequencesOnTest != null & aHmmSequencesOnTest.size() > 0) {
			// train one hmm using aHmmSequences
			opts.currentKc = preSkill;
			opts.currentKCIndex = hmmId;
			Hmm trainHmm = trainOneHmm(foldId, runId, curSkill, hmmId, aHmmSequences,
					null, null);
			hmms.add(trainHmm);
			while ((testLine = testReader.readLine()) != null) {
				String newLineOnTest = testLine.trim();
				if (newLineOnTest.length() == 0)
					continue;
				String[] columns = testLine.split("\\s*[,\t]\\s*");// ("\\s*,\\t*\\s*");
				String curSkillOnTest = columns[skillColumn].trim();
				if (!curSkillOnTest.equals(preSkillOnTest)
						|| !curSkillOnTest.equals(curSkill)) {
					System.out
							.println("ERROR: !curSkillOnTest.equals(preSkillOnTest) || !curSkillOnTest.equals(curSkill): curSkillOnTest="
									+ curSkillOnTest
									+ ",preSkillOnTest="
									+ preSkillOnTest
									+ ",curSkill=" + curSkill);
					System.exit(1);
				}
				aHmmSequencesOnTest.add(testLine);
			}
			predictOneHmmOnTest(foldId, runId, hmmId, preSkill, trainHmm,
					aHmmSequencesOnTest);
			aHmmSequencesOnTest.clear();
			aHmmSequences.clear();
		}
		testReader.close();
	}

	public void predictOneHmmOnTest(int foldId, int runId, int hmmId,
			String kcName, Hmm trainHmm, ArrayList<String> aHmmSequences)
			throws IOException {
		String str = "\n\n******** Testing: hmmID=" + hmmId + ", skill=" + kcName
				+ ", foldID=" + foldId + ", runID=" + runId + " ************";
		System.out.println(str);
		if (opts.writeMainLog) {
			opts.mainLogWriter.write(str + "\n");
			opts.mainLogWriter.flush();
		}
		opts.nowInTrain = false;
		ArrayList<Double> probs = new ArrayList<Double>();
		ArrayList<Integer> labels = new ArrayList<Integer>();
		ArrayList<Integer> actualLabels = new ArrayList<Integer>();
		ArrayList<Integer> trainTestIndicators = new ArrayList<Integer>();
		ArrayList<double[]> features = new ArrayList<double[]>();
		predictOneHmm(trainHmm, kcName, aHmmSequences, probs, labels, actualLabels,
				trainTestIndicators, features);
	}

	public void runBatch(int foldId, int runId) throws IOException {
		int fileId = runId * opts.numFolds + foldId;
		opts.trainFile = opts.inDir + opts.trainInFilePrefix + fileId
				+ opts.inFileSurfix;
		opts.testFile = opts.inDir + opts.testInFilePrefix + fileId
				+ opts.inFileSurfix;
		if (opts.tuneL2)
			opts.devFile = opts.inDir + opts.devInFilePrefix + fileId
					+ opts.inFileSurfix;
		opts.predictionFile = opts.outDir + opts.predPrefix + fileId
				+ opts.predSurfix;

		hmms = new ArrayList<Hmm>();
		Bijection trainSkills = new Bijection();
		ArrayList<ArrayList<String>> hmmsSequences = new ArrayList<ArrayList<String>>();
		splitForHmms(opts.trainFile, trainSkills, hmmsSequences);
		if (hmmsSequences.size() != opts.nbHmms) {
			System.out.println("\nWARNING:  reset nbHmms=" + hmmsSequences.size()
					+ " (ori=" + opts.nbHmms + ")");
			opts.nbHmms = hmmsSequences.size();
		}

		// print out
		System.out.println("\n\n********************** Training, foldID=" + foldId
				+ ", runID=" + runId + " **********************");
		String startDate = dateFormat.format(new Date());
		String str = getConfigurationStr(startDate, foldId, runId,
				hmmsSequences.size());
		System.out.println(str);
		opts.writeLogFiles(str);

		// for development set
		Bijection devSkills = null;
		ArrayList<ArrayList<String>> devHmmsSequences = null;
		if (opts.useDev)
			readDevSet(trainSkills, devSkills, devHmmsSequences);

		// do real training
		for (int i = 0; i < hmmsSequences.size(); i++) {
			hmms.add(trainOneHmm(foldId, runId, trainSkills.get(i), i,
					hmmsSequences.get(i), devSkills, devHmmsSequences));
		}
		str = "\n****** Training End: " + dateFormat.format(new Date())
				+ ", Start: " + startDate + ", foldID=" + foldId + ", runID=" + foldId
				+ " ************\n\n\n";
		System.out.println(str);
		opts.writeLogFiles(str);

		// prediction and evaluation
		// TODO: try to read from file to initialize an hmm
		str = "\n\n******** Testing: " + "foldID=" + foldId + ", runID=" + runId
				+ " ************";
		System.out.println(str);
		if (opts.writeMainLog)
			opts.mainLogWriter.write(str);
		opts.nowInTrain = false;

		Bijection testSkills = new Bijection();
		hmmsSequences = new ArrayList<ArrayList<String>>();
		splitForHmms(opts.testFile, testSkills, hmmsSequences);
		System.out.println("#HMMs:" + hmmsSequences.size());
		// TODO: judge whether testSkills same as trainSkills?
		// for one fold all skills (inside the code get evaluation for one fold,
		// but also output .pred files for later evaluate on all folds)
		predict(trainSkills, testSkills, getTrainHmms(), hmmsSequences);
	}

	public Hmm trainOneHmm(int foldId, int runId, String currentKc_, int hmmId,
			ArrayList<String> aHmmSequences, Bijection devSkills,
			ArrayList<ArrayList<String>> devHmmsSequences) throws IOException {
		opts.currentKc = currentKc_;
		opts.currentKCIndex = hmmId;
		// the first one is the header
		opts.nbDataPointsInTrainPerHmm = aHmmSequences.size() - 1;

		String str = "\n\n******** Training: hmmID=" + hmmId + ", skill="
				+ currentKc_ + ", foldID=" + foldId + ", runID=" + runId
				+ " ********\n";
		String logStr = str;
		System.out.print(str);

		StudentList aHmmSeqs = StudentList.loadData(aHmmSequences, opts);
		skillToTrainFeatures.put(opts.currentKc, aHmmSeqs.getFeatures());
		Bijection finalStudents = aHmmSeqs.getFinalStudents();

		if (opts.verbose || opts.writeDatapointsLog || opts.writeExpectedCountLog
				|| opts.writeGammaLog)
			FeatureHMM.print(aHmmSeqs);
		str = "#attempts(records)="
				+ (opts.nbDataPointsInTrainPerHmm)
				+ "\n#students(seqs)="
				+ aHmmSeqs.size()
				+ "\n"
				+ "#items(questions)="
				+ aHmmSeqs.getProblems().getSize()
				+ "\n"
				+ ((aHmmSeqs.getFeatures() != null) ? "#finalFeatures="
						+ aHmmSeqs.getFeatures().getSize() + "\n" : "#finalFeatures=0"
						+ "\n");
		logStr += str;
		System.out.print(str);
		// logger.info(str);
		if (opts.writeMainLog)
			opts.mainLogWriter.write(logStr + "\n\n");
		if (opts.skillsToCheck.contains(currentKc_) && opts.writeMainLog) {
			opts.mainLogWriter.write("\nKC=" + currentKc_ + "\t#attempts(records)="
					+ (aHmmSequences.size() - 1) + "\t#students(seqs)=" + aHmmSeqs.size()
					+ "\n");
			opts.mainLogWriter.flush();
		}

		StudentList devAHmmSeqs = null;
		if (opts.useDev) {
			int curKcDevSkillsIndex = devSkills.get(opts.currentKc);// devSkillIndex
			System.out.println("\nINFO: Loading devSet KC");
			devAHmmSeqs = StudentList.loadData(
					devHmmsSequences.get(curKcDevSkillsIndex), opts);
			System.out.println("\n");
		}

		// Per skill will have one HMM
		FeatureHMM featureHMM = new FeatureHMM(opts);
		Hmm hmm = null;
		if (!opts.useDev)
			hmm = featureHMM.doTrain(aHmmSeqs, opts.currentKc);
		else
			hmm = featureHMM.doTrain(aHmmSeqs, devAHmmSeqs, opts.currentKc);
		return hmm;
	}

	public static String getConfigurationStr(String startDate, int foldID,
			int runID) {
		return getConfigurationStr(startDate, foldID, runID, 1);
	}

	public static String getConfigurationStr(String startDate, int foldID,
			int runID, int hmmsSequencesSize) {
		String l2Range = "";
		if (opts.tuneL2 && opts.LBFGS)
			for (int i = 0; i < opts.LBFGS_REGULARIZATION_WEIGHT_RANGE.length; i++)
				l2Range += opts.LBFGS_REGULARIZATION_WEIGHT_RANGE[i] + ",";
		String str = "************  Start: "
				+ startDate
				+ ", foldID="
				+ foldID
				+ ", runID="
				+ runID
				+ " *****************\nConfiguration:"
				+ "\n\tmodelName="
				+ opts.modelName

				+ "\n\ttrain="
				+ opts.trainFile
				+ "\n\ttest="
				+ opts.testFile
				+ "\n\tnumFolds="
				+ opts.numFolds
				+ "\tnumRuns="
				+ opts.numRuns
				+ "\ttestSingleFile="
				+ opts.testSingleFile

				// + "\n\tgenerateLRInputs=" + opts.generateLRInputs + "\tswapData="
				// + opts.swapData + "\treadOneHmmOneTime=" + opts.readOneHmmOneTime
				// + "\taddSharedItemDummyFeatures=" + opts.addSharedItemDummyFeatures
				// + "\taddSharedStuDummyFeatures=" + opts.addSharedStuDummyFeatures

				+ "\n\tEM_TOLERANCE="
				+ opts.EM_TOLERANCE
				+ "\tEM_MAX_ITERS="
				+ opts.EM_MAX_ITERS
				+ "\tbaumWelchScaledLearner="
				+ opts.baumWelchScaledLearner
				// + "\tremoveSeqLength1InTrain="
				// + opts.removeSeqLength1InTrain + "\tremoveSeqLength1InTest="
				// + opts.removeSeqLength1InTest

				+ "\n\tLBFGS="
				+ opts.LBFGS
				+ "\tLBFGS_TOLERANCE="
				+ opts.LBFGS_TOLERANCE
				+ "\tLBFGS_MAX_ITERS="
				+ opts.LBFGS_MAX_ITERS
				// + "\tINSTANCE_WEIGHT_ROUNDING_THRESHOLD="
				// + opts.INSTANCE_WEIGHT_ROUNDING_THRESHOLD
				+ "\tACCETABLE_LL_DECREASE="
				+ opts.ACCETABLE_LL_DECREASE
				+ "\tensureStopForLBFGS="
				+ opts.ensureStopForLBFGS

				+ "\n\tparameterizedEmit="
				+ opts.parameterizedEmit
				+ "\toneLogisticRegression="
				+ opts.oneLogisticRegression
				// + "\tuseInstanceWeightToTrainParamterezdEmit="
				// + opts.useInstanceWeightToTrainParamterezdEmit
				// + "\tuseClassWeightToTrainParamerizedEmit="
				// + opts.useClassWeightToTrainParamerizedEmit
				// + "\tuseGammaAsInstanceWeight="
				// + opts.useGammaAsInstanceWeight
				// + "\tinstanceOrClassWeightMultiplier="
				// + opts.INSTANCE_WEIGHT_MULTIPLIER

				+ "\n\tuseReg="
				+ opts.useReg
				+ "\ttuneL2="
				+ opts.tuneL2
				+ (opts.tuneL2 && opts.LBFGS ? "\tl2Range=" + l2Range : "")
				+ "\ttuneByTestset="
				+ opts.tuneByTestset
				// + "\twritePerKcTestSetAUC="
				// + opts.writePerKcTestSetAUC
				// + "\tcombineTrainAndDev="
				// + opts.combineTrainAndDev
				+ "\tLBFGS_REGULARIZATION_WEIGHT=" + opts.LBFGS_REGULARIZATION_WEIGHT
				+ "\tLBFGS_REGULARIZATION_BIAS=" + opts.LBFGS_REGULARIZATION_BIAS

				+ "\n\tbias="
				+ opts.bias
				+ "\tduplicatedBias="
				+ opts.duplicatedBias
				+ "\toneBiasFeature="
				+ opts.oneBiasFeature

				// + "\tsameInitForFeatureAndNonfeature="
				// + opts.sameInitForFeatureAndNonfeature
				+ "\n\trandomRestartWholeProcessTimes="
				+ opts.randomRestartWholeProcessTimes + "\trandomRestartPerHmmTimes="
				+ opts.randomRestartPerHmmTimes + "\tdifferentInitializationPerSkill="
				+ opts.differentInitializationPerSkill
				// + "\tinitNonParamterizedParamRandSeedIndex="
				// + opts.initNonParamterizedParamRandSeedIndex
				// + "\tinitWeightRandSeedIndex=" + opts.initWeightRandSeedIndex
				+ "\tinitDirichlet=" + opts.initDirichlet

				+ "\n\tallowForget=" + opts.allowForget

				// + "\n" + "\tturnOffItemFeaturesWhenWritingDeltaPCorrect="
				// + opts.turnOffItemFeaturesWhenWritingDeltaPCorrect
				// + "\tcoefficientWeightedByGamma=" + opts.coefficientWeightedByGamma
				// + "\tpCorrectOnTrainUsingGamma=" + opts.pCorrectOnTrainUsingGamma

				+ "\n\t#HMMs=" + hmmsSequencesSize

				+ "\n";

		return str;
	}

	/**
	 * 
	 * @param filename
	 * @param skills
	 * @param hmmsSequences
	 *          : 1st arraylist: skills; 2nd arraylist: records (mixing all
	 *          students' together)
	 * @throws IOException
	 */
	public void splitForHmms(String filename, Bijection skills,
			ArrayList<ArrayList<String>> hmmsSequences) throws IOException {
		// for each data train one hmm, get parameters and
		// get Bijection skills
		InputStream is = this.getClass().getClassLoader()
				.getResourceAsStream(filename);
		if (is == null) {
			is = new FileInputStream(filename);
		}
		InputStreamReader isr = new InputStreamReader(is);
		BufferedReader br = new BufferedReader(isr);
		String line = null;
		// logger.debug("Loading data file: " + filename);
		// System.out.println("Loading data file: " + filename);
		int lineNumber = 0;

		String header = br.readLine();
		String[] headerColumns = header.split("\\s*[,\t]\\s*");// ("\\s*,\\t*\\s*");
		int skillColumn = -1;
		String reg = "KC.*";
		String reg2 = "skill.*";
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
			if (headerColumns[i].matches("(?i)" + reg)
					|| headerColumns[i].matches("(?i)" + reg2))
				skillColumn = i;
		}
		// ArrayList<ArrayList<String>> hmmsSequences = new
		// ArrayList<ArrayList<String>>();
		// this is for input that not neccessarily order by skill, but records
		// within a student should be ordered
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

			String aSkill = columns[skillColumn].trim();
			int skillID = skills.put(aSkill);
			if (hmmsSequences.size() < skillID) {
				System.out.println("Error: skill size mismatch!");
				System.exit(1);
			}
			if (hmmsSequences.size() > skillID) {// existed skillID
				ArrayList<String> hmmSeqs = hmmsSequences.get(skillID);
				if (hmmSeqs == null) {
					hmmSeqs = new ArrayList<String>();
					hmmSeqs.add(line);
					hmmsSequences.add(hmmSeqs);
				}
				else
					hmmSeqs.add(line);
			}
			else {// a new skillID
				ArrayList<String> hmmSeqs = new ArrayList<String>();
				// TODO: now still pass header since used original code to setcolumns
				hmmSeqs.add(header);
				hmmSeqs.add(line);
				hmmsSequences.add(hmmSeqs);
			}
		}
		br.close();
		// return hmmsSequences;
	}

	// public Bijection getTrainSkills() {
	// return skills;
	// }

	public ArrayList<Hmm> getTrainHmms() {
		return hmms;
	}

	/**
	 * one fold, all skills
	 * 
	 * @param trainSkills
	 * @param testSkills
	 * @param hmms
	 * @param hmmsSequences
	 *          : hmms are ordered according to test file, can be different from
	 *          that of train file
	 * @param opts
	 * 
	 * @throws IOException
	 */
	public void predict(Bijection trainSkills, Bijection testSkills,
			ArrayList<Hmm> hmms, ArrayList<ArrayList<String>> hmmsSequences)
			throws IOException {

		if (hmmsSequences.size() != hmms.size()) {
			// logger.error("hmmsSequences and hmms length mismatch!");
			System.out.println("The #hmms(#KCs) in training and testing mismatch!");
			System.exit(1);
		}
		if (trainSkills.getSize() != testSkills.getSize()) {
			// logger.error("trainSkills and testSkills #skills mismatch!");
			System.out.println("trainSkills and testSkills #skills mismatch!");
			System.exit(1);
		}
		else {
			for (int i = 0; i < trainSkills.getSize(); i++) {
				if (!trainSkills.get(i).equals(testSkills.get(i))) {
					// logger.warn("trainSkills and testSkills order mismatch!");
					System.out
							.println("WARNING: trainSkills and testSkills order mismatch!");
					break;
					// System.exit(1);
				}
			}
		}

		// all skills
		ArrayList<Double> allProbs = new ArrayList<Double>();
		ArrayList<Integer> allLabels = new ArrayList<Integer>();
		ArrayList<Integer> allActualLabels = new ArrayList<Integer>();
		ArrayList<Integer> allTrainTestIndicator = new ArrayList<Integer>();

		int totalNbTest = 0;
		int lineID = 2;
		opts.aucOnTestAllKcsSum = 0.0;

		for (int i = 0; i < hmmsSequences.size(); i++) {
			ArrayList<Double> probs = new ArrayList<Double>();
			ArrayList<Integer> labels = new ArrayList<Integer>();
			ArrayList<Integer> actualLabels = new ArrayList<Integer>();
			ArrayList<Integer> trainTestIndicators = new ArrayList<Integer>();
			ArrayList<double[]> features = new ArrayList<double[]>();

			opts.currentKc = testSkills.get(i);
			opts.currentKCIndex = i;
			String str = "\n******* Testing:  hmmID=" + i + ", skill="
					+ testSkills.get(i) + " ********";
			System.out.println(str);// including header
			if (opts.writeMainLog)
				opts.mainLogWriter.write(str + "\n");

			if (opts.skillsToCheck.contains(testSkills.get(i))) {
				if (opts.writeMainLog) {
					opts.mainLogWriter.write("\nKC=" + testSkills.get(i)
							+ "\t#attempts(records)=" + (hmmsSequences.get(i).size() - 1));
					opts.mainLogWriter.flush();
				}
			}
			int trainSkillID = trainSkills.get(testSkills.get(i));
			if (!trainSkills.get(trainSkillID).equals(testSkills.get(i))) {
				// logger.error("trainSkill and testSkill can not be matched!"
				// + "(trainSkill=" + trainSkills.get(trainSkillID) + ",testSkill="
				// + testSkills.get(i) + ")");
				System.out
						.println("ERROR: trainSkill and testSkill can not be matched!"
								+ "(trainSkill=" + trainSkills.get(trainSkillID)
								+ ",testSkill=" + testSkills.get(i) + ")");
				System.exit(1);
			}

			Hmm hmm = hmms.get(trainSkillID);
			String KCName = testSkills.get(i);
			if (opts.skillsToCheck.contains(testSkills.get(i))) {
				if (opts.writeMainLog) {
					opts.mainLogWriter.write("\nKC=" + testSkills.get(i) + "\tFINAL\t"
							+ hmm + "\n");
					opts.mainLogWriter.flush();
				}
			}
			predictOneHmm(hmm, testSkills.get(i), hmmsSequences.get(i), probs,
					labels, actualLabels, trainTestIndicators, features);
			allProbs.addAll(probs);
			allLabels.addAll(labels);
			allActualLabels.addAll(actualLabels);
			allTrainTestIndicator.addAll(trainTestIndicators);
		}

		// evaluation (one fold all skills)
		Evaluation evaluation = new Evaluation(opts);
		int realTotalTest = evaluation.doEvaluationAndWritePred(allProbs,
				allLabels, allActualLabels, allTrainTestIndicator);
		totalNbTest += realTotalTest;
		// String str = "\t#test instances:" + totalNbTest;
		// System.out.println(str);
		// if (opts.writeMainLog)
		// opts.mainLogWriter.write(str + "\n");

	}

	public void predictOneHmm(Hmm hmm, String kcName,
			ArrayList<String> aHmmSequence, ArrayList<Double> probs,
			ArrayList<Integer> labels, ArrayList<Integer> actualLabels,
			ArrayList<Integer> trainTestIndicators, ArrayList<double[]> features)
			throws IOException {
		StudentList hmmSequences = null;
		if (!opts.inputProvideFeatureColumns) {
			// TODO: figure out why I need this assignment? Seems to configure
			// trainFeatures as final features for test
			StudentList.trainFeatures = skillToTrainFeatures.get(kcName);
			hmmSequences = StudentList.loadData(aHmmSequence, opts);
		}
		else {
			// System.out.println("#skillsInTrain:" + skillToTrainFeatures.size());
			hmmSequences = StudentList.loadData(aHmmSequence, opts);
			Bijection testFeatures = hmmSequences.getFeatures();
			if (skillToTrainFeatures.get(kcName).getSize() != testFeatures.getSize()) {
				System.out.println("ERROR:trainFeatures.getSize()="
						+ skillToTrainFeatures.get(kcName).getSize()
						+ ",testFeatures.getSize()=" + testFeatures.getSize());
				System.exit(1);
			}
			else {
				for (int index = 0; index < skillToTrainFeatures.get(kcName).getSize(); index++) {
					String featureName = skillToTrainFeatures.get(kcName).get(index);
					if (!featureName.equals(testFeatures.get(index))) {
						// TODO: 1) need to use Bijection trainFeatures' order to re-arrange
						// featureVector, only use train feature vector's length (meaning
						// that
						// extra features on test will be set 0 and trimed)
						System.out
								.println("ERROR:trainFeatures.get(index) != testFeatures.get(index)) ");
						System.exit(1);
					}
				}
			}
		}

		// not including the header
		String str = "#attempts(records)=" + (aHmmSequence.size() - 1) + "\n";
		str += "#students(sequences)=" + hmmSequences.size() + "\n";
		str += "#items(questions)=" + hmmSequences.getProblems().getSize() + "\n";
		if (hmmSequences.getFeatures() != null)
			str += "#finalFeatures=" + hmmSequences.getFeatures().getSize() + "\n";
		else
			str += "#finalFeatures=0" + "\n";
		if (!opts.inputProvideFeatureColumns) {
			opts.upTillNowNewStudents.addAll(opts.newStudents);
			opts.upTillNowNewItems.addAll(opts.newItems);

			if (opts.addSharedStuDummyFeatures)
				str += "#newStudents=" + opts.newStudents.size()
						+ "\n#upTillNowNewStudents=" + opts.upTillNowNewStudents.size()
						+ "\n";
			if (opts.addSharedItemDummyFeatures)
				str += "#newItems=" + opts.newItems.size() + "\n#upTillNowNewItems="
						+ opts.upTillNowNewItems.size() + "\n";
		}
		System.out.println(str);
		// logger.info(str);
		if (opts.writeMainLog) {
			opts.mainLogWriter.write(str + "\n");
			opts.mainLogWriter.flush();
		}

		int lineID = 0;
		Predict pred = new Predict(opts);
		if (opts.generateLRInputs) {
			// in order to get features and trainTestIndicators
			lineID = pred.doPredict(hmm, hmmSequences, probs, labels, actualLabels,
					trainTestIndicators, lineID, kcName, features);
		}
		else {
			if (opts.writePerKcTestSetAUC) {
				BufferedWriter predWriter = new BufferedWriter(new FileWriter(
						opts.outDir + opts.currentKc + "." + opts.currentKc));
				predWriter.write("actualLabel,predLabel, predProb\n");
				pred.doPredictAndWritePredFile(hmm, hmmSequences, probs, labels,
						actualLabels, trainTestIndicators, 2, kcName, predWriter, null);
				predWriter.close();
				EvaluationGeneral allFoldRunsEval = new EvaluationGeneral();
				opts.allModelComparisonOutDir = opts.outDir;
				String curModelName = opts.currentKc;
				double auc = -1.0;
				auc = allFoldRunsEval.evaluateOnMultiFiles(curModelName, opts.numRuns,
						opts.numFolds, opts.allModelComparisonOutDir, opts.outDir,
						opts.outDir + curModelName + ".eval", "." + opts.currentKc, "");
				System.out.println("currentKc=" + opts.currentKc + "\tauc=" + auc
						+ "\n");
				opts.kcTestAucMap.put(opts.currentKc, auc);
				opts.aucOnTestAllKcsSum += auc;
			}
			if (opts.readOneHmmOneTime) {
				BufferedWriter predWriter = null;
				File curActionD = new File(opts.predictionFile);
				if (!curActionD.exists()) {
					predWriter = new BufferedWriter(new FileWriter(opts.predictionFile,
							true));
					predWriter.write("actualLabel,predLabel, predProb\n");
				}
				else {
					predWriter = new BufferedWriter(new FileWriter(opts.predictionFile,
							true));
				}

				BufferedWriter trainPredWriter = null;
				if (opts.writeTrainPredFile) {
					String file = opts.predictionFile + ".train";
					curActionD = new File(file);
					if (!curActionD.exists()) {
						trainPredWriter = new BufferedWriter(new FileWriter(file, true));
						trainPredWriter.write("actualLabel,predLabel, predProb\n");
					}
					else {
						trainPredWriter = new BufferedWriter(new FileWriter(file, true));
					}
				}

				if (opts.writePerKcTestSetAUC) {
					for (int index = 0; index < probs.size(); index++) {
						String s = actualLabels.get(index) + "," + labels.get(index) + ","
								+ opts.formatter2.format(probs.get(index));
						if (trainTestIndicators.get(index) == -1) {
							if (opts.writeTrainPredFile)
								trainPredWriter.write(s + "\n");
						}
						else {
							predWriter.write(s + "\n");
						}
					}
				}
				else
					pred.doPredictAndWritePredFile(hmm, hmmSequences, probs, labels,
							actualLabels, trainTestIndicators, lineID, kcName, predWriter,
							trainPredWriter);
				predWriter.close();
				trainPredWriter.close();
			}
			else {
				lineID = pred.doPredict(hmm, hmmSequences, probs, labels, actualLabels,
						trainTestIndicators, lineID, kcName);
			}
		}// !opts.generateLRInputs

		if (opts.generateLRInputs)
			try {
				generateTestLROutSideFile(hmmSequences.getFeatures(), actualLabels,
						features, trainTestIndicators);
			}
			catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
	}

	// public static void print(StudentList sequences) {
	// for (int i = 0; i < sequences.size(); i++) {
	// System.out.print("Stu" + i + ":\t");
	// for (int j = 0; j < sequences.get(i).size(); j++) {
	// System.out.print(sequences.get(i).get(j).getOutcome() + "\t");
	// }
	// System.out.println("");
	// }
	// }
	public void generateTestLROutSideFile(Bijection featureMapping,
			ArrayList<Integer> actualLabels, ArrayList<double[]> features,
			ArrayList<Integer> trainTestIndicators) throws Exception {
		String wgtStr = "";
		String generalLRFeatureStr = "";
		String generalLRLabelStr = "";
		String liblinearDataStr = "";
		String wekaDataStr = "";
		ArrayList<String> wekaDataStrs = new ArrayList<String>();
		ArrayList<Integer> nonTrainInstanceActualLabels = new ArrayList<Integer>();
		opts.testByWekaInputDataFile = opts.outDir
				+ (opts.nowInTrain ? opts.curFoldRunTrainInFilePrefix
						: opts.curFoldRunTestInFilePrefix) + "_" + opts.currentKc + ".csv";
		opts.testByWekaInputDataWriter = new BufferedWriter(new FileWriter(
				opts.testByWekaInputDataFile));
		boolean writeHeader = false;

		for (int ins = 0; ins < actualLabels.size(); ins++) {
			if (trainTestIndicators.get(ins) == -1)
				continue;
			if (ins % 1000 == 0)
				System.out.println("generateTestLROutSideFile: ins=" + ins);
			double[] curInsFeatures = features.get(ins);
			for (int f = 0; f < curInsFeatures.length; f++) {
				double aFeature = curInsFeatures[f];
				wekaDataStr += aFeature + ",";
			}
			wekaDataStr += (actualLabels.get(ins) == 0 ? "incorrect" : "correct");

			nonTrainInstanceActualLabels.add(actualLabels.get(ins));
			if (opts.swapData)
				wekaDataStrs.add(wekaDataStr);
			else {
				if (!writeHeader) {
					String header = "";
					for (int f = 0; f < featureMapping.getSize(); f++)
						header += featureMapping.get(f) + ",";
					header += "label";
					opts.testByWekaInputDataWriter.write(header + "\n");
					writeHeader = true;
				}
				opts.testByWekaInputDataWriter.write(wekaDataStr + "\n");
				opts.testByWekaInputDataWriter.flush();
			}
			wgtStr = "";
			generalLRFeatureStr = "";
			generalLRLabelStr = "";
			liblinearDataStr = "";
			wekaDataStr = "";
		}
		// swap targets to make 1 ("correct") appears first, so that Cp(which
		// corresponds to prob.y>0 and weighted_C[0](which corresponds to the
		// first
		// appearing label)) can always correspond to 1 ("correct"))
		if (nonTrainInstanceActualLabels.size() == 0) {
			System.out.println("INFO: nonTrainInstanceActualLabels.size() == 0!");
			return;
		}
		if (opts.swapData) {
			if (nonTrainInstanceActualLabels.get(0) != opts.obsClass1) {
				for (int k = 1; k < nonTrainInstanceActualLabels.size(); k++)
					if (nonTrainInstanceActualLabels.get(k) == opts.obsClass1) {
						// targets[k] == opts.obsClass1
						swap(wekaDataStrs, 0, k);
						System.out.println("swap");
						break;
					}
			}
			String header = "";
			for (int f = 0; f < featureMapping.getSize(); f++)
				header += featureMapping.get(f) + ",";
			header += "label";
			opts.testByWekaInputDataWriter.write(header + "\n");
			for (int ins = 0; ins < wekaDataStrs.size(); ins++) {
				opts.testByWekaInputDataWriter.write(wekaDataStrs.get(ins) + "\n");
				opts.testByWekaInputDataWriter.flush();
			}
		}
		opts.testByWekaInputDataWriter.close();
	}

	public void swap(ArrayList<String> dataStrs, int i, int j) {
		String temp = dataStrs.get(i);
		dataStrs.set(i, dataStrs.get(j));
		dataStrs.set(j, temp);
	}
}
