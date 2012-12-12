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
import java.io.IOException;

import org.imageterrier.toolopts.MatchingModelType;
import org.imageterrier.toolopts.ScoreModifierType;
import org.imageterrier.toolopts.ScoreModifierType.ScoreModifierTypeOptions;
import org.kohsuke.args4j.Option;
import org.kohsuke.args4j.ProxyOptionHandler;
import org.terrier.structures.Index;

public class BasicSearcherOptions {
	@Option(name = "--index", aliases = "-i", usage = "index path", required = false, metaVar = "path")
	public File index;

	@Option(name = "--query", aliases = "-q", usage = "query image path", required = false, metaVar = "path")
	public File queryImage;

	enum PrintModeOption{
		FILE {

			@Override
			public String docidToString(Index index, int docId) {
				String docIdPath;
				try {
					docIdPath = index.getMetaIndex().getItem("path", docId);
				} catch (IOException e) {
					throw new RuntimeException(e);
				}
				return index.getIndexProperty("index.image.base.path", "/") + docIdPath .replace(".fv.loc", "");
			}

		},RAW {
			@Override
			public String docidToString(Index index, int docId) {
				try {
					return index.getMetaIndex().getItem("docno", docId);
				} catch (IOException e) {
					throw new RuntimeException(e);
				}
			}
		};

		public abstract String docidToString(Index index, int docId) ;

	}
	@Option(name = "--print-mode", usage = "How documents are printed")
	PrintModeOption mode = PrintModeOption.FILE;

	@Option(name = "--interest-region", aliases = "-r", usage = "coordinates for query region-of-interest (x,y,h,w)", metaVar = "coords")
	public String roiCoordsString;

	@Option(name = "--limit", aliases = "-l", usage = "limit the number of results returned", metaVar = "number")
	public int limit = 0;

	@Option(name="--score-modifier", aliases="-sm", required=false, usage="Use specified model for re-ranking results.", handler=ProxyOptionHandler.class)
	protected ScoreModifierType scoreModifier = ScoreModifierType.NONE;
	public ScoreModifierTypeOptions scoreModifierOp = ScoreModifierType.NONE.getOptions();

	@Option(name="-matching-model", aliases="-mm", required=false, usage="Choose matching model",handler=ProxyOptionHandler.class)
	public MatchingModelType matchingModel = MatchingModelType.TFIDF;

	@Option(name = "--interactive", usage = "interactive mode")
	public boolean interactive = false;

	@Option(name = "--server", usage = "server mode port number", required = false, metaVar = "port")
	public int port = -1;

	@Option(name = "--display-results", aliases = "-dr", usage = "display results")
	public boolean displayResults = false;

	@Option(name = "--display-query", aliases = "-dq", usage = "display query")
	public boolean displayQuery = false;

	@Option(name = "--soft-quant-neighbours", usage = "Number of neighbours to use for soft quantisation")
	public int softQuantNeighbours = 0;

	@Option(name = "--time", usage = "time the feature extraction and querying")
	public boolean timeQuery = false;

	@Option(name = "--exact-assigner", usage = "Force an exact assigner for querys")
	public boolean exactAssigner = false;

	@Option(name = "--quantiser-file", usage = "The location of the quantiser to use", required=true)
	public File quantiser;

	public File getIndex() {
		return index;
	}

	public File getQueryImage() {
		return queryImage;
	}

	public void setQueryImageFile(File query) {
		this.queryImage = query;
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

	public BasicSearcherOptions newInstance(){
		return new BasicSearcherOptions();
	}

	public File getQuantiser() {
		return this.quantiser;
	}
}
