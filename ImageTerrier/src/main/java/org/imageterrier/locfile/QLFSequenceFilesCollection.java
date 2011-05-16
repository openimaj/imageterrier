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
 * The Original Code is QLFSequenceFilesCollection.java
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

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;

import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.BytesWritable;
import org.apache.hadoop.io.SequenceFile;
import org.apache.hadoop.io.Text;
import org.apache.log4j.Logger;
import org.openimaj.feature.local.list.MemoryLocalFeatureList;
import org.openimaj.feature.local.quantised.QuantisedLocalFeature;
import org.openimaj.hadoop.sequencefile.SequenceFileUtility;
import org.openimaj.hadoop.sequencefile.TextBytesSequenceFileUtility;
import org.terrier.indexing.Collection;
import org.terrier.indexing.Document;

/**
 * A concrete implementation of a {@link Collection} of 
 * {@link QLFDocument}s stored within a Hadoop {@link SequenceFile}.
 * 
 * @author Jonathon Hare <jsh2@ecs.soton.ac.uk>
 *
 * @param <F> type of feature
 */
public class QLFSequenceFilesCollection <F extends QuantisedLocalFeature<?>> implements QLFCollection<F> {
	static Logger logger = Logger.getLogger(QLFSequenceFilesCollection.class);
	
	protected LinkedList<URI> fileList = new LinkedList<URI>();
	protected LinkedList<URI> indexedList = new LinkedList<URI>();
	Iterator<Entry<Text,BytesWritable>> currentIterator = null;
	URI currentFile = null;
	protected Document currentDocument = null;
	protected int docid = -1;
	protected int maxIdChars = -1;
	
	Class<F> featureClass;
	
	public QLFSequenceFilesCollection(String uriOrPath, Class<F> featureClass) throws IOException {
		this.featureClass = featureClass;
		
		Path[] paths = SequenceFileUtility.getFilePaths(uriOrPath,"part");
		ArrayList<URI> uris = new ArrayList<URI>();
		for(Path p : paths) uris.add(p.toUri());
		fileList.addAll(uris);
	}
	
	@Override
	public void close() {
		return;
	}

	@Override
	public boolean endOfCollection() {
		return (fileList.size() == 0 && (currentIterator == null || !currentIterator.hasNext()));
	}

	@Override
	public String getDocid() {
		return docid+"";
	}

	@Override
	public Document getDocument() {
		return currentDocument;
	}

	@Override
	public void reset() {
		docid = -1;
		fileList.addAll(0, indexedList);
		indexedList.clear();
		nextDocument();
	}
	
	@Override
	public boolean nextDocument() {
		if(docid % 100 == 0) logger.debug("Getting document" + docid);
		
		if (endOfCollection())
			return false;

		if (currentIterator == null || !currentIterator.hasNext()) {
			currentFile = fileList.removeFirst();
			try {
				currentIterator = new TextBytesSequenceFileUtility(currentFile, true).iterator();
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
			indexedList.add(currentFile);
		}
		
		try {
			Entry<Text, BytesWritable> currentEntry = currentIterator.next();
			
			MemoryLocalFeatureList<F> loc = MemoryLocalFeatureList.read(
					new ByteArrayInputStream(currentEntry.getValue().getBytes()),
					featureClass);
			
			currentDocument = new QLFDocument<F>(loc, docid+"", null);
			currentDocument.getAllProperties().put("sequenceFileURI", currentFile.toString());
			currentDocument.getAllProperties().put("identifier", currentEntry.getKey().toString());
			currentDocument.getAllProperties().put("path", new File(currentFile.getRawPath()).getName() + "?key=" + currentEntry.getKey().toString());

			docid++;
			return true;
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
	
	@Override
	public int getMaxNumIDChars() {
		if(maxIdChars!=-1){
			return maxIdChars;
		}
		int count = 0;
		
		try {
			for (URI u : fileList) {
				TextBytesSequenceFileUtility sf = new TextBytesSequenceFileUtility(u, true);
				count += sf.getNumberRecords();
			}
			for (URI u : indexedList) {
				TextBytesSequenceFileUtility sf = new TextBytesSequenceFileUtility(u, true);
				count += sf.getNumberRecords();
			}
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		
		return ("" + count).length();
	}
	
	protected int getMaxPathChars(List<URI> fl) {
		int max = 0;
		
		try {
			for (URI u : fl) {
				TextBytesSequenceFileUtility sf = new TextBytesSequenceFileUtility(u, true);
				
				for (Text t : sf.listKeys()) {
					if (t.getLength() > max) max = t.getLength() + (sf.getSequenceFilePath().getName() + "?key=").length();
				}
			}
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		
		return max;
	}
	
	@Override
	public int getMaxPathChars() {
		return Math.max(getMaxPathChars(fileList), getMaxPathChars(indexedList));
	}

	public void setMaxIdChars(int maxIdChars) {
		this.maxIdChars = maxIdChars;
	}
}
