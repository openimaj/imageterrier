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

import gnu.trove.TLongArrayList;

import java.io.IOException;
import java.util.Arrays;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.io.BytesWritable;
import org.apache.hadoop.io.Text;
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
import org.imageterrier.hadoop.mapreduce.PositionAwareSplitWrapper;
import org.imageterrier.locfile.QLFDocument;
import org.imageterrier.toolopts.InputMode;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.openimaj.feature.local.LocalFeature;
import org.openimaj.feature.local.list.LocalFeatureList;
import org.openimaj.feature.local.list.MemoryLocalFeatureList;
import org.openimaj.feature.local.quantised.QuantisedLocalFeature;
import org.openimaj.hadoop.tools.clusterquantiser.HadoopClusterQuantiserOptions;
import org.openimaj.io.IOUtils;
import org.openimaj.ml.clustering.Cluster;
import org.openimaj.tools.clusterquantiser.ClusterType;
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

	public static final String QUANTISER_SIZE = "indexer.quantiser.size";
	
	/**
	 * The mapper implementation for direct quantised feature indexing
	 */
	static class QFIndexerMapper extends HadoopIndexerMapper<BytesWritable> {
		protected Class<? extends QuantisedLocalFeature<?>> featureClass;
		protected HadoopIndexerOptions options;

		@Override
		protected ExtensibleSinglePassIndexer createIndexer(Context context) throws IOException {
			options = getOptions(context.getConfiguration());
			
			featureClass = options.getFeatureClass();
			return options.getIndexType().getIndexer(null, null);
		}

		@SuppressWarnings({ "unchecked", "rawtypes" })
		@Override
		protected Document recordToDocument(Text key, BytesWritable value) throws IOException {
			return new QLFDocument(value.getBytes(), featureClass, key.toString(), null);
		}
	}

	/**
	 * Mapper implementation for directly processing images, that is safe to use with a 
	 * MultithreadedMapper. Each MultithreadedMapper thread will produce its own index.
	 */
	static class MTImageIndexerMapper extends HadoopIndexerMapper<BytesWritable> {
		protected static Class<? extends QuantisedLocalFeature<?>> featureClass;
		protected static HadoopIndexerOptions options;
		private static Cluster<?, ?> quantiser;
		private static TLongArrayList threads = new TLongArrayList();
		private int threadID;
		
		private static synchronized ExtensibleSinglePassIndexer setupIndexer(Context context) throws IOException {
			if (quantiser == null) {
				options = getOptions(context.getConfiguration());
				
				featureClass = options.getFeatureClass();
				
				System.out.println("Loading codebook...");
				String codebookURL = options.getInputModeOptions().getQuantiserFile();
				options.getInputModeOptions().quantiserType = HadoopClusterQuantiserOptions.sniffClusterType(codebookURL);
				
				if (options.getInputModeOptions().quantiserType != null)
					quantiser = IOUtils.read(HadoopClusterQuantiserOptions.getClusterInputStream(codebookURL), options.getInputModeOptions().getQuantiserType().getClusterClass());
				quantiser.optimize(options.getInputModeOptions().quantiserExact);
				System.out.println("Done!");
			}
			return options.getIndexType().getIndexer(null, null);
		}
		
		@Override
		protected ExtensibleSinglePassIndexer createIndexer(Context context) throws IOException {
			synchronized (MTImageIndexerMapper.class) {
				long id = Thread.currentThread().getId();
				if (!threads.contains(id)) {
					threads.add(id);
					this.threadID = threads.indexOf(id);
				}
			}
			
			return setupIndexer(context);
		}

		@Override
		protected int getSplitNum(Context context) {
			//Splitno is required by the reducer to be unique per mapper (in particular in the .runs files)
			//we modify the splitnos for each thread to allow this to work
			try {
				if (((Class<?>)context.getMapperClass()) == ((Class<?>)(MultithreadedMapper.class))) {
					int sidx = ((PositionAwareSplitWrapper<?>)context.getInputSplit()).getSplitIndex();
					return (sidx * MultithreadedMapper.getNumberOfThreads(context)) + threadID;
				}
			} catch (ClassNotFoundException e) {}
			return ((PositionAwareSplitWrapper<?>)context.getInputSplit()).getSplitIndex();
		}
		
		@Override
		protected String getTaskID(Context context) {
			//the task id is used to name the shard. we modify it per thread to allow each thread to
			//work on its own shard.
			try {
				if (((Class<?>)context.getMapperClass()) == ((Class<?>)(MultithreadedMapper.class))) {
					return context.getTaskAttemptID().getTaskID().toString() + threadID;
				}
			} catch (ClassNotFoundException e) {}
			return context.getTaskAttemptID().getTaskID().toString();
		}
		
		@SuppressWarnings({ "unchecked", "rawtypes" })
		@Override
		protected Document recordToDocument(Text key, BytesWritable value) throws IOException {
			//extract features
			LocalFeatureList<? extends LocalFeature<?>> features = null;
			try{
				logger.info("Extracting features...");
				features = options.getInputModeOptions().getFeatureType().getKeypointList(value.getBytes());
				
				logger.info("Quantising features...");
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
				
				logger.info("Construcing QLFDocument...");
				//create document
				return new QLFDocument(qkeys, key.toString().substring(0,Math.min(key.getLength(), 20)), null); //FIXME sort out key length
			}
			catch(Throwable e){
				logger.warn("Skipping image: " + key + " due to: " + e.getMessage());
				return null;
			}
		}
		
	}
	
	/**
	 * Mapper implementation that uses multiple threads to process
	 * images into visual terms and then emits them to the
	 * indexer
	 */
	static class ImageIndexerMapper extends HadoopIndexerMapper<BytesWritable> {
		protected Class<? extends QuantisedLocalFeature<?>> featureClass;
		protected HadoopIndexerOptions options;
		private ExecutorService service;
		private static Cluster<?, ?> quantiser;
		
		@Override
		protected void map(Text key, BytesWritable value, final Context context) throws IOException, InterruptedException {
			final Text innerkey = new Text(key.toString());
			final BytesWritable innervalue = new BytesWritable(Arrays.copyOf(value.getBytes(), value.getLength()));
			
			 
			Callable<Boolean> r = new Callable<Boolean>() {
				@Override
				public Boolean call() throws IOException {
					final String docno = innerkey.toString();
					
					final Document doc = recordToDocument(innerkey, innervalue);
					if(doc==null) return false;
					
//					long t1 = System.nanoTime();
//					synchronized (ImageIndexerMapper.this) {
//						long t2 = System.nanoTime();
//						
//						System.out.println("Spent " + ((t2-t1)*(1.0e-9)) + "s waiting for lock!");
//						
//						context.setStatus("Currently indexing "+docno);
//						
//						indexDocument(doc, context);
//						context.getCounter(Counters.INDEXED_DOCUMENTS).increment(1);
//					}
					return true;
				}
			};
			
			service.submit(r);
		}
		
		@Override
		protected void cleanup(Context context) throws IOException, InterruptedException {
			service.shutdown();
			logger.info("Waiting for mapper threads to finish");
			service.awaitTermination(1, TimeUnit.DAYS);
			logger.info("Mapper threads finished. Cleaning up.");
			super.cleanup(context);
		}
		
		@Override
		protected ExtensibleSinglePassIndexer createIndexer(Context context) throws IOException {
			options = getOptions(context.getConfiguration());
						
			featureClass = options.getFeatureClass();
			
			//load quantiser if required
			loadQuantiser(options,true);
			
			//set up threadpool
			int nThreads = options.getMultithread();
			service =  new ThreadPoolExecutor(nThreads, nThreads, 0L, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<Runnable>(nThreads) {
				//the ThreadPoolExecutor calls offer() on the backing queue, which unfortunately
				//doesn't block, and we end up getting exceptions because the job could not
				//be executed. This works around the problem by making offer() block (by calling put()). 
				private static final long serialVersionUID = 1L;

				@Override
			    public boolean offer(Runnable e)
			    {
			        // turn offer() and add() into a blocking calls (unless interrupted)
			        try {
			            put(e);
			            return true;
			        } catch(InterruptedException ie) {
			            Thread.currentThread().interrupt();
			        }
			        return false;
			    }
			});
			
			return options.getIndexType().getIndexer(null, null);
		}

		@SuppressWarnings({ "rawtypes", "unchecked" })
		@Override
		protected Document recordToDocument(Text key, BytesWritable value) throws IOException {
			//extract features
			LocalFeatureList<? extends LocalFeature<?>> features = null;
			try{
				logger.info("Extracting features...");
				features = options.getInputModeOptions().getFeatureType().getKeypointList(value.getBytes());
				
				logger.info("Quantising features...");
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
				
				logger.info("Construcing QLFDocument...");
				//create document
				return new QLFDocument(qkeys, key.toString().substring(0,Math.min(key.getLength(), 20)), null); //FIXME sort out key length
			}
			catch(Throwable e){
				logger.warn("Skipping image: " + key + " due to: " + e.getMessage());
				return null;
			}
		}

		private static synchronized void loadQuantiser(HadoopIndexerOptions options,boolean optimise) throws IOException {
			if (quantiser == null) {
				quantiser = readQuantiser(options,optimise);
			}
		}

		
	}
	
	protected static Cluster<?, ?> readQuantiser(HadoopIndexerOptions options,boolean optimise) throws IOException {
		Cluster<?, ?> quantiser = null;
		System.out.println("Loading codebook...");
		String codebookURL = options.getInputModeOptions().getQuantiserFile();
		options.getInputModeOptions().quantiserType = HadoopClusterQuantiserOptions.sniffClusterType(codebookURL);
		
		if (options.getInputModeOptions().quantiserType != null)
		{
			quantiser = IOUtils.read(HadoopClusterQuantiserOptions.getClusterInputStream(codebookURL), options.getInputModeOptions().getQuantiserType().getClusterClass());
			if(optimise)
				quantiser.optimize(options.getInputModeOptions().quantiserExact);
		}
		System.out.println("Done!");
		return quantiser;
		
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

		if (options.getInputMode() == InputMode.QUANTISED_FEATURES) {
			job.setMapperClass(QFIndexerMapper.class);
		} else { 
			if (options.shardPerThread) {
				job.setMapperClass(MultithreadedMapper.class);
				MultithreadedMapper.setMapperClass(job, MTImageIndexerMapper.class);
				MultithreadedMapper.setNumberOfThreads(job, options.getMultithread());				
			} else {
				job.setMapperClass(ImageIndexerMapper.class);
			}
		}
		// Load quantiser (if it exists), extract header, count codebook size
		String quantFile = options.getInputModeOptions().getQuantiserFile();
		if(quantFile!=null){
			System.out.println("Loading codebook to see its size");
			Cluster<?, ?> quantiser = readQuantiser(options,false);
			System.out.println("Setting codebook size: " + quantiser.getNumberClusters());
			job.getConfiguration().setInt(QUANTISER_SIZE, quantiser.getNumberClusters());
			if(quantiser.getNumberClusters() < options.getNumReducers())
				options.setNumReducers(quantiser.getNumberClusters());
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
				if(job.getConfiguration().getInt(QUANTISER_SIZE, -1) == -1){
					job.setPartitionerClass(NewSplitEmittedTerm.SETPartitionerHashedTerm.class);
				}
				else{
					job.setPartitionerClass(NewSplitEmittedTerm.SETPartitionerCodebookAwareTerm.class);
				}
				
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
//		args = new String[] { 
//				"-t", "BASIC",
//				"-j", "4",
//				"-nr", "1",
//				"-fc", "QuantisedKeypoint",
//				"-o", "/Users/jsh2/test.index",
//				"-m", "IMAGES", 
//				"-q", "hdfs://seurat.ecs.soton.ac.uk/data/codebooks/small-10.seq/final",
//				"hdfs://seurat.ecs.soton.ac.uk/data/image-net-timetests/image-net-10.seq"
//		};
		
		ToolRunner.run(new HadoopIndexer(), args);
	}
}
