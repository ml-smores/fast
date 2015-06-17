package fast.evaluation;

import java.util.ArrayList;
import fast.common.Bijection;

public class Mastery { //per skill on test set
	
	public final double MASTERY_THRESHOLD = 0.95;
	public Double nbTotalStudents = 0.0;
	public Bijection studentsReachedMastery = new Bijection(); // to avoid repeatedly add student into nbPracToReachMastery
	public ArrayList<Double> nbPracToReachMastery = new ArrayList<Double>();
	
//	public Mastery(double nbTotalStudents){
//		this.nbTotalStudents = nbTotalStudents;
//	}
}
