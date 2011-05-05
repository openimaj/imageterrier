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
 * The Original Code is ASPosting.java
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

public class ASPosting extends Posting {
	public void writeFirstDoc(final int doc, final int frequency, final int[] simulations) throws IOException {
		super.writeFirstDoc(doc, frequency);

		if (simulations.length != frequency) throw new RuntimeException("Mismatched frequency");
		write(simulations);
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
	public int insert(final int doc, final int freq, final int[] simulations) throws IOException{
		final int c = insert(doc, freq);

		if (simulations.length != freq) throw new RuntimeException("Mismatched frequency");		
		write(simulations);

		return c;
	}

	protected void write(int [] simulations) throws IOException {
		final int blockCount = simulations.length;

		int lastIndex = simulations[0];
		//docIds.writeGamma(lastIndex + 1 + 1);
		docIds.writeBinary(32, lastIndex);
		
		for (int i=1; i<blockCount; i++) {
			int newIndex = simulations[i];
			//docIds.writeGamma(newIndex - lastIndex + 1); //add one for zero distances from repeated terms
			docIds.writeBinary(32, newIndex);
			lastIndex = newIndex;
		}
	}
}

