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
 * The Original Code is FastAccumulatorResultSet.java
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
package org.terrier.matching;

import java.util.concurrent.locks.ReentrantLock;

/***
 * A modification of AccumulatorResultSet which is backed by large memory arrays.
 * The arrays can hold as many documents as exist in the corpus from init.
 * 
 * @author Sina Samangooei <ss@ecs.soton.ac.uk>
 */
public class FastAccumulatorResultSet extends AccumulatorResultSet{

	private static final long serialVersionUID = 1920903100731031864L;
	
	public int totalDocumentsWithAScore = 0;

	public FastAccumulatorResultSet(int numberOfDocuments) {
		super(numberOfDocuments);
		lock = new ReentrantLock();
		
		initHash(numberOfDocuments);

		resultSize = numberOfDocuments;
		exactResultSize = numberOfDocuments;
	}

	private void initHash(int numberOfDocuments) {
		this.scores = new double[numberOfDocuments];
		this.occurrences = new short[numberOfDocuments];
		
	}
	
	/** This method initialises the arrays to be sorted, after the matching phase has been completed */
	@Override
	public void initialise() 
	{
		this.docids = new int[this.scores.length];
		for(int i = 0; i < docids.length; i++){
			docids[i] = i;
		}
		resultSize = this.docids.length;
		exactResultSize = this.docids.length;

		scoresMap.clear();
		occurrencesMap.clear();
		this.arraysInitialised = true;
	}

}
