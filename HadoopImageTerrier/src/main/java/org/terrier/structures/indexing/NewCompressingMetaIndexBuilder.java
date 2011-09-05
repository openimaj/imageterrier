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
 * The Original Code is NewCompressingMetaIndexBuilder.java
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
package org.terrier.structures.indexing;

import gnu.trove.TObjectIntHashMap;

import java.io.DataInput;
import java.io.DataInputStream;
import java.io.DataOutput;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.apache.hadoop.conf.Configurable;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.BlockLocation;
import org.apache.hadoop.fs.FileStatus;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.InputFormat;
import org.apache.hadoop.mapreduce.InputSplit;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.JobContext;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Partitioner;
import org.apache.hadoop.mapreduce.RecordReader;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.TaskAttemptContext;
import org.apache.hadoop.mapreduce.lib.input.FileSplit;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.hadoop.mapreduce.lib.output.SequenceFileOutputFormat;
import org.apache.log4j.Level;
import org.imageterrier.hadoop.fs.TerrierHDFSAdaptor;
import org.terrier.structures.CompressingMetaIndex.InputStream;
import org.terrier.structures.Index;
import org.terrier.structures.collections.FSOrderedMapFile;
import org.terrier.structures.collections.FSOrderedMapFile.MapFileWriter;
import org.terrier.structures.seralization.FixedSizeTextFactory;
import org.terrier.structures.seralization.FixedSizeWriteableFactory;
import org.terrier.utility.ApplicationSetup;
import org.terrier.utility.ArrayUtils;
import org.terrier.utility.Files;
import org.terrier.utility.Wrapper;
import org.terrier.utility.io.HadoopUtility;

public class NewCompressingMetaIndexBuilder extends CompressingMetaIndexBuilder {

	public NewCompressingMetaIndexBuilder(Index index, String[] _keyNames, int[] _valueLens, String[] _forwardKeys)
	{
		super(index, "meta", _keyNames, _valueLens, _forwardKeys);
	}

	public NewCompressingMetaIndexBuilder(Index index, String _structureName, String[] _keyNames, int[] _valueLens, String[] _forwardKeys) {
		super(index, _structureName, _keyNames, _valueLens, _forwardKeys);
	}

	public static void reverseAsMapReduceJob(Index index, String structureName, String[] keys, Configuration conf) throws Exception
	{
		long time =System.currentTimeMillis();

		Job job = new Job(conf);

		job.setMapOutputKeyClass(KeyValueTuple.class);
		job.setMapOutputValueClass(IntWritable.class);
		job.setMapperClass(MetaIndexMapper.class);
		job.setReducerClass(MetaIndexReducer.class);
		job.setNumReduceTasks(keys.length);
		job.setPartitionerClass(KeyedPartitioner.class);
		job.setInputFormatClass(CompressingMetaIndexInputFormat.class);
		
		job.getConfiguration().setBoolean("mapred.reduce.tasks.speculative.execution", false);
		job.getConfiguration().set("MetaIndexInputStreamRecordReader.structureName", structureName);
		job.getConfiguration().setInt("CompressingMetaIndexBuilder.reverse.keyCount", keys.length);
		job.getConfiguration().set("CompressingMetaIndexBuilder.reverse.keys", ArrayUtils.join(keys, ","));
		job.getConfiguration().set("CompressingMetaIndexBuilder.forward.valueLengths", index.getIndexProperty("index."+structureName+".value-lengths", ""));
		job.getConfiguration().set("CompressingMetaIndexBuilder.forward.keys", index.getIndexProperty("index."+structureName+".key-names", ""));
		FileOutputFormat.setOutputPath(job, new Path(index.getPath(), ".meta"));

		HadoopUtility.toHConfiguration(index, job.getConfiguration());

		//job.setOutputFormatClass(NullOutputFormat.class);
		job.setOutputFormatClass(SequenceFileOutputFormat.class);
		job.setJarByClass(NewCompressingMetaIndexBuilder.class);

		try{
			if (!job.waitForCompletion(true)) throw new Exception("job failed");
		} catch (Exception e) { 
			throw new Exception("Problem running job to reverse metadata", e);
		}

		//only update the index from the controlling process, so that we dont have locking/concurrency issues
		index.setIndexProperty("index."+structureName+".reverse-key-names", ArrayUtils.join(keys, ","));
		index.flush();

		logger.info("Time Taken = "+((System.currentTimeMillis()-time)/1000)+" seconds");		
	}

	static class MetaIndexMapper extends Mapper<IntWritable, Wrapper<String[]>, KeyValueTuple, IntWritable> {		
		String[] reverseKeyNames;
		int[] reverseKeyIndices;
		int reverseKeyCount;

		@Override
		protected void setup(Context context) throws IOException, InterruptedException {
			TerrierHDFSAdaptor.initialiseHDFSAdaptor(context.getConfiguration());
			
			reverseKeyCount = context.getConfiguration().getInt("CompressingMetaIndexBuilder.reverse.keyCount", 0);
			reverseKeyNames = context.getConfiguration().get("CompressingMetaIndexBuilder.reverse.keys", "").split("\\s*,\\s*");
			
			TObjectIntHashMap<String> key2forwardOffset = new TObjectIntHashMap<String>(reverseKeyCount);
			String[] forwardKeyNames = context.getConfiguration().get("CompressingMetaIndexBuilder.forward.keys", "").split("\\s*,\\s*");
			
			int i=0;
			for(String k : forwardKeyNames) {
				key2forwardOffset.put(k, i++);
			}
			
			reverseKeyIndices = new int[reverseKeyNames.length];
			
			i = 0;
			for(String k : reverseKeyNames) {
				reverseKeyIndices[i] = key2forwardOffset.get(k);
			}
		}

		@Override
		protected void map(IntWritable key, Wrapper<String[]> value, Context context) throws IOException, InterruptedException {
			String[] metadata = value.getObject();
			context.setStatus("Processing metadata for document "+ key.get());
			
			for(int i=0;i<reverseKeyCount;i++) {
				context.write(new KeyValueTuple(reverseKeyNames[i], metadata[i]), key);
			}
			
			context.progress();
		}
	}
	
	static class MetaIndexReducer extends Reducer<KeyValueTuple, IntWritable, Object, Object> {
		String[] reverseKeyNames;
		int[] reverseKeyIndices;
		int reverseKeyCount;
		
		String currentReducingKey = null;
		MapFileWriter currentReducingOutput;
		Index index;
		Path reduceTaskFileDestinations;
		TObjectIntHashMap<String> key2reverseOffset = null;
		TObjectIntHashMap<String> key2valuelength = null;
		FixedSizeWriteableFactory<Text> keyFactory;
		int duplicateKeyCount = 0;
		int currentKeyTupleCount = 0;
		
		@Override
		protected void setup(Context context) throws IOException, InterruptedException {
			TerrierHDFSAdaptor.initialiseHDFSAdaptor(context.getConfiguration());
			
			Index.setIndexLoadingProfileAsRetrieval(false);
			index = HadoopUtility.fromHConfiguration(context.getConfiguration());
			reduceTaskFileDestinations = FileOutputFormat.getWorkOutputPath(context);
			
			String structureName = context.getConfiguration().get("MetaIndexInputStreamRecordReader.structureName", "");
			reverseKeyCount = context.getConfiguration().getInt("CompressingMetaIndexBuilder.reverse.keyCount", 0);
			reverseKeyNames = context.getConfiguration().get("CompressingMetaIndexBuilder.reverse.keys", "").split("\\s*,\\s*");
			
			key2reverseOffset = new TObjectIntHashMap<String>(reverseKeyCount);
			
			int i=0;
			for(String k : reverseKeyNames) {
				key2reverseOffset.put(k, i++);
			}
			
			key2valuelength = new TObjectIntHashMap<String>(reverseKeyCount);
			String[] allKeys = index.getIndexProperty("index."+structureName+".key-names", "").split("\\s*,\\s*");
			String[] allValueLens = index.getIndexProperty("index."+structureName+".value-lengths", "").split("\\s*,\\s*");
			
			i=0;
			for(String k : allKeys) {
				logger.debug("Key "+ k + " value length="+ allValueLens[i]);
				key2valuelength.put(k, Integer.parseInt(allValueLens[i++]));
			}
		}

		/** Reduce function. Input Key: (meta Key name, meta Key value) Value: list of matching docids. */
		@Override
		protected void reduce(KeyValueTuple metaTuple, Iterable<IntWritable> docidsIterable, Context context) throws IOException, InterruptedException {
			Iterator<IntWritable> docids = docidsIterable.iterator();
			
			if (currentReducingKey == null || !  metaTuple.getKeyName().equals(currentReducingKey))
			{
				if (currentReducingKey != null)
				{
					logger.info("currentKey was "+ currentReducingKey + " ("+currentKeyTupleCount+" entries) new Key is " + metaTuple.getKeyName()
							+ " : force closed");
					currentReducingOutput.close();
					if (duplicateKeyCount > 0)
					{
						logger.warn("MetaIndex key "+currentReducingKey + " had "+ duplicateKeyCount + " distinct values with duplicated associated document ids");
					}
					currentReducingOutput = null;
				}
				currentKeyTupleCount = 0;
				duplicateKeyCount = 0;
				currentReducingKey = metaTuple.getKeyName();
				currentReducingOutput = openMapFileWriter(currentReducingKey, context);
				logger.info("Opening new MapFileWriter for key "+ currentReducingKey);
			}
			
			final IntWritable docid = docids.next();
			final Text key = keyFactory.newInstance();
			key.set(metaTuple.getValue());
			currentReducingOutput.write(key, docid);
			currentKeyTupleCount++;
			
			int extraCount  = 0;
			while (docids.hasNext()) {
				docids.next();
				extraCount++;
			}
			
			context.progress();
			
			if (extraCount > 0) {
				//logger.warn("Key "+currentReducingKey + " value "+ metaTuple.getValue() + " had "+ extraCount +" extra documents. First document selected.");
				duplicateKeyCount++;
			}
			context.setStatus("Reducing metadata value "+ metaTuple.getValue());
		}
		
		@Override
		protected void cleanup(Context context) throws IOException, InterruptedException {
			if (currentKeyTupleCount > 0)
			{
				logger.info("Finished reducing for " + currentReducingKey +", with " +currentKeyTupleCount +" entries");
			}
			if (duplicateKeyCount > 0)
			{
				logger.warn("MetaIndex key "+currentReducingKey + " had "+ duplicateKeyCount + " distinct values with duplicated associated document ids");
			}
			if (currentReducingOutput != null)
			currentReducingOutput.close();
		}
		
		/* open a MapFileWriter for the specified key. This will automatically promoted to the index folder when the job is finished */
		protected MapFileWriter openMapFileWriter(String keyName, Context context) throws IOException
		{
			final int metaKeyIndex = key2reverseOffset.get(keyName);
			final int valueLength = key2valuelength.get(keyName);
			keyFactory = new FixedSizeTextFactory(valueLength);
			logger.info("Opening MapFileWriter for key "+ keyName + " - index " + metaKeyIndex);
			return FSOrderedMapFile.mapFileWrite(reduceTaskFileDestinations.toString() /*index.getPath()*/ 
						+ "/" + index.getPrefix() + "."
						+ context.getConfiguration().get("MetaIndexInputStreamRecordReader.structureName")
						+ "-"+metaKeyIndex+FSOrderedMapFile.USUAL_EXTENSION
			);
		}
	}
	
	public static class KeyedPartitioner extends Partitioner<KeyValueTuple, IntWritable> implements Configurable
	{
		protected int keyCount;
		protected TObjectIntHashMap<String> key2reverseOffset = null;
		protected Configuration conf;
		
		@Override
		public int getPartition(KeyValueTuple kv, IntWritable docid, int numReducers) {
			if (numReducers == 1)
				return 0;
			final String key = kv.getKeyName();
			final int keyIndex = key2reverseOffset.get(key);
			return keyIndex % numReducers;
		}

		@Override
		public Configuration getConf() {
			return conf;
		}

		@Override
		public void setConf(Configuration conf) {
			this.conf = conf;
			
			keyCount = conf.getInt("CompressingMetaIndexBuilder.reverse.keyCount", 0);
			key2reverseOffset = new TObjectIntHashMap<String>();
			String[] keys = conf.get("CompressingMetaIndexBuilder.reverse.keys", "").split("\\s*,\\s*");
			int i=0;
			for(String k : keys) {
				key2reverseOffset.put(k, i++);
			}
		}
	}
	
	public static class CompressingMetaIndexInputFormat extends InputFormat<IntWritable, Wrapper<String[]>>
	{
		static String STRUCTURE_NAME_JC_KEY = "MetaIndexInputStreamRecordReader.structureName";
		
		static class MetaIndexSplit extends FileSplit {
			int startId;
			int endId;
			
			public MetaIndexSplit() {
				super(null, (long)0, (long)0, new String[0]);
			}
			
			public MetaIndexSplit(Path file, long start, long length, String[] hosts, int _startId, int _endId) {
				super(file, start, length, hosts);
				startId = _startId;
				endId = _endId;
			}			
			
			@Override
			public void readFields(DataInput in) throws IOException {
				super.readFields(in);
				startId = in.readInt();
				endId = in.readInt();
			}

			@Override
			public void write(DataOutput out) throws IOException {
				super.write(out);
				out.writeInt(startId);
				out.writeInt(endId);
			}
			
			@Override
			public String toString()
			{
				StringBuilder rtr = new StringBuilder();
				rtr.append("MetaIndexSplit: BlockSize=").append(this.getLength());
				rtr.append(" startAt=").append(+this.getStart());
				try{
					rtr.append(" hosts=");
					rtr.append(ArrayUtils.join(this.getLocations(), ","));
				}
				catch (IOException ioe ) {
					logger.warn("Problem getting locations", ioe);
				}
				rtr.append(" ids=["+startId+","+endId +"]");
				return rtr.toString();
			}
		}		
		
		static class MetaIndexInputStreamRecordReader extends RecordReader<IntWritable, Wrapper<String[]>>
		{
			final InputStream in;
			final int startID;
			final int endID;
			
			IntWritable currentKey = new IntWritable();
			Wrapper<String[]> currentValue = new Wrapper<String[]>();
			
			public MetaIndexInputStreamRecordReader(Index index, String structureName, int startingDocID, int endingID) throws IOException {
				in = new InputStream(index, structureName, startingDocID, endingID);
				startID = startingDocID;
				endID = endingID;
			}
			
			@Override
			public void close() throws IOException {
				in.close();
			}

			@Override
			public float getProgress() throws IOException {
				return (float)(in.getIndex() - startID)/(float)(endID - startID);
			}

			@Override
			public IntWritable getCurrentKey() throws IOException, InterruptedException {
				return currentKey;
			}

			@Override
			public Wrapper<String[]> getCurrentValue() throws IOException, InterruptedException {
				return currentValue;
			}

			@Override
			public void initialize(InputSplit arg0, TaskAttemptContext arg1) throws IOException, InterruptedException {
				
			}

			@Override
			public boolean nextKeyValue() throws IOException, InterruptedException {
				if (!in.hasNext()) return false;
				
				//these methods MUST have this order
				currentValue.setObject(in.next());
				currentKey.set(in.getIndex());
				return true;
			}
			
		}
		
		private static final String[] getHosts(FileStatus fs, FileSystem f, long start, long len) throws IOException
		{
			BlockLocation[] bs = f.getFileBlockLocations(fs, start, len);
			Set<String> hosts = new HashSet<String>();
			
			for(BlockLocation b : bs) {
				for(String host : b.getHosts()) {
					hosts.add(host);
				}
			}
			
			return hosts.toArray(new String[0]);
		}
		
		long forcedDataFileBlockSize = -1;
		
		/* Permit the blocksize to be overridden, useful for testing different code paths */ 
		public void overrideDataFileBlockSize(long blocksize)
		{
			forcedDataFileBlockSize = blocksize;
		}

		@Override
		public RecordReader<IntWritable, Wrapper<String[]>> createRecordReader(InputSplit split, TaskAttemptContext context) throws IOException, InterruptedException {
			TerrierHDFSAdaptor.initialiseHDFSAdaptor(context.getConfiguration());
			
			//load the index
			Index.setIndexLoadingProfileAsRetrieval(false);
			Index index = HadoopUtility.fromHConfiguration(context.getConfiguration());
			if (index == null)
				throw new IOException("Index could not be loaded from JobConf: " + Index.getLastIndexLoadError() );
			
			//determine the structure to work on
			String structureName = context.getConfiguration().get(STRUCTURE_NAME_JC_KEY);
			if (structureName == null)
				throw new IOException("JobConf property "+STRUCTURE_NAME_JC_KEY+" not specified");
			
			//get the split
			MetaIndexSplit s = (MetaIndexSplit)split;
			return new MetaIndexInputStreamRecordReader(index, structureName, s.startId, s.endId);	
		}

		@Override
		public List<InputSplit> getSplits(JobContext context) throws IOException, InterruptedException {
			logger.setLevel(Level.DEBUG);
			//HadoopUtility.loadTerrierJob(jc);
			
			List<InputSplit> splits = new ArrayList<InputSplit>();
			Index index = HadoopUtility.fromHConfiguration(context.getConfiguration());
			String structureName = context.getConfiguration().get(STRUCTURE_NAME_JC_KEY);
			final String dataFilename = index.getPath() + ApplicationSetup.FILE_SEPARATOR + index.getPrefix() + "." + structureName + ".zdata";
			final String indxFilename = index.getPath() + ApplicationSetup.FILE_SEPARATOR + index.getPrefix() + "." + structureName + ".idx";
			final DataInputStream idx = new DataInputStream(Files.openFileStream(indxFilename));
			FileSystem fSys = FileSystem.get(context.getConfiguration());
			FileStatus fs = fSys.getFileStatus(new Path(dataFilename));
			
			final int entryCount = index.getIntIndexProperty("index."+structureName+".entries", 0);
			long dataFileBlockSize = fs.getBlockSize();
			if (forcedDataFileBlockSize != -1) dataFileBlockSize = forcedDataFileBlockSize;
			logger.debug("Block size for "+ dataFilename + " is " + dataFileBlockSize);
			//logger.debug("FSStatus("+dataFilename+")="+ fs.toString());
			int startingId = 0;
			int currentId = 0;
			long startingBlockLocation = 0;
			long blockSizeSoFar = 0;
			long lastRead = idx.readLong();
			while(++currentId < entryCount)
			{
				lastRead = idx.readLong();
				blockSizeSoFar = lastRead - startingBlockLocation;
				//logger.debug("Offset for docid "+ currentId + " is " + lastRead + " blockSizeSoFar="+blockSizeSoFar + " blockStartsAt="+startingBlockLocation);
				if (blockSizeSoFar > dataFileBlockSize)
				{
					final String[] hosts = getHosts(fs, fSys, startingBlockLocation, blockSizeSoFar);
					MetaIndexSplit s = new MetaIndexSplit(new Path(dataFilename), startingBlockLocation, blockSizeSoFar, hosts, startingId, currentId);
					splits.add(s);
					logger.debug("Got split: "+ s.toString());
					
					blockSizeSoFar = 0;
					startingBlockLocation = lastRead + 1;
					startingId = currentId +1;
				}
			}
			if (startingId < currentId)
			{
				blockSizeSoFar = lastRead - startingBlockLocation;
				final String[] hosts = getHosts(fs, fSys, startingBlockLocation, blockSizeSoFar);
				MetaIndexSplit s = new MetaIndexSplit(new Path(dataFilename), startingBlockLocation, blockSizeSoFar, hosts, startingId, currentId-1);
				logger.debug("Got last split: "+ s);
				splits.add(s);
			}
			idx.close();
			
			logger.debug("Got "+ splits.size() + " splits when splitting meta index");
			
			return splits;			
		}	
	}
}
