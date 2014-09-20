package fast.data;

import fast.hmmfeatures.Opts;

import java.util.Vector;

public class CVStudent extends Vector<DataPoint> {
	private static final long serialVersionUID = -7017179401151027439L;
	final int fold;

	public int getFold() {
		return fold;
	}

	public CVStudent(int fold) {
		super();
		this.fold = fold;
	}

	@Override
	public boolean add(DataPoint s) {
		if (s.getFold() != fold) {
			if (!Opts.preDpCurDpFromDifferentSet)
				System.out
						.println("Warn: Previous datapoint and current Datapoint are from different sets!");
			Opts.preDpCurDpFromDifferentSet = true;
		}
		// throw new
		// IllegalArgumentException("Multiple occurrences of a student should be in the same fold");

		return super.add(s);
	}

}
