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

package fast.data;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import fast.common.*;
//import fast.experimenter.Options;

/** This correspond to one HMM*/
public class DataPointList extends LinkedList<DataPoint> {// loading per HMM

	//private Options opts;h
	private static final long serialVersionUID = 4848754572225564741L;

	private final boolean parameterizing, parameterizedInit, parameterizedTran, parameterizedEmit;
	private final boolean forceUsingAllInputFeatures;
	//private final boolean differentBias;
	private final boolean bias;
	private final int nbHiddenStates;

	/* this is for all features including null for one hmm */
	private Bijection inputFeatures;
	/* this is for one hmm exclusing null, which should be the same as overall */
	private Bijection inputFeaturesNonNull;
	/* this is for one hmm all features excluding null; */
	private Bijection finalFeatures; // nonNull, can extend(add prefix) inputFeatures by init, tran and emit depending on configuration
	private Bijection finalInitFeatures;// nonNull for one HMM
	private Bijection finalTranFeatures;// nonNull for one HMM
	private Bijection finalEmitFeatures;// nonNull for one HMM
	private Bijection students, problems, steps, outcomes, skills;
	private HashMap<Integer, Set<Integer>> cognitiveModel; // aProb/aStep to a set of skills
	private TreeMap<Integer, String> featureColumnToName;
	
	private int student = -1, problemName = -1, stepName = -1, outcome = -1, fold = -1, skill = -1;
	private int feature = -1;
	private boolean verbose;
	
	public DataPointList(ArrayList<String> instances, 
			boolean parameterizing, boolean parameterizedInit, boolean parameterizedTran, boolean parameterizedEmit, boolean forceUsingInputFeatures, 
			boolean bias, int nbHiddenStates){
		this.parameterizing = parameterizing;
		this.parameterizedInit = parameterizedInit;
		this.parameterizedTran = parameterizedTran;
		this.parameterizedEmit = parameterizedEmit;
		this.forceUsingAllInputFeatures = forceUsingInputFeatures;
		//this.differentBias = differentBias;
		this.bias = bias;
		this.nbHiddenStates = nbHiddenStates;

		students = new Bijection(); // Dont'save the index of students
		// TODO: in the future, if trained by stu, may need to change
		cognitiveModel = new HashMap<Integer, Set<Integer>>();
		skills = new Bijection();
		problems = new Bijection();
		steps = new Bijection();
		outcomes = new Bijection();
		outcomes.put("incorrect");
		outcomes.put("correct");
		// if (!inputProvideFeatureColumns && !nowInTrain) {
		// finalAllFeatures = trainFeatures;
		// }

		inputFeaturesNonNull = new Bijection();
		finalFeatures = new Bijection(); // nonNull, can extend(add prefix) inputFeatures by init, tran and emit depending on configuration
		finalInitFeatures = new Bijection();// nonNull for one HMM
		finalTranFeatures = new Bijection();// nonNull for one HMM
		finalEmitFeatures = new Bijection();
		
		int lineNumber = 0;
		for (int ins = 0; ins < instances.size(); ins++) {
			// System.out.println("ins=" + ins);
			String columns[] = instances.get(ins).split("\\s*[,\t]+\\s*");
			if (lineNumber++ == 0) {
				// get Bijections input features (including null)
				inputFeatures = parseColumns(columns);
				continue;
			}

			int aStudent = students.put(columns[student]);
			Integer aOutcome = outcomes.get(columns[outcome]);
			if (aOutcome == null){
				System.out.println("ERROR: the code only support two observed states: correct or incorrect.");
				System.exit(-1);
			}
			int aFold, aProb, aStep, aSkill = -1;

			if (fold == -1)
				aFold = 1;
			else
				aFold = Integer.parseInt(columns[fold]);
			if (problemName == -1)
				aProb = problems.put("NULL");
			else
				aProb = problems.put(columns[problemName]);
			if (stepName == -1)
				aStep = steps.put("NULL");
			else
				aStep = steps.put(columns[stepName]);
			if (skill == -1) {
				if (stepName == -1)
					cognitiveModel.put(aProb, new HashSet<Integer>());
				else
					cognitiveModel.put(aStep, new HashSet<Integer>());
				aSkill = skills.put("");
			}
			else {
				HashSet<Integer> s = new HashSet<Integer>();
				//if (oneKcInKcColumn) {
				aSkill = skills.put(columns[skill]);
				s.add(aSkill);
				//}
//				else { // TODO: now only treat the kcs in kc_column as one kc; Now its
//								// not correct to put aSkill = skills.put... directly
//					for (String a : columns[skill].split("-")) {
//						aSkill = skills.put(a.trim());
//						s.add(aSkill);
//					}
				//}
				if (stepName == -1)
					cognitiveModel.put(aProb, s);
				else
					cognitiveModel.put(aStep, s);
			}

			// if (oneBiasFeature) {
			// double[][][] finalFeatureValues = new double[nbHiddenStates][3][];
			// finalFeatureValues[0] = 1.0;
			// finalAllFeatures.put("bias");
			// this.add(new DataPoint(aStudent, aProb, aStep, aFold, aOutcome,
			// finalFeatureValues));
			// }
			// else
			if (!parameterizing)
				this.add(new DataPoint(aStudent, aSkill, aProb, aStep, aFold, aOutcome));
			else {
				if (featureColumnToName == null || featureColumnToName.size() <= 0) {
					System.out.println("ERROR: please provide correct feature column names!");
					System.exit(-1);
				}
				/* inputFeaturesNonNull: nonNull; no bias features; it can be "features_", "init_features_", "tran_features", "emit_features_" */
				ArrayList<Double> inputFeatureValuesNonNull = new ArrayList<Double>();
				// int nbFeatures = 0;
				for (Map.Entry<Integer, String> iter : featureColumnToName.entrySet()) {
					// System.out.println("nbFeatures=" + nbFeatures++);
					int column = iter.getKey();
					String featureValue = columns[column];
					if (!featureValue.matches("(?i)null") && !featureValue.matches("(?i)nan") && !featureValue.equals("")) {//columns[i].matches("(?i).*feature.*")
						String featureName = iter.getValue();
						// double value = 0.0;
						// if (!(featureName.contains("stufield")
						// ||featureName.contains("quefield") ))
						// value = Double.parseDouble(featureValue);
						inputFeatureValuesNonNull.add(Double.parseDouble(featureValue));
						inputFeaturesNonNull.put(featureName);
					}
					//else
					//	outputFeatureCoefToEval = false;
				}
				if (inputFeaturesNonNull.getSize() != inputFeatureValuesNonNull.size()) {
					System.out.println("ERROR: inputFeaturesNonNull.getSize() != inputFeatureValuesNonNull.size(). Please make sure that the datapoints for the same HMM(KC) have the same NULL(NAN/empty) columns");
					System.exit(1);
				}
				// if (oneLogisticRegression) {
				// 1st: hiddenStates; 2nd: 0-init,1-tran,2-emit; 3rd:featureValues
				// corresponding to current hiddenState;
				double[][][] finalFeatureValues = new double[nbHiddenStates][3][];
				finalFeatureValues = getExpandedFeatureVector(inputFeatureValuesNonNull, inputFeaturesNonNull, finalFeatures, finalInitFeatures, finalTranFeatures, finalEmitFeatures);
				this.add(new DataPoint(aStudent, aSkill, aProb, aStep, aFold, aOutcome, finalFeatureValues));
			}
			// }
			// else {// not oneLogisticRegression
			// finalAllFeatures = inputFeaturesNonNull;
			// double[] finalFeatureValues = new double[nonNullFeatures.size()];
			// if (bias > 0) {
			// finalFeatureValues = new double[nonNullFeatures.size() + 1];
			// }
			// int i = 0;
			// for (; i < nonNullFeatures.size(); i++) {
			// finalFeatureValues[i] = nonNullFeatures.get(i);
			// }
			// if (bias > 0) {
			// finalFeatureValues[i] = 1.0;
			// finalAllFeatures.put("bias");
			// }
			// this.add(new DataPoint(aStudent, aProb, aStep, aFold, aOutcome,
			// finalFeatureValues));
			// }
			// aFeatures = new double[aFeatures_.size()];
			// for (int k = 0; k < aFeatures_.size(); k++)
			// aFeatures[k] = aFeatures_.get(k);

			/*
			 * // Here it only gets the full space of student or item dummies, later in // StudentList will reput the featureValues into DataPoint if (nowInTrain && !inputProvideFeatureColumns) { if (addSharedStuDummyFeatures) { if (finalFeatures == null) finalFeatures = new Bijection(); // TODO: may use "*features_" finalFeatures.put("*student" + columns[student]); } if (addSharedItemDummyFeatures) { if (finalFeatures == null) finalFeatures = new Bijection(); // TODO: may use "*features_" finalFeatures.put("*item" + columns[problemName]); } if (finalFeatures != null && finalFeatures.getSize() == 0) finalFeatures = null; } if (!nowInTrain && !inputProvideFeatureColumns) { if (!finalFeatures.contains("*student" + columns[student])) { newStudents.add(columns[student]); } if (!finalFeatures.contains("*item" + columns[problemName])) { newItems.add(columns[problemName]); } }
			 */
		}// per instance
		/*
		 * if (nowInTrain && !inputProvideFeatureColumns) { if (bias > 0 && finalFeatures != null) { if (!differentBias) finalFeatures.put("*bias"); else { finalFeatures.put("bias"); finalFeatures.put("bias_hidden1"); } } }
		 */
	}

	/**
	 * inputFeatureValuesNonNull(originalFeatureVector): nonNull; no bias features; it can be "(*)features_", "(*)features_init_", "(*)features_trans_", "(*)features_emit_"
	 * Return: 
	 *     finalXXXFeatures: final feature names after expansion and adding bias
	 *     newFeatureVector: 1st: hiddenStates; 2nd: 0-init,1-tran,2-emit; 3rd:featureValues corresponding to current hiddenState;
	 */
	public double[][][] getExpandedFeatureVector(ArrayList<Double> inputFeatureValuesNonNull, Bijection inputFeaturesNonNull, 
						Bijection finalAllFeatures, Bijection finalInitFeatures, Bijection finalTranFeatures, Bijection finalEmitFeatures) {

		// if (!oneLogisticRegression || oneBiasFeature) {
		// System.out
		// .println("ERROR: !oneLogisticRegression || oneBiasFeature");
		// System.exit(1);
		// }

		double[][][] newFeatureVector = new double[nbHiddenStates][3][];// hiddenStates;

		for (int hiddenStateIndex = 0; hiddenStateIndex < nbHiddenStates; hiddenStateIndex++) {
			ArrayList<Double> initFeatureVector = new ArrayList<Double>();
			ArrayList<Double> tranFeatureVector = new ArrayList<Double>();
			ArrayList<Double> emitFeatureVector = new ArrayList<Double>();
			for (int i = 0; i < inputFeatureValuesNonNull.size(); i++) {
				String featureName = inputFeaturesNonNull.get(i);
				double featureValue = inputFeatureValuesNonNull.get(i);
				/* For init features, it doesn't differentiate hidden states. */
				if ((featureName.startsWith("init_") || forceUsingAllInputFeatures) && parameterizedInit)//both hidden state has the same feature vector (same length same values)
					getFeatures(featureName, featureValue, initFeatureVector, finalInitFeatures, finalAllFeatures);
				if ((featureName.startsWith("tran_") || forceUsingAllInputFeatures) && parameterizedTran){
					getFeatures(hiddenStateIndex, featureName, featureValue, tranFeatureVector, finalTranFeatures, finalAllFeatures);
				}
				if ((featureName.startsWith("emit_") || forceUsingAllInputFeatures) && parameterizedEmit)//(!featureName.startsWith("init_") && !featureName.startsWith("tran_")
					getFeatures(hiddenStateIndex, featureName, featureValue, emitFeatureVector, finalEmitFeatures, finalAllFeatures);
			}
			if ((parameterizedInit && initFeatureVector.size() == 0) || (parameterizedTran && tranFeatureVector.size() == 0) || (parameterizedEmit && emitFeatureVector.size() == 0)) {
				if (parameterizedInit && initFeatureVector.size() == 0)
					System.out.println("ERROR: (parameterizedInit && initFeatures.size() == 0)");
				if (parameterizedTran && tranFeatureVector.size() == 0)
					System.out.println("ERROR: (parameterizedTran && tranFeatures.size() == 0)");
				if (parameterizedEmit && emitFeatureVector.size() == 0)
					System.out.println("ERROR: (parameterizedEmit && emitFeatures.size() == 0)");
				System.out.println("\tPlease recheck input format or configurtion! (e.g. If parameterizing initial, transition or emission probabilities, you should have feature column names with prefix 'init_', 'tran_' or 'emit_' such as 'tran_feature_XXX'; if the feature columns are named as 'feature_XXX', then please specify forceUsingInputFeature=true\n" 
													+ "\t#initFeatures=" + initFeatureVector.size() + ", #tranFeatures=" + tranFeatureVector.size() + ", #emitFeatures=" + emitFeatureVector.size());
				System.exit(1);
			}

			if (initFeatureVector.size() > 0) {
				if (bias)
					addBiasFeature(initFeatureVector, finalInitFeatures, finalAllFeatures);
				newFeatureVector[hiddenStateIndex][0] = new double[initFeatureVector.size()];//0: type="init"
				Utility.arrayListToArray(initFeatureVector, newFeatureVector[hiddenStateIndex][0]);
			}
			if (tranFeatureVector.size() > 0) {
				if (bias)
					addBiasFeature(hiddenStateIndex, tranFeatureVector, finalTranFeatures, finalAllFeatures);
				newFeatureVector[hiddenStateIndex][1] = new double[tranFeatureVector.size()];//1: type="tran"
				Utility.arrayListToArray(tranFeatureVector, newFeatureVector[hiddenStateIndex][1]);
			}
			if (emitFeatureVector.size() > 0) {
				if (bias)
					addBiasFeature(hiddenStateIndex, emitFeatureVector, finalEmitFeatures, finalAllFeatures);
				newFeatureVector[hiddenStateIndex][2] = new double[emitFeatureVector.size()];//2: type="emit"
				Utility.arrayListToArray(emitFeatureVector, newFeatureVector[hiddenStateIndex][2]);
			}
			
			if (parameterizedInit && (initFeatureVector.size() != finalInitFeatures.getSize())){
				System.out.println("ERROR: (parameterizedInit && initFeatureVectorSize(" +initFeatureVector.size() + ") != finalInitFeaturesSize(" + finalInitFeatures.getSize() + ").");
				System.exit(1);
			}
			if (parameterizedTran && (tranFeatureVector.size() != finalTranFeatures.getSize())){
				System.out.println("ERROR: (parameterizedTran && initFeatureVectorSize(" +tranFeatureVector.size() + ") != finalInitFeaturesSize(" + finalTranFeatures.getSize() + ").");
				System.exit(1);
			}
			if (parameterizedEmit && (emitFeatureVector.size() != finalEmitFeatures.getSize())){
				System.out.println("ERROR: (parameterizedEmit && emitFeatureVector(" +emitFeatureVector.size() + ") != finalInitFeaturesSize(" + finalEmitFeatures.getSize() + ").");
				System.exit(1);
			}
		}

		return newFeatureVector;
	}


	public void getFeatures(int hiddenStateIndex, String featureName, double featureValue, ArrayList<Double> featureVector, 
											Bijection aTypeFeatures, Bijection allFeatures) {

		if (featureName.contains("*feature")) {
//			System.out.println("WARNING: * in feature name is not completely ready yet!");
//			System.exit(-1);
			featureVector.add(featureValue);
			aTypeFeatures.put(featureName);
			allFeatures.put(featureName);
		}
		else {
			if (hiddenStateIndex == 0) {
				featureVector.add(featureValue);
				featureVector.add(0.0);
			}
			else {
				featureVector.add(0.0);
				featureVector.add(featureValue);
			}
			aTypeFeatures.put(featureName);
			aTypeFeatures.put(featureName + "_hidden1");
			allFeatures.put(featureName);
			allFeatures.put(featureName + "_hidden1");
		}
	}

	public void addBiasFeature(int hiddenStateIndex, ArrayList<Double> featureVector, Bijection aTypeFeatures, Bijection allFeatures) {

//		if (!differentBias) {
//			featureVector.add(1.0);
//			aTypeFeatures.put("bias");
//			allFeatures.put("bias");
//		}
//		else {
		if (hiddenStateIndex == 0) {
			featureVector.add(1.0);
			featureVector.add(0.0);
		}
		else {
			featureVector.add(0.0);
			featureVector.add(1.0);
		}
		aTypeFeatures.put("bias");
		aTypeFeatures.put("bias_hidden1");
		allFeatures.put("bias");
		allFeatures.put("bias_hidden1");
		//}
	}

	public void addBiasFeature(ArrayList<Double> featureVector, Bijection aTypeFeatures, Bijection allFeatures) {
		featureVector.add(1.0);
		aTypeFeatures.put("bias");
		allFeatures.put("bias");
	}

	// here is for getting features without differentiating hiddenstates (for
	// init)
	public void getFeatures(String featureName, double featureValue, ArrayList<Double> featureVector, 
													Bijection aTypeFeatures, Bijection allFeatures) {
		featureVector.add(featureValue);
		aTypeFeatures.put(featureName);
		allFeatures.put(featureName);
	}

	public Bijection parseColumns(String[] columns) {
		inputFeatures = new Bijection();
		String ignoredColumns = "";
		ArrayList<Integer> ignoredColumnList = new ArrayList<Integer>();
		featureColumnToName = null;

		// Case-insensitive matching can also be enabled via the embedded flag
		// expression (?i).
		for (int i = 0; i < columns.length; i++) {
			if (columns[i].matches("(?i)student.*"))// (?i): ignore case sensitive
				student = i;
			else if (columns[i].matches("(?i)problem.*"))
				problemName = i;
			else if (columns[i].matches("(?i)step.*"))
				stepName = i;
			else if (columns[i].matches("(?i)outcome.*"))
				outcome = i;
			else if (columns[i].matches("(?i)fold.*"))
				fold = i;
			else if (columns[i].matches("(?i)kc.*") || columns[i].matches("(?i)skill.*"))// (?i): ignore case sensitive
				skill = i;
			else if (columns[i].matches("(?i).*feature.*") && parameterizing) {
				// && !oneBiasFeature) {
				// [TODO]currently, use specific name "feature_" for geting all
				// features, didn't differentiate features for emit or transition
				feature = i;// starting index
				String featureName = columns[i];
				if (featureColumnToName == null)
					featureColumnToName = new TreeMap<Integer, String>();
				featureColumnToName.put(i, featureName);
				// String featureName = columns[i].replace("features_", "");
				// featureName = columns[i].replace("feature_", "");
				if (inputFeatures == null)
					inputFeatures = new Bijection();
				inputFeatures.put(featureName);
			}
			else {
				ignoredColumns += " " + columns[i];
				ignoredColumnList.add(i);
			}

			// if (parameterizedEmit && (bias > 0 || oneBiasFeature)) {
			// String featureName = "bias";
			// if (features == null)
			// features = new Bijection();
			// features.put(featureName);
			// }
		}

		if (ignoredColumns != "" && verbose) {
			// logger.info("Ignored Column(s):\t" + ignoredColumns);
			System.out.println("Ignored Column(s):\t" + ignoredColumns);
			// logger.info("#Ignored Column(s):\t" + ignoredColumnList.size());
			System.out.println("#Ignored Column(s):\t" + ignoredColumnList.size());
		}

		if (student == -1
		// || (problemName == -1 && !problemColumn.equalsIgnoreCase(""))
		// hy: to change: problemColum?
		// || (stepName == -1 && !stepColumn.equalsIgnoreCase(""))
				|| outcome == -1
				// || fold == -1
				|| skill == -1 || (parameterizing
				// && !oneBiasFeature
				&& (feature == -1 || featureColumnToName == null))) {
			// && (!addSharedItemDummyFeatures &&
			// !addSharedStuDummyFeatures) )) {// hy
			// logger.error("Cannot find column (student:" + student + ", problem:"
			// + problemName + ",step:" + stepName + ",outcome:" + outcome
			// + ",fold:" + fold + ",last feature:" + feature + ")"); // hy
			System.out.println("Cannot find column (student:" + student + ",outcome:" + outcome + ",KC:" + skill + ")");
			// ("Cannot find column (student:" + student
			// + ", problem:" + problemName + ",step:" + stepName + ",outcome:"
			// + outcome + ",fold:" + fold + ",last feature:" + feature + ")");
			throw new RuntimeException("Missing column");
		}

//		if (verbose) {
//			// logger.info("COLUMNS:\tstudent:" + student + ", problem:" + problemName
//			// + ",step:" + stepName + ",outcome:" + outcome + ",last feature:"
//			// + feature);// hy
//			System.out.println("COLUMNS:\tstudent:" + student + ", problem:" + problemName + ",step:" + stepName + ",outcome:" + outcome + ",last feature:" + feature);
//		}
		return inputFeatures;
	}

	public Bijection getStudents() {
		return students;
	}

	public Bijection getProblems() {
		return problems;
	}

	public Bijection getSteps() {
		return steps;
	}

	public Bijection getOutcomes() {
		return outcomes;
	}

//	public String getFilename() {
//		return filename;
//	}

	public Bijection getSkills() {
		return skills;
	}

	public Bijection getAllFeatures() {
		return finalFeatures;
	}

	public Bijection getInitFeatures() {
		return finalInitFeatures;
	}

	public Bijection getTranFeatures() {
		return finalTranFeatures;
	}

	public Bijection getEmitFeatures() {
		return finalEmitFeatures;
	}

	public HashMap<Integer, Set<Integer>> getCognitiveModel() {
		return cognitiveModel;
	}

}
