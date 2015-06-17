package fast.evaluation;

//import java.text.DateFormat;
import java.text.DecimalFormat;
//import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.Locale;
import fast.common.Bijection;
//import fast.evaluation.EvaluationGeneral.Metrics;

public class Metrics{
	private DecimalFormat formatter;
	{
		formatter = (DecimalFormat) DecimalFormat.getInstance(Locale.US);
		formatter.applyPattern("#.###");
	}
	//public static DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
	private String modelName = "";
	private HashMap<String, Double> metricNameToValue = new HashMap<String, Double>();
	private Bijection metricNames = new Bijection(); //Need to keep the ordering
	
	public Metrics(String name){
		modelName = name;
	}
	
	public Metrics(){
	}
	
	public Bijection getMetricNames(){
		return metricNames;
	}
	
	public double getMetricValue(String name){
		return metricNameToValue.get(name);
	}

	
	public void setMetricValue(String metricName, Double metricValue){
		metricNames.put(metricName);
		metricNameToValue.put(metricName, metricValue);
	}
	
	public void copyMetrics(Metrics eval){
		for (int i = 0; i < eval.metricNames.getSize(); i ++){
			String metricName = eval.metricNames.get(i);
			double metricValue = eval.metricNameToValue.get(metricName);
			metricNames.put(metricName);	
			metricNameToValue.put(metricName, metricValue);
		}
	}
	
	public String getHeader(String delim){
		String header = "Name" + delim;
		for (int i = 0; i < metricNames.getSize(); i ++){
			header += metricNames.get(i) + delim;
		}
		return header;
	}
	
	public String getEvaluationStr(Metrics eval, String delim){
		String evaluationStr = eval.modelName + delim;
		for (int i = 0; i < metricNames.getSize(); i ++){
			Double value = metricNameToValue.get(metricNames.get(i));
			String value_str = Double.isNaN(value)? "NaN" : formatter.format(value);
			evaluationStr += value_str + delim;
		}
		return evaluationStr;
	}
	
	public String getEvaluationStr(String delimiter){
		String evaluationStr = getEvaluationStr(this, delimiter);
		return evaluationStr;
	}
	
}
