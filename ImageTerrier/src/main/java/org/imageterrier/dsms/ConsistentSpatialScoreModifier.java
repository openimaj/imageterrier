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
 * The Original Code is ConsistentSpatialScoreModifier.java
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

import org.imageterrier.locfile.PositionSpec;


public class ConsistentSpatialScoreModifier extends AbstractHistogramConsistentScore {

	@Override
	public String getName() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public int[] getPositionSpecIndices(PositionSpec spec) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void incr(int[][] matchedTermPos, int[] queryTermIndecies,
			float[] hist, double[] maxVals) {
		// TODO Auto-generated method stub
		
	}
//
//	@Override
//	public String getName() {
//		return "ConsistentSpatialScoreModifier";
//	}
//
//	@Override
//	public int[] getPositionSpecIndecies(PositionSpec spec) {
//		int[] spatialIndecies = new int[2];
//		if (spec.getMode() == PositionSpecMode.SPATIAL || spec.getMode() == PositionSpecMode.SPATIAL_AFFINE || spec.getMode() == PositionSpecMode.SPATIAL_SCALE || spec.getMode() == PositionSpecMode.SPATIAL_SCALE_ORI) {
//			spatialIndecies[0] = 0;
//			spatialIndecies[1] = 1;
//		} else {
//			logger.warn("PositionSpec of the index is incompatible with this DSM");
//			return null;
//		}
//		return spatialIndecies;
//	}
//	@Override
//	public boolean modifyScores(Index index, MatchingQueryTerms queryTerms, ResultSet resultSet) {
//		if (!(index.getInvertedIndex() instanceof PositionInvertedIndex)) {
//			logger.warn("Inverted index is incompatible with DSM");
//			return false;
//		}
//
//		PositionInvertedIndex invidx = (PositionInvertedIndex) index.getInvertedIndex();
//		PositionSpec spec = invidx.getPositionSpec();
//		int[] indecies = this.getPositionSpecIndecies(spec);
//		if(indecies == null || indecies.length == 0){
//			logger.warn("PositionSpec of the index is incompatible with this DSM");
//			return false;
//		}
//		
//		Lexicon<String> lexicon = index.getLexicon();
//		QLFDocumentQuery<?> query = (QLFDocumentQuery<?>) queryTerms.getQuery();
//		QLFDocument<?> queryDoc = query.getDocument();
//		queryDoc.reset();
//		
//		double [] scores = resultSet.getScores();
//		
//		double[] maxVals = new double[indecies.length];
//		for(int i = 0; i < indecies.length; i++){
//			maxVals[i] = Math.pow(2, spec.getPositionBits()[i]) ;
//		}
//		int [] docids = resultSet.getDocids();
//		
//		TIntIntHashMap docposmap = new TIntIntHashMap();
//		for (int i=0; i<docids.length; i++) {
//			docposmap.put(docids[i], i); 
//		}
//		int nbins = Integer.parseInt(ApplicationSetup.getProperty(CONSISTENT_HISTOGRAM_BINS, "10"));
//		float [][] scaleHist = new float[scores.length][nbins];
//		
//		// Locate the center of masses for each query/document pair (a single point)
//		String queryTerm;
//		while (!queryDoc.endOfDocument()) {
//			queryTerm = queryDoc.getNextTerm();
//			LexiconEntry le = lexicon.getLexiconEntry(queryTerm);
//			if (le != null) {
//				int[] queryTermPos = new int[indecies.length];
//				for(int i = 0; i < indecies.length; i++){
//					queryTermPos[i] = spec.getPosition(queryDoc)[indecies[i]];
//				}
//				
//				
//				int[][][] matchDocs = invidx.getPositions((BitIndexPointer) le, docposmap,indecies);
//
//				for (int i=0; i<scores.length; i++) {
//					int [][] matchedTermPos = matchDocs[i];
//
//					if (matchedTermPos != null)
//						incr(matchedTermPos, queryTermPos, scaleHist[i], maxVals);
//				}
//			}
//		}
//		PeakMode peakMode = PeakMode.valueOf(ApplicationSetup.getProperty(PEAK_MODE, PeakMode.MAX+""));
//		//modify scores
//		for(int i=0; i<scores.length; i++) {
//			float[] hist = scaleHist[i];
//			
//			// get the weighting
//			float weight = peakMode.weighting(hist);
//			
//			// then combine max and L1 into a single score
//			scores[i] *= weight;
//		}
//		
//		return true;
//	}
//
//	@Override
//	public void incr(int[][] matchedTermPos, int[] queryTermIndecies,float[] hist, double[] maxVals) {
//	}

}
