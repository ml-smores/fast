package fast.evaluation;

import java.util.ArrayList;

public class TestSummary{
	public ArrayList<Integer> actualLabels = new ArrayList<Integer>();
	public ArrayList<Integer> predLabels  = new ArrayList<Integer>();
	public ArrayList<Double> predProbs  = new ArrayList<Double>();
	public ArrayList<Double> priorProbKnowns = new ArrayList<Double>();
	public ArrayList<Double> posteriorProbKnowns = new ArrayList<Double>();
	public Degeneracy degeneracy = new Degeneracy(); //also contain train info
	public Mastery mastery = new Mastery();
	public Metrics eval = new Metrics();

	
	public void update(ArrayList<Integer> actualLabels, ArrayList<Integer> predLabels,  ArrayList<Double> predProbs){
		this.actualLabels = actualLabels;
		this.predLabels = predLabels;
		this.predProbs = predProbs;
	}
	
	public void update(ArrayList<Integer> actualLabels, ArrayList<Integer> predLabels,  ArrayList<Double> predProbs,
										ArrayList<Double> priorProbKnowns, ArrayList<Double> posteriorProbKnowns){
		this.actualLabels = actualLabels;
		this.predLabels = predLabels;
		this.predProbs = predProbs;
		this.priorProbKnowns = priorProbKnowns;
		this.posteriorProbKnowns = posteriorProbKnowns;
	}
	
	public void update(ArrayList<Integer> actualLabels, ArrayList<Integer> predLabels, ArrayList<Double> predProbs,	Degeneracy degeneracy, Mastery mastery){ 
		this.actualLabels.addAll(actualLabels);
		this.predLabels.addAll(predLabels);
		this.predProbs.addAll(predProbs);
		this.degeneracy = degeneracy;
		this.mastery = mastery;
	}
	
//	public void update(TestSummary testSummary){
//		this.actualLabels.addAll(testSummary.actualLabels);
//		this.predLabels.addAll(testSummary.predLabels);
//		this.predProbs.addAll(testSummary.predProbs);
//		this.priorProbKnowns = testSummary.priorProbKnowns;
//		this.posteriorProbKnowns = testSummary.posteriorProbKnowns;
//		this.degeneracy = testSummary.degeneracy; //also contain train info
//		this.mastery = testSummary.mastery;
//		this.eval = testSummary.eval;
//	}
	
//	public Metrics getEval(){
//		return eval;
//	}
//	
//	public Mastery getMastery(){
//		return mastery;
//	}
//	
//	public Degeneracy getDegeneracy(){
//		return degeneracy;
//	}
//	
//	public void setEval(Metrics eval){
//		this.eval = eval;
//	}
//	
//	public ArrayList<Integer> getActualLabels(){
//		return actualLabels;
//	}
//	
//	public ArrayList<Integer> getPredLabels(){
//		return predLabels;
//	}
//	
//	public ArrayList<Double> getPredProbs(){
//		return predProbs;
//	}
	
}
