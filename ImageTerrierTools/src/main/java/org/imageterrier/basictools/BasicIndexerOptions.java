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
 * The Original Code is BasicIndexerOptions.java
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
package org.imageterrier.basictools;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.imageterrier.toolopts.IndexType;
import org.kohsuke.args4j.Argument;
import org.kohsuke.args4j.Option;
import org.kohsuke.args4j.ProxyOptionHandler;

import org.openimaj.tools.clusterquantiser.ClusterType;
import org.openimaj.tools.localfeature.LocalFeatureMode;

/**
 * Options for indexing
 * 
 * @author Jonathon Hare
 */
public class BasicIndexerOptions {
	@Option(name = "--output", aliases = "-o", usage = "path at which to write index", required = true, metaVar = "path")
	private String filename;

	@Option(name = "--quant-file", aliases = "-q", usage = "path to quantiser file", required = false, metaVar = "path")
	private File quantiserFile;
	
	@Option(name = "--quant-type", aliases = "-qt", usage = "Quantiser type. Defaults to AKM (FastKMeans)", required = false, metaVar = "type", handler=ProxyOptionHandler.class) 
	protected ClusterType quantiserType = ClusterType.FASTKMEANS;
	
	@Option(name = "--feature-type", aliases = "-ft", usage = "Feature type. Defaults to plain DoG/SIFT", required = false, metaVar = "type", handler=ProxyOptionHandler.class)
	LocalFeatureMode featureType = LocalFeatureMode.SIFT;
	
	@Option(name = "--force-regeneration", aliases = "-f", usage = "force visterm regeneration")
	private boolean forceRegeneration = false;

	@Option(name="--type", aliases="-t", required=false, usage="Choose index type",handler=ProxyOptionHandler.class)
	private IndexType indexType = IndexType.BASIC;
	
	@Option(name = "--verbose", aliases = "-v", usage = "print verbose output")
	boolean verbose = false;
	
	@Argument(required = true)
	private List<String> searchPaths = new ArrayList<String>();

	public boolean isVerbose() {
		return verbose;
	}
	
	public String getFilename() {
		return filename;
	}

	public boolean forceRegeneration() {
		return forceRegeneration;
	}

	public List<String> getSearchPaths() {
		return searchPaths;
	}

	public File getQuantiserFile() throws IOException {
		if (quantiserFile == null) {
			 quantiserFile = File.createTempFile("imageterrier", ".cluster");
			 quantiserFile.delete();
		}
		
		return quantiserFile;
	}

	public ClusterType getQuantiserType() {
		return quantiserType;
	}

	public LocalFeatureMode getFeatureType() {
		return featureType;
	}
	
	public IndexType getIndexType() {
		return indexType;
	}
}
