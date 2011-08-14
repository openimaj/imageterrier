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
 * The Original Code is BasicSinglePassIndexer.java
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

import org.terrier.indexing.Document;
import org.terrier.indexing.ExtensibleSinglePassIndexer;
import org.terrier.structures.indexing.DocumentPostingList;
import org.terrier.structures.indexing.singlepass.MemoryPostings;
import org.terrier.structures.indexing.singlepass.PostingInRun;
import org.terrier.structures.indexing.singlepass.SimplePostingInRun;
import org.terrier.terms.TermPipeline;

/**
 * This class is simply a version of org.terrier.indexing.BasicSinglePassIndexer
 * that is built on top of an ExtensibleSinglePassIndexer.
 * 
 * @author Jonathon Hare <jsh2@ecs.soton.ac.uk>
 *
 */
public class BasicSinglePassIndexer extends ExtensibleSinglePassIndexer {
	/** 
	 * This class implements an end of a TermPipeline that adds the
	 * term to the DocumentTree. This TermProcessor does NOT have field
	 * support.
	 */
	protected class BasicTermProcessor implements TermPipeline {
		//term pipeline implementation
		@Override
		public void processTerm(String term)
		{
			/* null means the term has been filtered out (eg stopwords) */
			if (term != null)
			{
				//add term to thingy tree
				termsInDocument.insert(term);
				numOfTokensInDocument++;
			}
		}

		@Override
		public boolean reset() {
			return true;
		}
	}
	
	public BasicSinglePassIndexer(String pathname, String prefix) {
		super(pathname, prefix);
		
		//delay the execution of init() if we are a parent class
		if (this.getClass() == BasicSinglePassIndexer.class) init();
	}
	
	@Override
	protected TermPipeline getEndOfPipeline() {
		return new BasicTermProcessor();
	}

	@Override
	protected Class<? extends PostingInRun> getPostingInRunClass() {
		return SimplePostingInRun.class;
	}

	@Override
	protected void createMemoryPostings() {
		mp = new MemoryPostings();
	}

	@Override
	protected void createDocumentPostings() {
		termsInDocument = new DocumentPostingList();
	}

	@Override
	protected void preProcess(Document doc, String term) {
		// Do nothing
	}
}
