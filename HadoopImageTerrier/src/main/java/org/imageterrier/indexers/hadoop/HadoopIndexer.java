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
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.BytesWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.io.compress.GzipCodec;
import org.apache.hadoop.mapred.RunningJob;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.JobID;
import org.apache.hadoop.mapreduce.lib.input.SequenceFileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.hadoop.mapreduce.lib.output.SequenceFileOutputFormat;
import org.apache.hadoop.mapreduce.lib.partition.HashPartitioner;
import org.apache.hadoop.util.ToolRunner;
import org.imageterrier.basictools.BasicTerrierConfig;
import org.imageterrier.hadoop.mapreduce.PositionAwareSequenceFileInputFormat;
import org.imageterrier.indexing.BasicSinglePassIndexer;
import org.imageterrier.locfile.PositionSpec;
import org.imageterrier.locfile.QLFDocument;
import org.openimaj.feature.local.quantised.QuantisedLocalFeature;
import org.openimaj.image.feature.local.keypoints.quantised.QuantisedKeypoint;
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
import org.terrier.utility.io.WrappedIOException;


/**
 * @author Jonathon Hare
 *
 */
public class HadoopIndexer extends AbstractHadoopIndexer {
	static {
		//initialise terrier
		BasicTerrierConfig.configure();
	}
	
	public static final String INDEXER_CLASS_KEY = "indexer.class";
	public static final String INDEXER_POSITION_SPEC_KEY = "indexer.position.spec";
	public static final String INDEXER_FEATURE_CLASS = "indexer.feature.class";

	/**
	 * The mapper implementation
	 */
	static class IndexerMapper extends HadoopIndexerMapper<BytesWritable> {
		protected Class<? extends QuantisedLocalFeature<?>> featureClass;

		@SuppressWarnings("unchecked")
		@Override
		protected ExtensibleSinglePassIndexer createIndexer(Context context) throws IOException {
			try {
				featureClass = (Class<? extends QuantisedLocalFeature<?>>) Class.forName(context.getConfiguration().get(INDEXER_FEATURE_CLASS));
			} catch (ClassNotFoundException e) {
				throw new WrappedIOException(e);
			}

			return createIndexerInstance(context.getConfiguration());
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
			return createIndexerInstance(context.getConfiguration());
		}
	}

	protected static ExtensibleSinglePassIndexer createIndexerInstance(Configuration conf) throws IOException {
		try {
			Class<?> clz = Class.forName(conf.get(INDEXER_CLASS_KEY));
			try {
				Constructor<?> cnstr = clz.getConstructor(String.class, String.class);
				try {
					return (ExtensibleSinglePassIndexer) cnstr.newInstance(null, null);
				} catch (Exception e) {
					throw new WrappedIOException(e);
				}
			} catch (SecurityException e) {
				throw new WrappedIOException(e);
			} catch (NoSuchMethodException e) {
				try {
					//special case for Position index
					Constructor<?> cnstr = clz.getConstructor(String.class, String.class, PositionSpec.class);
					return (ExtensibleSinglePassIndexer) cnstr.newInstance(null, null, PositionSpec.decode(conf.get(INDEXER_POSITION_SPEC_KEY)));
				} catch (Exception e2) {
					throw new WrappedIOException(e2);
				}
			}
		} catch (ClassNotFoundException e) {
			throw new WrappedIOException(e);
		}
	}

	private static final String usage()
	{
		return "Usage: HadoopIndexing [-p]";
	}

	/** Starts the Map reduce indexing.
	 * @param args
	 * @throws Exception
	 */
	@Override
	public int run(String[] args) throws Exception {
		long time =System.currentTimeMillis();

		Job job = new Job(getConf()) {
			@Override
			//this is broken in hadoop 0.20.1, and so we hack to fix
			public JobID getJobID() {
				Field f;
				try {
					f = Job.class.getDeclaredField("info");
					f.setAccessible(true);
					return ((RunningJob)f.get(this)).getID();
				} catch (Exception e) {
					e.printStackTrace();
					return null;
				}
			}
		};

		job.setJobName("terrierIndexing");

		if (Files.exists(ApplicationSetup.TERRIER_INDEX_PATH) && Index.existsIndex(ApplicationSetup.TERRIER_INDEX_PATH, ApplicationSetup.TERRIER_INDEX_PREFIX))
		{
			logger.fatal("Cannot index while index exists at " + ApplicationSetup.TERRIER_INDEX_PATH + "," + ApplicationSetup.TERRIER_INDEX_PREFIX);
			return 1;
		}

		boolean docPartitioned = false;
		int numberOfReducers = Integer.parseInt(ApplicationSetup.getProperty("terrier.hadoop.indexing.reducers", "26"));
		if (args.length==2 && args[0].equals("-p"))
		{
			logger.info("Document-partitioned Mode, "+numberOfReducers+" output indices.");
			numberOfReducers = Integer.parseInt(args[1]);
			docPartitioned = true;
		}
		else if (args.length == 0)
		{
			logger.info("Term-partitioned Mode, "+numberOfReducers+" reducers creating one inverted index.");
			docPartitioned = false;
			if (numberOfReducers > 26)
			{
				logger.warn("Excessive reduce tasks ("+numberOfReducers+") in use - SplitEmittedTerm.SETPartitionerLowercaseAlphaTerm can use 26 at most");
			}
		} else
		{
			logger.fatal(usage());
			return 1;
		}

		job.setMapperClass(IndexerMapper.class);
		job.setReducerClass(IndexerReducer.class);

		FileOutputFormat.setOutputPath(job, new Path(ApplicationSetup.TERRIER_INDEX_PATH));
		job.setMapOutputKeyClass(NewSplitEmittedTerm.class);
		job.setMapOutputValueClass(MapEmittedPostingList.class);
		job.getConfiguration().setBoolean("indexing.hadoop.multiple.indices", docPartitioned);

		if (!job.getConfiguration().get("mapred.job.tracker").equals("local")) {
			job.getConfiguration().set("mapred.map.output.compression.codec", GzipCodec.class.getCanonicalName());
			job.getConfiguration().setBoolean("mapred.compress.map.output", true);
		} else {
			job.getConfiguration().setBoolean("mapred.compress.map.output", false);
		}
		
		job.setInputFormatClass(PositionAwareSequenceFileInputFormat.class); //important
		//job.setOutputFormatClass(NullOutputFormat.class);
		job.setOutputFormatClass(SequenceFileOutputFormat.class);
		job.setSortComparatorClass(NewSplitEmittedTerm.SETRawComparatorTermSplitFlush.class);
		job.setGroupingComparatorClass(NewSplitEmittedTerm.SETRawComparatorTerm.class);

		job.getConfiguration().setBoolean("mapred.reduce.tasks.speculative.execution", false);

		//parse the collection.spec
//		BufferedReader specBR = Files.openFileReader(ApplicationSetup.COLLECTION_SPEC);
//		String line = null;
		List<Path> paths = new ArrayList<Path>();
//		while((line = specBR.readLine()) != null) {
//			if (line.startsWith("#"))
//				continue;
//			paths.add(new Path(line));
//		}
//		specBR.close();
//		paths.add(new Path("hdfs://degas/data/ukbench-sift-random-1000000/part-r-00000"));				//TEST
		paths.add(new Path("/Users/jsh2/test.seq"));													//TEST
		job.getConfiguration().set(INDEXER_FEATURE_CLASS, QuantisedKeypoint.class.getCanonicalName()); 	//TEST
		job.getConfiguration().set(INDEXER_CLASS_KEY, BasicSinglePassIndexer.class.getCanonicalName()); //TEST
		
		SequenceFileInputFormat.setInputPaths(job, paths.toArray(new Path[paths.size()]));
		job.setNumReduceTasks(numberOfReducers);
		if (numberOfReducers> 1) {
			if (docPartitioned)
				job.setPartitionerClass(NewSplitEmittedTerm.SETPartitioner.class);
			else
				job.setPartitionerClass(NewSplitEmittedTerm.SETPartitionerLowercaseAlphaTerm.class);
		} else {
			//for JUnit tests, we seem to need to restore the original partitioner class
			job.setPartitionerClass(HashPartitioner.class);
		}

		job.setJarByClass(this.getClass());

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
			deleteTaskFiles(ApplicationSetup.TERRIER_INDEX_PATH, jobId);
		}

		if (ranOK) {
			if (! docPartitioned) {
				if (numberOfReducers > 1) {
					mergeLexiconInvertedFiles(ApplicationSetup.TERRIER_INDEX_PATH, numberOfReducers);
				}
			}

			finish(ApplicationSetup.TERRIER_INDEX_PATH, docPartitioned ? numberOfReducers : 1, job.getConfiguration());
		}

		System.out.println("Time Taken = "+((System.currentTimeMillis()-time)/1000)+" seconds");

		return 0;
	}

	public static void main(String[] args) throws Exception {
		ApplicationSetup.setProperty("terrier.hadoop.indexing.reducers", "1");
		
		ToolRunner.run(new HadoopIndexer(), args);
	}
}
