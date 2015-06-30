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
	private Bijection students, problems;
	private Bijection steps, outcomes, skills;
	private HashMap<Integer, Set<Integer>> cognitiveModel; // aProb/aStep to a set of skills
	/* Feature column index to feature name. Ordering is important.*/
	private TreeMap<Integer, String> featureColumnToName;
	
	private int student = -1, problemName = -1, stepName = -1, outcome = -1, fold = -1, skill = -1;
	private int feature = -1;
	private boolean generateStudentDummy = false, generateItemDummy = false;
	private static String delimiterStr = "\\s*[,\t]+\\s*";
	private boolean verbose;
	
	public DataPointList(ArrayList<String> instances, 
			boolean parameterizing, boolean parameterizedInit, boolean parameterizedTran, boolean parameterizedEmit, boolean forceUsingInputFeatures, 
			boolean bias, int nbHiddenStates){
		this(instances, parameterizing, parameterizedInit, parameterizedTran, parameterizedEmit,
				 forceUsingInputFeatures, bias, nbHiddenStates, null, null);
	}
	
	public DataPointList(ArrayList<String> instances, 
			boolean parameterizing, boolean parameterizedInit, boolean parameterizedTran, boolean parameterizedEmit, boolean forceUsingInputFeatures, 
			boolean bias, int nbHiddenStates, Bijection students, Bijection items){//, boolean generateStudentDummy, boolean generateItemDummy){
		this.parameterizing = parameterizing;
		this.parameterizedInit = parameterizedInit;
		this.parameterizedTran = parameterizedTran;
		this.parameterizedEmit = parameterizedEmit;
		this.forceUsingAllInputFeatures = forceUsingInputFeatures;
		//this.differentBias = differentBias;
		this.bias = bias;
		this.nbHiddenStates = nbHiddenStates;
//		this.generateStudentDummy = generateStudentDummy;
//		this.generateItemDummy = generateItemDummy;
		if (students != null && students.getSize() > 0)
			generateStudentDummy = true;
		else{
			generateStudentDummy = false;
			students = new Bijection();
		}
		if (items != null && items.getSize() > 0)
			generateItemDummy = true;
		else{
			generateItemDummy = false;
			items = new Bijection();
		}
		this.students = students; // Dont'save the index of students
		this.problems = items;
		
		// TODO: in the future, if trained by stu, may need to change
		cognitiveModel = new HashMap<Integer, Set<Integer>>();
		skills = new Bijection();
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
			
		inputFeatures = new Bijection();
		featureColumnToName = new TreeMap<Integer, String>();
		
		int lineNumber = 0;
		for (int ins = 0; ins < instances.size(); ins++) {
			// System.out.println("ins=" + ins);
			String columns[] = instances.get(ins).split(delimiterStr);
			if (lineNumber == 0) {
//				if (!(inputFeatures != null && inputFeatures.getSize() > 0 && featureColumnToName != null && featureColumnToName.size() > 0))
//					parseColumns(columns, inputFeatures, featureColumnToName);
					parseColumns(columns, inputFeatures, featureColumnToName);
					lineNumber++;
					continue;	
			}

			/* use integer to save memory. Need the Bijection to recover the original string */

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
			//System.out.println(aSkill);

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
				if ((featureColumnToName == null || featureColumnToName.size() <= 0) && !generateStudentDummy && !generateItemDummy)
					throw new RuntimeException("ERROR: I can't read any features. Please provide correct feature column names!");
				
				/* inputFeaturesNonNull: nonNull; no bias features; it can be "features_", "init_features_", "tran_features", "emit_features_" */
				ArrayList<Double> inputFeatureValuesNonNull = new ArrayList<Double>();
				// int nbFeatures = 0;
				if (featureColumnToName != null){
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
				}
				if (generateStudentDummy){
					for (int stuId = 0; stuId < students.getSize(); stuId++){
						Double featureValue = 0.0;
						String studentStr = students.get(stuId);
						if (studentStr.equals(columns[student]))
							featureValue = 1.0;
						inputFeatureValuesNonNull.add(featureValue);
						//TODO: differentiate init, tran, emit
						inputFeaturesNonNull.put("*feature_student_" + studentStr);
					}
				}
				if (generateItemDummy){
					for (int itemId = 0; itemId < problems.getSize(); itemId++){
						Double featureValue = 0.0;
						String itemStr = problems.get(itemId);
						if (itemStr.equals(columns[problemName]))
							featureValue = 1.0;
						inputFeatureValuesNonNull.add(featureValue);
						//TODO: differentiate init, tran, emit
						inputFeaturesNonNull.put("*feature_item_" + itemStr);
					}
				}
				
				/* Current line #non-null features should = all the lines #non-null features */
				if (inputFeaturesNonNull.getSize() != inputFeatureValuesNonNull.size())
					throw new RuntimeException("ERROR: inputFeaturesNonNull.getSize() != inputFeatureValuesNonNull.size(). Please make sure that the datapoints for the same HMM(KC) have the same NULL(NAN/empty) columns");
			
				// 1st: hiddenStates; 2nd: 0-init,1-tran,2-emit; 3rd:featureValues
				// corresponding to current hiddenState;
				double[][][] finalFeatureValues = new double[nbHiddenStates][3][];
				finalFeatureValues = getExpandedFeatureVector(inputFeatureValuesNonNull, inputFeaturesNonNull, finalFeatures, finalInitFeatures, finalTranFeatures, finalEmitFeatures);
				this.add(new DataPoint(aStudent, aSkill, aProb, aStep, aFold, aOutcome, finalFeatureValues));
			}
		}
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
	
	public static int[] parseColumns(String[] columns){
		//String columns[] = header.split("\\s*[,\t]+\\s*");
		int[] indexes = {-1, -1};
		for (int i = 0; i < columns.length; i++) {
			if (indexes[0] != -1 && indexes[1] != -1)
				break;
			if (columns[i].matches("(?i)student.*"))// (?i): ignore case sensitive
				indexes[0] = i;
			else if (columns[i].matches("(?i)problem.*"))
				indexes[1] = i;
		}
		return indexes;
	}

	public void parseColumns(String[] columns, Bijection inputFeatures, TreeMap<Integer, String> featureColumnToName) {
		//inputFeatures = new Bijection();
		String ignoredColumns = "";
		ArrayList<Integer> ignoredColumnList = new ArrayList<Integer>();
		//featureColumnToName = null;

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
					throw new RuntimeException("ERROR: featureColumnToName==null!");
					//featureColumnToName = new TreeMap<Integer, String>();
				featureColumnToName.put(i, featureName);
				// String featureName = columns[i].replace("features_", "");
				// featureName = columns[i].replace("feature_", "");
				if (inputFeatures == null)
					throw new RuntimeException("ERROR: inputFeatures==null!");
					//inputFeatures = new Bijection();
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

		if (student == -1 || outcome == -1 || skill == -1 || 
				(parameterizing && !generateStudentDummy && !generateItemDummy && (feature == -1 || featureColumnToName == null))) {
			String str = "ERROR: Missing column in input data. Cannot find column ";
			if (student == -1)
				str += "'student' ";
			if (outcome == -1)
				str += "'outcome' ";
			if (skill == -1)
				str += "'KC' ";
			if (parameterizing && !generateStudentDummy && !generateItemDummy && (feature == -1 || featureColumnToName == null))
				str += "for features ";
			str += "!";
			//System.out.println(str);
			throw new RuntimeException(str);
		}
		//return inputFeatures;
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
	
	public static void readStudentsOrItems(ArrayList<String> instances, Bijection students, Bijection items){
		boolean generateStudentDummy = false, generateItemDummy = false;
		if (students != null)
			generateStudentDummy = true;
		if (items != null)
			generateItemDummy = true;
		int lineNumber = 0;
		int[] indexes = {-1, -1}; //student, item
			for (int ins = 0; ins < instances.size(); ins++) {
				String columns[] = instances.get(ins).split(delimiterStr);
				if (lineNumber == 0) {
					indexes = parseColumns(columns);
					lineNumber++;
					continue;
				}
				int studentColumn = indexes[0];
				int itemColumn = indexes[1];
				if (generateStudentDummy){
					if (studentColumn == -1)
						throw new RuntimeException("ERROR: please provide \"student\" column so that I could generate student dummies for you.");
					else
						students.put(columns[studentColumn]);
				}
				if (generateItemDummy){
					if (itemColumn == -1)
						throw new RuntimeException("ERROR: please provide \"problem\" column so that I could generate item dummies for you.");
					else
						items.put(columns[itemColumn]);
				}
			}
	}
}
