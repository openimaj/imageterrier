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
 * The Original Code is HadoopIndexerReducer.java
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

import java.io.DataInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;

import org.apache.hadoop.fs.FileStatus;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.fs.PathFilter;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.imageterrier.hadoop.fs.TerrierHDFSAdaptor;
import org.terrier.compression.BitIn;
import org.terrier.compression.BitOutputStream;
import org.terrier.structures.BasicLexiconEntry;
import org.terrier.structures.DocumentIndexEntry;
import org.terrier.structures.FSOMapFileLexiconOutputStream;
import org.terrier.structures.FieldDocumentIndexEntry;
import org.terrier.structures.FieldLexiconEntry;
import org.terrier.structures.Index;
import org.terrier.structures.IndexUtil;
import org.terrier.structures.LexiconOutputStream;
import org.terrier.structures.SimpleDocumentIndexEntry;
import org.terrier.structures.indexing.DocumentIndexBuilder;
import org.terrier.structures.indexing.MetaIndexBuilder;
import org.terrier.structures.indexing.singlepass.RunsMerger;
import org.terrier.structures.indexing.singlepass.hadoop.HadoopRunIteratorFactory;
import org.terrier.structures.indexing.singlepass.hadoop.HadoopRunsMerger;
import org.terrier.structures.indexing.singlepass.hadoop.IDComparator;
import org.terrier.structures.indexing.singlepass.hadoop.MapData;
import org.terrier.structures.indexing.singlepass.hadoop.MapEmittedPostingList;
import org.terrier.structures.indexing.singlepass.hadoop.NewSplitEmittedTerm;
import org.terrier.utility.ApplicationSetup;
import org.terrier.utility.ArrayUtils;
import org.terrier.utility.FieldScore;
import org.terrier.utility.Files;
import org.terrier.utility.io.WrappedIOException;

public abstract class HadoopIndexerReducer extends Reducer<NewSplitEmittedTerm, MapEmittedPostingList, Object, Object> {
	/** the underlying indexer object */
	protected ExtensibleSinglePassIndexer proxyIndexer;
	
	/** OutputStream for the Lexicon*/ 
	protected LexiconOutputStream<String> lexstream;
	
	/** runIterator factory being used to generate RunIterators */
	protected HadoopRunIteratorFactory runIteratorF = null;
	
	protected boolean mutipleIndices = true;
	protected int reduceId;
	protected String[] MapIndexPrefixes = null;

	protected int reduceCount = 0;
	
	/**
	 * This method returns an instance of an indexer, possibly
	 * using parameters extracted from the context.
	 * 
	 * @param context
	 * @return
	 */
	protected abstract ExtensibleSinglePassIndexer createIndexer(Context context) throws IOException;
	
	@Override
	protected void setup(Context context) throws IOException, InterruptedException {
		TerrierHDFSAdaptor.initialiseHDFSAdaptor(context.getConfiguration());
		
		proxyIndexer = createIndexer(context);
		
		//load in the current index
		final Path indexDestination = FileOutputFormat.getWorkOutputPath(context);
		
		reduceId = context.getTaskAttemptID().getTaskID().getId();
		proxyIndexer.path = indexDestination.toString();
		mutipleIndices = context.getConfiguration().getBoolean("indexing.hadoop.multiple.indices", true);
		
		if (context.getNumReduceTasks() > 1) {
			//gets the reduce number and suffices this to data
			proxyIndexer.prefix = ApplicationSetup.TERRIER_INDEX_PREFIX+"-"+reduceId;
		} else {
			proxyIndexer.prefix = ApplicationSetup.TERRIER_INDEX_PREFIX;
		}
		
		proxyIndexer.currentIndex = Index.createNewIndex(proxyIndexer.path, proxyIndexer.prefix);
		
		proxyIndexer.merger = createRunMerger();
	}
	
	/**
	 * Merge the postings for the current term, converts the document ID's in the
	 * postings to be relative to one another using the run number, number of documents
	 * covered in each run, the flush number for that run and the number of documents
	 * flushed.
	 * @param mapData - info about the runs(maps) and the flushes
	 */
	public void startReduce(LinkedList<MapData> mapData, Context context) throws IOException {
		ExtensibleSinglePassIndexer.logger.info("The number of Reduce Tasks being used : "+context.getNumReduceTasks());
		((HadoopRunsMerger)(proxyIndexer.merger)).beginMerge(mapData);
		proxyIndexer.currentIndex.setIndexProperty("max.term.length", ApplicationSetup.getProperty("max.term.length", ""+20));
		
		lexstream = new FSOMapFileLexiconOutputStream(proxyIndexer.currentIndex, "lexicon", 
				(FieldScore.FIELDS_COUNT  > 0 ? FieldLexiconEntry.Factory.class : BasicLexiconEntry.Factory.class));
		
		// Tell the merger how many to Reducers to merge for
		((HadoopRunsMerger) proxyIndexer.merger).setNumReducers(mutipleIndices ? context.getNumReduceTasks() : 1);
	}
	
	@Override
	protected void reduce(NewSplitEmittedTerm key, Iterable<MapEmittedPostingList> values, Context context) throws IOException, InterruptedException {
		if (reduceCount == 0) {
			LinkedList<MapData> runData = loadRunData(context);
	    	startReduce(runData, context);
		}
		
		reduceCount ++;
		
		String term = key.getTerm().trim();
		if (term.length() == 0)
			return;
		
		context.setStatus("Reducer is merging term " + term);
		
		runIteratorF.setRunPostingIterator(values.iterator());
		runIteratorF.setTerm(term);
		
		try{
			proxyIndexer.merger.mergeOne(lexstream);
		} catch (Exception e) {
			throw new WrappedIOException(e);
		}
		context.progress();
	}
	
	/** finishes the reduce step, by closing the lexicon and inverted file output,
	  * building the lexicon hash and index, and merging the document indices created
	  * by the map tasks. The output index finalised */
	@Override
	protected void cleanup(Context context) throws IOException, InterruptedException {
		if (reduceCount<=0)
		{
			if(reduceId!=0){
				ExtensibleSinglePassIndexer.logger.warn("No terms were input, skipping reduce close");
				return;
			}
			else{
				LinkedList<MapData> runData = loadRunData(context);
		    	startReduce(runData, context);
			}
		}
		
		//generate final index structures
		//1. any remaining lexicon terms
		proxyIndexer.merger.endMerge(lexstream);
		
		//2. the end of the inverted file
		proxyIndexer.merger.getBos().close();
		lexstream.close();
		
		//index updating is ONLY for 
		proxyIndexer.currentIndex.addIndexStructure(
				"inverted",
				proxyIndexer.invertedIndexClass,
				"org.terrier.structures.Index,java.lang.String,org.terrier.structures.DocumentIndex,java.lang.Class", 
				"index,structureName,document,"+ 
					(FieldScore.FIELDS_COUNT > 0
						? proxyIndexer.fieldInvertedIndexPostingIteratorClass
						: proxyIndexer.basicInvertedIndexPostingIteratorClass ));
		proxyIndexer.currentIndex.addIndexStructureInputStream(
                "inverted",
                proxyIndexer.invertedIndexInputStreamClass,
                "org.terrier.structures.Index,java.lang.String,java.util.Iterator,java.lang.Class",
                "index,structureName,lexicon-entry-inputstream,"+
                	(FieldScore.FIELDS_COUNT > 0
						? proxyIndexer.fieldInvertedIndexPostingIteratorClass
						: proxyIndexer.basicInvertedIndexPostingIteratorClass ));
		proxyIndexer.currentIndex.setIndexProperty("index.inverted.fields.count", ""+FieldScore.FIELDS_COUNT );
		proxyIndexer.currentIndex.setIndexProperty("index.inverted.fields.names", ArrayUtils.join(FieldScore.FIELD_NAMES, ","));
		
		
		//3. finalise the lexicon
		proxyIndexer.currentIndex.setIndexProperty("num.Terms",""+ lexstream.getNumberOfTermsWritten() );
		proxyIndexer.currentIndex.setIndexProperty("num.Tokens",""+lexstream.getNumberOfTokensWritten() );
		proxyIndexer.currentIndex.setIndexProperty("num.Pointers",""+lexstream.getNumberOfPointersWritten() );
		if(reduceCount > 0) proxyIndexer.finishedInvertedIndexBuild();
		if (FieldScore.FIELDS_COUNT > 0)
			proxyIndexer.currentIndex.addIndexStructure("lexicon-valuefactory", FieldLexiconEntry.Factory.class.getName(), "java.lang.String", "${index.inverted.fields.count}");
		
		//the document indices are only merged if we are creating multiple indices
		//OR if this is the first reducer for a job creating a single index
		if (mutipleIndices || reduceId == 0)
		{
			//4. document index
			Index[] sourceIndices = new Index[MapIndexPrefixes.length];
		 	for (int i= 0; i<MapIndexPrefixes.length;i++)
			{
				sourceIndices[i] = Index.createIndex(FileOutputFormat.getOutputPath(context).toString(), MapIndexPrefixes[i]);
				if (sourceIndices[i] == null)
					throw new IOException("Could not load index from ("
						+FileOutputFormat.getOutputPath(context).toString()+","+ MapIndexPrefixes[i] +") because "
						+Index.getLastIndexLoadError());
			}
		 	mergeDocumentIndex(sourceIndices, context);
		 	
		 	//5. close the map phase indices
			for(Index i : sourceIndices)
			{
				i.close();
			}
		}
		proxyIndexer.currentIndex.flush();
	}
	
	/** Merges the simple document indexes made for each map, instead creating the final document index */	
	@SuppressWarnings("unchecked")
	protected void mergeDocumentIndex(Index[] src, Context context) throws IOException
	{
		ExtensibleSinglePassIndexer.logger.info("Merging document and meta indices");
		final DocumentIndexBuilder docidOutput = new DocumentIndexBuilder(proxyIndexer.currentIndex, "document");
		final MetaIndexBuilder metaBuilder = proxyIndexer.createMetaIndexBuilder();
		int i_index = 0;
		int docCount =-1;
		for (Index srcIndex: src)
		{
			final Iterator<DocumentIndexEntry> docidInput = (Iterator<DocumentIndexEntry>)srcIndex.getIndexStructureInputStream("document");
			final Iterator<String[]> metaInput1 = (Iterator<String[]>)srcIndex.getIndexStructureInputStream("meta");
		    while (docidInput.hasNext())
			{
				docCount++;
				docidOutput.addEntryToBuffer(docidInput.next());
		        metaBuilder.writeDocumentEntry(metaInput1.next());
		        context.progress();
			}
		    IndexUtil.close(docidInput);
		    IndexUtil.close(metaInput1);
		    i_index++;
		}
		metaBuilder.close();
		docidOutput.finishedCollections();
		if (FieldScore.FIELDS_COUNT > 0)
		{
			proxyIndexer.currentIndex.addIndexStructure("document-factory", FieldDocumentIndexEntry.Factory.class.getName(), "java.lang.String", "${index.inverted.fields.count}");
		}
		else
		{
			proxyIndexer.currentIndex.addIndexStructure("document-factory", SimpleDocumentIndexEntry.Factory.class.getName(), "", "");
		}
		ExtensibleSinglePassIndexer.logger.info("Finished merging document indices from "+src.length+" map tasks: "+docCount +" documents found");
	}
	
	/** Creates the RunsMerger and the RunIteratorFactory */
	protected RunsMerger createRunMerger() {
		ExtensibleSinglePassIndexer.logger.info("creating run merged with fields="+proxyIndexer.useFieldInformation);
		
		runIteratorF = new HadoopRunIteratorFactory(null, proxyIndexer.getPostingInRunClass(), proxyIndexer.numFields);
		
		HadoopRunsMerger tempRM = new HadoopRunsMerger(runIteratorF);
		
		try{
			tempRM.setBos(new BitOutputStream(
					proxyIndexer.currentIndex.getPath() + ApplicationSetup.FILE_SEPARATOR 
					+ proxyIndexer.currentIndex.getPrefix() + ".inverted" + BitIn.USUAL_EXTENSION));
		} catch (IOException ioe) {
			ioe.printStackTrace();
		}
		
		return tempRM;
	}
	
	protected LinkedList<MapData> loadRunData(Context context) throws IOException 
	{
		// Load in Run Data
		ArrayList<String> mapTaskIDs = new ArrayList<String>();
		final LinkedList<MapData> runData = new LinkedList<MapData>();
		DataInputStream runDataIn;
	
		final String jobId = context.getTaskAttemptID().getJobID().toString().replaceAll("job", "task");
		
		final FileStatus[] files = FileSystem.get(context.getConfiguration()).listStatus(
			FileOutputFormat.getOutputPath(context), 
			new PathFilter()
			{ 
				@Override
				public boolean accept(Path path)
				{					
					final String name = path.getName();
					//1. is this a run file
					if (!(  name.startsWith( jobId )  && name.endsWith(".runs")))
						return false;
					return true;
				}
			}
		);

		if (files == null || files.length == 0)
		{
			throw new IOException("No run status files found in "+FileOutputFormat.getOutputPath(context));
		}
		
		final int thisPartition = context.getTaskAttemptID().getTaskID().getId();
		final NewSplitEmittedTerm.SETPartitioner partitionChecker = new NewSplitEmittedTerm.SETPartitioner();
		partitionChecker.setConf(context.getConfiguration());
		
		MapData tempHRD;
		for (FileStatus file : files) 
		{
			ExtensibleSinglePassIndexer.logger.info("Run data file "+ file.getPath().toString()+" has length "+Files.length(file.getPath().toString()));
			runDataIn = new DataInputStream(Files.openFileStream(file.getPath().toString()));
			tempHRD = new MapData(runDataIn);
			//check to see if this file contained our split information
			if (mutipleIndices && partitionChecker.calculatePartition(tempHRD.getSplitnum(), context.getNumReduceTasks()) != thisPartition)
				continue;
			
			mapTaskIDs.add(tempHRD.getMap());
			runData.add(tempHRD);
			runDataIn.close();
		}
		
		// Sort by splitnum
		Collections.sort(runData);
		Collections.sort(mapTaskIDs, new IDComparator(runData));
		
		// A list of the index shards
		MapIndexPrefixes = mapTaskIDs.toArray(new String[0]);
		return runData;
	}
}
