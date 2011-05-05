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
 * The Original Code is ConsistentOriScoreModifier.java
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


public class ConsistentOriScoreModifier extends AbstractHistogramConsistentScore {
	@Override
	public String getName() {
		return "ConsistentOriScoreModifier";
	}

	@Override
	public int[] getPositionSpecIndices(PositionSpec spec) {
		int oriidx;
		if (spec.getMode() == PositionSpecMode.ORI) {
			oriidx = 0;
		} else if (spec.getMode() == PositionSpecMode.SPATIAL_SCALE_ORI) {
			oriidx = 3;
		} else {
			return null;
		}
		return new int[]{oriidx};
	}

	@Override
	public void incr(int[][] matchedTermPos, int[] queryTermIndecies,float[] hist, double[] maxVals) {
		for (int i=0; i<matchedTermPos.length; i++) {
			double difTheta = (queryTermIndecies[0] - matchedTermPos[i][0] + maxVals[0]) / (2 * maxVals[0]);
			int bin = (int)(difTheta * hist.length);
			hist[bin]++;
		}
	}
}
