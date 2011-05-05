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
 * The Original Code is PositionPostingImpl.java
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
import org.terrier.utility.ArrayUtils;

public class PositionPostingImpl extends BasicPostingImpl implements PositionPosting {
	/**
	 * This stores how many bits were used to encode each
	 * of the position elements. The length of this array
	 * is the number of position ordinates per posting.
	 */
	protected int [] positionBits;
	
	int[][] positions;

	public PositionPostingImpl(int docid, int frequency, int[][] _positions, int [] positionBits) {
		super(docid, frequency);
		positions = _positions;
		this.positionBits = positionBits;
	}
	
	@Override
	public int[][] getPositions() {
		return positions;
	}

	@Override
	public void readFields(DataInput in) throws IOException {
		super.readFields(in);
		
		positions = new int[tf][]; 
		for(int i=0;i<tf;i++) {
			for (int o=0; o<positionBits.length; o++) 
				positions[i][o] = WritableUtils.readVInt(in);
		}
	}

	@Override
	public void write(DataOutput out) throws IOException {
		super.write(out);
		
		for(int [] pos : positions) {
			for (int o=0; o<positionBits.length; o++)
				WritableUtils.writeVInt(out, pos[o]);
		}
	}

	@Override
	public WritablePosting asWritablePosting() {
		int[][] newPos = positions.clone();
		
		for(int row=0; row<positions.length; row++)
			newPos[row]=positions[row].clone();
		
		return new PositionPostingImpl(id, tf, newPos, positionBits);
	}
	
	@Override
	public String toString()
	{
		String pos = "";
		for (int [] n : positions) pos += "(" + ArrayUtils.join(n, ",") + "),";
		pos.subSequence(0, pos.length() - 1);
		
		return "(" + id + "," + tf + ",B[" + pos + "])";
	}
}
