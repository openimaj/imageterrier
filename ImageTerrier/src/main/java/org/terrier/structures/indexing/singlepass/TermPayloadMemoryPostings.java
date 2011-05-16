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
 * The Original Code is TermPayloadMemoryPostings.java
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
import org.terrier.structures.indexing.DocumentPostingList;
import org.terrier.structures.indexing.TermPayloadDocumentPostingList;
import org.terrier.structures.indexing.singlepass.MemoryPostings;

/**
 * Postings in memory with payloads
 * 
 * @author Jonathon Hare <jsh2@ecs.soton.ac.uk>
 *
 * @param <PAYLOAD> the payload type
 */
public class TermPayloadMemoryPostings<PAYLOAD> extends MemoryPostings { 
	/**
	 * This stores info about the payload
	 */
	protected TermPayloadCoordinator<PAYLOAD> payloadConf;
	
	/**
	 * Construct the memory postings with the sepecified coordinator.
	 * @param payloadConf the coordinator.
	 */
	public TermPayloadMemoryPostings(TermPayloadCoordinator<PAYLOAD> payloadConf) {
		this.payloadConf = payloadConf;
	}
	
	@Override
	public void addTerms(DocumentPostingList _docPostings, int docid) throws IOException {  	 
		@SuppressWarnings("unchecked")
		TermPayloadDocumentPostingList<PAYLOAD> docPostings = (TermPayloadDocumentPostingList<PAYLOAD>)  _docPostings;
		
		for (String term : docPostings.termSet()) 	 
			add(term, docid, docPostings.getFrequency(term), docPostings.getPayloads(term)); 	 
	}

	/**
	 * Add a new posting for the specified term-document pair. The number of payloads
	 * should equal the frequency.
	 * @param term the term.
	 * @param doc the document that the term occurred in.
	 * @param frequency the number of times the term occurred in the document.
	 * @param payloads the payloads for the term.
	 * @throws IOException
	 */
	@SuppressWarnings("unchecked")
	public void add(String term, int doc, int frequency, PAYLOAD[] payloads) throws IOException {
		TermPayloadPosting<PAYLOAD> post;
		
		if((post = (TermPayloadPosting<PAYLOAD>)postings.get(term)) != null) {		
			post.insert(doc, frequency, payloads);
			int tf = post.getTF();
			// Update the max size
			if(maxSize < tf) maxSize = tf; 
		}
		else{
			post = new TermPayloadPosting<PAYLOAD>(payloadConf);
			post.writeFirstDoc(doc, frequency, payloads);			
			postings.put(term,post);
		}
		numPointers++;
	}
}
