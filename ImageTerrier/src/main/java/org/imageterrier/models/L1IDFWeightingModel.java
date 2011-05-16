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
 * The Original Code is L1IDFWeightingModel.java
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
package org.imageterrier.models;

import org.terrier.matching.models.WeightingModel;

/**
 * A {@link WeightingModel} that calculates the L1 distance 
 * between IDF weighted query and target vectors. 
 * 
 * @author Jonathon Hare <jsh2@ecs.soton.ac.uk>
 */
public class L1IDFWeightingModel extends WeightingModel {
	private static final long serialVersionUID = 1L;
	
	/** model name */
	private static final String name = "L1IDF_DISTANCE";
	
	@Override
	public String getInfo() {
		return name;
	}

	@Override
	public double score(double tf, double docLength) {
		double w = Math.log(this.numberOfDocuments / this.documentFrequency); //docFreq or termFreq?
		
		double dtf = w * tf/docLength;
		double qtf = w * keyFrequency * c; //Note: c must be set to correctly weight the query terms...
		
		double score = Math.abs(qtf - dtf) - qtf - dtf;
		return -1 * score;
	}

	@Override
	public double score(double tf, double docLength, double nT, double F_t, double keyFrequency) {
//		double dtf = tf/docLength;
//		double score = Math.abs(keyFrequency - dtf) - keyFrequency - dtf;
//		return score;
		throw new UnsupportedOperationException();
	}
}
