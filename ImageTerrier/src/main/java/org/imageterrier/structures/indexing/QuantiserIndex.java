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
import org.openimaj.ml.clustering.SpatialClusterer;
import org.openimaj.ml.clustering.SpatialClusters;
import org.openimaj.tools.clusterquantiser.ClusterType;
import org.openimaj.tools.clusterquantiser.ClusterType.ClusterTypeOp;
import org.terrier.structures.Index;

/**
 * An index structure for an vector quantiser (an OpenIMAJ
 * {@link SpatialClusterer}) which can be used to quantise features into visual
 * terms.
 * 
 * @author Jonathon Hare <jsh2@ecs.soton.ac.uk>
 * 
 */
public class QuantiserIndex {
	private static final String EXTENSION = ".featurequantiser";

	private SpatialClusters<?> quantiser;

	/**
	 * Construct a QuantiserIndex from a file
	 * 
	 * @param path
	 *            the path to the file
	 * @param prefix
	 *            the prefix of the file name
	 * @throws IOException
	 */
	public QuantiserIndex(String path, String prefix) throws IOException {
		load(new File(path + File.separator + prefix + EXTENSION));
	}

	/**
	 * Construct a QuantiserIndex from an existing quantiser
	 * 
	 * @param quantiser
	 *            the quantiser
	 */
	public QuantiserIndex(SpatialClusters<?> quantiser) {
		this.quantiser = quantiser;
	}

	/**
	 * Load a quantiser from a file
	 * 
	 * @param file
	 * @throws IOException
	 */
	public void load(File file) throws IOException {
		final ClusterTypeOp quantiserType = ClusterType.sniffClusterType(file);

		quantiser = IOUtils.read(file, quantiserType.getClusterClass());
	}

	/**
	 * Save the QuantiserIndex to the specified index
	 * 
	 * @param index
	 * @throws IOException
	 */
	public void save(Index index) throws IOException {
		final String path = index.getPath();
		final String prefix = index.getPrefix();

		save(new File(path + File.separator + prefix + EXTENSION));
	}

	/**
	 * Save the quantiser to the specified file
	 * 
	 * @param file
	 *            the file to save to
	 * @throws IOException
	 */
	public void save(File file) throws IOException {
		IOUtils.writeBinary(file, quantiser);
	}

	/**
	 * Get the underlying vector quantiser
	 * 
	 * @return the vector quantiser
	 */
	public SpatialClusters<?> getQuantiser() {
		return quantiser;
	}
}
