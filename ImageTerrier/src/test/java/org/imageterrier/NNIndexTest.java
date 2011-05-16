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
 * The Original Code is NNIndexTest.java
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
package org.imageterrier;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import gnu.trove.TIntHashSet;
import gnu.trove.TIntIntHashMap;
import gnu.trove.TIntObjectHashMap;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.imageterrier.basictools.BasicTerrierConfig;
import org.imageterrier.dsms.NNScoreModifier;
import org.imageterrier.indexing.NNSinglePassIndexer;
import org.imageterrier.locfile.QLFDocument;
import org.imageterrier.locfile.QLFInMemoryCollection;
import org.imageterrier.querying.parser.QLFDocumentQuery;
import org.imageterrier.structures.NNInvertedIndex;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.openimaj.feature.local.list.MemoryLocalFeatureList;
import org.openimaj.feature.local.quantised.QuantisedLocalFeature;
import org.openimaj.image.feature.local.keypoints.quantised.QuantisedKeypoint;
import org.terrier.indexing.Collection;
import org.terrier.matching.ResultSet;
import org.terrier.querying.Manager;
import org.terrier.querying.SearchRequest;
import org.terrier.structures.BitIndexPointer;
import org.terrier.structures.Index;
import org.terrier.structures.LexiconEntry;
import org.terrier.structures.postings.TermPayloadIterablePosting;
import org.terrier.utility.ApplicationSetup;

/**
 * NNTest nearest-neighbour index
 * 
 * @author Jonathon Hare <jsh2@ecs.soton.ac.uk>
 *
 */
public class NNIndexTest {
	static {
		BasicTerrierConfig.configure(); //initialise terrier
	}
	
	static int NN = 10;
	
	File indexDir;
	String indexName = "test";
	QLFInMemoryCollection<QuantisedKeypoint> collection;
	
	/**
	 * Setup tests
	 * @throws IOException
	 */
	@Before
	public void setup() throws IOException {
		indexDir = File.createTempFile("terrier", "");
		indexDir.delete();
		indexDir.mkdir();
		
		List<QLFDocument<QuantisedKeypoint>> docs = new ArrayList<QLFDocument<QuantisedKeypoint>>();
		docs.add(new QLFDocument<QuantisedKeypoint>(MemoryLocalFeatureList.read(getClass().getResourceAsStream("/org/imageterrier/siftintensity/ukbench00000.jpg.loc"), QuantisedKeypoint.class), "00000", null));
		docs.add(new QLFDocument<QuantisedKeypoint>(MemoryLocalFeatureList.read(getClass().getResourceAsStream("/org/imageterrier/siftintensity/ukbench00001.jpg.loc"), QuantisedKeypoint.class), "00001", null));
		docs.add(new QLFDocument<QuantisedKeypoint>(MemoryLocalFeatureList.read(getClass().getResourceAsStream("/org/imageterrier/siftintensity/ukbench01001.jpg.loc"), QuantisedKeypoint.class), "01001", null));
		collection = new QLFInMemoryCollection<QuantisedKeypoint>(docs);
	}
	
	/**
	 * cleanup tests
	 */
	@After
	public void cleanup() {
		for (File f : indexDir.listFiles()) f.delete();
		indexDir.delete();
	}
	
	/**
	 * test indexing
	 */
	public void buildIndex() {
		NNSinglePassIndexer indexer = new NNSinglePassIndexer(indexDir.toString(), indexName, NN);
		indexer.createInvertedIndex(new Collection[] { collection });
	}
	
	/**
	 * test searching
	 * @throws IOException
	 */
	public void testIndex() throws IOException {
		Index index = Index.createIndex(indexDir.toString(), indexName);
		NNInvertedIndex invidx = (NNInvertedIndex) index.getInvertedIndex();
		
		assertEquals(""+NN, index.getProperties().get("nearest.neighbours"));
		
		collection.reset();
		collection.nextDocument();
		QLFDocument<QuantisedKeypoint> doc = collection.getDocument();
		doc.reset();
		String term = doc.getNextTerm();
		int [] nns = doc.getCurrentNearestNeighbourTIdsKD(NN);
		TIntHashSet set = new TIntHashSet(nns);
		nns = set.toArray();
		Arrays.sort(nns);
		
		LexiconEntry le = index.getLexicon().getLexiconEntry(term);

		TermPayloadIterablePosting<int[]> posting = invidx.getPostings((BitIndexPointer) le);
		TIntObjectHashMap<int[][]> payloads = invidx.getPayloads((BitIndexPointer) le);
		
		posting.next();
		
		assertEquals(0, posting.getId()); //first doc in posting should be 0
		assertArrayEquals(nns, posting.getPayloads()[0]); //the neighbours should match
		assertArrayEquals(nns, payloads.get(posting.getId())[0]); //the neighbours should match
		
		ApplicationSetup.setProperty("ignore.low.idf.terms","false");
		ApplicationSetup.setProperty("matching.dsms", NNScoreModifier.class.getName());
//		ApplicationSetup.setProperty("nearest.neighbours", "10");
		
		QLFDocument<QuantisedKeypoint> qdoc = new QLFDocument<QuantisedKeypoint>(MemoryLocalFeatureList.read(getClass().getResourceAsStream("siftintensity/ukbench00000.jpg.loc"), QuantisedKeypoint.class), "00000", null);
		QLFDocumentQuery<QuantisedKeypoint> query = new QLFDocumentQuery<QuantisedKeypoint>(qdoc);
		
		ResultSet res = search(index, query);
		for (int i=0; i<res.getScores().length; i++) {
//			System.out.println(res.getDocids()[i] + "\t" + res.getScores()[i]);
			assertEquals(i, res.getDocids()[i]);
		}
	}
	
	protected <T extends QuantisedLocalFeature<?>> void configureRequest(SearchRequest request, QLFDocumentQuery<T> query) {
		TIntIntHashMap counts = new TIntIntHashMap();
		int max = 0;
		for (T ln : query.getDocument().getEntries()) {
			counts.adjustOrPutValue(ln.id, 1, 1);
			if (counts.get(ln.id) > max) max = counts.get(ln.id);			
		}

		request.addMatchingModel("Matching", org.imageterrier.models.L1WeightingModel.class.getName());
		request.setControl("c_set", "true");
		request.setControl("c", "" + ((double)max / (double)query.getNumberOfTerms()));
	}
	
	protected <T extends QuantisedLocalFeature<?>> ResultSet search(Index index, QLFDocumentQuery<T> query) {
		Manager manager = new Manager(index);
		SearchRequest request = manager.newSearchRequest("foo");
		request.setQuery(query);

		configureRequest(request, query);

		manager.runPreProcessing(request);
		manager.runMatching(request);
		manager.runPostProcessing(request);
		manager.runPostFilters(request);

		return request.getResultSet();
	}
	
	/**
	 * build and search index
	 * @throws IOException
	 */
	@Test
	public void test() throws IOException {
		buildIndex();
		testIndex();
	}
}
