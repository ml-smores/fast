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

package fast.experimenter;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;

public class Evaluation {

	public Opts opts;
	public boolean verbose = false;

	public Evaluation(Opts opts) {
		this.opts = opts;
	}

	// one fold all skills
	// one fold all skills
		public int doEvaluate(ArrayList<Double> probs, ArrayList<Integer> labels,
				ArrayList<Integer> actualLabels, ArrayList<Integer> trainTestIndicator, ArrayList<Double> pknow,
				ArrayList<String> students, ArrayList<String> kcs)throws IOException {
		// evaluate and print out
		if (probs.size() != labels.size() || labels.size() != actualLabels.size()
				|| actualLabels.size() != trainTestIndicator.size()) {
			System.out.println("Error: doEvluation size mismatch!");
			System.exit(1);
		}

		double rmse = 0;
		double accuracy = 0;
		double sumSquareError = 0;
		double nbMisclassification = 0;
		double correctRatio = 0.0;
		BufferedWriter predWriter = new BufferedWriter(new FileWriter(
				opts.predictionFile));
		predWriter.write("actualLabel,predLabel,predProb,probKnow,student,kc\n");

		// System.out.println("number of intances: " + probs.size());
		int realSize = 0;
		for (int i = 0; i < probs.size(); i++) {
			if (verbose) {
				System.out.println(i + "th:\tpredict:" + probs.get(i) + ","
						+ labels.get(i) + "\tactual:" + actualLabels.get(i));
			}
			if (trainTestIndicator.get(i) != -1) {
				realSize++;
				predWriter.write(actualLabels.get(i) + "," + labels.get(i) + ","
						+ probs.get(i) + "," + pknow.get(i) + "," + students.get(i) + "," + kcs.get(i) + "\n");
				sumSquareError += Math.pow((actualLabels.get(i) - probs.get(i)), 2);
				nbMisclassification += Math.abs(actualLabels.get(i) - labels.get(i));
				if (actualLabels.get(i) == 1)
					correctRatio++;
			}
		}
		predWriter.flush();
		predWriter.close();
		rmse = Math.pow(sumSquareError / realSize, 0.5);
		accuracy = (realSize - nbMisclassification) / realSize;
		correctRatio = correctRatio / realSize;
		String str = "\nTest Results:\n\trmse=\t" + rmse + "\taccuracy=\t"
				+ accuracy + "\tcorrectClassRatio=\t" + correctRatio
				+ "\t#testObservations=\t" + realSize;
		// String str = "\nTest Results:\n\tcorrectClassRatio=" + correctRatio;
		System.out.println(str);
		if (opts.writeMainLog) {
			opts.mainLogWriter.write(str + "\n");
			opts.mainLogWriter.flush();
		}
		return realSize;
	}

}
