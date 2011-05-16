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
 * The Original Code is NNInvertedIndex.java
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
package org.imageterrier.structures;

import java.io.IOException;

import org.terrier.structures.DocumentIndex;
import org.terrier.structures.Index;
import org.terrier.structures.IndexConfigurable;
import org.terrier.structures.TermPayloadInvertedIndex;
import org.terrier.structures.postings.IterablePosting;

/**
 * An inverted index that also stores the spatial nearest-neighbour
 * term-ids of each term occurrence in the style of Sivic and
 * Zisserman's "VideoGoogle" paper.
 * 
 * @author Jonathon Hare <jsh2@ecs.soton.ac.uk>
 */
public class NNInvertedIndex extends TermPayloadInvertedIndex<int[]> implements IndexConfigurable {
	/**
	 * Construct the inverted index.
	 * @param index the index to which this inverted index belongs
	 * @param structureName the name of the inverted index structure
	 * @param _doi the document index
	 * @param postingClass the class to use for iterating postings
	 * @throws IOException
	 */
	public NNInvertedIndex(Index index, String structureName, DocumentIndex _doi, Class<? extends IterablePosting> postingClass) throws IOException {
		super(index, structureName, _doi, postingClass);
	}
}
