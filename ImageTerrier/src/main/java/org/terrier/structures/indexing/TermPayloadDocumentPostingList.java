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
 * The Original Code is TermPayloadDocumentPostingList.java
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
package org.terrier.structures.indexing;

import gnu.trove.THashMap;

import java.util.ArrayList;
import java.util.List;

import org.terrier.structures.TermPayloadCoordinator;
import org.terrier.structures.indexing.DocumentPostingList;

public class TermPayloadDocumentPostingList<PAYLOAD> extends DocumentPostingList {
	/** mapping term to blockids in this document */
	protected final THashMap<String, List<PAYLOAD>> term_blocks = new THashMap<String, List<PAYLOAD>>(AVG_DOCUMENT_UNIQUE_TERMS);
	
	/** number of blocks in this document. usually equal to document length, but perhaps less */
	protected int blockCount = 0;

	protected TermPayloadCoordinator<PAYLOAD> payloadConf;
	
	/** Instantiate a new block document posting list. Saves position information, but no fields */
	public TermPayloadDocumentPostingList(TermPayloadCoordinator<PAYLOAD> payloadConf) {
		super();
		this.payloadConf = payloadConf;
	} 
	
	/** Insert a term into this document with given position */
	public void insert(String t, PAYLOAD position) {
		insert(t);
		List<PAYLOAD> nn = null;
		if ((nn = term_blocks.get(t)) == null)
		{
			term_blocks.put(t, nn = new ArrayList<PAYLOAD>());
		}
		if (position != null) {
			nn.add(position);
		}
		blockCount++;
	}
	
	public PAYLOAD[] getPayloads(String term) {
		PAYLOAD [] rtr = payloadConf.makePayloadArray(term_blocks.get(term).size());
		int i = 0;
		for (PAYLOAD pos : term_blocks.get(term)) {
			rtr[i] = pos;
			i++;
		}
		return rtr;
	}

	/** returns the postings suitable to be written into the block direct index */
	@Override
	public int[][] getPostings() {
		throw new RuntimeException("Not implemented");
	}
}
