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
 * The Original Code is NewHadoopRunWriter.java
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
package org.terrier.structures.indexing.singlepass.hadoop;

import java.io.IOException;

import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Mapper;
import org.terrier.compression.MemorySBOS;
import org.terrier.structures.indexing.singlepass.Posting;
import org.terrier.structures.indexing.singlepass.RunWriter;
import org.terrier.utility.io.WrappedIOException;

/** 
 * RunWriter for the MapReduce indexer
 * Updated for the new API
 *  
 * @author Richard McCreadie and Craig Macdonald
 * @author Jonathon Hare
 *  
 * @version $Revision: 1.0 $ 
 * @param <VALUEIN>  
 */
public class NewHadoopRunWriter<VALUEIN> extends RunWriter {
	/** output collector of Map task */
	protected Mapper<Text,VALUEIN,NewSplitEmittedTerm,MapEmittedPostingList>.Context context = null;
	
	/** map task id that is being flushed */
	protected String mapId;
	
	/** flushNo is the number of times this map task is being flushed */
	protected int flushNo;
	
	/** current split id */
	protected int splitId;
	
	/** Create a new HadoopRunWriter, specifying the output collector of the map task
	 * the run number and the flush number.
	 * @param context where to emit the posting lists to
	 * @param mapId the task id of the map currently being processed
	 * @param splitId the id of the split
	 * @param flushNo the number of times that this map task has flushed
	 */
	public NewHadoopRunWriter(Mapper<Text,VALUEIN,NewSplitEmittedTerm,MapEmittedPostingList>.Context context, String mapId, int splitId, int flushNo)
	{
		this.context = context;
		this.mapId = mapId;
		this.flushNo = flushNo;
		this.splitId = splitId;
		this.info = "NewHadoopRunWriter(Map "+ mapId +", flush "+flushNo+")"; 
	}
	
	@Override
	public void beginWrite(int maxSize, int size) throws IOException
	{}
	
	/** Write the posting to the output collector
	 */
	@Override
	public void writeTerm(final String term, final Posting post) throws IOException
	{	
		final MemorySBOS Docs = post.getDocs();
		Docs.pad();
		//get the posting array buffer
		byte[] buffer = new byte[Docs.getMOS().getPos()+1];
		System.arraycopy(Docs.getMOS().getBuffer(), 0, 
				buffer, 0, 
				Math.min(Docs.getMOS().getBuffer().length, Docs.getMOS().getPos()+1));
		
		//emit the term and its posting list
		try {
			context.write(
					NewSplitEmittedTerm.createNewTerm(term, splitId, flushNo), 
					MapEmittedPostingList.create_Hadoop_WritableRunPostingData(
							mapId,
							flushNo, 
							splitId,
							buffer,
							post.getDocF(), post.getTF()));
		} catch (InterruptedException e) {
			throw new WrappedIOException(e);
		}
	}
	
	@Override
	public void finishWrite() throws IOException
	{}
	
	/** This RunWriter does not require that the output be sorted.
	  */
	@Override
	public boolean writeSorted()
	{
		return false;
	}
}
