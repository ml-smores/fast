package fast.common;
/*
 * Copyright (c) 2002-5 Gregor Heinrich, Arbylon. All rights reserved. No
 * redistribution prior to release, in source or binary forms, without written
 * consent of the author. A release will be considered after functionality has
 * been thoroughly tested and after licensing is clearly defined. All
 * contributors are required to comply with this and keep the code confidential.
 */

import java.util.Random;



/**
 * Instance-based samplers with diverse sampling methods, including beta, gamma,
 * multinomial, and Dirichlet distributions as well as Dirichlet processes,
 * using Sethurahman's stick-breaking construction and Chinese restaurant
 * process. The random generator used is provided in the constructor.
 * 
 * @author heinrich (partly adapted from Yee Whye Teh's npbayes Matlab / C code)
 */
public class RandomSampler
{
    public double lastRand;
    private Random rand;
    
    public RandomSampler(Random rand)
    {
        this.rand = rand;
    }
    
    protected double drand() 
    {
        return rand.nextDouble();
    }

    /**
     * self-contained gamma generator. Multiply result with scale parameter (or
     * divide by rate parameter). After Teh (npbayes).
     * 
     * @param rr shape parameter
     * @return
     */
    public double randGamma(double rr) {
        double bb, cc, dd;
        double uu, vv, ww, xx, yy, zz;

        if (rr <= 0.0) {
            /* Not well defined, set to zero and skip. */
            return 0.0;
        } else if (rr == 1.0) {
            /* Exponential */
            return -Math.log(drand());
        } else if (rr < 1.0) {
            /* Use Johnks generator */
            cc = 1.0 / rr;
            dd = 1.0 / (1.0 - rr);
            while (true) {
                xx = Math.pow(drand(), cc);
                yy = xx + Math.pow(drand(), dd);
                if (yy <= 1.0) {
                    assert yy != 0 && xx / yy > 0;
                    return -Math.log(drand()) * xx / yy;
                }
            }
        } else { /* rr > 1.0 */
            /* Use bests algorithm */
            bb = rr - 1.0;
            cc = 3.0 * rr - 0.75;
            while (true) {
                uu = drand();
                vv = drand();
                ww = uu * (1.0 - uu);
                yy = Math.sqrt(cc / ww) * (uu - 0.5);
                xx = bb + yy;
                if (xx >= 0) {
                    zz = 64.0 * ww * ww * ww * vv * vv;
                    assert zz > 0 && bb != 0 && xx / bb > 0;
                    if ((zz <= (1.0 - 2.0 * yy * yy / xx))
                        || (Math.log(zz) <= 2.0 * (bb * Math.log(xx / bb) - yy))) {
                        return xx;
                    }
                }
            }
        }
    }

    /**
     * randgamma(aa) Generates gamma samples, one for each element in aa.
     * 
     * @param aa
     */
    public double[] randGamma(double[] aa) {
        double[] gamma = new double[aa.length];
        for (int i = 0; i < gamma.length; i++) {
            gamma[i] = randGamma(aa[i]);
        }
        return gamma;
    }

    /**
     * randdir(aa) generates one Dirichlet sample vector according to the
     * parameters alpha. ORIG: Generates Dirichlet samples, with weights given
     * in aa. The output sums to 1 along normdim, and each such sum corresponds
     * to one Dirichlet sample.
     * 
     * @param aa
     * @param normdim
     * @return
     */
    public double[] randDir(double[] aa) {
        double[] ww = randGamma(aa);

        double sum = 0;
        for (int i = 0; i < ww.length; i++) 
        {
            sum += ww[i];
        }
        for (int i = 0; i < ww.length; i++) 
        {
            ww[i] /= sum;
        }
        return ww;
    }



    /**
     * Creates one multinomial sample given the parameter vector pp. Each
     * category is named after the index (0-based!) of the respective element of
     * pp; Sometimes called categorical distribution (e.g., in BUGS). This
     * version uses a binary search algorithm and does not require
     * normalisation.
     */
    public int randMult(final double[] pp) {

        int i;
        double[] cumPp = new double[pp.length];

        System.arraycopy(pp, 0, cumPp, 0, pp.length);

        for (i = 1; i < pp.length; i++) {
            cumPp[i] += cumPp[i - 1];

        }
        // this automatically normalises.
        double randNum = drand() * cumPp[i - 1];

        // TODO: use insertion point formula in Array.binarySearch()
        i = binarySearch(cumPp, randNum);

        // System.out.println(Vectors.print(pp) + " " + i);

        return i;
    }
    
    public int JPrandMult(final double[] pp)
    {
    	double target = drand();
    	double cumProb = 0;
    	for (int i = 0; i < pp.length; i++)
    	{
    		cumProb += pp[i];
    		if (target < cumProb)
    			return i;
    	}
    	return -1;
    }
    

    /**
     * Creates one multinomial sample given the parameter vector pp. Each
     * category is named after the index (0-based!) of the respective element of
     * pp; Sometimes called categorical distribution (e.g., in BUGS). This
     * version uses a binary search algorithm and does not require
     * normalisation. Note that the parameters used <i>directly</i> changed
     * because the multinomial is cumulated to save memory and copying time.
     */
    public int randMultDirect(double[] pp) {

        int i;
        for (i = 1; i < pp.length; i++) {
            pp[i] += pp[i - 1];

        }
        // this automatically normalises.
        double randNum = drand() * pp[i - 1];
        lastRand = randNum;

        // TODO: use insertion point formula in Array.binarySearch()
        i = binarySearch(pp, randNum);

        // System.out.println(Vectors.print(pp) + " " + i);

        return i;
    }
    
    /**
     * perform a binary search and return the first index i at which a[i] >= p.
     * Adapted from java.util.Arrays.binarySearch.
     * 
     * @param a
     * @param p
     * @return
     */
    public int binarySearch(double[] a, double p) {
        if (p < a[0]) {
            return 0;
        }
        int low = 0;
        int high = a.length - 1;
        while (low <= high) {
            int mid = (low + high) >> 1;
            double midVal = a[mid];

            if (midVal < p) {
                low = mid + 1;
            } else if (midVal > p) {
                if (a[mid - 1] < p)
                    return mid;
                high = mid - 1;
            } else {
                return mid;
            }
        }
        // out of range.
        return a.length;
    }


}
