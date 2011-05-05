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
 * The Original Code is ExtensibleSinglePassIndexer.java
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
import java.util.LinkedList;

import org.terrier.structures.FieldDocumentIndexEntry;
import org.terrier.structures.Index;
import org.terrier.structures.SimpleDocumentIndexEntry;
import org.terrier.structures.indexing.DocumentIndexBuilder;
import org.terrier.structures.indexing.singlepass.FileRunIteratorFactory;
import org.terrier.structures.indexing.singlepass.PostingInRun;
import org.terrier.structures.indexing.singlepass.RunsMerger;
import org.terrier.terms.TermPipeline;
import org.terrier.utility.ApplicationSetup;
import org.terrier.utility.FieldScore;

/**
 * Directly based on BasicSinglePassIndexer, with just a few modifications
 * to enable some extra hooks.
 * 
 * @author Roi Blanco
 * @author Jonathon Hare <jsh2@ecs.soton.ac.uk>
 */
public abstract class ExtensibleSinglePassIndexer extends BasicSinglePassIndexer {
	/* (non-Javadoc)
	 * @see org.terrier.indexing.BasicIndexer#getEndOfPipeline()
	 */
	@Override
	protected abstract TermPipeline getEndOfPipeline();

	/**
	 * Default constructor
	 * @param pathname String the path where the datastructures will 
	 * be created. This is assumed to be absolute.
	 * @param prefix String the prefix of the index, usually "data".
	 */
	public ExtensibleSinglePassIndexer(String pathname, String prefix) {
		super(pathname, prefix);
	}

	/**
	 * Get the class for storing postings in runs.
	 * @return PostingInRun Subclass of PostingInRun for this indexer
	 */
	protected abstract Class<? extends PostingInRun> getPostingInRunClass();
	
	/* (non-Javadoc)
	 * @see org.terrier.indexing.BasicSinglePassIndexer#createRunMerger(java.lang.String[][])
	 */
	@Override
	protected void createRunMerger(String[][] files) throws Exception{
		//modified to use getPostingInRunClass()
		merger = new RunsMerger(new FileRunIteratorFactory(files, getPostingInRunClass(), 0));
	}

	/* (non-Javadoc)
	 * @see org.terrier.indexing.BasicSinglePassIndexer#createMemoryPostings()
	 */
	@Override
	protected abstract void createMemoryPostings();
	
	/* (non-Javadoc)
	 * @see org.terrier.indexing.BasicIndexer#createDocumentPostings()
	 */
	@Override
	protected abstract void createDocumentPostings();
	
	/**
	 * Builds the inverted file and lexicon file for the given collections
	 * Loops through each document in each of the collections,
	 * extracting terms and pushing these through the Term Pipeline
	 * (e.g. stemming, stopping, lowercase, etc.).
	 * 
	 * JH NOTE: only one thing is modified from BasicSinglePassIndexer - 
	 * I've added a pre-processing operation before each term is passed
	 * to the pipeline
	 * 
	 *  @param collections Collection[] the collections to be indexed.
	 */
	@Override
	public void createInvertedIndex(Collection[] collections) {
		logger.info("Creating IF (no direct file)..");
		long startCollection, endCollection;
		fileNames = new LinkedList<String[]>();	
		numberOfDocuments = currentId = numberOfDocsSinceCheck = numberOfDocsSinceFlush = numberOfUniqueTerms = 0;
		numberOfTokens = numberOfPointers = 0;
		createMemoryPostings();
		currentIndex = Index.createNewIndex(path, prefix);
		docIndexBuilder = new DocumentIndexBuilder(currentIndex, "document");
		metaBuilder = createMetaIndexBuilder();
		
		emptyDocIndexEntry = (FieldScore.FIELDS_COUNT > 0) ? new FieldDocumentIndexEntry(FieldScore.FIELDS_COUNT) : new SimpleDocumentIndexEntry();
		
		MAX_DOCS_PER_BUILDER = Integer.parseInt(ApplicationSetup.getProperty("indexing.max.docs.per.builder", "0"));
		maxMemory = Long.parseLong(ApplicationSetup.getProperty("indexing.singlepass.max.postings.memory", "0"));
		final boolean boundaryDocsEnabled = BUILDER_BOUNDARY_DOCUMENTS.size() > 0;
		final int collections_length = collections.length;
		boolean stopIndexing = false;
		System.gc();
		memoryAfterFlush = runtime.freeMemory();
		logger.debug("Starting free memory: "+memoryAfterFlush/1000000+"M");

		for(int collectionNo = 0; ! stopIndexing && collectionNo < collections_length; collectionNo++)
		{
			Collection collection = collections[collectionNo];
			startCollection = System.currentTimeMillis();
			
			while(collection.nextDocument())
			{
				/* get the next document from the collection */
				//Document doc = collection./next();
				Document doc = collection.getDocument();
				if (doc == null)
					continue;
				//numberOfDocuments++;
				/* setup for parsing */
				createDocumentPostings();

				String term; //term we're currently processing
				numOfTokensInDocument = 0;
				//get each term in the document
				while (!doc.endOfDocument()) {

					if ((term = doc.getNextTerm())!=null && !term.equals("")) {
						termFields = doc.getFields();
						
						//perform pre-op
						preProcess(doc, term); //JH MOD
						
						/* pass term into TermPipeline (stop, stem etc) */
						pipeline_first.processTerm(term);
						/* the term pipeline will eventually add the term to this object. */
					}
					if (MAX_TOKENS_IN_DOCUMENT > 0 &&
							numOfTokensInDocument > MAX_TOKENS_IN_DOCUMENT)
						break;
				}
				//if we didn't index all tokens from document,
				//we need to get to the end of the document.
				while (!doc.endOfDocument())
					doc.getNextTerm();
				/* we now have all terms in the DocumentTree, so we save the document tree */
				try
				{
					if (termsInDocument.getDocumentLength() == 0)
					{	/* this document is empty, add the minimum to the document index */
						indexEmpty(doc.getAllProperties());
						if (IndexEmptyDocuments)
						{
							currentId++;
							numberOfDocuments++;
						}
					}
					else
					{	/* index this document */
						numberOfTokens += numOfTokensInDocument;
						indexDocument(doc.getAllProperties(), termsInDocument);
					}
				}
				catch (Exception ioe)
				{
					logger.error("Failed to index "+doc.getProperty("docno"),ioe);
				}

				if (MAX_DOCS_PER_BUILDER>0 && numberOfDocuments >= MAX_DOCS_PER_BUILDER)
				{
					stopIndexing = true;
					break;
				}

				if (boundaryDocsEnabled && BUILDER_BOUNDARY_DOCUMENTS.contains(doc.getProperty("docno")))
				{
					logger.warn("Document "+doc.getProperty("docno")+" is a builder boundary document. Boundary forced.");
					stopIndexing = true;
					break;
				}
				termsInDocument.clear();
			}
			
			try{
				forceFlush();
				endCollection = System.currentTimeMillis();
				long partialTime = (endCollection-startCollection)/1000;
				logger.info("Collection #"+collectionNo+ " took "+partialTime+ " seconds to build the runs for "+numberOfDocuments+" documents\n");
							
				
				
				docIndexBuilder.finishedCollections();
				if (FieldScore.FIELDS_COUNT > 0)
				{
					currentIndex.addIndexStructure("document-factory", FieldDocumentIndexEntry.Factory.class.getName(), "java.lang.String", "${index.inverted.fields.count}");
				}
				else
				{
					currentIndex.addIndexStructure("document-factory", SimpleDocumentIndexEntry.Factory.class.getName(), "", "");
				}
				metaBuilder.close();
				currentIndex.flush();
				
				logger.info("Merging "+fileNames.size()+" runs...");
				startCollection = System.currentTimeMillis();
				
				performMultiWayMerge();
				currentIndex.flush();
				endCollection = System.currentTimeMillis();
				logger.info("Collection #"+collectionNo+" took "+((endCollection-startCollection)/1000)+" seconds to merge\n ");
				logger.info("Collection #"+collectionNo+" total time "+( (endCollection-startCollection)/1000+partialTime));
				long secs = ((endCollection-startCollection)/1000);
				if (secs > 3600)
	                 logger.info("Rate: "+((double)numberOfDocuments/((double)secs/3600.0d))+" docs/hour");
			} catch (Exception e) {
				logger.error("Problem finishing index", e);
			}
		}
		finishedInvertedIndexBuild();
	}	

	
	/**
	 * Perform an operation before the term pipeline is initiated.
	 * 
	 * This could for example extract data and store in a field
	 * that the pipeline could access
	 * 
	 * @param doc Current document
	 * @param term Current term
	 */
	protected abstract void preProcess(Document doc, String term);

	/**
	 * Get the index currently being constructed by this indexer.
	 * This might be null if indexing hasn't commenced yet. It is
	 * useful for adding extra properties, etc to the index after 
	 * indexing is finished.
	 * 
	 * @return the current index
	 */
	public Index getCurrentIndex() {
		return currentIndex;
	}

	/**
	 * Delegate for HadoopIndexerMapper to intercept flushes
	 */
	protected SinglePassIndexerFlushDelegate flushDelegate;
	
	/**
	 * Set the flushDelegate
	 * @param flushDelegate
	 */
	protected void setFlushDelegate(SinglePassIndexerFlushDelegate flushDelegate) {
		this.flushDelegate = flushDelegate;
	}
	
	/**
	 * Get the flushDelegate
	 * @return the flushDelegate
	 */
	protected SinglePassIndexerFlushDelegate getFlushDelegate() {
		return flushDelegate;
	}
	
	/** 
	 * Force the indexer to flush everything and free memory.
	 * Either calls the super method, or passes to a delegate if 
	 * the flushDelegate is set.
	 * @see org.terrier.indexing.BasicSinglePassIndexer#forceFlush()
	 */
	@Override
	protected void forceFlush() throws IOException {
		if (flushDelegate == null) super.forceFlush();
		else flushDelegate.forceFlush();
	}
}

