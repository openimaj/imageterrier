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
 * The Original Code is QLFInMemoryCollection.java
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

import java.io.IOException;
import java.util.List;

import org.openimaj.feature.local.quantised.QuantisedLocalFeature;


@SuppressWarnings("rawtypes")
public class QLFInMemoryCollection<F extends QuantisedLocalFeature<?>> implements QLFCollection {
	public List<QLFDocument<F>> docs;
	int idx = -1;
	
	public QLFInMemoryCollection(List<QLFDocument<F>> docs) {
		this.docs = docs;
	}
	
	@Override
	public void close() throws IOException { /* do nothing */ }

	@Override
	public boolean nextDocument() {
		if (idx+1 >= docs.size()) return false; 
		idx++;
		return true;
	}

	@Override
	public QLFDocument<F> getDocument() {
		return docs.get(idx);
	}

	@Override
	public String getDocid() {
		return null;
	}

	@Override
	public boolean endOfCollection() {
		return (idx+1 >= docs.size());
	}

	@Override
	public void reset() {
		idx = -1;
	}

	@Override
	public int getMaxNumIDChars() {
		return ((docs.size() + 1) + "").length();
	}

	@Override
	public int getMaxPathChars() {
		return 0;
	}
}
