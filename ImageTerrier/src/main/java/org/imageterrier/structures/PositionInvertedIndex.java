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
import java.util.Iterator;
import java.util.Map.Entry;

import org.imageterrier.locfile.PositionSpec;
import org.imageterrier.locfile.PositionSpec.PositionSpecMode;
import org.imageterrier.structures.postings.PositionIterablePosting;
import org.terrier.compression.BitIn;
import org.terrier.structures.BitIndexPointer;
import org.terrier.structures.DocumentIndex;
import org.terrier.structures.Index;
import org.terrier.structures.IndexConfigurable;
import org.terrier.structures.InvertedIndex;
import org.terrier.structures.Lexicon;
import org.terrier.structures.LexiconEntry;
import org.terrier.structures.postings.BlockIterablePosting;
import org.terrier.structures.postings.IterablePosting;
import org.terrier.utility.ArrayUtils;


public class PositionInvertedIndex extends InvertedIndex implements IndexConfigurable {
	protected int DocumentBlockCountDelta = 1;
	protected int [] positionBits = {8,8};

	public PositionInvertedIndex(Index index, String structureName, DocumentIndex _doi, Class<? extends IterablePosting> postingClass) throws IOException
	{
		super(index, structureName, _doi, postingClass);
	}

	public PositionInvertedIndex(Index index, String structureName, DocumentIndex doi) throws IOException {
		super(index, structureName, doi, BlockIterablePosting.class);
	}


	public PositionInvertedIndex(Index index, String structureName) throws IOException {
		this(index, structureName, index.getDocumentIndex());
	}

	/** let it know which index to use */
	@Override
	public void setIndex(Index i)
	{
		DocumentBlockCountDelta = i.getIntIndexProperty("blocks.invertedindex.countdelta", 1);

		//read in the index properties for term positions
		int nPositionOrdinates = i.getIntIndexProperty("positions.ordinates", 0);
		positionBits = new int[nPositionOrdinates];

		positionBits = ArrayUtils.parseCommaDelimitedInts(i.getIndexProperty("positions.nbits", ""));
		if (positionBits.length != nPositionOrdinates)
			throw new RuntimeException("Position bits length is not equal to the number of ordinates");
	}

	/**
	 * Returns a 2D array containing the document ids, 
	 * the term frequencies, the field scores the block frequencies and 
	 * the block ids for the given documents. 
	 * @return int[][] the six dimensional [6][] array containing 
	 *				 the document ids, frequencies, field scores and block 
	 *				 frequencies, matchFreqs and the last vector contains the 
	 *				 term identifiers and it has a different length from 
	 *				 the document identifiers.
	 * @param startOffset start byte of the postings in the inverted file
	 * @param startBitOffset start bit of the postings in the inverted file
	 * @param endOffset end byte of the postings in the inverted file
	 * @param endBitOffset end bit of the postings in the inverted file
	 * @param df the number of postings to expect 
	 */
	@Override
	public int[][] getDocuments(BitIndexPointer pointer) {
		final long startOffset = pointer.getOffset();
		final byte startBitOffset = pointer.getOffsetBits();
		final int df = pointer.getNumberOfEntries();

		final int[][] documentTerms = new int[6][];
		documentTerms[0] = new int[df];
		documentTerms[1] = new int[df];
		documentTerms[2] = new int[df];
		documentTerms[3] = new int[df];

		try{
			final BitIn file = this.file[pointer.getFileNumber()].readReset(startOffset, startBitOffset);

			documentTerms[0][0] = file.readGamma() - 1; //docid
			documentTerms[1][0] = file.readUnary();		//freq

			int numOfTerms = documentTerms[3][0] = documentTerms[1][0];
			int tmpBlocks[][] = new int[numOfTerms][positionBits.length];

			for (int i=0; i<numOfTerms; i++) {
				for (int o=0; o<positionBits.length; o++)
					tmpBlocks[i][o] = file.readBinary(positionBits[o]);
			}

			for (int i = 1; i < df; i++) {
				documentTerms[0][i]  = file.readGamma() + documentTerms[0][i - 1];
				documentTerms[1][i]  = file.readUnary();

				numOfTerms = documentTerms[3][i] = documentTerms[1][i];
				tmpBlocks = new int[numOfTerms][positionBits.length];

				for (int k=0; k<numOfTerms; k++) {
					for (int o=0; o<positionBits.length; o++)
						tmpBlocks[i][o] = file.readBinary(positionBits[o]);
				}
			}

			return documentTerms;
		} catch (IOException ioe) {
			logger.error("Problem reading block inverted index", ioe);
			return null;
		}
	}

	/**
	 * For a given postings list return a map of
	 * docid -> positions_of_each_term_instance
	 * @param pointer postings list
	 * @return
	 */
	public TIntObjectHashMap<int [][]> getPositions(BitIndexPointer pointer) {
		final TIntObjectHashMap<int [][]> docsMatches = new TIntObjectHashMap<int [][]>();

		final long startOffset = pointer.getOffset();
		final byte startBitOffset = pointer.getOffsetBits();
		final int df = pointer.getNumberOfEntries();

		try {
			final BitIn file = this.file[pointer.getFileNumber()].readReset(startOffset, startBitOffset);

			int docid = -1;			
			for (int i = 0; i < df; i++) {
				docid = file.readGamma() + docid;	//docid
				int numOfTerms = file.readUnary(); 	//freq == numTerms

				int[][] positions = new int[numOfTerms][positionBits.length];

				for (int k=0; k<numOfTerms; k++) {
					for (int o=0; o<positionBits.length; o++)
						positions[k][o] = file.readBinary(positionBits[o]);
				}

				docsMatches.put(docid, positions);
			}

			return docsMatches;
		} catch (IOException ioe) {
			logger.error("Problem reading block inverted index", ioe);
			return null;
		}
	}

	/**
	 * For a given postings list return a map of
	 * docid -> positions_of_each_term_instance
	 * @param pointer postings list
	 * @return
	 */
	public int[][][] getPositions(BitIndexPointer pointer, TIntIntHashMap docposmap, int... requestedindices) {
		int [][][] docsMatches = new int [docposmap.size()][][];

		final long startOffset = pointer.getOffset();
		final byte startBitOffset = pointer.getOffsetBits();
		final int df = pointer.getNumberOfEntries();
		
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
	
	public int getNumOrdinates() {
		return this.index.getIntIndexProperty("positions.ordinates", 0);
	}

	public int [] getBitsPerOrdinate() {
		return ArrayUtils.parseCommaDelimitedInts(index.getIndexProperty("positions.nbits", ""));
	}

	public PositionSpecMode getPositionSpecMode() {
		return PositionSpecMode.valueOf(index.getIndexProperty("positions.mode", "NONE"));
	}

	public double [] getPositionLowerBounds() {
		return ArrayUtils.parseCommaDelimitedDoubles(index.getIndexProperty("positions.lowerBounds", ""));
	}

	public double [] getPositionUpperBounds() {
		return ArrayUtils.parseCommaDelimitedDoubles(index.getIndexProperty("positions.upperBounds", ""));
	}

	public PositionSpec getPositionSpec() {
		return new PositionSpec(getPositionSpecMode(), positionBits, getPositionLowerBounds(), getPositionUpperBounds());
	}

	@Override
	public PositionIterablePosting getPostings(BitIndexPointer pointer) throws IOException {
		PositionIterablePosting posting = (PositionIterablePosting) super.getPostings(pointer);
		posting.setPositionBits(positionBits);
		return posting;
	}
	
	@Override
	public void print() {
		try {
			Lexicon<String> l = index.getLexicon();

			Iterator<Entry<String, LexiconEntry>> iter = l.iterator();

			while (iter.hasNext()) {
				Entry<String, LexiconEntry> le = iter.next();

				System.out.println();
				System.out.println("Term: " + le.getKey());

				PositionIterablePosting ip;
				ip = (PositionIterablePosting) getPostings((BitIndexPointer) le.getValue());

				while (ip.next() != IterablePosting.EOL) {
					System.out.format("Doc: %10s\t", index.getMetaIndex().getItem("docno", ip.getId()));
					System.out.format("Freq: %2d\t", ip.getFrequency());

					int [][] pos = ip.getPositions();
					for (int i=0; i<ip.getFrequency(); i++) {
						System.out.format("(%d,%d) ", pos[i][0], pos[i][1]);
					}

					System.out.println();				
				}
			}		
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
