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
import org.openimaj.knn.CoordinateKDTree;
import org.openimaj.math.geometry.point.Point2dImpl;
import org.openimaj.math.geometry.shape.Rectangle;
import org.terrier.indexing.Document;


/**
 * A concrete {@link Document} implementation for documents
 * built up of a list of visual terms in the form of
 * {@link QuantisedLocalFeature}s.
 * 
 * @author Jonathon Hare <jsh2@ecs.soton.ac.uk>
 *
 * @param <F>
 */
public class QLFDocument<F extends QuantisedLocalFeature<?>> implements Document {
	protected List<F> featureList;
	protected Iterator<F> iterator;
	protected Map<String,String> props = new HashMap<String, String>();
	protected String termPrefix = "loc";
	protected String termSuffix = "";
	protected F current = null;
	
	/**
	 * Construct a new QLFDocument instance from the contents of the given file.
	 * The file contents must be compatible with {@link FileLocalFeatureList}.
	 * 
	 * @param file the file to read
	 * @param clz the class of {@link QuantisedLocalFeature}.
	 * @param docno the document identifier or number
	 * @param extraProps a map of properties to store in the metadata index
	 * @throws IOException
	 */
	public QLFDocument(File file, Class<F> clz, String docno, Map<String, String> extraProps) throws IOException {
		//TODO get metadata from list and store in props
		this(FileLocalFeatureList.read(file, clz), docno, extraProps);
	}
	
	/**
	 * Construct a new QLFDocument instance from the contents of the given byte array.
	 * The byte array contents must be compatible with {@link MemoryLocalFeatureList}.
	 * 
	 * @param bytes an array of bytes from which to read the features.
	 * @param clz the class of {@link QuantisedLocalFeature}.
	 * @param docno the document identifier or number
	 * @param extraProps a map of properties to store in the metadata index
	 * @throws IOException
	 */
	public QLFDocument(byte[] bytes, Class<F> clz, String docno, Map<String,String> extraProps) throws IOException {
		this(MemoryLocalFeatureList.read(new ByteArrayInputStream(bytes), clz), docno, extraProps);
	}
	
	/**
	 * Construct a new QLFDocument instance from the contents of the given feature list.
	 * 
	 * @param list the feature list
	 * @param docno the document identifier or number
	 * @param extraProps a map of properties to store in the metadata index
	 */
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
	
	/**
	 * Get the underlying list of features.
	 * @return the feature list
	 */
	public List<F> getEntries() {
		return featureList;
	}
	
	/**
	 * Convenience methods to filter all features that have spatial locations 
	 * outside the given rectangle. It is assumed that the 0th ordinate of the
	 * feature location is the x-ordinate, and the 1st is the y ordinate.
	 * @param rect the rectangle.
	 */
	public void filter(Rectangle rect) {
		float x1 = rect.x;
		float y1 = rect.y;
		float x2 = rect.x + rect.width;
		float y2 = rect.y + rect.height;
		
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

	/**
	 * Reset the document to its default state, with the 
	 * current term pointer just before the first term.
	 */
	public void reset() {
		iterator = featureList.iterator();
		current = null;		
	}

	/**
	 * Get the location associated with the current term
	 * pointer.
	 * @return the current location.
	 */
	public Location getLocation() {
		return current.getLocation();
	}
	
	/**
	 * Get the spatially nearest neighbouring terms to the
	 * target term. It is assumed that the 0th ordinate of the
	 * feature location is the x-ordinate, and the 1st is the y ordinate.
	 * @param target the target term. 
	 * @param nNeighbours the number of neighbours.
	 * @return a list of neighbouring terms
	 */
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
	
	/**
	 * Get the spatially nearest neighbouring terms to the
	 * current term. It is assumed that the 0th ordinate of the
	 * feature location is the x-ordinate, and the 1st is the y ordinate.
	 * @param limit the number of neighbours.
	 * @return an array of neighbouring terms ids
	 */
	public int [] getCurrentNearestNeighbourTIds(int limit) {
		List<F> nns = getNearestNeighbours(current, limit);
		int [] ids = new int[nns.size()];
		for (int i=0; i<ids.length; i++) 
			ids[i] = nns.get(i).id;
		return ids;
	}
	
	CoordinateKDTree<Pt> tree;
	
	class Pt extends Point2dImpl {
		/**
		 * 
		 */
		private static final long serialVersionUID = 2587165830032484705L;
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
	
	/**
	 * Get the spatially nearest neighbouring terms to the
	 * target term. It is assumed that the 0th ordinate of the
	 * feature location is the x-ordinate, and the 1st is the y ordinate.
	 * 
	 * This method uses an underlying KD-Tree to speed the neighbour operation.
	 * 
	 * @param target the target term. 
	 * @param nNeighbours the number of neighbours.
	 * @return a list of neighbouring terms
	 */
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
	
	/**
	 * Get the spatially nearest neighbouring terms to the
	 * current term. It is assumed that the 0th ordinate of the
	 * feature location is the x-ordinate, and the 1st is the y ordinate.
	 * 
	 * This method uses an underlying KD-Tree to speed the neighbour operation.
	 * 
	 * @param limit the number of neighbours.
	 * @return an array of neighbouring terms ids
	 */
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
}
