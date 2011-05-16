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
 * The Original Code is TermPayloadInvertedIndex.java
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
package org.terrier.structures;

import gnu.trove.TIntObjectHashMap;

import java.io.IOException;
import java.util.Iterator;
import java.util.Map.Entry;

import org.terrier.compression.BitIn;
import org.terrier.structures.postings.IterablePosting;
import org.terrier.structures.postings.TermPayloadIterablePosting;


/**
 * This class implements the inverted index for performing retrieval. 
 * The inverted index contains payload data for each term occurrence.
 * 
 * @author Jonathon Hare <jsh2@ecs.soton.ac.uk>
 *
 * @param <PAYLOAD> the payload type
 */
public class TermPayloadInvertedIndex<PAYLOAD> extends InvertedIndex implements IndexConfigurable {
	protected int DocumentBlockCountDelta = 1;
	protected TermPayloadCoordinator<PAYLOAD> payloadConf;

	/**
	 * Construct the inverted index.
	 * @param index the index to which this inverted index belongs
	 * @param structureName the name of the inverted index structure
	 * @param _doi the document index
	 * @param postingClass the class to use for iterating postings
	 * @throws IOException
	 */
	public TermPayloadInvertedIndex(Index index, String structureName, DocumentIndex _doi, Class<? extends IterablePosting> postingClass) throws IOException {
		super(index, structureName, _doi, postingClass);
	}
	
	/** let it know which index to use */
	@Override
	public void setIndex(Index i) {
		DocumentBlockCountDelta = i.getIntIndexProperty("blocks.invertedindex.countdelta", 1);

		//read in the index properties for term positions
		payloadConf = TermPayloadCoordinator.create(i);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public int[][] getDocuments(BitIndexPointer pointer) {
		final long startOffset = pointer.getOffset();
		final byte startBitOffset = pointer.getOffsetBits();
		final int df = pointer.getNumberOfEntries();

		final int[][] documentTerms = new int[4][];
		documentTerms[0] = new int[df];
		documentTerms[1] = new int[df];
		documentTerms[2] = new int[df];
		documentTerms[3] = new int[df];

		try{
			final BitIn file = this.file[pointer.getFileNumber()].readReset(startOffset, startBitOffset);

			documentTerms[0][0] = file.readGamma() - 1; //docid
			documentTerms[1][0] = file.readUnary();		//freq

			int numOfTerms = documentTerms[3][0] = documentTerms[1][0];
			for (int i=0; i<numOfTerms; i++) {
				payloadConf.readPayload(file);
			}

			for (int i = 1; i < df; i++) {
				documentTerms[0][i]  = file.readGamma() + documentTerms[0][i - 1];
				documentTerms[1][i]  = file.readUnary();

				numOfTerms = documentTerms[3][i] = documentTerms[1][i];
				for (int k=0; k<numOfTerms; k++) {
					payloadConf.readPayload(file);
				}
			}

			return documentTerms;
		} catch (IOException ioe) {
			logger.error("Problem reading block inverted index", ioe);
			return null;
		}
	}

	/**
	 * For a given postings list return a map of docid -> payloads_of_each_term_instance
	 * @param pointer postings list
	 * @return document->payloads mapping
	 */
	public TIntObjectHashMap<PAYLOAD[]> getPayloads(BitIndexPointer pointer) {
		final TIntObjectHashMap<PAYLOAD[]> docsMatches = new TIntObjectHashMap<PAYLOAD[]>();

		final long startOffset = pointer.getOffset();
		final byte startBitOffset = pointer.getOffsetBits();
		final int df = pointer.getNumberOfEntries();

		try {
			final BitIn file = this.file[pointer.getFileNumber()].readReset(startOffset, startBitOffset);

			int docid = -1;			
			for (int i = 0; i < df; i++) {
				docid = file.readGamma() + docid;	//docid
				int numOfTerms = file.readUnary(); 	//freq == numTerms

				PAYLOAD[] payloads = payloadConf.makePayloadArray(numOfTerms);

				for (int k=0; k<numOfTerms; k++) {
					payloads[k] = payloadConf.readPayload(file);
				}

				docsMatches.put(docid, payloads);
			}

			return docsMatches;
		} catch (IOException ioe) {
			logger.error("Problem reading block inverted index", ioe);
			return null;
		}
	}

	/**
	 * Get the payload coordinator
	 * @return the payload coordinator
	 */
	public TermPayloadCoordinator<PAYLOAD> getPayloadConfig() {
		return payloadConf;
	}

	@SuppressWarnings("unchecked")
	@Override
	public TermPayloadIterablePosting<PAYLOAD> getPostings(BitIndexPointer pointer) throws IOException {
		TermPayloadIterablePosting<PAYLOAD> posting = (TermPayloadIterablePosting<PAYLOAD>) super.getPostings(pointer);
		posting.setPayloadConfig(payloadConf);
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

				TermPayloadIterablePosting<PAYLOAD> ip;
				ip = (TermPayloadIterablePosting<PAYLOAD>) getPostings((BitIndexPointer) le.getValue());

				while (ip.next() != IterablePosting.EOL) {
					System.out.format("Doc: %10s\t", index.getMetaIndex().getItem("docno", ip.getId()));
					System.out.format("Freq: %2d\t", ip.getFrequency());

					PAYLOAD[] pos = ip.getPayloads();
					for (int i=0; i<ip.getFrequency(); i++) {
						System.out.format("(%s) ", pos[i].toString());
					}

					System.out.println();				
				}
			}		
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
