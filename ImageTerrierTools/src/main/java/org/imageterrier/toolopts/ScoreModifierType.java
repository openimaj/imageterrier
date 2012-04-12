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

import org.imageterrier.dsms.ASIFTSimIdScoreModifier;
import org.imageterrier.dsms.AbstractHistogramConsistentScore;
import org.imageterrier.dsms.AbstractRANSACGeomModifier;
import org.imageterrier.dsms.AbstractRANSACGeomModifier.ScoringScheme;
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

/**
 * Score modifiers applied to rerank after initial retrieval.
 * 
 * @author Jonathon Hare <jsh2@ecs.soton.ac.uk>
 */
public enum ScoreModifierType implements CmdLineOptionsProvider {
	/**
	 * Attempt to rescore by fitting a homography between the query and the top results
	 * 
	 * @author Jonathon Hare <jsh2@ecs.soton.ac.uk>
	 */
	HOMOGRAPHY {
		@Override
		public ScoreModifierTypeOptions getOptions() {
			return new HomographyOptions();
		}
	},
	/**
	 * Attempt to rescore by fitting a fundamental matrix between the query and the top results
	 * 
	 * @author Jonathon Hare <jsh2@ecs.soton.ac.uk>
	 */
	FUNDAMENTAL {
		@Override
		public ScoreModifierTypeOptions getOptions() {
			return new FundamentalOptions();
		}
	},
	/**
	 * Attempt to rescore by fitting a affine transform between the query and the top results
	 * 
	 * @author Jonathon Hare <jsh2@ecs.soton.ac.uk>
	 */
	AFFINE {
		@Override
		public ScoreModifierTypeOptions getOptions() {
			return new AffineOptions();
		}
		
	},
	/**
	 * Rescore by looking for evidence of a consistent local-feature orientation change
	 * 
	 * @author Jonathon Hare <jsh2@ecs.soton.ac.uk>
	 */
	CONSISTENT_ORI {
		@Override
		public ScoreModifierTypeOptions getOptions() {
			return new ConsistentOriTypeOptions();
		}
	},
	/**
	 * Rescore by looking for evidence of a consistent local-feature scale change
	 * 
	 * @author Jonathon Hare <jsh2@ecs.soton.ac.uk>
	 */
	CONSISTENT_SCALE {
		@Override
		public ScoreModifierTypeOptions getOptions() {
			return new ConsistentScaleTypeOptions();
		}
	},
	/**
	 * Rescore by looking for evidence of a consistent local-feature affine change
	 * 
	 * @author Jonathon Hare <jsh2@ecs.soton.ac.uk>
	 */
	CONSISTENT_AFFINE {
		@Override
		public ScoreModifierTypeOptions getOptions() {
			return new ConsistentAffineTypeOptions();
		}
	},
	/**
	 * Rescore by looking for evidence of a consistent local-feature affine change
	 * 
	 * @author Jonathon Hare <jsh2@ecs.soton.ac.uk>
	 */
	CONSISTENT_AFFINE_SIM {
		@Override
		public ScoreModifierTypeOptions getOptions() {
			return new BasicScoreModifierTypeOptions(ASIFTSimIdScoreModifier.class.getName(), NONE, PositionInvertedIndex.class);
		}
	},
	/**
	 * Rescore by looking for evidence of a consistent local-feature nearest neighbours
	 * 
	 * @author Jonathon Hare <jsh2@ecs.soton.ac.uk>
	 */
	NEAREST_NEIGHBOUR {
		@Override
		public ScoreModifierTypeOptions getOptions() {
			return new BasicScoreModifierTypeOptions(NNScoreModifier.class.getName(), NONE, NNInvertedIndex.class);
		}
	},
	/**
	 * No score modifier
	 * 
	 * @author Jonathon Hare <jsh2@ecs.soton.ac.uk>
	 */
	NONE {
		@Override
		public ScoreModifierTypeOptions getOptions() {
			return new BasicScoreModifierTypeOptions("", NONE, InvertedIndex.class);
		}
	}
	;
	
	@Override
	public abstract ScoreModifierTypeOptions getOptions();
	
	/**
	 * Base for all options
	 * 
	 * @author Jonathon Hare <jsh2@ecs.soton.ac.uk>
	 *
	 */
	public abstract class ScoreModifierTypeOptions {
		private Class<?> supportedIndexClasses[];
		protected ScoreModifierType type;
		
		ScoreModifierTypeOptions(ScoreModifierType type, Class<?>... supportedIndexClasses) {
			this.type = type;
			this.supportedIndexClasses = supportedIndexClasses;
		}
		
		protected abstract String getScoreModifierClass();
		
		public String getScoreModifierClass(InvertedIndex index) {
			if (checkIndexIsSupported(index)) {
				return getScoreModifierClass();
			} else {
				return "";
			}
		}
		
		protected boolean checkIndexIsSupported(InvertedIndex index) {
			for (Class<?> c : supportedIndexClasses)
				if (c.isAssignableFrom(index.getClass())) 
					return true;
				
			System.err.println("Warning: ScoreModifier " + type.name() + " disabled as it is not supported by the index structure");
			
			return false;
		}
	}
	
	/**
	 * Basic options with no setup
	 * 
	 * @author Jonathon Hare <jsh2@ecs.soton.ac.uk>
	 *
	 */
	public class BasicScoreModifierTypeOptions extends ScoreModifierTypeOptions {
		String clz;
		
		BasicScoreModifierTypeOptions(String clz, ScoreModifierType type, Class<?>... supportedIndexClasses) {
			super(type, supportedIndexClasses);
			this.clz = clz;
		}

		@Override
		protected String getScoreModifierClass() {
			return clz;
		}
	}
	
	/**
	 * Base options for ScoreModifiers that use a RANSAC fitting strategy
	 * 
	 * @author Jonathon Hare <jsh2@ecs.soton.ac.uk>
	 */
	abstract class RansacModifierTypeOptions extends ScoreModifierTypeOptions {
		@Option(name="--scoring-scheme", required=false, usage="Scoring scheme. Defaults to the number of matches.")
		ScoringScheme scoringScheme = ScoringScheme.NUM_MATCHES;
		
		@Option(name="--num-docs-rerank", required=false, usage="Number of documents to consider in geometric reranking. Defaults to 100.")
		int numDocsRerank = 100;

		@Option(name="--colinear-filter-thresh", required=false, usage="Threshold for removal of colinear matches. Setting to 0 (default) disables filter.")
		float filterThresh = 0f;
		
		@Option(name="--ransac-num-successful", required=false, usage="Number of matches required for RANSAC to succeed if > 1. Percentage matches if <= 1. Between 0 and -1 means -desired error probability.")
		float numSuccessfulMatches = 7;
		
		@Option(name="--ransac-max-niter", required=false, usage="Maximum number of RANSAC iterations.")
		int nIter = 100;
		
		@Option(name="--tolerance", required=false, usage="Tolerance in pixels that a point is allowed to move before being rejected.")
		float tolerance = 10;
		
		RansacModifierTypeOptions(ScoreModifierType type, Class<?>... supportedIndexClasses) {
			super(type, supportedIndexClasses);
		}
		
		@Override
		public final String getScoreModifierClass() {
			ApplicationSetup.setProperty(AbstractRANSACGeomModifier.SCORING_SCHEME, scoringScheme.name());
			ApplicationSetup.setProperty(HomographyScoreModifier.N_DOCS_TO_RERANK, numDocsRerank+"");
			ApplicationSetup.setProperty(HomographyScoreModifier.FILTERING_THRESHOLD, filterThresh+"");
			ApplicationSetup.setProperty(HomographyScoreModifier.RANSAC_PER_MATCHES_SUCCESS, numSuccessfulMatches+"");
			ApplicationSetup.setProperty(HomographyScoreModifier.RANSAC_MAX_ITER, nIter+"");
			ApplicationSetup.setProperty(HomographyScoreModifier.MODEL_TOLERANCE, tolerance+"");
			
			return setupAndGetScoreModifierClass();
		}

		abstract String setupAndGetScoreModifierClass();
	}
	
	/**
	 * Options for the HOMOGRAPHY score modifier
	 * 
	 * @author Jonathon Hare <jsh2@ecs.soton.ac.uk>
	 */
	public class HomographyOptions extends RansacModifierTypeOptions {
		HomographyOptions() {
			super(HOMOGRAPHY, PositionInvertedIndex.class);
		}

		@Override
		String setupAndGetScoreModifierClass() {
			return HomographyScoreModifier.class.getName();
		}
	}

	/**
	 * Options for the FUNDAMENTAL score modifier
	 * 
	 * @author Jonathon Hare <jsh2@ecs.soton.ac.uk>
	 */
	public class FundamentalOptions extends RansacModifierTypeOptions {
		FundamentalOptions() {
			super(FUNDAMENTAL, PositionInvertedIndex.class);
		}

		@Override
		String setupAndGetScoreModifierClass() {
			return FundamentalScoreModifier.class.getName();
		}
	}
	
	/**
	 * Options for the AFFINE score modifier
	 * 
	 * @author Jonathon Hare <jsh2@ecs.soton.ac.uk>
	 */
	public class AffineOptions extends RansacModifierTypeOptions {
		AffineOptions() {
			super(FUNDAMENTAL, PositionInvertedIndex.class);
		}

		@Override
		String setupAndGetScoreModifierClass() {
			return AffineScoreModifier.class.getName();
		}
	}
	
	/**
	 * Base options for the score modifiers based on histogram scoring
	 * 
	 * @author Jonathon Hare <jsh2@ecs.soton.ac.uk>
	 */
	abstract class Histogram1dTypeOptions extends ScoreModifierTypeOptions {
		@Option(name="--num-histogram-bins", aliases="-nbins", required=false, usage="number of histogram bins")
		int nbins = 36;
		
		@Option(name="--peak-mode", aliases="-pm", required=false, usage="The method a peak should be found in the histogram")
		private PeakMode peakMode = PeakMode.MAX;
		
		Histogram1dTypeOptions(ScoreModifierType type, Class<?>... supportedIndexClasses) {
			super(type, supportedIndexClasses);
		}
		
		@Override
		public String getScoreModifierClass() {
			ApplicationSetup.setProperty(AbstractHistogramConsistentScore.CONSISTENT_HISTOGRAM_BINS, nbins+"");
			ApplicationSetup.setProperty(AbstractHistogramConsistentScore.PEAK_MODE, peakMode+"");

			return setupAndGetScoreModifierClass();
		}

		abstract String setupAndGetScoreModifierClass();
	}
	
	/**
	 * Options for the CONSISTENT_ORI score modifier
	 * 
	 * @author Jonathon Hare <jsh2@ecs.soton.ac.uk>
	 */
	public class ConsistentOriTypeOptions extends Histogram1dTypeOptions {
		ConsistentOriTypeOptions() {
			super(CONSISTENT_ORI, PositionInvertedIndex.class);
		}

		@Override
		String setupAndGetScoreModifierClass() {
			return ConsistentOriScoreModifier.class.getName();
		}
	}

	/**
	 * Options for the CONSISTENT_SCALE score modifier
	 * 
	 * @author Jonathon Hare <jsh2@ecs.soton.ac.uk>
	 */
	public class ConsistentScaleTypeOptions extends Histogram1dTypeOptions {
		ConsistentScaleTypeOptions() {
			super(CONSISTENT_SCALE, PositionInvertedIndex.class);
		}

		@Override
		String setupAndGetScoreModifierClass() {
			return ConsistentScaleScoreModifier.class.getName();
		}
	}

	/**
	 * Options for the CONSISTENT_AFFINE score modifier
	 * 
	 * @author Jonathon Hare <jsh2@ecs.soton.ac.uk>
	 */
	public class ConsistentAffineTypeOptions extends ScoreModifierTypeOptions {
		@Option(name="--num-histogram-theta-bins", aliases="-nthbins", required=false, usage="number of histogram bins in theta direction")
		int nthbins = 5;
		
		@Option(name="--num-histogram-tilt-bins", aliases="-ntibins", required=false, usage="number of histogram bins in tilt direction")
		int ntibins = 5;
		
		@Option(name="--peak-mode", aliases="-pm", required=false, usage="The method a peak should be found in the histogram")
		private PeakMode peakMode = PeakMode.MAX;

		ConsistentAffineTypeOptions() {
			super(CONSISTENT_AFFINE, PositionInvertedIndex.class);
		}
		
		@Override
		public String getScoreModifierClass() {
			ApplicationSetup.setProperty(AbstractHistogramConsistentScore.CONSISTENT_HISTOGRAM_BINS, (nthbins * ntibins)+"");
			ApplicationSetup.setProperty(AbstractHistogramConsistentScore.CONSISTENT_HISTOGRAM_BINS + ".theta", nthbins+"");
			ApplicationSetup.setProperty(AbstractHistogramConsistentScore.CONSISTENT_HISTOGRAM_BINS + ".tilt", ntibins+"");
			ApplicationSetup.setProperty(AbstractHistogramConsistentScore.PEAK_MODE, peakMode+"");
			return ConsistentAffineScoreModifier.class.getName();
		}
	}
}
