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
 * The Original Code is ASSinglePassIndexer.java
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


import org.imageterrier.locfile.QLFDocument;
import org.imageterrier.structures.ASInvertedIndex;
import org.imageterrier.structures.ASInvertedIndexInputStream;
import org.imageterrier.structures.indexing.ASDocumentPostingList;
import org.imageterrier.structures.indexing.singlepass.ASMemoryPostings;
import org.imageterrier.structures.indexing.singlepass.ASPostingInRun;
import org.imageterrier.structures.postings.ASIterablePosting;
import org.terrier.indexing.Document;
import org.terrier.indexing.ExtensibleSinglePassIndexer;
import org.terrier.terms.TermPipeline;


public class ASSinglePassIndexer extends ExtensibleSinglePassIndexer {

	protected class ASTermProcessor implements TermPipeline {
		@Override
		public void processTerm(String t) {
			//	null means the term has been filtered out (eg stopwords)
			if (t != null) {
				((ASDocumentPostingList)termsInDocument).insert(t, simulationIndex);
				numOfTokensInDocument++;
			}
		}
	}

	protected int simulationIndex;

	@Override
	protected TermPipeline getEndOfPipeline() {
		return new ASTermProcessor();
	}

	public ASSinglePassIndexer(String pathname, String prefix) {
		super(pathname, prefix);

		//delay the execution of init() if we are a parent class
		if (this.getClass() == ASSinglePassIndexer.class) init();

		invertedIndexClass = ASInvertedIndex.class.getName();
		invertedIndexInputStreamClass =  ASInvertedIndexInputStream.class.getName();
		basicInvertedIndexPostingIteratorClass = ASIterablePosting.class.getName();
	}
	
	@Override
	protected void createMemoryPostings(){
		mp = new ASMemoryPostings();
	}

	@Override
	protected void createDocumentPostings(){
		termsInDocument = new ASDocumentPostingList();
	}

	@Override
	protected Class<ASPostingInRun> getPostingInRunClass() {
		return ASPostingInRun.class;
	}

	@Override
	protected void preProcess(Document doc, String term) {
		simulationIndex = ((QLFDocument<?>)doc).getSimulationIndex();
	}
}
