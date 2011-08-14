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
 * The Original Code is TermPayloadSinglePassIndexer.java
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
package org.terrier.indexing;

import java.io.IOException;

import org.terrier.indexing.Document;
import org.terrier.structures.InvertedIndex;
import org.terrier.structures.TermPayloadCoordinator;
import org.terrier.structures.TermPayloadInvertedIndex;
import org.terrier.structures.TermPayloadInvertedIndexInputStream;
import org.terrier.structures.indexing.TermPayloadDocumentPostingList;
import org.terrier.structures.indexing.singlepass.FileRunIterator;
import org.terrier.structures.indexing.singlepass.FileRunIteratorFactory;
import org.terrier.structures.indexing.singlepass.PostingInRun;
import org.terrier.structures.indexing.singlepass.RunIterator;
import org.terrier.structures.indexing.singlepass.RunIteratorFactory;
import org.terrier.structures.indexing.singlepass.RunsMerger;
import org.terrier.structures.indexing.singlepass.TermPayloadMemoryPostings;
import org.terrier.structures.indexing.singlepass.TermPayloadPostingInRun;
import org.terrier.structures.postings.TermPayloadIterablePosting;
import org.terrier.terms.TermPipeline;

/**
 * A single-pass indexer capable of adding term payload information
 * to each posting. 
 * 
 * @see BasicSinglePassIndexer
 * 
 * @author Jonathon Hare <jsh2@ecs.soton.ac.uk>
 *
 * @param <PAYLOAD> the payload type
 */
public class TermPayloadSinglePassIndexer<PAYLOAD> extends ExtensibleSinglePassIndexer {
	protected TermPayloadCoordinator<PAYLOAD> payloadConf;
	protected PAYLOAD currentPayload;
	
	protected class PayloadTermProcessor implements TermPipeline {
		@SuppressWarnings("unchecked")
		@Override
		public void processTerm(String t) {
			//	null means the term has been filtered out (eg stopwords)
			if (t != null) {
				((TermPayloadDocumentPostingList<PAYLOAD>)termsInDocument).insert(t, currentPayload);
				numOfTokensInDocument++;
			}
		}

		@Override
		public boolean reset() {
			return true;
		}
	}

	@Override
	protected TermPipeline getEndOfPipeline() {
		return new PayloadTermProcessor();
	}

	protected TermPayloadSinglePassIndexer(String pathname, String prefix, TermPayloadCoordinator<PAYLOAD> payloadConf, Class<? extends InvertedIndex> idxClz) {
		super(pathname, prefix);

		//delay the execution of init() if we are a parent class
		if (this.getClass() == TermPayloadSinglePassIndexer.class) init();

		invertedIndexClass = idxClz.getName();
		invertedIndexInputStreamClass =  TermPayloadInvertedIndexInputStream.class.getName();
		basicInvertedIndexPostingIteratorClass = TermPayloadIterablePosting.class.getName();
		
		this.payloadConf = payloadConf;
	}

	/**
	 * Constructs an instance of a TermPayloadSinglePassIndexer, using the given path name
	 * for storing the data structures and TermPayloadCoordinator for controlling what and how 
	 * payloads will be written to the index.
	 * @param pathname String the path where the data structures will be created. This is assumed to be
	 * absolute.
	 * @param prefix String the prefix of the index, usually "data".
	 * @param payloadConf the payload coordinator
	 */
	public TermPayloadSinglePassIndexer(String pathname, String prefix, TermPayloadCoordinator<PAYLOAD> payloadConf) {
		this(pathname, prefix, payloadConf, TermPayloadInvertedIndex.class);
	}

	@Override
	protected void createMemoryPostings(){
		mp = new TermPayloadMemoryPostings<PAYLOAD>(payloadConf);
	}

	@Override
	protected void createDocumentPostings(){
		termsInDocument = new TermPayloadDocumentPostingList<PAYLOAD>(payloadConf);
	}

	@SuppressWarnings("rawtypes")
	@Override
	protected Class<TermPayloadPostingInRun> getPostingInRunClass() {
		return TermPayloadPostingInRun.class;
	}

	@Override
	protected void preProcess(Document doc, String term) {
		currentPayload = payloadConf.createPayload(doc, term);
	}
	
	@Override
	protected void finishedInvertedIndexBuild() {
		super.finishedInvertedIndexBuild();
		
		//now update the index with the extra info about
		payloadConf.saveConfiguration(this.currentIndex);
				
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
	 * This version is hacked to set the correct PayloadConfiguration parameters for the PayloadPostingInRun class 
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
		@SuppressWarnings("unchecked")
		@Override
		public RunIterator createRunIterator(int runNumber) throws Exception
		{
			FileRunIterator<PostingInRun> fri = new FileRunIterator<PostingInRun>(files[runNumber][0], files[runNumber][1], runNumber, postingClass, super.numberOfFields);
			((TermPayloadPostingInRun<PAYLOAD>)fri.current()).setPayloadConfig(payloadConf);
			return fri;
		}
	}
}
