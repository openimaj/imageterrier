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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.regex.Pattern;

import org.imageterrier.features.FeatureTask;
import org.imageterrier.features.QuantiserTask;
import org.imageterrier.locfile.QLFFilesCollection;
import org.imageterrier.structures.indexing.QuantiserIndex;
import org.imageterrier.toolopts.InputMode.ImagesModeOptions;
import org.imageterrier.toolopts.InputMode.QFModeOptions;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.openimaj.feature.local.quantised.QuantisedLocalFeature;
import org.openimaj.image.feature.local.keypoints.Keypoint;
import org.openimaj.image.feature.local.keypoints.quantised.QuantisedAffineSimulationKeypoint;
import org.openimaj.image.feature.local.keypoints.quantised.QuantisedKeypoint;
import org.openimaj.io.IOUtils;
import org.openimaj.ml.clustering.SpatialClusters;
import org.openimaj.tools.clusterquantiser.ClusterQuantiser;
import org.openimaj.tools.clusterquantiser.ClusterQuantiserOptions;
import org.openimaj.tools.clusterquantiser.ClusterType;
import org.openimaj.tools.clusterquantiser.FileType;
import org.openimaj.tools.localfeature.options.LocalFeatureMode;
import org.terrier.indexing.Collection;
import org.terrier.indexing.ExtensibleSinglePassIndexer;
import org.terrier.utility.ApplicationSetup;

public class BasicIndexer {
	static {
		BasicTerrierConfig.configure();
	}

	public static String[] IMAGE_EXTENSIONS = {
			".jpg",
			".jpeg",
			".png"
	};

	BasicIndexerOptions options = new BasicIndexerOptions();
	List<File> images = new ArrayList<File>();

	protected void findImages(File dir) {
		for (final File f : dir.listFiles()) {
			if (f.isDirectory()) {
				findImages(f);
			} else {
				for (final String ext : IMAGE_EXTENSIONS) {
					if (f.getName().endsWith(ext)) {
						images.add(f);
						break;
					}
				}
			}
		}
	}

	protected void findFiles(File dir, String ext) {
		for (final File f : dir.listFiles()) {
			if (f.isDirectory()) {
				findFiles(f, ext);
			} else {
				if (f.getName().endsWith(ext)) {
					images.add(f);
				}
			}
		}
	}

	class ProcessData {
		QLFFilesCollection<? extends QuantisedLocalFeature<?>> collection;
		SpatialClusters<?> cluster;

		public ProcessData(QLFFilesCollection<? extends QuantisedLocalFeature<?>> collection, SpatialClusters<?> cluster)
		{
			this.collection = collection;
			this.cluster = cluster;
		}
	}

	protected ProcessData processFiles(BasicIndexerOptions toolOpts) throws IOException {
		final QFModeOptions options = (QFModeOptions) toolOpts.getInputModeOptions();

		// build a list of images
		if (toolOpts.isVerbose())
			System.err.println("Building file list");
		for (final String f : toolOpts.getSearchPaths())
			findFiles(new File(f), options.getFileExtension());

		if (toolOpts.isVerbose())
			System.err.println("Found " + images.size() + " files");

		final Class<? extends QuantisedLocalFeature<?>> qclass = options.getFeatureType().getFeatureClass() == Keypoint.class ? QuantisedKeypoint.class
				: QuantisedAffineSimulationKeypoint.class;
		@SuppressWarnings({ "unchecked", "rawtypes" })
		final QLFFilesCollection<? extends QuantisedLocalFeature<?>> collection = new QLFFilesCollection(qclass, options
				.getFileExtension().replace(".", "\\."), "");

		for (final File f : images) {
			collection.addFile(f);
		}

		if (toolOpts.isVerbose())
			System.err.println("Loading quantiser");
		options.quantiserTypeOp = ClusterType.sniffClusterType(new File(options.getQuantiserFile()));

		SpatialClusters<?> cluster = null;
		if (options.quantiserTypeOp != null)
			cluster = IOUtils.read(new File(options.getQuantiserFile()), options.getQuantiserType().getClusterClass());

		return new ProcessData(collection, cluster);
	}

	protected ProcessData processImages(BasicIndexerOptions toolOpts) throws Exception {
		final ImagesModeOptions options = (ImagesModeOptions) toolOpts.getInputModeOptions();

		// build a list of images
		if (toolOpts.isVerbose())
			System.err.println("Building image list");
		for (final String f : toolOpts.getSearchPaths())
			findImages(new File(f));

		// make executor service
		final ExecutorService es = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());

		// Extract features
		if (toolOpts.isVerbose())
			System.err.println("Extracting features");

		final List<FeatureTask> featureTasks = new ArrayList<FeatureTask>();
		for (final File im : images) {
			featureTasks.add(new FeatureTask(im, options.forceRegeneration(), options.getFeatureType()));
		}
		final List<Future<File>> featureFutures = es.invokeAll(featureTasks);

		// Create or load quantiser
		SpatialClusters<?> cluster;
		if (options.getQuantiserFile() != null && new File(options.getQuantiserFile()).exists()) {
			if (toolOpts.isVerbose())
				System.err.println("Loading quantiser");

			options.quantiserTypeOp = ClusterType.sniffClusterType(new File(options.getQuantiserFile()));

			if (options.quantiserTypeOp == null)
				throw new RuntimeException("Unknown cluster type");

			cluster = IOUtils.read(new File(options.getQuantiserFile()), options.getQuantiserType().getClusterClass());
		} else {
			if (toolOpts.isVerbose())
				System.err.println("Building quantiser");

			final ClusterQuantiserOptions cqopts = new ClusterQuantiserOptions();

			final List<File> files = new ArrayList<File>();
			for (final Future<File> f : featureFutures)
				if (f.get() != null)
					files.add(f.get());

			cqopts.setInputFiles(files);
			cqopts.setClusterTypeOp(options.getQuantiserType());
			final FileType ft = options.featureTypeOp.getMode() == LocalFeatureMode.ASIFTENRICHED ? FileType.ASIFTENRICHED_BINARY
					: FileType.BINARY_KEYPOINT;
			cqopts.setFileType(ft);
			final byte[][] data = ClusterQuantiser.do_getSamples(cqopts);
			cluster = options.getQuantiserType().create(data);

			IOUtils.writeBinary(new File(options.getQuantiserFile()), cluster);
		}

		// Make terms and save files [equivalent to direct index]
		if (toolOpts.isVerbose())
			System.err.println("Quantising features");

		final List<QuantiserTask> quantiserTasks = new ArrayList<QuantiserTask>();
		for (final Future<File> f : featureFutures) {
			if (f.get() != null) {
				quantiserTasks.add(new QuantiserTask(f.get(), options.forceRegeneration(), options.getFeatureType()
						.getFeatureClass(), cluster));
			}
		}
		final List<Future<File>> quantFutures = es.invokeAll(quantiserTasks);

		// shutdown executors
		es.shutdown();

		final Class<? extends QuantisedLocalFeature<?>> qclass = options.getFeatureType().getFeatureClass() == Keypoint.class ? QuantisedKeypoint.class
				: QuantisedAffineSimulationKeypoint.class;
		@SuppressWarnings({ "unchecked", "rawtypes" })
		final QLFFilesCollection<? extends QuantisedLocalFeature<?>> collection = new QLFFilesCollection(qclass,
				"\\.fv\\.loc", "");
		for (final Future<File> f : quantFutures) {
			if (f.get() != null) {
				collection.addFile(f.get());
			}
		}

		return new ProcessData(collection, cluster);
	}

	protected void run(String[] args) throws Exception {
		final CmdLineParser parser = new CmdLineParser(options);

		try {
			parser.parseArgument(args);
		} catch (final CmdLineException e) {
			System.err.println(e.getMessage());
			System.err.println("Usage: java -jar ImageTerrier.jar [options...] paths_to_search");
			parser.printUsage(System.err);
			return;
		}

		ProcessData data;
		switch (options.getInputMode()) {
		case QUANTISED_FEATURES:
			data = processFiles(options);
			break;
		case IMAGES:
			data = processImages(options);
			break;
		default:
			throw new UnsupportedOperationException("InputMode " + options.getInputMode() + " is not supported");
		}

		if (options.getSearchPaths().size() == 1) {
			data.collection.setPathRegex(Pattern.quote(new File(options.getSearchPaths().get(0)).getAbsolutePath()), "");
		}

		// configure indexing
		ApplicationSetup.setProperty("indexer.meta.forward.keys", "docno,path");
		ApplicationSetup.setProperty("indexer.meta.forward.keylens",
				String.format("%d,%d", data.collection.getMaxNumIDChars(), data.collection.getMaxPathChars()));
		ApplicationSetup.setProperty("indexer.meta.reverse.keys", "docno,path");

		// create inverted index from saved term files
		if (options.isVerbose())
			System.err.println("Building index");

		final String filename = new File(options.getFilename()).getAbsolutePath();
		new File(filename).mkdirs();

		final ExtensibleSinglePassIndexer indexer = options.getIndexType().getIndexer(filename, "index");
		indexer.createInvertedIndex(new Collection[] { data.collection });

		indexer.getCurrentIndex().setIndexProperty("index.feature.type",
				options.getInputModeOptions().getFeatureType().name());
		indexer.getCurrentIndex().setIndexProperty("index.lexicon.data-source", "fileinmem");

		if (options.getSearchPaths().size() == 1) {
			indexer.getCurrentIndex().setIndexProperty("index.image.base.path",
					new File(options.getSearchPaths().get(0)).getAbsolutePath());
		} else {
			indexer.getCurrentIndex().setIndexProperty("index.image.base.path", "/");
		}

		if (data.cluster != null) {
			final QuantiserIndex quantIndex = new QuantiserIndex(data.cluster);
			quantIndex.save(indexer.getCurrentIndex());
			indexer.getCurrentIndex().addIndexStructure("featureQuantiser", quantIndex.getClass().getName());
		}

		indexer.getCurrentIndex().flush();

		// optionally remove term files?
	}

	public static void main(String[] args) throws Exception {
		final BasicIndexer indexer = new BasicIndexer();
		indexer.run(args);
	}
}
