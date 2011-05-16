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
 * The Original Code is QLFDocumentTest.java
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
package org.imageterrier.locfile;

import java.io.IOException;

import org.junit.Test;
import org.openimaj.feature.local.list.MemoryLocalFeatureList;
import org.openimaj.image.feature.local.keypoints.quantised.QuantisedKeypoint;

/**
 * Tests for QLFDocument
 * 
 * @author Jonathon Hare <jsh2@ecs.soton.ac.uk>
 *
 */
public class QLFDocumentTest {
	/**
	 * Test nearest-neighbour method
	 * @throws IOException
	 */
	@Test
	public void testNN() throws IOException {
		long t1 = System.currentTimeMillis();
		
		QLFDocument<QuantisedKeypoint> doc = new QLFDocument<QuantisedKeypoint>(MemoryLocalFeatureList.read(getClass().getResourceAsStream("/org/imageterrier/siftintensity/ukbench00000.jpg.loc"), QuantisedKeypoint.class), "00000", null);
		while (!doc.endOfDocument()) {
			doc.getNextTerm();
			doc.getCurrentNearestNeighbourTIdsKD(15);
		}
		
		long t2 = System.currentTimeMillis();
		
		System.out.println(t2-t1);
	}
	
	/**
	 * Test nearest-neighbour method
	 * @throws IOException
	 */
	@Test
	public void testNN2() throws IOException {
		long t1 = System.currentTimeMillis();
		
		QLFDocument<QuantisedKeypoint> doc = new QLFDocument<QuantisedKeypoint>(MemoryLocalFeatureList.read(getClass().getResourceAsStream("/org/imageterrier/siftintensity/ukbench00000.jpg.loc"), QuantisedKeypoint.class), "00000", null);
		while (!doc.endOfDocument()) {
			doc.getNextTerm();
			doc.getCurrentNearestNeighbourTIds(15);
		}
		
		long t2 = System.currentTimeMillis();
		
		System.out.println(t2-t1);
	}
}
