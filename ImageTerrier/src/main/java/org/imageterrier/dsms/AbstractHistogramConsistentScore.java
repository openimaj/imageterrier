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
 * The Original Code is AbstractHistogramConsistentScore.java
 *
 * The Original Code is Copyright (C) 2011 the University of Southampton
 * and the original contributors.
 * All Rights Reserved.
 *
 * Contributor(s):
 *   Sina Samangooei <ss@ecs.soton.ac.uk> (original contributor)
 *   Jonathon Hare <jsh2@ecs.soton.ac.uk> (original contributor)
 *   David Dupplaw <dpd@ecs.soton.ac.uk>
 */
package org.imageterrier.dsms;

import gnu.trove.TIntIntHashMap;

import org.apache.log4j.Logger;
import org.imageterrier.basictools.ApplicationSetupUtils;
import org.imageterrier.locfile.PositionSpec;
import org.imageterrier.locfile.QLFDocument;
import org.imageterrier.querying.parser.QLFDocumentQuery;
import org.imageterrier.structures.PositionInvertedIndex;
import org.terrier.matching.MatchingQueryTerms;
import org.terrier.matching.ResultSet;
import org.terrier.matching.dsms.DocumentScoreModifier;
import org.terrier.structures.BitIndexPointer;
import org.terrier.structures.Index;
import org.terrier.structures.Lexicon;
import org.terrier.structures.LexiconEntry;
import org.terrier.utility.ApplicationSetup;


public abstract class AbstractHistogramConsistentScore implements DocumentScoreModifier {
	protected static final Logger logger = Logger.getLogger(AbstractHistogramConsistentScore.class);
	
	public abstract int[] getPositionSpecIndices(PositionSpec spec);
	
	public abstract void incr(int[][] matchedTermPos, int[] queryTermIndices,float[] hist, double[] maxVals) ;
	
	public static final String CONSISTENT_HISTOGRAM_BINS = "consistent.histogram.bins";
	public static String PEAK_MODE = "consistent.peak.mode";	
	
	@Override
	public boolean modifyScores(Index index, MatchingQueryTerms queryTerms, ResultSet resultSet) {
		if (!(index.getInvertedIndex() instanceof PositionInvertedIndex)) {
			logger.warn("Inverted index is incompatible with DSM");
			return false;
		}

		PositionInvertedIndex invidx = (PositionInvertedIndex) index.getInvertedIndex();
		PositionSpec spec = invidx.getPositionSpec();
		int[] indices = this.getPositionSpecIndices(spec);
		
		if(indices == null || indices.length == 0){
			logger.warn("PositionSpec of the index is incompatible with this DSM");
			return false;
		}
		
		Lexicon<String> lexicon = index.getLexicon();
		QLFDocumentQuery<?> query = (QLFDocumentQuery<?>) queryTerms.getQuery();
		QLFDocument<?> queryDoc = query.getDocument();
		queryDoc.reset();
		
		int nbins = ApplicationSetupUtils.getProperty(CONSISTENT_HISTOGRAM_BINS, 10);
		PeakMode peakMode = PeakMode.valueOf(ApplicationSetup.getProperty(PEAK_MODE, PeakMode.MAX+""));
		
		double [] scores = resultSet.getScores();
		float [][] scaleHist = new float[scores.length][nbins];
		
		double[] maxVals = new double[indices.length];
		for(int i = 0; i < indices.length; i++){
			maxVals[i] = Math.pow(2, spec.getPositionBits()[indices[i]]) ;
		}
		
		int [] docids = resultSet.getDocids();
		
		TIntIntHashMap docposmap = new TIntIntHashMap();
		TIntIntHashMap posdocmap = new TIntIntHashMap();
		for (int i=0; i<docids.length; i++) {
			docposmap.put(docids[i], i); 
			posdocmap.put(i,docids[i]);
		}		
		
		//accumulate histograms
		String queryTerm;
		while (!queryDoc.endOfDocument()) {
			queryTerm = queryDoc.getNextTerm();
			LexiconEntry le = lexicon.getLexiconEntry(queryTerm);
			if (le != null) {
				int[] queryTermPos = new int[indices.length];
				for(int i = 0; i < indices.length; i++){
					queryTermPos[i] = spec.getPosition(queryDoc)[indices[i]];
				}
				
				
				int[][][] matchDocs = invidx.getPositions((BitIndexPointer) le, docposmap,indices);

				for (int i=0; i<scores.length; i++) {
					int [][] matchedTermPos = matchDocs[i];

					if (matchedTermPos != null)
						incr(matchedTermPos, queryTermPos, scaleHist[i], maxVals);
				}
			}
		}

		//modify scores
		for(int i=0; i<scores.length; i++) {
//			try {
//				System.out.println("DOCID: " + index.getMetaIndex().getItem("path", posdocmap.get(i)));
//			} catch (IOException e) {
//				System.out.println("DOCID: " + posdocmap.get(i));
//			}
			float[] hist = scaleHist[i];
//			System.out.println("...HIST: " + Arrays.toString(hist));
						
			// get the weighting
			float weight = peakMode.weighting(hist);
//			System.out.println("...WEIGHT: " + weight);
//			System.out.println("...SCORE FROM: " + scores[i] + " TO " + (scores[i] * weight));
			// then combine max and L1 into a single score
			scores[i] *= weight;
		}
		
		return true;
	}
	
	@Override
	public Object clone() {
		try {
			return super.clone();
		} catch (CloneNotSupportedException e) {
			throw new AssertionError(e);
		}
	}
}
