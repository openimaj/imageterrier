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
 * The Original Code is PositionPosting.java
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
package org.imageterrier.structures.indexing.singlepass;

import java.io.IOException;

import org.terrier.structures.indexing.singlepass.Posting;

public class PositionPosting extends Posting {
	/**
	 * This stores how many bits were used to encode each
	 * of the position elements. The length of this array
	 * is the number of position ordinates per posting.
	 */
	protected int [] positionBits;
	
	public PositionPosting(int [] positionBits) {
		this.positionBits = positionBits;
	}
	
	public void writeFirstDoc(final int doc, final int frequency, final int[][] positions) throws IOException {
		super.writeFirstDoc(doc, frequency);

		if (positions.length != frequency) throw new RuntimeException("Mismatched frequency");
		write(positions);
	}

	/**
	 * Inserts a new document in the posting list. Document insertions must be done
	 * in order.  
	 * @param doc the document identifier.
	 * @param freq the frequency of the term in the document.
	 * @param blockids the blockids for all the term
	 * @return the updated term frequency.
	 * @throws IOException if and I/O error occurs.
	 */
	public int insert(final int doc, final int freq, final int[][] positions) throws IOException{
		final int c = insert(doc, freq);

		if (positions.length != freq) throw new RuntimeException("Mismatched frequency");
		write(positions);

		return c;
	}

	protected void write(int [][] positions) throws IOException {
		final int blockCount = positions.length;
		
		for (int i=0; i<blockCount; i++) {
			for (int o=0; o<positionBits.length; o++) 
				docIds.writeBinary(positionBits[o], positions[i][o]);
		}
	}
}

