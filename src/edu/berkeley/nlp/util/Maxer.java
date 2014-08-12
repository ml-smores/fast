package edu.berkeley.nlp.util;

import fig.basic.Fmt;

public class Maxer<T> {
    private double max = Double.NEGATIVE_INFINITY;
    private T argMax = null;
    private boolean verbose = false;

    public String toString() {
        return argMax.toString() + ": " + Fmt.D(max);
    }

    public void observe(T t, double val) {
        if (verbose) Logger.logss("Observing " + t.toString() + " @ " + Fmt.D(val));
        if (val > max) {
            max = val;
            argMax = t;
            Logger.logss(t.toString() + " is new max");
        }
    }

    public double getMax() {
        return max;
    }

    public T argMax() {
        return argMax;
    }

    public void setVerbose(boolean b) {
        verbose = b;
    }
}
