package org.imageterrier.index.merge;

import java.util.Date;

import org.apache.log4j.Logger;
import org.imageterrier.basictools.BasicTerrierConfig;
import org.terrier.structures.Index;
import org.terrier.structures.merging.StructureMerger;
import org.terrier.utility.ApplicationSetup;

public class StructureMergerTool {
	static {
		// initialise terrier
		BasicTerrierConfig.configure();
	}
	private static Logger logger = Logger.getLogger(StructureMergerTool.class);
	/** Usage: java org.terrier.structures.merging.StructureMerger [binary bits] [inverted file 1] [inverted file 2] [output inverted file] <p>
     * Binary bits concerns the number of fields in use in the index. */
	public static void main(String[] args) throws Exception {

		if (args.length != 6)
		{
			logger.fatal("usage: java org.terrier.structures.merging.StructureMerger srcPath1 srcPrefix1 srcPath2 srcPrefix2 destPath1 destPrefix1 ");
			logger.fatal("Exiting ...");
			return;
		}

		Index.setIndexLoadingProfileAsRetrieval(false);
		Index indexSrc1 = Index.createIndex(args[0], args[1]);
		Index indexSrc2 = Index.createIndex(args[2], args[3]);
		Index indexDest = Index.createNewIndex(args[4], args[5]);

		StructureMerger sMerger = new StructureMerger(indexSrc1, indexSrc2, indexDest);
		long start = System.currentTimeMillis();
		logger.info("started at " + (new Date()));
		if (ApplicationSetup.getProperty("merger.onlylexicons","false").equals("true")) {
			System.err.println("Use LexiconMerger");
			return;
		} else {
			sMerger.mergeStructures();
		}
		indexSrc1.close();
		indexSrc2.close();
		indexDest.close();

		logger.info("finished at " + (new Date()));
		long end = System.currentTimeMillis();
		logger.info("time elapsed: " + ((end-start)*1.0d/1000.0d) + " sec.");
	}
}
