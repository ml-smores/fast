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

//import java.io.BufferedReader;
//import java.io.FileReader;
//import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
//import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
//import java.util.Set;
import fast.common.Bijection;

/** This correspond to one HMM */
public class StudentList extends LinkedList<CVStudent> {

	private static final long serialVersionUID = 730176924591642451L;

	/* students vs. finalStudents: Datapoints' student interger always matches to students, so you should always look up students to get original student name; However, finalStudents keep track of the actual number of final students if you configured to remove sequencelength=1 students; */
	private final Bijection students, problems, steps, outcomes, skills;
	private Bijection finalStudents;
	private Bijection allFeatures, initFeatures, tranFeatures, emitFeatures; // allFeatures includes init, tran, emit (all)
	private int nbDataPoints = 0;
	//private final HashMap<Integer, Set<Integer>> cognitiveModel;
	
	public StudentList(ArrayList<String> training, boolean parameterizing, boolean parameterizedInit, boolean parameterizedTran, boolean parameterizedEmit,
			boolean forceUsingInputFeatures, boolean bias, int nbHiddenStates, Bijection students, Bijection items) {//boolean differentBias, 
		this(new DataPointList(training, parameterizing, parameterizedInit, parameterizedTran, parameterizedEmit,
				 forceUsingInputFeatures, bias, nbHiddenStates, students, items));//differentBias, 
	}
	
	public StudentList(DataPointList data) {
		super();
		//this.filename = data.getFilename();
		this.finalStudents = new Bijection();
		this.students = data.getStudents();
		this.problems = data.getProblems();
		this.steps = data.getSteps();
		this.outcomes = data.getOutcomes();
		this.skills = data.getSkills();
		//this.cognitiveModel = data.getCognitiveModel();
		this.allFeatures = data.getAllFeatures();// get finalFeatures()
		this.initFeatures = data.getInitFeatures();
		this.tranFeatures = data.getTranFeatures();
		this.emitFeatures = data.getEmitFeatures();
		//this.numberOfObservations = data.size();
		Iterator<DataPoint> iDatum = data.iterator();
		int previousStudent = -1, previousProblem = -1, previousStep = -1;
		// String previousStudentStr = "";
		// double[][] previousFeatures_ = null;
		// double[] previousFeatures = null;
		int notPutStudent = -1;
		ArrayList<String> seqLength1Stu = new ArrayList<String>();
		// int nbLines = 1;
		nbDataPoints = 0;
		
		while (iDatum.hasNext()) {
			// System.out.println("nbLines=" + nbLines);
			DataPoint datum = iDatum.next();
			nbDataPoints++;
			int aStudent = datum.getStudent();
			int aProblem = datum.getProblem();
			int aStep = datum.getStep();

			// hy: "this" keep track of the LinkedList
			CVStudent student;
			if (previousStudent != aStudent) {
				// hy: remove the student's sequence which has just length 1
				if (previousStudent != -1) {
					CVStudent preStudent = this.getLast();
					if (preStudent.size() == 1) {
						String stu = students.get(previousStudent);// students.get(preStudent.get(0).getStudent());
//						if (opts.removeSeqLength1InTrain && opts.nowInTrain || (opts.removeSeqLength1InTest && !opts.nowInTrain)) {
//							System.out.println("Removing seq length=1 for student:" + stu);
//							// }
//							notPutStudent = previousStudent;
//							this.removeLast();
//						}
//						else 
						finalStudents.put(previousStudent + "");
						seqLength1Stu.add(stu);
					}
					else if (preStudent.size() == 2) {
						int fold = preStudent.get(0).getFold();
						if (fold == -1) {
							String stu = students.get(preStudent.get(0).getStudent());
//							if (opts.removeSeqLength1InTrain && opts.nowInTrain || (opts.removeSeqLength1InTest && !opts.nowInTrain)) {
//								// if (opts.verbose) {
//								// logger.error("Removing seq length=1 for student:" + stu);
//								//System.out.println("Removing seq length=1 for student:" + stu);
//								// }
//								notPutStudent = previousStudent;
//								this.removeLast();
//							}
//							else
							finalStudents.put(previousStudent + "");
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
			(previousStudent == aStudent) && (previousProblem == aProblem)
					&& (previousStep == aStep)) {
				System.out.println("WARNING: Repeated datum (same as the one before) for kc=" + data.getSkills().get(datum.getSkill()) 
						+ ", student= " + data.getStudents().get(aStudent) 
						+ " problem=" + data.getProblems().get(aProblem) 
						+ " step=" + data.getSteps().get(aStep) + ". Yet I still keep it in the dataset.");// + "or features are repeated!");
			}
			
			previousStudent = aStudent;
			previousProblem = aProblem;
			previousStep = aStep;
		}
		
		if (previousStudent != -1) {
			CVStudent preStudent = this.getLast();
			if (preStudent.size() == 1) {
				String stu = students.get(previousStudent);// students.get(preStudent.get(0).getStudent());
//				if (opts.removeSeqLength1InTrain && opts.nowInTrain || (opts.removeSeqLength1InTest && !opts.nowInTrain)) {
//					notPutStudent = previousStudent;
//					this.removeLast();
//				}
//				else
				finalStudents.put(previousStudent + "");
				seqLength1Stu.add(stu);
			}
			else if (preStudent.size() == 2) {
				int fold = preStudent.get(0).getFold();
				if (fold == -1) {
					String stu = students.get(preStudent.get(0).getStudent());
//					if (opts.removeSeqLength1InTrain && opts.nowInTrain
//							|| (opts.removeSeqLength1InTest && !opts.nowInTrain)) {
//						// if (opts.verbose) {
//						// logger.error("Removing seq length=1 for student:" + stu);
//						//System.out.println("Removing seq length=1 for student:" + stu);
//						// }
//						notPutStudent = previousStudent;
//						this.removeLast();
//					}
//					else 
					finalStudents.put(previousStudent + "");
					seqLength1Stu.add(stu);
				}
			}
			if (previousStudent != notPutStudent)
				finalStudents.put(previousStudent + "");
		}
		seqLength1Stu = new ArrayList<String>();
	}

	public Bijection getFinalStudents() {
		return finalStudents;// students;
	}

	// here is the id matches the datapoing student ids.
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

//	public String getFilename() {
//		return filename;
//	}

	public Bijection getExpertSkills() {
		return skills;
	}

//	public HashMap<Integer, Set<Integer>> getExpertCM() {
//		return cm;
//	}

	public void shuffleTime() {
		for (CVStudent student : this)
			Collections.shuffle(student);
	}

	public Bijection getAllFeatures() {
		return allFeatures;
	}

	public Bijection getInitFeatures() {
		return initFeatures;
	}

	public Bijection getTranFeatures() {
		return tranFeatures;
	}

	public Bijection getEmitFeatures() {
		return emitFeatures;
	}
	
	public int getNbDataPoints(){
		return nbDataPoints;
	}

//	public static StudentList loadData(String file) {// boolean
//		// parameterizedEmit,
//		// int obsClass1, String obsClass1Name) {
//		String problems = "problem.*";// "problem.name";
//		String steps = "";
//		if (inputHasStepColumn)
//			steps = "step.*";// "step.name"
//		Bijection bProblems, bSteps, bOutcome, bTrainFeatures;
//		bProblems = new Bijection();
//		bSteps = new Bijection();
//		bOutcome = new Bijection();
//		bTrainFeatures = new Bijection();
//
//		ArrayList<String> instances = new ArrayList<String>();
//		try {
//			BufferedReader br = new BufferedReader(new FileReader(file));
//			String line = "";
//			// logger.debug("Loading data file: " + file);
//			System.out.println("Loading data file: " + file);
//			while ((line = br.readLine()) != null) {
//				instances.add(line);
//			}
//			br.close();
//		}
//		catch (IOException e) {
//			// logger.error("Error reading file");
//			System.out.println("Error reading file");
//			e.printStackTrace();
//		}
//
//		// hy* return new StudentList(data, problems, steps, bProblems, bSteps,
//		// bOutcome);
//		return new StudentList(instances, problems, steps, bProblems, bSteps,
//				bOutcome, bTrainFeatures);// parameterizedEmit, obsClass1,
//																				// obsClass1Name);
//	}

	// received data from one hmm
//	public static StudentList loadData(ArrayList<String> data) {
//		String problems = "problem.*";// "problem.name";
//		String steps = "";
//		if (inputHasStepColumn)
//			steps = "step.*";// "step.name";
//
//		Bijection bProblems, bSteps, bOutcome, bTrainFeatures;
//		bProblems = new Bijection();
//		bSteps = new Bijection();
//		bOutcome = new Bijection();
//		bTrainFeatures = new Bijection();
//
//		return new StudentList(data, problems, steps, bProblems, bSteps, bOutcome,
//				bTrainFeatures);
//	}

//	public static StudentList loadData(ArrayList<String> data,
//			Bijection trainFeatures) {
//		String problems = "problem.*";// "problem.name";
//		String steps = "";
//		if (opts.inputHasStepColumn)
//			steps = "step.*";// "step.name";
//
//		Bijection bProblems, bSteps, bOutcome, bTrainFeatures;
//		bProblems = new Bijection();
//		bSteps = new Bijection();
//		bOutcome = new Bijection();
//		bTrainFeatures = trainFeatures;
//
//		// hy* return new StudentList(data, problems, steps, bProblems, bSteps,
//		// bOutcome);
//		return new StudentList(data, problems, steps, bProblems, bSteps, bOutcome,
//				bTrainFeatures);// parameterizedEmit, obsClass1, obsClass1Name);
//	}
	
}
