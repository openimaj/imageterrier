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
import org.apache.hadoop.mapred.JobConf;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.JobID;
import org.apache.hadoop.mapreduce.lib.input.SequenceFileInputFormat;
import org.apache.hadoop.mapreduce.lib.map.MultithreadedMapper;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.hadoop.mapreduce.lib.output.SequenceFileOutputFormat;
import org.apache.hadoop.mapreduce.lib.partition.HashPartitioner;
import org.apache.hadoop.util.ToolRunner;
import org.apache.log4j.Logger;
import org.imageterrier.basictools.BasicTerrierConfig;
import org.imageterrier.hadoop.mapreduce.PositionAwareSequenceFileInputFormat;
import org.imageterrier.locfile.QLFDocument;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.openimaj.feature.local.LocalFeature;
import org.openimaj.feature.local.list.LocalFeatureList;
import org.openimaj.feature.local.list.MemoryLocalFeatureList;
import org.openimaj.feature.local.quantised.QuantisedLocalFeature;
import org.openimaj.hadoop.tools.clusterquantiser.HadoopClusterQuantiserOptions;
import org.openimaj.io.IOUtils;
import org.openimaj.ml.clustering.Cluster;
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
	protected static final Logger logger = Logger.getLogger(HadoopIndexer.class);
	
	public static final String INDEXER_ARGS_STRING = "indexer.args";

	/**
	 * The mapper implementation
	 */
	static class IndexerMapper extends HadoopIndexerMapper<BytesWritable> {
		protected static Class<? extends QuantisedLocalFeature<?>> featureClass;
		protected static HadoopIndexerOptions options;
		private static Cluster<?, ?> quantiser;

		@Override
		protected ExtensibleSinglePassIndexer createIndexer(Context context) throws IOException {
			options = getOptions(context.getConfiguration());
			
			featureClass = options.getFeatureClass();
			return options.getIndexType().getIndexer(null, null);
		}

		@SuppressWarnings({ "rawtypes", "unchecked" })
		@Override
		protected Document recordToDocument(Text key, BytesWritable value) throws IOException {
			switch (options.getInputMode()) {
			case QUANTISED_FEATURES:
				return new QLFDocument(value.getBytes(), featureClass, key.toString(), null);
			case IMAGES:
				return processImage(key, value);
			default:
				throw new RuntimeException("Unsupported mode");
			}
		}
		
		
		
		@SuppressWarnings({ "rawtypes", "unchecked" })
		private Document processImage(Text key, BytesWritable value) throws IOException {
			//extract features
			LocalFeatureList<? extends LocalFeature<?>> features = null;
			try{
				logger.warn("Extracting features...");
				features = options.getInputModeOptions().getFeatureType().getKeypointList(value.getBytes());
				
				logger.warn("Found features:" + features .size());
				logger.warn("Loading quantiser...");
				//load quantiser if required
				loadQuantiser(options);
				
				logger.warn("Quantising features...");
				//quantise features
				LocalFeatureList<QuantisedLocalFeature<?>> qkeys = new MemoryLocalFeatureList<QuantisedLocalFeature<?>>(features.size());
				if (quantiser.getClusters() instanceof byte[][]) {
					for (LocalFeature k : features) {
						int id = ((Cluster<?,byte[]>)quantiser).push_one((byte[])k.getFeatureVector().getVector());
						qkeys.add(new QuantisedLocalFeature(k.getLocation(), id));
					}
				} else {
					for (LocalFeature k : features) {
						int id = ((Cluster<?,int[]>)quantiser).push_one((int[])k.getFeatureVector().getVector());
						qkeys.add(new QuantisedLocalFeature(k.getLocation(), id));
					}
				}
				
				logger.warn("Construcing QLFDocument...");
				//create document
				return new QLFDocument(qkeys, key.toString().substring(0,20), null);
			}
			catch(Throwable e){
				logger.warn("Skipping image: " + key + " due to: " + e.getMessage());
				return null;
			}
		}

		private static synchronized void loadQuantiser(HadoopIndexerOptions options) throws IOException {
			if (quantiser == null) {
				System.out.println("Loading codebook...");
				String codebookURL = options.getInputModeOptions().getQuantiserFile();
				options.getInputModeOptions().quantiserType = HadoopClusterQuantiserOptions.sniffClusterType(codebookURL);
				
				if (options.getInputModeOptions().quantiserType != null)
					quantiser = IOUtils.read(HadoopClusterQuantiserOptions.getClusterInputStream(codebookURL), options.getInputModeOptions().getQuantiserType().getClusterClass());
				quantiser.optimize(options.getInputModeOptions().quantiserExact);
				System.out.println("Done!");
			}
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

		if (options.getMultithread() <= 0) {
			job.setMapperClass(IndexerMapper.class);
		} else {
			job.setMapperClass(MultithreadedMapper.class);
//			((JobConf)job.getConfiguration()).setNumTasksToExecutePerJvm(-1);
			MultithreadedMapper.setNumberOfThreads(job, options.getMultithread());
			MultithreadedMapper.setMapperClass(job, IndexerMapper.class);
		}
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
			{
				job.setPartitionerClass(NewSplitEmittedTerm.SETPartitioner.class);
			}
			else
			{
//				job.setPartitionerClass(NewSplitEmittedTerm.SETPartitionerLowercaseAlphaTerm.class);
				job.setPartitionerClass(NewSplitEmittedTerm.SETPartitionerHashedTerm.class);
			}
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
		long time = System.currentTimeMillis();
		
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
				if (job.getNumReduceTasks() > 1) {
					mergeLexiconInvertedFiles(options.getOutputPathString(), job.getNumReduceTasks());
				}
			}

			finish(options.getOutputPathString(), options.isDocumentPartitionMode() ? job.getNumReduceTasks() : 1, job.getConfiguration());
		}

		System.out.println("Time Taken = "+((System.currentTimeMillis()-time)/1000)+" seconds");

		return 0;
	}

	public static void main(String[] args) throws Exception {
		args = new String[] { 
				"-t", "BASIC",
				"-nr", "3",
				"-fc", "QuantisedKeypoint",
				"-o", "/Users/ss/test.index",
				"-m", "QUANTISED_FEATURES", 
				"-j", "2", 
				"hdfs://seurat.ecs.soton.ac.uk/data/quantised_features/ukbench-sift-intensity-100.seq"
		};
		
		ToolRunner.run(new HadoopIndexer(), args);
	}
}
