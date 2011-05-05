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
 * The Original Code is AffineScoreModifier.java
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
import org.openimaj.math.geometry.transforms.AffineTransformModel;
import org.openimaj.math.model.Model;
import org.terrier.matching.dsms.DocumentScoreModifier;


public class AffineScoreModifier extends AbstractRANSACGeomModifier implements DocumentScoreModifier {
	public static final String MODEL_TOLERANCE = "AffineScoreModifier.model_tolerance";

	@Override
	public String getName() {
		return "AffineScoreModifier";
	}
	
	@Override
	public AffineScoreModifier clone() {
		//new one, as we don't have state
		return new AffineScoreModifier();
	}

	@Override
	public Model<Point2d, Point2d> makeModel() {
		float tol = ApplicationSetupUtils.getProperty(MODEL_TOLERANCE, 10.0f);
		return new AffineTransformModel(tol);
	}
}
