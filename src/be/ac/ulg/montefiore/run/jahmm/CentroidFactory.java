package be.ac.ulg.montefiore.run.jahmm;

/**
 * Creates a centroid for type <O>.  Used by the k-means algorithm.
 */
public interface CentroidFactory<O>
{
	public Centroid<O> factor();
}
