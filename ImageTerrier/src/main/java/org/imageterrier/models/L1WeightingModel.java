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
 * The Original Code is L1WeightingModel.java
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
 * between unweighted query and target vectors. 
 * 
 * @author Jonathon Hare <jsh2@ecs.soton.ac.uk>
 */
public class L1WeightingModel extends WeightingModel {
	private static final long serialVersionUID = 1L;
	
	/** model name */
	private static final String name = "UNWEIGHTED_L1_DISTANCE";
	
	@Override
	public String getInfo() {
		return name;
	}

	@Override
	public double score(double tf, double docLength) {
		double dtf = tf/docLength;
		double qtf = keyFrequency * c; //Note: c must be set to correctly weight the query terms...
		/*
		 * This comes from doing this:
		 * L1( q , d )  = sum([abs(q[i] - d[i]) for i in terms])
		 * 
		 * some terms can be zero in the document or the query so we can write:
		 * termsInQ = [x for x in terms if q[x] != 0]
		 * termsInD = [x for x in terms if d[x] != 0]
		 * 
		 * L1(q,d) = sum([d[i] for i in (terms - termsInQ)]) + sum([q[i] for i in (terms - termsInD)]) + sum([abs(q[i] - d[i]) for i in union(termsInQ,termsInD)])
		 * 
		 * So if you take a d[i] for every term in Q, and take a q[i] for every term in D you can replace the first two terms with the lengths of q and d.
		 * 
		 * L1(q,d) = lengthOfQ + lengthOfD + sum([abs(q[i] - d[i]) - q[i] - d[i] for i in union(termsInQ,termsInD)])
		 * 
		 * But as we've normalised Q and D you end up with:
		 * 
		 * 2 + sum([abs(q[i] - d[i]) - q[i] - d[i] for i in union(termsInQ,termsInD)]) 
		 * And we are guaranteed to get to this function only if this document contains this term and this query contains this term. Our value is also between 2 and minus some number, 
		 * 
		 * 2 is good so we give it a lower score using * -1
		 * 
		 */
		double score = Math.abs(qtf - dtf) - qtf - dtf;
		return -1 * score;
	}

	@Override
	public double score(double tf, double docLength, double nT, double F_t, double keyFrequency) {
		throw new UnsupportedOperationException();
	}
}
