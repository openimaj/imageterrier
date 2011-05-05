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
 * The Original Code is ASInvertedIndex.java
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

import gnu.trove.TIntObjectHashMap;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.Map.Entry;

import org.imageterrier.indexing.ASSinglePassIndexer;
import org.imageterrier.locfile.QLFFilesCollection;
import org.imageterrier.structures.postings.ASIterablePosting;
import org.openimaj.image.feature.local.keypoints.quantised.QuantisedAffineSimulationKeypoint;
import org.terrier.compression.BitIn;
import org.terrier.indexing.Collection;
import org.terrier.structures.BitIndexPointer;
import org.terrier.structures.DocumentIndex;
import org.terrier.structures.Index;
import org.terrier.structures.IndexConfigurable;
import org.terrier.structures.InvertedIndex;
import org.terrier.structures.Lexicon;
import org.terrier.structures.LexiconEntry;
import org.terrier.structures.postings.BlockIterablePosting;
import org.terrier.structures.postings.IterablePosting;


public class ASInvertedIndex extends InvertedIndex implements IndexConfigurable {
	protected int DocumentBlockCountDelta = 1;

	public ASInvertedIndex(Index index, String structureName, DocumentIndex _doi, Class<? extends IterablePosting> postingClass) throws IOException
	{
		super(index, structureName, _doi, postingClass);
	}
	
	public ASInvertedIndex(Index index, String structureName, DocumentIndex doi) throws IOException {
		super(index, structureName, doi, BlockIterablePosting.class);
	}
	

	public ASInvertedIndex(Index index, String structureName) throws IOException {
		this(index, structureName, index.getDocumentIndex());
	}

	/** let it know which index to use */
	@Override
	public void setIndex(Index i)
	{
		DocumentBlockCountDelta = i.getIntIndexProperty("blocks.invertedindex.countdelta", 1);
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
			int tmpBlocks[] = new int[numOfTerms];
			
			for (int i=0; i<numOfTerms; i++) {
				tmpBlocks[i] = file.readBinary(32);
			}

			for (int i = 1; i < df; i++) {
				documentTerms[0][i]  = file.readGamma() + documentTerms[0][i - 1];
				documentTerms[1][i]  = file.readUnary();

				numOfTerms = documentTerms[3][i] = documentTerms[1][i];
				tmpBlocks = new int[numOfTerms];
				
				for (int k=0; k<numOfTerms; k++) {
					tmpBlocks[k] = file.readBinary(32);
				}
			}

			return documentTerms;

		} catch (IOException ioe) {
			logger.error("Problem reading block inverted index", ioe);
			return null;
		}
	}

	public TIntObjectHashMap<int []> getMatches(BitIndexPointer pointer) {
		final TIntObjectHashMap<int []> docsMatches = new TIntObjectHashMap<int []>();
		
		final long startOffset = pointer.getOffset();
		final byte startBitOffset = pointer.getOffsetBits();
		final int df = pointer.getNumberOfEntries();
		
		try{
			final BitIn file = this.file[pointer.getFileNumber()].readReset(startOffset, startBitOffset);
			
//			int previousPos = -1;
			int docid = -1;
			
			for (int i = 0; i < df; i++) {
				docid = file.readGamma() + docid;	//docid
				int numOfTerms = file.readUnary(); 	//freq == numTerms
				
				int[] simulations = new int[numOfTerms];
				
				for (int k=0; k<numOfTerms; k++) {
					//previousPos = file.readGamma() + previousPos - 1;
					//positions[k] = Utils.fromIndex(previousPos);
					simulations[k] = file.readBinary(32);
				}
				
				docsMatches.put(docid, simulations);
			}

			return docsMatches;
		} catch (IOException ioe) {
			logger.error("Problem reading block inverted index", ioe);
			return null;
		}
	}
	
	public static void main(String [] args) throws IOException {
		File temp = File.createTempFile("test", "idx");
		File kFile = new File(ASSinglePassIndexer.class.getResource("/uk/ac/soton/ecs/jsh2/imageterrier/loc/testAsiftEnriched.bkey.loc").getFile());
		temp.delete();
		temp.mkdirs();
		ASSinglePassIndexer indexer = new ASSinglePassIndexer(temp.getAbsolutePath(), "test");
		Collection[] collections = new Collection[]{
			new QLFFilesCollection<QuantisedAffineSimulationKeypoint>(kFile.getParentFile(), ".bkey.loc", QuantisedAffineSimulationKeypoint.class)	
		};
		indexer.createInvertedIndex(collections);
		
		Index index = Index.createIndex(temp.getAbsolutePath(), "test");
		
		Lexicon<String> l = index.getLexicon();
		
		Iterator<Entry<String, LexiconEntry>> iter = l.iterator();
			
		while (iter.hasNext()) {
			Entry<String, LexiconEntry> le = iter.next();
			
			System.out.println();
			System.out.println("Term: " + le.getKey());
			
			ASIterablePosting ip = (ASIterablePosting) index.getInvertedIndex().getPostings((BitIndexPointer) le.getValue());
			
			while (ip.next() != IterablePosting.EOL) {
				System.out.format("Doc: %10s\t", index.getMetaIndex().getItem("docno", ip.getId()));
				System.out.format("Freq: %2d\t", ip.getFrequency());
				
				int [] pos = ip.getSimulations();
				for (int i=0; i<ip.getFrequency(); i++) {
					System.out.format("(%d) ", pos[i]);
				}
				
				System.out.println();				
			}
		}
	}
}
