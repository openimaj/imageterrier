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
 * The Original Code is PositionMemoryPostings.java
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


import org.imageterrier.structures.indexing.PositionDocumentPostingList;
import org.terrier.structures.indexing.DocumentPostingList;
import org.terrier.structures.indexing.singlepass.MemoryPostings;


public class PositionMemoryPostings extends MemoryPostings { 
	/**
	 * This stores how many bits were used to encode each
	 * of the position elements. The length of this array
	 * is the number of position ordinates per posting.
	 */
	protected int [] positionBits;
	
	public PositionMemoryPostings(int [] positionBits) {
		this.positionBits = positionBits;
	}
	
	@Override
	public void addTerms(DocumentPostingList _docPostings, int docid) throws IOException {  	 
		PositionDocumentPostingList docPostings = (PositionDocumentPostingList)  _docPostings; 	 
		for (String term : docPostings.termSet()) 	 
			add(term, docid, docPostings.getFrequency(term), docPostings.getPositions(term)); 	 
	}

	public void add(String term, int doc, int frequency, int[][] positions) throws IOException{
		PositionPosting post;
		
		if((post =(PositionPosting) postings.get(term)) != null) {		
			post.insert(doc, frequency, positions);
			int tf = post.getTF();
			// Update the max size
			if(maxSize < tf) maxSize = tf; 
		}
		else{
			post = new PositionPosting(positionBits);
			post.writeFirstDoc(doc, frequency, positions);			
			postings.put(term,post);
		}
		numPointers++;
	}
}
