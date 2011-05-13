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
 * The Original Code is PositionSinglePassIndexer.java
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
package org.imageterrier.indexing;

import org.imageterrier.locfile.PositionSpec;
import org.imageterrier.structures.PositionInvertedIndex;
import org.imageterrier.termpayload.PositionTermPayloadCoordinator;
import org.terrier.indexing.TermPayloadSinglePassIndexer;


public class PositionSinglePassIndexer extends TermPayloadSinglePassIndexer<int[]> {
	public PositionSinglePassIndexer(String pathname, String prefix, PositionSpec positionSpec) {
		super(pathname, prefix, new PositionTermPayloadCoordinator(positionSpec), PositionInvertedIndex.class);
		if (this.getClass() == PositionSinglePassIndexer.class) init();
	}
}
