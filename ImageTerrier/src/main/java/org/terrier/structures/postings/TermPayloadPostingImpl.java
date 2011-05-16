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
 * The Original Code is TermPayloadPostingImpl.java
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

import org.terrier.structures.TermPayloadCoordinator;
import org.terrier.structures.postings.BasicPostingImpl;
import org.terrier.structures.postings.WritablePosting;

/**
 * A posting holding payloads
 * 
 * @author Jonathon Hare <jsh2@ecs.soton.ac.uk>
 *
 * @param <PAYLOAD> the payload type
 */
public class TermPayloadPostingImpl<PAYLOAD> extends BasicPostingImpl implements TermPayloadPosting<PAYLOAD> {
	/**
	 * This stores info about the payload
	 */
	protected TermPayloadCoordinator<PAYLOAD> payloadConf;
	
	PAYLOAD[] payloads;

	/**
	 * @param docid
	 * @param frequency
	 * @param _positions
	 * @param payloadConf
	 */
	public TermPayloadPostingImpl(int docid, int frequency, PAYLOAD[] _positions, TermPayloadCoordinator<PAYLOAD> payloadConf) {
		super(docid, frequency);
		payloads = _positions;
		this.payloadConf = payloadConf;
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
		
		return new TermPayloadPostingImpl<PAYLOAD>(id, tf, newPayloads, payloadConf);
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
