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

/**
 * The abstract base class for classes capable of controlling
 * how a term payload is written to the index.
 * 
 * @author Jonathon Hare <jsh2@ecs.soton.ac.uk>
 *
 * @param <PAYLOAD> the type of payload
 */
public abstract class TermPayloadCoordinator<PAYLOAD> {
	private static final String PAYLOAD_COORDINATOR_CLASS_KEY = "payload.coordinator.class";
		
	/**
	 * Add any payload coordinator related parameters to the index.
	 * Called once the indexer has finished building the index.
	 * 
	 * @param currentIndex
	 */
	public void saveConfiguration(Index currentIndex) {
		currentIndex.setIndexProperty(PAYLOAD_COORDINATOR_CLASS_KEY, this.getClass().getName());
	}
	
	/**
	 * Read any payload coordinator related parameters from the index.
	 * Called straight after the coordinator is constructed. 
	 * @param currentIndex the index
	 * @see #create(Index)
	 */
	public void readConfiguration(Index currentIndex) {
		//Intentionally blank. Subclasses can override as necessary.
	}

	/**
	 * Build a new payload for the given term and document.
	 * The document will currently be pointing at the respective
	 * term.
	 * @param doc the document
	 * @param term the term
	 * @return a new payload object for the given term occurrence in the document 
	 */
	public abstract PAYLOAD createPayload(Document doc, String term);

	/**
	 * Transfer a single payload from an in-memory source to an on-disk source.
	 * The payload will have been previously written to the memory source using
	 * {@link #writePayloadInMem(MemorySBOS, Object)}.
	 * 
	 * Typically, in this method you'll read in the payload data from the
	 * input and write it to the output directly. Sometimes you might want
	 * to modify the encoding/format between memory and disk formats though.  
	 * @param postingSource the input
	 * @param bos the output
	 * @throws IOException
	 */
	public abstract void append(BitIn postingSource, BitOut bos) throws IOException;

	/**
	 * Write a single payload to an in-memory bit-stream. The format that
	 * is used to write the payload does not necessarily match the format
	 * that will be used to write the index to disk, as it can be changed 
	 * later by {@link #append(BitIn, BitOut)}. 
	 * @param sink the in-memory stream to write to.
	 * @param payload the payload to write.
	 * @throws IOException
	 */
	public abstract void writePayloadInMem(MemorySBOS sink, PAYLOAD payload) throws IOException;

	/**
	 * Read a single payload from a disk-backed input stream.
	 * The payload must be constructed from the encoding format
	 * used by {@link #append(BitIn, BitOut)}.
	 * 
	 * @param bitFileReader the input stream
	 * @return a new payload object constructed from the input stream
	 * @throws IOException
	 */
	public abstract PAYLOAD readPayload(BitIn bitFileReader) throws IOException;

	/**
	 * Read a single payload from a {@link DataInput} object.
	 * The payload must be constructed from the encoding format
	 * used by {@link #writePayload(DataOutput, Object)}.
	 * 
	 * @param in the input source object
	 * @return a new payload object constructed from the input object
	 * @throws IOException
	 */
	public abstract PAYLOAD readPayload(DataInput in) throws IOException;

	/**
	 * Write a single payload to the given {@link DataOutput} object.
	 * @param out the output sink object
	 * @param payload the payload to write
	 * @throws IOException
	 */
	public abstract void writePayload(DataOutput out, PAYLOAD payload) throws IOException;

	/**
	 * Create a cloned copy of a payload.
	 * @param payload payload to copy
	 * @return a copy
	 */
	public abstract PAYLOAD clone(PAYLOAD payload);

	/**
	 * Instantiate the specific payload coordinator used by the specified
	 * index, and configure it for use.
	 * 
	 * @param <PAYLOAD> the payload type
	 * @param i the index
	 * @return a TermPayloadCoordinator compatible with the specified index.
	 */
	public static <PAYLOAD> TermPayloadCoordinator<PAYLOAD> create(Index i) {
		String clzStr = i.getIndexProperty(PAYLOAD_COORDINATOR_CLASS_KEY, "");
		
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

	/**
	 * Construct an empty array of payloads of the given size.
	 * @param size the size of the array
	 * @return an array.
	 */
	public abstract PAYLOAD[] makePayloadArray(int size);
}
