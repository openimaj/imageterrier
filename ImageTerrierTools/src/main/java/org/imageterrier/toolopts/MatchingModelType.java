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
 * The Original Code is MatchingModelType.java
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
package org.imageterrier.toolopts;

import gnu.trove.TIntIntHashMap;

import org.imageterrier.querying.parser.QLFDocumentQuery;
import org.openimaj.feature.local.quantised.QuantisedLocalFeature;
import org.terrier.querying.SearchRequest;
import org.terrier.utility.ApplicationSetup;


/*
 * Ranking model
 */
public enum MatchingModelType {
	TFIDF {
		@Override
		public <T extends QuantisedLocalFeature<?>> void configureRequest(SearchRequest request, QLFDocumentQuery<T> query) {
			request.addMatchingModel(ApplicationSetup.getProperty("matching.mmodel", "Matching"), "TF_IDF");
		}	
	}, 
	L1 {
		@Override
		public <T extends QuantisedLocalFeature<?>> void configureRequest(SearchRequest request, QLFDocumentQuery<T> query) {
			TIntIntHashMap counts = new TIntIntHashMap();
			int max = 0;
			for (T ln : query.getDocument().getEntries()) {
				counts.adjustOrPutValue(ln.id, 1, 1);
				if (counts.get(ln.id) > max) max = counts.get(ln.id);			
			}

			request.addMatchingModel(ApplicationSetup.getProperty("matching.mmodel", "Matching"), org.imageterrier.models.L1WeightingModel.class.getName());
			request.setControl("c_set", "true");
			request.setControl("c", "" + ((double)max / (double)query.getNumberOfTerms()));
		}
	},
	L1QNORM {
		@Override
		public <T extends QuantisedLocalFeature<?>> void configureRequest(SearchRequest request, QLFDocumentQuery<T> query) {
			TIntIntHashMap counts = new TIntIntHashMap();
			int max = 0;
			for (T ln : query.getDocument().getEntries()) {
				counts.adjustOrPutValue(ln.id, 1, 1);
				if (counts.get(ln.id) > max) max = counts.get(ln.id);			
			}

			request.addMatchingModel(ApplicationSetup.getProperty("matching.mmodel", "Matching"), org.imageterrier.models.L1WeightingModel.class.getName());
			request.setControl("c_set", "true");
			request.setControl("c", "" + ((double)max / (double)query.getNumberOfTerms()*(double)query.getNumberOfTerms()));
		}
	},
	COSINE {
		@Override
		public <T extends QuantisedLocalFeature<?>> void configureRequest(SearchRequest request, QLFDocumentQuery<T> query) {
			TIntIntHashMap counts = new TIntIntHashMap();
			int max = 0;
			for (T ln : query.getDocument().getEntries()) {
				counts.adjustOrPutValue(ln.id, 1, 1);
				if (counts.get(ln.id) > max) max = counts.get(ln.id);			
			}

			request.addMatchingModel(ApplicationSetup.getProperty("matching.mmodel", "Matching"), org.imageterrier.models.CosineWeightingModel.class.getName());
			request.setControl("c_set", "true");
			request.setControl("c", "" + ((double)max / (double)query.getNumberOfTerms()*(double)query.getNumberOfTerms()));
			request.setControl("ql_set", "true");
			//				request.setControl("ql", "" + query.);
		}
	},
	L1IDF {
		@Override
		public <T extends QuantisedLocalFeature<?>> void configureRequest(SearchRequest request, QLFDocumentQuery<T> query) {
			TIntIntHashMap counts = new TIntIntHashMap();
			int max = 0;
			for (T ln : query.getDocument().getEntries()) {
				counts.adjustOrPutValue(ln.id, 1, 1);
				if (counts.get(ln.id) > max) max = counts.get(ln.id);			
			}

			request.addMatchingModel(ApplicationSetup.getProperty("matching.mmodel", "Matching"), org.imageterrier.models.L1IDFWeightingModel.class.getName());
			request.setControl("c_set", "true");
			request.setControl("c", "" + ((double)max / (double)query.getNumberOfTerms()));
		}
	};
	public abstract <T extends QuantisedLocalFeature<?>> void configureRequest(SearchRequest request, QLFDocumentQuery<T> query);
}
