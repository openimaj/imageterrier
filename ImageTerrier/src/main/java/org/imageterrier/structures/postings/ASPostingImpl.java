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
 * The Original Code is ASPostingImpl.java
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
import org.terrier.structures.postings.BasicPostingImpl;
import org.terrier.structures.postings.WritablePosting;

public class ASPostingImpl extends BasicPostingImpl implements ASPosting {
	int[] simulations;

	public ASPostingImpl() {
	}

	public ASPostingImpl(int docid, int frequency, int[] _simulations) {
		super(docid, frequency);
		simulations = _simulations;
	}
	
	@Override
	public int[] getSimulations() {
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
		
		for(int pos : simulations) {
			WritableUtils.writeVInt(out, pos);
		}
	}

	@Override
	public WritablePosting asWritablePosting()
	{
		int[] newNei = simulations.clone();
		
		return new ASPostingImpl(id, tf, newNei);
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
