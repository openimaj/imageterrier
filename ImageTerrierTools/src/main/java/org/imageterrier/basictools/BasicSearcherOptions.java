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
 * The Original Code is BasicSearcherOptions.java
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

import org.imageterrier.toolopts.MatchingModelType;
import org.imageterrier.toolopts.ScoreModifierType;
import org.imageterrier.toolopts.ScoreModifierType.ScoreModifierTypeOptions;
import org.kohsuke.args4j.Option;
import org.kohsuke.args4j.ProxyOptionHandler;

public class BasicSearcherOptions {
	@Option(name = "--index", aliases = "-i", usage = "index path", required = true, metaVar = "path")
	private File index;

	@Option(name = "--query", aliases = "-q", usage = "query image path", required = false, metaVar = "path")
	private File queryImage;

//  TODO: implement this
//	@Option(name = "--query-quantised", aliases = "-qm", usage = "query using quantised feature", required = false, metaVar = "path")
//	private File queryQuantised;
	
	@Option(name = "--interest-region", aliases = "-r", usage = "coordinates for query region-of-interest (x,y,h,w)", metaVar = "coords")
	private String roiCoordsString;
	
	@Option(name = "--limit", aliases = "-l", usage = "limit the number of results returned", metaVar = "number")
	private int limit = 0;
	
	@Option(name="--score-modifier", aliases="-sm", required=false, usage="Use specified model for re-ranking results.", handler=ProxyOptionHandler.class)
	protected ScoreModifierType scoreModifier = ScoreModifierType.NONE;
	private ScoreModifierTypeOptions scoreModifierOp = ScoreModifierType.NONE.getOptions();
	
	@Option(name="-matching-model", aliases="-mm", required=false, usage="Choose matching model",handler=ProxyOptionHandler.class)
	private MatchingModelType matchingModel = MatchingModelType.TFIDF;
	
	@Option(name = "--interactive", usage = "interactive mode")
	private boolean interactive = false;
	
	@Option(name = "--server", usage = "server mode port number", required = false, metaVar = "port")
	private int port = -1;
	
	@Option(name = "--display-results", aliases = "-dr", usage = "display results")
	private boolean displayResults = false;
	
	@Option(name = "--display-query", aliases = "-dq", usage = "display query")
	private boolean displayQuery = false;

	@Option(name = "--soft-quant-neighbours", usage = "Number of neighbours to use for soft quantisation")
	private int softQuantNeighbours = 0;
	
	@Option(name = "--time", usage = "time the feature extraction and querying")
	private boolean timeQuery = false;
	
	public File getIndex() {
		return index;
	}

	public File getQueryImage() {
		return queryImage;
	}

	public String getRoiCoordsString() {
		return roiCoordsString;
	}

	public int [] getRoiCoords() {
		if (roiCoordsString == null) return null;
		
		String [] parts = roiCoordsString.split(",");
		int [] coords = new int[4];
		
		for (int i=0; i<4; i++)
			coords[i] = Integer.parseInt(parts[i]); 
		
		return coords;
	}
	
	public int getLimit() {
		return limit;
	}
	
	public boolean isInteractive() {
		return interactive;
	}
	
	public int serverPort() {
		return port;
	}
	
	public boolean isServer() {
		return port>0;
	}
	
	public boolean displayQuery() {
		return displayQuery;
	}
	
	public boolean displayResults() {
		return displayResults;
	}
	
	public ScoreModifierTypeOptions getScoreModifierTypeOptions() {
		return scoreModifierOp;
	}
	
	public ScoreModifierType getScoreModifierType() {
		return scoreModifier;
	}
	
	public MatchingModelType getMatchingModelType() {
		return matchingModel;
	}
	
	public int getSoftQuantNeighbours() {
		return softQuantNeighbours;
	}
	
	public boolean timeQuery() {
		return timeQuery;
	}
}
