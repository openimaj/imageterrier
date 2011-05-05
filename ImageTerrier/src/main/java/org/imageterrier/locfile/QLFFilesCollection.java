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
 * The Original Code is QLFFilesCollection.java
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

import java.io.File;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

import org.openimaj.feature.local.quantised.QuantisedLocalFeature;
import org.terrier.indexing.Document;


public class QLFFilesCollection<F extends QuantisedLocalFeature<?>> implements QLFCollection<F> {
	protected LinkedList<File> fileList = new LinkedList<File>();
	protected LinkedList<File> indexedList = new LinkedList<File>();
	protected File currentFile = null; 
	protected Document currentDocument = null;
	protected int docid = -1;
	protected String pathRegexFind = null;
	protected String pathRegexRep = null;
	
	Class<F> featureClass;
	
	public QLFFilesCollection(File directory, String extension, Class<F> featureClass) {
		this(featureClass);
		
		processDirs(directory, extension);
	}
	
	public QLFFilesCollection(Class<F> featureClass) {
		this.featureClass = featureClass;
	}
	
	public QLFFilesCollection(File directory, String extension, Class<F> featureClass, String pathRegexFind, String pathRegexRep) {
		this(featureClass, pathRegexFind, pathRegexRep);
		
		processDirs(directory, extension);
	}
	
	public QLFFilesCollection(Class<F> featureClass, String pathRegexFind, String pathRegexRep) {
		this(featureClass);
		this.pathRegexFind = pathRegexFind;
		this.pathRegexRep = pathRegexRep;
	}
	
	public void addFile(File file) {
		fileList.addFirst(file);
	}
	
	public void setPathRegex(String find, String replace) {
		pathRegexFind = find;
		pathRegexRep = replace;
	}
	
	protected void processDirs(File dir, String ext) {
		for (File f : dir.listFiles()) {
			if (f.isDirectory()) {
				processDirs(f, ext);
			} else {
				if (f.getName().endsWith(ext) && !f.getName().startsWith(".")) {
					fileList.add(f);
				}
			}
		}
	}
	
	@Override
	public void close() {
		return;
	}

	@Override
	public boolean endOfCollection() {
		return (fileList.size() == 0);
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
	
	public List<File> getIndexDocuments() {
		return indexedList;
	}

	@Override
	public boolean nextDocument() {
		if (fileList.size() == 0)
			return false;

		currentFile = fileList.removeFirst();
		indexedList.add(currentFile);
		
		try {
			currentDocument = new QLFDocument<F>(currentFile, this.featureClass, docid+"", null);
			if (pathRegexFind == null)
				currentDocument.getAllProperties().put("path", currentFile.getAbsolutePath());
			else
				currentDocument.getAllProperties().put("path", currentFile.getAbsolutePath().replaceAll(pathRegexFind, pathRegexRep));
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		docid++;
		return true;
	}
	
	@Override
	public int getMaxNumIDChars() {
		return ((fileList.size() + indexedList.size() + 1) + "").length();
	}
	
	protected int getMaxPathChars(List<File> fl) {
		int max = 0;
		
		for (File f : fl) {
			int sz;
			if (pathRegexFind == null)
				sz = f.getAbsolutePath().length();
			else
				sz = f.getAbsolutePath().replaceAll(pathRegexFind, pathRegexRep).length();
			
			if (sz > max) max = sz;
		}
		
		return max;
	}
	
	@Override
	public int getMaxPathChars() {
		return Math.max(getMaxPathChars(fileList), getMaxPathChars(indexedList));
	}
}
