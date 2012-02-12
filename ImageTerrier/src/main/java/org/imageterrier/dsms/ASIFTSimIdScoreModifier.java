package org.imageterrier.dsms;

import gnu.trove.TIntIntHashMap;
import gnu.trove.TObjectIntHashMap;

import org.apache.log4j.Logger;
import org.imageterrier.locfile.PositionSpec;
import org.imageterrier.locfile.QLFDocument;
import org.imageterrier.locfile.PositionSpec.PositionSpecMode;
import org.imageterrier.querying.parser.QLFDocumentQuery;
import org.imageterrier.structures.PositionInvertedIndex;
import org.openimaj.feature.local.quantised.QuantisedLocalFeature;
import org.openimaj.image.feature.local.affine.AffineSimulationKeypoint.AffineSimulationKeypointLocation;
import org.terrier.matching.MatchingQueryTerms;
import org.terrier.matching.ResultSet;
import org.terrier.matching.dsms.DocumentScoreModifier;
import org.terrier.structures.BitIndexPointer;
import org.terrier.structures.Index;
import org.terrier.structures.Lexicon;
import org.terrier.structures.LexiconEntry;

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
		
		@SuppressWarnings("unchecked")
		TObjectIntHashMap<String>[] simulations = new TObjectIntHashMap[scores.length];
		
		//accumulate histograms
		for (QuantisedLocalFeature<?> queryTerm : queryDoc.getEntries()) {
			LexiconEntry le = lexicon.getLexiconEntry("loc" + queryTerm.id);
			
			if (le != null) {
				int[][][] matchDocs = invidx.getPositions((BitIndexPointer) le, docposmap, indices);

				for (int i=0; i<scores.length; i++) {
					TObjectIntHashMap<String> sim = simulations[i];
					if (sim == null) 
						sim = simulations[i] = new TObjectIntHashMap<String>();
					
					int [][] matchedTermPos = matchDocs[i];

					if (matchedTermPos != null) {
						for (int j=0; j<matchedTermPos.length; j++) {
							int docSimulationId = matchedTermPos[j][0];
							int querySimulationId = 0;
							
							if (queryTerm.location instanceof AffineSimulationKeypointLocation) {
								querySimulationId = ((AffineSimulationKeypointLocation)queryTerm.location).index;
							}
							
							sim.adjustOrPutValue(querySimulationId + ":" + docSimulationId, 1, 1);
						}
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

	private double score(TObjectIntHashMap<String> map) {
		//double [] weights = {0.106, 0.066, 0.069, 0.069, 0.069, 0.045, 0.046, 0.048, 0.048, 0.046, 0.029, 0.029, 0.030, 0.033, 0.033, 0.030, 0.029, 0.017, 0.017, 0.016, 0.017, 0.020, 0.021, 0.020, 0.017, 0.016, 0.017, 0.000, 0.000, 0.000, 0.000, 0.000};
		
		double [] vals = new double[map.size()];
		double max = 0;
		double sum = 0;
		int i = 0;
		
		for (String key : map.keys(new String[map.size()])) {
			vals[i] = map.get(key);/// weights[i];// == 0 ? 0 : (double)counts.get(i)/(double)base.get(i);
			sum += vals[i];
			if (vals[i] > max) max = vals[i];
			i++;
		}

//		int count = 0;
//		for (i=0; i<vals.length; i++) {
//			if (vals[i] > 0.8*max) count++;
//		}
//		if (count == 0) return 0; 
//		return 1.0/count;
		
		if ((double)map.get("0:0")/sum > 0.5) return 1.0;
		return 2.0;
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
