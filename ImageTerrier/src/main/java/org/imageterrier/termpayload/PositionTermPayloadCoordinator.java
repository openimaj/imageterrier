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
 * The Original Code is PositionTermPayloadCoordinator.java
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
package org.imageterrier.termpayload;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import org.apache.hadoop.io.WritableUtils;
import org.imageterrier.locfile.PositionSpec;
import org.imageterrier.locfile.QLFDocument;
import org.imageterrier.locfile.PositionSpec.PositionSpecMode;
import org.terrier.compression.BitIn;
import org.terrier.compression.BitOut;
import org.terrier.compression.MemorySBOS;
import org.terrier.indexing.Document;
import org.terrier.structures.Index;
import org.terrier.structures.TermPayloadCoordinator;
import org.terrier.utility.ArrayUtils;

public class PositionTermPayloadCoordinator extends TermPayloadCoordinator<int[]> {
	/** The default number of NN terms */
	public static final int DEFAULT_NEAREST_NEIGHBOURS = 15;
	
	protected PositionSpec positionSpec;
	
	public PositionTermPayloadCoordinator() {
		this.positionSpec = null;
	}
	
	public PositionTermPayloadCoordinator(PositionSpec positionSpec) {
		this.positionSpec = positionSpec;
	}
	
	@Override
	public void readConfiguration(Index index) {
		int nPositionOrdinates = index.getIntIndexProperty("positions.ordinates", 0);
		int [] positionBits = new int[nPositionOrdinates];

		positionBits = ArrayUtils.parseCommaDelimitedInts(index.getIndexProperty("positions.nbits", ""));
		if (positionBits.length != nPositionOrdinates)
			throw new RuntimeException("Position bits length is not equal to the number of ordinates");
		
		PositionSpecMode mode = PositionSpecMode.valueOf(index.getIndexProperty("positions.mode", "NONE"));
		double[] lowerBounds = ArrayUtils.parseCommaDelimitedDoubles(index.getIndexProperty("positions.lowerBounds", ""));
		double[] upperBounds = ArrayUtils.parseCommaDelimitedDoubles(index.getIndexProperty("positions.upperBounds", ""));
		
		this.positionSpec = new PositionSpec(mode, positionBits, lowerBounds, upperBounds);
	}
			
	@Override
	public void saveConfiguration(Index currentIndex) {
		super.saveConfiguration(currentIndex);
		
		//now update the index with the extra info about
		currentIndex.setIndexProperty("positions.ordinates", positionSpec.getPositionBits().length+"");
		currentIndex.setIndexProperty("positions.nbits", ArrayUtils.join(positionSpec.getPositionBits(), ","));
		currentIndex.setIndexProperty("positions.mode", positionSpec.getMode().name());
		currentIndex.setIndexProperty("positions.lowerBounds", ArrayUtils.join(positionSpec.getLowerBounds(), ","));
		currentIndex.setIndexProperty("positions.upperBounds", ArrayUtils.join(positionSpec.getUpperBounds(), ","));
	}

	@Override
	public int[] createPayload(Document doc, String term) {
		return positionSpec.getPosition((QLFDocument<?>) doc);
	}

	@Override
	public void append(BitIn postingSource, BitOut bos) throws IOException {
		int[] positionBits = positionSpec.getPositionBits();
		
		for (int i=0; i<positionBits.length; i++) {
			bos.writeBinary(positionBits[i], postingSource.readBinary(positionBits[i]));
		}
	}

	@Override
	public void writePayloadInMem(MemorySBOS docIds, int[] position) throws IOException {
		int[] positionBits = positionSpec.getPositionBits();
	
		for (int i=0; i<positionBits.length; i++) {
			docIds.writeBinary(positionBits[i], position[i]);
		}
	}

	@Override
	public int[] readPayload(BitIn bitFileReader) throws IOException {
		int[] positionBits = positionSpec.getPositionBits();
		int[] position = new int[positionBits.length];
				
		for (int i=0; i<positionBits.length; i++) { 
			position[i] = bitFileReader.readBinary(positionBits[i]);
		}
		
		return position;
	}

	@Override
	public int[] readPayload(DataInput in) throws IOException {
		int[] positionBits = positionSpec.getPositionBits();
		int[] position = new int[positionBits.length];
		
		for (int i=0; i<positionBits.length; i++) 
			position[i] = WritableUtils.readVInt(in);
		
		return position;
	}

	@Override
	public void writePayload(DataOutput out, int[] position) throws IOException {
		for (int p : position)
			WritableUtils.writeVInt(out, p);
	}

	@Override
	public int[] clone(int[] payload) {
		return payload.clone();
	}

	@Override
	public int[][] makePayloadArray(int size) {
		return new int[size][];
	}
	
	public PositionSpec getPositionSpec() {
		return positionSpec;
	}
}
