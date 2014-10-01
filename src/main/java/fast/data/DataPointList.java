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
import fast.common.Bijection;
import fast.hmmfeatures.Opts;

//import org.apache.log4j.Logger;

public class DataPointList extends LinkedList<DataPoint> {

	private Opts opts;
	private static final long serialVersionUID = 4848754572225564741L;

	// private static Logger logger = Logger.getLogger("");
	private int student = -1, problemName = -1, stepName = -1, outcome = -1,
			fold = -1, skill = -1;
	private int feature = -1;
	private TreeMap<Integer, String> featureColumnToName;

	public final Bijection students, problems, steps, outcomes, skills;
	// this is for all features(including null) for one hmm (currently)
	public Bijection inputFeatures = new Bijection();
	// this is for per attempt for one hmm (currently), which should be the same
	// as overall
	public Bijection nonNullInputFeatures = new Bijection();
	// this is for all features(excluding null) for one hmm (currently)
	public Bijection finalFeatures = new Bijection();

	public final HashMap<Integer, Set<Integer>> cognitiveModel;
	private String filename;

	public DataPointList(ArrayList<String> instances, String problemColumn,
			String stepColumn, Bijection problems, Bijection steps,
			Bijection outcomes, Bijection trainFeatures, Opts opts) {
		this.opts = opts;
		this.students = new Bijection(); // Dont'save the index of students
		// TODO: in the future, if trained by stu, may need to change
		cognitiveModel = new HashMap<Integer, Set<Integer>>();
		skills = new Bijection();
		this.problems = problems;
		this.steps = steps;
		this.outcomes = outcomes;
		if (opts.obsClass1 == 0) {
			outcomes.put(opts.obsClass1Name);
			outcomes.put(opts.obsClass1Name.equals("correct") ? "incorrect"
					: "correct");
		}
		else {
			outcomes.put(opts.obsClass1Name.equals("correct") ? "incorrect"
					: "correct");
			outcomes.put(opts.obsClass1Name);
		}
		if (!opts.inputProvideFeatureColumns && !opts.nowInTrain) {
			finalFeatures = trainFeatures;
		}

		int lineNumber = 0;
		for (int ins = 0; ins < instances.size(); ins++) {
			// System.out.println("ins=" + ins);
			String columns[] = instances.get(ins).split("\\s*[,\t]+\\s*");
			if (lineNumber++ == 0) {
				// get Bijections input features (including null)
				inputFeatures = setColumnsGetFeatureBijection(columns, problemColumn,
						stepColumn);
				continue;
			}
			// System.out.println("lineNumber=" + lineNumber);

			int aStudent = students.put(columns[student]);
			int aOutcome = outcomes.get(columns[outcome]);
			int aFold, aProb, aStep;

			if (fold == -1)
				aFold = 1;
			else
				aFold = Integer.parseInt(columns[fold]);
			if (problemName == -1)
				aProb = problems.put("woof");
			else
				aProb = problems.put(columns[problemName]);
			if (stepName == -1)
				aStep = steps.put("meow");
			else
				aStep = steps.put(columns[stepName]);
			if (skill == -1)
				if (stepName == -1)
					cognitiveModel.put(aProb, new HashSet<Integer>());
				else
					cognitiveModel.put(aStep, new HashSet<Integer>());
			else {
				HashSet<Integer> s = new HashSet<Integer>();
				for (String aSkill : columns[skill].split("-")) {
					skills.put(aSkill.trim());
					s.add(skills.get(aSkill));
				}
				if (stepName == -1)
					cognitiveModel.put(aProb, s);
				else
					cognitiveModel.put(aStep, s);
			}

			if (opts.oneBiasFeature) {
				double[] finalFeatureValues = new double[1];
				finalFeatureValues[0] = 1.0;
				// if (finalFeatures == null)
				// finalFeatures = new Bijection();
				finalFeatures.put("bias");
				this.add(new DataPoint(aStudent, aProb, aStep, aFold, aOutcome,
						finalFeatureValues));
			}
			else if (opts.parameterizedEmit && featureColumnToName != null
					&& featureColumnToName.size() > 0) {
				ArrayList<Double> aFeatures_ = new ArrayList<Double>();
				nonNullInputFeatures = new Bijection();
				// int nbFeatures = 0;
				for (Map.Entry<Integer, String> iter : featureColumnToName.entrySet()) {
					// System.out.println("nbFeatures=" + nbFeatures++);
					int column = iter.getKey();
					String featureValue = columns[column];
					if (!featureValue.equals("NULL")) {
						String featureName = iter.getValue();
						// double value = 0.0;
						// if (!(featureName.contains("stufield")
						// ||featureName.contains("quefield") ))
						// value = Double.parseDouble(featureValue);
						aFeatures_.add(Double.parseDouble(featureValue));
						nonNullInputFeatures.put(featureName);
					}
				}
				if (nonNullInputFeatures.getSize() != aFeatures_.size()) {
					System.out
							.println("ERROR: nonNullInputFeatures.getSize() != aFeatures_.size()");
					System.exit(1);
				}

				if (opts.oneLogisticRegression) {
					double[][] finalFeatureValues = new double[opts.nbHiddenStates][];
					for (int hiddenStateIndex = 0; hiddenStateIndex < opts.nbHiddenStates; hiddenStateIndex++)
						finalFeatureValues[hiddenStateIndex] = expandFeatureVector(
								aFeatures_, hiddenStateIndex, nonNullInputFeatures,
								finalFeatures);
					this.add(new DataPoint(aStudent, aProb, aStep, aFold, aOutcome,
							finalFeatureValues));
				}
				else {// not oneLogisticRegression
					finalFeatures = nonNullInputFeatures;
					// should be the same as previous one
					// for (int index = 0; index < nonNullInputFeatures.getSize();
					// index++)
					// finalFeatures.put(nonNullInputFeatures.get(index));
					double[] finalFeatureValues = new double[aFeatures_.size()];
					if (opts.bias > 0) {
						finalFeatureValues = new double[aFeatures_.size() + 1];
					}
					int i = 0;
					for (; i < aFeatures_.size(); i++) {
						finalFeatureValues[i] = aFeatures_.get(i);
					}
					if (opts.bias > 0) {
						finalFeatureValues[i] = 1.0;
						finalFeatures.put("bias");
					}
					this.add(new DataPoint(aStudent, aProb, aStep, aFold, aOutcome,
							finalFeatureValues));
				}
				// aFeatures = new double[aFeatures_.size()];
				// for (int k = 0; k < aFeatures_.size(); k++)
				// aFeatures[k] = aFeatures_.get(k);
			}
			else {// not (opts.parameterizedEmit && featureColumnToName != null &&
						// featureColumnToName.size() > 0); not oneBiasFeature
				// finalFeatures = null;
				this.add(new DataPoint(aStudent, aProb, aStep, aFold, aOutcome));
			}
			// Here it only gets the full space of student or item dummies, later in
			// StudentList will reput the featureValues into DataPoint
			if (opts.nowInTrain && !opts.inputProvideFeatureColumns) {
				if (opts.addSharedStuDummyFeatures) {
					if (finalFeatures == null)
						finalFeatures = new Bijection();
					// TODO: may use "*features_"
					finalFeatures.put("*student" + columns[student]);
				}
				if (opts.addSharedItemDummyFeatures) {
					if (finalFeatures == null)
						finalFeatures = new Bijection();
					// TODO: may use "*features_"
					finalFeatures.put("*item" + columns[problemName]);
				}
				if (finalFeatures != null && finalFeatures.getSize() == 0)
					finalFeatures = null;
			}
			if (!opts.nowInTrain && !opts.inputProvideFeatureColumns) {
				if (!finalFeatures.contains("*student" + columns[student])) {
					opts.newStudents.add(columns[student]);
				}
				if (!finalFeatures.contains("*item" + columns[problemName])) {
					opts.newItems.add(columns[problemName]);
				}
			}
		}// per instance
		if (opts.nowInTrain && !opts.inputProvideFeatureColumns) {
			if (opts.bias > 0 && finalFeatures != null) {
				if (!opts.duplicatedBias)
					finalFeatures.put("*bias");
				else {
					finalFeatures.put("bias");
					finalFeatures.put("bias_hidden1");
				}
			}
		}
	}

	public double[] expandFeatureVector(ArrayList<Double> originalFeatureVector,
			int hiddenStateIndex, Bijection nonNullInputFeatures,
			Bijection finalFeatures) {

		// if (finalFeatures != null && finalFeatures.getSize() > 0) {
		// System.out.println("ERROR: finalFeatures.getSize()>0");
		// System.exit(1);
		// }

		if (!opts.oneLogisticRegression || opts.oneBiasFeature) {
			System.out
					.println("ERROR: !opts.oneLogisticRegression || opts.oneBiasFeature");
			System.exit(1);
		}

		ArrayList<Double> newFeatureVector = new ArrayList<Double>();
		for (int i = 0; i < originalFeatureVector.size(); i++) {
			if (!nonNullInputFeatures.get(i).startsWith("*")) {
				if (hiddenStateIndex == 0) {
					newFeatureVector.add(originalFeatureVector.get(i));
					newFeatureVector.add(0.0);
				}
				else {
					newFeatureVector.add(0.0);
					newFeatureVector.add(originalFeatureVector.get(i));
				}
				// if (finalFeatures == null) {
				// finalFeatures = new Bijection();
				// }
				// TODO: optimize
				finalFeatures.put(nonNullInputFeatures.get(i));
				finalFeatures.put(nonNullInputFeatures.get(i) + "_hidden1");
			}
			else {
				newFeatureVector.add(originalFeatureVector.get(i));
				// TODO: optimize
				// if (finalFeatures == null)
				// finalFeatures = new Bijection();
				finalFeatures.put(nonNullInputFeatures.get(i));
			}
		}
		if (opts.bias > 0) {
			if (!opts.duplicatedBias) {
				newFeatureVector.add(1.0);
				// if (finalFeatures == null)
				// finalFeatures = new Bijection();
				// TODO: optimize
				finalFeatures.put("*bias");
			}
			else {
				if (hiddenStateIndex == 0) {
					newFeatureVector.add(1.0);
					newFeatureVector.add(0.0);
				}
				else {
					newFeatureVector.add(0.0);
					newFeatureVector.add(1.0);
				}
				// TODO: optimize
				// if (finalFeatures == null) {
				// finalFeatures = new Bijection();
				// }
				finalFeatures.put("bias");
				finalFeatures.put("bias_hidden1");
			}
		}
		double[] newFeatureVector_ = new double[newFeatureVector.size()];
		for (int i = 0; i < newFeatureVector.size(); i++)
			newFeatureVector_[i] = newFeatureVector.get(i);

		return newFeatureVector_;
	}

	private Bijection setColumnsGetFeatureBijection(String[] columns,
			String problemColumn, String stepColumn) {
		inputFeatures = new Bijection();
		String ignoredColumns = "";
		ArrayList<Integer> ignoredColumnList = new ArrayList<Integer>();
		featureColumnToName = null;

		// Case-insensitive matching can also be enabled via the embedded flag
		// expression (?i).
		for (int i = 0; i < columns.length; i++) {
			if (columns[i].matches("(?i)student.*"))// (?i): ignore case sensitive
				student = i;
			else if (columns[i].matches("(?i)" + problemColumn))
				problemName = i;
			else if (columns[i].matches("(?i)" + stepColumn))
				stepName = i;
			else if (columns[i].matches("(?i)outcome"))
				outcome = i;
			else if (columns[i].matches("(?i)fold"))
				fold = i;
			else if (columns[i].matches("(?i)KC.*")
					|| columns[i].matches("(?i)skill.*"))
				skill = i;
			else if (columns[i].matches("(?i).*feature.*") && opts.parameterizedEmit
					&& !opts.oneBiasFeature) {
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

			// if (opts.parameterizedEmit && (opts.bias > 0 || opts.oneBiasFeature)) {
			// String featureName = "bias";
			// if (features == null)
			// features = new Bijection();
			// features.put(featureName);
			// }
		}

		if (ignoredColumns != "" && opts.verbose) {
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
				|| skill == -1
				|| (opts.parameterizedEmit
						&& !opts.oneBiasFeature
						&& (!opts.addSharedItemDummyFeatures && !opts.addSharedStuDummyFeatures) && (feature == -1 || featureColumnToName == null))) {// hy
			// logger.error("Cannot find column (student:" + student + ", problem:"
			// + problemName + ",step:" + stepName + ",outcome:" + outcome
			// + ",fold:" + fold + ",last feature:" + feature + ")"); // hy
			System.out.println("Cannot find column (student:" + student + ",outcome:"
					+ outcome + ",KC:" + skill + ")");
			// ("Cannot find column (student:" + student
			// + ", problem:" + problemName + ",step:" + stepName + ",outcome:"
			// + outcome + ",fold:" + fold + ",last feature:" + feature + ")");
			throw new RuntimeException("Missing column");
		}

		if (opts.verbose) {
			// logger.info("COLUMNS:\tstudent:" + student + ", problem:" + problemName
			// + ",step:" + stepName + ",outcome:" + outcome + ",last feature:"
			// + feature);// hy
			System.out.println("COLUMNS:\tstudent:" + student + ", problem:"
					+ problemName + ",step:" + stepName + ",outcome:" + outcome
					+ ",last feature:" + feature);
		}
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

	public String getFilename() {
		return filename;
	}

	public Bijection getSkills() {
		return skills;
	}

	// hy:
	public Bijection getFeatures() {
		return finalFeatures;
	}

	public HashMap<Integer, Set<Integer>> getCognitiveModel() {
		return cognitiveModel;
	}

}
