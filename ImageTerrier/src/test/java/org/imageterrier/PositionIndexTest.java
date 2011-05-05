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
 * The Original Code is PositionIndexTest.java
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
import gnu.trove.TIntIntHashMap;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.imageterrier.basictools.BasicTerrierConfig;
import org.imageterrier.dsms.ConsistentOriScoreModifier;
import org.imageterrier.indexing.PositionSinglePassIndexer;
import org.imageterrier.locfile.PositionSpec;
import org.imageterrier.locfile.QLFDocument;
import org.imageterrier.locfile.QLFInMemoryCollection;
import org.imageterrier.querying.parser.QLFDocumentQuery;
import org.imageterrier.structures.PositionInvertedIndex;
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
import org.terrier.structures.Index;
import org.terrier.utility.ApplicationSetup;



public class PositionIndexTest {
	static {
		BasicTerrierConfig.configure(); //initialise terrier
	}
	
	File indexDir;
	String indexName = "test";
	QLFInMemoryCollection<QuantisedKeypoint> collection;
	PositionSpec positionSpec;
	
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
		
		positionSpec = new PositionSpec(PositionSpec.PositionSpecMode.SPATIAL_SCALE_ORI, new int[] {8,8,8,8}, new double[]{0,0,0,-Math.PI}, new double[]{640,480,100,Math.PI});
	}
	
	@After
	public void cleanup() {
		for (File f : indexDir.listFiles()) f.delete();
		indexDir.delete();
	}
	
	public void buildIndex() {
		PositionSinglePassIndexer indexer = new PositionSinglePassIndexer(indexDir.toString(), indexName, positionSpec);
		indexer.createInvertedIndex(new Collection[] { collection });
	}
	
	public void testIndex() throws IOException {
		Index index = Index.createIndex(indexDir.toString(), indexName);
		PositionInvertedIndex invidx = (PositionInvertedIndex) index.getInvertedIndex();
		
		//check positionSpec
		PositionSpec readspec = invidx.getPositionSpec();
		assertEquals(positionSpec.getMode(), readspec.getMode());
		assertArrayEquals(positionSpec.getLowerBounds(), readspec.getLowerBounds(), 0.01);
		assertArrayEquals(positionSpec.getUpperBounds(), readspec.getUpperBounds(), 0.01);
		assertArrayEquals(positionSpec.getPositionBits(), readspec.getPositionBits());
		
		ApplicationSetup.setProperty("ignore.low.idf.terms","false");
		ApplicationSetup.setProperty("matching.dsms", ConsistentOriScoreModifier.class.getName());
		ApplicationSetup.setProperty("consistent.ori.histogram.bins", "36");
		
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
	
	@Test
	public void test() throws IOException {
		buildIndex();
		testIndex();
	}
}
