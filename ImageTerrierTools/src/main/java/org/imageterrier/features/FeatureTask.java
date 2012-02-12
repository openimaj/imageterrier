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
 * The Original Code is FeatureTask.java
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
import java.io.FileInputStream;
import java.io.IOException;
import java.util.concurrent.Callable;

import org.apache.log4j.Logger;
import org.openimaj.feature.local.list.LocalFeatureList;
import org.openimaj.io.IOUtils;
import org.openimaj.tools.localfeature.LocalFeatureMode;


public class FeatureTask implements Callable<File> {
	protected static Logger logger = Logger.getLogger(FeatureTask.class);

	protected boolean forceRegeneration;
	protected File imageFile;
	protected LocalFeatureMode mode;

	public FeatureTask(File imageFile, boolean forceRegeneration, LocalFeatureMode mode) {
		this.imageFile = imageFile;
		this.forceRegeneration = forceRegeneration;
		this.mode = mode;
	}

	public File getFeatureFile() {
		return makeFeatureFilename(".fv");
	}

	@Override
	public File call() throws Exception {
		return call(getFeatureFile());
	}

	/**
	 * Process image and make loc file.
	 * Processed loc file will be created at saveLocation, 
	 * unless one already existed alongside the image, in which case that one is returned.
	 * @param filename
	 * @return filename
	 * @throws Exception
	 */
	public File call(File filename) throws Exception {
		if (filename.exists() && !forceRegeneration) {
			logger.info("Loading visual terms for " + imageFile.getName());
			return filename;
		}

		//Get the keypoints
		try {
			logger.info("Generating keypoints for " + imageFile.getName());
			LocalFeatureList<?> keys = mode.getKeypointList(loadImageData(imageFile));
			IOUtils.writeBinary(filename, keys);
		} catch (Exception e) {
			System.err.println("Error processing image " + imageFile);
			return null;
		}

		return filename;
	}

	public static LocalFeatureList<?> computeFeatures(File imageFile, LocalFeatureMode mode) throws IOException {
		return mode.getKeypointList(loadImageData(imageFile));
	}
	
	protected static byte[] loadImageData(File imageFile) throws IOException {
		if (imageFile.isDirectory())
			throw new RuntimeException("Unsupported operation, file " + imageFile.getAbsolutePath() + " is a directory");
		if (imageFile.length() > Integer.MAX_VALUE)
			throw new RuntimeException("Unsupported operation, file " + imageFile.getAbsolutePath() + " is too big");

		FileInputStream in = null;
		final byte buffer[] = new byte[(int) imageFile.length()];
		try {
			in = new FileInputStream(imageFile);
			in.read(buffer);
		} catch (Exception e) {
			throw new RuntimeException("Exception occured on reading file " + imageFile.getAbsolutePath(), e);
		} finally {
			if (in != null) {
				try {
					in.close();
				} catch (Exception e) {
					throw new RuntimeException( "Exception occured on closing file"  + imageFile.getAbsolutePath(), e);
				}
			}
		}
		return buffer;
	}

	protected File makeFeatureFilename(String extension) {
		String s = imageFile.toString();

		s = s + extension;

		return new File(s);
	}
}
