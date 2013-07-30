package org.imageterrier.bbc;

import java.io.File;
import java.io.FilenameFilter;
import java.util.Arrays;
import java.util.List;

import org.apache.log4j.Logger;
import org.imageterrier.basictools.DocidScore;
import org.imageterrier.toolopts.MatchingModelType;
import org.imageterrier.tools.multi.MultiIndexSearcher;
import org.imageterrier.tools.multi.MultiIndexSearcherOptions;
import org.openimaj.ml.clustering.SpatialClusters;
import org.openimaj.ml.clustering.assignment.Assigner;
import org.openimaj.time.Timer;
import org.openimaj.tools.localfeature.options.LocalFeatureMode;

public class BBCWorldSearcher {
	private static Logger logger;

	SpatialClusters<?> cluster;
	Assigner<byte[]> hardAssigner;
	private File DEFAULT_CLUSTER = new File("/Volumes/BBC World Search/worldsearch/codebooks/index.featurequantiser");
	private String featureType = "SIFT";
	private LocalFeatureMode mode;
	private MultiIndexSearcherOptions opts;
	private MultiIndexSearcher searcher;
	final static String BASE_DIR = "/Volumes/BBC World Search/worldsearch/indexes_720_0.00_max_5000000";

	// final static String BASE_DIR =
	// "/Users/ss/Experiments/bbc/selectiveIndexCreation/testIndecies";

	public BBCWorldSearcher() {
		prepare();
	}

	private void prepare() {
		this.opts = new MultiIndexSearcherOptions();
		opts.index = loadIndecies(BASE_DIR, "index_.*");
		opts.quantiser = DEFAULT_CLUSTER;
		opts.limit = 10;
		opts.matchingModel = MatchingModelType.TFIDF_DAAT;
		this.searcher = new MultiIndexSearcher(opts);
		searcher.loadCompletely();
	}

	List<File> loadIndecies(String baseDir, final String match) {
		final File[] indexFiles = new File(baseDir).listFiles(new FilenameFilter() {

			@Override
			public boolean accept(File dir, String name) {
				return name.matches(match);
			}

		});
		return Arrays.asList(indexFiles);
		// return Arrays.asList(
		// new
		// File("/Volumes/BBC World Search/worldsearch/indexes_merged/odd/index_merged_0_205/")
		// new
		// File("/Volumes/BBC World Search/worldsearch/indexes_merged/index_0_112_161_165_combination")
		// new
		// File("/Volumes/BBC World Search/worldsearch/indexes/index_0_112_combination"),
		// new
		// File("/Volumes/BBC World Search/worldsearch/indexes/index_115_115_combination"),
		// new
		// File("/Volumes/BBC World Search/worldsearch/indexes/index_122_123_combination"),
		// new
		// File("/Volumes/BBC World Search/worldsearch/indexes/index_129_137_combination"),
		// new
		// File("/Volumes/BBC World Search/worldsearch/indexes/index_161_165_combination")
		// );
	}

	public static void main(String[] args) throws Exception {
		final BBCWorldSearcher searcher = new BBCWorldSearcher();
		searcher.search(new File("/Users/ss/Desktop/imgres.jpeg"));
		searcher.search(new File("/Users/ss/Desktop/oh-the-huge-manatee1.jpg"));
		searcher.search(new File("/Users/ss/Desktop/imgres.jpeg"));

	}

	public void search(File imageFile) throws Exception {
		this.opts.setQueryImageFile(imageFile);
		System.out.println("Starting search for: " + imageFile);
		final Timer timer = Timer.timer();
		final List<DocidScore> rs = searcher.search(this.opts.getQueryImage(), this.opts.getRoiCoords(), opts);
		System.out.println(String.format("Search complete! Got: %d results Took: %fs", rs.size(),
				timer.duration() / 1000f));
	}
}
