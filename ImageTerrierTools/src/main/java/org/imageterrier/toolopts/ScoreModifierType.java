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
 * The Original Code is ScoreModifierType.java
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
package org.imageterrier.toolopts;

import org.imageterrier.dsms.AbstractHistogramConsistentScore;
import org.imageterrier.dsms.AffineScoreModifier;
import org.imageterrier.dsms.ConsistentAffineScoreModifier;
import org.imageterrier.dsms.ConsistentOriScoreModifier;
import org.imageterrier.dsms.ConsistentScaleScoreModifier;
import org.imageterrier.dsms.FundamentalScoreModifier;
import org.imageterrier.dsms.HomographyScoreModifier;
import org.imageterrier.dsms.NNScoreModifier;
import org.imageterrier.dsms.PeakMode;
import org.imageterrier.structures.NNInvertedIndex;
import org.imageterrier.structures.PositionInvertedIndex;
import org.kohsuke.args4j.CmdLineOptionsProvider;
import org.kohsuke.args4j.Option;
import org.terrier.structures.InvertedIndex;
import org.terrier.utility.ApplicationSetup;


public enum ScoreModifierType implements CmdLineOptionsProvider {
	HOMOGRAPHY(PositionInvertedIndex.class) {
		@Option(name="--num-docs-rerank", required=false, usage="Number of documents to consider in geometric reranking.")
		int numDocsRerank = 0;

		@Option(name="--colinear-filter-thresh", required=false, usage="Threshold for removal of colinear matches. Setting to 0 disables filter.")
		float filterThresh = 0.5f;
		
		@Option(name="--ransac-num-successful", required=false, usage="Number of matches required for RANSAC to succeed if > 1. Percentage matches if <= 1. Between 0 and -1 means -desired error probability.")
		float numSuccessfulMatches = 7;
		
		@Option(name="--ransac-max-niter", required=false, usage="Maximum number of RANSAC iterations.")
		int nIter = 100;
		
		@Option(name="--tolerance", required=false, usage="Tolerance in pixels that a point is allowed to move before being rejected.")
		float tolerance = 10;
		
		@Override
		public String getScoreModifierClass() {
			ApplicationSetup.setProperty(HomographyScoreModifier.N_DOCS_TO_RERANK, numDocsRerank+"");
			ApplicationSetup.setProperty(HomographyScoreModifier.FILTERING_THRESHOLD, filterThresh+"");
			ApplicationSetup.setProperty(HomographyScoreModifier.RANSAC_PER_MATCHES_SUCCESS, numSuccessfulMatches+"");
			ApplicationSetup.setProperty(HomographyScoreModifier.RANSAC_MAX_ITER, nIter+"");
			
			ApplicationSetup.setProperty(HomographyScoreModifier.MODEL_TOLERANCE, tolerance+"");
			
			return HomographyScoreModifier.class.getName();
		}
	},
	FUNDAMENTAL(PositionInvertedIndex.class) {
		@Option(name="--num-docs-rerank", required=false, usage="Number of documents to consider in geometric reranking.")
		int numDocsRerank = 0;

		@Option(name="--colinear-filter-thresh", required=false, usage="Threshold for removal of colinear matches. Setting to 0 disables filter.")
		float filterThresh = 0.5f;
		
		@Option(name="--ransac-num-successful", required=false, usage="Number of matches required for RANSAC to succeed if > 1. Percentage matches if <= 1. Between 0 and -1 means -desired error probability.")
		float numSuccessfulMatches = 7;
		
		@Option(name="--ransac-max-niter", required=false, usage="Maximum number of RANSAC iterations.")
		int nIter = 100;
		
		@Option(name="--tolerance", required=false, usage="Tolerance in the difference of the value of y' * F * x  from 0")
		float tolerance = 0.1f;
		
		@Override
		public String getScoreModifierClass() {
			ApplicationSetup.setProperty(FundamentalScoreModifier.N_DOCS_TO_RERANK, numDocsRerank+"");
			ApplicationSetup.setProperty(FundamentalScoreModifier.FILTERING_THRESHOLD, filterThresh+"");
			ApplicationSetup.setProperty(FundamentalScoreModifier.RANSAC_PER_MATCHES_SUCCESS, numSuccessfulMatches+"");
			ApplicationSetup.setProperty(FundamentalScoreModifier.RANSAC_MAX_ITER, nIter+"");
			
			ApplicationSetup.setProperty(FundamentalScoreModifier.MODEL_TOLERANCE, tolerance+"");
			
			return FundamentalScoreModifier.class.getName();
		}
	},
	AFFINE(PositionInvertedIndex.class) {
		@Option(name="--num-docs-rerank", required=false, usage="Number of documents to consider in geometric reranking.")
		int numDocsRerank = 0;

		@Option(name="--colinear-filter-thresh", required=false, usage="Threshold for removal of colinear matches. Setting to 0 disables filter.")
		float filterThresh = 0.5f;
		
		@Option(name="--ransac-num-successful", required=false, usage="Number of matches required for RANSAC to succeed if > 1. Percentage matches if <= 1. Between 0 and -1 means -desired error probability.")
		float numSuccessfulMatches = 7;
		
		@Option(name="--ransac-max-niter", required=false, usage="Maximum number of RANSAC iterations.")
		int nIter = 100;
		
		@Option(name="--tolerance", required=false, usage="Tolerance in pixels that a point is allowed to move before being rejected.")
		float tolerance = 10;
		
		@Override
		public String getScoreModifierClass() {
			ApplicationSetup.setProperty(AffineScoreModifier.N_DOCS_TO_RERANK, numDocsRerank+"");
			ApplicationSetup.setProperty(AffineScoreModifier.FILTERING_THRESHOLD, filterThresh+"");
			ApplicationSetup.setProperty(AffineScoreModifier.RANSAC_PER_MATCHES_SUCCESS, numSuccessfulMatches+"");
			ApplicationSetup.setProperty(AffineScoreModifier.RANSAC_MAX_ITER, nIter+"");
			
			ApplicationSetup.setProperty(AffineScoreModifier.MODEL_TOLERANCE, tolerance+"");
			
			return AffineScoreModifier.class.getName();
		}
	},
	CONSISTENT_ORI(PositionInvertedIndex.class) {
		@Option(name="--num-histogram-bins", aliases="-nbins", required=false, usage="number of histogram bins")
		int nbins = 36;
		@Option(name="--peak-mode", aliases="-pm", required=false, usage="The method a peak should be found in the histogram")
		private PeakMode peakMode = PeakMode.MAX;
		
		@Override
		public String getScoreModifierClass() {
			ApplicationSetup.setProperty(AbstractHistogramConsistentScore.CONSISTENT_HISTOGRAM_BINS, nbins+"");
			ApplicationSetup.setProperty(AbstractHistogramConsistentScore.PEAK_MODE, peakMode+"");

			return ConsistentOriScoreModifier.class.getName();
		}
	},
	CONSISTENT_SCALE(PositionInvertedIndex.class) {
		@Option(name="--num-histogram-bins", aliases="-nbins", required=false, usage="number of histogram bins")
		int nbins = 10;
		@Option(name="--peak-mode", aliases="-pm", required=false, usage="The method a peak should be found in the histogram")
		private PeakMode peakMode = PeakMode.MAX;

		@Override
		public String getScoreModifierClass() {
			ApplicationSetup.setProperty(AbstractHistogramConsistentScore.CONSISTENT_HISTOGRAM_BINS, nbins+"");
			ApplicationSetup.setProperty(AbstractHistogramConsistentScore.PEAK_MODE, peakMode+"");
			return ConsistentScaleScoreModifier.class.getName();
		}
	},
	CONSISTENT_AFFINE(PositionInvertedIndex.class) {
		@Option(name="--num-histogram-theta-bins", aliases="-nthbins", required=false, usage="number of histogram bins in theta direction")
		int nthbins = 5;
		@Option(name="--num-histogram-tilt-bins", aliases="-ntibins", required=false, usage="number of histogram bins in tilt direction")
		int ntibins = 5;
		@Option(name="--peak-mode", aliases="-pm", required=false, usage="The method a peak should be found in the histogram")
		private PeakMode peakMode = PeakMode.MAX;
		
		@Override
		public String getScoreModifierClass() {
			ApplicationSetup.setProperty(AbstractHistogramConsistentScore.CONSISTENT_HISTOGRAM_BINS, (nthbins * ntibins)+"");
			ApplicationSetup.setProperty(AbstractHistogramConsistentScore.CONSISTENT_HISTOGRAM_BINS + ".theta", nthbins+"");
			ApplicationSetup.setProperty(AbstractHistogramConsistentScore.CONSISTENT_HISTOGRAM_BINS + ".tilt", ntibins+"");
			ApplicationSetup.setProperty(AbstractHistogramConsistentScore.PEAK_MODE, peakMode+"");
			return ConsistentAffineScoreModifier.class.getName();
		}
	},
	NEAREST_NEIGHBOUR(NNInvertedIndex.class) {
		@Override
		protected String getScoreModifierClass() {
			return NNScoreModifier.class.getName();
		}
	},
	NONE(InvertedIndex.class) {
		@Override
		public String getScoreModifierClass() {
			return "";
		}
	}
	;

	private Class<?> supportedIndexClasses[];
	
	ScoreModifierType(Class<?>... supportedIndexClasses) {
		this.supportedIndexClasses = supportedIndexClasses;
	}
	
	protected abstract String getScoreModifierClass();
	
	protected boolean checkIndexIsSupported(InvertedIndex index) {
		for (Class<?> c : supportedIndexClasses)
			if (c.isAssignableFrom(index.getClass())) 
				return true;
			
		System.err.println("Warning: ScoreModifier " + this.name() + " disabled as it is not supported by the index structure");
		
		return false;
	}
	
	public String getScoreModifierClass(InvertedIndex index) {
		if (checkIndexIsSupported(index)) {
			return getScoreModifierClass();
		} else {
			return "";
		}
	}
	
	@Override
	public Object getOptions() {
		return this;
	}
}
