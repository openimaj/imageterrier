package org.imageterrier.indexers.hadoop;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.hadoop.fs.Path;
import org.imageterrier.toolopts.IndexType;
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
	@Option(name="--type", aliases="-t", required=false, usage="Choose index type", handler=ProxyOptionHandler.class)
	private IndexType indexType = IndexType.BASIC;
	
	@Option(name="--num-reducers", aliases="-nr", required=false, usage="set the number of reduce tasks")
	private int numReducers = 26;
	
	@Option(name="--documentPartition", aliases="-p", required=false, usage="enable document-partitioned mode")
	private boolean documentPartitionMode = false;
	
	@Option(name = "--output", aliases = "-o", usage = "path at which to write index", required = true, metaVar = "path")
	private String outputPath;
	
	@Option(name = "--feature-class", aliases = "-fc", usage = "Java class of the quantised features", required = true, metaVar = "class")
	private String featureClass;
	
	@Argument(required = true)
	private List<String> inputPaths = new ArrayList<String>();

	/**
	 * @return the indexType
	 */
	public IndexType getIndexType() {
		return indexType;
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
	 * @throws ClassNotFoundException 
	 */
	@SuppressWarnings("unchecked")
	public Class<? extends QuantisedLocalFeature<?>> getFeatureClass() {
		if (featureClass.equalsIgnoreCase("QuantisedKeypoint")) return QuantisedKeypoint.class;
		if (featureClass.equalsIgnoreCase("QuantisedAffineSimulationKeypoint")) return QuantisedAffineSimulationKeypoint.class;
		
		try {
			return (Class<? extends QuantisedLocalFeature<?>>) Class.forName(featureClass);
		} catch (ClassNotFoundException e) {
			throw new RuntimeException(e);
		}
	}
	
	
	/**
	 * Get the list of paths to build the index from. The paths
	 * should be sequence files containing quantised local features
	 * of the specified type. If a directory is given, it is automatically
	 * searched for internal sequence files starting with the name "part".
	 * This allows easy coupling to features generated from previous reducer
	 * passes (i.e. from the cluster-quantiser).
	 * 
	 * @return
	 * @throws IOException
	 */
	public Path[] getInputPaths() throws IOException {
		List<Path> allPaths = new ArrayList<Path>();
		
		for (String inputPath : inputPaths) {
			Path[] paths = SequenceFileUtility.getFilePaths(inputPath, "part");
			
			for (Path path : paths)
				allPaths.add(path);
		}
		
		return allPaths.toArray(new Path[allPaths.size()]);
	}

	public String getOutputPathString() {
		return outputPath;
	}
}
