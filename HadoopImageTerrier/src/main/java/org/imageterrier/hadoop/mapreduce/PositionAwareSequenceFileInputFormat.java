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
 * The Original Code is PositionAwareSequenceFileInputFormat.java
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
package org.imageterrier.hadoop.mapreduce;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.hadoop.fs.BlockLocation;
import org.apache.hadoop.fs.FileStatus;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.mapreduce.InputSplit;
import org.apache.hadoop.mapreduce.JobContext;
import org.apache.hadoop.mapreduce.lib.input.FileSplit;
import org.apache.hadoop.mapreduce.lib.input.SequenceFileInputFormat;

import com.sun.org.apache.commons.logging.Log;
import com.sun.org.apache.commons.logging.LogFactory;

/**
 * Subclass of SequenceFileInputFormat that provides position-aware splits
 * 
 * @author Jonathon Hare
 *
 * @param <K>
 * @param <V>
 */
public class PositionAwareSequenceFileInputFormat<K, V> extends SequenceFileInputFormat<K, V> {
	private static final Log LOG = LogFactory.getLog(PositionAwareSequenceFileInputFormat.class);
	private static final double SPLIT_SLOP = 1.1;   // 10% slop
	
	/** 
	 * Generate the list of files and make them into FileSplits.
	 */ 
	@Override
	public List<InputSplit> getSplits(JobContext job) throws IOException {
		long minSize = Math.max(getFormatMinSplitSize(), getMinSplitSize(job));
		long maxSize = getMaxSplitSize(job);
		int splitnum = 0;
		
		// generate splits
		List<InputSplit> splits = new ArrayList<InputSplit>();
		for (FileStatus file: listStatus(job)) {
			Path path = file.getPath();
			FileSystem fs = path.getFileSystem(job.getConfiguration());
			long length = file.getLen();
			BlockLocation[] blkLocations = fs.getFileBlockLocations(file, 0, length);
			if ((length != 0) && isSplitable(job, path)) { 
				long blockSize = file.getBlockSize();
				long splitSize = computeSplitSize(blockSize, minSize, maxSize);

				long bytesRemaining = length;
				while (((double) bytesRemaining)/splitSize > SPLIT_SLOP) {
					int blkIndex = getBlockIndex(blkLocations, length-bytesRemaining);
					
					splits.add(new PositionAwareSplitWrapper<FileSplit>(new FileSplit(path, length-bytesRemaining, splitSize, blkLocations[blkIndex].getHosts()), splitnum++));
					bytesRemaining -= splitSize;
				}

				if (bytesRemaining != 0) {
					splits.add(new PositionAwareSplitWrapper<FileSplit>(new FileSplit(path, length-bytesRemaining, bytesRemaining, blkLocations[blkLocations.length-1].getHosts()), splitnum++));
				}
			} else if (length != 0) {
				splits.add(new PositionAwareSplitWrapper<FileSplit>(new FileSplit(path, 0, length, blkLocations[0].getHosts()), splitnum++));
			} else { 
				//Create empty hosts array for zero length files
				splits.add(new PositionAwareSplitWrapper<FileSplit>(new FileSplit(path, 0, length, new String[0]), splitnum++));
			}
		}
		
		LOG.debug("Total # of splits: " + splits.size());
		return splits;
	}

}
