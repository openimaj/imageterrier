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
 * The Original Code is FastFull.java
 *
 * The Original Code is Copyright (C) 2011 the University of Southampton
 * and the original contributors.
 * All Rights Reserved.
 *
 * Contributor(s):
 *   Sina Samangooei <ss@ecs.soton.ac.uk> (original contributor)
 *   Jonathon Hare <jsh2@ecs.soton.ac.uk>
 *   David Dupplaw <dpd@ecs.soton.ac.uk>
 */
package org.terrier.matching.taat;

import java.io.IOException;

import org.terrier.matching.BaseMatching;
import org.terrier.matching.CollectionResultSet;
import org.terrier.matching.FastAccumulatorResultSet;
import org.terrier.matching.MatchingQueryTerms;
import org.terrier.matching.ResultSet;
import org.terrier.matching.models.WeightingModel;
import org.terrier.structures.BitIndexPointer;
import org.terrier.structures.Index;
import org.terrier.structures.LexiconEntry;
import org.terrier.structures.postings.IterablePosting;

/**
 * 
 * A Full Term at a Time matcher which uses the FastAccumulatorResultSet. Document scores are held
 * in memory in an array large enough such that every document could be given a score. This was used to avoid
 * the expensive hashing functions per document but didn't work very well.
 * @author Sina Samangooei <ss@ecs.soton.ac.uk>
 *
 */
public class FastFull extends BaseMatching {

	public FastFull(Index index) {
		super(index);
		resultSet = new FastAccumulatorResultSet(collectionStatistics.getNumberOfDocuments());
	}
	
	@Override
	public String getInfo() {
		return "Full term-at-a-time (TAAT) algorithm";
	}

	@Override
	public ResultSet match(String queryNumber, MatchingQueryTerms queryTerms) throws IOException 
	{
		initialise(queryTerms);
		// Check whether we need to match an empty query. If so, then return the existing result set.
		// String[] queryTermStrings = queryTerms.getTerms();
		if (MATCH_EMPTY_QUERY && queryTermsToMatchList.size() == 0) {
			resultSet = new CollectionResultSet(collectionStatistics.getNumberOfDocuments());
			resultSet.setExactResultSize(collectionStatistics.getNumberOfDocuments());
			resultSet.setResultSize(collectionStatistics.getNumberOfDocuments());
			return resultSet;
		}
						
		int queryLength = queryTermsToMatchList.size();
		// The posting list iterator from the inverted file
		IterablePosting postings;		
		for (int i = 0; i < queryLength; i++) 
		{
			LexiconEntry lexiconEntry = queryTermsToMatchList.get(i).getValue();
			postings = invertedIndex.getPostings((BitIndexPointer)lexiconEntry);
			assignScores(i, wm[i], (FastAccumulatorResultSet) resultSet, postings);
		}

		resultSet.initialise();
		this.numberOfRetrievedDocuments = resultSet.getExactResultSize();
		finalise(queryTerms);
		return resultSet;
	}
	
	/**
	 * 
	 * Altered to use FastAccumulatorResultSet. Most expensive calls become: postings.next() and wmodel.score().
	 * 
	 * @param i
	 * @param wModels
	 * @param rs
	 * @param postings
	 * @throws IOException
	 */
	protected void assignScores(int i, final WeightingModel[] wModels, FastAccumulatorResultSet rs, final IterablePosting postings) throws IOException
	{
		int docid;
		double score;
		
		short mask = 0;
		if (i < 16)
			mask = (short)(1 << i);
		
		while (postings.next() != IterablePosting.EOL)
		{
			score = 0.0; docid = postings.getId();

			for (WeightingModel wmodel: wModels)
				score += wmodel.score(postings);
			
			boolean documentDoesNotExist = rs.scores[docid] == 0;
			rs.scores[docid] += score;
			
			if (documentDoesNotExist && (score > 0.0d))
				numberOfRetrievedDocuments++;
			else if (!documentDoesNotExist && (score < 0.0d))
				numberOfRetrievedDocuments--;

			rs.occurrences[docid] = (short) (rs.occurrences[docid] | mask);
		}
	}

}
