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
 * The Original Code is NNTermPayloadCoordinator.java
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

import gnu.trove.TIntHashSet;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.Arrays;

import org.apache.hadoop.io.WritableUtils;
import org.imageterrier.locfile.QLFDocument;
import org.terrier.compression.BitIn;
import org.terrier.compression.BitOut;
import org.terrier.compression.MemorySBOS;
import org.terrier.indexing.Document;
import org.terrier.structures.Index;
import org.terrier.structures.TermPayloadCoordinator;

/**
 * Coordinator for storing the term identifiers for the N nearest spatial 
 * neighbouring terms of each term occurrence in the index.
 * 
 * The neighbour payload is encoded by ordering the list of neighbours and
 * storing the termid deltas using gamma coding.
 * 
 * @author Jonathon Hare <jsh2@ecs.soton.ac.uk>
 *
 */
public class NNTermPayloadCoordinator extends TermPayloadCoordinator<int[]> {
	/** The default number of NN terms */
	public static final int DEFAULT_NEAREST_NEIGHBOURS = 15;
	
	protected int numNearestNeighbours;
	
	/**
	 * Default constructor; uses 15 neighbouring terms.
	 */
	public NNTermPayloadCoordinator() {
		this(DEFAULT_NEAREST_NEIGHBOURS);
	}
	
	/**
	 * Construct the NNTermPayloadCoordinator to use the given number
	 * of neighbouring terms.
	 * @param numNearestNeighbours number of neighbouring terms.
	 */
	public NNTermPayloadCoordinator(int numNearestNeighbours) {
		this.numNearestNeighbours = numNearestNeighbours;
	}
	
	@Override
	public void saveConfiguration(Index currentIndex) {
		super.saveConfiguration(currentIndex);
		
		//now update the index with the extra info
		currentIndex.setIndexProperty("nearest.neighbours", numNearestNeighbours+"");
	}

	@Override
	public int[] createPayload(Document doc, String term) {
		//We're only going to write nearest neighbour terms once (dups removed). 
		//We're also going to order the terms and save their deltas (see writePayloadInMem)
		//rather than the actual nos.
		int [] payload = ((QLFDocument<?>)doc).getCurrentNearestNeighbourTIdsKD(numNearestNeighbours); 
		payload = new TIntHashSet(payload).toArray();
		Arrays.sort(payload);
		
		return payload;
	}

	@Override
	public void append(BitIn postingSource, BitOut bos) throws IOException {
		final int numOfNeighbours = postingSource.readUnary() - 1;
		bos.writeUnary(numOfNeighbours+1);
		
		for (int k=0; k<numOfNeighbours; k++) {
			bos.writeGamma(postingSource.readGamma());
		}
	}

	@Override
	public void writePayloadInMem(MemorySBOS docIds, int[] payload) throws IOException {
		docIds.writeUnary(payload.length+1);		
		
		if (payload.length > 0) {
			docIds.writeGamma(payload[0] + 1);
			for (int i=1; i<payload.length; i++) {
				docIds.writeGamma(payload[i] - payload[i-1]);
			}
		}
	}

	@Override
	public int[] readPayload(BitIn bitFileReader) throws IOException {
		int numOfNeighbours = bitFileReader.readUnary() - 1;
		int previousTermId = -1;
		
		int [] payload = new int[numOfNeighbours];
		for(int j=0;j<numOfNeighbours;j++) {
			payload[j] = previousTermId = bitFileReader.readGamma() + previousTermId;
		}
		
		return payload;
	}

	@Override
	public int[] readPayload(DataInput in) throws IOException {
		int numNeighbours = WritableUtils.readVInt(in);
		int [] neighbours = new int[numNeighbours];
		
		for (int j=0; j<numNeighbours; j++)
			neighbours[j] = WritableUtils.readVInt(in);
		
		return neighbours;
	}

	@Override
	public void writePayload(DataOutput out, int[] payload) throws IOException {
		WritableUtils.writeVInt(out, payload.length);
		for (int p : payload)
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
}
