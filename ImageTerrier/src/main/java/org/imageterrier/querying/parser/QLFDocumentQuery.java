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
 * The Original Code is QLFDocumentQuery.java
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
package org.imageterrier.querying.parser;

import gnu.trove.TIntObjectProcedure;
import gnu.trove.TObjectIntHashMap;
import gnu.trove.TObjectIntProcedure;

import org.imageterrier.locfile.QLFDocument;
import org.openimaj.feature.local.quantised.QuantisedLocalFeature;
import org.terrier.querying.parser.MultiTermQuery;
import org.terrier.querying.parser.SingleTermQuery;

/**
 * A {@link MultiTermQuery} constructed from an instance of
 * a {@link QLFDocument}. This is used to use a whole document
 * as a query (reasonable in ImageTerrier as whole images are
 * usually the query). 
 * 
 * @author Jonathon Hare <jsh2@ecs.soton.ac.uk>
 *
 * @param <T> the type of {@link QuantisedLocalFeature}.
 */
public class QLFDocumentQuery<T extends QuantisedLocalFeature<?>> extends MultiTermQuery {
	private static final long serialVersionUID = 1L;

	protected QLFDocument<T> document;
	
	/**
	 * Construct the query from the given document
	 * @param document the document 
	 */
	public QLFDocumentQuery(QLFDocument<T> document) {
		super();
		
		this.document = document;
		
		TObjectIntHashMap<String> counts = new TObjectIntHashMap<String>();		
		while (!document.endOfDocument()){
			counts.adjustOrPutValue(document.getNextTerm(), 1, 1);
		}
		document.reset();
		
		counts.forEachEntry(new TObjectIntProcedure<String>() {
			@Override
			public boolean execute(String term, int count) {
				SingleTermQuery stq = new SingleTermQuery(term);
				stq.setWeight(count);
				add(stq);
				return true;
			}
		});		
	}
	
	/**
	 * Get the underlying document used to construct the query
	 * @return the document
	 */
	public QLFDocument<T> getDocument() {
		return document;
	}
}
