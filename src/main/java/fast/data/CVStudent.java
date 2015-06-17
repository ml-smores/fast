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

package fast.data;

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
	//	if (s.getFold() != fold) {
//			if (!this.opts.preDpCurDpFromDifferentSet)
//				System.out
//						.println("Warn: Previous datapoint and current Datapoint are from different sets!");
//			this.opts.preDpCurDpFromDifferentSet = true;
	//	}
		// throw new
		// IllegalArgumentException("Multiple occurrences of a student should be in the same fold");

		return super.add(s);
	}

}
