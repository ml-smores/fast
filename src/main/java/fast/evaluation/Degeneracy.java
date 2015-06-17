package fast.evaluation;

public class Degeneracy { //cur Hmm per process 

	//main
	public Double guessPlusSlipFeatureOff = -1.0; 
  public Double nbDegKcsBasedOnGuessPlusSlipFeatureOff = -1.0;// either 0 or 1; g+s feature off whether >= 1(1) or not (0)
	public String degeneracyJudgementInequality = "be";

  //secondary
	public Double guessPlusSlipFeatureOn = -1.0; //if use null for KT, this is underfined. just consider turn on the first feature (and bias)
	public Double guessPlusSlipAvgPerDP = -1.0; //(g+s on all dp from train and test)
  public Double pctDegDps = -1.0; //%dp that have g+s>(=)1 on both train and test set
  public Double pctDecProbKnown = -1.0;//on test
  public Double pctDecProbCorrect = -1.0; //on test
  public Double minGuessPlusSlipPerDpOnTrain = -1.0;
  public Double minGuessPlusSlipPerDpOnTest = -1.0;
  /*
	 * 0 # degenerated cases by datapoints in train; 
	 * 1 # datapoints in train; 
	 * 2 # degenerated cases by datapoints in test; 
	 * 3 # datapoints in test; 
	 * 4 # sum of guess+slip across datapoints in train  
	 * 5 # sum of guess+slip across datapoints in test 
	 * 6 % decrease pKnow per dp on test 
	 * 7 % decrease pCorrect per dp on test 
	 * 8 minimum g+s per dp on train 
	 * 9 minimum g+s per dp on test
	 */
	public double[] degeneracyJudgementsAcrossDataPoints = new double[10];
	
//public double avgPerKcFeatureOffGuessPlusSlip = -1.0; //avg per kc
//public double avgPerKcFeatureOnGuessPlusSlip = -1.0; //avg per kc
////public double avgPerDpGuessPlusSlip = -1.0; //g+s on all dp from train and test on all kcs
//public double avgPerKcGuessPlusSlipAvgPerDP = -1.0; //avg (g+s on all dp from train and test) per kc
//public double overallNbDegKcs = -1.0; //overall #kcs that have g+s>1 for either train or test set
////public double overallPctDegDps = -1.0; //overall %datapoints that have g+s>1 on both train and test set
//public double avgPerKcPctDegDps = -1.0;

}
