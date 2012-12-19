package org.imageterrier.tools.multi;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

import jline.ArgumentCompletor;
import jline.ConsoleReader;
import jline.FileNameCompletor;

import org.imageterrier.basictools.BasicSearcher;
import org.imageterrier.basictools.BasicSearcherOptions;
import org.imageterrier.basictools.DocidScore;
import org.imageterrier.locfile.QLFDocument;
import org.imageterrier.toolopts.ScoreModifierType;
import org.imageterrier.tools.multi.IndexLoader.IndexLoadCompleteListener;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.openimaj.feature.local.quantised.QuantisedLocalFeature;
import org.openimaj.util.parallel.GlobalExecutorPool;
import org.openimaj.util.parallel.Operation;
import org.openimaj.util.parallel.Parallel;
import org.terrier.structures.Index;


public class MultiIndexSearcher extends BasicSearcher<MultiIndexSearcherOptions>{

	private List<Index> indexes;
	private IndexLoader indexLoader;

	public MultiIndexSearcher(MultiIndexSearcherOptions options) {
		super(options);
	}

	@Override
	public void initIndexInput(MultiIndexSearcherOptions options) {
		this.indexLoader = new IndexLoader(options.index);
		this.indexLoader.addLoadCompleteListener(new IndexLoadCompleteListener(){

			@Override
			public void indexesLoaded(List<Index> indexes) {
				MultiIndexSearcher.this.indexes = indexes;
			}

		});
		new Thread(indexLoader).start();
	}

	@Override
	public boolean isIndexLoadComplete() {
		return this.indexes != null;
	}

	@Override
	public <T extends QuantisedLocalFeature<?>> List<DocidScore> search(final QLFDocument<T> query, final BasicSearcherOptions options){
		final List<DocidScore> finalRS = Collections.synchronizedList( new ArrayList<DocidScore>());
//		for (Index index : indexes) {
		Parallel.forEach(indexes, new Operation<Index>() {
			@Override
			public void perform(Index index) {
				System.out.println("Searching with index: " + new File(index.getPath()).getName());
				List<DocidScore> rs = search(cloneQuery(query), options, index);
				finalRS.addAll(rs);
			}
		}, (ThreadPoolExecutor)Executors.newFixedThreadPool(8,new GlobalExecutorPool.DaemonThreadFactory()));
		return finalRS;
	}

	private static <T extends QuantisedLocalFeature<?>> QLFDocument<T> cloneQuery(QLFDocument<T> query){
		QLFDocument<T> retQ = new QLFDocument<T>(query.getEntries(),query.getProperty("docno"),query.getAllProperties());
		return retQ;
	}

	@Override
	public String getFeatureType() {
		return indexes.get(0).getIndexProperty("index.feature.type", "SIFT");
	}

	/**
	 * @param args
	 * @throws Exception
	 */
	public static void main(String[] args) throws Exception {
		MultiIndexSearcherOptions options = new MultiIndexSearcherOptions();
		final CmdLineParser parser = new CmdLineParser(options);

		try {
			parser.parseArgument(args);
		} catch (final CmdLineException e) {
			System.err.println(e.getMessage());
			System.err.println("Usage: java -jar ImageTerrier.jar [options...]");
			parser.printUsage(System.err);
			return;
		}

		MultiIndexSearcher searcher = new MultiIndexSearcher(options);
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
		}
//		else if (options.isServer()) {
//			BasicSearcherXmlRpcServlet.options = options;
//			BasicSearcherXmlRpcServlet.searcher = searcher;
//			final XmlRpcServlet servlet = new XmlRpcServlet() {
//				private static final long serialVersionUID = 1L;
//
//				@Override
//				protected XmlRpcHandlerMapping newXmlRpcHandlerMapping() throws XmlRpcException {
//					final PropertyHandlerMapping mapping = (PropertyHandlerMapping) super.newXmlRpcHandlerMapping();
//					XmlRpcSystemImpl.addSystemHandler(mapping);
//					return mapping;
//				}
//			};
//			final ServletWebServer webServer = new ServletWebServer(servlet, options.serverPort());
//			webServer.start();
//		}
		else {
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

				if (!cmd.contains("-il"))
				{
					for (File f : options.index) {
						cmd += " -il " + f;
					}
				}
				final MultiIndexSearcherOptions interactiveOptions = new MultiIndexSearcherOptions();
				final CmdLineParser interactiveParser = new CmdLineParser(interactiveOptions);

				try {
					interactiveParser.parseArgument(cmd.split("\\s+"));

					if (interactiveOptions.getQueryImage() == null) {
						System.out.println("Error: No query image specified.");
						continue;
					}

					if (!interactiveOptions.getIndexes().equals(options.getIndexes())) {
						// reload index
						System.out.println("Reloading indexes");
						searcher = new MultiIndexSearcher(interactiveOptions);
						options = interactiveOptions;
					}

					// print a description of the query
					System.out.print("SELECT IMG_FILE, SCORE FROM " + interactiveOptions.getIndexes() + " WHERE IMG");
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

					final List<DocidScore> rs = searcher.search(
							interactiveOptions.getQueryImage(),
							interactiveOptions.getRoiCoords(),
							interactiveOptions
					);
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
