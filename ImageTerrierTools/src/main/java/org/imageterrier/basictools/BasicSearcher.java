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
 * The Original Code is BasicSearcher.java
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
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;

import jline.ArgumentCompletor;
import jline.ConsoleReader;
import jline.FileNameCompletor;

import org.apache.xmlrpc.XmlRpcException;
import org.apache.xmlrpc.metadata.XmlRpcSystemImpl;
import org.apache.xmlrpc.server.PropertyHandlerMapping;
import org.apache.xmlrpc.server.XmlRpcHandlerMapping;
import org.apache.xmlrpc.webserver.ServletWebServer;
import org.apache.xmlrpc.webserver.XmlRpcServlet;
import org.imageterrier.basictools.BasicSearcherOptions.PrintModeOption;
import org.imageterrier.features.FeatureTask;
import org.imageterrier.features.QuantiserTask;
import org.imageterrier.locfile.QLFDocument;
import org.imageterrier.querying.parser.QLFDocumentQuery;
import org.imageterrier.toolopts.ScoreModifierType;
import org.imageterrier.tools.multi.AssignerLoader;
import org.imageterrier.tools.multi.AssignerLoader.AssignerLoadListener;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.openimaj.feature.local.list.LocalFeatureList;
import org.openimaj.feature.local.quantised.QuantisedLocalFeature;
import org.openimaj.image.DisplayUtilities;
import org.openimaj.image.ImageUtilities;
import org.openimaj.image.MBFImage;
import org.openimaj.math.geometry.shape.Rectangle;
import org.openimaj.ml.clustering.SpatialClusters;
import org.openimaj.ml.clustering.assignment.Assigner;
import org.openimaj.ml.clustering.assignment.HardAssigner;
import org.openimaj.ml.clustering.assignment.SoftAssigner;
import org.openimaj.tools.localfeature.LocalFeatureMode;
import org.terrier.compression.BitFileBuffered;
import org.terrier.matching.ResultSet;
import org.terrier.querying.Manager;
import org.terrier.querying.SearchRequest;
import org.terrier.structures.Index;
import org.terrier.utility.ApplicationSetup;


/**
 * The ImageTerrier BasicSearcher tool. Allows an index to be loaded and
 * searched.
 *
 * @author Jonathon Hare <jsh2@ecs.soton.ac.uk>
 *
 */
public class BasicSearcher<SEARCH_OPTIONS extends BasicSearcherOptions> {
	static {
		BasicTerrierConfig.configure();
		try {
			Field dbl = BitFileBuffered.class.getDeclaredField("DEFAULT_BUFFER_LENGTH");
			dbl.setAccessible(true);
			Field modifiersField = Field.class.getDeclaredField("modifiers");
			modifiersField.setAccessible(true);
			modifiersField.setInt(dbl, dbl.getModifiers() &~Modifier.FINAL);
			dbl.setInt(null, 1024*1024);
			System.out.println(dbl.getInt(null));
		} catch (SecurityException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (NoSuchFieldException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IllegalArgumentException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	protected Index index;
	protected SpatialClusters<?> cluster;
	private Assigner<?> hardAssigner;
	private PrintModeOption pm;
	private boolean exactAassigner;


	public BasicSearcher(SEARCH_OPTIONS options) {
		this.pm = options.mode;
		this.exactAassigner = options.exactAssigner;
		initIndexInput(options);
		prepareQuantiser(options);
	}

	public void loadCompletely() {
		while(this.hardAssigner == null || !isIndexLoadComplete()){
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
			}
		}
		if(this.hardAssigner == null || !isIndexLoadComplete()) throw new RuntimeException("Failed to load!");
	}

	public boolean isIndexLoadComplete() {
		return true;
	}

	public void initIndexInput(SEARCH_OPTIONS options) {
		final File filename = options.getIndex();
		final String filenameStr = filename.getAbsolutePath();
		index = Index.createIndex(filenameStr, "index");
	}

	public <T extends QuantisedLocalFeature<?>> List<DocidScore> search(QLFDocument<T> query, BasicSearcherOptions options) {
		return search(query,options,index);
	}
	public <T extends QuantisedLocalFeature<?>> List<DocidScore> search(QLFDocument<T> query, BasicSearcherOptions options, Index index) {
		final QLFDocumentQuery<T> q = new QLFDocumentQuery<T>(query);

		final Manager manager = new Manager(index);
		final SearchRequest request = manager.newSearchRequest("foo");
		request.setQuery(q);
		options.getMatchingModelType().configureRequest(request, q);
		ApplicationSetup.setProperty("matching.dsms",
				options.getScoreModifierTypeOptions().getScoreModifierClass(index.getInvertedIndex()));

		ApplicationSetup.setProperty("ignore.low.idf.terms", "false");

		if (options.getLimit() == 0)
			ApplicationSetup.setProperty("matching.retrieved_set_size", ""
					+ index.getCollectionStatistics().getNumberOfDocuments());
		else
			ApplicationSetup.setProperty("matching.retrieved_set_size", "" + options.getLimit());

		manager.runPreProcessing(request);
		manager.runMatching(request);
		manager.runPostProcessing(request);
		manager.runPostFilters(request);
		ResultSet rs = request.getResultSet();
		int[] docids = rs.getDocids();
		double[] scores = rs.getScores();
		List<DocidScore> ret = new ArrayList<DocidScore>();
		for (int i = 0; i < docids.length; i++) {
			DocidScore e = new DocidScore(docids[i],scores[i]);
			e.index = index;
			ret.add(e);
		}
		return ret;
	}


	public String getDocumentId(Index index, int docno) {
		try {
			return index.getMetaIndex().getItem("docno", docno);
		} catch (final IOException e) {
			e.printStackTrace();
			return "";
		}
	}

	public String getPath(Index index, int docid) throws IOException {
		return index.getMetaIndex().getItem("path", docid).replace(".fv.loc", "");
	}

	public File getFile(Index index, int docid) throws IOException {
		String docIdPath = getDocidPath(index,docid);
		return new File(index.getIndexProperty("index.image.base.path", "/") + docIdPath.replace(".fv.loc", ""));
	}

	private String getDocidPath(Index index, int docid) throws IOException {
		String item = index.getMetaIndex().getItem("path", docid);
		return item;
	}

	public void prepareQuantiser(BasicSearcherOptions options) {
		AssignerLoader loader = new AssignerLoader(options.getQuantiser(), options.getSoftQuantNeighbours(), options.exactAssigner);
		loader.addLoadListener(new AssignerLoadListener() {
			@Override
			public void loadComplete(Assigner<?> assigner) {
				BasicSearcher.this.hardAssigner = assigner;
			}
		});
		new Thread(loader).start();
	}
	public List<DocidScore> search(File imageFile, int[] coords, BasicSearcherOptions options) throws Exception {
		String featureType = getFeatureType();
		return search(imageFile,coords,options,featureType);
	}

	public String getFeatureType() {
		return index.getIndexProperty("index.feature.type", "");
	}
	public List<DocidScore> search(File imageFile, int[] coords, BasicSearcherOptions options, String featureType) throws Exception {
//		final Assigner<?> quantizer = getQuantizer(options.getSoftQuantNeighbours());
		Assigner<?> quantizer = hardAssigner;

		final long t1 = System.currentTimeMillis();


		// process the image
		final LocalFeatureMode mode = LocalFeatureMode.valueOf(featureType);
		final LocalFeatureList<?> features = FeatureTask.computeFeatures(imageFile, mode.getOptions());

		LocalFeatureList<QuantisedLocalFeature<?>> qfeatures;
		if (options.getSoftQuantNeighbours() == 0)
			qfeatures = QuantiserTask.quantiseFeatures((HardAssigner<?, ?, ?>) quantizer, features);
		else
			qfeatures = QuantiserTask.quantiseFeaturesSoft((SoftAssigner<?, ?>) quantizer, features);

		final QLFDocument<QuantisedLocalFeature<?>> d = new QLFDocument<QuantisedLocalFeature<?>>(qfeatures, "query",
				null);

		if (coords != null) {
			final Rectangle r = new Rectangle(coords[0], coords[1], coords[2], coords[3]);
			d.filter(r);
		}

		final long t2 = System.currentTimeMillis();

		List<DocidScore> ret = search(d, options);

		final long t3 = System.currentTimeMillis();

		if (options.timeQuery()) {
			System.out.println("[INFO] Feature extraction took:	" + ((t2 - t1) / 1000.0) + " secs");
			System.out.println("[INFO] Search took:				" + ((t3 - t2) / 1000.0) + " secs");
		}



		return ret;
	}

	public void printResultSet(List<DocidScore> rs, int limit) throws IOException {
		if (limit <= 0)
			limit = rs.size();

		for (DocidScore didScore: rs) {
			final String namedDoc = this.pm.docidToString(didScore.index,didScore.first);

			if (didScore.second <= 0)
				break; // filter 0 results

			System.out.format("%s\t%f\n", namedDoc, didScore.second);
		}
	}

	public void displayResults(String title, List<DocidScore> rs, int limit) throws IOException {
		if (limit <= 0)
			limit = rs.size();
		final List<File> files = new ArrayList<File>();

		for (DocidScore didScore: rs) {
			final File file = getFile(didScore.index,didScore.first);

			if (didScore.second <= 0)
				break; // filter 0 results

			files.add(file);
		}

		displayImage(title, files.toArray(new File[files.size()]));
	}

	public void displayImage(String title, File... images) {
		if (images.length == 1) {
			try {
				DisplayUtilities.display(ImageUtilities.readMBF(images[0]), title);
			} catch (final IOException e) {
				System.err.println("Unable to load " + images[0]);
			}
		} else {
			final List<MBFImage> bimages = new ArrayList<MBFImage>();
			for (int i = 0; i < images.length; i++) {
				try {
					bimages.add(ImageUtilities.readMBF(images[i]));
				} catch (final IOException e) {
					System.err.println("Unable to load " + images[i]);
				}
			}
			DisplayUtilities.display(title, bimages.toArray(new MBFImage[bimages.size()]));
		}
	}

	/**
	 * @param args
	 * @throws Exception
	 */
	public static void main(String[] args) throws Exception {
		BasicSearcherOptions options = new BasicSearcherOptions();
		final CmdLineParser parser = new CmdLineParser(options);

		try {
			parser.parseArgument(args);
		} catch (final CmdLineException e) {
			System.err.println(e.getMessage());
			System.err.println("Usage: java -jar ImageTerrier.jar [options...]");
			parser.printUsage(System.err);
			return;
		}

		BasicSearcher<BasicSearcherOptions> searcher = new BasicSearcher<BasicSearcherOptions>(options);
		searcher.loadCompletely();

		if (!options.isInteractive() && !options.isServer()) {
			if (options.getQueryImage() == null) {
				System.err.println("Error: No query image provided.");
				System.err.println("Usage: java -jar ImageTerrier.jar [options...]");
				parser.printUsage(System.err);
				return;
			}

			if (options.getQueryImage().isDirectory()) {
				for (final File f : options.getQueryImage().listFiles()) {
					try {
						final List<DocidScore> rs = searcher.search(f, options.getRoiCoords(), options);
						System.out.println("Results from querying with: " + f.getName());
						searcher.printResultSet(rs, options.getLimit());
					} catch (final Exception e) {
						// ignore it
					}
				}
			} else {
				if (options.displayQuery())
					searcher.displayImage("Query:  " + options.getQueryImage(), options.getQueryImage());

				final List<DocidScore> rs = searcher.search(options.getQueryImage(), options.getRoiCoords(), options);
				searcher.printResultSet(rs, options.getLimit());

				if (options.displayResults())
					searcher.displayResults("Results:  " + options.getQueryImage(), rs, options.getLimit());
			}
		} else if (options.isServer()) {
			BasicSearcherXmlRpcServlet.options = options;
			BasicSearcherXmlRpcServlet.searcher = searcher;
			final XmlRpcServlet servlet = new XmlRpcServlet() {
				private static final long serialVersionUID = 1L;

				@Override
				protected XmlRpcHandlerMapping newXmlRpcHandlerMapping() throws XmlRpcException {
					final PropertyHandlerMapping mapping = (PropertyHandlerMapping) super.newXmlRpcHandlerMapping();
					XmlRpcSystemImpl.addSystemHandler(mapping);
					return mapping;
				}
			};
			final ServletWebServer webServer = new ServletWebServer(servlet, options.serverPort());
			webServer.start();
		} else {
			// interactive mode -- useful for debugging
			// Note to self: jline is awesome!
			final ConsoleReader cr = new ConsoleReader();
			final ArgumentCompletor ac = new ArgumentCompletor(new FileNameCompletor());
			ac.setStrict(false);
			cr.addCompletor(ac);
			while (true) {
				String cmd = cr.readLine("query> ").trim();

				if (cmd.length() == 0)
					continue;

				if (cmd.equals("exit") || cmd.equals("quit"))
					break;

				if (!cmd.contains("-i "))
					cmd += " -i " + options.getIndex();
				final BasicSearcherOptions interactiveOptions = new BasicSearcherOptions();
				final CmdLineParser interactiveParser = new CmdLineParser(interactiveOptions);

				try {
					interactiveParser.parseArgument(cmd.split("\\s+"));

					if (interactiveOptions.getQueryImage() == null) {
						System.out.println("Error: No query image specified.");
						continue;
					}

					if (!interactiveOptions.getIndex().equals(options.getIndex())) {
						// reload index
						searcher = new BasicSearcher<BasicSearcherOptions>(interactiveOptions);
						options = interactiveOptions;
					}

					// print a description of the query
					System.out.print("SELECT IMG_FILE, SCORE FROM " + interactiveOptions.getIndex() + " WHERE IMG");
					if (interactiveOptions.getScoreModifierType() != ScoreModifierType.NONE) {
						System.out.print(" CONTAINS ");
					} else {
						System.out.print(" IS_SIMILAR_TO ");
					}
					System.out.print(interactiveOptions.getQueryImage());
					if (interactiveOptions.getRoiCoordsString() != null)
						System.out.print("(" + interactiveOptions.getRoiCoordsString() + ")");
					if (interactiveOptions.getLimit() != 0) {
						System.out.print(" LIMIT " + interactiveOptions.getLimit());
					} else if (options.getLimit() != 0) {
						System.out.print(" LIMIT " + options.getLimit());
					}
					System.out.println();

					int limit = options.getLimit();
					if (interactiveOptions.getLimit() != 0)
						limit = interactiveOptions.getLimit();

					if (interactiveOptions.displayQuery())
						searcher.displayImage("Query:  " + interactiveOptions.getQueryImage(),
								interactiveOptions.getQueryImage());

					final List<DocidScore> rs = searcher.search(interactiveOptions.getQueryImage(),
							interactiveOptions.getRoiCoords(), interactiveOptions);
					searcher.printResultSet(rs, limit);

					if (interactiveOptions.displayResults())
						searcher.displayResults("Results:  " + interactiveOptions.getQueryImage(), rs, limit);
				} catch (final CmdLineException cle) {
					System.out.println("Syntax error:" + cle.getMessage());
				} catch (final Exception e) {
					e.printStackTrace();
					System.out.println("Error: " + e.getMessage());
				}
			}
		}
	}
}
