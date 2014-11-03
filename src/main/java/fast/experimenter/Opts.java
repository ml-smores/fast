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
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.HashSet;
import java.util.Locale;
import java.util.Random;
import java.util.TreeMap;
import fig.basic.Option;

public class Opts {

	@Option(gloss = "basicModelName is either \"FAST\" or \"KT\". ")
			//+ "basicModelName, variant1ModelName and variant2ModelName are used to configure modelName and in/out directories. However, you can specify modelName and inDir/outDir directly.")
	public String basicModelName = "FAST";
	@Option(gloss = "variant1ModelName could be empty or contain any strings.")
			//+ "basicModelName, variant1ModelName and variant2ModelName are used to configure modelName and in/out directories. However, you can specify modelName and inDir/outDir directly.")
	public String variant1ModelName = "item";
	@Option(gloss = "")
	public String variant2ModelName = "";
	public String basicDir = "./examples/example_data/";
	@Option(gloss = "Human friendly description of the model")
	public String modelName = basicModelName + (variant1ModelName.equals("") ? "" : ("-" + variant1ModelName + (variant2ModelName.equals("") ? ""
							: ("-" + variant2ModelName))));//
	@Option(gloss = "testSingleFile is used to decide just run one train test pair or multiple train, test pairs.")
	public boolean testSingleFile = true;
	@Option(gloss = "numFolds and numRuns are used to decide how many times FAST runs. For testSingleFile=true, numFolds and numRuns should be set to 1. The code uses foldID to change the train, test file name (e.g. train0.txt~train9.txt are for two runs of 5 fold CV or one run of 10 fold CV) foldId = runID * opts.numFolds + foldID")
	public int numFolds = 1;
	@Option(gloss = "numFolds and numRuns are used to decide how many times FAST runs. For testSingleFile=true, numFolds and numRuns should be set to 1. The code uses foldID to change the train, test file name (e.g. train0.txt~train9.txt are for two runs of 5 fold CV or one run of 10 fold CV) foldId = runID * opts.numFolds + foldID")
	public int numRuns = 1;
	@Option(gloss = "datasplit is used to help configure outDir.")
	public String datasplit = testSingleFile ? "datasets" : "CVdatasets";
	@Option(gloss = "inDir is where FAST get input files(train and test).")
	public String inDir = basicDir;
	// basicDir
	// + basicModelName
	// + "/"
	// + (variant1ModelName.equals("") ? (datasplit + "/") : (variant1ModelName
	// + "/" + datasplit + "/"));
	@Option(gloss = "outDir is where FAST output prediction files and log files.")
	public String outDir = "./output/";// inDir + modelName + "/";
	@Option(gloss = "allModelComparisonOutDir is where all different models are compared (by the average evaluation metric).")
	public String allModelComparisonOutDir = outDir;// basicDir;
	@Option(gloss = "If just one train test pair, then train(test) file will be \"train(test)InFilePreifix0.txt\". If there are multiple train, test pairs, then based on the configuration of numFolds and numRuns, train(test) file will be \"train(test)InFilePreifixFoldId.txt\", e.g. \"train1.txt\" or \"test1.txt\" for the second fold first run.")
	public String trainInFilePrefix = "train";
	@Option(gloss = "If just one train test pair, then train(test) file will be \"train(test)InFilePreifix0.txt\". If there are multiple train, test pairs, then based on the configuration of numFolds and numRuns, train(test) file will be \"train(test)InFilePreifixFoldId.txt\", e.g. \"train1.txt\" or \"test1.txt\" for the second fold first run.")
	public String testInFilePrefix = "test";
	public String devInFilePrefix = "dev";
	@Option(gloss = "The suffix for train and test files.")
	public String inFileSuffix = ".txt";
	// @Option(gloss =
	// "ratio is used to configure the name of input train/test files when FAST is training on the \"ratio\" of a sequence and testing on remaining of the sequence. Yet you can set it as 0 and gives the standard names (train.txt, test.txt)")
	public String ratio = "";// "0.5";
	@Option(gloss = "If there are more than one runs, these are configured dynamically in each run with changing foldID, e.g. train(test) file will be \"train(test)InFilePreifixFoldId.txt\", e.g. \"train0.txt\" or \"test0.txt\" for the first fold.")
	public String trainFile = inDir + trainInFilePrefix + "0" + inFileSuffix;
	@Option(gloss = "If there are more than one runs, these are configured dynamically in each run with changing foldID, e.g. train(test) file will be \"train(test)InFilePreifixFoldId.txt\", e.g. \"train0.txt\" or \"test0.txt\" for the first fold.")
	public String testFile = inDir + testInFilePrefix + "0" + inFileSuffix;
	public String devFile = inDir + devInFilePrefix + "0" + inFileSuffix;
	public String curFoldRunTrainInFilePrefix = "";
	public String curFoldRunTestInFilePrefix = "";
	public String curFoldRunDevInFilePrefix = "";
	@Option(gloss = "predSuffix=.pred is neccessary if we are to use the evaluation code inside this code.")
	public String predSuffix = ".pred";
	// predictionFile should contain modelName, because modelName and predSuffix
	// are used together to identify prediction file automatically by Evaluation
	// engine.
	public String predPrefix = modelName + "_test";
	public String predictionFile = outDir + predPrefix + "0" + predSuffix;

	// This part is for configuring regularization.
	public boolean useReg = true;
	// tuneL2 is for tuning the weights of L2 regularization.
	public boolean tuneL2 = false;
	public boolean useDev = false;
	public boolean combineTrainAndDev = false;
	public boolean tuneByTestset = false;

	@Option(gloss = "By default, configure baumWelchScaledLearner=true meaning that we use baumWelchScaledLearner.")
	public boolean baumWelchScaledLearner = true;
	@Option(gloss = "TOLERANCE is used to decide the convergence of outer EM. Setting bigger value could make training stop earlier.")
	public double EM_TOLERANCE = 1.0E-6;// 1.0E-6(EDM paper);1.0E-4(quick
																			// experiments)
	@Option(gloss = "EM_MAX_ITERS is used for maximum iteration of outer EM. Setting smaller value could make training stop earlier.")
	public int EM_MAX_ITERS = 400;// usual:400(EDM paper)

	public boolean removeSeqLength1InTrain = false;
	public boolean removeSeqLength1InTest = false;

	@Option(gloss = "parameterizedEmit can be configured as true by the code itself or by outside specification.")
	public boolean parameterizedEmit = true;
	// @Option(gloss =
	// "bias<0 means no bias, >0 means adding a bias to the featue space. This configuration is only activated when running FAST (not KT).")
	public double bias = 1.0;// -1.0
	// TODO: check @Option(gloss =
	// "oneBiasFeatue=true means just using one  feature. These two can also configures by putting \"bias\" and \"1bias\" in the variant2ModelName")
	public boolean oneBiasFeature = false;
	// @Option(gloss =
	// "duplicatedBias=true means using one bias feature for \"known\" state and another bias feature for the \"unknown\" state. To set it true is necessary when other features are all shared by two states.")
	public boolean duplicatedBias = true;

	// @Option(gloss =
	// "These are for both LIBLINEAR and LBFGS. By default use gamma as instance weight and use it(instead of class weight) to train parameterized emission.")
	public boolean useGammaAsInstanceWeight = true;
	// @Option(gloss =
	// "These are for both LIBLINEAR and LBFGS. By default use gamma as instance weight and use it(instead of class weight) to train parameterized emission.")
	public boolean useInstanceWeightToTrainParamterezdEmit = true;
	// @Option(gloss =
	// "These are for both LIBLINEAR and LBFGS. By default use gamma as instance weight and use it(instead of class weight) to train parameterized emission.")
	public boolean useClassWeightToTrainParamerizedEmit = false;
	// Option(gloss = "By default, just use one LR for emission probabilities.")
	public boolean oneLogisticRegression = true;

	@Option(gloss = "LBFGS=true means use LBFGS to optimize the logistic regression part; (deprecated: otherwise use LIBLINEAR.)")
	public boolean LBFGS = true;
	@Option(gloss = "LBFGS_TOLERANCE is used to decide the convergence of inner LBFGS. Setting bigger value could make training stop earlier.")
	public double LBFGS_TOLERANCE = 1.0E-6;// usual:1.0E-6(EDM paper);
	@Option(gloss = "LBFGS_MAX_ITERS is used for maximum iteration of inner LBFGS. Setting smaller value could make training stop earlier.")
	public int LBFGS_MAX_ITERS = 200;// 500;// usual:500(EDM paper)
	@Option(gloss = "For LBFGS, regulariztion term is sum_i[ c*(w_i - b)^2 ] where c is regularization weight and b is regularization bias.")
	public double LBFGS_REGULARIZATION_WEIGHT = useReg ? 1.0 : 0.0;
	public double[] LBFGS_REGULARIZATION_WEIGHT_RANGE = { 0.01, 0.1, 1, 10, 100 };
	// { 1.0E-5, 1.0E-4,1.0E-3, 0.01, 0.1, 1, 5, 10, 30, 50, 100 };//{ 1.0 };
	public double[] regularizationWeightsForLBFGS;
	@Option(gloss = "For LBFGS, regulariztion term is sum_i[ c*(w_i - b)^2 ] where c is regularization weight and b is regularization bias.")
	public double LBFGS_REGULARIZATION_BIAS = 0.0;
	public double[] regularizationBiasesForLBFGS;
	// @Option(gloss =
	// "ensureStopForLBFGS stops LBFGS by force when LL changes smaller than LBFGS tolerance.")
	// public boolean ensureStopForLBFGS = true;
	@Option(gloss = "forceSetInstanceWeightForLBFGS>0 will set the instance weight by the value by force only for LBFGS.")
	public double forceSetInstanceWeightForLBFGS = -1.0;

	public double LIBLINEAR_C_PENALTY = 1;


	public double INSTANCE_WEIGHT_ROUNDING_THRESHOLD =  -1.0;// : 1.0E-4;// -1.0;//
	public double INSTANCE_WEIGHT_MULTIPLIER = 1.0;

	// @Option(gloss =
	// "used to decide whether use same intialization for featureHMM and non-featureHMM. Only useful when use 1 bias feature version of featureHMM")
	// public boolean sameInitForFeatureAndNonfeature = false;
	public int randomRestartPerHmmTimes = 1;
	public int nbHmms = 500;
	public int randomRestartWholeProcessTimes = 1;
	public boolean differentInitializationPerSkill = true;
	@Option(gloss = "for initializing both paramerized and non-paramtereized paramters' values (usually set 1).")
	public double initDirichlet = 1;// take care to change
	@Option(gloss = "for deciding lower and upper bound for each emission probability.")
	public double[][] initialWeightsBounds = { { -0.1, 0.1 }, { -0.1, 0.1 } };
	// existed papers: maximum probabilities of initial knowledge, guess, slip and
	// learning are 0.85, 0.3, 0.1 and 0.3, respectively.
	// public double guessBound = 0.5;
	// public double slipBound = 0.5;
	@Option(gloss = "Now by default, allows forget.")
	public boolean allowForget = true;
	@Option(gloss = "To decide printing out verbose information or not.")
	public boolean verbose = false;

	/*
	@Option(gloss = "To decide printing out verbose information for LBFGS optimization result or not.")
	public boolean LBFGSverbose = false;
	@Option(gloss = "To decide printing out verbose information for LBFGS optimization iteration or not.")
	
	public boolean LBFGS_PRINT_MINIMIZER = false; */
	@Option(gloss = "EPS is for avoiding divided by 0.")
	public double EPS = 1e-10;
	
	
	@Option(gloss = "EXPECTED_COUNT_SMALL_VALUE is used to decide whether expected count is too small and if so print out warning in log files.")
	public double EXPECTED_COUNT_SMALL_VALUE = 1.0E-4;// 1.0E-6;
	@Option(gloss = "ACCETABLE_LL_DECREASE is used to decide whether LL decreas is too small and if so print out warning in log files.")
	public double ACCETABLE_LL_DECREASE = 1.0E-3;
	@Option(gloss = "INIT_LL is used to determine initial LL to compute the first LL change.")
	public double INIT_LL = -1.0E10;
	// @Option(gloss =
	// "To decide print out intermediate files for testing liblinear or LBFGS outside.")
	public boolean testLiblinear = false;
	public boolean testLogsticRegression = false;
	public String skillToTest = "ArrayList";

	public boolean useEmissionToJudgeHiddenStates = false;
	public boolean useTransitionToJudgeHiddenStates = useEmissionToJudgeHiddenStates ? false
			: true;
	// @Option(gloss =
	// "These are decided by default to give convinience to do inference")
	// [hiddenState1] in prediction, will decide again by seeing
	// P(correct|hiddenState); in other places are used to initialize forget (if
	// don't allow forget)
	// public int realHiddenState1 = 1;// known
	// public int realHiddenState0 = 1 - realHiddenState1;// unknown
	public int hiddenState1 = 1;// known
	public int hiddenState0 = 1 - hiddenState1;// unknown
	// [obsClass1] correspond to "correct", decides: (1)reading input, put
	// "incorrect" first
	// (2)LR always output p(Correct). After these, obsClass1 is always "correct"
	@Option(gloss = "These are decided by default to give convinience to do inference and decide LR's probabilities")
	public int obsClass1 = 1;
	@Option(gloss = "For discrete classification, the string that is going to be class # 1")
	public String obsClass1Name = "correct";
	@Option(gloss = "These are decided by default to give convinience to do inference and decide LR's probabilities")
	public int nbHiddenStates = 2;
	@Option(gloss = "These are decided by default to give convinience to do inference and decide LR's probabilities")
	public int nbObsStates = 2;

	@Option(gloss = "for decising what information to log.")
	public boolean writeFinalHmmParameters = false;
	@Option(gloss = "for decising what information to log.")
	public boolean writeMainLog = true;
	// @Option(gloss = "for decising what information to log.")
	public boolean writeLlLog = false;
	// @Option(gloss = "for decising what information to log.")
	public boolean writeExpectedCountLog = false;
	// if write final feature weights, plz see above config.
	// @Option(gloss = "for decising what information to log.")
	public boolean writeFeatureWeightsLog = false;
	// @Option(gloss = "for decising what information to log.")
	public boolean writeGammaLog = false;
	// @Option(gloss = "for decising what information to log.")
	public boolean writeInitLearnForgetProbLog = false;
	// @Option(gloss = "for decising what information to log.")
	public boolean writeGuessSlipProbLog = false;
	// @Option(gloss = "for decising what information to log.")
	public boolean writeGuessSlipProbLog2 = false;
	// @Option(gloss = "for decising what information to log.")
	public boolean writeDatapointsLog = false;
	// 3 columns: item, practice opp, skillname
	public boolean writeDatapointsLog2 = false;
	public boolean writeForLearningCurve = false;
	// public boolean writeForItemVsPractice = true;
	public boolean writeBestRegWeightLog = false;

	// These configuration determine later how to write
	public boolean writeDeltaGamma = false;
	public boolean writeOnlyDeltaGamma = false;
	public boolean writePerKcAucVsAvgDeltaGamma = false;
	// one delta is for one student on one skill, they are arranged in one column
	public boolean writeEachDeltaGamma = false;
	// one delta is for one student on one skill, they are arranged in one column
	// public boolean writePerKcAucVsDeltaGamma3 = false;
	public boolean writePerKcTestSetAUC = false;
	// TODO: later use it: public boolean perkcaucontest = true;// true
	public boolean getAucOnDevPerKc = false;

	// determines later how to write
	// TODO: formatting the output:
	@Option(gloss = "for decising what information to log.")
	public boolean writeFinalFeatureWeights = false;
	// TODO: formatting the output: Option(gloss =
	// "writeOnlyOriginalFinalFeatureWeights=False but writeFinalFeatureWeights=True, then will write also the difference of feature weights between two states.")
	public boolean writeOnlyOriginalFinalFeatureWeights = true;
	public boolean coefficientWeightedByGamma = false;
	public boolean writeDeltaPCorrectOnTrain = false;
	public boolean pCorrectOnTrainUsingGamma = false;
	public boolean turnOffItemFeaturesWhenWritingDeltaPCorrect = false;

	// These are the configuration for experimenting FAST+IRT.
	public boolean shareAddress = false;
	public boolean readOneHmmOneTime = false;
	// This is for generating IRT (weka) input per HMM.
	public boolean generateLRInputs = false;
	public boolean swapData = false;
	// if inputProvideFeatureColumns=true, then train and test files should have
	// same # and order of features
	public boolean inputProvideFeatureColumns = true;
	public HashSet<String> newStudents = new HashSet<String>();
	public HashSet<String> newItems = new HashSet<String>();
	public HashSet<String> upTillNowNewStudents = new HashSet<String>();
	public HashSet<String> upTillNowNewItems = new HashSet<String>();

	public boolean writeTrainPredFile = false;
	// TODO: by default, it is adding in a shared (by both hidden states) way, so
	// opts.modelName should contain("dupbias"); haven't tested completely
	public boolean addSharedStuDummyFeatures = false;
	public boolean addSharedItemDummyFeatures = false;
	public boolean inputHasStepColumn = true;
	public boolean hasStudentDummy = false;

	// "These are all for storing information dynamically, no need to configure.")
	// [nowInTrain] is for dynamcially recording whether it is in train or test.")
	public int wholeProcessRunId = 0;
	public int randomRestartId = 0;
	public int currentKCIndex = -1;
	public String currentKc = "";
	public boolean nowInTrain = true;
	public double bestAucOnDevAllKcsSum = 0.0;
	public double aucOnTestAllKcsSum = 0.0;
	public boolean writeForTheBestAucOnDev = false;
	public boolean preDpCurDpFromDifferentSet = false;
	public int nbDataPointsInTrainPerHmm = 0;
	public int currentBaumWelchIteration = 0;
	public double nbLlError = 0;
	public int maxNbInsWeightRoundTo0PerIterPerHmm = 0;
	public int maxNbInsWeightRoundTo0PerIter = 0;
	public double maxLLDecreaseValuePerIterPerHmm = 0;
	public double maxLLDecreaseValuePerIter = 0;
	public double maxLLDecreaseRatioValuePerIterPerHmm = 0;// valueChange/valueAvg
	public double maxLLDecreaseRatioValuePerIter = 0;// valueChange/valueAvg
	public boolean cgReachesTrustRegionBoundary = false;
	public double[][] currentIterHiddenStatesSmallLLs;
	public double[] currentIterHiddenStatesSmallLLs_;
	public double[][] currentIterHiddenStatesExpectedCount;
	public double[] currentIterHiddenStatesExpectedCount_;
	public String[] currentIterfeatureWeights;
	public String currentIterfeatureWeights_;
	public HashSet<String> hmmsForcedToNonParmTrainDueToLBFGSException = new HashSet<String>();

	public String mainLogFile = outDir + "main.log";
	public String llLogFile = outDir + "ll.log";
	public String expectedCountLogFile = outDir + "expectedCount.log";
	public String featureWeightsLogFile = outDir + "featureWeights.log";
	public String finalFeatureWeightsFile = outDir + "finalFeatureWeights.log";
	public String datapointsLogFile = outDir + "dataPoints.log";
	public String datapointsLogFile2 = outDir + "dataPoints2.txt";
	public String learningCurveFile = outDir + "learningCurve.txt";
	public String initLearnForgetProbLogFile = outDir + "initLearnForgetProb.log";
	public String guessSlipProbLogFile = outDir + "guessSlipProb.log";
	public String guessSlipProbLogFile2 = outDir + "guessSlipProb2.log";
	public String gammaLogFile = outDir + "gamma.log";
	public String testLRInstanceWeightsFile = outDir + "instance-weight.txt";
	public String testLRLabelFile = outDir + "label.txt";
	public String testLRFeaturesFile = outDir + "features.txt";
	public String liblinearInputDataFile = outDir + "liblinear-data.txt";
	public String testByWekaInputDataFile = outDir + "weka-data.txt";
	public String deltaPCorrectFile = outDir + "deltaPCorrect.txt";
	public String bestRegWeightFile = outDir + "bestRegWeight.txt";
	public String deltaGammaFile = outDir + "deltaGamma.txt";
	public String perKcAucVsAvgDeltaGammaFile = outDir
			+ "perKcAucVsAvgDeltaGammaFile.txt";
	public String eachDeltaGammaFile = outDir + "eachDeltaGamma.txt";
	// + "perKcAucVsDeltaGamma2.txt";
	public String finalHmmParametersFile = outDir + "finalHmmParameters.txt";

	public BufferedWriter testLRInstanceWeightsWriter = null;
	public BufferedWriter testLRLabelWriter = null;
	public BufferedWriter testLRFeatureWriter = null;
	public BufferedWriter liblinearInputDataWriter = null;
	public BufferedWriter testByWekaInputDataWriter = null;
	// writes only specified/problemastic iteration
	public BufferedWriter mainLogWriter = null;
	public BufferedWriter expectedCountLogWriter = null;
	// writes every iteration
	public BufferedWriter featureWeightsLogWriter = null;
	public BufferedWriter llLogWriter = null; // LL, smallll
	public BufferedWriter datapointsLogWriter = null;
	public BufferedWriter datapointsLogWriter2 = null;
	public BufferedWriter learningCurveWriter = null;
	public BufferedWriter initLearnForgetProbLogWriter = null;
	public BufferedWriter guessSlipProbLogWriter = null;
	public BufferedWriter guessSlipProbLogWriter2 = null;
	public BufferedWriter gammaWriter = null;
	public BufferedWriter finalFeatureWeightsWriter = null;
	public BufferedWriter deltaPCorrectWriter = null;
	public BufferedWriter deltaGammaWriter = null;
	public BufferedWriter bestRegWeightWriter = null;
	public BufferedWriter predWriter = null;
	public BufferedWriter perKcAucVsAvgDeltaGammaWriter = null;
	public BufferedWriter eachDeltaGammaWriter = null;// perKcAucVsDeltaGammaWriter2
																										// = null;
	// writes final iteration
	public BufferedWriter finalHmmParametersWriter = null;

	public HashSet<String> skillsToCheck = new HashSet<String>();
	public TreeMap<String, Double> kcAvgDeltaGammaMap = new TreeMap<String, Double>();
	public TreeMap<String, Double> kcTestAucMap = new TreeMap<String, Double>();

	public DecimalFormat formatter;
	{
		formatter = (DecimalFormat) DecimalFormat.getInstance(Locale.US);
		formatter.applyPattern("#.######");
	}

	public DecimalFormat formatter2;
	{
		formatter2 = (DecimalFormat) DecimalFormat.getInstance(Locale.US);
		formatter2.applyPattern("#.####");
	}

	public Random wholeProcessBaseRand;
	public Random[] perHmmBaseRand;
	public Random[][] featureWeightsRands;
	public Random[][] nonFeatureParasRands;

	public void configure() {

		modelName = basicModelName + (variant1ModelName.equals("") ? "" : ("-" + variant1ModelName + (variant2ModelName.equals("") ? "": ("-" + variant2ModelName))));//
		checkConfig();

		allModelComparisonOutDir = outDir;// basicDir;
		// inDir = basicDir;
		// + basicModelName
		// + "/"
		// + (variant1ModelName.equals("") ? (datasplit + "/")
		// : (variant1ModelName + "/" + datasplit + "/"));
		// outDir = inDir + modelName + "/";
		predPrefix = modelName + "_test";
		predictionFile = outDir + predPrefix + (testSingleFile ? "0" : "") + predSuffix;
		curFoldRunTrainInFilePrefix = trainInFilePrefix
				+ (testSingleFile ? "0" : "");
		trainFile = inDir + curFoldRunTrainInFilePrefix + inFileSuffix;
		curFoldRunTestInFilePrefix = testInFilePrefix + (testSingleFile ? "0" : "");
		testFile = inDir + curFoldRunTestInFilePrefix + inFileSuffix;
		curFoldRunDevInFilePrefix = devInFilePrefix + (testSingleFile ? "0" : "");
		devFile = inDir + curFoldRunDevInFilePrefix + inFileSuffix;

		skillsToCheck.add("Variables");
	}

	// This is called for all kinds of argumentation input for main().
	public void reconfigure() {
		predPrefix = modelName + "_test";
		predictionFile = outDir + predPrefix + (testSingleFile ? "0" : "")
				+ predSuffix;
		mainLogFile = outDir + "main.log";
		llLogFile = outDir + "ll.log";
		expectedCountLogFile = outDir + "expectedCount.log";
		featureWeightsLogFile = outDir + "featureWeights.log";
		finalFeatureWeightsFile = outDir + "finalFeatureWeights.txt";
		datapointsLogFile = outDir + "dataPoints.log";
		datapointsLogFile2 = outDir + "dataPoints2.txt";
		learningCurveFile = outDir + "learningCurve.txt";
		initLearnForgetProbLogFile = outDir + "initLearnForgetProb.log";
		guessSlipProbLogFile = outDir + "guessSlipProb.log";
		guessSlipProbLogFile2 = outDir + "guessSlipProb2.log";
		gammaLogFile = outDir + "gamma.log";
		testLRInstanceWeightsFile = outDir + "instance-weight.txt";
		testLRLabelFile = outDir + "label.txt";
		testLRFeaturesFile = outDir + "features.txt";
		liblinearInputDataFile = outDir + "liblinear-data.txt";
		testByWekaInputDataFile = outDir + "weka-data.txt";
		deltaPCorrectFile = outDir + "deltaPCorrect.txt";
		bestRegWeightFile = outDir + "bestRegWeight.txt";
		deltaGammaFile = outDir + "deltaGamma.txt";
		perKcAucVsAvgDeltaGammaFile = outDir + "perKcAucVsAvgDeltaGammaFile.txt";
		eachDeltaGammaFile = outDir + "eachDeltaGamma.txt";// "perKcAucVsDeltaGammaFile2.txt";
		finalHmmParametersFile = outDir + "finalHmmParameters.txt";

		checkConfig();

		try {
			if (writeFinalHmmParameters) {
				finalHmmParametersWriter = new BufferedWriter(new FileWriter(
						finalHmmParametersFile));
				finalHmmParametersWriter
						.write("hmmId\tpi0\ta00\ta01\tb00\tb01\tpi1\ta10\ta11\tb10\tb11\n");
			}
			if (writeMainLog)
				mainLogWriter = new BufferedWriter(new FileWriter(mainLogFile, true));
			if (writeLlLog)
				llLogWriter = new BufferedWriter(new FileWriter(llLogFile, true));
			if (writeExpectedCountLog)
				expectedCountLogWriter = new BufferedWriter(new FileWriter(
						expectedCountLogFile, true));
			if (writeFeatureWeightsLog)
				featureWeightsLogWriter = new BufferedWriter(new FileWriter(
						featureWeightsLogFile, true));
			if (writeFinalFeatureWeights)
				finalFeatureWeightsWriter = new BufferedWriter(new FileWriter(
						finalFeatureWeightsFile));
			if (writeDatapointsLog)
				datapointsLogWriter = new BufferedWriter(new FileWriter(
						datapointsLogFile, true));
			if (writeDatapointsLog2)
				datapointsLogWriter2 = new BufferedWriter(new FileWriter(
						datapointsLogFile2));
			if (writeInitLearnForgetProbLog)
				initLearnForgetProbLogWriter = new BufferedWriter(new FileWriter(
						initLearnForgetProbLogFile, true));
			if (writeGuessSlipProbLog)
				guessSlipProbLogWriter = new BufferedWriter(new FileWriter(
						guessSlipProbLogFile, true));
			if (writeGuessSlipProbLog2)
				guessSlipProbLogWriter2 = new BufferedWriter(new FileWriter(
						guessSlipProbLogFile2, true));
			if (writeGammaLog)
				gammaWriter = new BufferedWriter(new FileWriter(gammaLogFile, true));
			if (testLiblinear) {
				testLRInstanceWeightsWriter = new BufferedWriter(new FileWriter(
						testLRInstanceWeightsFile));
				liblinearInputDataWriter = new BufferedWriter(new FileWriter(
						liblinearInputDataFile));
			}
			if (testLogsticRegression) {
				testLRInstanceWeightsWriter = new BufferedWriter(new FileWriter(
						testLRInstanceWeightsFile));
				testLRLabelWriter = new BufferedWriter(new FileWriter(testLRLabelFile));
				testLRFeatureWriter = new BufferedWriter(new FileWriter(
						testLRFeaturesFile));
			}
			if (writeDeltaPCorrectOnTrain)
				deltaPCorrectWriter = new BufferedWriter(new FileWriter(
						deltaPCorrectFile, true));
			if (writeDeltaGamma && !writeEachDeltaGamma)
				deltaGammaWriter = new BufferedWriter(new FileWriter(deltaGammaFile,
						true));
			if (writeEachDeltaGamma)
				eachDeltaGammaWriter = new BufferedWriter(new FileWriter(
						eachDeltaGammaFile));
			if (writeBestRegWeightLog)
				bestRegWeightWriter = new BufferedWriter(new FileWriter(
						bestRegWeightFile, true));
			if (writePerKcAucVsAvgDeltaGamma)
				perKcAucVsAvgDeltaGammaWriter = new BufferedWriter(new FileWriter(
						perKcAucVsAvgDeltaGammaFile, true));

			if (writeForLearningCurve)
				learningCurveWriter = new BufferedWriter(new FileWriter(
						learningCurveFile, true));
		}
		catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		// ArrayList<Double[]> hiddenStateIsmallLL = new ArrayList<Double[]>();
		currentIterHiddenStatesExpectedCount = new double[nbHiddenStates][];
		currentIterfeatureWeights = new String[nbHiddenStates];
		// before, after
		currentIterHiddenStatesSmallLLs = new double[nbHiddenStates][2];
		currentIterHiddenStatesSmallLLs_ = new double[2];
	}

	public void checkConfig() {
		File curActionD = new File(outDir);
		if (!curActionD.exists()) {
			System.out.println("\noutDir doesn't exist, creating this directory: "
					+ outDir);
			boolean result = curActionD.mkdir();
			if (!result) {
				System.out.println("Error: directory creation failed: " + outDir);
				System.exit(1);
			}
		}

		curActionD = new File(inDir);
		if (!curActionD.exists()) {
			System.out.println("\nError: inDir doesn't exist: " + inDir);
			System.exit(1);
		}

		if (modelName.contains("KT")) {
			parameterizedEmit = false;
			oneBiasFeature = false;
			duplicatedBias = false;
			bias = -1.0;
			oneLogisticRegression = false;
			writeExpectedCountLog = false;
			writeLlLog = false;
			writeFeatureWeightsLog = false;
			writeFinalFeatureWeights = false;
		}
		else if (modelName.contains("FAST") && oneBiasFeature) {
			parameterizedEmit = true;
			bias = 1.0;
			oneLogisticRegression = false;
		}
		else {
			parameterizedEmit = true;
			oneBiasFeature = false;
		}

		// This is for generating IRT (weka) input per HMM.
		if (generateLRInputs) {
			oneLogisticRegression = false;
			EM_MAX_ITERS = 1;
			LBFGS_MAX_ITERS = 0;
			getAucOnDevPerKc = false;
			randomRestartPerHmmTimes = 1;
			swapData = false;
			shareAddress = true;
			writeFinalHmmParameters = false;
			writeDeltaGamma = false;
			writeFinalFeatureWeights = false;
			writeDeltaPCorrectOnTrain = false;
			writeMainLog = false;
			writeLlLog = false;
			writeExpectedCountLog = false;
			writeFeatureWeightsLog = false;
			writeGammaLog = false;
			writeInitLearnForgetProbLog = false;
			writeGuessSlipProbLog = false;
			writeGuessSlipProbLog2 = false;
			writeDatapointsLog = false;
			writeDatapointsLog2 = false;
			writePerKcAucVsAvgDeltaGamma = false;
			writeEachDeltaGamma = false;
			writeForLearningCurve = false;
		}
		if (tuneL2)
			writeBestRegWeightLog = true;

		if (testSingleFile && (numFolds > 1 || numRuns > 1)) {
			System.out
					.println("WARNING: testSingleFile=true && (numFolds > 1 || numRuns > 1)! Reset testSingleFile=False!");
			testSingleFile = false;
		}

		if ((parameterizedEmit || oneBiasFeature) && modelName.contains("KT")) {
			System.out
					.println("ERROR: (parameterizedEmit||oneBiasFeature) && modelName.contains(\"KT\")!");
			System.exit(1);
		}

		if (bias < 0 && oneBiasFeature) {
			System.out.println("ERROR: bias < 0 && oneBiasFeature!");
			System.exit(1);
		}

		// if (!(bias > 0) && modelName.contains("bias")) {
		// System.out
		// .println("ERROR: !(bias > 0 || oneBiasFeature) && modelName.contains(\"bias\")!");
		// System.exit(1);
		// }

		if (!oneLogisticRegression && parameterizedEmit) {
			System.out
					.println("WARNING: currently not supporting: !oneLogisticRegression &&  parameterizedEmit!");
			// System.exit(1);
		}

		if (useClassWeightToTrainParamerizedEmit
				&& useInstanceWeightToTrainParamterezdEmit) {
			System.out
					.println("ERROR: opts.useClassWeightToTrainParamerizedEmit && opts.useInstanceWeightToTrainParamterezdEmit");
			System.exit(1);
		}

		if (coefficientWeightedByGamma && !hasStudentDummy) {
			System.out
					.println("WARNING: I am not going to weight the coefficient by gamma unless are using duplicated studummies");
			coefficientWeightedByGamma = false;
		}

		// TODO:for those bias=1.0 and dupbias=False, the printed out
		// finalFeatureWeights have problems
		// if (writeFinalFeatureWeights) {
		// System.out
		// .println("Warning: writeFinalFeatureWeights configuration not ready yet!");
		// }
	}

	public void resetRandom(int wholeProcessRunId) {
		wholeProcessBaseRand = new Random();// (43569);
		perHmmBaseRand = new Random[randomRestartPerHmmTimes];
		featureWeightsRands = new Random[nbHmms][randomRestartPerHmmTimes];
		nonFeatureParasRands = new Random[nbHmms][randomRestartPerHmmTimes];
		wholeProcessBaseRand = new Random(wholeProcessRunId);
		for (int hmmIndex = 0; hmmIndex < nbHmms; ++hmmIndex) {
			for (int randomRestartIndex = 0; randomRestartIndex < randomRestartPerHmmTimes; randomRestartIndex++) {
				perHmmBaseRand[randomRestartIndex] = new Random(
						wholeProcessBaseRand.nextInt());
			}
			featureWeightsRands[hmmIndex] = perHmmBaseRand;
			for (int randomRestartIndex = 0; randomRestartIndex < randomRestartPerHmmTimes; randomRestartIndex++) {
				perHmmBaseRand[randomRestartIndex] = new Random(
						wholeProcessBaseRand.nextInt());
			}
			// one nonFeatureParasRands[i] is for generating all randomRestart seed
			nonFeatureParasRands[hmmIndex] = perHmmBaseRand;
		}
	}

	public void closeAndClear() {
		try {
			if (writeFinalHmmParameters)
				finalHmmParametersWriter.close();
			if (writeMainLog)
				mainLogWriter.close();
			if (writeLlLog)
				llLogWriter.close();
			if (writeExpectedCountLog)
				expectedCountLogWriter.close();
			if (writeFeatureWeightsLog)
				featureWeightsLogWriter.close();
			if (writeFinalFeatureWeights)
				finalFeatureWeightsWriter.close();
			if (writeDatapointsLog)
				datapointsLogWriter.close();
			if (writeDatapointsLog2)
				datapointsLogWriter2.close();
			if (writeInitLearnForgetProbLog)
				initLearnForgetProbLogWriter.close();
			if (writeGuessSlipProbLog)
				guessSlipProbLogWriter.close();
			if (writeGuessSlipProbLog2)
				guessSlipProbLogWriter2.close();
			if (writeGammaLog)
				gammaWriter.close();
			if (testLiblinear) {
				liblinearInputDataWriter.close();
				testLRInstanceWeightsWriter.close();
			}
			if (testLogsticRegression) {
				testLRInstanceWeightsWriter.close();
				testLRLabelWriter.close();
				testLRFeatureWriter.close();
			}
			if (writeDeltaPCorrectOnTrain)
				deltaPCorrectWriter.close();
			if (writeDeltaGamma && !writeEachDeltaGamma)
				deltaGammaWriter.close();
			if (writeEachDeltaGamma)
				eachDeltaGammaWriter.close();
			if (writeBestRegWeightLog)
				bestRegWeightWriter.close();
			if (writePerKcAucVsAvgDeltaGamma)
				perKcAucVsAvgDeltaGammaWriter.close();
			if (writeForLearningCurve)
				learningCurveWriter.close();

		}
		catch (IOException e) {
			e.printStackTrace();
		}
		skillsToCheck.clear();

	}

	public void writeLogFiles(String str) throws IOException {
		if (writeMainLog)
			mainLogWriter.write(str);
		if (writeLlLog)
			llLogWriter.write(str);
		if (writeExpectedCountLog)
			expectedCountLogWriter.write(str);
		if (writeFeatureWeightsLog)
			featureWeightsLogWriter.write(str);
		// if (writeFinalFeatureWeights)
		// finalFeatureWeightsWriter.write(str);
		if (writeDatapointsLog)
			datapointsLogWriter.write(str);
		if (writeInitLearnForgetProbLog)
			initLearnForgetProbLogWriter
					.write(str
							+ "KC\titer\t~~~~~~\t~~~~~~\t~~~~~~\tunknown\tA0\tA00\tA01(learn)\tknown\tA1(init)\tA10(forget)\tA11\n");
		if (writeGuessSlipProbLog)
			guessSlipProbLogWriter.write(str);
		if (writeGuessSlipProbLog2)
			guessSlipProbLogWriter2.write(str);
		if (writeGammaLog)
			gammaWriter.write(str);
		if (writeDeltaGamma && !writeEachDeltaGamma)
			deltaGammaWriter.write(str);
		if (writeDeltaPCorrectOnTrain)
			deltaPCorrectWriter.write(str);
		if (writeBestRegWeightLog)
			bestRegWeightWriter.write(str);
		if (writeForLearningCurve)
			learningCurveWriter.write(str);
	}
}
