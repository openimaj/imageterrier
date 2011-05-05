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
 * The Original Code is TermPayloadCoordinator.java
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
package org.terrier.structures;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import org.terrier.compression.BitIn;
import org.terrier.compression.BitOut;
import org.terrier.compression.MemorySBOS;
import org.terrier.indexing.Document;
import org.terrier.structures.Index;

public abstract class TermPayloadCoordinator<PAYLOAD> {
	private static final String PAYLOAD_CONTROLLER_CLASS_KEY = "payload.controller.class";
		
	/**
	 * Add any payload controller related parameters to the index.
	 * Called once the indexer has finished building the index.
	 * 
	 * @param currentIndex
	 */
	public void saveConfiguration(Index currentIndex) {
		currentIndex.setIndexProperty(PAYLOAD_CONTROLLER_CLASS_KEY, this.getClass().getName());
	}
	
	public void readConfiguration(Index currentIndex) {
		//Intentionally blank. Subclasses can override as necessary.
	}

	/**
	 * Build a new payload for the given term and document.
	 * The document will currently be pointing at the respective
	 * term.
	 * @param doc
	 * @param term
	 * @return
	 */
	public abstract PAYLOAD createPayload(Document doc, String term);

	public abstract void append(BitIn postingSource, BitOut bos) throws IOException;

	public abstract void writePayloadInMem(MemorySBOS docIds, PAYLOAD payload) throws IOException;

	public abstract PAYLOAD readPayload(BitIn bitFileReader) throws IOException;

	public abstract PAYLOAD readPayload(DataInput in) throws IOException;

	public abstract void writePayload(DataOutput out, PAYLOAD payload) throws IOException;

	public abstract PAYLOAD clone(PAYLOAD payload);

	public static <PAYLOAD> TermPayloadCoordinator<PAYLOAD> create(Index i) {
		String clzStr = i.getIndexProperty(PAYLOAD_CONTROLLER_CLASS_KEY, "");
		
		try {
			@SuppressWarnings("unchecked")
			Class<TermPayloadCoordinator<PAYLOAD>> clz = (Class<TermPayloadCoordinator<PAYLOAD>>) Class.forName(clzStr);
			
			TermPayloadCoordinator<PAYLOAD> pc = clz.newInstance();
			pc.readConfiguration(i);
			
			return pc;
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	public abstract PAYLOAD[] makePayloadArray(int size);
}
