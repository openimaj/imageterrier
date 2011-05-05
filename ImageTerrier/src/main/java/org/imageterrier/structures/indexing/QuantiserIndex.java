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
 * The Original Code is QuantiserIndex.java
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
package org.imageterrier.structures.indexing;

import java.io.File;
import java.io.IOException;

import org.openimaj.io.IOUtils;
import org.openimaj.ml.clustering.Cluster;
import org.terrier.structures.Index;

import org.openimaj.tools.clusterquantiser.ClusterType;

public class QuantiserIndex {
	private static final String EXTENSION = ".featurequantiser";
	
	private Cluster<?,?> quantiser;
	
	public QuantiserIndex(String path, String prefix) throws IOException {
		load(new File(path + File.separator + prefix + EXTENSION));
	}
	
	public QuantiserIndex(Cluster<?,?> quantiser) {
		this.quantiser = quantiser;
	}
	
	public void load(File file) throws IOException {
		ClusterType quantiserType = ClusterType.sniffClusterType(file);
		
		quantiser = IOUtils.read(file, quantiserType.getClusterClass());
	}
	
	public void save(Index index) throws IOException {
		String path = index.getPath();
		String prefix = index.getPrefix();
		
		save(new File(path + File.separator + prefix + EXTENSION));
	}
	
	public void save(File file) throws IOException {
		IOUtils.writeBinary(file, quantiser);
	}
	
	public Cluster<?,?> getQuantiser() {
		return quantiser;
	}
}
