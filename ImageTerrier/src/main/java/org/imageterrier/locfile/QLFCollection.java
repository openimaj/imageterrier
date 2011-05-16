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
 * The Original Code is QLFCollection.java
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

import org.openimaj.feature.local.quantised.QuantisedLocalFeature;
import org.terrier.indexing.Collection;


/**
 * A {@link Collection} of documents built from {@link QuantisedLocalFeature}s.
 * 
 * @author Jonathon Hare <jsh2@ecs.soton.ac.uk>
 *
 * @param <F> type of feature
 */
public interface QLFCollection<F extends QuantisedLocalFeature<?>> extends Collection {
	/**
	 * Get the maximum number of characters required to store
	 * the IDs of all the documents in this collection.
	 * Assumes that the ID numbering is sequential from 1.
	 * 
	 * @return the maximum number of id characters
	 */
	public int getMaxNumIDChars();
	
	/**
	 * Get the maximum number of characters required to store
	 * the path of each file in the collection
	 * 
	 * @return  the maximum number of path characters
	 */
	public int getMaxPathChars();
}
