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
 * The Original Code is HomographyScoreModifier.java
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

import org.imageterrier.basictools.ApplicationSetupUtils;
import org.openimaj.math.geometry.point.Point2d;
import org.openimaj.math.geometry.transforms.FundamentalModel;
import org.openimaj.math.model.EstimatableModel;
import org.terrier.matching.dsms.DocumentScoreModifier;

/**
 * A score modifier that can be applied to a position index with x and y
 * coordinates, and works by fitting a {@link FundamentalModel} to the matching
 * visual term pairs.
 * 
 * If a fundamental matrix that fits the point pairs is found the document score
 * is set to the number of inliers, otherwise the score is zero.
 * 
 * @author Jonathon Hare <jsh2@ecs.soton.ac.uk>
 */
public class FundamentalScoreModifier extends AbstractRANSACGeomModifier implements DocumentScoreModifier {
	/**
	 * The distance in pixels that points are allowed to move from their
	 * predicted position and still be considered a match.
	 */
	public static final String MODEL_TOLERANCE = "FundamentalScoreModifier.model_tolerance";

	@Override
	public String getName() {
		return "FundamentalScoreModifier";
	}

	@Override
	public FundamentalScoreModifier clone() {
		// new one, as we don't have state
		return new FundamentalScoreModifier();
	}

	@Override
	public EstimatableModel<Point2d, Point2d> makeModel() {
		return new FundamentalModel();
	}

	@Override
	protected double getTolerance() {
		return ApplicationSetupUtils.getProperty(MODEL_TOLERANCE, 0.1f);
	}
}
