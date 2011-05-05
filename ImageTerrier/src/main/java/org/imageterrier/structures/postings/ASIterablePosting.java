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
 * The Original Code is ASIterablePosting.java
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
package org.imageterrier.structures.postings;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import org.apache.hadoop.io.WritableUtils;
import org.terrier.compression.BitIn;
import org.terrier.structures.DocumentIndex;
import org.terrier.structures.postings.BasicIterablePosting;
import org.terrier.structures.postings.WritablePosting;

public class ASIterablePosting extends BasicIterablePosting implements ASPosting
{
	int[] simulations;
	
	public ASIterablePosting(){super();}
	
	public ASIterablePosting(BitIn _bitFileReader, int _numEntries, DocumentIndex doi) throws IOException {
		super(_bitFileReader, _numEntries, doi);
	}
	
	@Override
	public int next() throws IOException {
		if (numEntries-- == 0) return EOL;
		
		id = bitFileReader.readGamma() + id;
		tf = bitFileReader.readUnary();
		
		//TODO: this has a memory allocation for every posting in the posting list. can we reuse an array?
		int numOfTerms = tf;//bitFileReader.readUnary() - 1;
		simulations = new int[numOfTerms];
		
//		int previousPosition = -1;
		for (int k=0; k<numOfTerms; k++) {
			//positions[k] = Utils.fromIndex(previousPosition = bitFileReader.readGamma() + previousPosition - 1);
			simulations[k] = bitFileReader.readBinary(32);
		}
		
		return id;
	}
	
	@Override
	public int [] getSimulations() {
		return simulations;
	}

	@Override
	public void readFields(DataInput in) throws IOException {
		super.readFields(in);
		
		simulations = new int[tf]; 
		for(int i=0;i<tf;i++) {
			int index = WritableUtils.readVInt(in);
			simulations[i] = index;
		}
	}

	@Override
	public void write(DataOutput out) throws IOException {
		super.write(out);
		
		for(int sim : simulations) {
			WritableUtils.writeVInt(out, sim);
		}
	}

	@Override
	public WritablePosting asWritablePosting() {
		int[] newSim = simulations.clone();
		
		return new ASPostingImpl(getId(), getFrequency(), newSim);
	}

	@Override
	public String toString()
	{
		String pos = "";
		for (int n : simulations) pos += "(" + n + "),";
		pos.subSequence(0, pos.length() - 1);
		
		return "(" + id + "," + tf + ",B[" + pos + "])";
	}
}
