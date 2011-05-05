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
 * The Original Code is PositionSinglePassIndexer.java
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

import java.io.IOException;

import org.imageterrier.locfile.PositionSpec;
import org.imageterrier.locfile.QLFDocument;
import org.imageterrier.structures.PositionInvertedIndex;
import org.imageterrier.structures.PositionInvertedIndexInputStream;
import org.imageterrier.structures.indexing.PositionDocumentPostingList;
import org.imageterrier.structures.indexing.singlepass.PositionMemoryPostings;
import org.imageterrier.structures.indexing.singlepass.PositionPostingInRun;
import org.imageterrier.structures.postings.PositionIterablePosting;
import org.terrier.indexing.Document;
import org.terrier.indexing.ExtensibleSinglePassIndexer;
import org.terrier.structures.indexing.singlepass.FileRunIterator;
import org.terrier.structures.indexing.singlepass.FileRunIteratorFactory;
import org.terrier.structures.indexing.singlepass.PostingInRun;
import org.terrier.structures.indexing.singlepass.RunIterator;
import org.terrier.structures.indexing.singlepass.RunIteratorFactory;
import org.terrier.structures.indexing.singlepass.RunsMerger;
import org.terrier.terms.TermPipeline;
import org.terrier.utility.ArrayUtils;


public class PositionSinglePassIndexer extends ExtensibleSinglePassIndexer {
	protected PositionSpec positionSpec;
	protected int[] currentPosition;
	
	protected class PositionTermProcessor implements TermPipeline {
		@Override
		public void processTerm(String t) {
			//	null means the term has been filtered out (eg stopwords)
			if (t != null) {
				((PositionDocumentPostingList)termsInDocument).insert(t, currentPosition);
				numOfTokensInDocument++;
			}
		}
	}

	@Override
	protected TermPipeline getEndOfPipeline() {
		return new PositionTermProcessor();
	}

	public PositionSinglePassIndexer(String pathname, String prefix, PositionSpec positionSpec) {
		super(pathname, prefix);

		//delay the execution of init() if we are a parent class
		if (this.getClass() == PositionSinglePassIndexer.class) init();

		invertedIndexClass = PositionInvertedIndex.class.getName();
		invertedIndexInputStreamClass =  PositionInvertedIndexInputStream.class.getName();
		basicInvertedIndexPostingIteratorClass = PositionIterablePosting.class.getName();
		
		this.positionSpec = positionSpec;
	}

	@Override
	protected void createMemoryPostings(){
		mp = new PositionMemoryPostings(positionSpec.getPositionBits());
	}

	@Override
	protected void createDocumentPostings(){
		termsInDocument = new PositionDocumentPostingList();
	}

	@Override
	protected Class<PositionPostingInRun> getPostingInRunClass() {
		return PositionPostingInRun.class;
	}

	@Override
	protected void preProcess(Document doc, String term) {
		currentPosition = positionSpec.getPosition((QLFDocument<?>) doc);
	}
	
	@Override
	protected void finishedInvertedIndexBuild() {
		super.finishedInvertedIndexBuild();
		
		//now update the index with the extra info about
		currentIndex.setIndexProperty("positions.ordinates", positionSpec.getPositionBits().length+"");
		currentIndex.setIndexProperty("positions.nbits", ArrayUtils.join(positionSpec.getPositionBits(), ","));
		currentIndex.setIndexProperty("positions.mode", positionSpec.getMode().name());
		currentIndex.setIndexProperty("positions.lowerBounds", ArrayUtils.join(positionSpec.getLowerBounds(), ","));
		currentIndex.setIndexProperty("positions.upperBounds", ArrayUtils.join(positionSpec.getUpperBounds(), ","));
				
		try {
			currentIndex.flush();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	/*
	 * Hacked to use our "MyFileRunIteratorFactory" instead of a FileRunIteratorFactory
	 * (non-Javadoc)
	 * @see org.imageterrier.ExtensibleSinglePassIndexer#createRunMerger(java.lang.String[][])
	 */
	@Override
	protected void createRunMerger(String[][] files) throws Exception{
		merger = new RunsMerger(new MyFileRunIteratorFactory(files, getPostingInRunClass(), 0) {
			
		});
	}
	
	/** 
	 * @see FileRunIteratorFactory
	 * This version is hacked to set the correct parameters for the PositionPostingInRun class 
	 */
	class MyFileRunIteratorFactory extends RunIteratorFactory {
		/** type of the postings in the run data files */
		Class <? extends PostingInRun> postingClass;
		
		/** all the run filesnames */
		String[][] files;
		
		public MyFileRunIteratorFactory(String[][] _files, Class <? extends PostingInRun> _postingClass, int numFields)
		{
			super(numFields);
			files = _files;
			postingClass = _postingClass;
		}
		
		/** Return a RunIterator for the specified runNumber */
		@Override
		public RunIterator createRunIterator(int runNumber) throws Exception
		{
			FileRunIterator<PostingInRun> fri = new FileRunIterator<PostingInRun>(files[runNumber][0], files[runNumber][1], runNumber, postingClass, super.numberOfFields);
			((PositionPostingInRun)fri.current()).setPositionBits(positionSpec.getPositionBits());
			return fri;
		}
	}
}
