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
 * The Original Code is QuantiserTask.java
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
package org.imageterrier.features;

import java.io.File;
import java.util.List;
import java.util.concurrent.Callable;

import org.apache.log4j.Logger;
import org.openimaj.feature.local.LocalFeature;
import org.openimaj.feature.local.list.LocalFeatureList;
import org.openimaj.feature.local.list.MemoryLocalFeatureList;
import org.openimaj.feature.local.quantised.QuantisedLocalFeature;
import org.openimaj.io.IOUtils;
import org.openimaj.ml.clustering.SpatialClusters;
import org.openimaj.ml.clustering.assignment.HardAssigner;
import org.openimaj.ml.clustering.assignment.SoftAssigner;

public class QuantiserTask implements Callable<File> {
	protected static Logger logger = Logger.getLogger(QuantiserTask.class);

	protected boolean forceRegeneration;
	protected File featureFile;
	protected Class<? extends LocalFeature<?, ?>> featureClz;
	protected SpatialClusters<?> quantizer;
	protected HardAssigner<?, ?, ?> hardAssigner;

	public QuantiserTask(File featureFile, boolean forceRegeneration, Class<? extends LocalFeature<?, ?>> featureClz,
			SpatialClusters<?> cluster)
	{
		this.featureFile = featureFile;
		this.forceRegeneration = forceRegeneration;
		this.featureClz = featureClz;
		this.quantizer = cluster;
		this.hardAssigner = cluster.defaultHardAssigner();
	}

	public File getLocFile() {
		return makeFeatureFilename(".loc");
	}

	@Override
	public File call() throws Exception {
		return call(getLocFile());
	}

	/**
	 * Process feature and make loc file. Processed loc file will be created at
	 * saveLocation, unless one already existed alongside the image, in which
	 * case that one is returned.
	 * 
	 * @param filename
	 * @return filename
	 * @throws Exception
	 */
	public File call(File filename) throws Exception {
		if (filename.exists() && !forceRegeneration) {
			logger.info("Loading visual terms for " + featureFile.getName());
			return filename;
		}

		// load features
		final MemoryLocalFeatureList<? extends LocalFeature<?, ?>> keys = MemoryLocalFeatureList.read(featureFile,
				featureClz);

		// Quantise the sift feature
		logger.info("Generating visual terms for " + featureFile.getName());
		final LocalFeatureList<QuantisedLocalFeature<?>> keyLoc = quantiseFeatures(hardAssigner, keys);

		// Save the loc file
		IOUtils.writeBinary(filename, keyLoc);

		return filename;
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public static LocalFeatureList<QuantisedLocalFeature<?>> quantiseFeatures(HardAssigner quantizer,
			List<? extends LocalFeature> keys)
	{
		final LocalFeatureList<QuantisedLocalFeature<?>> qkeys = new MemoryLocalFeatureList<QuantisedLocalFeature<?>>(
				keys.size());

		if (quantizer.getClass().getName().contains("Byte")) {
			for (final LocalFeature k : keys) {
				final int id = ((HardAssigner<byte[], ?, ?>) quantizer).assign((byte[]) k.getFeatureVector().getVector());
				qkeys.add(new QuantisedLocalFeature(k.getLocation(), id));
			}
		} else {
			for (final LocalFeature k : keys) {
				final int id = ((HardAssigner<int[], ?, ?>) quantizer).assign((int[]) k.getFeatureVector().getVector());
				qkeys.add(new QuantisedLocalFeature(k.getLocation(), id));
			}
		}

		return qkeys;
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public static LocalFeatureList<QuantisedLocalFeature<?>> quantiseFeaturesSoft(SoftAssigner<?, ?> quantizer,
			List<? extends LocalFeature> keys)
	{
		final LocalFeatureList<QuantisedLocalFeature<?>> qkeys = new MemoryLocalFeatureList<QuantisedLocalFeature<?>>(
				keys.size());

		if (quantizer.getClass().getName().contains("Byte")) {
			for (final LocalFeature k : keys) {
				final int[] ids = ((SoftAssigner<byte[], ?>) quantizer)
						.assign(((byte[]) k.getFeatureVector().getVector()));
				for (final int id : ids)
					qkeys.add(new QuantisedLocalFeature(k.getLocation(), id));
			}
		} else {
			for (final LocalFeature k : keys) {
				final int[] ids = ((SoftAssigner<int[], ?>) quantizer).assign(((int[]) k.getFeatureVector().getVector()));
				for (final int id : ids)
					qkeys.add(new QuantisedLocalFeature(k.getLocation(), id));
			}
		}

		return qkeys;
	}

	protected File makeFeatureFilename(String extension) {
		String s = featureFile.toString();

		s = s + extension;

		return new File(s);
	}
}
