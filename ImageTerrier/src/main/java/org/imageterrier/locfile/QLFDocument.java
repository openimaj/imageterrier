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
 * The Original Code is QLFDocument.java
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
package org.imageterrier.locfile;

import gnu.trove.TIntObjectHashMap;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import org.openimaj.feature.local.Location;
import org.openimaj.feature.local.list.FileLocalFeatureList;
import org.openimaj.feature.local.list.MemoryLocalFeatureList;
import org.openimaj.feature.local.quantised.QuantisedLocalFeature;
import org.openimaj.image.feature.local.affine.AffineSimulationKeypoint;
import org.openimaj.image.feature.local.affine.AffineSimulationKeypoint.AffineSimulationKeypointLocation;
import org.openimaj.knn.CoordinateKDTree;
import org.openimaj.math.geometry.point.Point2dImpl;
import org.terrier.indexing.Document;


public class QLFDocument<F extends QuantisedLocalFeature<?>> implements Document {
	protected List<F> featureList;
	protected Iterator<F> iterator;
	protected Map<String,String> props = new HashMap<String, String>();
	protected String termPrefix = "loc";
	protected String termSuffix = "";
	protected F current = null;
	
	//TODO get metadata from list and store in props
	public QLFDocument(File file, Class<F> clz, String docno, Map<String,String> extraProps) throws IOException {
		this(FileLocalFeatureList.read(file, clz), docno, extraProps);
	}
	
	public QLFDocument(byte[] bytes, Class<F> clz, String docno, Map<String,String> extraProps) throws IOException {
		this(MemoryLocalFeatureList.read(new ByteArrayInputStream(bytes), clz), docno, extraProps);
	}
	
	public QLFDocument(List<F> list, String docno, Map<String,String> extraProps) {
		featureList = new MemoryLocalFeatureList<F>(list);
		iterator = list.iterator();
		
		if (extraProps != null)
			props.putAll(extraProps);
		
		props.put("docno", docno);
	}
		
	@Override
	public String getNextTerm() {
		current = iterator.next();
		int id = current.id;
		return termPrefix + id + termSuffix;
	}

	@Override
	public Set<String> getFields() {
		return null;
	}

	@Override
	public boolean endOfDocument() {
		return !iterator.hasNext();
	}

	@Override
	public Reader getReader() {
		return null;
	}

	@Override
	public String getProperty(String name) {
		return props.get(name);
	}

	@Override
	public Map<String, String> getAllProperties() {
		return props;
	}
	
	public List<F> getEntries() {
		return featureList;
	}
	
	public void filter(float x1, float y1, float x2, float y2) {
		List<F> newEntries = new ArrayList<F>();
		TIntObjectHashMap<List<F>> newIndex = new TIntObjectHashMap<List<F>>();
	
		//TODO: this is currently selecting regions that overlap the bounds...
		//we might want to stop that...
		for (F le : featureList) {
			if (le.getLocation().getOrdinate(0).floatValue() >= x1 && le.getLocation().getOrdinate(0).floatValue() <= x2 &&
					le.getLocation().getOrdinate(1).floatValue() >= y1 && le.getLocation().getOrdinate(1).floatValue() <= y2) {
				newEntries.add(le);
				
				if (!newIndex.containsKey(le.id))
					newIndex.put(le.id, new ArrayList<F>());
				newIndex.get(le.id).add(le);
			}
		}
		featureList = newEntries;
		iterator = featureList.iterator();
		current = null;
	}

	public void reset() {
		iterator = featureList.iterator();
		current = null;		
	}

	public Location getLocation() {
		return current.getLocation();
	}
	
	public List<F> getNearestNeighbours(F target, int nNeighbours) {
		SortedSet<DistEntry> neighbourDist = new TreeSet<DistEntry>();
		
		for (F le : featureList) {
			if (le != target) {
				float x1 = le.getLocation().getOrdinate(0).floatValue();
				float y1 = le.getLocation().getOrdinate(1).floatValue();
				
				float x2 = target.getLocation().getOrdinate(0).floatValue();
				float y2 = target.getLocation().getOrdinate(1).floatValue();
				
				DistEntry de = new DistEntry();
				
				float dx = x1-x2;
				float dy = y1-y2;
				
				de.distance = dx*dx + dy*dy;
				de.le = le;
				
				neighbourDist.add(de);
			}
		}
		
		int count = 0;
		List<F> neighbours = new ArrayList<F>();
		for (DistEntry de : neighbourDist) {
			if (count >= nNeighbours) break;
			neighbours.add(de.le);
			
			count++;
		}
		
		return neighbours;
	}
	
	public int [] getCurrentNearestNeighbourTIds(int limit) {
		List<F> nns = getNearestNeighbours(current, limit);
		int [] ids = new int[nns.size()];
		for (int i=0; i<ids.length; i++) 
			ids[i] = nns.get(i).id;
		return ids;
	}
	
	CoordinateKDTree<Pt> tree;
	
	class Pt extends Point2dImpl {
		F f;
		public Pt(float x, float y, F f) {
			super(x, y);
			this.f = f;
		}
	}
	
	void makeTree() {
		List<Pt> pts = new ArrayList<Pt>();
		for (F f : featureList) {
			pts.add(new Pt(f.getLocation().getOrdinate(0).floatValue(), f.getLocation().getOrdinate(1).floatValue(), f));
		}
		
		tree = new CoordinateKDTree<Pt>(pts);
	}
	
	public int [] getNearestNeighboursKD(F target, int nNeighbours) {
		if (tree == null) makeTree();
		
		List<Pt> pts = new ArrayList<Pt>();
		tree.fastKNN(pts, new Pt(target.getLocation().getOrdinate(0).floatValue(), target.getLocation().getOrdinate(1).floatValue(), target), nNeighbours);
		
		int [] ids = new int[pts.size()];
		
		for (int i=0; i<ids.length; i++) {
			ids[i] = pts.get(i).f.id;
		}
		
		return ids;
	}
	
	public int [] getCurrentNearestNeighbourTIdsKD(int limit) {
		return getNearestNeighboursKD(current, limit);
	}
	
	protected class DistEntry implements Comparable<DistEntry> {
		float distance;
		F le;
		
		@Override
		public int compareTo(DistEntry o) {
			if (this.distance < o.distance) return -1;
			if (this.distance > o.distance) return 1;
			return 0;
		}
	}

	public int getSimulationIndex() {
		F ln = current;
		Location k = ln.getLocation();
		if(k instanceof AffineSimulationKeypoint.AffineSimulationKeypointLocation ){
			AffineSimulationKeypoint.AffineSimulationKeypointLocation ak = (AffineSimulationKeypointLocation) k;
			return ak.index;
		}
		return 0;
	}
}
