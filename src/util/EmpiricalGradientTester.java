package util;

import edu.berkeley.nlp.math.DifferentiableFunction;
import edu.berkeley.nlp.math.DoubleArrays;
import fig.basic.LogInfo;

public class EmpiricalGradientTester {
	static final double EPS = 1e-6;
	static final double DEL_INITIAL = 1e-1;
	static final double DEL_MIN = 1e-9;
	public static void test(DifferentiableFunction func, double[] x) {
		double[] nextX = DoubleArrays.clone(x);
		double baseVal = func.valueAt(x);
		double[] grad = func.derivativeAt(x);
		for (int i=0; i<x.length; ++i) {
			double delta = DEL_INITIAL;
			boolean ok = false;
			double empDeriv = 0.0;
			while (delta > DEL_MIN && !ok) {
				nextX[i] += delta;
				double nextVal = func.valueAt(nextX);
				empDeriv = (nextVal - baseVal) / delta;
				if (close(empDeriv, grad[i])) {
					LogInfo.logss("Gradient ok for dim %d, delta %f, calculated %f, empirical: %f", i, delta, grad[i], empDeriv);
					ok = true;
				}
				nextX[i] -= delta;
				if (!ok) delta /= 2;
			}
			if (!ok) LogInfo.logss("Empirical gradient step-size underflow dim %d, delta %f, calculated %f, empirical: %f", i, delta, grad[i], empDeriv);
		}
	}
	public static boolean close(double x, double y) {
		return Math.abs(x - y) < EPS;
	}
}
