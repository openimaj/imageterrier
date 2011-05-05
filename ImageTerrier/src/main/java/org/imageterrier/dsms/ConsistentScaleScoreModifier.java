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
 * The Original Code is ConsistentScaleScoreModifier.java
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


public class ConsistentScaleScoreModifier extends AbstractHistogramConsistentScore {
	@Override
	public String getName() {
		return "ConsistentScaleScoreModifier";
	}

	@Override
	public int[] getPositionSpecIndices(PositionSpec spec) {
		int sclidx;
		if (spec.getMode() == PositionSpecMode.SCALE) {
			sclidx = 0;
		} else if (spec.getMode() == PositionSpecMode.SPATIAL_SCALE_ORI) {
			sclidx = 2;
		} else {
			logger.warn("PositionSpec of the index is incompatible with this DSM");
			return null;
		}
		return new int[]{sclidx};
	}

	@Override
	public void incr(int[][] matchedTermPos, int[] queryTermIndecies, float[] hist, double[] maxVals) {
		double qlog = Math.log(queryTermIndecies[0]);
		double maxlog = Math.log(maxVals[0]);
		for (int i=0; i<matchedTermPos.length; i++) {
			double difTheta = (qlog - Math.log(matchedTermPos[i][0]) + maxlog) / (2 * maxlog );
			int bin = (int)(difTheta * hist.length);
			hist[bin]++;
		}
	}
}
