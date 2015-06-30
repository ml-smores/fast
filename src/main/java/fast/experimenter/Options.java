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
import java.io.File;
//import java.io.FileWriter;
//import java.io.IOException;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
//import java.util.ArrayList;
//import java.util.Date;
//import java.util.HashMap;
//import java.util.HashSet;
import java.util.Locale;
import java.util.Random;
//import java.util.TreeMap;
//import fast.common.Bijection;
//import fast.evaluation.*;
import fig.basic.Option;

public class Options {

	@Option(gloss = "modelName should contain either \"FAST\" or \"KT\" in the string (capital letters). ")
	public String modelName = "KT";

	@Option(gloss = "If use features(FAST), then please configure parameterizing=true.")
	public boolean parameterizing = false;
	@Option(gloss = "parameterizingInit will parameterize initial probabilities.")
	public boolean parameterizingInit = false;
	@Option(gloss = "parameterizingTran will parameterize transition probabilities.")
	public boolean parameterizingTran = false;
	@Option(gloss = "parameterizingEmit will parameterize emission probabilities.")
	public boolean parameterizingEmit = false;
	/* DataPointList.expandFeatureVector() */
	@Option(gloss = "If forceUsingInputFeatures=true, it allows you to use all input features without differentiating which probability to be parameterized (initial/transition/emission); Otherwise the code uses \"init_\", \"tran_\" or \"emit_\" prefix to recognize corresponding feature columns for initial, transition or emission probabilities (e.g., \"tran_feature_XXX\").")
	public boolean forceUsingAllInputFeatures = true;
	
	@Option(gloss = "generateStudentDummy is for automatically generating binary student dummies (indicators) based on training dataset.")
	public boolean generateStudentDummy = false;
	@Option(gloss = "generateItemDummy is for automatically generating binary item dummies (indicators) based on training dataset. By default, it treats the \"problem\" column as the item column (one problem is one item).")
	public boolean generateItemDummy = false;

	@Option(gloss = "nbRandomRestart is used for specifying how many random starts (for initialing parameters) for each HMM(KC).")
	public int nbRandomRestart = 3;// 20 //18s/time
	@Option(gloss = "nbFiles is used to decide how many train-test pairs so that the code can read train-test files automatically (e.g. if nbFiles=10, trainInFilePrefix=\"train\", testInFilePrefix=\"test\", inFileSuffix=\".csv\", then the code automatically reads train0.csv~train9.csv.")
	public int nbFiles = 3;
	
	@Option(gloss = "inDir is for getting input files(train and test).")
	public String inDir = "./data/temp/";//"./data/IRT_exp/";
	@Option(gloss = "outDir is for getting output prediction files and log files.")
	public String outDir = "./data/temp/";//"./data/IRT_exp/";
//	@Option(gloss = "allModelComparisonOutDir is where all different models are compared (by the average evaluation metric).")
//	public String allModelComparisonOutDir = outDir;
	@Option(gloss = "trainInFilePrefix combined with nbFiles and inFileSuffix is used for retrieving train file. If trainInFilePrefix=\"train\" and inFileSuffix=\".csv\", then 1) if nbFiles=1, train file will be \"train0.csv\", 2) if nbFiles>1, train file will be automatically configured with an increasing id as surfix. e.g., \"train1.txt\" for the 2nd file.")
	public String trainInFilePrefix = "KT_train";
	@Option(gloss = "testInFilePrefix combined with nbFiles and inFileSuffix is used for retrieving test file. If testInFilePrefix=\"test\" and inFileSuffix=\".csv\", then 1) if nbFiles=1, test file will be \"test0.csv\", 2) if nbFiles>1, test file will be automatically configured with an increasing id as surfix. e.g., \"test1.txt\" for the 2nd file.")
	public String testInFilePrefix = "KT_test";
	@Option(gloss = "inFileSuffix is for specifying train and test file suffixes.")
	public String inFileSuffix = ".txt";//".csv";	

	@Option(gloss = "EMMaxIters is used for the maximum iteration of outer EM. Setting smaller value could make training stop earlier, yet could decrease accuracy.")
	public int EMMaxIters = 500;// 500; EDM:400; doesn't influence auc that much.
	/* original package: 20; EDM:500; doesn't influence speed that much; doesn't influence auc that much */
	@Option(gloss = "LBFGSMaxIters is used for the maximum iteration of inner LBFGS. Setting smaller value could make training stop earlier, yet could decrease accuracy.")
	public int LBFGSMaxIters = 50;
	
	/* EDM:1.0E-6; default for BNT: absolute 1.0E-4; influence the speed and auc; when setting -10, most >500iter, some more than 1000 iter FAST with features vs. KT: under 1.0E-4, FAST may be worse than KT, yet under 1.0E-6 FAST can be similar or better. */
	@Option(gloss = "EMTolerance is used to decide the convergence of outer EM. Setting bigger value could make training stop earlier, yet could decrease accuracy.")
	public double EMTolerance = 1.0E-6;// 1.0E-6;
	/* LBFGS_TOLERANCE influence the speed a lot; it seems higher the value higher the auc, and its influence is bigger than that of EM's iteration and its own iteration FAST with features vs. KT: under 1.0E-4, FAST may be worse than KT, yet under 1.0E-6 FAST can be similar or better. original package: 1.0E-10; EDM:1.0E-6; (setting 1.0E-10 is similar as 6) */
	@Option(gloss = "LBFGSTolerance is used to decide the convergence of inner LBFGS. Setting bigger value could make training stop earlier, yet could decrease accuracy.")
	public double LBFGSTolerance = 1.0E-6;// 1.0E-6;

	
	
	
	
	/* Following are optional configurations*/
	
	@Option(gloss = "By default bias=true. bias=true means adding a bias(intercept) to the featue space (By default, different hidden states will use differnt biases). bias can only be false for KT.") 
	public boolean bias = true;// -1.0
	
	// @Option(gloss = "differentBias=true means using one bias feature for \"known\" state and another bias feature for the \"unknown\" state. To set it true is necessary when other features are all shared by two states.")
	//public boolean differentBias = bias ? true : false;
	/*
	 * TODO: for specifying initial values for HMM When assigning them, it assumes 0 is unknown 1 is known state, so the assigning is correct when !allowForget; 
	 * TODO: However, when allowforget, these assignments may not be corresponding!
	 */
//	@Option(gloss = "specify_initial_values is for specifying the initial values of KT parameters. By default the code randomly generates this value (specify_initial_values=false).")
//	public boolean specify_initial_values = false;
	@Option(gloss = "initialK0 is for specifying KT init(Prob(known)). By default the code initializes randomly (initialK0=-1).")
	public double initialK0 = -1;//0.68;// 0.68//0.60;//0.67;
	@Option(gloss = "initialT is for specifying KT learn(Prob(known|unknown)). By default the code initializes randomly (initialT=-1).")
	public double initialT = -1;//0.50;// 0.55;//0.48;
	//public double INIT_FORGET = 0.0; // will be over written by allowForget configuration
	@Option(gloss = "initialG is for specifying KT guess(Prob(correct|unknown)). By default the code initializes randomly (initialG=-1).")
	public double initialG = -1;//0.1;// 0.30;//0.53;
	@Option(gloss = "initialS is for specifying KT slip(Prob(incorrect|known)). By default the code initializes randomly (initialS=-1).")
	public double initialS =-1;// 0.1;// 0.30;//0.33;

	@Option(gloss = "initialWeightsBounds is for deciding initial lower and upper bound for each feature coefficient. By default 10.0.")
	public double initialFeatureWeightsBounds = 10.0;
	
	@Option(gloss = "If useReg=true, the logistic regression part uses regularization..")
	public boolean useReg = true;
	
	@Option(gloss = "For LBFGS, regulariztion term is sum_i[ c *(w_i - b)^2 ] where c is regularization weight(LBFGSRegWeight) and b is regularization bias(LBFGSRegBias).")
	public double LBFGSRegWeight = useReg ? 1.0 : 0.0;
	//public double[] LBFGS_REGULARIZATION_WEIGHT_RANGE = { 0.01, 0.1, 1, 10, 100 };
	// { 1.0E-5, 1.0E-4,1.0E-3, 0.01, 0.1, 1, 5, 10, 30, 50, 100 };//{ 1.0 };
	//public double[] regularizationWeights;
	@Option(gloss = "For LBFGS, regulariztion term is sum_i[ c *(w_i - b)^2 ] where c is regularization weight(LBFGSRegWeight) and b is regularization bias(LBFGSRegBias).")
	public double LBFGSRegBias = 0.0;
	//public double[] regularizationBiases;
	
	//@Option(gloss = "By default, the code does't allow forget.")
	public boolean allowForget = false; //here by allowForget=false, we set p(0|1)=0, so we always have hidden state 1 as known state.	
	//@Option(gloss = "If true, automatically create one subfolder per KC under outDir folder using each KC's name specified in KC column in input file.")
	public boolean outputMultiRestarts = false;
	
	//@Option(gloss = "Directory for outputing best model from multiple random starts. By default best model is the one with the maximum LL (if multiple HMMs are involved then the maximum mean LL across HMMs) on train.")
	//public String bestModelFileNameOutDir = inDir;
	//public String bestModelFileNameFile = bestModelFileNameOutDir + "BestModels.csv";

//	@Option(gloss = "For printing out initial HMM.")
//	public boolean printInitHmm = false;
//	@Option(gloss = "For printing out final HMM.")
//	public boolean printFinalHmm = false;
	//@Option(gloss = "By default bias=1.0.")
//	/** Caution: if one of the dummy is totally decided by other dummies, then adding bias will cause collinearity problem! In other cases, please specify bias=1.0! 
	
	//public String devInFilePrefix = "dev";// (modelName.contains("FAST")?"FAST":"KT") + "_" +
	public String evalSurfix = "_Evaluation.csv";// ".eval"
	public String evalFile = outDir + "File0_" + modelName + evalSurfix;
	public String allFilesEvalFile = outDir + "AllFiles_" + modelName + evalSurfix;

	//@Option(gloss = "If nbFiles>0, trainFile is configured dynamically with changing fileId, e.g. train(test) file will be \"train(test)InFilePreifixFoldId.txt\", e.g. \"train0.txt\" or \"test0.txt\" for the first fold.")
	public String trainFile = inDir + trainInFilePrefix + "0" + inFileSuffix;
	//@Option(gloss = "If there are more than one runs, these are configured dynamically in each run with changing foldID, e.g. train(test) file will be \"train(test)InFilePreifixFoldId.txt\", e.g. \"train0.txt\" or \"test0.txt\" for the first fold.")
	public String testFile = inDir + testInFilePrefix + "0" + inFileSuffix;
	//public String devFile = inDir + devInFilePrefix + "0" + inFileSuffix;
	//@Option(gloss = "predSuffix=.pred is neccessary if we are to use the evaluation code inside this code.")
	public String predSuffix = "_Prediction.csv";
	// predictionFile should contain modelName, because modelName and predSuffix
	// are used together to identify prediction file automatically by Evaluation
	// engine.
	public String predictionFile = outDir + "File0" + "_" + modelName + predSuffix;
//	public String hmmNameFileSurfix = "_AllHMMNames.csv";
//	public String hmmNameFileName = outDir + "File0" + "_" + modelName  + hmmNameFileSurfix;

	public String mainLogFileName = "Runtime.log";
	public String mainLogFile = outDir + mainLogFileName;
	public String parametersFileSurfix = "_Parameters.csv";
	public String parametersFile = outDir + "File0_" + modelName + parametersFileSurfix;
	//public String allProcessSummaryFile = outDir + "AllRestarts_" + modelName +  evalSurfix;
	//public String allHmmsSummaryDir = outDir;// + "allHmms_" + modelName + ".eval.csv";
	// TODO: changed to LL later since we are choosing for each KC
	public String oriOutDir = ""; // for temporally storing the outDir, useful when we output per KC, because we output the overall summary in the oriOutDir.

	//public String chooseBestAmongRandomStarts = "ll";// "meanLL";//"#deg_kc
	public boolean judgeHiddenByEmit = true;
	//public boolean computeStatForParameters = true;
	public String degInequalityStr = "be"; // or "b" //bigger or equal to
//	public double GLOBAL_OPTIMA_DIST = 1.0E-3; // [max(LL)+max(LL)*LL_GLOBAL_OPTIMA_DIST, max(LL)]
//	public String globaloptima_judge_metric = "ll";
//	public String nondeg_judge_metric = "guess+slip_feauture_off";
//	public String distToGlobalOptimumStr = "avgILGSdistance_to_globaloptimum";
//	public String distToGlobalOptimumNonDegStr = "avgILGSdistance_to_globaloptimum&nondeg";
//	public String probGlobalOptimalStr = "p(" + globaloptima_judge_metric + "_global_optimal)";
//	public String probGlobalOptimalAndNonDegStr = "p(" + globaloptima_judge_metric + "_global_optimal&" + ((degInequalityStr.equals("be") ? nondeg_judge_metric + "<1" : nondeg_judge_metric + "<=1")) + ")";
//	public String outputILGSNameSurfix = "_feature_off";// must start with "_" because of later parsing

	//@Option(gloss = "By default, configure baumWelchScaledLearner=true meaning that we use baumWelchScaledLearner.")
	public boolean useBaumWelchScaledLearner = true;
	public final double PROBABILITY_MIN_VALUE = 0.0;// 1.0E-6;
	// public final double PROBABILITY_MAX_VALUE = (1.0 - PROBABILITY_MIN_VALUE);
//	public boolean removeSeqLength1InTrain = false;
//	public boolean removeSeqLength1InTest = false;

	public boolean getDataPointRelatedDegeneray = false;
	public boolean getMastery = false;
	// @Option(gloss =
	// "These are for both LIBLINEAR and LBFGS. By default use gamma as instance weight and use it(instead of class weight) to train parameterized emission.")
	//public boolean useGammaAsInstanceWeight = true;
	//public boolean useXiAsInstanceWeight = true;
	
//	@Option(gloss = "LBFGS=true means use LBFGS to optimize the logistic regression part; (deprecated: otherwise use LIBLINEAR.)")
//	public boolean LBFGS = true;

	
	//public double INSTANCE_WEIGHT_ROUNDING_THRESHOLD = -1.0;// : 1.0E-4;// -1.0;//
	//public double INSTANCE_WEIGHT_MULTIPLIER = 1.0;

	//public boolean oneKcInKcColumn = true;
	// @Option(gloss =
	// "used to decide whether use same intialization for featureHMM and non-featureHMM. Only useful when use 1 bias feature version of featureHMM")
	// public boolean sameInitForFeatureAndNonfeature = false;
	// TODO: didn't use following but use process instead
	// public int randomRestartPerHmmTimes = 1;
	
	//@Option(gloss = "for initializing both paramerized and non-paramtereized paramters' values (usually set 1).")
	public double initDirichlet = 1;// take care to change
	// existed papers: maximum probabilities of initial knowledge, guess, slip and
	// learning are 0.85, 0.3, 0.1 and 0.3, respectively.
	// public double guessBound = 0.5;
	// public double slipBound = 0.5;
	//@Option(gloss = "To decide printing out verbose information or not.")
	public boolean verbose = false;
	

	/*
	 * @Option(gloss = "To decide printing out verbose information for LBFGS optimization result or not." ) public boolean LBFGSverbose = false;
	 * @Option(gloss = "To decide printing out verbose information for LBFGS optimization iteration or not." )
	 * public boolean LBFGS_PRINT_MINIMIZER = false;
	 */
	
	// @Option(gloss = "EPS is for avoiding divided by 0.")
	public final double EPS = 1E-10;

	// @Option(gloss = "EXPECTED_COUNT_SMALL_VALUE is used to decide whether expected count is too small and if so print out warning in log files.")
	//public double EXPECTED_COUNT_SMALL_VALUE = 1.0E-4;// 1.0E-6;
	//@Option(gloss = "ACCETABLE_LL_DECREASE is used to decide whether LL decrease is too small and if so print out warning in log files.")
	public final double ACCEPTABLE_LL_DECREASE = 1.0E-3;

	// @Option(gloss =
	// "To decide print out intermediate files for testing liblinear or LBFGS outside.")
	// public boolean testLiblinear = false;
	// public boolean testLogsticRegression = false;
	// public String skillToTest = "ArrayList";

	// public boolean useTransitionToJudgeHiddenStates = useEmissionToJudgeHiddenStates ? false
	// : true;
	// @Option(gloss =
	// "These are decided by default to give convinience to do inference")
	// [hiddenState1] in prediction, will decide again by seeing
	// P(correct|hiddenState); in other places are used to initialize forget (if
	// don't allow forget)
	// public int realHiddenState1 = 1;// known
	// public int realHiddenState0 = 1 - realHiddenState1;// unknown
	
//	/**
//	 * when fitting hmm, index 0(1) is always corresponding to 0(1)
//	 */
	// public int hiddenState1 = 1;
	// public int hiddenState0 = 1 - hiddenState1;
	// public int knownState = 1; // without checking
	// public int unknownState = 0; // without checking
//	/**
//	 * obsClass1 corresponds to "correct", decides: (1) reading input in DataPointList.java, put "incorrect" first (2) LR always output p(Correct). After these, obsClass1 is always "correct"
//	 */
	// @Option(gloss = "These are decided by default to give convinience to do inference and decide LR's probabilities")
	// public int obsClass1 = 1;
	// @Option(gloss = "For discrete classification, the string that is going to be class # 1")
	// public String obsClass1Name = "correct";
	// public int obsClass0 = 1-obsClass1;
	// public String obsClass0Name = "incorrect";
	
	//@Option(gloss = "These are decided by default to give convinience to do inference and decide LR's probabilities")
	public final int Nb_HIDDEN_STATES = 2;
	//@Option(gloss = "These are decided by default to give convinience to do inference and decide LR's probabilities")
	public final int NB_OBS_STATES = 2;

//	@Option(gloss = "for writing all random restarts summary.")
//	public boolean writeAllProcessSummary = true;
//	@Option(gloss = "for decising what information to log.")
//	public boolean writeParameters = true;
//	@Option(gloss = "for decising what information to log.")
//	public boolean writeMainLog = true;
	
	// @Option(gloss = "for decising what information to log.")
	//public boolean writeFinalFeatureWeights = true;
	// TODO: formatting the output: Option(gloss =
	// "writeOnlyOriginalFinalFeatureWeights=False but writeFinalFeatureWeights=True, then will write also the difference of feature weights between two states.")
	//public boolean writeOnlyOriginalFinalFeatureWeights = true;
	
//	public boolean writeTrainPredFile = false;
//	public boolean inputHasStepColumn = true;

	// "These are all for storing information dynamically, no need to configure.")
	//public String curHmmName = "";
	
//@Option(gloss = "testSingleFile is used to decide just run one train test pair or multiple train, test pairs.")
//public boolean testSingleFile = true;
//@Option(gloss = "nbFolds and nbRuns are used to decide how many times FAST runs. For testSingleFile=true, numFolds and numRuns should be set to 1. The code uses foldID to change the train, test file name (e.g. train0.txt~train9.txt are for two runs of 5 fold CV or one run of 10 fold CV) foldId = runID * opts.numFolds + foldID")
//public int nbFolds = 1;

// public double[] initialWeightsBounds = { -10.0, 10.0 }; //{ { -0.1, 0.1 }, { -0.1, 0.1 } };
/* if true, evaluation file also contain every feature coef; otherwise, only the original K0, L, G, S. If there is "NULL" in input, will automatically set this option false. */
//public boolean outputFeatureCoefToEval = true;// if different hmms have differnt feature due to null, the code will break
//public HashSet<String> skillsToSkip = new HashSet<String>();
//public HashSet<String> stusToSkip = new HashSet<String>();

//public int startingRestartId = 0;

//	public boolean preDpCurDpFromDifferentSet = false;// for split by sequence checking
//	public int nbDataPointsInTrainPerHmm = 0;
//	public int currentBaumWelchIteration = 0;
//	public double nbLLError = 0;
	//public double maxLLDecreaseValuePerIterPerHmm = 0;
//	public double maxLLDecreaseValuePerIter = 0;
	//public double maxLLDecreaseRatioValuePerIterPerHmm = 0;// valueChange/valueAvg
//	public double maxLLDecreaseRatioValuePerIter = 0;// valueChange/valueAvg
//	public boolean cgReachesTrustRegionBoundary = false;
//	public double[][] currentIterHiddenStatesSmallLLs;
//	public double[] currentIterHiddenStatesSmallLLs_;
//	public double[][] currentIterHiddenStatesExpectedCount;
//	public double[] currentIterHiddenStatesExpectedCount_;
//	public String[] currentIterfeatureWeights;
//	public String currentIterfeatureWeights_;

	public DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
	public DecimalFormat formatter;
	{
		formatter = (DecimalFormat) DecimalFormat.getInstance(Locale.US);
		formatter.applyPattern("#.###");
	}

	
	public void configure() {
		configure(0, 0, "");
	}

	public void configure(int fileId, int restartId, String subfolderName) {
		if (oriOutDir.equals(""))
			oriOutDir = outDir;
		if (!subfolderName.equals("") && outputMultiRestarts)
			outDir = oriOutDir + subfolderName + "/";
		
		trainFile = inDir + trainInFilePrefix + fileId + inFileSuffix;
		testFile = inDir + testInFilePrefix + fileId + inFileSuffix;
		String prefix = "File" + fileId + "_" + (outputMultiRestarts? restartId + "_" : "") + modelName;
		evalFile = outDir + prefix + evalSurfix;
		allFilesEvalFile = outDir + "AllFiles_" + modelName + evalSurfix;
		predictionFile = outDir + prefix + predSuffix;
		parametersFile = outDir + prefix + parametersFileSurfix;
		mainLogFile = outDir + mainLogFileName;

		checkConfig();
	}

	public void checkConfig() {
		if (!(modelName.contains("FAST") || modelName.contains("KT"))) {
			System.out.println("\nError: modelName should contain either \"FAST\" or \"KT\" in the string (capital letters).");
			System.exit(1);
		}

		File curActionD = new File(outDir);
		if (!curActionD.exists()) {
			// System.out.println("\noutDir doesn't exist, creating this directory: "
			// + outDir);
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
			parameterizing = false;
			parameterizingEmit = false;
			parameterizingInit = false;
			parameterizingTran = false;
			generateStudentDummy = false;
			generateItemDummy = false;
			// oneBiasFeature = false;
			//differentBias = false;
			bias = false;			
			// writeFinalFeatureWeights = false;
		}
		else {
			parameterizing = true;
			if (!parameterizingTran && !parameterizingInit)
				parameterizingEmit = true;
			// bias = 1.0;
			//differentBias = (bias? true : false);
			// oneBiasFeature = false;
		}

		judgeHiddenByEmit = allowForget ? true : false;

//		if (readOneHmmOneTime) {
//			System.out.println("Warning: readOneHmmOneTime not ready yet!");
//			System.exit(1);
//		}

		if (allowForget) {
			System.out.println("Warning: allowForget not ready yet!");
			System.exit(1);
		}

//		if (!globaloptima_judge_metric.equals("ll") || !nondeg_judge_metric.equals("guess+slip_feauture_off")) {
//			System.out.println("Warning: !global_optimal_judge_metric.equals(ll) || !non_deg_judge_metric.equals(guess+slip_feauture_off)");
//			System.exit(1);
//		}
//
//		if (!outputILGSNameSurfix.equals("_feature_off")) {
//			System.out.println("Warning: !outputILGSNameSurfix.equals(_feature_off)");
//			System.exit(1);
//		}
		
		if (outputMultiRestarts){
			System.out.println("Warning: outputMultiRestarts not ready yet!");
			System.exit(1);
		}
	}
	
	//public Random baseRand;
	//private Random[] perHmmBaseRand;
	public Random[][] featureWeightsRands;
	public Random[][] nonFeatureParasRands;
	/** given the same id, the same hmms can ge regenerated */
	public void resetRandom(int fileId, int nbRandomRestart, int nbHmms) {
		Random baseRand = new Random(fileId);
		Random[] perHmmBaseRand = new Random[nbHmms];
		featureWeightsRands = new Random[nbRandomRestart][nbHmms];
		nonFeatureParasRands = new Random[nbRandomRestart][nbHmms];
		for (int randomRestartIndex = 0; randomRestartIndex < nbRandomRestart; randomRestartIndex++){
			for (int hmmIndex = 0; hmmIndex < nbHmms; ++hmmIndex) 
				perHmmBaseRand[hmmIndex] = new Random(baseRand.nextInt());
			featureWeightsRands[randomRestartIndex] = perHmmBaseRand;
			
			perHmmBaseRand = new Random[nbHmms];
			for (int hmmIndex = 0; hmmIndex < nbHmms; ++hmmIndex) 
				perHmmBaseRand[hmmIndex] = new Random(baseRand.nextInt());
			nonFeatureParasRands[randomRestartIndex] = perHmmBaseRand;
		}
	}
}
