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
 * The Original Code is PositionInvertedIndex.java
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
package org.imageterrier.structures;

import gnu.trove.TIntIntHashMap;
import gnu.trove.TIntObjectHashMap;

import java.io.IOException;
import java.util.Arrays;

import org.imageterrier.locfile.PositionSpec;
import org.imageterrier.termpayload.PositionTermPayloadCoordinator;
import org.openimaj.feature.local.Location;
import org.terrier.compression.BitIn;
import org.terrier.structures.BitIndexPointer;
import org.terrier.structures.DocumentIndex;
import org.terrier.structures.Index;
import org.terrier.structures.IndexConfigurable;
import org.terrier.structures.TermPayloadInvertedIndex;
import org.terrier.structures.postings.IterablePosting;


/**
 * An ImageTerrier inverted index with payloads capable of storing
 * information on the position (i.e. spatial position, scale, orientation, etc.)
 * of each visual-term occurrence.
 * 
 * @author Jonathon Hare <jsh2@ecs.soton.ac.uk>
 */
public class PositionInvertedIndex extends TermPayloadInvertedIndex<int[]> implements IndexConfigurable {
	protected int DocumentBlockCountDelta = 1;
	
	/**
	 * Construct the inverted index.
	 * @param index the index to which this inverted index belongs
	 * @param structureName the name of the inverted index structure
	 * @param _doi the document index
	 * @param postingClass the class to use for iterating postings
	 * @throws IOException
	 */
	public PositionInvertedIndex(Index index, String structureName, DocumentIndex _doi, Class<? extends IterablePosting> postingClass) throws IOException
	{
		super(index, structureName, _doi, postingClass);
	}

	/**
	 * Get the {@link PositionSpec} used for this index.
	 * @return the PositionSpec.
	 */
	public PositionSpec getPositionSpec() {
		return ((PositionTermPayloadCoordinator)payloadConf).getPositionSpec();
	}
	
	/**
	 * Syntatic sugar for {@link #getPayloads(BitIndexPointer)}.
	 * @see #getPayloads(BitIndexPointer)
	 * @param pointer postings list pointer
	 * @return the position information for each posting in the list
	 */
	public TIntObjectHashMap<int[][]> getPositions(BitIndexPointer pointer) {
		return this.getPayloads(pointer);
	}
	
	/**
	 * Get a subset of the position information for a given postings list.
	 * The input is a map of the required documents to their index in the output array,
	 * and a list of required position indices (i.e. you can just get the x, and y parts
	 * and ignore everything else).
	 * The output is a three dimensional integer array of documents -> list of payload info
	 * where the payload info is itself an array of (encoded) ints.
	 * 
	 * @param pointer postings list pointer
	 * @param docposmap a mapping of document-id -> array index in the output array for all relevant documents.
	 * @param requestedindices the position indices required
	 * @return subset of the position information of the posting
	 */
	public int[][][] getPositions(BitIndexPointer pointer, TIntIntHashMap docposmap, int... requestedindices) {
		int [][][] docsMatches = new int [docposmap.size()][][];

		final long startOffset = pointer.getOffset();
		final byte startBitOffset = pointer.getOffsetBits();
		final int df = pointer.getNumberOfEntries();
		int [] positionBits = ((PositionTermPayloadCoordinator)payloadConf).getPositionSpec().getPositionBits();
		
		if (requestedindices == null || requestedindices.length==0) {
			requestedindices = new int[positionBits.length];
			for (int i=0; i<positionBits.length; i++) requestedindices[i] = i;
		} else {
			Arrays.sort(requestedindices);
		}
		
		try{
			final BitIn file = this.file[pointer.getFileNumber()].readReset(startOffset, startBitOffset);

			int docid = -1;
			for (int i = 0; i < df; i++) {
				docid = file.readGamma() + docid;	//docid
				int numOfTerms = file.readUnary(); 	//freq == numTerms

				int[][] positions = new int[numOfTerms][requestedindices.length];

				for (int k=0; k<numOfTerms; k++) {
					for (int o=0, ri=0; o<positionBits.length; o++)
					{
						if (ri < requestedindices.length && requestedindices[ri] == o) {
							positions[k][ri] = file.readBinary(positionBits[o]);
							ri++;
						} else {
							file.skipBits(positionBits[o]);
						}
					}
				}

				docsMatches[docposmap.get(docid)] = positions;
			}

			return docsMatches;
		} catch (IOException ioe) {
			logger.error("Problem reading block inverted index", ioe);
			return null;
		}
	}
	
	/**
	 * Get all the decoded location payloads from a given postings list.
	 * @see #getPayloads(BitIndexPointer)
	 * @param pointer postings list pointer
	 * @return the position information for each posting in the list
	 */
	public TIntObjectHashMap<Location[]> getPositionsDecoded(BitIndexPointer pointer) {
		final TIntObjectHashMap<Location[]> docsMatches = new TIntObjectHashMap<Location[]>();

		final long startOffset = pointer.getOffset();
		final byte startBitOffset = pointer.getOffsetBits();
		final int df = pointer.getNumberOfEntries();

		PositionSpec spec = ((PositionTermPayloadCoordinator)payloadConf).getPositionSpec();
		
		try {
			final BitIn file = this.file[pointer.getFileNumber()].readReset(startOffset, startBitOffset);

			int docid = -1;			
			for (int i = 0; i < df; i++) {
				docid = file.readGamma() + docid;	//docid
				int numOfTerms = file.readUnary(); 	//freq == numTerms

				Location[] payloads = new Location[numOfTerms];
				
				for (int k=0; k<numOfTerms; k++) {
					int [] payload = payloadConf.readPayload(file);
					
					payloads[k] = spec.decode(payload);
				}

				docsMatches.put(docid, payloads);
			}

			return docsMatches;
		} catch (IOException ioe) {
			logger.error("Problem reading block inverted index", ioe);
			return null;
		}
	}
}
