/**
 * FAST v1.0       08/12/2014
 * 
 * This code is originally developed for research purpose and is still under improvement. 
 * Please email to us if you want to keep in touch with the latest release.
	 We sincerely welcome you to contact Yun Huang (huangyun.ai@gmail.com), or Jose P.Gonzalez-Brenes (josepablog@gmail.com) for problems in the code or cooperation.
 * We thank Taylor Berg-Kirkpatrick (tberg@cs.berkeley.edu) and Jean-Marc Francois (jahmm) for part of their codes that FAST is developed based on.
 *
 */

package fast.hmmfeatures;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Locale;
import java.util.Random;
import java.util.TreeMap;
import fig.basic.Option;

public class Opts {

	@Option(gloss = "Currently, basicModelName SHOULD be either \"FAST\" or \"KT\". "
			+ "basicModelName, variant1ModelName and variant2ModelName are used to configure modelName and in/out directories. However, you can specify modelName and inDir/outDir directly.")
	public static String basicModelName = "FAST";
	@Option(gloss = "Currently, variant1ModelName could be empty or contain any strings."
			+ "basicModelName, variant1ModelName and variant2ModelName are used to configure modelName and in/out directories. However, you can specify modelName and inDir/outDir directly.")
	public static String variant1ModelName = "item";
	@Option(gloss = "Currently, variant2ModelName could be empty or contain any strings. basicModelName, variant1ModelName and variant2ModelName are used to configure modelName and in/out directories. However, you can specify modelName and inDir/outDir directly.")
	public static String variant2ModelName = "";
	public static String basicDir = "../data/input/";
	@Option(gloss = "modelName is used help doing many other configurations. However, you can also specify other configurations by yourself.")
	public static String modelName = basicModelName
			+ (variant1ModelName.equals("") ? ""
					: ("-" + variant1ModelName + (variant2ModelName.equals("") ? ""
							: ("-" + variant2ModelName))));//
	@Option(gloss = "testSingleFile is used to decide just run one train test pair or multiple train, test pairs.")
	public static boolean testSingleFile = true;
	@Option(gloss = "numFolds and numRuns are used to decide how many times FAST runs. For testSingleFile=true, numFolds and numRuns should be set to 1. The code uses foldID to change the train, test file name (e.g. train0.txt~train9.txt are for two runs of 5 fold CV or one run of 10 fold CV) foldId = runID * opts.numFolds + foldID")
	public static int numFolds = 1;
	@Option(gloss = "numFolds and numRuns are used to decide how many times FAST runs. For testSingleFile=true, numFolds and numRuns should be set to 1. The code uses foldID to change the train, test file name (e.g. train0.txt~train9.txt are for two runs of 5 fold CV or one run of 10 fold CV) foldId = runID * opts.numFolds + foldID")
	public static int numRuns = 1;
	@Option(gloss = "datasplit is used to help configure outDir.")
	public static String datasplit = testSingleFile ? "datasets" : "CVdatasets";
	@Option(gloss = "inDir is where FAST get input files(train and test).")
	public static String inDir = basicDir
			+ basicModelName
			+ "/"
			+ (variant1ModelName.equals("") ? (datasplit + "/") : (variant1ModelName
					+ "/" + datasplit + "/"));
	@Option(gloss = "outDir is where FAST output prediction files and log files.")
	public static String outDir = inDir + modelName + "/";
	@Option(gloss = "allModelComparisonOutDir is where all different models are compared (by the average evaluation metric).")
	public static String allModelComparisonOutDir = basicDir;
	@Option(gloss = "If just one train test pair, then train(test) file will be \"train(test)InFilePreifix0.txt\". If there are multiple train, test pairs, then based on the configuration of numFolds and numRuns, train(test) file will be \"train(test)InFilePreifixFoldId.txt\", e.g. \"train1.txt\" or \"test1.txt\" for the second fold first run.")
	public static String trainInFilePrefix = "train";
	@Option(gloss = "If just one train test pair, then train(test) file will be \"train(test)InFilePreifix0.txt\". If there are multiple train, test pairs, then based on the configuration of numFolds and numRuns, train(test) file will be \"train(test)InFilePreifixFoldId.txt\", e.g. \"train1.txt\" or \"test1.txt\" for the second fold first run.")
	public static String testInFilePrefix = "test";
	public static String devInFilePrefix = "dev";
	@Option(gloss = "The surfix for train and test files.")
	public static String inFileSurfix = ".txt";
	// @Option(gloss =
	// "ratio is used to configure the name of input train/test files when FAST is training on the \"ratio\" of a sequence and testing on remaining of the sequence. Yet you can set it as 0 and gives the standard names (train.txt, test.txt)")
	public static String ratio = "";// "0.5";
	@Option(gloss = "If there are more than one runs, these are configured dynamically in each run with changing foldID, e.g. train(test) file will be \"train(test)InFilePreifixFoldId.txt\", e.g. \"train0.txt\" or \"test0.txt\" for the first fold.")
	public static String trainFile = inDir + trainInFilePrefix + "0"
			+ inFileSurfix;
	@Option(gloss = "If there are more than one runs, these are configured dynamically in each run with changing foldID, e.g. train(test) file will be \"train(test)InFilePreifixFoldId.txt\", e.g. \"train0.txt\" or \"test0.txt\" for the first fold.")
	public static String testFile = inDir + testInFilePrefix + "0" + inFileSurfix;
	public static String devFile = inDir + devInFilePrefix + "0" + inFileSurfix;
	public static String curFoldRunTrainInFilePrefix = "";
	public static String curFoldRunTestInFilePrefix = "";
	public static String curFoldRunDevInFilePrefix = "";
	@Option(gloss = "predSurfix=.pred is neccessary if we are to use the evaluation code inside this code.")
	public static String predSurfix = ".pred";
	// predictionFile should contain modelName, because modelName and predSurfix
	// are used together to identify prediction file automatically by Evaluation
	// engine.
	public static String predPrefix = modelName + "_test";
	public static String predictionFile = outDir + predPrefix + "0" + predSurfix;

	// This part is for configuring regularization.
	public static boolean useReg = true;
	// tuneL2 is for tuning the weights of L2 regularization.
	public static boolean tuneL2 = false;
	public static boolean useDev = false;
	public static boolean combineTrainAndDev = false;
	public static boolean tuneByTestset = false;

	@Option(gloss = "By default, configure baumWelchScaledLearner=true meaning that we use baumWelchScaledLearner.")
	public static boolean baumWelchScaledLearner = true;
	@Option(gloss = "TOLERANCE is used to decide the convergence of outer EM. Setting bigger value could make training stop earlier.")
	public static double EM_TOLERANCE = 1.0E-6;// 1.0E-6(EDM paper);1.0E-4(quick
																							// experiments)
	@Option(gloss = "EM_MAX_ITERS is used for maximum iteration of outer EM. Setting smaller value could make training stop earlier.")
	public static int EM_MAX_ITERS = 400;// usual:400(EDM paper)

	public static boolean removeSeqLength1InTrain = false;
	public static boolean removeSeqLength1InTest = false;

	@Option(gloss = "parameterizedEmit can be configured as true by the code itself or by outside specification.")
	public static boolean parameterizedEmit = true;
	// @Option(gloss =
	// "bias<0 means no bias, >0 means adding a bias to the featue space. This configuration is only activated when running FAST (not KT).")
	public static double bias = 1.0;// -1.0
	// TODO: check @Option(gloss =
	// "oneBiasFeatue=true means just using one  feature. These two can also configures by putting \"bias\" and \"1bias\" in the variant2ModelName")
	public static boolean oneBiasFeature = false;
	// @Option(gloss =
	// "duplicatedBias=true means using one bias feature for \"known\" state and another bias feature for the \"unknown\" state. To set it true is necessary when other features are all shared by two states.")
	public static boolean duplicatedBias = true;

	// @Option(gloss =
	// "These are for both LIBLINEAR and LBFGS. By default use gamma as instance weight and use it(instead of class weight) to train parameterized emission.")
	public static boolean useGammaAsInstanceWeight = true;
	// @Option(gloss =
	// "These are for both LIBLINEAR and LBFGS. By default use gamma as instance weight and use it(instead of class weight) to train parameterized emission.")
	public static boolean useInstanceWeightToTrainParamterezdEmit = true;
	// @Option(gloss =
	// "These are for both LIBLINEAR and LBFGS. By default use gamma as instance weight and use it(instead of class weight) to train parameterized emission.")
	public static boolean useClassWeightToTrainParamerizedEmit = false;
	// Option(gloss = "By default, just use one LR for emission probabilities.")
	public static boolean oneLogisticRegression = true;

	@Option(gloss = "LBFGS=true means use LBFGS to optimize the logistic regression part; (deprecated: otherwise use LIBLINEAR.)")
	public static boolean LBFGS = true;
	@Option(gloss = "LBFGS_TOLERANCE is used to decide the convergence of inner LBFGS. Setting bigger value could make training stop earlier.")
	public static double LBFGS_TOLERANCE = 1.0E-6;// usual:1.0E-6(EDM paper);
	@Option(gloss = "LBFGS_MAX_ITERS is used for maximum iteration of inner LBFGS. Setting smaller value could make training stop earlier.")
	public static int LBFGS_MAX_ITERS = 500;// usual:500(EDM paper)
	@Option(gloss = "For LBFGS, regulariztion term is sum_i[ c*(w_i - b)^2 ] where c is regularization weight and b is regularization bias.")
	public static double LBFGS_REGULARIZATION_WEIGHT = useReg ? 1.0 : 0.0;
	public static double[] LBFGS_REGULARIZATION_WEIGHT_RANGE = { 0.01, 0.1, 1,
			10, 100 };
	// { 1.0E-5, 1.0E-4,1.0E-3, 0.01, 0.1, 1, 5, 10, 30, 50, 100 };//{ 1.0 };
	public static double[] regularizationWeightsForLBFGS;
	@Option(gloss = "For LBFGS, regulariztion term is sum_i[ c*(w_i - b)^2 ] where c is regularization weight and b is regularization bias.")
	public static double LBFGS_REGULARIZATION_BIAS = 0.0;
	public static double[] regularizationBiasesForLBFGS;
	@Option(gloss = "ensureStopForLBFGS stops LBFGS by force when LL changes smaller than LBFGS tolerance.")
	public static boolean ensureStopForLBFGS = true;
	@Option(gloss = "forceSetInstanceWeightForLBFGS>0 will set the instance weight by the value by force only for LBFGS.")
	public static double forceSetInstanceWeightForLBFGS = -1.0;

	// Option(gloss =
	// "LIBLINEAR_C_PENALTY is (for liblinear option) the penalty weight for adjusting the regularization terms importance (yet it is not the direct coefficient in front of regurlarization term.")
	// [LR_C_PENALTY ] LR_C_PENALTY=1,10, fold0 maxLLD=0.25;
	// LR_C_PENALTY=0.5,fold0 maxLLD>0.214; can configure it to be
	// LR_C_PENALTY=2^-3, 2^-2, 2^-1,and 2^0.
	public static double LIBLINEAR_C_PENALTY = 1;

	// @Option(gloss =
	// "INSTANCE_WEIGHT_ROUNDING_THRESHOLD is used to round the instance weight. By default, no need to use when using LBFGS, but use when use liblinear.")
	// [INSTANCE_WEIGHT_ROUNDING_THRESHOLD] instance weight <= threshold ->0;
	// >=(1-threshold) -> 1, if threshold<0 then we don't round at all;
	// In liblinear, i already added a removeNonpositiveWeights() function to help
	// remove the instance with weights =0;
	// Setting threshold smaller is better, and it seems set a threshold is better
	// than none (when setting 0.1, the LL decrease a lot)
	public static double INSTANCE_WEIGHT_ROUNDING_THRESHOLD = LBFGS ? -1.0
			: 1.0E-4;// -1.0;//
	// @Option(gloss =
	// "INSTANCE_WEIGHT_MULTIPLIER will multiply the instance weight by specified value (for both liblinear and LBFGS).")
	public static double INSTANCE_WEIGHT_MULTIPLIER = 1.0;

	// @Option(gloss =
	// "used to decide whether use same intialization for featureHMM and non-featureHMM. Only useful when use 1 bias feature version of featureHMM")
	// public static boolean sameInitForFeatureAndNonfeature = false;
	public static int randomRestartPerHmmTimes = 1;
	public static int nbHmms = 500;
	public static int randomRestartWholeProcessTimes = 1;
	public static boolean differentInitializationPerSkill = true;
	@Option(gloss = "for initializing both paramerized and non-paramtereized paramters' values (usually set 1).")
	public static double initDirichlet = 1;// take care to change
	@Option(gloss = "for deciding lower and upper bound for each emission probability.")
	public static double[][] initialWeightsBounds = { { -0.1, 0.1 },
			{ -0.1, 0.1 } };
	// existed papers: maximum probabilities of initial knowledge, guess, slip and
	// learning are 0.85, 0.3, 0.1 and 0.3, respectively.
	// public static double guessBound = 0.5;
	// public static double slipBound = 0.5;
	@Option(gloss = "Now by default, allows forget.")
	public static boolean allowForget = true;
	@Option(gloss = "To decide printing out verbose information or not.")
	public static boolean verbose = false;
	@Option(gloss = "To decide printing out verbose information or not.")
	public static boolean LBFGS_PRINT_MINIMIZER = false;
	@Option(gloss = "EPS is for avoiding divided by 0.")
	public static double EPS = 1e-10;
	@Option(gloss = "EXPECTED_COUNT_SMALL_VALUE is used to decide whether expected count is too small and if so print out warning in log files.")
	public static double EXPECTED_COUNT_SMALL_VALUE = 1.0E-4;// 1.0E-6;
	@Option(gloss = "ACCETABLE_LL_DECREASE is used to decide whether LL decreas is too small and if so print out warning in log files.")
	public static double ACCETABLE_LL_DECREASE = 1.0E-3;
	@Option(gloss = "INIT_LL is used to determine initial LL to compute the first LL change.")
	public static double INIT_LL = -1.0E10;
	// @Option(gloss =
	// "To decide print out intermediate files for testing liblinear or LBFGS outside.")
	public static boolean testLiblinear = false;
	public static boolean testLogsticRegression = false;
	public static String skillToTest = "ArrayList";

	public static boolean useEmissionToJudgeHiddenStates = false;
	public static boolean useTransitionToJudgeHiddenStates = useEmissionToJudgeHiddenStates ? false
			: true;
	// @Option(gloss =
	// "These are decided by default to give convinience to do inference")
	// [hiddenState1] in prediction, will decide again by seeing
	// P(correct|hiddenState); in other places are used to initialize forget (if
	// don't allow forget)
	// public static int realHiddenState1 = 1;// known
	// public static int realHiddenState0 = 1 - realHiddenState1;// unknown
	public static int hiddenState1 = 1;// known
	public static int hiddenState0 = 1 - hiddenState1;// unknown
	// [obsClass1] correspond to "correct", decides: (1)reading input, put
	// "incorrect" first
	// (2)LR always output p(Correct). After these, obsClass1 is always "correct"
	@Option(gloss = "These are decided by default to give convinience to do inference and decide LR's probabilities")
	public static int obsClass1 = 1;
	@Option(gloss = "These are decided by default to give convinience to do inference and decide LR's probabilities")
	public static String obsClass1Name = "correct";
	@Option(gloss = "These are decided by default to give convinience to do inference and decide LR's probabilities")
	public static int nbHiddenStates = 2;
	@Option(gloss = "These are decided by default to give convinience to do inference and decide LR's probabilities")
	public static int nbObsStates = 2;

	@Option(gloss = "for decising what information to log.")
	public static boolean writeFinalHmmParameters = false;
	@Option(gloss = "for decising what information to log.")
	public static boolean writeMainLog = true;
	// @Option(gloss = "for decising what information to log.")
	public static boolean writeLlLog = false;
	// @Option(gloss = "for decising what information to log.")
	public static boolean writeExpectedCountLog = false;
	// if write final feature weights, plz see above config.
	// @Option(gloss = "for decising what information to log.")
	public static boolean writeFeatureWeightsLog = false;
	// @Option(gloss = "for decising what information to log.")
	public static boolean writeGammaLog = false;
	// @Option(gloss = "for decising what information to log.")
	public static boolean writeInitLearnForgetProbLog = false;
	// @Option(gloss = "for decising what information to log.")
	public static boolean writeGuessSlipProbLog = false;
	// @Option(gloss = "for decising what information to log.")
	public static boolean writeGuessSlipProbLog2 = false;
	// @Option(gloss = "for decising what information to log.")
	public static boolean writeDatapointsLog = false;
	// 3 columns: item, practice opp, skillname
	public static boolean writeDatapointsLog2 = false;
	public static boolean writeForLearningCurve = false;
	// public static boolean writeForItemVsPractice = true;
	public static boolean writeBestRegWeightLog = false;

	// These configuration determine later how to write
	public static boolean writeDeltaGamma = false;
	public static boolean writeOnlyDeltaGamma = false;
	public static boolean writePerKcAucVsAvgDeltaGamma = false;
	// one delta is for one student on one skill, they are arranged in one column
	public static boolean writeEachDeltaGamma = false;
	// one delta is for one student on one skill, they are arranged in one column
	// public static boolean writePerKcAucVsDeltaGamma3 = false;
	public static boolean writePerKcTestSetAUC = false;
	// TODO: later use it: public static boolean perkcaucontest = true;// true
	public static boolean getAucOnDevPerKc = false;

	// determines later how to write
	// TODO: formatting the output:
	@Option(gloss = "for decising what information to log.")
	public static boolean writeFinalFeatureWeights = false;
	// TODO: formatting the output: Option(gloss =
	// "writeOnlyOriginalFinalFeatureWeights=False but writeFinalFeatureWeights=True, then will write also the difference of feature weights between two states.")
	public static boolean writeOnlyOriginalFinalFeatureWeights = true;
	public static boolean coefficientWeightedByGamma = false;
	public static boolean writeDeltaPCorrectOnTrain = false;
	public static boolean pCorrectOnTrainUsingGamma = false;
	public static boolean turnOffItemFeaturesWhenWritingDeltaPCorrect = false;

	// These are the configuration for experimenting FAST+IRT.
	public static boolean shareAddress = false;
	public static boolean readOneHmmOneTime = false;
	// This is for generating IRT (weka) input per HMM.
	public static boolean generateLRInputs = false;
	public static boolean swapData = false;
	// if inputProvideFeatureColumns=true, then train and test files should have
	// same # and order of features
	public static boolean inputProvideFeatureColumns = true;
	public static HashSet<String> newStudents = new HashSet<String>();
	public static HashSet<String> newItems = new HashSet<String>();
	public static HashSet<String> upTillNowNewStudents = new HashSet<String>();
	public static HashSet<String> upTillNowNewItems = new HashSet<String>();

	public static boolean writeTrainPredFile = false;
	// TODO: by default, it is adding in a shared (by both hidden states) way, so
	// opts.modelName should contain("dupbias"); haven't tested completely
	public static boolean addSharedStuDummyFeatures = false;
	public static boolean addSharedItemDummyFeatures = false;
	public static boolean inputHasStepColumn = true;
	public static boolean hasStudentDummy = false;

	// "These are all for storing information dynamically, no need to configure.")
	// [nowInTrain] is for dynamcially recording whether it is in train or test.")
	public static int wholeProcessRunId = 0;
	public static int randomRestartId = 0;
	public static int currentKCIndex = -1;
	public static String currentKc = "";
	public static boolean nowInTrain = true;
	public static double bestAucOnDevAllKcsSum = 0.0;
	public static double aucOnTestAllKcsSum = 0.0;
	public static boolean writeForTheBestAucOnDev = false;
	public static boolean preDpCurDpFromDifferentSet = false;
	public static int nbDataPointsInTrainPerHmm = 0;
	public static int currentBaumWelchIteration = 0;
	public static double nbLlError = 0;
	public static int maxNbInsWeightRoundTo0PerIterPerHmm = 0;
	public static int maxNbInsWeightRoundTo0PerIter = 0;
	public static double maxLLDecreaseValuePerIterPerHmm = 0;
	public static double maxLLDecreaseValuePerIter = 0;
	public static double maxLLDecreaseRatioValuePerIterPerHmm = 0;// valueChange/valueAvg
	public static double maxLLDecreaseRatioValuePerIter = 0;// valueChange/valueAvg
	public static boolean cgReachesTrustRegionBoundary = false;
	public static double[][] currentIterHiddenStatesSmallLLs;
	public static double[] currentIterHiddenStatesSmallLLs_;
	public static double[][] currentIterHiddenStatesExpectedCount;
	public static double[] currentIterHiddenStatesExpectedCount_;
	public static String[] currentIterfeatureWeights;
	public static String currentIterfeatureWeights_;
	public static HashSet<String> hmmsForcedToNonParmTrainDueToLBFGSException = new HashSet<String>();

	public static String mainLogFile = outDir + "main.log";
	public static String llLogFile = outDir + "ll.log";
	public static String expectedCountLogFile = outDir + "expectedCount.log";
	public static String featureWeightsLogFile = outDir + "featureWeights.log";
	public static String finalFeatureWeightsFile = outDir
			+ "finalFeatureWeights.log";
	public static String datapointsLogFile = outDir + "dataPoints.log";
	public static String datapointsLogFile2 = outDir + "dataPoints2.txt";
	public static String learningCurveFile = outDir + "learningCurve.txt";
	public static String initLearnForgetProbLogFile = outDir
			+ "initLearnForgetProb.log";
	public static String guessSlipProbLogFile = outDir + "guessSlipProb.log";
	public static String guessSlipProbLogFile2 = outDir + "guessSlipProb2.log";
	public static String gammaLogFile = outDir + "gamma.log";
	public static String testLRInstanceWeightsFile = outDir
			+ "instance-weight.txt";
	public static String testLRLabelFile = outDir + "label.txt";
	public static String testLRFeaturesFile = outDir + "features.txt";
	public static String liblinearInputDataFile = outDir + "liblinear-data.txt";
	public static String testByWekaInputDataFile = outDir + "weka-data.txt";
	public static String deltaPCorrectFile = outDir + "deltaPCorrect.txt";
	public static String bestRegWeightFile = outDir + "bestRegWeight.txt";
	public static String deltaGammaFile = outDir + "deltaGamma.txt";
	public static String perKcAucVsAvgDeltaGammaFile = outDir
			+ "perKcAucVsAvgDeltaGammaFile.txt";
	public static String eachDeltaGammaFile = outDir + "eachDeltaGamma.txt";
	// + "perKcAucVsDeltaGamma2.txt";
	public static String finalHmmParametersFile = outDir
			+ "finalHmmParameters.txt";

	public static BufferedWriter testLRInstanceWeightsWriter = null;
	public static BufferedWriter testLRLabelWriter = null;
	public static BufferedWriter testLRFeatureWriter = null;
	public static BufferedWriter liblinearInputDataWriter = null;
	public static BufferedWriter testByWekaInputDataWriter = null;
	// writes only specified/problemastic iteration
	public static BufferedWriter mainLogWriter = null;
	public static BufferedWriter expectedCountLogWriter = null;
	// writes every iteration
	public static BufferedWriter featureWeightsLogWriter = null;
	public static BufferedWriter llLogWriter = null; // LL, smallll
	public static BufferedWriter datapointsLogWriter = null;
	public static BufferedWriter datapointsLogWriter2 = null;
	public static BufferedWriter learningCurveWriter = null;
	public static BufferedWriter initLearnForgetProbLogWriter = null;
	public static BufferedWriter guessSlipProbLogWriter = null;
	public static BufferedWriter guessSlipProbLogWriter2 = null;
	public static BufferedWriter gammaWriter = null;
	public static BufferedWriter finalFeatureWeightsWriter = null;
	public static BufferedWriter deltaPCorrectWriter = null;
	public static BufferedWriter deltaGammaWriter = null;
	public static BufferedWriter bestRegWeightWriter = null;
	public static BufferedWriter predWriter = null;
	public static BufferedWriter perKcAucVsAvgDeltaGammaWriter = null;
	public static BufferedWriter eachDeltaGammaWriter = null;// perKcAucVsDeltaGammaWriter2
																														// = null;
	// writes final iteration
	public static BufferedWriter finalHmmParametersWriter = null;

	public static HashSet<String> skillsToCheck = new HashSet<String>();
	public static TreeMap<String, Double> kcAvgDeltaGammaMap = new TreeMap<String, Double>();
	public static TreeMap<String, Double> kcTestAucMap = new TreeMap<String, Double>();

	public static DecimalFormat formatter;
	static {
		formatter = (DecimalFormat) DecimalFormat.getInstance(Locale.US);
		formatter.applyPattern("#.######");
	}

	public static DecimalFormat formatter2;
	static {
		formatter2 = (DecimalFormat) DecimalFormat.getInstance(Locale.US);
		formatter2.applyPattern("#.####");
	}

	public static Random wholeProcessBaseRand;
	public static Random[] perHmmBaseRand;
	public static Random[][] featureWeightsRands;
	public static Random[][] nonFeatureParasRands;

	public static void configure() {

		modelName = basicModelName
				+ (variant1ModelName.equals("") ? ""
						: ("-" + variant1ModelName + (variant2ModelName.equals("") ? ""
								: ("-" + variant2ModelName))));//
		checkConfig();

		allModelComparisonOutDir = basicDir;
		inDir = basicDir
				+ basicModelName
				+ "/"
				+ (variant1ModelName.equals("") ? (datasplit + "/")
						: (variant1ModelName + "/" + datasplit + "/"));
		outDir = inDir + modelName + "/";
		predPrefix = modelName + "_test";
		predictionFile = outDir + predPrefix + (testSingleFile ? "0" : "")
				+ predSurfix;
		curFoldRunTrainInFilePrefix = trainInFilePrefix
				+ (testSingleFile ? "0" : "");
		trainFile = inDir + curFoldRunTrainInFilePrefix + inFileSurfix;
		curFoldRunTestInFilePrefix = testInFilePrefix + (testSingleFile ? "0" : "");
		testFile = inDir + curFoldRunTestInFilePrefix + inFileSurfix;
		curFoldRunDevInFilePrefix = devInFilePrefix + (testSingleFile ? "0" : "");
		devFile = inDir + curFoldRunDevInFilePrefix + inFileSurfix;

		skillsToCheck.add("Variables");
	}

	// This is called for all kinds of argumentation input for main().
	public static void reconfigure() {
		File curActionD = new File(outDir);
		if (!curActionD.exists()) {
			System.out.println("\nCreating directory: " + outDir);
			boolean result = curActionD.mkdir();
			if (!result) {
				System.out.println("Error: directory creation failed: " + outDir);
				System.exit(1);
			}
		}

		predPrefix = modelName + "_test";
		predictionFile = outDir + predPrefix + (testSingleFile ? "0" : "")
				+ predSurfix;
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

		ArrayList<Double[]> hiddenStateIsmallLL = new ArrayList<Double[]>();
		currentIterHiddenStatesExpectedCount = new double[nbHiddenStates][];
		currentIterfeatureWeights = new String[nbHiddenStates];
		// before, after
		currentIterHiddenStatesSmallLLs = new double[nbHiddenStates][2];
		currentIterHiddenStatesSmallLLs_ = new double[2];
	}

	public static void checkConfig() {
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
					.println("ERROR: testSingleFile=true && (numFolds > 1 || numRuns > 1)");
			System.exit(1);
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

	public static void writeLogFiles(String str) throws IOException {
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
