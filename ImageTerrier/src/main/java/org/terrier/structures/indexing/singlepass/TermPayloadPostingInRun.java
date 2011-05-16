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
 * The Original Code is TermPayloadPostingInRun.java
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
package org.terrier.structures.indexing.singlepass;

import java.io.IOException;

import org.terrier.compression.BitOut;
import org.terrier.structures.TermPayloadCoordinator;
import org.terrier.structures.indexing.singlepass.SimplePostingInRun;

/**
 * Class holding the information for a posting list containing payloads read
 * from a previously written run at disk. Used in the merging phase of the Single pass inversion method.
 * This class knows how to append itself to a {@link org.terrier.compression.BitOut} and it
 * represents the a posting with payloads: <code>(TF, df, [docid, tf, payload_1...payload_tf])</code>

 * @author Jonathon Hare <jsh2@ecs.soton.ac.uk>
 *
 * @param <PAYLOAD> the type of payload
 */
public class TermPayloadPostingInRun<PAYLOAD> extends SimplePostingInRun {
	/**
	 * This stores info about the payload
	 */
	protected TermPayloadCoordinator<PAYLOAD> payloadConf;
	
	/**
	 * Default Constructor
	 */
	public TermPayloadPostingInRun() {
		super();
	}

	/**
	 * Writes the document data of this posting to a {@link org.terrier.compression.BitOut} 
	 * It encodes the data with the right compression methods.
	 * The stream is written as <code>d1, idf(d1), blockNo(d1), bid1, bid2, ...,  d2 - d1, idf(d2), blockNo(d2), ...</code> etc
	 * @param bos BitOut to be written.
	 * @param last int representing the last document written in this posting.
	 * @param runShift amount of delta to apply to the first posting read.
	 * @return The last posting written.
	 */
	@Override
	public int append(BitOut bos, int last, int runShift)  throws IOException {
		int current = runShift - 1;
		for(int i = 0; i < termDf; i++){
			int docid = postingSource.readGamma() + current;
			bos.writeGamma(docid - last);

			int tf = postingSource.readGamma();
			bos.writeUnary(tf);
			current = last = docid;

			for (int j=0; j<tf; j++)
				payloadConf.append(postingSource, bos);
		}
		try{
			postingSource.align();
		}catch(Exception e){
			// last posting
		}
		return last;
	}

	/**
	 * Set the payload coordinator.
	 * @param payloadConf the coordinator.
	 */
	public void setPayloadConfig(TermPayloadCoordinator<PAYLOAD> payloadConf) {
		this.payloadConf = payloadConf;
	}
}
