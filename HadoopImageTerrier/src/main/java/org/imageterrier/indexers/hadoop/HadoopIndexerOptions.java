package org.imageterrier.indexers.hadoop;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.hadoop.fs.Path;
import org.imageterrier.toolopts.IndexType;
import org.imageterrier.toolopts.IndexType.IndexTypeOptions;
import org.imageterrier.toolopts.InputMode;
import org.imageterrier.toolopts.InputMode.InputModeOptions;
import org.kohsuke.args4j.Argument;
import org.kohsuke.args4j.Option;
import org.kohsuke.args4j.ProxyOptionHandler;
import org.openimaj.feature.local.quantised.QuantisedLocalFeature;
import org.openimaj.hadoop.sequencefile.SequenceFileUtility;
import org.openimaj.image.feature.local.keypoints.quantised.QuantisedAffineSimulationKeypoint;
import org.openimaj.image.feature.local.keypoints.quantised.QuantisedKeypoint;

/**
 * Options object for the Hadoop Indexer
 * 
 * @author Jonathon Hare <jsh2@ecs.soton.ac.uk>
 * 
 */
public class HadoopIndexerOptions {
	@Option(
			name = "--type",
			aliases = "-t",
			required = false,
			usage = "Choose index type",
			handler = ProxyOptionHandler.class)
	private IndexType indexType = IndexType.BASIC;
	private IndexTypeOptions indexTypeOp = IndexType.BASIC.getOptions();

	@Option(name = "--num-reducers", aliases = "-nr", required = false, usage = "set the number of reduce tasks")
	private int numReducers = 26;

	@Option(name = "--documentPartition", required = false, usage = "enable document-partitioned mode")
	private boolean documentPartitionMode = false;

	@Option(name = "--output", aliases = "-o", usage = "path at which to write index", required = true, metaVar = "path")
	private String outputPath;

	@Option(
			name = "--feature-class",
			aliases = "-fc",
			usage = "Java class of the quantised features",
			required = true,
			metaVar = "class")
	private String featureClass;

	@Option(name = "--mode", aliases = "-m", usage = "input mode", required = true, handler = ProxyOptionHandler.class)
	private InputMode inputMode;
	private InputModeOptions inputModeOp;

	@Option(
			name = "--multithread",
			aliases = "-j",
			usage = "enable multithreaded feature extraction with given number of threads. 0 or will use hosts number of procs. ONLY FOR IMAGES MODE.",
			required = false,
			metaVar = "threads")
	int multithread = 0;

	@Option(
			name = "--shard-per-thread",
			aliases = "-spt",
			usage = "Allow each thread to produce a shard, rather than sharing one accross threads. ONLY FOR IMAGES MODE WITH MULTITHREADING.",
			required = false)
	boolean shardPerThread = false;

	@Argument(required = true)
	private List<String> inputPaths = new ArrayList<String>();

	/**
	 * @return the indexType
	 */
	public IndexTypeOptions getIndexType() {
		return indexTypeOp;
	}

	/**
	 * @return the numReducers
	 */
	public int getNumReducers() {
		return numReducers;
	}

	/**
	 * @return the documentPartitionMode
	 */
	public boolean isDocumentPartitionMode() {
		return documentPartitionMode;
	}

	/**
	 * @return the outputPath
	 */
	public Path getOutputPath() {
		return new Path(outputPath);
	}

	/**
	 * @return the featureClass
	 */
	@SuppressWarnings("unchecked")
	public Class<? extends QuantisedLocalFeature<?>> getFeatureClass() {
		if (featureClass.equalsIgnoreCase("QuantisedKeypoint"))
			return QuantisedKeypoint.class;
		if (featureClass.equalsIgnoreCase("QuantisedAffineSimulationKeypoint"))
			return QuantisedAffineSimulationKeypoint.class;

		try {
			return (Class<? extends QuantisedLocalFeature<?>>) Class.forName(featureClass);
		} catch (final ClassNotFoundException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Get the list of paths to build the index from. The paths should be
	 * sequence files containing quantised local features or images (depending
	 * on the mode). If a directory is given, it is automatically searched for
	 * internal sequence files starting with the name "part". This allows easy
	 * coupling to features generated from previous reducer passes (i.e. from
	 * the cluster-quantiser).
	 * 
	 * @return the input paths
	 * @throws IOException
	 */
	public Path[] getInputPaths() throws IOException {
		final List<Path> allPaths = new ArrayList<Path>();

		for (final String inputPath : inputPaths) {
			final Path[] paths = SequenceFileUtility.getFilePaths(inputPath, "part");

			for (final Path path : paths)
				allPaths.add(path);
		}

		return allPaths.toArray(new Path[allPaths.size()]);
	}

	public String getOutputPathString() {
		return outputPath;
	}

	public InputMode getInputMode() {
		return inputMode;
	}

	public InputModeOptions getInputModeOptions() {
		return inputModeOp;
	}

	/**
	 * @return the multithread
	 */
	public int getMultithread() {
		if (multithread <= 0)
			return Runtime.getRuntime().availableProcessors();
		return multithread;
	}

	public void setNumReducers(int nReducers) {
		this.numReducers = nReducers;
	}
}
