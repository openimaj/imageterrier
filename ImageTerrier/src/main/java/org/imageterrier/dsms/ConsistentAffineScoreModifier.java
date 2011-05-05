/**
 * ImageTerrier - The Terabyte Retriever for Images
 * Webpage: http://www.imageterrier.org/
 * Contact: jsh2@ecs.soton.ac.uk
 * Electronics and Computer Science, University of Southampton
 * http://www.ecs.soton.ac.uk/
 *
 * The contents of this file are subject to the Mozilla Public License
 * Version 1.1 (the "License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://www.mozilla.org/MPL/
 *
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See
 * the License for the specific language governing rights and limitations
 * under the License.
 *
 * The Original Code is ConsistentAffineScoreModifier.java
 *
 * The Original Code is Copyright (C) 2011 the University of Southampton
 * and the original contributors.
 * All Rights Reserved.
 *
 * Contributor(s):
 *   Sina Samangooei <ss@ecs.soton.ac.uk> (original contributor)
 *   Jonathon Hare <jsh2@ecs.soton.ac.uk> (original contributor)
 *   David Dupplaw <dpd@ecs.soton.ac.uk>
 */
package org.imageterrier.dsms;

import org.imageterrier.locfile.PositionSpec;
import org.imageterrier.locfile.PositionSpec.PositionSpecMode;
import org.terrier.utility.ApplicationSetup;


public class ConsistentAffineScoreModifier  extends AbstractHistogramConsistentScore {
	int ntibins = Integer.parseInt(ApplicationSetup.getProperty(CONSISTENT_HISTOGRAM_BINS+".tilt", "10"));
	int nthbins = Integer.parseInt(ApplicationSetup.getProperty(CONSISTENT_HISTOGRAM_BINS+".theta", "10"));
	@Override
	public String getName() {
		return "ConsistentAffineScoreModifier";
	}

	@Override
	public int[] getPositionSpecIndices(PositionSpec spec) {
		int[] affineIndecies = new int[2];
		if (spec.getMode() == PositionSpecMode.AFFINE) {
			affineIndecies[0] = 0;
			affineIndecies[1] = 1;
		} else if (spec.getMode() == PositionSpecMode.SPATIAL_AFFINE) {
			affineIndecies[0] = 2;
			affineIndecies[1] = 3;
		} else {
			logger.warn("PositionSpec of the index is incompatible with this DSM");
			return null;
		}
		return affineIndecies;
	}

	@Override
	public void incr(int[][] matchedTermPos, int[] queryPositions, float[] hist, double[] maxVals) {
		
		float qtheta = (float) (queryPositions[0] / maxVals[0]);
		float qtilt = (float) (queryPositions[1] / maxVals[1]);
		for (int i=0; i<matchedTermPos.length; i++) {
			float dtheta = (float) (matchedTermPos[i][0] / maxVals[0]);
			float dtilt = (float) (matchedTermPos[i][1] / maxVals[1]);
			
			int tiltBin = (int) (( ((qtilt- dtilt) + 1) ) * ntibins / 2.0);
			int thetaBin = (int) (( ((qtheta - dtheta) + 1) ) * nthbins / 2.0);
			int bin = tiltBin + (thetaBin * ntibins);

			hist[bin]++;
		}
	}

}
