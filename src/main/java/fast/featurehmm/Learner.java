package fast.featurehmm;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Random;
import fast.common.Bijection;
import fast.common.Utility;
import fast.data.StudentList;
//import fast.evaluation.TrainSummary;
//import fast.evaluation.TrainSummary;

public class Learner {

	private final int nbHiddenStates, nbObsStates;
	private final boolean parameterizing, parameterizedInit,  parameterizedTran,  parameterizedEmit;
	private final boolean allowForget;//, specify_initial_values;
	private final double INIT_K0, INIT_L, INIT_G, INIT_S;
	private final double PROBABILITY_MIN_VALUE;
	private final double LBFGS_REGULARIZATION_WEIGHT, LBFGS_REGULARIZATION_BIAS;
	private final Random featureWeightsRand, nonFeatureParasRands;
	private final boolean useBaumWelchScaledLearner;
	private final double EM_TOLERANCE, EPS, ACCETABLE_LL_DECREASE;
	private final int EM_MAX_ITERS;
	private final double LBFGS_TOLERANCE;
	private final int LBFGS_MAX_ITERS;
	private final double initialWeightsBounds;

	private double[] regularizationWeightsForLBFGS, regularizationBiasesForLBFGS;
	private BaumWelchLearner learner; 
	
	//TODO: simplify the parameters
	public Learner(int nbHiddenState, int nbObsStates,
									boolean parameterizing, boolean parameterizedInit, boolean parameterizedTran, boolean parameterizedEmit,
									boolean allowForget, 
									double PROBABILITY_MIN_VALUE, double LBFGS_REGULARIZATION_WEIGHT, double LBFGS_REGULARIZATION_BIAS,
									double INIT_K0, double INIT_L, double INIT_G, double INIT_S,
									Random featureWeightsRand, double initialWeightsBounds, Random nonFeatureParasRands,
									boolean useBaumWelchScaledLearner,
									double EM_TOLERANCE, double EPS, double ACCETABLE_LL_DECREASE, int EM_MAX_ITER,
									double LBFGS_TOLERANCE, int LBFGS_MAX_ITERS) throws IOException {
				this.nbHiddenStates = nbHiddenState;
				this.nbObsStates = nbObsStates;
				this.parameterizing = parameterizing;
				this.parameterizedInit = parameterizedInit;
				this.parameterizedTran = parameterizedTran;
				this.parameterizedEmit = parameterizedEmit;
				this.allowForget = allowForget;
				//this.specify_initial_values = specify_initial_values;
				this.PROBABILITY_MIN_VALUE = PROBABILITY_MIN_VALUE;
				this.LBFGS_REGULARIZATION_WEIGHT = LBFGS_REGULARIZATION_WEIGHT;
				this.LBFGS_REGULARIZATION_BIAS = LBFGS_REGULARIZATION_BIAS;
				this.INIT_K0 = INIT_K0;
				this.INIT_L = INIT_L;
				this.INIT_G = INIT_G;
				this.INIT_S = INIT_S;
				this.featureWeightsRand = featureWeightsRand;
				this.initialWeightsBounds = initialWeightsBounds;
				this.nonFeatureParasRands = nonFeatureParasRands;
				this.useBaumWelchScaledLearner = useBaumWelchScaledLearner;
				this.EM_TOLERANCE = EM_TOLERANCE;
				this.EPS = EPS;
				this.ACCETABLE_LL_DECREASE = ACCETABLE_LL_DECREASE;
				this.EM_MAX_ITERS = EM_MAX_ITER;
				this.LBFGS_TOLERANCE = LBFGS_TOLERANCE;
				this.LBFGS_MAX_ITERS = LBFGS_MAX_ITERS;
	}

	
		public FeatureHMM train(StudentList curKcStuList) throws IOException{
//		opts.regularizationWeightsForLBFGS = initializer.regularizationWeightsForLBFGS;
//		opts.regularizationBiasesForLBFGS = initializer.regularizationBiasesForLBFGS;

			Bijection initFeatures = curKcStuList.getInitFeatures();
			Bijection tranFeatures = curKcStuList.getTranFeatures();
			Bijection emitFeatures = curKcStuList.getEmitFeatures();
			
			FeatureHMM hmm = getRandomFeatureHMM(initFeatures.getSize(), tranFeatures.getSize(), emitFeatures.getSize(), //Bijection outcomes, 
																					initialWeightsBounds, featureWeightsRand, nonFeatureParasRands);
			if (curKcStuList.size() == 0)
				return hmm;
			BaumWelchLearner learner; 
			
			if (useBaumWelchScaledLearner) 
				learner = new BaumWelchScaledLearner(parameterizing, parameterizedInit, parameterizedTran, parameterizedEmit, nbHiddenStates, EM_TOLERANCE, EPS, ACCETABLE_LL_DECREASE, EM_MAX_ITERS);
			else 
				learner = new BaumWelchLearner(parameterizing, parameterizedInit, parameterizedTran, parameterizedEmit, nbHiddenStates, EM_TOLERANCE, EPS, ACCETABLE_LL_DECREASE, EM_MAX_ITERS);
			//learner.setNbIterations(EM_MAX_ITERS);
			hmm = learner.learn(hmm, curKcStuList);
			this.learner = learner;
			return hmm;
	}

	public FeatureHMM getRandomFeatureHMM(int nbInitFeatures, int nbTranFeatures, int nbEmitFeatures, //Bijection outcomes, 
																double initialWeightsBounds, Random featureWeightsRand, Random nonFeatureParasRands) throws IOException {
	
//		Bijection hiddenStates = new Bijection();
//		if (nbHiddenStates == 2){
//			hiddenStates.put("hidden0");
//			hiddenStates.put("hidden1");
//		}
//		else{
//			for (int i = 0; i < nbHiddenStates; i++)
//				hiddenStates.put("hidden" + i);
//		}
//		int nbClasses = hiddenStates.getSize();
	
		ArrayList<PdfFeatureAwareLogisticRegression> initials = new ArrayList<PdfFeatureAwareLogisticRegression>();
		ArrayList<PdfFeatureAwareLogisticRegression> transitions = new ArrayList<PdfFeatureAwareLogisticRegression>();
		ArrayList<PdfFeatureAwareLogisticRegression> emissions = new ArrayList<PdfFeatureAwareLogisticRegression>();
		
		if (parameterizing && parameterizedInit)
			initializeParameterizedProb(initials, nbHiddenStates, nbHiddenStates, nbInitFeatures, initialWeightsBounds, featureWeightsRand);
		else
			initializeNonParameterizedProb(initials, nbHiddenStates, nbHiddenStates, "init", nonFeatureParasRands);
	
		if (parameterizing && parameterizedTran)
			initializeParameterizedProb(transitions, nbHiddenStates, nbHiddenStates, nbTranFeatures, initialWeightsBounds, featureWeightsRand);
		else
			initializeNonParameterizedProb(transitions, nbHiddenStates, nbHiddenStates, "trans", nonFeatureParasRands);
	
		if (parameterizing && parameterizedEmit)
			initializeParameterizedProb(emissions,  nbHiddenStates, nbObsStates, nbEmitFeatures, initialWeightsBounds, featureWeightsRand);
		else
			initializeNonParameterizedProb(emissions, nbHiddenStates, nbObsStates, "emit", nonFeatureParasRands);

		
		FeatureHMM hmm = new FeatureHMM(initials, transitions, emissions); // (opts.limit_fast_guess_slip?opts.FAST_GUESS_SLIP_LIMIT: null),
	
		return hmm;
	}

	/** classes can be outcomes or hiddenStates */
	public void initializeNonParameterizedProb( ArrayList<PdfFeatureAwareLogisticRegression> pdfs,
			int nb1stStates, int nbClasses,
			String type, Random nonFeatureParasRand) {//Bijection classes, 
		int nb2ndStates = nbClasses;
		if (nb2ndStates == 0)
			nb2ndStates = nb1stStates;
		double[][] params = new double[nb1stStates][nb2ndStates];
		for (int i = 0; i < nb1stStates; i++)
			params[i] = Utility.uniformRandomArraySumToOne(nb2ndStates, PROBABILITY_MIN_VALUE, 1, nonFeatureParasRand);
		
	//	if (specify_initial_values){
		if (allowForget){
//		params[1][0] = opts.INIT_FORGET;
//		params[0][0] = 1 - opts.INIT_FORGET;
			System.out.println("WARNING: the code doesn't support allowForget=true yet!");
			System.exit(-1);
		}
		if (type.contains("tran") && INIT_L >= 0) {
			params[0][1] = INIT_L;
			params[0][0] = 1 - INIT_L;
//			params[1][0] = 0.0;
//			params[1][1] = 1.0;
		}	
		else if (type.contains("emi")){
			if (INIT_G >= 0){
				params[0][1] = INIT_G;
				params[0][0] = 1 - INIT_G;
			}
			if (INIT_S >= 0){
				params[1][0] = INIT_S;
				params[1][1] = 1 - INIT_S;
			}
		}
	  else if (type.contains("init") && INIT_K0 >= 0) {//only the second dimension is used
			params[0][1] = INIT_K0;
			params[0][0] = 1 - INIT_K0;
		}
		
		if (type.contains("tran") && !allowForget){
			params[1][0] = 0.0;
			params[1][1] = 1.0;
		}
		
		for (int i = 0; i < nb1stStates; i++) {
			PdfFeatureAwareLogisticRegression pdf = new PdfFeatureAwareLogisticRegression(parameterizing, 
																											parameterizedInit, parameterizedTran, parameterizedEmit,
																											allowForget,
																											PROBABILITY_MIN_VALUE, nbHiddenStates, nbObsStates,
																											regularizationWeightsForLBFGS, regularizationBiasesForLBFGS,
																											LBFGS_TOLERANCE, LBFGS_MAX_ITERS);
			if (type.contains("tran") || type.contains("emi")){
				pdf.initialize(params[i]);//classes
			}
			else if (type.contains("init")) {
				pdf.initialize(params[0][i]);//ensure sum up to 1; nb1stStates=nb2ndStates
			}
			pdfs.add(pdf);
		}
	}

	/** 
	 * classes can be outcome or hiddenstates.
	 * TODO: even if we don't allow forget, here still initialize the feature weights. so we always need to use "allowForget" to judge when computing transition probabilities.
	 */
	public void initializeParameterizedProb(ArrayList<PdfFeatureAwareLogisticRegression> pdfs, 
																				int nbHiddenStates, int nbClasses, int nbFinalFeatures, //Bijection finalFeatures, Bijection classes,
													 							double initialWeightsBounds, Random featureWeightsRand) throws IOException {
		int featureDimension = -1;
		featureDimension = nbFinalFeatures;
		double[] initialFeatureWeights = new double[featureDimension];
		initialFeatureWeights = Utility.uniformRandomArray(featureDimension, initialWeightsBounds * (-1.0), initialWeightsBounds, featureWeightsRand);
	
		regularizationWeightsForLBFGS = new double[nbFinalFeatures];
		regularizationBiasesForLBFGS = new double[nbFinalFeatures];
		for (int f = 0; f < nbFinalFeatures; f++) {
			regularizationWeightsForLBFGS[f] = LBFGS_REGULARIZATION_WEIGHT;
			regularizationBiasesForLBFGS[f] = LBFGS_REGULARIZATION_BIAS;
		}
		
		// TODO: model forget..
		PdfFeatureAwareLogisticRegression pdf = new PdfFeatureAwareLogisticRegression(
																								parameterizing, parameterizedInit, parameterizedTran, parameterizedEmit,
																								allowForget,
																								PROBABILITY_MIN_VALUE, nbHiddenStates, nbObsStates,
																								regularizationWeightsForLBFGS, regularizationBiasesForLBFGS,
																								LBFGS_TOLERANCE, LBFGS_MAX_ITERS);
		pdf.initialize(initialFeatureWeights, nbClasses);//, finalFeatures);//classes, 
	
		// CAUTION: even put the same transition/emission object, later when initializing hmm, actually it turns into two objects
		// TODO: later when fitting the feature weights, both objects are assigned the same addresses' same weights; try to improve this design!
		for (int i = 0; i < nbHiddenStates; i++) 
			pdfs.add(pdf);
	}
	
//	public TrainSummary getTrainSummary(){
//	   return learner.getTrainSummary();
//	}
	public double getTrainLL(){
		return learner.getTrainLL();
	}
	
	public int getNbLLError(){
		return learner.getNbLLError();
	}
	
	public double getMaxLLDecrease(){
		return learner.getMaxLLDecrease();
	}
	
	public double getMaxLLDecreaseRatio(){
		return learner.getMaxLLDecreaseRatio();
	}
	
	public int getNbParameterizingFailed(){
		return learner.getNbParameterizingFailed();
	}
	
	public int getNbStopByEMIteration(){
		return learner.getNbStopByEMIteration();
	}

}
