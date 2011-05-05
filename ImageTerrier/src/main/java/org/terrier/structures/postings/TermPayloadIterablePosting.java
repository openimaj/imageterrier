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
 * The Original Code is TermPayloadIterablePosting.java
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
package org.terrier.structures.postings;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import org.terrier.compression.BitIn;
import org.terrier.structures.DocumentIndex;
import org.terrier.structures.TermPayloadCoordinator;
import org.terrier.structures.postings.BasicIterablePosting;
import org.terrier.structures.postings.WritablePosting;

public class TermPayloadIterablePosting<PAYLOAD> extends BasicIterablePosting implements TermPayloadPosting<PAYLOAD> {
	/**
	 * This stores info about the payload
	 */
	protected TermPayloadCoordinator<PAYLOAD> payloadConf;
	
	protected PAYLOAD[] payloads;
	
	public TermPayloadIterablePosting() { super(); }
	
	public TermPayloadIterablePosting(BitIn _bitFileReader, int _numEntries, DocumentIndex doi) throws IOException {
		super(_bitFileReader, _numEntries, doi);
	}

	public void setPayloadConfig(TermPayloadCoordinator<PAYLOAD> payloadConf) {
		this.payloadConf = payloadConf;
	}
	
	@Override
	public int next() throws IOException {
		if (numEntries-- == 0) return EOL;
		
		id = bitFileReader.readGamma() + id; //id is the docid
		tf = bitFileReader.readUnary(); //tf is how many times the term occurs in the doc
		
		//TODO: this has a memory allocation for every posting in the posting list. can we reuse an array?
		payloads = payloadConf.makePayloadArray(tf);
		
		for (int k=0; k<tf; k++) {
			payloads[k] = payloadConf.readPayload(bitFileReader); 
		}
		
		return id;
	}
	
	@Override
	public PAYLOAD[] getPayloads() {
		return payloads;
	}

	@Override
	public void readFields(DataInput in) throws IOException {
		super.readFields(in);
		
		payloads = payloadConf.makePayloadArray(tf); 
		
		for(int i=0;i<tf;i++) {
			payloads[i] = payloadConf.readPayload(in);
		}
	}

	@Override
	public void write(DataOutput out) throws IOException {
		super.write(out);
		
		for(PAYLOAD payload : payloads) {
			payloadConf.writePayload(out, payload);
		}
	}

	@Override
	public WritablePosting asWritablePosting() {
		PAYLOAD[] newPayloads = payloads.clone();
		
		for(int row=0; row<payloads.length; row++)
			newPayloads[row] = payloadConf.clone(payloads[row]);
		
		return new TermPayloadPostingImpl<PAYLOAD>(getId(), getFrequency(), newPayloads, payloadConf);
	}

	@Override
	public String toString()
	{
		String pos = "";
		for (PAYLOAD payload : payloads) pos += "(" + payload + "),";
		pos.subSequence(0, pos.length() - 1);
		
		return "(" + id + "," + tf + ",B[" + pos + "])";
	}
}
