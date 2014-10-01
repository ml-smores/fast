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

package fast.hmmfeatures;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import fast.common.Bijection;

public class LogisticRegression {

	private Opts opts;
	private final double[][] features;
	private final int[] labels;
	private double[] instanceWeights;
	private double[] classWeights;
	private final Bijection featureMapping;
	private final Bijection labelMapping;
	private double[] featureWeights;
	private OpdfContextAwareLogisticRegression opdf;
	private double[] expectedCounts;

	public LogisticRegression(double[][] featureValues, int[] outcomes,
			double[] weights, Bijection featureMapping, Bijection outcomeMapping,
			Opts opts, OpdfContextAwareLogisticRegression opdf) {
		this.opts = opts;
		this.opdf = new OpdfContextAwareLogisticRegression(opdf);

		if (featureValues.length != outcomes.length) {
			System.out.println("featureValues.length != outcomes.length!");
			System.exit(1);
		}
		if (featureValues[0].length != opdf.featureWeights.length) {
			System.out
					.println("featureValues[0].length != opdf.featureWeights.length");
			System.exit(1);
		}

		if (opts.useClassWeightToTrainParamerizedEmit
				&& opts.useInstanceWeightToTrainParamterezdEmit) {
			System.out
					.println("ERROR: opts.useClassWeightToTrainParamerizedEmit && opts.useInstanceWeightToTrainParamterezdEmit!");
			System.exit(1);
		}
		if (weights == null || weights.length != outcomes.length) {
			System.out
					.println("ERROR: instance/class weights are null || weights.length != outcomes.length");
			System.exit(1);
		}

		if (!opts.shareAddress) {
			if (opts.useInstanceWeightToTrainParamterezdEmit) {
				this.instanceWeights = new double[weights.length];
				for (int i = 0; i < weights.length; i++) {
					this.instanceWeights[i] = weights[i];
				}
			}
			else if (opts.useClassWeightToTrainParamerizedEmit) {
				this.classWeights = new double[weights.length];
				for (int i = 0; i < weights.length; i++) {
					this.classWeights[i] = weights[i];
				}
			}
			this.features = new double[featureValues.length][featureValues[0].length];
			for (int i = 0; i < featureValues.length; i++) {
				for (int j = 0; j < featureValues[0].length; j++) {
					this.features[i][j] = featureValues[i][j];
				}
			}
			this.labels = new int[outcomes.length];
			for (int i = 0; i < outcomes.length; i++) {
				this.labels[i] = outcomes[i];
			}
			this.featureMapping = new Bijection(featureMapping);
			this.labelMapping = new Bijection(outcomeMapping);

		}
		else {
			if (opts.useInstanceWeightToTrainParamterezdEmit)
				this.instanceWeights = weights;
			else if (opts.useClassWeightToTrainParamerizedEmit)
				this.classWeights = weights;
			this.features = featureValues;
			this.labels = outcomes;
			this.featureMapping = featureMapping;
			this.labelMapping = outcomeMapping;
		}

		expectedCounts = null;
		if (opts.useInstanceWeightToTrainParamterezdEmit)
			expectedCounts = instanceWeights;
		else if (opts.useClassWeightToTrainParamerizedEmit)
			expectedCounts = classWeights;
		larger(expectedCounts, opts.INSTANCE_WEIGHT_MULTIPLIER);

		this.featureWeights = new double[featureMapping.getSize()];
		double[] previousFeatureWeights = opdf.featureWeights;
		for (int f = 0; f < featureWeights.length; f++) {
			featureWeights[f] = previousFeatureWeights[f];
		}

		// checkBiasFeature(opts);

	}

	public double[] train() {

		// && opts.currentKc.equals(opts.skillToTest)) {
		if (opts.testLiblinear || opts.testLogsticRegression
				|| opts.generateLRInputs)
			try {
				generateTrainLROutSideFile();// expectedCounts, features, labels);
				return featureWeights;

			}
			catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

		if (opts.LBFGS)
			featureWeights = useLBFGS(features, featureWeights, labels,
					expectedCounts);
		else
			throw new RuntimeException("We only support LBFGS");

		if (featureWeights.length != featureMapping.getSize()
				|| featureWeights.length != features[0].length) {
			System.out
					.println("featureWeights.length != featureMapping.getSize() || featureWeights.length != features[0].length!");
			System.exit(1);
		}
		return featureWeights;
	}

	public double[] useLBFGS(double[][] features, double[] initialFeatureWeights,
			int[] outcomes, double[] expectedCounts) {
		LBFGS LBFGSTrain = new LBFGS(features, initialFeatureWeights,
				expectedCounts, outcomes, opdf, opts);
		featureWeights = LBFGSTrain.run();
		return featureWeights;
	}

	public void swap(double[] targets, int i, int j) {
		double temp = targets[i];
		targets[i] = targets[j];
		targets[j] = temp;
	}

	public void swap(double[][] features, int i, int j) {
		double[] temp = features[i];
		features[i] = features[j];
		features[j] = temp;
	}

	public void swap(ArrayList<String> dataStrs, int i, int j) {
		String temp = dataStrs.get(i);
		dataStrs.set(i, dataStrs.get(j));
		dataStrs.set(j, temp);
	}

	public void checkBiasFeature(Opts opts) {
		// double addBias = -1.0;
		if (opts.bias >= 0 && featureMapping.get("bias") == null) {
			System.out
					.println("ERROR: Requiring bias feature inside the datapoint! bias="
							+ opts.bias + ",onebiasfeature=" + opts.oneBiasFeature
							+ ",featureMapping.get(bias)=" + featureMapping.get("bias"));
			System.exit(1);
		}
		if (opts.bias < 0 && featureMapping.get("bias") != null) {
			// already add bias inside the feature vector;
			System.out.println("ERROR: bias=" + opts.bias + ",onebiasfeature="
					+ opts.oneBiasFeature + ",featureMapping.get(bias)="
					+ featureMapping.get("bias"));
			System.exit(1);
		}

		// if ((opts.bias >= 0 || opts.oneBiasFeature)
		// && featureMapping.get("bias") != null) {
		// // already add bias inside the feature vector;
		// addBias = -1.0;
		// }
		// return addBias;
	}

	private void larger(double[] array, double multiplier) {
		if (opts.verbose)
			System.out.println("Instance Weight Mulitply by " + multiplier);
		for (int i = 0; i < array.length; i++)
			array[i] *= multiplier;

	}

	@SuppressWarnings("unused")
	private double scale(double[] array) {
		double sum = 0.;

		for (int i = 0; i < array.length; i++)
			sum += array[i];

		for (int i = 0; i < array.length; i++)
			array[i] /= sum;
		return sum;
	}

	public double[] intToDoubleArray(int[] labels) {
		double[] targets = new double[labels.length];
		for (int i = 0; i < labels.length; i++) {
			targets[i] = labels[i];
		}
		return targets;
	}

	public void generateTrainLROutSideFile() throws Exception {
		// double[] expectedCounts_,
		// double[][] featureValues_, int[] outcomes_) throws Exception {
		// System.out.println("generateTestLROutSideFile...");
		// double[] expectedCounts = null;
		// if (!(opts.generateLRInputs || opts.shareAddress)) {
		// expectedCounts = new double[expectedCounts_.length];
		// for (int i = 0; i < expectedCounts_.length; i++) {
		// expectedCounts[i] = expectedCounts_[i];
		// }
		//
		// double[][] features = new
		// double[featureValues_.length][featureValues_[0].length];
		// for (int i = 0; i < featureValues_.length; i++) {
		// for (int j = 0; j < featureValues_[0].length; j++) {
		// features[i][j] = featureValues_[i][j];
		// }
		// }
		// int[] labels = new int[outcomes_.length];
		// for (int i = 0; i < outcomes_.length; i++) {
		// labels[i] = outcomes_[i];
		// }
		// }

		String wgtStr = "";
		String generalLRFeatureStr = "";
		String generalLRLabelStr = "";
		String liblinearDataStr = "";
		ArrayList<String> wekaDataStrs = new ArrayList<String>();
		String wekaDataStr = "";
		// boolean noNeedWeka = false;
		if (opts.generateLRInputs) {
			opts.testByWekaInputDataFile = opts.outDir
					+ (opts.nowInTrain ? opts.curFoldRunTrainInFilePrefix
							: opts.curFoldRunTestInFilePrefix) + "_" + opts.currentKc
					+ ".csv";
			opts.testByWekaInputDataWriter = new BufferedWriter(new FileWriter(
					opts.testByWekaInputDataFile));
		}
		boolean writeHeader = false;

		for (int ins = 0; ins < labels.length; ins++) {
			if (ins % 1000 == 0)
				System.out.println("generateTrainLROutSideFile: ins=" + ins);
			if (opts.testLiblinear) {
				if (opts.useInstanceWeightToTrainParamterezdEmit)
					wgtStr += expectedCounts[ins] + "\n";
				if (opts.testLogsticRegression)
					generalLRLabelStr += labels[ins] + "\n";
				if (opts.testLiblinear)
					liblinearDataStr += (labels[ins] == 0.0 ? "-1" : "+1") + " ";
			}
			double[] curInsFeatures = features[ins];
			for (int f = 0; f < curInsFeatures.length; f++) {
				double aFeature = curInsFeatures[f];
				if (opts.generateLRInputs)
					wekaDataStr += aFeature + ",";
				else if (opts.testLiblinear) {
					if (aFeature != 0.0)
						liblinearDataStr += (f + 1) + ":" + aFeature
								+ ((f == curInsFeatures.length - 1) ? "\n" : " ");
				}
				else if (opts.testLogsticRegression)
					generalLRFeatureStr += aFeature
							+ ((f == curInsFeatures.length - 1) ? "\n" : "\t");
			}
			if (opts.generateLRInputs)
				wekaDataStr += (labels[ins] == 0.0 ? "incorrect" : "correct");
			try {
				if (opts.generateLRInputs) {
					if (opts.swapData)
						wekaDataStrs.add(wekaDataStr);
					else {
						String header = "";
						if (!writeHeader) {
							for (int f = 0; f < featureMapping.getSize(); f++)
								header += featureMapping.get(f) + ",";
							header += "label";
							opts.testByWekaInputDataWriter.write(header + "\n");
							writeHeader = true;
						}
						opts.testByWekaInputDataWriter.write(wekaDataStr + "\n");
						opts.testByWekaInputDataWriter.flush();
					}
				}
				else if (opts.testLiblinear) {
					if (opts.useInstanceWeightToTrainParamterezdEmit)
						opts.testLRInstanceWeightsWriter.write(wgtStr);
					opts.liblinearInputDataWriter.write(liblinearDataStr);
					opts.testLRInstanceWeightsWriter.flush();
					opts.liblinearInputDataWriter.flush();
				}
				else if (opts.testLogsticRegression) {
					opts.testLRInstanceWeightsWriter.write(wgtStr);
					opts.testLRFeatureWriter.write(generalLRFeatureStr);
					opts.testLRLabelWriter.write(generalLRLabelStr);
					opts.testLRInstanceWeightsWriter.flush();
					opts.testLRFeatureWriter.flush();
					opts.testLRLabelWriter.flush();
				}
			}
			catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			wgtStr = "";
			generalLRFeatureStr = "";
			generalLRLabelStr = "";
			liblinearDataStr = "";
			wekaDataStr = "";
		}
		if (opts.generateLRInputs) {
			if (opts.swapData) {
				// double[] targets = intToDoubleArray(labels);
				// swap targets to make 1 ("correct") appears first, so that Cp(which
				// corresponds to prob.y>0 and weighted_C[0](which corresponds to the
				// first
				// appearing label)) can always correspond to 1 ("correct"))
				if (labels[0] != opts.obsClass1) {
					for (int k = 1; k < labels.length; k++)
						if (labels[k] == opts.obsClass1) {
							// targets[k] == opts.obsClass1
							swap(wekaDataStrs, 0, k);
							// System.out.println("swap");
							break;
						}
				}
				String header = "";
				for (int f = 0; f < featureMapping.getSize(); f++)
					header += featureMapping.get(f) + ",";
				header += "label";
				opts.testByWekaInputDataWriter.write(header + "\n");
				for (int ins = 0; ins < wekaDataStrs.size(); ins++) {
					// if (ins % 1000 == 0)
					System.out.println("ins=" + ins);
					opts.testByWekaInputDataWriter.write(wekaDataStrs.get(ins) + "\n");
					opts.testByWekaInputDataWriter.flush();
				}
			}
			opts.testByWekaInputDataWriter.close();
		}
	}

	// /**
	// *
	// * @param features
	// * index f1 f2 f3 || 0 1 2.5 3 || 1 0 1.3 4
	// * @return double[][] index content || 0 1:1 2:2.5 3:3 || 1 2:1.3 3:4
	// *
	// */
	// public double[][] transformToSparseFeatureFormat(double[][] features) {
	// double[][] sparseFeatures = new double[features.length][];
	//
	// return null;
	// }

	public void setFeatureWeights(double[] weights) {
		featureWeights = weights;
	}

	public double[][] getFeatures() {
		return features;
	}

	public int[] getLabels() {
		return labels;
	}

	public double[] getInstanceWeights() {
		return instanceWeights;
	}

	public double[] getFeatureWeights() {
		return featureWeights;
	}

	public void printInstanceWeights() {
		System.out.println("Instance Weights:");
		for (int i = 0; i < instanceWeights.length; i++)
			System.out.print(instanceWeights[i] + "\t");
		System.out.println();
	}

	public void printFeatureWeights() {
		System.out.println("Feature Weights:");
		for (int i = 0; i < featureWeights.length; i++)
			System.out.print(featureWeights[i] + "\t");
		System.out.println();
	}

	// change to toString()
	public void printFeatureMapping() {
		System.out.println(featureMapping);
	}

	public void printLabelMapping() {
		System.out.println(labelMapping);
	}

}
