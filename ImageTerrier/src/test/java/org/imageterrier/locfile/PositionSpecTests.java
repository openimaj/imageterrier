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
 * The Original Code is PositionSpecTests.java
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

import static org.junit.Assert.assertEquals;

import org.imageterrier.locfile.PositionSpec;
import org.imageterrier.locfile.PositionSpec.PositionSpecMode;
import org.junit.Test;

public class PositionSpecTests {
	@Test
	public void testEncode() {
		assertEquals(0, PositionSpec.PositionSpecMode.NONE.encode(0, 0, 10, 8));
		assertEquals(255, PositionSpec.PositionSpecMode.NONE.encode(10, 0, 10, 8));
		assertEquals(127, PositionSpec.PositionSpecMode.NONE.encode(0, -10, 10, 8));
	}
	
	@Test
	public void testDecode() {
		assertEquals(0, PositionSpec.PositionSpecMode.NONE.decode(0, 0, 10, 8), 0.05);
		assertEquals(10, PositionSpec.PositionSpecMode.NONE.decode(255, 0, 10, 8), 0.05);
		assertEquals(0, PositionSpec.PositionSpecMode.NONE.decode(127, -10, 10, 8), 0.05);
	}
	
	@Test
	public void testDecodeString() {
		PositionSpec spec = new PositionSpec(PositionSpecMode.SPATIAL, new int[] {2,3}, new double[] {0,1}, new double[]{10,11});
		
		assertEquals(spec, PositionSpec.decode(spec.toString()));
	}
}
