package org.imageterrier.dsms;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import gnu.trove.TIntArrayList;
import gnu.trove.TIntIntHashMap;
import gnu.trove.TIntObjectHashMap;

import org.apache.commons.math.stat.descriptive.SummaryStatistics;
import org.apache.log4j.Logger;
import org.imageterrier.basictools.ApplicationSetupUtils;
import org.imageterrier.locfile.PositionSpec;
import org.imageterrier.locfile.QLFDocument;
import org.imageterrier.locfile.PositionSpec.PositionSpecMode;
import org.imageterrier.querying.parser.QLFDocumentQuery;
import org.imageterrier.structures.PositionInvertedIndex;
import org.terrier.matching.MatchingQueryTerms;
import org.terrier.matching.ResultSet;
import org.terrier.matching.dsms.DocumentScoreModifier;
import org.terrier.structures.BitIndexPointer;
import org.terrier.structures.Index;
import org.terrier.structures.Lexicon;
import org.terrier.structures.LexiconEntry;
import org.terrier.utility.ApplicationSetup;

/**
 ** EXPERIMENTAL - DO NOT USE!! **
 * @author Jonathon Hare <jsh2@ecs.soton.ac.uk>
 *
 */
public class ASIFTSimIdScoreModifier implements DocumentScoreModifier {
	protected static final Logger logger = Logger.getLogger(ASIFTSimIdScoreModifier.class);
	
	@Override
	public boolean modifyScores(Index index, MatchingQueryTerms queryTerms, ResultSet resultSet) {
		if (!(index.getInvertedIndex() instanceof PositionInvertedIndex)) {
			logger.warn("Inverted index is incompatible with DSM");
			return false;
		}

		PositionInvertedIndex invidx = (PositionInvertedIndex) index.getInvertedIndex();
		PositionSpec spec = invidx.getPositionSpec();
		
		if(spec.getMode() != PositionSpecMode.AFFINE_INDEX) {
			logger.warn("PositionSpec of the index is incompatible with this DSM");
			return false;
		}
		
		Lexicon<String> lexicon = index.getLexicon();
		QLFDocumentQuery<?> query = (QLFDocumentQuery<?>) queryTerms.getQuery();
		QLFDocument<?> queryDoc = query.getDocument();
		queryDoc.reset();
		
		int [] docids = resultSet.getDocids();
		
		TIntIntHashMap docposmap = new TIntIntHashMap();
		TIntIntHashMap posdocmap = new TIntIntHashMap();
		for (int i=0; i<docids.length; i++) {
			docposmap.put(docids[i], i); 
			posdocmap.put(i,docids[i]);
		}
		
		int[] indices = {0}; //only one payload item to get
		
		double [] scores = resultSet.getScores();
		Map<String, int[]>[] simulations = new Map[scores.length];
		
		//accumulate histograms
		String queryTerm;
		while (!queryDoc.endOfDocument()) {
			queryTerm = queryDoc.getNextTerm();
			LexiconEntry le = lexicon.getLexiconEntry(queryTerm);
			if (le != null) {
				int[] queryTermPos = new int[1];
				queryTermPos[0] = spec.getPosition(queryDoc)[indices[0]];
				
				int[][][] matchDocs = invidx.getPositions((BitIndexPointer) le, docposmap, indices);

				for (int i=0; i<scores.length; i++) {
					Map<String, int[]> sim = simulations[i];
					if (sim == null) 
						sim = simulations[i] = new HashMap<String, int[]>();
					
					int [][] matchedTermPos = matchDocs[i];

					if (matchedTermPos != null) {
						int [] vals = new int[matchedTermPos.length];
						for (int j=0; j<matchedTermPos.length; j++) vals[j] = matchedTermPos[j][0];
						
						sim.put(queryTerm, vals);
					}
				}
			}
		}

		//modify scores
		for(int i=0; i<scores.length; i++) {
			scores[i] *= score(simulations[i]);
		}
		
		return true;
	}

	private double score(Map<String, int[]> map) {
		double [] counts = new double[32];
		
		double [] weights = {0.106, 0.066, 0.069, 0.069, 0.069, 0.045, 0.046, 0.048, 0.048, 0.046, 0.029, 0.029, 0.030, 0.033, 0.033, 0.030, 0.029, 0.017, 0.017, 0.016, 0.017, 0.020, 0.021, 0.020, 0.017, 0.016, 0.017, 0.000, 0.000, 0.000, 0.000, 0.000};
		
		for (String term : map.keySet()) {
			int [] matchingSims = map.get(term);
			
			if (matchingSims.length == 1) {
				for (int simIdx : matchingSims)
					counts[simIdx]++;
			}
		}
//		
//		SummaryStatistics stats = new SummaryStatistics();
//		for (int i=0; i<counts.length; i++) {
//			stats.addValue(counts[i] = weights[i] == 0 ? 0 : counts[i] / weights[i]);
//		}
//		
//		double mean = stats.getMean();
//		double stddev = stats.getStandardDeviation();
//		
//		int peaks = 0;
//		for (int i=0; i<counts.length; i++) {
//			if (counts[i] > mean + 2*stddev) peaks++;
//		}
//		
//		if (peaks == 0) return 0;
//		else return (1.0 / peaks);
		
		double [] vals = new double[27];
		double max = 0;
		for (int i=0; i<27; i++) {
			vals[i] = counts[i] ;/// weights[i];// == 0 ? 0 : (double)counts.get(i)/(double)base.get(i);

			if (vals[i] > max) max = vals[i];
		}

		int count = 0;
		for (int i=0; i<27; i++) {
			if (vals[i] > 0.8*max) count++;
		}
		if (count == 0) return 0; 
		return 1.0/count;
	}

	@Override
	public String getName() {
		return getClass().getName();
	}

	@Override
	public ASIFTSimIdScoreModifier clone() {
		return new ASIFTSimIdScoreModifier();
	}
}
