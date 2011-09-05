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
 * The Original Code is HadoopIndexer.java
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
package org.imageterrier.indexers.hadoop;

import java.io.IOException;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.io.BytesWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.JobID;
import org.apache.hadoop.mapreduce.lib.input.SequenceFileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.hadoop.mapreduce.lib.output.SequenceFileOutputFormat;
import org.apache.hadoop.mapreduce.lib.partition.HashPartitioner;
import org.apache.hadoop.util.ToolRunner;
import org.imageterrier.basictools.BasicTerrierConfig;
import org.imageterrier.hadoop.mapreduce.PositionAwareSequenceFileInputFormat;
import org.imageterrier.locfile.QLFDocument;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.openimaj.feature.local.quantised.QuantisedLocalFeature;
import org.terrier.indexing.AbstractHadoopIndexer;
import org.terrier.indexing.Document;
import org.terrier.indexing.ExtensibleSinglePassIndexer;
import org.terrier.indexing.HadoopIndexerMapper;
import org.terrier.indexing.HadoopIndexerReducer;
import org.terrier.structures.Index;
import org.terrier.structures.indexing.singlepass.hadoop.MapEmittedPostingList;
import org.terrier.structures.indexing.singlepass.hadoop.NewSplitEmittedTerm;
import org.terrier.utility.ApplicationSetup;
import org.terrier.utility.Files;


/**
 * @author Jonathon Hare
 *
 */
public class HadoopIndexer extends AbstractHadoopIndexer {
	static {
		//initialise terrier
		BasicTerrierConfig.configure();
	}
	
	public static final String INDEXER_ARGS_STRING = "indexer.args";

	/**
	 * The mapper implementation
	 */
	static class IndexerMapper extends HadoopIndexerMapper<BytesWritable> {
		protected Class<? extends QuantisedLocalFeature<?>> featureClass;

		@Override
		protected ExtensibleSinglePassIndexer createIndexer(Context context) throws IOException {
			HadoopIndexerOptions options = getOptions(context.getConfiguration());
			
			featureClass = options.getFeatureClass();
			return options.getIndexType().getIndexer(null, null);
		}

		@SuppressWarnings({ "rawtypes", "unchecked" })
		@Override
		protected Document recordToDocument(Text key, BytesWritable value) throws IOException {
			return new QLFDocument(value.getBytes(), featureClass, key.toString(), null);
		}
	}

	/**
	 * The reducer implementation
	 */
	static class IndexerReducer extends HadoopIndexerReducer {
		@Override
		protected ExtensibleSinglePassIndexer createIndexer(Context context) throws IOException {
			return getOptions(context.getConfiguration()).getIndexType().getIndexer(null, null);
		}
	}

	private static HadoopIndexerOptions getOptions(Configuration conf) throws IOException {
		String [] args = conf.getStrings(INDEXER_ARGS_STRING);

		HadoopIndexerOptions options = new HadoopIndexerOptions();
		CmdLineParser parser = new CmdLineParser(options);
		
		try {
		    parser.parseArgument(args);
		} catch(CmdLineException e) {
			throw new IOException(e);
		}
		
		return options;
	}
	
	private static final String usage()
	{
		return "Usage: HadoopIndexing [-p]";
	}
	
	protected Job createJob(HadoopIndexerOptions options) throws IOException {
		Job job = new Job(getConf());
		job.setJobName("terrierIndexing");

		job.setMapperClass(IndexerMapper.class);
		job.setReducerClass(IndexerReducer.class);

		FileOutputFormat.setOutputPath(job, options.getOutputPath());
		job.setMapOutputKeyClass(NewSplitEmittedTerm.class);
		job.setMapOutputValueClass(MapEmittedPostingList.class);
		job.getConfiguration().setBoolean("indexing.hadoop.multiple.indices", options.isDocumentPartitionMode());

//		if (!job.getConfiguration().get("mapred.job.tracker").equals("local")) {
//			job.getConfiguration().set("mapred.map.output.compression.codec", GzipCodec.class.getCanonicalName());
//			job.getConfiguration().setBoolean("mapred.compress.map.output", true);
//		} else {
			job.getConfiguration().setBoolean("mapred.compress.map.output", false);
//		}
		
		job.setInputFormatClass(PositionAwareSequenceFileInputFormat.class); //important
		
		job.setOutputFormatClass(SequenceFileOutputFormat.class);
		
		job.setSortComparatorClass(NewSplitEmittedTerm.SETRawComparatorTermSplitFlush.class);
		job.setGroupingComparatorClass(NewSplitEmittedTerm.SETRawComparatorTerm.class);

		job.getConfiguration().setBoolean("mapred.reduce.tasks.speculative.execution", false);
		
		SequenceFileInputFormat.setInputPaths(job, options.getInputPaths());
		
		job.setNumReduceTasks(options.getNumReducers());
		if (options.getNumReducers() > 1) {
			if (options.isDocumentPartitionMode())
				job.setPartitionerClass(NewSplitEmittedTerm.SETPartitioner.class);
			else
				job.setPartitionerClass(NewSplitEmittedTerm.SETPartitionerLowercaseAlphaTerm.class);
		} else {
			//for JUnit tests, we seem to need to restore the original partitioner class
			job.setPartitionerClass(HashPartitioner.class);
		}

		job.setJarByClass(this.getClass());
		
		return job;
	}
	
	/** 
	 * Process the arguments and start the map-reduce indexing.
	 * 
	 * @param args
	 * @throws Exception
	 */
	@Override
	public int run(String[] args) throws Exception {
		long time =System.currentTimeMillis();
		
		HadoopIndexerOptions options = new HadoopIndexerOptions();
		CmdLineParser parser = new CmdLineParser(options);
		try {
		    parser.parseArgument(args);
		} catch(CmdLineException e) {
		    logger.fatal(e.getMessage());
		    logger.fatal(usage());
		    return 1;
		}

		if (Files.exists(options.getOutputPathString()) && Index.existsIndex(options.getOutputPathString(), ApplicationSetup.TERRIER_INDEX_PREFIX)) {
			logger.fatal("Cannot index while index exists at " + options.getOutputPathString() + "," + ApplicationSetup.TERRIER_INDEX_PREFIX);
			return 1;
		}
		
		// create job
		Job job = createJob(options);
		
		//set args string
		job.getConfiguration().setStrings(INDEXER_ARGS_STRING, args);

		//run job
		JobID jobId = null;
		boolean ranOK = true;
		try {
			ranOK = job.waitForCompletion(true);
			jobId = job.getJobID();
		} catch (Exception e) { 
			logger.error("Problem running job", e);
			ranOK = false;
		}

		if (jobId != null) {
			deleteTaskFiles(options.getOutputPathString(), jobId);
		}

		if (ranOK) {
			if (! options.isDocumentPartitionMode()) {
				if (options.getNumReducers() > 1) {
					mergeLexiconInvertedFiles(options.getOutputPathString(), options.getNumReducers());
				}
			}

			finish(options.getOutputPathString(), options.isDocumentPartitionMode() ? options.getNumReducers() : 1, job.getConfiguration());
		}

		System.out.println("Time Taken = "+((System.currentTimeMillis()-time)/1000)+" seconds");

		return 0;
	}

	public static void main(String[] args) throws Exception {
//		args = new String[] { 
//				"-t", "BASIC",
//				"-nr", "1",
//				"-fc", "QuantisedKeypoint",
//				"-o", "/Users/jsh2/test.index",
//				"/Users/jsh2/ukbench-sift-intensity.seq"
//		};
		
		ToolRunner.run(new HadoopIndexer(), args);
	}
}
