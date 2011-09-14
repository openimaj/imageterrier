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
 * The Original Code is HadoopIndexerMapper.java
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

import java.io.DataOutputStream;
import java.io.IOException;

import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.lib.map.MultithreadedMapper;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.imageterrier.hadoop.fs.TerrierHDFSAdaptor;
import org.imageterrier.hadoop.mapreduce.PositionAwareSplitWrapper;
import org.terrier.structures.FieldDocumentIndexEntry;
import org.terrier.structures.Index;
import org.terrier.structures.SimpleDocumentIndexEntry;
import org.terrier.structures.indexing.CompressingMetaIndexBuilder;
import org.terrier.structures.indexing.DocumentIndexBuilder;
import org.terrier.structures.indexing.MetaIndexBuilder;
import org.terrier.structures.indexing.singlepass.hadoop.MapEmittedPostingList;
import org.terrier.structures.indexing.singlepass.hadoop.NewHadoopRunWriter;
import org.terrier.structures.indexing.singlepass.hadoop.NewSplitEmittedTerm;
import org.terrier.utility.ApplicationSetup;
import org.terrier.utility.FieldScore;
import org.terrier.utility.Files;
import org.terrier.utility.io.WrappedIOException;


public abstract class HadoopIndexerMapper<VALUEIN> extends Mapper<Text, VALUEIN, NewSplitEmittedTerm, MapEmittedPostingList> implements SinglePassIndexerFlushDelegate {
	/** the underlying indexer object */
	protected ExtensibleSinglePassIndexer proxyIndexer;
	
	/** the current map context */
	protected Context currentContext;
	
	/** Current map number */
	protected String mapTaskID;

	/** the split number of the map context */
	protected int splitnum;
	
	/** How many flushes have we made */
	protected int flushNo;
	
	/** OutputStream for the the data on the runs (runNo, flushes etc) */
	protected DataOutputStream runData;
	
	static enum Counters {  
		INDEXED_DOCUMENTS, INDEXED_EMPTY_DOCUMENTS, INDEXER_FLUSHES, INDEXED_TOKENS, INDEXED_POINTERS;
	};
	
	/**
	 * This method returns an instance of an indexer, possibly
	 * using parameters extracted from the context. This is only called
	 * once at the beginning of the mapper setup, so is also a good
	 * place to do any extra initialisation.
	 * 
	 * @param context
	 * @return
	 * @throws IOException 
	 */
	protected abstract ExtensibleSinglePassIndexer createIndexer(Context context) throws IOException;
	
	protected String getThreadIndex(Context context) {
		try {
			if (((Class<?>)context.getMapperClass()) == ((Class<?>)(MultithreadedMapper.class))) {
				Thread t = Thread.currentThread();
				return "" + t.getId();
			}
		} catch (ClassNotFoundException e) {
			System.err.println("Shouldn't get here!");
			e.printStackTrace(System.err);
		}
		return "";
	}
	
	@Override
	protected void setup(Context context) throws IOException, InterruptedException {
		TerrierHDFSAdaptor.initialiseHDFSAdaptor(context.getConfiguration());
		
		proxyIndexer = createIndexer(context);
		
		currentContext = context;
		splitnum = ((PositionAwareSplitWrapper<?>)context.getInputSplit()).getSplitIndex();
		
		proxyIndexer.setFlushDelegate(this);
		
		Path indexDestination = FileOutputFormat.getWorkOutputPath(context);
		indexDestination.getFileSystem(context.getConfiguration()).mkdirs(indexDestination); 
		
		mapTaskID = context.getTaskAttemptID().getTaskID().toString() + getThreadIndex(context);
		proxyIndexer.currentIndex = Index.createNewIndex(indexDestination.toString(), mapTaskID);
		proxyIndexer.maxMemory = Long.parseLong(ApplicationSetup.getProperty("indexing.singlepass.max.postings.memory", "0"));

		//during reduce, we dont want to load indices into memory, as we only use them as streams
		proxyIndexer.currentIndex.setIndexProperty("index.preloadIndices.disabled", "true");
		runData = new DataOutputStream(Files.writeFileStream(new Path(indexDestination, mapTaskID+".runs").toString()));
		runData.writeUTF(mapTaskID);
		
		proxyIndexer.createMemoryPostings();
		proxyIndexer.docIndexBuilder = new DocumentIndexBuilder(proxyIndexer.currentIndex, "document");
		proxyIndexer.metaBuilder = createMetaIndexBuilder();
		proxyIndexer.emptyDocIndexEntry = (FieldScore.FIELDS_COUNT > 0) ? new FieldDocumentIndexEntry(FieldScore.FIELDS_COUNT) : new SimpleDocumentIndexEntry();
	}

	protected MetaIndexBuilder createMetaIndexBuilder() {
		final String[] forwardMetaKeys = ApplicationSetup.getProperty("indexer.meta.forward.keys", "docno").split("\\s*,\\s*");
		final int[] metaKeyLengths = Indexer.parseInts(ApplicationSetup.getProperty("indexer.meta.forward.keylens", "20").split("\\s*,\\s*"));
		//no reverse metadata during main indexing, pick up as separate job later
		return new CompressingMetaIndexBuilder(proxyIndexer.currentIndex, forwardMetaKeys, metaKeyLengths, new String[0]);
	}
	
	/**
	 * This method transforms the Hadoop record to a Terrier document
	 * @param value
	 * @return
	 * @throws IOException 
	 */
	protected abstract Document recordToDocument(Text key, VALUEIN value) throws IOException;
	
	/**
	 * Map processes a single document. Stores the terms in the document along with the posting list
	 * until memory is full or all documents in this map have been processed then writes then to
	 * the output collector.  
	 * @param key Wrapper for Document Identifier
	 * @param value The document itself (a serialized list of quantised features)
	 * @param context The mapper context 
	 * @throws IOException
	 */
	@Override
	protected void map(Text key, VALUEIN value, Context context) throws IOException, InterruptedException {
		final String docno = key.toString();
		context.setStatus("Currently indexing "+docno);
		
		final Document doc = recordToDocument(key, value);
		if(doc==null) return;
		
		indexDocument(doc, context);
		context.getCounter(Counters.INDEXED_DOCUMENTS).increment(1);
	}
	
	protected void indexDocument(final Document doc, Context context) throws IOException {
		/* setup for parsing */
		proxyIndexer.createDocumentPostings();
		
		String term;//term we're currently processing
		proxyIndexer.numOfTokensInDocument = 0;
		
		//get each term in the document
		while (!doc.endOfDocument()) {
			context.progress();
			
			if ((term = doc.getNextTerm())!=null && !term.equals("")) {
				proxyIndexer.termFields = doc.getFields();
				
				//perform pre-op
				proxyIndexer.preProcess(doc, term); //JH MOD
				
				/* pass term into TermPipeline (stop, stem etc) */
				proxyIndexer.pipeline_first.processTerm(term);
				/* the term pipeline will eventually add the term to this object. */
			}
			if (proxyIndexer.MAX_TOKENS_IN_DOCUMENT > 0 &&
					proxyIndexer.numOfTokensInDocument > proxyIndexer.MAX_TOKENS_IN_DOCUMENT)
				break;
		}
		
		//if we didn't index all tokens from document,
		//we need tocurrentId get to the end of the document.
		while (!doc.endOfDocument()){
			doc.getNextTerm();
		}
		/* we now have all terms in the DocumentTree, so we save the document tree */
		if (proxyIndexer.termsInDocument.getDocumentLength() == 0)
		{	/* this document is empty, add the minimum to the document index */
			proxyIndexer.indexEmpty(doc.getAllProperties());
			if (proxyIndexer.IndexEmptyDocuments)
			{
				proxyIndexer.currentId++;
				proxyIndexer.numberOfDocuments++;
				context.getCounter(Counters.INDEXED_EMPTY_DOCUMENTS).increment(1);
			}
		}
		else
		{	/* index this document */
			try{
				proxyIndexer.numberOfTokens += proxyIndexer.numOfTokensInDocument;
				proxyIndexer.indexDocument(doc.getAllProperties(), proxyIndexer.termsInDocument);
				
				context.getCounter(Counters.INDEXED_TOKENS).increment(proxyIndexer.numOfTokensInDocument);				
				context.getCounter(Counters.INDEXED_POINTERS).increment(proxyIndexer.termsInDocument.getNumberOfPointers());
			} catch (IOException ioe) {
				throw ioe;				
			} catch (Exception e) {
				throw new WrappedIOException(e);
			}
		}
		
		proxyIndexer.termsInDocument.clear();
	}
	
	/** causes the posting lists built up in memory to be flushed out */
	@Override
	public void forceFlush() throws IOException
	{
		ExtensibleSinglePassIndexer.logger.info("Map "+mapTaskID+", flush requested, containing "+proxyIndexer.numberOfDocsSinceFlush+" documents, flush "+flushNo);
		
		if (proxyIndexer.mp == null)
			throw new IOException("Map flushed before any documents were indexed");
		
		proxyIndexer.mp.finish(new NewHadoopRunWriter<VALUEIN>(currentContext, mapTaskID, splitnum, flushNo));
		runData.writeInt(proxyIndexer.currentId);
		
		if (currentContext != null)
			currentContext.getCounter(Counters.INDEXER_FLUSHES).increment(1);
		
		System.gc();
		
		proxyIndexer.createMemoryPostings();
		proxyIndexer.memoryCheck.reset();
		proxyIndexer.numberOfDocsSinceFlush = 0;
		proxyIndexer.currentId = 0;
		flushNo++;
	}	
	
	@Override
	protected void cleanup(Context context) throws IOException, InterruptedException {
		proxyIndexer.forceFlush();
		proxyIndexer.docIndexBuilder.finishedCollections();
		proxyIndexer.currentIndex.setIndexProperty("index.inverted.fields.count", ""+FieldScore.FIELDS_COUNT);
		if (FieldScore.FIELDS_COUNT > 0)
		{
			proxyIndexer.currentIndex.addIndexStructure("document-factory", FieldDocumentIndexEntry.Factory.class.getName(), "java.lang.String", "${index.inverted.fields.count}");
		}
		else
		{
			proxyIndexer.currentIndex.addIndexStructure("document-factory", SimpleDocumentIndexEntry.Factory.class.getName(), "", "");
		}
		proxyIndexer.metaBuilder.close();
		proxyIndexer.currentIndex.flush();
		proxyIndexer.currentIndex.close();
		runData.writeInt(-1);
		runData.writeInt(proxyIndexer.numberOfDocuments);
		runData.writeInt(splitnum);
		runData.close();
		ExtensibleSinglePassIndexer.logger.info("Map "+mapTaskID+ " finishing, indexed "+proxyIndexer.numberOfDocuments+ " in "+(flushNo-1)+" flushes");
	}
}
