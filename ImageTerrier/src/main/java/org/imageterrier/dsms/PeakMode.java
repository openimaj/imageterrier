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
 * The Original Code is PeakMode.java
 *
 * The Original Code is Copyright (C) 2011 the University of Southampton
 * and the original contributors.
 * All Rights Reserved.
 *
 * Contributor(s):
 *   Jonathon Hare <jsh2@ecs.soton.ac.uk> (original contributor)
 *   Sina Samangooei <ss@ecs.soton.ac.uk>
 *   David Dupplaw <dpd@ecs.soton.ac.uk>
 */
package org.imageterrier.dsms;

/**
 * Different techniques for generating weights from 
 * histograms in scoring.
 * 
 * @author Sina Samangooei <ss@ecs.soton.ac.uk>
 * @author Jonathon Hare <jsh2@ecs.soton.ac.uk>
 *
 */
public enum PeakMode {
	/**
	 * weight by the histogram variance 
	 */
	VARIANCE {
		float findVar(float [] hist) {
			float mean = 0;
			float oldmean = 0;
			float var = 0;
			float totalSum = 0;
			for (int i = 0 ; i < hist.length; i++) {
				for(int j = 0; j < hist[i]; j++){
					totalSum ++;
					oldmean = mean;
					float x = i;
					mean = mean + (x - oldmean)/totalSum;
					var = var + (x - oldmean) * (x - mean);
				}
			}
			var /= totalSum;

			return (float) var;
		}
		
		@Override
		public float weighting(float[] hist) {
			return findVar(hist);
		}
	},
	/**
	 * weight by the normalised maximum value of the histogram  
	 */
	MAX {
		
		float findMax(float [] hist) {			
			//norm
			normalizeVec(hist);
			
			float max = 0;
			
			for (float v : hist) {
				if (v > max) max = v;
			}
			return max;
		}
		
		@Override
		public float weighting(float[] hist) {
			return findMax(hist);
		}
	},
	/**
	 * Don't weight 
	 */
	NONE {
		@Override
		public float weighting(float[] hist) {
			return 1f;
		}
	};
	
	/**
	 * Get the weighting for a given histogram 
	 * @param hist the histogram
	 * @return the weighting
	 */
	public abstract float weighting(float[] hist);
	
	protected static void normalizeVec(float[] vec) {
		int i;
		float val, fac, sqlen = 0.0f;

		for (i = 0; i < vec.length; i++) {
			val = vec[i];
			sqlen += val * val;
		}
		fac = 1.0f / (float) Math.sqrt(sqlen);
		for (i = 0; i < vec.length; i++)
			vec[i] *= fac;
	}
}
