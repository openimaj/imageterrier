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
 * The Original Code is AbstractHadoopIndexer.java
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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.conf.Configured;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.JobID;
import org.apache.hadoop.mapreduce.TaskID;
import org.apache.hadoop.util.Tool;
import org.apache.log4j.Logger;
import org.imageterrier.hadoop.fs.TerrierHDFSAdaptor;
import org.imageterrier.indexers.hadoop.HadoopIndexer;
import org.terrier.structures.BitIndexPointer;
import org.terrier.structures.BitPostingIndexInputStream;
import org.terrier.structures.FSOMapFileLexiconOutputStream;
import org.terrier.structures.FieldLexiconEntry;
import org.terrier.structures.Index;
import org.terrier.structures.IndexUtil;
import org.terrier.structures.LexiconEntry;
import org.terrier.structures.LexiconOutputStream;
import org.terrier.structures.indexing.LexiconBuilder;
import org.terrier.structures.indexing.NewCompressingMetaIndexBuilder;
import org.terrier.structures.seralization.FixedSizeWriteableFactory;
import org.terrier.utility.ApplicationSetup;
import org.terrier.utility.FieldScore;
import org.terrier.utility.Files;


public abstract class AbstractHadoopIndexer extends Configured implements Tool {
	protected static final Logger logger = Logger.getLogger(HadoopIndexer.class);
	
	/* (non-Javadoc)
	 * @see org.apache.hadoop.conf.Configured#setConf(org.apache.hadoop.conf.Configuration)
	 */
	@Override
	public void setConf(Configuration conf) {
		super.setConf(conf);
		try {
			if (conf != null) TerrierHDFSAdaptor.initialiseHDFSAdaptor(conf);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	@SuppressWarnings("unchecked")
	protected void mergeLexiconInvertedFiles(String index_path, int numberOfReducers) throws IOException {
		final String lexiconStructure = "lexicon";
		final String tmpLexiconStructure = "newlex";
		final String invertedStructure = "inverted";

		//we're handling indices as streams, so dont need to load it. but remember previous status
		//moreover, our indices dont have document objects, so errors may occur in preloading
		final boolean indexProfile = Index.getIndexLoadingProfileAsRetrieval();
		Index.setIndexLoadingProfileAsRetrieval(false);


		//1. load in the input indices
		final Index[] srcIndices = new Index[numberOfReducers];
		final boolean[] existsIndices = new boolean[numberOfReducers];
		boolean anyExists = false;
		Arrays.fill(existsIndices, true);
		int firstNonEmpty = -1;
		for(int i=0;i<numberOfReducers;i++)
		{
			srcIndices[i] = Index.createIndex(index_path, ApplicationSetup.TERRIER_INDEX_PREFIX+"-"+i);
			if (srcIndices[i] == null)
			{
				//remove any empty inverted file for this segment
				Files.delete(BitPostingIndexInputStream.getFilename(index_path, ApplicationSetup.TERRIER_INDEX_PREFIX+"-"+i, invertedStructure, (byte)1, (byte)1));

				//remember that this index doesnt exist
				existsIndices[i] = false;
				logger.warn("No reduce "+i+" output : no output index ["+index_path+","+(ApplicationSetup.TERRIER_INDEX_PREFIX+"-"+i)+ "]");
				
			} else {
				anyExists = true;
				if(firstNonEmpty == -1 && srcIndices[i].getIndexStructure(lexiconStructure + "-valuefactory")!=null){
					firstNonEmpty = i;
				}
			}
		}
		
		if (!anyExists) {
			//none exists. maybe fewer mappers ran, or there was a problem
			logger.warn("No reduce output found for the " + numberOfReducers + " reducers. Most likely only one reducer actually ran.");
			return; 
		}
		
		//2. the target index is the first source index
		Index dest = srcIndices[firstNonEmpty];

		//3. create the new lexicon
		LexiconOutputStream<String> lexOut = new FSOMapFileLexiconOutputStream(
				dest, tmpLexiconStructure, 
				(FixedSizeWriteableFactory<Text>) dest.getIndexStructure(lexiconStructure + "-keyfactory"),
				(Class<? extends FixedSizeWriteableFactory<LexiconEntry>>) dest.getIndexStructure(lexiconStructure + "-valuefactory").getClass());

		//4. append each source lexicon on to the new lexicon, amending the filenumber as we go
		int termId = 0;
		for(int i=0;i<numberOfReducers;i++)
		{
			//the partition did not have any stuff
			if (! existsIndices[i])
			{
				//touch an empty inverted index file for this segment, as BitPostingIndex requires that all of the files exist
				Files.writeFileStream(BitPostingIndexInputStream.getFilename(
						dest, invertedStructure, (byte)numberOfReducers, (byte)i)).close();
				continue;
			}
			//else, append the lexicon
			Iterator<Map.Entry<String,LexiconEntry>> lexIn = (Iterator<Map.Entry<String, LexiconEntry>>) srcIndices[i].getIndexStructureInputStream("lexicon");
			while(lexIn.hasNext())
			{
				Map.Entry<String,LexiconEntry> e = lexIn.next();
				e.getValue().setTermId(termId);
				((BitIndexPointer)e.getValue()).setFileNumber((byte)i);
				lexOut.writeNextEntry(e.getKey(), e.getValue());
				termId++;
			}
			IndexUtil.close(lexIn);
			//rename the inverted file to be part of the destination index
			Files.rename(
					BitPostingIndexInputStream.getFilename(srcIndices[i], invertedStructure, (byte)1, (byte)1), 
					BitPostingIndexInputStream.getFilename(dest, invertedStructure, (byte)numberOfReducers, (byte)i));
		}
		lexOut.close();

		//5. change over lexicon structures
		final String[] structureSuffices = new String[]{"", "-entry-inputstream"};
		//remove old lexicon structures
		for (String suffix : structureSuffices)
		{
			if (! IndexUtil.deleteStructure(dest, lexiconStructure + suffix))
				logger.warn("Structure " + lexiconStructure + suffix + " not found when removing");
		}
		//rename new lexicon structures
		for (String suffix : structureSuffices)
		{
			if (! IndexUtil.renameIndexStructure(dest, tmpLexiconStructure + suffix, lexiconStructure + suffix))
				logger.warn("Structure " + tmpLexiconStructure + suffix + " not found when renaming");
		}

		//6. update destination index

		if (FieldScore.FIELDS_COUNT > 0)
			dest.addIndexStructure("lexicon-valuefactory", FieldLexiconEntry.Factory.class.getName(), "java.lang.String", "${index.inverted.fields.count}");
		dest.setIndexProperty("index."+invertedStructure+".data-files", ""+numberOfReducers);
		LexiconBuilder.optimise(dest, lexiconStructure);
		dest.flush();

		//7. close source and dest indices
		for(Index src: srcIndices) //dest is also closed
		{
			if (src != null)
				src.close();
		}

		//8. rearrange indices into desired layout

		//rename target index
		IndexUtil.renameIndex(index_path, ApplicationSetup.TERRIER_INDEX_PREFIX+"-0", index_path, ApplicationSetup.TERRIER_INDEX_PREFIX);
		//delete other source indices
		for(int i=1;i<numberOfReducers;i++)
		{
			if (existsIndices[i])
				IndexUtil.deleteIndex(index_path, ApplicationSetup.TERRIER_INDEX_PREFIX+"-" + i);
		}

		//restore loading profile
		Index.setIndexLoadingProfileAsRetrieval(indexProfile);
	}

	public void deleteTaskFiles(String path, JobID job)
	{
		String[] fileNames = Files.list(path);

		if (fileNames == null) return;

		for(String filename : fileNames) {
			String periodParts[] = filename.split("\\.");

			try {
				TaskID tid = TaskID.forName(periodParts[0]);

				if (tid.getJobID().compareTo(job) == 0) {
					if (! Files.delete(path + "/" + filename))
						logger.warn("Could not delete temporary map side-effect file "+ path + "/" + filename);
				}
			} catch (Exception e) {}
			
			//remove any empty reduce files created as a side effect of using sequencefileoutputformat rather than nulloutputformat
			if (filename.startsWith("part-r-")) Files.delete(path + "/" + filename);
		}
	}
	
	public void finish(final String destinationIndexPath, int numberOfReduceTasks, Configuration conf) throws Exception
	{
		final String[] reverseMetaKeys = ApplicationSetup.getProperty("indexer.meta.reverse.keys", "docno").split("\\s*,\\s*");
		Index.setIndexLoadingProfileAsRetrieval(false);
		if (numberOfReduceTasks == 1)
		{			
			Index index = Index.createIndex(destinationIndexPath, ApplicationSetup.TERRIER_INDEX_PREFIX);
			if (index == null)
			{
				throw new IOException("No such index ["+destinationIndexPath+","+ApplicationSetup.TERRIER_INDEX_PREFIX+"]");
			}
			NewCompressingMetaIndexBuilder.reverseAsMapReduceJob(index, "meta", reverseMetaKeys, conf);
			index.close();
			return;
		}
		//make a list of MR jobs in separate threads
		List<Thread> threads = new ArrayList<Thread>(numberOfReduceTasks);
		for(int i=0;i<numberOfReduceTasks;i++)
		{
			final int id = i;
			threads.add(new Thread() {
				@Override
				public void run() {
					try{
						Index index = Index.createIndex(destinationIndexPath, ApplicationSetup.TERRIER_INDEX_PREFIX+"-"+id);
						NewCompressingMetaIndexBuilder.reverseAsMapReduceJob(index, "meta", reverseMetaKeys);
						index.close();
					} catch (Exception e) {
						logger.error("Problem finishing meta", e);
						e.printStackTrace();
					}
				}
			});
		}
		
		//start the threads
		for(Thread t : threads)
			t.start();
		
		//wait for the threads to end
		for(Thread t : threads)
			t.join();
	}
}
