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

import java.util.ArrayList;
import java.util.List;

import org.imageterrier.toolopts.IndexType;
import org.imageterrier.toolopts.InputMode;
import org.imageterrier.toolopts.InputMode.InputModeOptions;
import org.kohsuke.args4j.Argument;
import org.kohsuke.args4j.Option;
import org.kohsuke.args4j.ProxyOptionHandler;

/**
 * Options for indexing
 * 
 * @author Jonathon Hare
 */
public class BasicIndexerOptions {
	@Option(name = "--mode", aliases = "-m", usage = "input mode", required = true, handler=ProxyOptionHandler.class)
	private InputMode inputMode;
	private InputModeOptions inputModeOp;
	
	@Option(name = "--output", aliases = "-o", usage = "path at which to write index", required = true, metaVar = "path")
	private String filename;

	@Option(name="--type", aliases="-t", required=false, usage="Choose index type", handler=ProxyOptionHandler.class)
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
	
	public IndexType getIndexType() {
		return indexType;
	}
	
	public List<String> getSearchPaths() {
		return searchPaths;
	}
	
	public InputMode getInputMode() {
		return inputMode;
	}
	
	public InputModeOptions getInputModeOptions() {
		return inputModeOp;
	}
}
