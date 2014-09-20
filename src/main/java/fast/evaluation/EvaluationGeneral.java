package fast.evaluation;

import fast.hmmfeatures.Opts;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

public class EvaluationGeneral {

	public static DecimalFormat formatter;
	static {
		formatter = (DecimalFormat) DecimalFormat.getInstance(Locale.US);
		formatter.applyPattern("#.###");
	}
	public static String lineSplitter = "[,\t]";
	public static boolean outputPerItemPerformance = false;
	public static String perItemPerformanceOutFile = "";
	private static String recordInfoFilePrefix = "record-info-";
	private static String recordInfoFileSurfix = ".map";
	private static String recordInfoMapTrainInFile = "";// reconfigure();
	private static String recordInfoMapTestInFile = "";
	private static TreeMap<String, Integer> trainItemNbRecordsMap = new TreeMap<String, Integer>();
	private static TreeMap<String, Integer> itemNbKcsMap = new TreeMap<String, Integer>();
	private static ArrayList<String> testRecordItemList = new ArrayList<String>();
	private static String recordMapFilesDir = "";
	private static int itemCol = 1;
	private static int nbCols = 5;

	private static String trainFilePrefix = "train";
	private static String testFilePrefix = "test";

	// assume input is 1 run, 5 folds, if changes then need to change codes
	public static boolean extractEval = false;
	public static String extractEvalOutFile = "";
	public static String extractEvalOutFileSurfix = "-spss.txt";
	public static boolean extractMajAUC = false;
	public static String extractEvalOriHeader = "";
	public static String extractEvalNewHeaderField = "";
	public static BufferedWriter extractEvalWriter = null;
	public static ArrayList<String> extractEvalFilePreviousRecords = new ArrayList<String>();

	public static int[] nbInstancesPerFoldRun = null;
	public static boolean testSingleFile = true;
	public static int numRuns = (testSingleFile == true ? 1 : 1);
	public static int numFolds = (testSingleFile == true ? 1 : 5);
	public static boolean predAllInOneFile = false;
	public static int[] topXRange = { -1, 5 };// min,max
	public static int topX = topXRange[0];
	static double[] topXPercentRange = { -0.1, 0.2, 0.3, 0.4, 0.5, 0.6, 0.7, 0.8,
			0.9 };
	static double topXPercent = topXPercentRange[0];
	// TODO: haven't configured this yet.
	public static boolean batch = false;
	public static boolean verbose = false;

	public static String basicModelName = "FAST";
	public static String variant1ModelName = "concept-outcome";
	public static String variant2ModelName = "";
	public static boolean outputStandardPredictionFile = (basicModelName
			.contains("MiBKT") || basicModelName.contains("BKT-avg-all")) ? true
			: false;

	public static String modelName = basicModelName
			+ (variant1ModelName.equals("") ? ""
					: ("-" + variant1ModelName + (variant2ModelName.equals("") ? ""
							: ("-" + variant2ModelName))));
	public static String datasplit = "CVdatasets";
	public static String basicBasicDir = "";
	public static String basicDir = basicBasicDir + basicModelName + "/";

	public static String inOutDir = basicDir
			+ (variant1ModelName.equals("") ? variant1ModelName : (variant1ModelName
					+ "/" + (variant2ModelName.equals("") ? variant2ModelName
					: (variant2ModelName + "/"))));
	public static boolean inputPredFileHasHeader = (basicModelName
			.contains("MiBKT") || basicModelName.contains("BKT-avg-all") ? false
			: true);
	public static String outFileSurfix = ".eval";
	public static String outFile = inOutDir + modelName + "-evaluation-metrics"
			+ outFileSurfix;// instead of modelNames.get(0);
	public static String outFileNameAllModel = "all-models-evaluation-metrics"
			+ outFileSurfix;
	public static String outFileAllModel = basicBasicDir + outFileNameAllModel;

	// confusion matrix's "positive" corresponds to majority
	public static String actualLabel1correspondingStr = "correct";
	public static String actualLabel0correspondingStr = "incorrect";
	public static String majority = "correct";
	public static String minority = "incorrect";
	public static String standardPredFileSurfix = ".pred";// ".csv";
	public static String otherPredFileSurfix = (basicModelName.contains("MiBKT")
			|| basicModelName.contains("BKT-avg-all") ? ".predprob"
			: standardPredFileSurfix);
	public static String testFileSurfix = (basicModelName.contains("MiBKT")
			|| basicModelName.contains("BKT-avg-all") ? ".mibkttest" : "");
	public static String splitter = "[,\t]";
	public static String delim = "\t";
	public static String modelNameExtractor = "_";

	public static boolean outputFoldrunid = false;
	public static boolean outputNbTest = true;
	public static boolean outputWeightedAvg = true;
	public static boolean outputAUC = true;
	public static boolean outputAccuracy = true;
	public static boolean outputRMSE = true;
	public static boolean outputFmeasure = true;
	public static boolean outputPrecision = true;
	public static boolean outputRecall = true;
	public static boolean outputConfusionMatrix = true;
	public static boolean outputLL = true;// correspond to the whole dataset
	public static String outputHeader = "model"
			+ delim
			+ (outputFoldrunid ? "foldrunid" + delim : "")
			+ (outputNbTest ? "nbtest" + delim : "")
			+ (outputAUC ? (outputWeightedAvg ? "AUC" + delim : "") + "Maj.AUC"
					+ delim + "Min.AUC" + delim : "")
			+ (outputLL ? "LL" + delim + "MeanLL" + delim : "")
			+ (outputAccuracy ? "Accuracy" + delim : "")
			+ (outputRMSE ? "RMSE" + delim : "")
			+ (outputFmeasure ? (outputWeightedAvg ? "F-measure" + delim : "")
					+ "Maj.F-measure" + delim + "Min.F-measure" + delim : "")
			+ (outputPrecision ? (outputWeightedAvg ? "Precision" + delim : "")
					+ "Maj.Precision" + delim + "Min.Precision" + delim : "")
			+ (outputRecall ? (outputWeightedAvg ? "Recall" + delim : "")
					+ "Maj.Recall" + delim + "Min.Recall" + delim : "")
			+ (outputConfusionMatrix ? "TP" + delim + "TN" + delim + "FP" + delim
					+ "FN" + delim : "");
	// "model" + delim + "fold" + delim
	// + "nbInstances" + delim + "acc" + delim + "rmse" + delim + "TP" + delim
	// + "TN" + delim + "FP" + delim + "FN" + delim + "MajPrec" + delim
	// + "MinPrec" + delim + "MajRec" + delim + "MinRec\n";

	public static ArrayList<String> fileNames = new ArrayList<String>();
	public static ArrayList<String> modelNames = new ArrayList<String>();

	public static int nbItems = 94;
	public static boolean getNbDatapoints = true;
	public static boolean getNbKcs = true;
	public static boolean getItemGroupMapping = false;
	public static String itemNbKcsMapInFile = "/Users/hy/inf/Study/CS/Projects_Codes_Data/Data/Data_PAWS/EDM_UM_LE_DIFF_Data/UM_Data/tables-logs/item-nkc-mapping.txt";
	public static TreeMap<String, ItemMetrics> itemMetricsMap = new TreeMap<String, ItemMetrics>();
	public static ArrayList<ArrayList<String>> testRecordItemListAllFoldRuns = new ArrayList<ArrayList<String>>();
	public static String perItemPerformanceKcSelectionField = "";
	public static ArrayList<TreeMap<String, Integer>> trainPerItemNbDpAllFoldRuns = new ArrayList<TreeMap<String, Integer>>();

	public static class ItemMetrics {
		ArrayList<Integer> actualLabels = new ArrayList<Integer>();
		ArrayList<Integer> predictLabels = new ArrayList<Integer>();
		ArrayList<Double> predictProbs = new ArrayList<Double>();

		int[] nbTrainingDp = new int[numFolds * numRuns];
		int[] nbTestingDp = new int[numFolds * numRuns];
		double[] majAUC = new double[numFolds * numRuns];
		double[] minAUC = new double[numFolds * numRuns];
		double[] accuracy = new double[numFolds * numRuns];
		double[] rmse = new double[numFolds * numRuns];
		double[] majFmeasure = new double[numFolds * numRuns];
		double[] minFmeasure = new double[numFolds * numRuns];
		double[] majPrecision = new double[numFolds * numRuns];
		double[] minPrecision = new double[numFolds * numRuns];
		double[] majRecall = new double[numFolds * numRuns];
		double[] minRecall = new double[numFolds * numRuns];
		double[] TP = new double[numFolds * numRuns];
		double[] TN = new double[numFolds * numRuns];
		double[] FP = new double[numFolds * numRuns];
		double[] FN = new double[numFolds * numRuns];
		double LL = 0.0;
		// int nbIns = 0;
		double nbAccurate = 0;
		double squaredError = 0.0;
		double totalNbInstances = 0.0;
	}

	public static BufferedWriter allModelWriter = null;
	public static BufferedWriter allModelAllFoldRunsWriter = null;
	public static BufferedWriter perItemPerfromanceWriter = null;

	public static BufferedWriter logger = null;
	public static DateFormat dateFormat = new SimpleDateFormat(
			"yyyy/MM/dd HH:mm:ss");
	static Date loggerDate = null;
	static String loggerOutFileName = "evaluation.log";
	static String loggerOutFile = inOutDir + loggerOutFileName;

	public static void readArgs(String basicModelName_,
			String variant1ModelName_, String variant2ModelName_, int topXRange0,
			int topXRange1, String topXPercentRangeStr) {
		basicModelName = basicModelName_;
		variant1ModelName = variant1ModelName_;
		variant2ModelName = variant2ModelName_;
		topXRange[0] = topXRange0;
		topXRange[1] = topXRange1;
		topX = topXRange[0];
		basicDir = "./" + basicModelName + "/";
		numFolds = 5;
		numRuns = 1;
		String[] splitResult = topXPercentRangeStr.split("~");
		topXPercentRange = new double[splitResult.length];
		for (int s = 0; s < splitResult.length; s++)
			topXPercentRange[s] = Double.parseDouble(splitResult[s]);
		topXPercent = topXPercentRange[0];
	}

	public static void readArgs(String basicModelName_,
			String variant1ModelName_, String variant2ModelName_, int topXRange0,
			int topXRange1, String topXPercentRangeStr, boolean extractEval_) {
		basicModelName = basicModelName_;
		variant1ModelName = variant1ModelName_;
		variant2ModelName = variant2ModelName_;
		topXRange[0] = topXRange0;
		topXRange[1] = topXRange1;
		topX = topXRange[0];
		basicDir = "./" + basicModelName + "/";
		numFolds = 5;
		numRuns = 1;
		String[] splitResult = topXPercentRangeStr.split("~");
		topXPercentRange = new double[splitResult.length];
		for (int s = 0; s < splitResult.length; s++)
			topXPercentRange[s] = Double.parseDouble(splitResult[s]);
		topXPercent = topXPercentRange[0];
		extractEval = extractEval_;
	}

	public static void readArgs(String basicModelName_,
			String variant1ModelName_, int numFolds_, int numRuns_) {
		basicModelName = basicModelName_;
		variant1ModelName = variant1ModelName_;
		variant2ModelName = "";
		basicBasicDir = "./";
		basicDir = basicBasicDir + basicModelName + "/";
		topXRange[0] = -1;
		topXRange[1] = -1;
		topX = topXRange[0];
		topXPercentRange[0] = -1;
		topXPercent = topXPercentRange[0];
		numFolds = numFolds_;
		numRuns = numRuns_;
	}

	// require having standardPredFile (actualLabel,....)
	public static void evaluateOnMultiFiles(String basicModelName_,
			String variant1ModelName_, String variant2ModelName_, int numRuns_,
			int numFolds_, String outDirForComparingModels, String inOutDir,
			String outFile, String standardPredFileSurfix) throws IOException {
		// outFileNameAllModel= "all-models-evaluation-metrics" + outFileSurfix
		// loggerOutFileName = "evaluation.log";
		basicModelName = basicModelName_;
		variant1ModelName = variant1ModelName_;
		variant2ModelName = variant2ModelName_;
		topXRange[0] = -1;
		topX = -1;
		topXPercentRange[0] = -1;
		topXPercent = -1;
		numRuns = numRuns_;
		numFolds = numFolds_;
		configure();
		reconfigure();// for new model name
		run(modelName, numRuns, numFolds, inOutDir, outFile,
				(inOutDir + loggerOutFileName),
				(outDirForComparingModels + outFileNameAllModel),
				standardPredFileSurfix, "", false, "");
	}

	// require having standardPredFile (actualLabel,....)

	public static double evaluateOnMultiFiles(String modelName_, int numRuns_,
			int numFolds_, String outDirForComparingModels, String inOutDir,
			String outFile, String standardFileSurfix, String standardFilePrefix) {
		// outFileNameAllModel= "all-models-evaluation-metrics" + outFileSurfix
		// loggerOutFileName = "evaluation.log";
		modelName = modelName_;
		topXRange[0] = -1;
		topX = 0;
		topXPercentRange[0] = -1;
		topXPercent = -1;
		numRuns = numRuns_;
		numFolds = numFolds_;
		outputStandardPredictionFile = false;
		inputPredFileHasHeader = true;
		// configure();
		double auc = -1.0;
		auc = run(modelName, numRuns, numFolds, inOutDir, outFile,
				(inOutDir + loggerOutFileName),
				(outDirForComparingModels + outFileNameAllModel), standardFileSurfix,
				"", false, standardFilePrefix);
		return auc;
	}

	public static void configure() throws IOException {
		topX = topXRange[0];
		topXPercent = topXPercentRange[0];
		if (topXRange[0] > 0 && topXPercentRange[0] > 0) {
			System.out
					.println("Error: you can only select either topX or topXRange!");
			System.exit(1);
		}
		if (variant2ModelName.contains("ori")
				&& (topXRange[0] > 0 || topXPercentRange[0] > 0)) {
			System.out.println("ERROR: variant2ModelName=" + variant2ModelName
					+ ", topX=" + topX + ", topXPercent=" + topXPercent);
			System.exit(1);
		}
		if ((variant1ModelName.contains("random") || variant2ModelName
				.contains("random")) && !(topXRange[0] > 0 || topXPercentRange[0] > 0)) {
			System.out
					.println("Error: you should specify how many random KCs to select by topX(Percnet)!");
			System.exit(1);
		}
		outputStandardPredictionFile = (basicModelName.contains("MiBKT") || basicModelName
				.contains("BKT-avg-all")) ? true : false;
		inputPredFileHasHeader = (basicModelName.contains("MiBKT")
				|| basicModelName.contains("BKT-avg-all") ? false : true);
		otherPredFileSurfix = (basicModelName.contains("MiBKT")
				|| basicModelName.contains("BKT-avg-all") ? ".predprob"
				: standardPredFileSurfix);
		testFileSurfix = (basicModelName.contains("MiBKT")
				|| basicModelName.contains("BKT-avg-all") ? ".mibkttest" : "");
		if (extractEval) {
			extractEvalOutFile = basicDir + basicModelName + extractEvalOutFileSurfix;
			File curActionD = new File(extractEvalOutFile);
			if (curActionD.exists() && curActionD.isFile())
				readExtractEvalFile(extractEvalFilePreviousRecords, extractEvalOutFile);
			else {
				extractEvalFilePreviousRecords.add("");
				for (int i = 0; i < numRuns; i++)
					for (int j = 0; j < numFolds; j++)
						extractEvalFilePreviousRecords.add("");
			}
			extractEvalWriter = new BufferedWriter(new FileWriter(extractEvalOutFile));
		}
		if (outputPerItemPerformance) {
			perItemPerformanceOutFile = basicDir + "per-item-" + basicModelName
					+ outFileSurfix;
			perItemPerfromanceWriter = new BufferedWriter(new FileWriter(
					perItemPerformanceOutFile, true));
		}
	}

	// public static void getNbDatapointsOnTrain(){
	//
	//
	// }
	public static void reconfigure() throws IOException {
		datasplit = (numFolds == 1 && numRuns == 1) ? "datasets" : "CVdatasets";
		modelName = basicModelName
				+ (variant1ModelName.equals("") ? ""
						: ("-" + variant1ModelName + (variant2ModelName.equals("") ? ""
								: ("-" + variant2ModelName)
										+ (topX > 0 ? ("-top" + topX)
												: (topXPercent > 0 ? ("-topPercent" + topXPercent) : "")))));
		inOutDir = basicDir
				+ (variant1ModelName.equals("") ? variant1ModelName
						: (variant1ModelName + "/" + (variant2ModelName.equals("") ? ""
								: (variant2ModelName + (topXRange[0] > 0 ? "-top" + topX
										: (topXPercentRange[0] > 0 ? "-topPercent" + topXPercent
												: "") + ""))))) + "/";
		recordMapFilesDir = (basicModelName.contains("BKT") ? basicBasicDir
				+ "BKT-avg-all" + "/" : basicDir)
				+ (variant1ModelName.equals("") ? variant1ModelName
						: (variant1ModelName + "/" + (variant2ModelName.equals("") ? variant2ModelName
								: (variant2ModelName + (topXRange[0] > 0 ? "-top" + topX
										: (topXPercentRange[0] > 0 ? "-topPercent" + topXPercent
												: "") + ""))))) + "/" + datasplit + "/";
		inOutDir += datasplit + "/" + modelName + "/";
		// }
		outFile = inOutDir + modelName + "-evaluation-metrics" + outFileSurfix;
		loggerOutFile = inOutDir + loggerOutFileName;
		outFileAllModel = basicBasicDir + outFileNameAllModel;
		if (extractEval) {
			extractEvalNewHeaderField = variant2ModelName
					+ (topX > 0 ? ("-top" + topX)
							: (topXPercent > 0 ? ("-topPercent" + topXPercent) : ""));
			String header = extractEvalFilePreviousRecords.get(0);
			String newHeader = (header.equals("") ? "" : header + delim)
					+ extractEvalNewHeaderField;
			extractEvalFilePreviousRecords.set(0, newHeader);
		}
		if (outputPerItemPerformance) {
			if (variant2ModelName.equals("ori"))
				perItemPerformanceKcSelectionField = "100%";
			if (topXPercentRange[0] > 0)
				perItemPerformanceKcSelectionField = topXPercent > 0 ? topXPercent
						* 100 + "%" : "";
		}
	}

	public static void readExtractEvalFile(
			ArrayList<String> extractEvalFilePreviousRecords,
			String extractEvalOutFile) throws IOException {
		BufferedReader extractEvalReader = new BufferedReader(new FileReader(
				extractEvalOutFile));
		String line = "";
		while ((line = extractEvalReader.readLine()) != null) {
			extractEvalFilePreviousRecords.add(line);
		}
		extractEvalReader.close();
		if ((extractEval && extractEvalFilePreviousRecords.size() != (numRuns * numFolds) + 1)
				|| outputPerItemPerformance
				&& extractEvalFilePreviousRecords.size() != nbItems) {// 1:
			// header
			System.out
					.println("ERROR: extractEvalFilePreviousRecords.size() != (numRuns * numFolds) + 1!");
			System.exit(1);
		}
	}

	public static void main(String[] args) throws IOException {
		if (args != null && args.length > 0) {
			if (args.length >= 6) {
				String basicModelName = args[0];
				String variant1ModelName = args[1];
				String variant2ModelName = args[2];
				int topXRange0 = -1;
				int topXRange1 = -1;
				try {
					topXRange0 = Integer.parseInt(args[3]);
					topXRange1 = Integer.parseInt(args[4]);
				}
				catch (NumberFormatException e) {
					System.err.println("ERROR: 4rd and 5th arguments"
							+ " must be integers!");
					System.exit(1);
				}
				String topXPercentRangeStr = args[5];
				if (args.length == 6)
					readArgs(basicModelName, variant1ModelName, variant2ModelName,
							topXRange0, topXRange1, topXPercentRangeStr);
				if (args.length == 7) {
					boolean extractEvaluation = (args[6].equals("true") ? true : false);
					readArgs(basicModelName, variant1ModelName, variant2ModelName,
							topXRange0, topXRange1, topXPercentRangeStr, extractEvaluation);
				}
			}
			else if (args.length == 4) {
				String basicModelName = args[0];
				String variant1ModelName = args[1];
				int numFolds = 5;
				int numRuns = 1;
				try {
					numFolds = Integer.parseInt(args[2]);
					numRuns = Integer.parseInt(args[3]);
				}
				catch (NumberFormatException e) {
					System.err.println("ERROR: 3rd and 4th arguments"
							+ " must be integers!");
					System.exit(1);
				}
				readArgs(basicModelName, variant1ModelName, numFolds, numRuns);
			}
			else {
				System.out
						.println("ERROR: AUGUMENTS: basicModelName variant1ModelName variant2ModelName topXRange0 topXRange1");
				System.exit(1);
			}
		}

		configure();

		if (batch) {
			// if (modelNames.length != curDirs.length) {
			// System.out.println("modelNames and curDirs length mismatch!");
			// System.exit(1);
			// }
			// else {
			// for (int s = 0; s < modelNames.length; s++) {
			// modelNamesArrayList.add(modelNames[s]);
			// curDirsArrayList.add(curDirs[s]);
			// }
			// }
		}
		else if (topXRange[0] > 0) {
			for (topX = topXRange[1]; topX >= topXRange[0]; topX--) {
				reconfigure();
				run(modelName, numRuns, numFolds, inOutDir, outFile, loggerOutFile,
						outFileAllModel, otherPredFileSurfix, testFileSurfix,
						predAllInOneFile, "");
			}
			topX = topXRange[0];
		}
		else if (topXPercentRange[0] > 0) {
			for (int index = topXPercentRange.length - 1; index >= 0; index--) {
				topXPercent = topXPercentRange[index];
				reconfigure();
				run(modelName, numRuns, numFolds, inOutDir, outFile, loggerOutFile,
						outFileAllModel, otherPredFileSurfix, testFileSurfix,
						predAllInOneFile, "");
			}
			topXPercent = topXPercentRange[0];
		}
		else {
			reconfigure();
			run(modelName, numRuns, numFolds, inOutDir, outFile, loggerOutFile,
					outFileAllModel, otherPredFileSurfix, testFileSurfix,
					predAllInOneFile, "");
		}
		if (extractEval)
			writeExtractEvalFile(extractEvalFilePreviousRecords, extractEvalWriter);
		close();
	}

	public static void writeExtractEvalFile(
			ArrayList<String> extractEvalFilePreviousRecords,
			BufferedWriter extractEvalWriter) throws IOException {
		for (int i = 0; i < extractEvalFilePreviousRecords.size(); i++) {
			extractEvalWriter.write(extractEvalFilePreviousRecords.get(i) + "\n");
		}
	}

	public static void close() throws IOException {
		if (extractEval)
			extractEvalWriter.close();
		if (outputPerItemPerformance)
			perItemPerfromanceWriter.close();
	}

	// predFileSurfix: used to get the files to get predicted probability (and/or
	// predicted labels), for mibkt, it is .predprob(file generated by mibkt),
	// otherwise, it is usually .pred (standard pred file); if it is mibkt, then
	// use testFileSurfix to get actual
	// labels
	public static double run(String modelName, int numRuns, int numFolds,
			String inOutDir, String outFile, String loggerOutFile,
			String outFileAllModel, String predFileSurfix, String testFileSurfix,
			boolean predAllInOneFile, String standardFilePrefix) {
		double finalAuc = -1.0;

		try {
			logger = new BufferedWriter(new FileWriter(loggerOutFile, true));
			File f = new File(outFileAllModel);
			if (!(f.exists() && f.isFile())) { /* do something */
				allModelWriter = new BufferedWriter(new FileWriter(outFileAllModel,
						true));
				allModelWriter.write(outputHeader + "\n");
			}
			else
				allModelWriter = new BufferedWriter(new FileWriter(outFileAllModel,
						true));
			nbInstancesPerFoldRun = new int[numRuns * numFolds];

			// get fileNames according to the order of
			// run0-fold0~4, run1-fold0~4....
			fileNames = new ArrayList<String>();// including surfix
			modelNames = new ArrayList<String>();
			// fileName: standardPredFile or otherPredFile(.predprob)
			getFilesInSpecifiedDir(inOutDir, predFileSurfix, modelNameExtractor,
					numRuns, numFolds, fileNames, modelNames, standardFilePrefix);

			ArrayList<ArrayList<Integer>> actualLabelsAllFoldRuns = new ArrayList<ArrayList<Integer>>();
			ArrayList<ArrayList<Integer>> predictLabelsAllFoldRuns = new ArrayList<ArrayList<Integer>>();
			ArrayList<ArrayList<Double>> predictProbsAllFoldRuns = new ArrayList<ArrayList<Double>>();
			testRecordItemListAllFoldRuns = new ArrayList<ArrayList<String>>();
			trainPerItemNbDpAllFoldRuns = new ArrayList<TreeMap<String, Integer>>();

			// only when if (modelName.contains("MiBKT") ||
			// modelName.contains("BKT-avg-all")), testFileSurfix is useful (for
			// getting actual labels)
			// fileName: standardPredFile or otherPredFile(.predprob)
			input(modelName, inOutDir, fileNames, testFileSurfix, numRuns, numFolds,
					nbInstancesPerFoldRun, actualLabelsAllFoldRuns,
					predictLabelsAllFoldRuns, predictProbsAllFoldRuns,
					testRecordItemListAllFoldRuns, trainPerItemNbDpAllFoldRuns);
			if (outputStandardPredictionFile) {
				// when fileNames (with predFileSurfix) is not standardPredFile, then
				// generate them!
				outputStandardPredictionFile(actualLabelsAllFoldRuns,
						predictLabelsAllFoldRuns, predictProbsAllFoldRuns, inOutDir,
						fileNames, predFileSurfix, standardPredFileSurfix,
						nbInstancesPerFoldRun, modelNameExtractor);
			}

			finalAuc = evaluateClassifier(modelName, numFolds, numRuns,
					actualLabelsAllFoldRuns, predictLabelsAllFoldRuns,
					predictProbsAllFoldRuns, nbInstancesPerFoldRun, outFile);

			// one model(fileName) has one file including all runs
			// instead of modelNames.get(0);
			// for (int i = 0; i < fileNames.size(); i++) {
			// String curModelName = modelNames.get(i);
			// String inFile = inOutDir + fileNames.get(i);// contains all
			// // folds
			// String outFile = inOutDir + curModelName + "-evaluation-metrics"
			// + outFileSurfix;
			// evaluateClassifier(inOutDir, inFile, nbInstancesPerFoldRun,
			// outFile, curModelName);
			// }

			allModelWriter.flush();
			allModelWriter.close();
			logger.close();
		}
		catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return finalAuc;
	}

	public static void readAFieldIntoArrayList(String inFile,
			ArrayList<String> recordFieldList, int fieldCol, int nbCols)
			throws IOException {

		if (recordFieldList == null || recordFieldList.size() != 0) {
			System.out.println("ERROR: recordFieldList=" + recordFieldList);
			System.exit(1);
		}
		BufferedReader reader = new BufferedReader(new FileReader(inFile));
		String line = "";

		line = reader.readLine();
		while ((line = reader.readLine()) != null) {
			String[] splitResult = line.split(lineSplitter);
			if (splitResult.length != nbCols) {
				System.out.println("ERROR: input format incorrect " + inFile);
				System.exit(1);
			}
			String field = splitResult[fieldCol];
			itemMetricsMap.put(field, new ItemMetrics());
			recordFieldList.add(field);
		}
		printAndLog("readAFieldIntoArrayList():\t" + "#records="
				+ recordFieldList.size() + ", file=" + inFile + "\n");
	}

	public static void readAFieldIntoMap(String inFile,
			TreeMap<String, Integer> fieldMap, int fieldCol, int nbCols)
			throws IOException {
		if (fieldMap == null || fieldMap.size() != 0) {
			System.out.println("ERROR: fieldMap=" + fieldMap);
			System.exit(1);
		}
		BufferedReader reader = new BufferedReader(new FileReader(inFile));
		String line = "";

		line = reader.readLine();
		while ((line = reader.readLine()) != null) {
			String[] splitResult = line.split(lineSplitter);
			if (splitResult.length != nbCols) {
				System.out.println("ERROR: input format incorrect " + inFile);
				System.exit(1);
			}
			String field = splitResult[fieldCol];
			Integer count = fieldMap.get(field);
			if (count == null)
				count = 0;
			count++;
			fieldMap.put(field, count);
		}
		printAndLog("fieldMap():\t" + "#fields=" + fieldMap.size() + ", file="
				+ inFile + "\n");
	}

	public static void input(String modelName, String inOutDir,
			ArrayList<String> fileNames, String testFileSurfix, int numRuns,
			int numFolds, int[] nbInstancesPerFoldRun,
			ArrayList<ArrayList<Integer>> actualLabelsAllFoldRuns,
			ArrayList<ArrayList<Integer>> predictLabelsAllFoldRuns,
			ArrayList<ArrayList<Double>> predictProbsAllFoldRuns,
			ArrayList<ArrayList<String>> testRecordItemListAllFoldRuns,
			ArrayList<TreeMap<String, Integer>> trainPerItemNbDpAllFoldRuns)
			throws IOException {

		// System.out.println(modelName);
		if (modelName.contains("MiBKT") || modelName.contains("BKT-avg-all"))
			getActualLabelsInSpecifiedDir(inOutDir, testFileSurfix, numRuns,
					numFolds, actualLabelsAllFoldRuns);
		int nbIns = 0;

		for (int foldRunID = 0; foldRunID < fileNames.size(); foldRunID++) {
			if (outputPerItemPerformance) {
				testRecordItemList = new ArrayList<String>();
				trainItemNbRecordsMap = new TreeMap<String, Integer>();
				itemNbKcsMap = new TreeMap<String, Integer>();
				recordInfoMapTrainInFile = recordMapFilesDir + recordInfoFilePrefix
						+ trainFilePrefix + foldRunID + recordInfoFileSurfix;
				recordInfoMapTestInFile = recordMapFilesDir + recordInfoFilePrefix
						+ testFilePrefix + foldRunID + recordInfoFileSurfix;
				readAFieldIntoMap(recordInfoMapTrainInFile, trainItemNbRecordsMap,
						itemCol, nbCols);
				readAFieldIntoArrayList(recordInfoMapTestInFile, testRecordItemList,
						itemCol, nbCols);
				// getAKcTrainingNbDp(recordInfoMapTrainInFile);
				testRecordItemListAllFoldRuns.add(testRecordItemList);
				if (getNbKcs) {
					getTwoFieldsMapping(itemNbKcsMapInFile, itemNbKcsMap);
				}
				trainPerItemNbDpAllFoldRuns.add(trainItemNbRecordsMap);
				// if (getItemGroupMapping) {
				// getItemGroupMapping();
				// }
			}

			nbIns = 0;
			ArrayList<Integer> actualLabels = new ArrayList<Integer>();
			ArrayList<Integer> predictLabels = new ArrayList<Integer>();
			ArrayList<Double> predictProbs = new ArrayList<Double>();
			BufferedReader reader = new BufferedReader(new FileReader(inOutDir
					+ fileNames.get(foldRunID)));

			String line = "";
			if (inputPredFileHasHeader)
				line = reader.readLine();
			while ((line = reader.readLine()) != null) {
				int actualLabel = -1;
				int predictLabel = -1;
				double predictProb = -1.0;

				String[] splitResult = line.split(splitter);
				if (modelName.contains("MiBKT") || modelName.contains("BKT-avg-all")) {
					predictProb = Double.parseDouble(splitResult[0]);
					if (predictProb >= 0.5) {
						predictLabel = 1;
					}
					else {
						predictLabel = 0;
					}
				}
				else {
					actualLabel = Integer.parseInt(splitResult[0]);
					predictLabel = Integer.parseInt(splitResult[1]);
					predictProb = Double.parseDouble(splitResult[2]);
				}
				if (!(modelName.contains("MiBKT") || modelName.contains("BKT-avg-all")))
					actualLabels.add(actualLabel);
				predictLabels.add(predictLabel);
				predictProbs.add(predictProb);
				nbIns++;
			}
			reader.close();

			nbInstancesPerFoldRun[foldRunID] = nbIns;
			predictLabelsAllFoldRuns.add(predictLabels);
			predictProbsAllFoldRuns.add(predictProbs);
			if (!(modelName.contains("MiBKT") || modelName.contains("BKT-avg-all")))
				actualLabelsAllFoldRuns.add(actualLabels);
		}
	}

	public static void getTwoFieldsMapping(String inFile,
			TreeMap<String, Integer> twoFieldsMap) throws IOException {
		if (twoFieldsMap == null || twoFieldsMap.size() != 0) {
			System.out.println("ERROR: fieldMap=" + twoFieldsMap);
			System.exit(1);
		}
		BufferedReader reader = new BufferedReader(new FileReader(inFile));
		String line = "";

		line = reader.readLine();
		while ((line = reader.readLine()) != null) {
			String[] splitResult = line.split(lineSplitter);
			twoFieldsMap.put(splitResult[0], Integer.parseInt(splitResult[1]));
		}
		printAndLog("fieldMap():\t" + "#fields=" + twoFieldsMap.size() + ", file="
				+ inFile + "\n");
	}

	public static void outputStandardPredictionFile(
			ArrayList<ArrayList<Integer>> actualLabelsAllFoldRuns,
			ArrayList<ArrayList<Integer>> predictLabelsAllFoldRuns,
			ArrayList<ArrayList<Double>> predictProbsAllFoldRuns, String inOutDir,
			ArrayList<String> fileNames, String predFileSurfix,
			String standardPredFileSurfix, int[] nbInstancesPerFoldRun,
			String modelNameExtractor) throws IOException {

		if (actualLabelsAllFoldRuns == null || predictLabelsAllFoldRuns == null
				|| predictProbsAllFoldRuns == null) {
			System.out
					.println("ERROR: actualLabel, predictLabel and predictProbs AllFoldRuns are null!");
			System.exit(1);
		}
		if ((actualLabelsAllFoldRuns.size() != predictLabelsAllFoldRuns.size())
				|| (actualLabelsAllFoldRuns.size() != predictProbsAllFoldRuns.size())
				|| (predictLabelsAllFoldRuns.size() != predictProbsAllFoldRuns.size())) {
			System.out
					.println("ERROR: actualLabel, predictLabel and predictProbs AllFoldRuns size mismatch!");
			System.exit(1);
		}
		if (actualLabelsAllFoldRuns.size() != numFolds * numRuns) {
			System.out
					.println("ERROR: actualLabelsAllFoldRuns.size() != numFolds * numRuns!");
			System.exit(1);
		}
		BufferedWriter writer = null;
		for (int foldRunID = 0; foldRunID < fileNames.size(); foldRunID++) {
			String inFileName = fileNames.get(foldRunID);
			int pos = inFileName.indexOf(modelNameExtractor);
			if (pos != -1) {
				inFileName = inFileName.substring(pos + 1);
			}
			String predOutFileName = modelName
					+ "_"
					+ inFileName.substring(0,
							inFileName.length() - predFileSurfix.length())
					+ standardPredFileSurfix;
			writer = new BufferedWriter(new FileWriter(inOutDir + predOutFileName));
			writer.write("actualLabel,predictLabel,predictProb\n");

			ArrayList<Integer> actualLabels = actualLabelsAllFoldRuns.get(foldRunID);
			ArrayList<Integer> predictLabels = predictLabelsAllFoldRuns
					.get(foldRunID);
			ArrayList<Double> predictProbs = predictProbsAllFoldRuns.get(foldRunID);

			if ((actualLabels.size() != predictLabels.size())
					|| (actualLabels.size() != predictLabels.size())
					|| (predictLabels.size() != predictProbs.size())) {
				System.out
						.println("ERROR: actualLabel, predictLabel and predictProbs size mismatch!");
				System.exit(1);
			}
			if (actualLabels.size() == 1) {
				System.out.println("WARNING: actualLabel size=1!");
				// System.exit(1);
			}
			int nbIns = 0;
			for (int insId = 0; insId < actualLabels.size(); insId++) {
				nbIns++;
				int actualLabel = actualLabels.get(insId);
				int predictLabel = predictLabels.get(insId);
				double predictProb = predictProbs.get(insId);
				writer.write(actualLabel + "," + predictLabel + "," + predictProb
						+ "\n");
			}
			if (nbIns != nbInstancesPerFoldRun[foldRunID]) {
				System.out.println("ERROR: nbIns in foldRunID=" + foldRunID
						+ " is different from nbInstancesPerFoldRun[foldRunID]!");
				System.exit(1);
			}
			writer.close();
		}
	}

	public static double evaluateClassifier(String modelName, int numRuns,
			int numFolds, ArrayList<ArrayList<Integer>> actualLabelsAllFoldRuns,
			ArrayList<ArrayList<Integer>> predictLabelsAllFoldRuns,
			ArrayList<ArrayList<Double>> predictProbsAllFoldRuns,
			int[] nbInstancesPerFoldRun, String outFile) throws IOException {

		System.out.println("evaluateClassifer()...");
		if (actualLabelsAllFoldRuns == null || predictLabelsAllFoldRuns == null
				|| predictProbsAllFoldRuns == null) {
			System.out
					.println("ERROR: actualLabel, predictLabel and predictProbs AllFoldRuns are null!");
			System.exit(1);
		}
		if ((actualLabelsAllFoldRuns.size() != predictLabelsAllFoldRuns.size())
				|| (actualLabelsAllFoldRuns.size() != predictProbsAllFoldRuns.size())
				|| (predictLabelsAllFoldRuns.size() != predictProbsAllFoldRuns.size())) {
			System.out
					.println("ERROR: actualLabel, predictLabel and predictProbs AllFoldRuns size mismatch!");
			System.exit(1);
		}
		if (actualLabelsAllFoldRuns.size() != numRuns * numFolds) {
			System.out
					.println("ERROR: actualLabelsAllFoldRuns.size() != numRuns * numFolds!");
			System.exit(1);
		}

		BufferedWriter writer = new BufferedWriter(new FileWriter(outFile));
		writer.write(outputHeader + "\n");
		printAndLog(outputHeader + "\n");

		int numFoldsRuns = numFolds * numRuns;

		double[] majAUC = new double[numFoldsRuns];
		double[] minAUC = new double[numFoldsRuns];
		double[] accuracy = new double[numFoldsRuns];
		double[] rmse = new double[numFoldsRuns];
		double[] majFmeasure = new double[numFoldsRuns];
		double[] minFmeasure = new double[numFoldsRuns];
		double[] majPrecision = new double[numFoldsRuns];
		double[] minPrecision = new double[numFoldsRuns];
		double[] majRecall = new double[numFoldsRuns];
		double[] minRecall = new double[numFoldsRuns];
		double[] TP = new double[numFoldsRuns];
		double[] TN = new double[numFoldsRuns];
		double[] FP = new double[numFoldsRuns];
		double[] FN = new double[numFoldsRuns];
		double[] LL = new double[numFoldsRuns];

		int nbIns = 0;
		double nbAccurate = 0;
		double squaredError = 0.0;
		double totalNbInstances = 0.0;
		// double LL = 0.0;

		for (int foldRunID = 0; foldRunID < actualLabelsAllFoldRuns.size(); foldRunID++) {

			ArrayList<Integer> actualLabels = actualLabelsAllFoldRuns.get(foldRunID);
			ArrayList<Integer> predictLabels = predictLabelsAllFoldRuns
					.get(foldRunID);
			ArrayList<Double> predictProbs = predictProbsAllFoldRuns.get(foldRunID);
			ArrayList<String> recordItems = null;
			LL[foldRunID] = 0.0;
			if (outputPerItemPerformance) {
				recordItems = testRecordItemListAllFoldRuns.get(foldRunID);
				for (Map.Entry<String, Integer> iter : trainPerItemNbDpAllFoldRuns.get(
						foldRunID).entrySet()) {
					ItemMetrics im = itemMetricsMap.get(iter.getKey());
					im.nbTrainingDp[foldRunID] = iter.getValue();
					// System.out.println();
				}
			}

			if ((actualLabels.size() != predictLabels.size())
					|| (actualLabels.size() != predictLabels.size())
					|| (predictLabels.size() != predictProbs.size())) {
				System.out
						.println("ERROR: actualLabel, predictLabel and predictProbs size mismatch!");
				System.exit(1);
			}
			if (actualLabels.size() == 1) {
				System.out.println("WARNING: actualLabel size=1!");
				// System.exit(1);
			}

			nbIns = 0;
			for (int insId = 0; insId < actualLabels.size(); insId++) {
				nbIns++;

				int actualLabel = actualLabels.get(insId);
				int predictLabel = predictLabels.get(insId);
				double predictProb = predictProbs.get(insId);

				String item = "";
				ItemMetrics curItemMetrics = null;
				if (outputPerItemPerformance) {
					item = recordItems.get(insId);
					curItemMetrics = itemMetricsMap.get(item);
					curItemMetrics.actualLabels.add(actualLabel);
					curItemMetrics.predictLabels.add(predictLabel);
					curItemMetrics.predictProbs.add(predictProb);
					itemMetricsMap.put(item, curItemMetrics);
					// if (item.equals("jInterfaces5")) {
					// System.out.println("here!");
					// System.out.println("actualLabelSize="
					// + curItemMetrics.actualLabels.size());
					//
					// }
				}

				if (actualLabel == 1)// (predictProb >= 0.5)
					LL[foldRunID] += Math.log(predictProb);
				else
					LL[foldRunID] += Math.log(1.0 - predictProb);

				// if (predictProb >= 0.5)
				// LL[foldRunID] += Math.log(predictProb);
				// else
				// LL[foldRunID] += Math.log(1.0 - predictProb);

				if (predictProb > 1.0 || predictProb < 0.0) {
					System.out
							.println("Error:predictProb > 1.0 || predictProb < 0.0! insId="
									+ insId);
					System.exit(1);
				}
				if (predictLabel == actualLabel) {
					nbAccurate += 1;
					if (predictLabel == 1
							&& majority.equals(actualLabel1correspondingStr))
						TP[foldRunID] += 1.0;
					else if (predictLabel == 0
							&& minority.equals(actualLabel0correspondingStr))
						TN[foldRunID] += 1.0;
					else {
						System.out
								.println("ERROR: majority&minority string and actual labels mismatch!");
					}
				}
				else {
					if (predictLabel == 1
							&& majority.equals(actualLabel1correspondingStr))
						FP[foldRunID] += 1.0;
					else if (predictLabel == 0
							&& minority.equals(actualLabel0correspondingStr))
						FN[foldRunID] += 1.0;
					else {
						System.out
								.println("ERROR: majority&minority string and actual labels mismatch!");
					}
				}
				squaredError += Math.pow(actualLabel - predictProb, 2);
				if (Double.isNaN(squaredError)) {
					System.out.println("Error: squaredError is NaN");
					System.exit(1);
				}
			}
			if (nbIns != nbInstancesPerFoldRun[foldRunID]) {
				System.out.println("ERROR: nbIns in foldRunID=" + foldRunID
						+ " is different from nbInstancesPerFoldRun[foldRunID]!");
				System.exit(1);
			}

			if (outputPerItemPerformance) {
				for (Map.Entry<String, ItemMetrics> iter : itemMetricsMap.entrySet()) {
					ItemMetrics curItemMetrics = iter.getValue();
					String curItem = iter.getKey();
					Double auc = getAUC(curItemMetrics.actualLabels,
							curItemMetrics.predictProbs);
					curItemMetrics.majAUC[foldRunID] = auc;
					if (Double.isNaN(auc))
						System.out.println("auc=" + auc);
				}
			}

			majAUC[foldRunID] = getAUC(actualLabels, predictProbs);
			minAUC[foldRunID] = -1;
			// Not sure about the following method is correct (a little bit different
			// from weka...)
			// ArrayList<Integer> inverseActualLabels = new ArrayList<Integer>();
			// ArrayList<Double> inversePredictProbs = new ArrayList<Double>();
			// for (int ii = 0; ii < actualLabels.size(); ii++) {
			// int label = actualLabels.get(ii);
			// double prob = predictProbs.get(ii);
			// inverseActualLabels.add(1 - label);
			// inversePredictProbs.add(1.0 - prob);
			// }
			// minAUC[foldRunID] = getAUC(inverseActualLabels, inversePredictProbs);
			accuracy[foldRunID] = nbAccurate / nbIns;
			rmse[foldRunID] = Math.sqrt(squaredError / nbIns);
			if (rmse[foldRunID] == 0.0) {
				System.out.println("Error: RMSE=0");
				System.exit(1);
			}
			majPrecision[foldRunID] = ((TP[foldRunID] + FP[foldRunID]) == 0.0) ? 0.0
					: TP[foldRunID] / (TP[foldRunID] + FP[foldRunID]);
			minPrecision[foldRunID] = ((TN[foldRunID] + FN[foldRunID]) == 0.0) ? 0.0
					: TN[foldRunID] / (TN[foldRunID] + FN[foldRunID]);
			majRecall[foldRunID] = ((TP[foldRunID] + FN[foldRunID]) == 0.0) ? 0.0
					: TP[foldRunID] / (TP[foldRunID] + FN[foldRunID]);
			minRecall[foldRunID] = ((TN[foldRunID] + FP[foldRunID]) == 0.0) ? 0.0
					: TN[foldRunID] / (TN[foldRunID] + FP[foldRunID]);
			double denominator = majPrecision[foldRunID] + majRecall[foldRunID];
			majFmeasure[foldRunID] = denominator == 0.0 ? 0.0
					: (2 * majPrecision[foldRunID] * majRecall[foldRunID])
							/ (majPrecision[foldRunID] + majRecall[foldRunID]);
			denominator = minPrecision[foldRunID] + minRecall[foldRunID];
			minFmeasure[foldRunID] = denominator == 0.0 ? 0.0
					: (2 * minPrecision[foldRunID] * minRecall[foldRunID])
							/ (minPrecision[foldRunID] + minRecall[foldRunID]);

			String evaluationStr = modelName
					+ delim
					+ (outputFoldrunid ? foldRunID + delim : "")
					+ (outputNbTest ? nbIns + delim : "")
					+ (outputAUC ? (outputWeightedAvg ? "/" + delim : "")
							+ formatter.format(majAUC[foldRunID]) + delim + "/" + delim : "") // minAUC[foldRunID]
					+ (outputLL ? formatter.format(LL[foldRunID]) + delim
							+ formatter.format(LL[foldRunID] / nbIns) + delim : "")
					+ (outputAccuracy ? formatter.format(accuracy[foldRunID]) + delim
							: "")
					+ (outputRMSE ? formatter.format(rmse[foldRunID]) + delim : "")
					+ (outputFmeasure ? (outputWeightedAvg ? "/" + delim : "")
							+ formatter.format(majFmeasure[foldRunID]) + delim
							+ formatter.format(minFmeasure[foldRunID]) + delim : "")
					+ (outputPrecision ? (outputWeightedAvg ? "/" + delim : "")
							+ formatter.format(majPrecision[foldRunID]) + delim
							+ formatter.format(minPrecision[foldRunID]) + delim : "")
					+ (outputRecall ? (outputWeightedAvg ? "/" + delim : "")
							+ formatter.format(majRecall[foldRunID]) + delim
							+ formatter.format(minRecall[foldRunID]) + delim : "")
					+ (outputConfusionMatrix ? TP[foldRunID] + delim + TN[foldRunID]
							+ delim + FP[foldRunID] + delim + FN[foldRunID] + delim : "");
			// (outputLL ? "LL" + delim + "MeanLL" + delim : "")

			// String evaluation = modelName + delim + foldRunID + delim + nbIns +
			// delim
			// + accuracy[foldRunID] + delim + rmse[foldRunID] + delim
			// + TP[foldRunID] + delim + TN[foldRunID] + delim + FP[foldRunID]
			// + delim + FN[foldRunID] + delim + majPrecision[foldRunID] + delim
			// + minPrecision[foldRunID] + delim + majRecall[foldRunID] + delim
			// + minRecall[foldRunID] + "\n";

			writer.write(evaluationStr + "\n");
			printAndLog(evaluationStr + "\n");

			if (extractEval && extractMajAUC) {
				// the first line is header
				String oriRecord = extractEvalFilePreviousRecords.get(foldRunID + 1);
				String newRecord = (oriRecord.equals("") ? "" : oriRecord + delim)
						+ majAUC[foldRunID];
				extractEvalFilePreviousRecords.set(foldRunID + 1, newRecord);
			}

			totalNbInstances += nbIns;
			// nbInstancesPerFoldRun[foldRunID] = nbIns;
			nbIns = 0;
			nbAccurate = 0;
			squaredError = 0.0;
		}
		double avgMajAuc = 0.0;
		double avgMinAuc = 0.0;
		double avgAcc = 0.0;
		double avgRmse = 0.0;
		double avgMajFmeasure = 0.0;
		double avgMinFmeasure = 0.0;
		double avgMajPrecision = 0.0;
		double avgMinPrecision = 0.0;
		double avgMajRecall = 0.0;
		double avgMinRecall = 0.0;
		double avgTP = 0.0;
		double avgTN = 0.0;
		double avgFP = 0.0;
		double avgFN = 0.0;
		double avgLL = 0.0;

		for (int k = 0; k < numFoldsRuns; k++) {
			avgMajAuc += majAUC[k] / numFoldsRuns;
			avgMinAuc += minAUC[k] / numFoldsRuns;
			avgAcc += accuracy[k] / numFoldsRuns;
			avgRmse += rmse[k] / numFoldsRuns;
			avgMajFmeasure += majFmeasure[k] / numFoldsRuns;
			avgMinFmeasure += minFmeasure[k] / numFoldsRuns;
			avgMajPrecision += majPrecision[k] / numFoldsRuns;
			avgMinPrecision += minPrecision[k] / numFoldsRuns;
			avgMajRecall += majRecall[k] / numFoldsRuns;
			avgMinRecall += minRecall[k] / numFoldsRuns;
			avgTP += TP[k] / numFoldsRuns;
			avgTN += TN[k] / numFoldsRuns;
			avgFP += FP[k] / numFoldsRuns;
			avgFN += FN[k] / numFoldsRuns;
			avgLL += LL[k] / numFoldsRuns;
		}

		if (outputPerItemPerformance) {
			String avgFoldRunPerItemEvalStr = "";
			for (Map.Entry<String, ItemMetrics> iter : itemMetricsMap.entrySet()) {

				String curItem = iter.getKey();
				ItemMetrics curItemMetrics = iter.getValue();
				double curItemAvgMajAuc = 0.0;
				int curItemAvgTrainDp = 0;
				for (int k = 0; k < numFoldsRuns; k++) {
					curItemAvgMajAuc += curItemMetrics.majAUC[k] / numFoldsRuns;
					curItemAvgTrainDp += curItemMetrics.nbTrainingDp[k] / numFoldsRuns;
				}
				avgFoldRunPerItemEvalStr = perItemPerformanceKcSelectionField + delim
						+ curItem + delim + curItemAvgMajAuc + delim
						+ (getNbDatapoints ? curItemAvgTrainDp + delim : "")
						+ (getNbKcs ? itemNbKcsMap.get(curItem) + "" : "");
				perItemPerfromanceWriter.write(avgFoldRunPerItemEvalStr + "\n");
				System.out.println(avgFoldRunPerItemEvalStr + "\n");
				perItemPerfromanceWriter.flush();
			}
		}

		// String avg = modelName + delim + "all" + delim + totalNbInstances + delim
		// + avgAcc + delim + avgRmse + delim + avgTP + delim + avgTN + delim
		// + avgFP + delim + avgFN + delim + avgMajPrecision + delim
		// + avgMinPrecision + delim + avgMajRecall + delim + avgMinRecall + "\n";

		String avgFoldrunEvaluationStr = modelName
				+ delim
				+ (outputFoldrunid ? "all" + delim : "")
				+ (outputNbTest ? totalNbInstances + delim : "")
				+ (outputAUC ? (outputWeightedAvg ? "/" + delim : "")
						+ formatter.format(avgMajAuc) + delim + "/" + delim : "")// avgMinAuc
				+ (outputLL ? formatter.format(avgLL) + delim
						+ formatter.format(avgLL * numFoldsRuns / totalNbInstances) + delim
						: "")
				+ (outputAccuracy ? formatter.format(avgAcc) + delim : "")
				+ (outputRMSE ? formatter.format(avgRmse) + delim : "")
				+ (outputFmeasure ? (outputWeightedAvg ? "/" + delim : "")
						+ formatter.format(avgMajFmeasure) + delim
						+ formatter.format(avgMinFmeasure) + delim : "")
				+ (outputPrecision ? (outputWeightedAvg ? "/" + delim : "")
						+ formatter.format(avgMajPrecision) + delim
						+ formatter.format(avgMinPrecision) + delim : "")
				+ (outputRecall ? (outputWeightedAvg ? "/" + delim : "")
						+ formatter.format(avgMajRecall) + delim
						+ formatter.format(avgMinRecall) + delim : "")
				+ (outputConfusionMatrix ? avgTP + delim + avgTN + delim + avgFP
						+ delim + avgFN + delim : "");

		printAndLog(avgFoldrunEvaluationStr + "\n");
		writer.write(avgFoldrunEvaluationStr + "\n");
		writer.close();
		allModelWriter.write(avgFoldrunEvaluationStr + "\n");
		printAndLog("Finished!");

		return avgMajAuc;
	}

	public static double getAUC(double[] actualLabels, double[] predictProbs) {
		// double[] labels = { 1.0, 0.0 };// label
		// double[] predictions = { 1.0, 0.0 };// predictions
		Sample data = new Sample(actualLabels);
		AUC aucCalculator = new AUC();
		double auc = aucCalculator.measure(predictProbs, data);
		// System.out.println("auc=" + auc);
		return auc;
	}

	public static double getAUC(ArrayList<Integer> actualLabels,
			ArrayList<Double> predictProbs) {
		double[] actualLabelsArray = new double[actualLabels.size()];
		double[] predictProbsArray = new double[actualLabels.size()];
		for (int ii = 0; ii < actualLabels.size(); ii++) {
			actualLabelsArray[ii] = actualLabels.get(ii) * 1.0;
			predictProbsArray[ii] = predictProbs.get(ii);
		}
		double auc = getAUC(actualLabelsArray, predictProbsArray);
		return auc;
	}

	/**
	 * get fileNames according to the order of run0-fold0~4, run1-fold0~4....
	 * 
	 * @param specifiedDir
	 * @param surfix
	 * @param modelNameExtractor
	 * @param numRuns
	 * @param numFolds
	 * @param fileNames
	 * @param modelNames
	 * @throws IOException
	 */
	public static void getFilesInSpecifiedDir(String specifiedDir, String surfix,
			String modelNameExtractor, int numRuns, int numFolds,
			ArrayList<String> fileNames, ArrayList<String> modelNames,
			String standardFilePrefix) throws IOException {
		printAndLog("\n\n***************** Evaluating ******************\ngetFilesInSpecifiedDir(): "
				+ specifiedDir + "\n");
		for (int j = 0; j < numRuns; j++) {
			for (int i = 0; i < numFolds; i++) {
				String temp = "";
				fileNames.add(temp);
				modelNames.add(temp);
			}
		}

		File folder = new File(specifiedDir);
		File[] listOfFiles = folder.listFiles();
		String fileName = "";
		int j = 0;
		int realID = 0;

		for (int i = 0; i < listOfFiles.length; i++) {
			fileName = listOfFiles[i].getName();
			if (listOfFiles[i].isFile() & fileName.endsWith(surfix)
					& fileName.startsWith(standardFilePrefix)) {
				printAndLog("\tFile: " + fileName + "\n");

				int pos = fileName.indexOf(".");
				String foldIDStr = fileName.substring(pos - 1, pos);
				int foldID = Integer.parseInt(foldIDStr);
				realID = foldID;

				if (realID + 1 > fileNames.size()) {
					System.out.println("ERROR: please ensure there are only "
							+ fileNames.size() + " files with .pred surfix in " + Opts.outDir
							+ "!");
					System.exit(-1);
				}

				fileNames.set(realID, fileName);
				String[] splitResult = fileName.split(modelNameExtractor);
				modelNames.set(realID, splitResult[0]);
				j++;
			}
			else if (listOfFiles[i].isDirectory()) {
				printAndLog("Directory: " + fileName + "\n");
			}
		}
		if (j != numFolds * numRuns) {
			System.out.println("ERROR: #files should be numFolds * numRuns!");
			System.exit(1);
		}
		printAndLog("\t#Files: " + j + "\n");
		folder.delete();
	}

	// actualLabelsAll, nbInstances correspond to the order of run0,fold0~4,
	// run1,fold0~4.... run19,fold0~4
	public static ArrayList<ArrayList<Integer>> getActualLabelsInSpecifiedDir(
			String specifiedDir, String surfix, int numRuns, int numFolds,
			ArrayList<ArrayList<Integer>> actualLabelsAllFiles) throws IOException {
		if (actualLabelsAllFiles == null || actualLabelsAllFiles.size() != 0) {
			System.out
					.println("ERROR: actualLabelsAllFiles not correctly initialized1");
			System.exit(1);
		}
		for (int j = 0; j < numRuns; j++) {
			for (int i = 0; i < numFolds; i++) {
				ArrayList<Integer> temp = new ArrayList<Integer>();
				actualLabelsAllFiles.add(temp);
			}
		}

		printAndLog("getActualLabelsInSpecifiedDir(): " + specifiedDir + "\n");
		File folder = new File(specifiedDir);
		File[] listOfFiles = folder.listFiles();
		String fileName = "";
		int j = 0;

		for (int i = 0; i < listOfFiles.length; i++) {
			fileName = listOfFiles[i].getName();
			if (listOfFiles[i].isFile() & fileName.endsWith(surfix)) {
				printAndLog("File: " + fileName + "\n");
				ArrayList<Integer> actualLabels = new ArrayList<Integer>();
				BufferedReader reader = new BufferedReader(new FileReader(specifiedDir
						+ fileName));

				int realID = -1;
				if (numRuns > 1) {
					int pos = fileName.indexOf("-");
					String foldIDStr = fileName.substring(pos - 1, pos);
					int foldID = Integer.parseInt(foldIDStr);
					pos = fileName.indexOf(".");
					int pos2 = fileName.indexOf("run");
					pos2 = pos2 + 3;
					String runIDStr = fileName.substring(pos2, pos);
					int runID = Integer.parseInt(runIDStr);
					// run0,fold0~4, run1,fold0~4.... run19,fold0~4
					// runID * 5+ foldID
					realID = runID * 5 + foldID;
				}
				else {
					int pos = fileName.indexOf(".");
					String foldIDStr = fileName.substring(pos - 1, pos);
					realID = Integer.parseInt(foldIDStr);
				}

				String line = "";
				int nbIns = 0;
				while ((line = reader.readLine()) != null) {
					String[] splitResults = line.split(splitter);
					String result = splitResults[0];
					if (splitResults[0].equals("2"))
						result = "0";
					actualLabels.add(Integer.parseInt(result));
					nbIns++;
				}
				reader.close();
				nbInstancesPerFoldRun[realID] = nbIns;
				actualLabelsAllFiles.set(realID, actualLabels);
				j++;
			}
			else if (listOfFiles[i].isDirectory()) {
				printAndLog("Directory: " + fileName + "\n");
			}
		}
		printAndLog("#Files: " + j + "\n");
		folder.delete();
		return actualLabelsAllFiles;
	}

	@SuppressWarnings("deprecation")
	public static void printAndLog(String str) throws IOException {
		System.out.print(str);
		loggerDate = new Date();
		logger.write(dateFormat.format(loggerDate) + " ");
		logger.write(str);
	}

}
