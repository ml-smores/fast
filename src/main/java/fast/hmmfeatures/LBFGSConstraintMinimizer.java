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

/* 
 * This class is built based on: edu.berkeley.nlp.math.LBFGSMinimizer
 */

package fast.hmmfeatures;

import java.io.Serializable;
import java.util.LinkedList;
import edu.berkeley.nlp.math.DifferentiableFunction;
import edu.berkeley.nlp.math.DoubleArrays;
import edu.berkeley.nlp.math.GradientMinimizer;
import edu.berkeley.nlp.util.CallbackFunction;
import edu.berkeley.nlp.util.Logger;

public class LBFGSConstraintMinimizer implements GradientMinimizer,
		Serializable {
	private static final long serialVersionUID = 36473897808840226L;
	double eps = 1e-10;
	int maxIterations = 20;
	int maxHistorySize = 5;
	LinkedList<double[]> inputDifferenceVectorList = new LinkedList<double[]>();
	LinkedList<double[]> derivativeDifferenceVectorList = new LinkedList<double[]>();
	transient CallbackFunction iterCallbackFunction = null;
	int minIterations = -1;
	double initialStepSizeMultiplier = 0.01;
	double stepSizeMultiplier = 0.5;
	boolean convergedAndClearedHistories = false;
	public boolean parameterizedEmit = true;

	public LBFGSConstraintMinimizer(double eps, int maxIterations) {
		this.eps = eps;
		this.maxIterations = maxIterations;
	}

	public LBFGSConstraintMinimizer() {
	}

	public void setMinIteratons(int minIterations) {
		this.minIterations = minIterations;
	}

	public void setMaxIterations(int maxIterations) {
		this.maxIterations = maxIterations;
	}

	public void setInitialStepSizeMultiplier(double initialStepSizeMultiplier) {
		this.initialStepSizeMultiplier = initialStepSizeMultiplier;
	}

	public void setStepSizeMultiplier(double stepSizeMultiplier) {
		this.stepSizeMultiplier = stepSizeMultiplier;
	}

	public double[] getSearchDirection(int dimension, double[] derivative) {
		double[] initialInverseHessianDiagonal = getInitialInverseHessianDiagonal(dimension);
		double[] direction = implicitMultiply(initialInverseHessianDiagonal,
				derivative);
		return direction;
	}

	protected double[] getInitialInverseHessianDiagonal(int dimension) {
		double scale = 1.0;
		if (derivativeDifferenceVectorList.size() >= 1) {
			double[] lastDerivativeDifference = getLastDerivativeDifference();
			double[] lastInputDifference = getLastInputDifference();
			double num = DoubleArrays.innerProduct(lastDerivativeDifference,
					lastInputDifference);
			double den = DoubleArrays.innerProduct(lastDerivativeDifference,
					lastDerivativeDifference);
			scale = num / den;
		}
		return DoubleArrays.constantArray(scale, dimension);
	}

	public double[] minimize(DifferentiableFunction function, double[] initial,
			double tolerance) {
		return minimize(function, initial, tolerance, false);
	}

	// hy: minimizer.minimize(negativeLikelihood, weights, MINIMIZER_TOL,
	// PRINT_MINIMIZER);
	public double[] minimize(DifferentiableFunction function, double[] initial,
			double tolerance, boolean printProgress) {

		LineSearcher lineSearcher = new LineSearcher();
		lineSearcher.growStepSizeMultiplier = 1.0;
		lineSearcher.stepSize = 1.0;
		double[] guess = DoubleArrays.clone(initial);
		for (int iteration = 0; iteration < maxIterations; iteration++) {
			// EmpiricalGradientTester.test(function, guess);
			// if (Opts.verbose)
			// System.out.println("LBFGS iter=" + iteration);
			double value = function.valueAt(guess);
			double[] derivative = function.derivativeAt(guess);
			double[] initialInverseHessianDiagonal = getInitialInverseHessianDiagonal(function);
			double[] direction = implicitMultiply(initialInverseHessianDiagonal,
					derivative);
			// System.out.println(" Derivative is: "+DoubleArrays.toString(derivative,
			// 100));
			// DoubleArrays.assign(direction, derivative);
			// hy:
			if (direction == null) {
				// 1) return initial
				Logger.err("\tWARN: return initial guess!");
				// 2) re-initialize feature weights, re-calucuate initial NLL and
				// restart
				return initial;
			}
			DoubleArrays.scale(direction, -1.0);
			// System.out.println(" Looking in direction: "+DoubleArrays.toString(direction,
			// 100));
			if (iteration == 0)
				lineSearcher.shrinkStepSizeMultiplier = initialStepSizeMultiplier;
			else
				lineSearcher.shrinkStepSizeMultiplier = stepSizeMultiplier;
			lineSearcher.stepSize = 1.0;
			double[] nextGuess = lineSearcher.minimize(function, guess, direction);
			if (lineSearcher.stepSizeUnderflow) {
				// hy{
				double nextValue = function.valueAt(nextGuess);// nextGuess=initial
				if (iteration >= minIterations
						&& converged(value, nextValue, tolerance)) {
					System.err
							.println("WARN: step size underflow but LL is changing small, return initial!");
					return nextGuess;
				}
				// }hy
				// if step size underflow, clear histories and repeat this iteration
				clearHistories();
				--iteration;
				continue;
			}
			double nextValue = function.valueAt(nextGuess);
			double[] nextDerivative = function.derivativeAt(nextGuess);
			if (printProgress)
				printProgress(iteration, nextValue);

			if (iteration >= minIterations && converged(value, nextValue, tolerance)) {
				if (!convergedAndClearedHistories) {
					clearHistories();
					convergedAndClearedHistories = true;
				}
				else {
					return nextGuess;
				}
			}
			else {
				convergedAndClearedHistories = false;
			}

			updateHistories(guess, nextGuess, derivative, nextDerivative);
			guess = nextGuess;
			value = nextValue;
			derivative = nextDerivative;
			if (iterCallbackFunction != null) {
				iterCallbackFunction.callback(guess, iteration, value, derivative);
			}
		}
		// Logger.logs("LBFGSMinimizer.minimize: Exceeded maxIterations without converging.");
		// System.err.println("LBFGSMinimizer.minimize: Exceeded maxIterations without converging.");
		return guess;
	}

	private void printProgress(int iteration, double nextValue) {
		Logger.logs(String.format(
				"[LBFGSMinimizer.minimize] Iteration %d ended with value %.6f\n",
				iteration, nextValue));

	}

	protected boolean converged(double value, double nextValue, double tolerance) {
		if (value == nextValue)
			return true;
		double valueChange = Math.abs(nextValue - value);
		// hy
		double valueAverage = Math.abs(nextValue + value + this.eps) / 2.0;
		if (valueChange / valueAverage < tolerance)
			return true;
		return false;
	}

	protected void updateHistories(double[] guess, double[] nextGuess,
			double[] derivative, double[] nextDerivative) {
		double[] guessChange = DoubleArrays.addMultiples(nextGuess, 1.0, guess,
				-1.0);
		double[] derivativeChange = DoubleArrays.addMultiples(nextDerivative, 1.0,
				derivative, -1.0);
		pushOntoList(guessChange, inputDifferenceVectorList);
		pushOntoList(derivativeChange, derivativeDifferenceVectorList);
	}

	private void pushOntoList(double[] vector, LinkedList<double[]> vectorList) {
		vectorList.addFirst(vector);
		if (vectorList.size() > maxHistorySize)
			vectorList.removeLast();
	}

	public void clearHistories() {
		// if (Opts.verbose)
		// LogInfo.logss("LBFGS cleared history.");
		inputDifferenceVectorList.clear();
		derivativeDifferenceVectorList.clear();
	}

	private int historySize() {
		return inputDifferenceVectorList.size();
	}

	public void setMaxHistorySize(int maxHistorySize) {
		this.maxHistorySize = maxHistorySize;
	}

	private double[] getInputDifference(int num) {
		// 0 is previous, 1 is the one before that
		return inputDifferenceVectorList.get(num);
	}

	private double[] getDerivativeDifference(int num) {
		return derivativeDifferenceVectorList.get(num);
	}

	private double[] getLastDerivativeDifference() {
		return derivativeDifferenceVectorList.getFirst();
	}

	private double[] getLastInputDifference() {
		return inputDifferenceVectorList.getFirst();
	}

	private double[] implicitMultiply(double[] initialInverseHessianDiagonal,
			double[] derivative) {
		double[] rho = new double[historySize()];
		double[] alpha = new double[historySize()];
		double[] right = DoubleArrays.clone(derivative);
		// loop last backward
		for (int i = historySize() - 1; i >= 0; i--) {
			double[] inputDifference = getInputDifference(i);
			double[] derivativeDifference = getDerivativeDifference(i);
			rho[i] = DoubleArrays.innerProduct(inputDifference, derivativeDifference);
			if (rho[i] == 0.0) {
				Logger.err("LBFGSMinimizer.implicitMultiply: Curvature problem.");
				this.parameterizedEmit = false;// use just EM to retrain
				return null; // the only place to return null
				// }
				// else
				// throw new RuntimeException(
				// "LBFGSMinimizer.implicitMultiply: Curvature problem.");
			}
			alpha[i] = DoubleArrays.innerProduct(inputDifference, right) / rho[i];
			right = DoubleArrays.addMultiples(right, 1.0, derivativeDifference, -1.0
					* alpha[i]);
		}
		double[] left = DoubleArrays.pointwiseMultiply(
				initialInverseHessianDiagonal, right);
		for (int i = 0; i < historySize(); i++) {
			double[] inputDifference = getInputDifference(i);
			double[] derivativeDifference = getDerivativeDifference(i);
			double beta = DoubleArrays.innerProduct(derivativeDifference, left)
					/ rho[i];
			left = DoubleArrays.addMultiples(left, 1.0, inputDifference, alpha[i]
					- beta);
		}
		return left;
	}

	private double[] getInitialInverseHessianDiagonal(
			DifferentiableFunction function) {
		double scale = 1.0;
		if (derivativeDifferenceVectorList.size() >= 1) {
			double[] lastDerivativeDifference = getLastDerivativeDifference();
			double[] lastInputDifference = getLastInputDifference();
			double num = DoubleArrays.innerProduct(lastDerivativeDifference,
					lastInputDifference);
			double den = DoubleArrays.innerProduct(lastDerivativeDifference,
					lastDerivativeDifference);
			scale = num / den;
		}
		return DoubleArrays.constantArray(scale, function.dimension());
	}

	/**
	 * User callback function to test or examine weights at the end of each
	 * iteration
	 * 
	 * @param callbackFunction
	 *          Will get called with the following args (double[] currentGuess,
	 *          int iterDone, double value, double[] derivative) You don't have to
	 *          read any or all of these.
	 */
	public void setIterationCallbackFunction(CallbackFunction callbackFunction) {
		this.iterCallbackFunction = callbackFunction;
	}
}
