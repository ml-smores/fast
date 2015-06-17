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

package fast.featurehmm;

//import fast.common.Bijection;

public class LogisticRegression {

	private final double[][] featureValues;
	private final int[] labels;
	private final double[] instanceWeights;
	private final String type;
	private final double[] regularizationWeights, regularizationBiases;
	private final double LBFGS_TOLERANCE;
	private final int LBFGS_MAX_ITERS;
	private final PdfFeatureAwareLogisticRegression pdf;
	private final double[] initalFeatureWeights;
	
	private int nbParameterizingFailed = 0;

	public LogisticRegression(PdfFeatureAwareLogisticRegression pdf, double[] instanceWeights, 
							double[][] featureValues, int[] labels, 
							String type, //Bijection featureMapping, Bijection labelMapping, 
							double[] regularizationWeights, double[] regularizationBiases, 
							double LBFGS_TOLERANCE, int LBFGS_MAX_ITERS ) {
		
		
		if (featureValues.length != labels.length) {
			System.out.println("featureValues.length != outcomes.length!");
			System.exit(1);
		}
		if (featureValues[0].length != pdf.getFeatureWeights().length) {
			System.out
					.println("featureValues[0].length != opdf.featureWeights.length");
			System.exit(1);
		}
		if (instanceWeights == null || instanceWeights.length != labels.length) {
			System.out.println("ERROR: instance/class weights are null || weights.length != outcomes.length");
			System.exit(1);
		}
		
		this.pdf = new PdfFeatureAwareLogisticRegression(pdf);
		
		this.initalFeatureWeights = new double[pdf.getFeatureWeights().length];
		for (int f = 0; f < initalFeatureWeights.length; f++) 
			initalFeatureWeights[f] = pdf.getFeatureWeights()[f];
		
		this.instanceWeights = new double[instanceWeights.length];
		for (int i = 0; i < instanceWeights.length; i++) 
			this.instanceWeights[i] = instanceWeights[i];
		
		this.featureValues = new double[featureValues.length][featureValues[0].length];
		for (int i = 0; i < featureValues.length; i++)
			for (int j = 0; j < featureValues[0].length; j++)
				this.featureValues[i][j] = featureValues[i][j];
		
		this.labels = new int[labels.length];
		for (int i = 0; i < labels.length; i++) 
			this.labels[i] = labels[i];

		//this.featureMapping = new Bijection(featureMapping);
		//this.labelMapping = new Bijection(labelMapping);
		this.type = type;
		this.regularizationWeights = regularizationWeights;
		this.regularizationBiases = regularizationBiases;
		this.LBFGS_TOLERANCE = LBFGS_TOLERANCE;
		this.LBFGS_MAX_ITERS = LBFGS_MAX_ITERS;
	}

	public double[] train() {
		LBFGS LBFGSTrain = new LBFGS(initalFeatureWeights, pdf, instanceWeights, featureValues, labels, type, regularizationWeights,
				regularizationBiases, LBFGS_MAX_ITERS, LBFGS_TOLERANCE);
		double[] finalFeatureWeights = LBFGSTrain.run();
		nbParameterizingFailed = LBFGSTrain.getParameterizingResult();
		
		if (finalFeatureWeights.length != featureValues[0].length) {//finalFeatureWeights.length != featureMapping.getSize() |
			System.out.println("featureWeights.length != features[0].length!");//featureWeights.length != featureMapping.getSize() || 
			System.exit(1);
		}
		return finalFeatureWeights;
	}
	
	public int getParameterizingResult(){
		return nbParameterizingFailed;
	}

//	public void checkBiasFeature(Opts opts) {
//		if (opts.bias >= 0 && featureMapping.get("bias") == null) {
//			System.out.println("ERROR: Requiring bias feature inside the datapoint! bias="
//							+ opts.bias // + ",onebiasfeature=" + opts.oneBiasFeature
//							+ ",featureMapping.get(bias)=" + featureMapping.get("bias"));
//			System.exit(1);
//		}
//		if (opts.bias < 0 && featureMapping.get("bias") != null) {
//			// already add bias inside the feature vector;
//			System.out.println("ERROR: bias=" + opts.bias
//					+ ",featureMapping.get(bias)=" + featureMapping.get("bias"));
//			System.exit(1);
//		}
//	}

//	public void setFeatureWeights(double[] weights) {
//		featureWeights = weights;
//	}

//	public double[][] getFeatures() {
//		return featureValues;
//	}
//
//	public int[] getLabels() {
//		return labels;
//	}
//
//	public double[] getInstanceWeights() {
//		return instanceWeights;
//	}
//
//	public double[] getFeatureWeights() {
//		return featureWeights;
//	}

}
