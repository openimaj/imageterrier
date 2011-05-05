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
 * The Original Code is TermPayloadPosting.java
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
package org.terrier.structures.indexing.singlepass;

import java.io.IOException;

import org.terrier.structures.TermPayloadCoordinator;
import org.terrier.structures.indexing.singlepass.Posting;

public class TermPayloadPosting<PAYLOAD> extends Posting {
	/**
	 * This stores info about the payload
	 */
	protected TermPayloadCoordinator<PAYLOAD> payloadConf;
	
	public TermPayloadPosting(TermPayloadCoordinator<PAYLOAD> payloadConf) {
		this.payloadConf = payloadConf;
	}
	
	public void writeFirstDoc(final int doc, final int frequency, final PAYLOAD[] payloads) throws IOException {
		super.writeFirstDoc(doc, frequency);

		if (payloads.length != frequency) throw new RuntimeException("Mismatched frequency");
		write(payloads);
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
	public int insert(final int doc, final int freq, final PAYLOAD[] payloads) throws IOException{
		final int c = insert(doc, freq);

		if (payloads.length != freq) throw new RuntimeException("Mismatched frequency");
		write(payloads);

		return c;
	}

	protected void write(final PAYLOAD[] payloads) throws IOException {
		for (PAYLOAD payload : payloads) {
			payloadConf.writePayloadInMem(docIds, payload);
		}
	}
}

