package data;

import fast.hmmfeatures.Opts;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Set;

import common.Bijection;

public class StudentList extends LinkedList<CVStudent> {

	private static final long serialVersionUID = 730176924591642451L;

	// private static Logger logger = Logger.getLogger("");

	private final Bijection students, problems, steps, outcomes, skills,
			finalStudents;
	private Bijection features = null;;
	private final String filename;
	private final HashMap<Integer, Set<Integer>> cm;
	public final int numberOfObservations;
	public static Opts opts;
	public static Bijection trainFeatures = null;

	public StudentList(DataPointList data, Opts opts_) {
		super();
		this.opts = opts_;
		this.filename = data.getFilename();
		finalStudents = new Bijection();
		this.students = data.getStudents();
		this.problems = data.getProblems();
		this.steps = data.getSteps();
		this.outcomes = data.getOutcomes();
		this.skills = data.getSkills();
		this.cm = data.getCognitiveModel();
		this.features = data.getFeatures();// get finalFeatures()
		this.numberOfObservations = data.size();
		Iterator<DataPoint> iDatum = data.iterator();
		int previousStudent = -1, previousProblem = -1, previousStep = -1;
		String previousStudentStr = "";
		double[][] previousFeatures_ = null;
		double[] previousFeatures = null;
		int notPutStudent = -1;
		ArrayList<String> seqLength1Stu = new ArrayList<String>();
		int nbLines = 1;

		while (iDatum.hasNext()) {
			// System.out.println("nbLines=" + nbLines);
			DataPoint datum = iDatum.next();
			int aStudent = datum.getStudent();
			int aProblem = datum.getProblem();
			int aStep = datum.getStep();
			if (datum.getFold() == 1)
				nbLines++;
			// just for determining a record is repeated or not
			// for oneLR
			double[][] aFeatures_ = new double[this.opts.nbHiddenStates][];
			double[] aFeatures = null;
			if (opts.oneLogisticRegression) {
				for (int h = 0; h < this.opts.nbHiddenStates; h++) {
					aFeatures_[h] = datum.getFeatures(h);
				}
				// TODO: another design: make Bijection's key sorted by integer, and
				// then
				// access by integer
				// currently, add them in a "shared" way
				if (opts.addSharedItemDummyFeatures || opts.addSharedStuDummyFeatures) {
					if (aFeatures_[0] == null) {
						aFeatures_ = new double[this.opts.nbHiddenStates][features
								.getSize()];
						for (int featureIndex = 0; featureIndex < features.keys().size(); featureIndex++) {
							String aStudent_ = students.get(aStudent);
							String aProblem_ = problems.get(aProblem);
							// currently, only supports share features
							if (features.get(featureIndex).equals("*student" + aStudent_)) {
								aFeatures_[0][featureIndex] = 1.0;
								aFeatures_[1][featureIndex] = 1.0;
							}
							else if (features.get(featureIndex).equals("*item" + aProblem_)) {
								aFeatures_[0][featureIndex] = 1.0;
								aFeatures_[1][featureIndex] = 1.0;
							}
							else if (features.get(featureIndex).equals("bias")) {
								aFeatures_[0][featureIndex] = 1.0;
								aFeatures_[1][featureIndex] = 0.0;
							}
							else if (features.get(featureIndex).equals("bias_hidden1")) {
								aFeatures_[0][featureIndex] = 0.0;
								aFeatures_[1][featureIndex] = 1.0;
							}
							else {
								aFeatures_[0][featureIndex] = 0.0;
								aFeatures_[1][featureIndex] = 0.0;
							}
						}
						datum.setExpandedFeatures(aFeatures_);
					}
					else {
						System.out
								.println("WARNING: not supporting adding dummies to existed features yet ;0");
						System.exit(1);
					}
				}
			}
			else {// two LR:
				// System.out
				// .println("WARNING: Haven't checked whether this make sense or not yet!");
				// System.exit(1);
				aFeatures = datum.getFeatures(0);// 0 and 1 use the same one
				if (opts.addSharedItemDummyFeatures || opts.addSharedStuDummyFeatures) {
					if (aFeatures == null) {
						aFeatures = new double[features.getSize()];
						for (int featureIndex = 0; featureIndex < features.keys().size(); featureIndex++) {
							String aStudent_ = students.get(aStudent);
							String aProblem_ = problems.get(aProblem);
							if (features.get(featureIndex).equals("*student" + aStudent_))
								aFeatures[featureIndex] = 1.0;
							else if (features.get(featureIndex).equals("*item" + aProblem_))
								aFeatures[featureIndex] = 1.0;
							else if (features.get(featureIndex).equals("bias"))
								aFeatures[featureIndex] = 1.0;
							else
								aFeatures[featureIndex] = 0.0;
						}
						datum.setFeatures(aFeatures);
					}
					else {
						System.out
								.println("WARNING: not supporting adding dummies to existed features yet ;0");
						System.exit(1);
					}
				}
			}

			// hy: "this" keep track of the LinkedList
			CVStudent student;
			// if (previousStudent == 12)
			// System.out.println();
			if (previousStudent != aStudent) {
				// hy: remove the student's sequence which has just length 1
				if (previousStudent != -1) {
					CVStudent preStudent = this.getLast();
					if (preStudent.size() == 1) {
						String stu = students.get(previousStudent);// students.get(preStudent.get(0).getStudent());
						// System.out.println(students.get(previousStudent));
						if (opts.removeSeqLength1InTrain && opts.nowInTrain
								|| (opts.removeSeqLength1InTest && !opts.nowInTrain)) {
							// if (opts.verbose) {
							// logger.error("Removing seq length=1 for student:" + stu);
							System.out.println("Removing seq length=1 for student:" + stu);
							// }
							notPutStudent = previousStudent;
							this.removeLast();
						}
						else {
							// if (opts.verbose) {
							// logger.warn("Seqlength=1 for student:" + stu);
							System.out.println("Seqlength=1 for student:" + stu);
							// }
							finalStudents.put(previousStudent + "");
						}
						seqLength1Stu.add(stu);
					}
					else if (preStudent.size() == 2) {
						int fold = preStudent.get(0).getFold();
						if (fold == -1) {
							String stu = students.get(preStudent.get(0).getStudent());
							if (opts.removeSeqLength1InTrain && opts.nowInTrain
									|| (opts.removeSeqLength1InTest && !opts.nowInTrain)) {
								// if (opts.verbose) {
								// logger.error("Removing seq length=1 for student:" + stu);
								System.out.println("Removing seq length=1 for student:" + stu);
								// }
								notPutStudent = previousStudent;
								this.removeLast();
							}
							else {
								// if (opts.verbose) {
								// logger.warn("Seqlength=1 for student:" + stu);
								System.out.println("Seqlength=1 for student:" + stu);
								// }
								finalStudents.put(previousStudent + "");
							}
							seqLength1Stu.add(stu);
						}
					}
					if (previousStudent != notPutStudent)
						finalStudents.put(previousStudent + "");
				}
				// finalStudents.put(previousStudent + "");
				student = new CVStudent(datum.getFold());
				this.add(student);
			}
			else {
				student = this.getLast();
			}
			student.add(datum);

			if (// reportError&&
			(previousStudent == aStudent)
					&& (previousProblem == aProblem)
					&& (previousStep == aStep)
					&& ((opts.oneLogisticRegression && (previousFeatures != null
							&& aFeatures != null && Arrays
								.equals(previousFeatures, aFeatures))) || (!opts.oneLogisticRegression && (previousFeatures_ != null
							&& aFeatures_ != null && Arrays.equals(previousFeatures_,
							aFeatures_))))) {
				// logger.warn("Repeated datum, student= "
				// + data.getStudents().get(aStudent) + " problem: "
				// + data.getProblems().get(aProblem) + " step: "
				// + data.getSteps().get(aStep));// hy:[TODO]add features
				// reportError = false;
				System.out.println("Repeated datum, student= "
						+ data.getStudents().get(aStudent) + " problem="
						+ data.getProblems().get(aProblem) + " step="
						+ data.getSteps().get(aStep) + "or features are repeated!");
			}

			previousStudent = aStudent;
			previousProblem = aProblem;
			previousStep = aStep;
			if (opts.oneLogisticRegression)
				previousFeatures_ = aFeatures_;
			else
				previousFeatures = aFeatures;
		}
		if (previousStudent != -1) {
			CVStudent preStudent = this.getLast();
			if (preStudent.size() == 1) {
				String stu = students.get(previousStudent);// students.get(preStudent.get(0).getStudent());
				// System.out.println(students.get(previousStudent));
				if (opts.removeSeqLength1InTrain && opts.nowInTrain
						|| (opts.removeSeqLength1InTest && !opts.nowInTrain)) {
					// if (opts.verbose) {
					// logger.error("Removing seq length=1 for student:" + stu);
					System.out.println("Removing seq length=1 for student:" + stu);
					// }
					notPutStudent = previousStudent;
					this.removeLast();
				}
				else {
					// if (opts.verbose) {
					// logger.warn("Seqlength=1 for student:" + stu);
					System.out.println("Seqlength=1 for student:" + stu);
					// }
					finalStudents.put(previousStudent + "");
				}
				seqLength1Stu.add(stu);
			}
			else if (preStudent.size() == 2) {
				int fold = preStudent.get(0).getFold();
				if (fold == -1) {
					String stu = students.get(preStudent.get(0).getStudent());
					if (opts.removeSeqLength1InTrain && opts.nowInTrain
							|| (opts.removeSeqLength1InTest && !opts.nowInTrain)) {
						// if (opts.verbose) {
						// logger.error("Removing seq length=1 for student:" + stu);
						System.out.println("Removing seq length=1 for student:" + stu);
						// }
						notPutStudent = previousStudent;
						this.removeLast();
					}
					else {
						// if (opts.verbose) {
						// logger.warn("Seqlength=1 for student:" + stu);
						System.out.println("Seqlength=1 for student:" + stu);
						// }
						finalStudents.put(previousStudent + "");
					}
					seqLength1Stu.add(stu);
				}
			}
			if (previousStudent != notPutStudent)
				finalStudents.put(previousStudent + "");
		}
		// logger.warn("#sequence length 1 students:" + seqLength1Stu.size());
		System.out.println("#sequence length 1 stus:" + seqLength1Stu.size()
				+ ", #finalStus=" + finalStudents.getSize() + ", #oriStus="
				+ students.getSize());
		seqLength1Stu = new ArrayList<String>();
	}

	public StudentList(ArrayList<String> training, String problemColumn,
			String stepsColumn, Bijection problems, Bijection steps,
			Bijection outcomes, Opts opts) {// boolean parameterizedEmit, int
																			// obsClass1,
		// String obsClass1Name) {
		this(new DataPointList(training, problemColumn, stepsColumn, problems,
				steps, outcomes, trainFeatures, opts), opts);
	}

	public Bijection getFinalStudents() {
		return finalStudents;// students;
	}

	public Bijection getOriStudents() {
		return students;// students;
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

	public Bijection getExpertSkills() {
		return skills;
	}

	public HashMap<Integer, Set<Integer>> getExpertCM() {
		return cm;
	}

	public void shuffleTime() {
		for (CVStudent student : this)
			Collections.shuffle(student);
	}

	public Bijection getFeatures() {
		return features;
	}

	public static StudentList loadData(String file, Opts opts) {// boolean
		// parameterizedEmit,
		// int obsClass1, String obsClass1Name) {
		String problems = "problem.*";// "problem.name";
		String steps = "";
		if (opts.inputHasStepColumn)
			steps = "step.*";// "step.name"
		Bijection bProblems, bSteps, bOutcome;
		bProblems = new Bijection();
		bSteps = new Bijection();
		bOutcome = new Bijection();

		ArrayList<String> instances = new ArrayList<String>();
		try {
			BufferedReader br = new BufferedReader(new FileReader(file));
			String line = "";
			// logger.debug("Loading data file: " + file);
			System.out.println("Loading data file: " + file);
			while ((line = br.readLine()) != null) {
				instances.add(line);
			}
			br.close();
		}
		catch (IOException e) {
			// logger.error("Error reading file");
			System.out.println("Error reading file");
			e.printStackTrace();
		}

		// hy* return new StudentList(data, problems, steps, bProblems, bSteps,
		// bOutcome);
		return new StudentList(instances, problems, steps, bProblems, bSteps,
				bOutcome, opts);// parameterizedEmit, obsClass1, obsClass1Name);
	}

	// received data from one hmm
	public static StudentList loadData(ArrayList<String> data, Opts opts) {
		String problems = "problem.*";// "problem.name";
		String steps = "";
		if (opts.inputHasStepColumn)
			steps = "step.*";// "step.name";

		Bijection bProblems, bSteps, bOutcome;
		bProblems = new Bijection();
		bSteps = new Bijection();
		bOutcome = new Bijection();

		// hy* return new StudentList(data, problems, steps, bProblems, bSteps,
		// bOutcome);
		return new StudentList(data, problems, steps, bProblems, bSteps, bOutcome,
				opts);// parameterizedEmit, obsClass1, obsClass1Name);
	}
}
