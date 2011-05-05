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
 * The Original Code is BasicIndexer.java
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
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.regex.Pattern;

import org.imageterrier.basictools.BasicTerrierConfig;
import org.imageterrier.features.FeatureTask;
import org.imageterrier.features.QuantiserTask;
import org.imageterrier.locfile.QLFFilesCollection;
import org.imageterrier.structures.indexing.QuantiserIndex;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.openimaj.feature.local.quantised.QuantisedLocalFeature;
import org.openimaj.image.feature.local.keypoints.Keypoint;
import org.openimaj.image.feature.local.keypoints.quantised.QuantisedAffineSimulationKeypoint;
import org.openimaj.image.feature.local.keypoints.quantised.QuantisedKeypoint;
import org.openimaj.io.IOUtils;
import org.openimaj.ml.clustering.Cluster;
import org.terrier.indexing.Collection;
import org.terrier.indexing.ExtensibleSinglePassIndexer;
import org.terrier.utility.ApplicationSetup;

import org.openimaj.tools.clusterquantiser.ClusterQuantiser;
import org.openimaj.tools.clusterquantiser.ClusterQuantiserOptions;
import org.openimaj.tools.clusterquantiser.ClusterType;
import org.openimaj.tools.clusterquantiser.FileType;
import org.openimaj.tools.localfeature.LocalFeatureMode;

public class BasicIndexer {
	static {
		BasicTerrierConfig.configure();
	}

	public static String [] IMAGE_EXTENSIONS = {
		".jpg",
		".jpeg",
		".png"
	};
	
	BasicIndexerOptions options = new BasicIndexerOptions();
	List<File> images = new ArrayList<File>();
	
	protected void findImages(File dir) {
		for (File f : dir.listFiles()) {
			if (f.isDirectory()) {
				findImages(f);
			} else {
				for (String ext : IMAGE_EXTENSIONS) {
					if (f.getName().endsWith(ext)) {
						images.add(f);
						break;
					}
				}
			}
		}
	}
	
	protected void run(String [] args) throws IOException, InterruptedException, ExecutionException {
	    CmdLineParser parser = new CmdLineParser(options);
		
	    try {
		    parser.parseArgument(args);
		} catch(CmdLineException e) {
		    System.err.println(e.getMessage());
		    System.err.println("Usage: java -jar ImageTerrier.jar [options...] paths_to_search");
		    parser.printUsage(System.err);
		    return;
		}
		
		//build a list of images
		if (options.isVerbose()) System.err.println("Building image list");
		for (String f : options.getSearchPaths())
			findImages(new File(f));
		
		//make executor service
		ExecutorService es = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
		
		//Extract features
		if (options.isVerbose()) System.err.println("Extracting features");
		
		List<FeatureTask> featureTasks = new ArrayList<FeatureTask>();
		for (File im : images) {
			featureTasks.add(new FeatureTask(im, options.forceRegeneration(), options.getFeatureType()));
		}
		List<Future<File>> featureFutures = es.invokeAll(featureTasks);

		//Create or load quantiser
		Cluster<?,?> cluster;
		if (options.getQuantiserFile().exists()) {
			if (options.isVerbose()) System.err.println("Loading quantiser");
			
			options.quantiserType = ClusterType.sniffClusterType(options.getQuantiserFile());
			
			if (options.quantiserType == null) throw new RuntimeException("Unknown cluster type");
			
			cluster = IOUtils.read(options.getQuantiserFile(), options.getQuantiserType().getClusterClass());
		} else {
			if (options.isVerbose()) System.err.println("Building quantiser");
			
			ClusterQuantiserOptions cqopts = new ClusterQuantiserOptions();
			
			List<File> files = new ArrayList<File>();
			for (Future<File> f : featureFutures)
				 if (f.get() != null) 
					 files.add(f.get());
			
			cqopts.setInputFiles(files);
			cqopts.setClusterType(options.getQuantiserType());
			FileType ft = options.featureType == LocalFeatureMode.ASIFTENRICHED ? FileType.ASIFTENRICHED_BINARY : FileType.BINARY_KEYPOINT; 
			cqopts.setFileType(ft);
			byte [][] data = ClusterQuantiser.do_getSamples(cqopts);
			cluster = options.getQuantiserType().create(data);
			
			IOUtils.writeBinary(options.getQuantiserFile(), cluster);
		}
		cluster.optimize(false);
		
		//Make terms and save files [equivalent to direct index]
		if (options.isVerbose()) System.err.println("Quantising features");
		
		List<QuantiserTask> quantiserTasks = new ArrayList<QuantiserTask>();
		for (Future<File> f : featureFutures) {
			if (f.get() != null) {
				quantiserTasks.add(new QuantiserTask(f.get(), options.forceRegeneration(), options.getFeatureType().getFeatureClass(), cluster));
			}
		}
		List<Future<File>> quantFutures = es.invokeAll(quantiserTasks);
		
		//shutdown executors
		es.shutdown();
		
		//optionally remove feature files?
		
		//make collection
		Class<? extends QuantisedLocalFeature<?>> qclass = options.getFeatureType().getFeatureClass() == Keypoint.class ? QuantisedKeypoint.class : QuantisedAffineSimulationKeypoint.class;
		@SuppressWarnings({ "unchecked", "rawtypes" })
		QLFFilesCollection<? extends QuantisedLocalFeature<?>> collection = new QLFFilesCollection(qclass, "\\.fv\\.loc", "");
		for (Future<File> f : quantFutures) 
			if (f.get() != null) {
				collection.addFile(f.get());
			}
		
		if (options.getSearchPaths().size() == 1) {
			collection.setPathRegex(Pattern.quote(new File(options.getSearchPaths().get(0)).getAbsolutePath()), "");
		}
		
		//configure indexing
		ApplicationSetup.setProperty("indexer.meta.forward.keys", "docno,path");
		ApplicationSetup.setProperty("indexer.meta.forward.keylens", String.format("%d,%d", collection.getMaxNumIDChars(), collection.getMaxPathChars()));
		ApplicationSetup.setProperty("indexer.meta.reverse.keys", "docno,path");
		
		//create inverted index from saved term files
		if (options.isVerbose()) System.err.println("Building index");
		
		String filename = new File(options.getFilename()).getAbsolutePath();
		new File(filename).mkdirs();
		
		ExtensibleSinglePassIndexer indexer = options.getIndexType().getIndexer(filename, "index");
		indexer.createInvertedIndex(new Collection[] {collection});
		
		indexer.getCurrentIndex().setIndexProperty("index.feature.type", options.getFeatureType().name());
		indexer.getCurrentIndex().setIndexProperty("index.lexicon.data-source", "fileinmem");
		
		if (options.getSearchPaths().size() == 1) {
			indexer.getCurrentIndex().setIndexProperty("index.image.base.path", new File(options.getSearchPaths().get(0)).getAbsolutePath());
		} else {
			indexer.getCurrentIndex().setIndexProperty("index.image.base.path", "/");
		}
		
		QuantiserIndex quantIndex = new QuantiserIndex(cluster);
		quantIndex.save(indexer.getCurrentIndex());
		indexer.getCurrentIndex().addIndexStructure("featureQuantiser", quantIndex.getClass().getName());
		
		indexer.getCurrentIndex().flush();
		
		//optionally remove term files?
	}
	
	public static void main(String [] args) throws IOException, InterruptedException, ExecutionException {
		BasicIndexer indexer = new BasicIndexer();
		indexer.run(args);
	}
}
