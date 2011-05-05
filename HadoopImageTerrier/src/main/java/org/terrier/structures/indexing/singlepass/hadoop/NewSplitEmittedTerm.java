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
 * The Original Code is NewSplitEmittedTerm.java
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

import java.io.ByteArrayInputStream;
import java.io.DataInput;
import java.io.DataInputStream;
import java.io.DataOutput;
import java.io.IOException;
import java.io.Serializable;
import java.nio.charset.CharacterCodingException;

import org.apache.hadoop.conf.Configurable;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.io.RawComparator;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.io.WritableComparable;
import org.apache.hadoop.io.WritableUtils;
import org.apache.hadoop.mapreduce.Partitioner;

/**
 * Represents a Term key used during MapReduce Indexing. Term keys are emitted from
 * each map task, and are used for sorting and partitioning the output. Paritioning
 * is done by splitno. Two options for sorting (a) term only, (b) term, split, flush
 * @author richardm
 * @since 3.0
 */
public class NewSplitEmittedTerm implements WritableComparable<NewSplitEmittedTerm> {

	private static final boolean USE_HADOOP_TEXT = false;
	
	/**
	 * Factory method for creating a new Term key object
	 * @param term
	 * @param splitno
	 * @param flushno
	 * @return a new split emitted term.
	 */
	public static NewSplitEmittedTerm createNewTerm(String term, int splitno, int flushno) {
		return new NewSplitEmittedTerm(term, splitno, flushno);
	}
	
	/** The term */
	private String term;
	/** The split that this instance of the term has been processed by */ 
	private int splitno;
	/** The flush within the split that this instance of the term was emitted by */
	private int flushno;
	
	/**
	 * Empty Constructor
	 */
	public NewSplitEmittedTerm() {}
	
	/**
	 * Constructor for a Term key. Is used for sorting map output and partitioning
	 * posting lists between reducers. Each term is only unique in conjunction with
	 * the split and flush that it was emitted from.
	 * @param term 
	 * @param splitno
	 * @param flushno
	 */
	public NewSplitEmittedTerm(String term, int splitno, int flushno) {
		this.term = term;
		this.splitno = splitno;
		this.flushno = flushno;
	}
	
	@Override
	public int hashCode() {
		return term.hashCode() + splitno + flushno;
	}

	@Override
	public boolean equals(Object _o)
	{
		if (! (_o instanceof NewSplitEmittedTerm))
			return false;
		NewSplitEmittedTerm o = (NewSplitEmittedTerm)_o;
		return this.term.equals(o.term) && this.splitno == o.splitno && this.flushno == o.flushno;		
	}
	
	@Override
	public String toString() {
		return term + ":" + splitno + ":" + flushno;
	}

	/**
	 * Read in a Term key object from the input stream 'in'
	 */
	@Override
	public void readFields(DataInput in) throws IOException {
		if (USE_HADOOP_TEXT)
			term = Text.readString(in); 
		else 
			term = in.readUTF();
		splitno = WritableUtils.readVInt(in);
		flushno = WritableUtils.readVInt(in);
	}

	/**
	 * Write out this Term key to output stream 'out'
	 */
	@Override
	public void write(DataOutput out) throws IOException {
		if (USE_HADOOP_TEXT)
			Text.writeString(out, term);
		else
			out.writeUTF(term);
		WritableUtils.writeVInt(out, splitno);
		WritableUtils.writeVInt(out, flushno);		
	}

	/**
	 * Compares this Term key to another term key. Note that terms are
	 * unique only in conjunction with their associated split and flush.  
	 */
	@Override
	public int compareTo(NewSplitEmittedTerm term2) {
		int result;
		if ((result = term.compareTo(term2.getTerm()))!=0) return result;
		if ((result = splitno - term2.getSplitno())!=0) return result;
		return flushno - term2.getFlushno();
	}	
	
	

	/**
	 * @return the term
	 */
	public String getTerm() {
		return term;
	}

	/**
	 * @param term the term to set
	 */
	public void setTerm(String term) {
		this.term = term;
	}

	/**
	 * @return the splitno
	 */
	public int getSplitno() {
		return splitno;
	}

	/**
	 * @param splitno the splitno to set
	 */
	public void setSplitno(int splitno) {
		this.splitno = splitno;
	}

	/**
	 * @return the flushno
	 */
	public int getFlushno() {
		return flushno;
	}

	/**
	 * @param flushno the flushno to set
	 */
	public void setFlushno(int flushno) {
		this.flushno = flushno;
	}
	
	/** Sorter by term only */
	public static class SETRawComparatorTerm implements RawComparator<NewSplitEmittedTerm>, Serializable
	{
		private static final long serialVersionUID = 1L;

		/**
		 * Compares raw Term key 1 to raw Term key 2. Note that only terms are considered.  
		 */
		@Override
		public int compare(byte[] bterm1, int offset1, int length1, byte[] bterm2, int offset2, int length2)
		{
			if (USE_HADOOP_TEXT)
			{
				try {
					return Text.decode(bterm1, offset1, length1).trim().compareTo(Text.decode(bterm2, offset2, length2).trim());
				} catch (CharacterCodingException e) {
					return 0;
				}
			}
			else
			{
				try {
					DataInputStream b1S = new DataInputStream(new ByteArrayInputStream(bterm1, offset1, length1));
					DataInputStream b2S = new DataInputStream(new ByteArrayInputStream(bterm2, offset2, length2));
					String term1 = b1S.readUTF();
					String term2 = b2S.readUTF();
					return term1.trim().compareTo(term2.trim());
				} catch (IOException e) {
					System.err.println("ERROR during raw comparision of term objects, unable to read input streams.");
					e.printStackTrace();
				}
				return 0;
			}
		}

		@Override
		public int compare(NewSplitEmittedTerm o1, NewSplitEmittedTerm o2) {
			return o1.getTerm().compareTo(o2.getTerm());
		}
	}
	
	public static class SETRawComparatorTermSplitFlush implements RawComparator<NewSplitEmittedTerm>, Serializable
	{
		private static final long serialVersionUID = 1L;
	
		/**
		 * Compares raw Term key 1 to raw Term key 2. Note that terms are
		 * unique only in conjunction with their associated split and flush.  
		 */		
		@Override
		public int compare(byte[] bterm1, int offset1, int length1, byte[] bterm2, int offset2, int length2)
		{
			//this implementation doesnt create NewSplitEmittedTerm objects, saving a bit on gc
			DataInputStream b1S = new DataInputStream(new ByteArrayInputStream(bterm1, offset1, length1));
			DataInputStream b2S = new DataInputStream(new ByteArrayInputStream(bterm2, offset2, length2));
			try {
				String t1;
				String t2;
				if (USE_HADOOP_TEXT)
				{
					t1 = Text.readString(b1S);
					t2 = Text.readString(b2S);
				} else {
					t1 = b1S.readUTF();
					t2 = b2S.readUTF();
				}
				int result = t1.compareTo(t2);
				if (result != 0)
					return result;
				int i1 = WritableUtils.readVInt(b1S);
				int i2 = WritableUtils.readVInt(b2S);
				if (i1 != i2)
					return i1 - i2;
				i1 = WritableUtils.readVInt(b1S);
				i2 = WritableUtils.readVInt(b2S);
				return i1 - i2;
			} catch (IOException e) {
				System.err.println("ERROR during raw comparision of term objects, unable to read input streams.");
				e.printStackTrace();
				return 0;
			}			
		}

		/**
		 * Compares Term key 1 to Term key 2. Note that terms are
		 * unique only in conjunction with their associated split and flush.  
		 */
		@Override
		public int compare(NewSplitEmittedTerm term1, NewSplitEmittedTerm term2) {
			return term1.compareTo(term2);
		}
	}
	
	/** Partitions NewSplitEmittedTerms by split that they came from.
	 */
	public static class SETPartitioner extends Partitioner<NewSplitEmittedTerm, MapEmittedPostingList> implements Configurable
	{
		Configuration conf;
		
		/** The number of chunks the collection was split into */
		private int numSplits;
		
		/** Retuns the partition for the specified term and posting list, given the specified
		 * number of partitions.
		 */
		@Override
		public int getPartition(NewSplitEmittedTerm term, MapEmittedPostingList posting, int numPartitions)
		{
			//System.err.println("set="+term.toString() + " partition="+ calculatePartition(term.getSplitno(), numPartitions));
			return calculatePartition(term.getSplitno(), numPartitions);
		}
		
		/** Calculates the partitions for a given split number.
		 * @param splitno - which split index, starting at 0
		 * @param numPartitions - number of partitions (reducers) configured
		 * @return the reduce partition number to allocate the split to. */
		public int calculatePartition(int splitno, int numPartitions) {
			final int partitionSize = (int) (Math.ceil((double)numSplits / (double) numPartitions ));
			return splitno / partitionSize;
		}

		@Override
		public Configuration getConf() {
			return conf;
		}

		@Override
		public void setConf(Configuration conf) {
			this.conf = conf;
			numSplits = conf.getInt("mapred.map.tasks", 1);
		}
	}
	
	/** Partitions NewSplitEmittedTerms by term. This version assumes that most initial characters are in lowercase a-z.
	 * 0-9 will goto the first partition, all character higher than 'z' will go to the last partition.
	 */
	public static class SETPartitionerLowercaseAlphaTerm extends Partitioner<NewSplitEmittedTerm, MapEmittedPostingList>
	{
		/** Retuns the partition for the specified term and posting list, given the specified
		 * number of partitions.
		 */
		@Override
		public int getPartition(NewSplitEmittedTerm term, MapEmittedPostingList posting, int numPartitions)
		{
			//System.err.println("set="+term.toString() + " partition="+ calculatePartition(term.getSplitno(), numPartitions));
			return calculatePartition(term.getTerm().charAt(0), numPartitions);
		}
		
		/** Calculates the partitions for a given split number.
		 * @param _initialChar - what's the first character in the term
		 * @param numPartitions - number of partitions (reducers) configured
		 * @return the reduce partition number to allocate the split to. */
		public int calculatePartition(char _initialChar, int numPartitions) {
			final int partitionSize = (int) (Math.ceil( 26.0d / (double) numPartitions ));
			int initialChar = (int)_initialChar;
			if (initialChar < 'a')
				return 0;
			if (initialChar > 'z')
				return numPartitions -1;
			return (initialChar - 97) / partitionSize;
		}
	}	
}
