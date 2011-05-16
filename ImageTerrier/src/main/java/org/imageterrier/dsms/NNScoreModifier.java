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
 * The Original Code is NNScoreModifier.java
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
package org.imageterrier.dsms;

import gnu.trove.TIntObjectHashMap;

import java.util.Arrays;

import org.imageterrier.basictools.ApplicationSetupUtils;
import org.imageterrier.locfile.QLFDocument;
import org.imageterrier.querying.parser.QLFDocumentQuery;
import org.imageterrier.structures.NNInvertedIndex;
import org.imageterrier.termpayload.NNTermPayloadCoordinator;
import org.terrier.matching.MatchingQueryTerms;
import org.terrier.matching.ResultSet;
import org.terrier.matching.dsms.DocumentScoreModifier;
import org.terrier.structures.BitIndexPointer;
import org.terrier.structures.Index;
import org.terrier.structures.Lexicon;
import org.terrier.structures.LexiconEntry;


/**
 * An score modifier that can be used to re-weight retrieved documents
 * in the style of Sivic and Zisserman's "VideoGoogle" paper.
 * 
 * @author Jonathon Hare <jsh2@ecs.soton.ac.uk>
 */
public class NNScoreModifier implements DocumentScoreModifier {
	@Override
	public String getName() {
		return "NNScoreModifier";
	}

	@Override
	public boolean modifyScores(Index index, MatchingQueryTerms queryTerms, ResultSet resultSet) {
		Lexicon<String> lexicon = index.getLexicon();
		NNInvertedIndex invertedIndex = (NNInvertedIndex) index.getInvertedIndex();
		double [] scores = resultSet.getScores();
		QLFDocumentQuery<?> query = (QLFDocumentQuery<?>) queryTerms.getQuery();
		QLFDocument<?> queryDoc = query.getDocument();

		Arrays.fill(scores, 0);

		queryDoc.reset();

		String queryTerm;
		while (!queryDoc.endOfDocument()) {
			queryTerm = queryDoc.getNextTerm();
			LexiconEntry le = lexicon.getLexiconEntry(queryTerm);
			if (le != null) {
				int[] queryNeighbours = queryDoc.getCurrentNearestNeighbourTIds(ApplicationSetupUtils.getProperty("nearest.neighbours", index.getIntIndexProperty("nearest.neighbours", NNTermPayloadCoordinator.DEFAULT_NEAREST_NEIGHBOURS)));

				//TIntObjectHashMap<int[][]> matchDocs = invertedIndex.getMatches((BitIndexPointer) le);
				TIntObjectHashMap<int[][]> matchDocs = invertedIndex.getPayloads((BitIndexPointer) le);

				for (int i=0; i<scores.length; i++) {
					int[][] matchedNeighbours = matchDocs.get(resultSet.getDocids()[i]);

					if (matchedNeighbours != null)
						scores[i] += score(matchedNeighbours, queryNeighbours);
				}
			}
		}

		return true;
	}

	protected int score(int[][] matchedNeighbours, int [] queryNeighbours) {
		int score = 0;

		for (int[] mn : matchedNeighbours) {
			score += vote(mn, queryNeighbours);
//			int v = vote(mn, queryNeighbours);
//			if (v>score) score = v;
		}

		return score;
	}

	protected int vote(int [] mn, int [] qn) {
		int vote = 0;

		for (int n : qn) {
			int idx = Arrays.binarySearch(mn, n);
			//if (idx >= 0 && idx < mn.length && mn[idx] == n) vote++;
			if (idx >= 0) 
				vote++;
		}

		return vote;
	}

	@Override
	public NNScoreModifier clone() {
		//new one, as we don't have state
		return new NNScoreModifier();
	}
}
